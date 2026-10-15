package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 18: the game-bus handlers of the charms. One damage listener (the Lover's Divide shares a blow, the Coin of Fortune bends the next one, the
 * Defender's Dial cuts the dialled kind, the Mirror of Spell Reflection catches a spell), a server tick that runs the {@link Wave18Queue} jobs and
 * the Pocket Campsite's tiny hut once a second, and a player tick for the Tiny Orbit's moon. Every table is bounded and cleared when the server stops.
 */
final class Wave18Events {
    private Wave18Events() {}

    static final Wave18Queue QUEUE = new Wave18Queue();

    /** A pair of hearts joined by the Lover's Divide: both ends map to the same link. */
    record Link(UUID a, UUID b, UUID wearer, long until) {
        UUID other(UUID me) { return a.equals(me) ? b : a; }
    }

    /** The tiny hut of a pitched camp. */
    record Zone(ResourceKey<Level> dimension, BlockPos centre, long until) {}

    private static final Map<UUID, Link> LINKS = new HashMap<>();
    private static final Map<UUID, Zone> ZONES = new HashMap<>();
    private static boolean sharing;
    private static final TagKey<DamageType>[] DIAL = dialTags();

    @SuppressWarnings("unchecked")
    private static TagKey<DamageType>[] dialTags() {
        TagKey<DamageType>[] t = (TagKey<DamageType>[]) new TagKey[3];
        for (int i = 0; i < 3; i++) t[i] = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(Wave18Pure.dialDamageTag(i)));
        return t;
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave18Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave18Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave18Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave18Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave18Events::onStopping);
    }

    // ---------------------------------------------------------------- the tables
    /** Joins the wearer and a partner: each blow either takes is shared (see {@link Wave18Pure#partnerShare}). Replaces older links of both. */
    static void link(ServerPlayer wearer, LivingEntity partner, long now) {
        unlink(wearer.getUUID());
        unlink(partner.getUUID());
        if (LINKS.size() >= Wave18Tune.LINK_CAP * 2) LINKS.clear();
        Link l = new Link(wearer.getUUID(), partner.getUUID(), wearer.getUUID(), now + Wave18Tune.LINK_TICKS);
        LINKS.put(wearer.getUUID(), l);
        LINKS.put(partner.getUUID(), l);
    }

    static void unlink(UUID id) {
        Link l = LINKS.remove(id);
        if (l != null) LINKS.remove(l.other(id));
    }

    static boolean linked(UUID id) { return LINKS.containsKey(id); }

    static void pitch(UUID owner, Zone z) { ZONES.put(owner, z); }

    static void strike(UUID owner) { ZONES.remove(owner); }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        unlink(e.getEntity().getUUID());
        ZONES.remove(e.getEntity().getUUID());
    }

    private static void onStopping(ServerStoppingEvent e) {
        LINKS.clear();
        ZONES.clear();
        QUEUE.clear();
        sharing = false;
    }

    // ---------------------------------------------------------------- damage
    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;                       // the void and /kill are nobody's to share or bend
        long now = victim.level().getGameTime();

        // a coin's good or bad fortune on the next blow the wearer deals
        if (src.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            int f = fortune(attacker, now);
            if (f != 0) {
                e.setAmount((float) (e.getAmount() * Wave18Pure.fortuneOut(f)));
                spend(attacker, f > 0 ? "Fortune: your blow lands true." : "Misfortune: your blow glances off.");
            }
        }
        if (victim instanceof ServerPlayer p) {
            // the Defender's Dial
            if (Wave18Data.getLong(p, Wave18Data.DIAL_UNTIL) > now) {
                int type = Wave18Pure.dialType(Wave18Data.getInt(p, Wave18Data.DIAL_TYPE));
                if (src.is(DIAL[type])) e.setAmount((float) (e.getAmount() * Wave18Tune.DIAL_KEEP));
            }
            // the Mirror of Spell Reflection: a spell that comes while the mirror is raised goes back at its caster
            if (Wave18Data.getLong(p, Wave18Data.MIRROR_UNTIL) > now && Wave6Kit.isMagic(src) && src.getEntity() instanceof LivingEntity caster && caster != p) {
                Wave18Data.putLong(p, Wave18Data.MIRROR_UNTIL, 0L);
                float back = Math.min(e.getAmount(), Wave18Tune.MIRROR_CAP);
                e.setCanceled(true);
                Fx.damage(p, caster, back, "minecraft:magic");
                Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xC9D6F2);
                Fx.say(p, "The mirror throws the spell back.");
                return;
            }
            // a coin's fortune on the next blow taken
            int f = fortune(p, now);
            if (f != 0 && !(src.getEntity() instanceof ServerPlayer sp && sp == p)) {
                e.setAmount((float) (e.getAmount() * Wave18Pure.fortuneIn(f)));
                spend(p, f > 0 ? "Fortune: the blow is softened." : "Misfortune: the blow bites deeper.");
            }
        }
        share(e, victim, now);
    }

    /** The coin's pending fortune for this player: +1, -1, or 0 (none, or run out at the end of the day). */
    private static int fortune(ServerPlayer p, long now) {
        int f = Wave18Data.getInt(p, Wave18Data.FORTUNE);
        if (f == 0) return 0;
        if (Wave18Data.getLong(p, Wave18Data.FORTUNE_UNTIL) <= now) {
            Wave18Data.remove(p, Wave18Data.FORTUNE);
            return 0;
        }
        return f;
    }

    private static void spend(ServerPlayer p, String line) {
        Wave18Data.remove(p, Wave18Data.FORTUNE);
        Wave18Data.remove(p, Wave18Data.FORTUNE_UNTIL);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal(line).withStyle(net.minecraft.ChatFormatting.GOLD), true);
    }

    /** The Lover's Divide: half of the blow goes to the partner, never taking their last health. */
    private static void share(LivingIncomingDamageEvent e, LivingEntity victim, long now) {
        if (sharing || LINKS.isEmpty()) return;
        Link l = LINKS.get(victim.getUUID());
        if (l == null || !(victim.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        ServerPlayer wearer = server.getPlayerList().getPlayer(l.wearer());
        if (now >= l.until() || wearer == null || !wearer.isAlive() || !RelicFlags.has(wearer, Wave18Tune.F_LOVERS)) {
            unlink(victim.getUUID());
            return;
        }
        Entity other = level.getEntity(l.other(victim.getUUID()));
        if (!(other instanceof LivingEntity partner) || !partner.isAlive() || partner.distanceTo(victim) > Wave18Tune.LINK_MAX_DISTANCE) return;
        float share = Wave18Pure.partnerShare(e.getAmount(), partner.getHealth());
        if (share <= 0f) return;
        sharing = true;
        boolean hurt;
        try {
            hurt = partner.hurt(partner.damageSources().generic(), share);
        } finally {
            sharing = false;
        }
        if (hurt) {
            e.setAmount(e.getAmount() - share);
            RukhFx.spray(level, Wave18Kit.align(0xE8A23A), partner.getX(), partner.getY() + partner.getBbHeight() * 0.6, partner.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
    }

    // ---------------------------------------------------------------- ticks
    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        QUEUE.tick(server.overworld().getGameTime());                                       // every level shares the overworld's game time, which the jobs are timed in
        if (!ZONES.isEmpty() && server.getTickCount() % 20 == 5) huts(server);
    }

    /** The Pocket Campsite's tiny hut, once a second: those who rest inside mend, and hostile things are turned away. */
    private static void huts(MinecraftServer server) {
        for (Map.Entry<UUID, Zone> en : new ArrayList<>(ZONES.entrySet())) {
            Zone z = en.getValue();
            ServerLevel level = server.getLevel(z.dimension());
            if (level == null) { ZONES.remove(en.getKey()); continue; }
            if (level.getGameTime() >= z.until()) {
                ZONES.remove(en.getKey());
                ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
                if (owner != null) Fx.say(owner, "The tiny hut fades; the tents stay until you pack them.");
                continue;
            }
            if (!level.isLoaded(z.centre())) continue;
            Vec3 c = Vec3.atCenterOf(z.centre());
            double r = Wave18Tune.CAMP_RADIUS;
            AABB box = new AABB(c, c).inflate(r, 4.0, r);
            for (Player p : level.players()) {
                if (p.isSpectator() || p.position().distanceToSqr(c) > r * r) continue;
                Wave6Kit.give(p, "minecraft:regeneration", 80, 0, true);
                if (p.getFoodData().getFoodLevel() < 16) p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel() + 1);          // a slow top-up, not endless saturation
            }
            int n = 0;
            for (Mob m : level.getEntitiesOfClass(Mob.class, box, mob -> mob instanceof Enemy)) {
                if (n++ >= 24) break;
                Vec3 away = m.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() > r * r) continue;
                Vec3 dir = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
                m.setDeltaMovement(dir.x * 0.6, 0.25, dir.z * 0.6);
                m.hurtMarked = true;
                if (m.getTarget() instanceof Player) m.setTarget(null);
            }
            RukhFx.Alignment glow = Wave18Kit.align(0xF2C14E);
            for (int i = 0; i < 16; i++) {
                double a = i * (Math.PI * 2 / 16) + level.getGameTime() * 0.05;
                RukhFx.spray(level, glow, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
            }
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator() || (p.tickCount & 1) != 0) return;
        if (!RelicFlags.has(p, Wave18Tune.F_ORBIT)) return;
        ServerLevel level = p.serverLevel();
        double a = p.tickCount * 0.2;
        double x = p.getX() + Math.cos(a) * Wave18Tune.ORBIT_RADIUS, z = p.getZ() + Math.sin(a) * Wave18Tune.ORBIT_RADIUS;
        double y = p.getY() + p.getBbHeight() + 0.2 + Math.sin(a * 0.5) * 0.05;
        RukhFx.spray(level, Wave18Kit.align(0xE9E4D4), x, y, z, 1, 0, 0, 0, 0);
        RukhFx.spray(level, Wave18Kit.align(0x8E8A82), x + 0.03, y + 0.02, z, 1, 0, 0, 0, 0);
        if ((p.tickCount + p.getId()) % 40 == 8 && level.getMaxLocalRawBrightness(p.blockPosition()) < Wave18Tune.ORBIT_DARK) {
            Wave6Kit.give(p, "minecraft:night_vision", 260, 0, true);                       // the little moon lights the dark
        }
    }

    /** Players the pitched camp should treat as inside (for tests of the glue and for the pack-up check). */
    static List<UUID> zoneOwners() { return new ArrayList<>(ZONES.keySet()); }
}
