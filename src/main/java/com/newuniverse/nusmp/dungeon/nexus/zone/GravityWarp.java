package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The gravity magic of the Underworld Gate, the part that touches the game: the zones of {@link GravityCycle} turned into what a player
 * feels and what a player sees. Every second the zone's site tells it who is inside ({@link #watch}); every tick it works out each zone's
 * state, applies the bounded push to the players inside, and glows the ground:
 * <ul>
 *   <li><b>Heavy</b>: the Gravity attribute is raised and the jump strength cut by transient attribute modifiers, Slowness comes with it. The
 *       client predicts its own movement, so nothing jerks.</li>
 *   <li><b>Light</b> and <b>reversal</b>: Levitation (which also keeps the server from kicking a player for floating) with Slow Falling, and
 *       only below the zone's cap.</li>
 *   <li><b>Telegraph</b>: the last four seconds of a calm before a spell, and the spell itself, glow in the floor in the colour of what is
 *       coming (crimson, blue, violet), with a sound cue and a line on the action bar.</li>
 * </ul>
 * Every effect is short (it lapses a moment after the player leaves the zone by itself), the attribute modifiers are transient (never saved)
 * and cleared the moment the player is out of every zone, dead, gone, in another dimension or the hall has collapsed, and a sweep every two
 * seconds removes any that were missed. Falling is cushioned for everyone inside the hall (and for six seconds after): the gravity of this
 * place never kills. Creative and spectator players, and those wearing an attuned Band of the Spatial Anchor, are not touched.
 */
public final class GravityWarp {
    private GravityWarp() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private static final ResourceLocation GRAVITY_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "underworld_gravity"),
            JUMP_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "underworld_jump");
    /** Ticks without a report from the site before an arena or a player in it is let go; how long an effect lasts; how often it is renewed; how long a fall stays cushioned; sweep and emit periods. */
    private static final int STALE = 45, EFFECT = 14, SOFT = 50, RENEW = 4, CUSHION = 120, SWEEP = 40, EMIT = 4;
    /** How far (blocks) a player sees the glow of a zone. */
    private static final double SEE = 52.0;

    /** One hall that has players in it: its zones, who is in it and when it was last reported. */
    private static final class Arena {
        final int cellX, cellZ;
        final List<GravityCycle.Zone> zones;
        long seen;
        final Map<UUID, Long> players = new HashMap<>();
        final int[] lastCode;

        Arena(int cellX, int cellZ) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.zones = UnderworldGatePlan.zones(cellX, cellZ);
            this.lastCode = new int[zones.size()];
            java.util.Arrays.fill(lastCode, -1);
        }
    }

    private static final Map<Long, Arena> ARENAS = new HashMap<>();
    /** Fall damage is cancelled for these players until the game time. */
    private static final Map<UUID, Long> CUSHION_UNTIL = new HashMap<>();
    /** Players who carry one of the transient modifiers. */
    private static final Set<UUID> MODIFIED = new HashSet<>();
    /** What a player was last told (zone and state), so the action bar is not repeated every tick. */
    private static final Map<UUID, Integer> TOLD = new HashMap<>();
    /** Whether a player wears an attuned Band of the Spatial Anchor, and the game time it was looked at (the lookup reads their Curios slots, so it is not made every tick). */
    private static final Map<UUID, long[]> ANCHORED = new HashMap<>();
    private static final int ANCHOR_EVERY = 20;

    // ---------------------------------------------------------------- what the site reports
    /** Called about once a second by the Underworld Gate's site with the players inside the hall. */
    public static void watch(ServerLevel level, int cellX, int cellZ, List<ServerPlayer> inside) {
        long now = level.getGameTime();
        Arena a = ARENAS.computeIfAbsent(NexusCells.key(cellX, cellZ), k -> new Arena(cellX, cellZ));
        a.seen = now;
        for (ServerPlayer p : inside) a.players.put(p.getUUID(), now);
    }

    /** The hall of this cell collapsed (or was reset): its players are let go at once. */
    public static void release(MinecraftServer server, int cellX, int cellZ) {
        Arena a = ARENAS.remove(NexusCells.key(cellX, cellZ));
        if (a == null) return;
        for (UUID id : a.players.keySet()) free(server, id);
    }

    /** The world is closing: nothing of it may stay in the statics. */
    public static void onServerStopping(ServerStoppingEvent event) {
        ARENAS.clear();
        CUSHION_UNTIL.clear();
        MODIFIED.clear();
        TOLD.clear();
        ANCHORED.clear();
        UnderworldDirector.forgetAll();
        UnderworldGates.forget();
        UnderworldGateSite.forget();
    }

    // ---------------------------------------------------------------- the server tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ARENAS.isEmpty() && MODIFIED.isEmpty() && CUSHION_UNTIL.isEmpty() && !UnderworldDirector.hasBars()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(LivingDungeon.DIMENSION);
        if (level == null) {
            for (UUID id : new HashSet<>(MODIFIED)) free(server, id);
            ARENAS.clear();
            CUSHION_UNTIL.clear();
            UnderworldDirector.forgetAll();
            return;
        }
        long now = level.getGameTime();
        Set<UUID> touched = new HashSet<>();
        for (Iterator<Arena> it = ARENAS.values().iterator(); it.hasNext(); ) {
            Arena a = it.next();
            if (now - a.seen > STALE) {
                for (UUID id : a.players.keySet()) free(server, id);
                it.remove();
                continue;
            }
            try {
                tickArena(server, level, a, now, touched);
            } catch (RuntimeException failure) {
                LOG.warn("[nusmp] The gravity of the Underworld Gate of cell ({}, {}) failed this tick", a.cellX, a.cellZ, failure);
            }
        }
        if (now % SWEEP == 0) {
            sweep(server, touched);
            UnderworldDirector.sweepBars(level);
            CUSHION_UNTIL.values().removeIf(until -> until < now);
            ANCHORED.keySet().removeIf(id -> !MODIFIED.contains(id) && !touched.contains(id));
        }
    }

    private static void tickArena(MinecraftServer server, ServerLevel level, Arena a, long now, Set<UUID> touched) {
        int n = a.zones.size();
        GravityCycle.State[] states = new GravityCycle.State[n];
        for (int i = 0; i < n; i++) states[i] = GravityCycle.at(a.zones.get(i), now);
        List<ServerPlayer> here = new java.util.ArrayList<>();
        for (Iterator<Map.Entry<UUID, Long>> it = a.players.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive() || p.level() != level || now - e.getValue() > STALE) {
                free(server, e.getKey());
                it.remove();
                continue;
            }
            here.add(p);
            CUSHION_UNTIL.put(p.getUUID(), now + CUSHION);
            if (p.isCreative() || p.isSpectator() || anchored(p, now)) {
                free(server, p.getUUID());
                continue;
            }
            touched.add(p.getUUID());
            apply(p, a, states, now);
        }
        if (!here.isEmpty()) telegraph(level, a, states, here, now);
    }

    /** Is the player held steady by the Band of the Spatial Anchor? Looked at once a second. */
    private static boolean anchored(ServerPlayer p, long now) {
        long[] c = ANCHORED.get(p.getUUID());
        if (c != null && now >= c[0] && now - c[0] < ANCHOR_EVERY) return c[1] != 0;
        boolean yes = Relics.anchored(p);
        ANCHORED.put(p.getUUID(), new long[]{now, yes ? 1 : 0});
        return yes;
    }

    // ---------------------------------------------------------------- what a player feels
    private static void apply(ServerPlayer p, Arena a, GravityCycle.State[] states, long now) {
        int best = -1;
        double bestScore = -1.0;
        for (int i = 0; i < a.zones.size(); i++) {
            if (!a.zones.get(i).contains(p.getX(), p.getY(), p.getZ())) continue;
            double score = states[i].strength() + (states[i].telegraphing() ? 0.01 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        GravityCycle.Push push = GravityCycle.Push.NONE;
        if (best >= 0) {
            push = GravityCycle.push(states[best], a.zones.get(best), p.getY());
            tell(p, best, states[best]);
        } else {
            TOLD.remove(p.getUUID());
        }
        modify(p, GRAVITY_ID, Attributes.GRAVITY, push.gravityBoost(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modify(p, JUMP_ID, Attributes.JUMP_STRENGTH, -push.jumpCut(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (push.gravityBoost() > 0.005 || push.jumpCut() > 0.005) MODIFIED.add(p.getUUID());
        else if (MODIFIED.contains(p.getUUID()) && !has(p)) MODIFIED.remove(p.getUUID());
        if (!push.any() || Math.floorMod(now + p.getId(), RENEW) != 0) {
            if (!p.onGround()) p.fallDistance = 0f;
            return;
        }
        if (push.slowAmp() >= 0) effect(p, MobEffects.MOVEMENT_SLOWDOWN, push.slowAmp(), EFFECT);
        if (push.levitateAmp() >= 0) effect(p, MobEffects.LEVITATION, push.levitateAmp(), EFFECT);
        if (push.slowFall()) effect(p, MobEffects.SLOW_FALLING, 0, SOFT);
        p.fallDistance = 0f;
    }

    private static void effect(ServerPlayer p, Holder<MobEffect> effect, int amplifier, int ticks) {
        p.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, true));
    }

    /** Sets a transient modifier to `amount` (or takes it away when that is next to nothing), without resending it for a tiny change. */
    private static void modify(ServerPlayer p, ResourceLocation id, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (Math.abs(amount) <= 0.005) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        if (cur == null || Math.abs(cur.amount() - amount) > 0.02) inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static boolean has(ServerPlayer p) {
        AttributeInstance g = p.getAttribute(Attributes.GRAVITY), j = p.getAttribute(Attributes.JUMP_STRENGTH);
        return (g != null && g.hasModifier(GRAVITY_ID)) || (j != null && j.hasModifier(JUMP_ID));
    }

    /** What the action bar says while a player stands in a zone: once for each change of the zone's state. */
    private static void tell(ServerPlayer p, int zone, GravityCycle.State s) {
        int code = 1 + zone * 16 + s.mode().ordinal() * 4 + s.warning().ordinal();
        Integer last = TOLD.put(p.getUUID(), code);
        if (last != null && last == code) return;
        String text = s.telegraphing() ? UnderworldText.warning(s.warning()) : UnderworldText.active(s.mode());
        if (text.isEmpty()) return;
        GravityCycle.Mode shown = s.telegraphing() ? s.warning() : s.mode();
        ChatFormatting colour = shown == GravityCycle.Mode.HEAVY ? ChatFormatting.RED : shown == GravityCycle.Mode.LIGHT ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE;
        p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    // ---------------------------------------------------------------- letting go
    private static void free(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) clear(p);
        MODIFIED.remove(id);
        TOLD.remove(id);
    }

    private static void clear(ServerPlayer p) {
        AttributeInstance g = p.getAttribute(Attributes.GRAVITY), j = p.getAttribute(Attributes.JUMP_STRENGTH);
        if (g != null && g.hasModifier(GRAVITY_ID)) g.removeModifier(GRAVITY_ID);
        if (j != null && j.hasModifier(JUMP_ID)) j.removeModifier(JUMP_ID);
    }

    /** Takes a modifier of ours off anyone who has one and was not touched this tick, whatever the book says. */
    private static void sweep(MinecraftServer server, Set<UUID> touched) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (touched.contains(p.getUUID())) continue;
            if (has(p)) clear(p);
            MODIFIED.remove(p.getUUID());
        }
    }

    // ---------------------------------------------------------------- falling
    /** The gravity of this place never kills: anyone in the hall (and for a while after) takes no fall damage. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (CUSHION_UNTIL.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || !e.getSource().is(DamageTypeTags.IS_FALL)) return;
        Long until = CUSHION_UNTIL.get(p.getUUID());
        if (until != null && p.level().getGameTime() <= until) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- what a player sees and hears
    private static void telegraph(ServerLevel level, Arena a, GravityCycle.State[] states, List<ServerPlayer> here, long now) {
        for (int i = 0; i < a.zones.size(); i++) {
            GravityCycle.Zone z = a.zones.get(i);
            GravityCycle.State s = states[i];
            boolean near = false;
            for (ServerPlayer p : here) if (z.distance(p.getX(), p.getZ()) <= z.radius() + SEE) near = true;
            if (!near) continue;
            GravityCycle.Mode shown = s.active() ? s.mode() : s.warning();
            int code = shown.ordinal() * 2 + (s.active() ? 1 : 0);
            if (code != a.lastCode[i]) {
                if (a.lastCode[i] >= 0 && shown != GravityCycle.Mode.CALM) cue(level, z, shown, s.active());
                a.lastCode[i] = code;
            }
            if (now % EMIT == Math.floorMod(i, EMIT)) glow(level, z, s, shown, now);
        }
    }

    /** A sound at the middle of the zone when its state changes: a low note for a press, a high one for a lift, a chord for a flip. */
    private static void cue(ServerLevel level, GravityCycle.Zone z, GravityCycle.Mode mode, boolean starting) {
        BlockPos at = new BlockPos(z.x(), z.yMin() + 1, z.z());
        float pitch = mode == GravityCycle.Mode.HEAVY ? 0.5f : mode == GravityCycle.Mode.LIGHT ? 1.6f : 1.0f;
        level.playSound(null, at, starting ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 2.5f, pitch);
        if (starting && mode == GravityCycle.Mode.REVERSAL) level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 2.5f, 0.7f);
    }

    /** The glow in the floor: a ring of dust at the edge of the zone in the colour of what is coming (or here), denser as the change draws near, with a column of something moving the way the gravity does. */
    private static void glow(ServerLevel level, GravityCycle.Zone z, GravityCycle.State s, GravityCycle.Mode shown, long now) {
        boolean calm = shown == GravityCycle.Mode.CALM;
        if (calm && now % (EMIT * 6L) != Math.floorMod(z.id(), EMIT)) return;       // a dim ring now and then
        int rgb = GravityCycle.colour(shown);
        Vector3f colour = new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        double level01 = calm ? 0.0 : s.active() ? s.strength() : s.warnProgress();
        DustParticleOptions dust = new DustParticleOptions(colour, calm ? 0.8f : (float) (1.1 + 0.8 * level01));
        RandomSource rnd = level.getRandom();
        double cx = z.x() + 0.5, cz = z.z() + 0.5, y = z.yMin() + 0.1;
        int ring = calm ? 4 : 6 + (int) Math.round(14 * level01);
        for (int k = 0; k < ring; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            level.sendParticles(dust, cx + Math.cos(ang) * z.radius(), y, cz + Math.sin(ang) * z.radius(), 1, 0.05, 0.02, 0.05, 0.0);
        }
        if (calm) return;
        int column = s.active() ? 4 + (int) Math.round(6 * level01) : 2;
        for (int k = 0; k < column; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * z.radius();
            double top = Math.min(z.yMax(), z.yMin() + 14);
            double py = z.yMin() + rnd.nextDouble() * (top - z.yMin());
            double px = cx + Math.cos(ang) * d, pz = cz + Math.sin(ang) * d;
            switch (shown) {
                case HEAVY -> level.sendParticles(ParticleTypes.CRIMSON_SPORE, px, py, pz, 0, 0.0, -0.12, 0.0, 1.0);
                case LIGHT -> level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 0, 0.0, 0.07, 0.0, 1.0);
                default -> level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0, 0.0, 0.2, 0.0, 1.0);
            }
        }
    }
}
