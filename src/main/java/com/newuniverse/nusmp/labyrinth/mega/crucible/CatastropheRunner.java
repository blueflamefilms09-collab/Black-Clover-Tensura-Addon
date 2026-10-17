package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonEffects;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the three catastrophes of the Crucible (a collapse of falling dust, a toxic magicule flood, a Cursed Spirit) are one small tick-driven state
 * machine, because each is "something that happens in the vault for a while and then ends", and the ending is what matters: the Crucible is RESOLVED
 * (and {@code MegaDungeon.crucibleResolved} told) when the catastrophe ends, however it ends. The saved state is only the {@code catastrophe} string
 * of the sector's crucible; the running state (start tick, the spirit's id, the clocks) is kept in memory and is NOT saved on purpose: a server restart
 * in the middle of a catastrophe simply ends it ({@code CrucibleVault} resolves every CHOSEN vault that has no running catastrophe), so nobody is
 * ever trapped by a countdown that no longer exists.
 * <p>
 * Every one is survivable by running out of the vault, and none of them changes a single block (so nothing needs the Dungeon Guard's permission and
 * nothing has to be restored): the COLLAPSE warns for two seconds with rumbling and a rain of dust, then pulses small damage every 1.5 seconds to the
 * players inside the vault box (never more than 35 % of their health per pulse); the FLOOD renews magicule sickness (level from the sector's threat) and
 * slowness II once a second on the players inside the box, counts down on the action bar and ends early when nobody is inside any more; the SPIRIT
 * appears at the vault centre inside a three second summoning circle, then hunts, and ends when it dies, when it is gone from loaded ground, or
 * ten minutes after the last player left its sector. The clocks are the pure {@link CatastropheRules}; this class only applies them. The new crowd
 * control here is environmental (a status effect inside a room, gone when the player leaves), not a spell, so it does not go through the
 * caster-based {@code BalanceLaw.beginControl}.
 */
public final class CatastropheRunner {
    private CatastropheRunner() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The in-memory state of one running catastrophe. */
    private static final class Active {
        final int sectorId, threat;
        final String kind;
        final long start;
        final BlockPos center;
        /** True for a real pick (the Crucible is resolved at the end); false for an admin's test run. */
        final boolean resolves;
        UUID spirit;
        BlockPos spiritPos;
        long lastPlayerTick;
        boolean awake;
        /** Consecutive ticks the spirit's ground was loaded and the spirit was not found (a chunk's entities load a moment after the chunk). */
        int missingTicks;

        Active(int sectorId, int threat, String kind, long start, BlockPos center, boolean resolves) {
            this.sectorId = sectorId;
            this.threat = threat;
            this.kind = kind;
            this.start = start;
            this.center = center;
            this.resolves = resolves;
            this.lastPlayerTick = start;
        }
    }

    private static final Map<Integer, Active> ACTIVE = new LinkedHashMap<>();
    /** How many catastrophes were started since the server came up (the self-test reads it: one pick must start exactly one). */
    private static int started;

    // ---------------------------------------------------------------- queries
    /** How many catastrophes have been started since the server came up. */
    public static int started() { return started; }

    /** The id of the running spirit of this sector's catastrophe, or null. */
    static UUID spiritOf(int sectorId) {
        Active a = ACTIVE.get(sectorId);
        return a == null ? null : a.spirit;
    }

    /** Is a catastrophe running for this sector? */
    public static boolean active(int sectorId) { return ACTIVE.containsKey(sectorId); }

    /** The kind running for this sector ({@code collapse}, {@code flood}, {@code spirit}), or "". */
    public static String kindOf(int sectorId) {
        Active a = ACTIVE.get(sectorId);
        return a == null ? "" : a.kind;
    }

    /** Is this entity the spirit of a running catastrophe? (A spirit loaded from disk that nobody tracks is an orphan.) */
    static boolean knowsSpirit(UUID id) {
        for (Active a : ACTIVE.values()) if (id.equals(a.spirit)) return true;
        return false;
    }

    /** Forgets every running catastrophe (the server is stopping: the next start resolves what was left CHOSEN). */
    public static void clear() { ACTIVE.clear(); }

    // ---------------------------------------------------------------- start / stop
    /**
     * Starts a catastrophe of {@code kind} centred on {@code center} (the vault centre for a real pick). {@code resolves} is true for a pick (the Crucible
     * is resolved when it ends). A SPIRIT that cannot be summoned (no roster entity exists) becomes a COLLAPSE and the sector's saved catastrophe is
     * corrected. Returns false when it could not start at all (no centre).
     */
    public static boolean start(ServerLevel level, Sector s, String kind, ServerPlayer chooser, BlockPos center, boolean resolves) {
        BlockPos at = center != null ? center : (chooser != null ? chooser.blockPosition() : null);
        if (at == null) return false;
        stop(level, s.id);
        String k = CrucibleRules.KINDS.contains(kind) ? kind : CrucibleRules.COLLAPSE;
        Mob spirit = null;
        if (CrucibleRules.SPIRIT.equals(k)) {
            try {
                spirit = CursedSpirits.spawn(level, s, at, chooser);
            } catch (RuntimeException e) {                                              // a borrowed body may refuse to be built: that is a collapse, not a crash
                LOG.warn("[nusmp] mega crucible: the Cursed Spirit of sector {} could not be built: {}", s.id, e.toString());
            }
            if (spirit == null) {
                LOG.warn("[nusmp] mega crucible: no Cursed Spirit could be summoned in sector {}: it becomes a collapse", s.id);
                k = CrucibleRules.COLLAPSE;
            }
        }
        if (resolves && !k.equals(s.crucible.catastrophe)) {
            s.crucible.catastrophe = k;
            MegaData.get(level.getServer()).setDirty();
        }
        Active a = new Active(s.id, s.threat, k, level.getGameTime(), at.immutable(), resolves);
        if (spirit != null) {
            a.spirit = spirit.getUUID();
            a.spiritPos = spirit.blockPosition();
        }
        ACTIVE.put(s.id, a);
        started++;
        try {
            intro(level, a);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] mega crucible: the opening of the {} in sector {} failed: {}", a.kind, s.id, e.toString());
        }
        return true;
    }

    /** Stops a running catastrophe without resolving anything: its spirit, if loaded, is let go. */
    public static void stop(ServerLevel level, int sectorId) {
        Active a = ACTIVE.remove(sectorId);
        if (a == null || a.spirit == null || level == null) return;
        Entity e = level.getEntity(a.spirit);
        if (e != null) {
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            e.discard();
        }
    }

    private static void intro(ServerLevel level, Active a) {
        BlockPos c = a.center;
        List<ServerPlayer> all = CruciblePedestals.playersInBox(level, c);
        switch (a.kind) {
            case CrucibleRules.COLLAPSE -> {
                for (ServerPlayer p : all) p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.collapse").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                level.playSound(null, c, SoundEvents.DEEPSLATE_TILES_FALL, SoundSource.BLOCKS, 1.5f, 0.5f);
            }
            case CrucibleRules.FLOOD -> {
                for (ServerPlayer p : all) p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.flood_start").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                level.playSound(null, c, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.5f, 0.5f);
            }
            default -> {
                for (ServerPlayer p : all) p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.spirit").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
                level.playSound(null, c, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.5f, 0.8f);
            }
        }
    }

    // ---------------------------------------------------------------- the tick
    /** Advances every running catastrophe by one tick; called from the server tick handler (cheap when nothing runs). */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        tick(server, level, level.getGameTime());
    }

    /** The same with the clock handed in: the self-tests drive a whole catastrophe in a few milliseconds by passing a made-up game time. */
    static void tick(MinecraftServer server, ServerLevel level, long now) {
        if (ACTIVE.isEmpty()) return;
        MegaData data = MegaData.get(server);
        for (Iterator<Active> it = ACTIVE.values().iterator(); it.hasNext(); ) {
            Active a = it.next();
            Sector s = data.sector(a.sectorId);
            if (s == null) {
                it.remove();
                continue;
            }
            boolean done;
            try {
                int elapsed = (int) Math.min(Integer.MAX_VALUE, now - a.start);
                done = switch (a.kind) {
                    case CrucibleRules.COLLAPSE -> collapse(level, a, elapsed);
                    case CrucibleRules.FLOOD -> flood(level, a, elapsed);
                    default -> spirit(level, s, a, elapsed, now);
                };
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] mega crucible: the {} of sector {} failed and ends: {}", a.kind, a.sectorId, e.toString());
                done = true;
            }
            if (done) {
                it.remove();
                finish(level, s, a);
            }
        }
    }

    /** The end of a catastrophe: a real one resolves the Crucible (only if it is still CHOSEN: a reset in between wins). */
    private static void finish(ServerLevel level, Sector s, Active a) {
        if (!a.resolves) return;
        Sector.Crucible c = s.crucible;
        Phase now = Phase.of(c.phase.name());
        if (now != Phase.CHOSEN) return;
        c.phase = Sector.Crucible.Phase.valueOf(CrucibleRules.afterCatastrophe(now).name());
        MegaData.get(level.getServer()).setDirty();
        for (ServerPlayer p : CruciblePedestals.playersInBox(level, a.center))
            p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.resolved").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        MegaDungeon.crucibleResolved(level, s.id);
    }

    // ---------------------------------------------------------------- collapse
    private static boolean collapse(ServerLevel level, Active a, int elapsed) {
        int total = CatastropheRules.collapseTotalTicks(ThemeConfig.get(ThemeConfig.COLLAPSE_SECONDS));
        if (elapsed >= total) return true;
        int warn = CatastropheRules.collapseWarnTicks(total);
        BlockPos c = a.center;
        RandomSource rng = level.getRandom();
        if (elapsed % 3 == 0) dust(level, a, elapsed >= warn ? 3 : 1);
        if (elapsed % 12 == 0)
            level.playSound(null, c, elapsed < warn ? SoundEvents.GRAVEL_FALL : SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f + rng.nextFloat() * 0.3f);
        if (CatastropheRules.collapsePulseAt(elapsed, total)) for (ServerPlayer p : CruciblePedestals.victimsInBox(level, c)) pulse(level, a.threat, p);
        return false;
    }

    /** One collapse pulse on one victim: a blow of the configured size (never more than 35 % of its maximum health), a puff of smoke and a dull crash. */
    static void pulse(ServerLevel level, int threat, LivingEntity victim) {
        float dmg = CrucibleRules.collapsePulseDamage(threat, ThemeConfig.get(ThemeConfig.COLLAPSE_DAMAGE), ThemeConfig.get(ThemeConfig.COLLAPSE_DAMAGE_PER_THREAT), victim.getMaxHealth());
        if (dmg > 0f) victim.hurt(level.damageSources().generic(), dmg);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + 0.3, victim.getZ(), 12, 0.5, 0.2, 0.5, 0.04);
        level.playSound(null, victim.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.7f, 0.4f);
    }

    /** A rain of deepslate dust from the ceiling: over every player in the box and at a few random places of the hall. */
    private static void dust(ServerLevel level, Active a, int intensity) {
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DEEPSLATE.defaultBlockState());
        for (ServerPlayer p : CruciblePedestals.playersInBox(level, a.center))
            level.sendParticles(dust, p.getX(), p.getY() + 2.6, p.getZ(), 6 * intensity, 1.6, 0.5, 1.6, 0.0);
        RandomSource rng = level.getRandom();
        for (int i = 0; i < intensity; i++) {
            double x = a.center.getX() + 0.5 + (rng.nextDouble() - 0.5) * 22.0, z = a.center.getZ() + 0.5 + (rng.nextDouble() - 0.5) * 22.0;
            level.sendParticles(dust, x, a.center.getY() + 6.0 + rng.nextDouble() * 3.0, z, 10, 1.5, 0.4, 1.5, 0.0);
        }
    }

    // ---------------------------------------------------------------- flood
    private static boolean flood(ServerLevel level, Active a, int elapsed) {
        int total = CatastropheRules.floodTotalTicks(ThemeConfig.get(ThemeConfig.FLOOD_SECONDS));
        if (elapsed >= total) return true;
        BlockPos c = a.center;
        if (elapsed % 4 == 0) fumes(level, c);
        if (!CatastropheRules.floodPulseAt(elapsed)) return false;
        List<ServerPlayer> victims = CruciblePedestals.victimsInBox(level, c);
        if (CatastropheRules.floodEndsEarly(elapsed, victims.size())) return true;
        int amp = CrucibleRules.floodAmplifier(a.threat, ThemeConfig.get(ThemeConfig.FLOOD_AMPLIFIER_PER_THREAT));
        int left = CatastropheRules.floodSecondsLeft(elapsed, total);
        for (ServerPlayer p : victims) {
            floodEffects(p, amp);
            p.displayClientMessage(Component.translatable("message.nusmp.mega.crucible.flood", left).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        return false;
    }

    /** One second of the flood on one victim: magicule sickness of the given level and slowness II, each for three seconds, so they die away by themselves when it leaves. */
    static void floodEffects(LivingEntity victim, int amplifier) {
        victim.addEffect(new MobEffectInstance(DungeonEffects.MAGICULE_SICKNESS, 60, amplifier, false, true, true));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
    }

    /** Violet magicule fumes rising from the vault floor. */
    private static void fumes(ServerLevel level, BlockPos c) {
        DustParticleOptions violet = new DustParticleOptions(new Vector3f(0.58f, 0.22f, 0.82f), 1.6f);
        RandomSource rng = level.getRandom();
        for (int i = 0; i < 4; i++) {
            double x = c.getX() + 0.5 + (rng.nextDouble() - 0.5) * 20.0, z = c.getZ() + 0.5 + (rng.nextDouble() - 0.5) * 20.0;
            level.sendParticles(violet, x, c.getY() + 0.2, z, 6, 0.8, 0.3, 0.8, 0.0);
            level.sendParticles(ParticleTypes.WITCH, x, c.getY() + 0.3, z, 0, 0.0, 1.0, 0.0, 0.12);
        }
    }

    // ---------------------------------------------------------------- spirit
    private static boolean spirit(ServerLevel level, Sector s, Active a, int elapsed, long now) {
        Entity e = a.spirit == null ? null : level.getEntity(a.spirit);
        if (e != null) {
            if (!e.isAlive()) return true;                                           // it died: the Crucible is done
            a.spiritPos = e.blockPosition();
            a.missingTicks = 0;
            if (e instanceof Mob mob) {
                if (!a.awake) {
                    if (CatastropheRules.spiritAwake(elapsed)) {
                        a.awake = true;
                        ServerPlayer first = nearestVictim(level, e);
                        CursedSpirits.wake(mob, first);
                        SpiritRules.sweepSkills(mob);                                // a body that learned something while it slept
                        level.playSound(null, mob.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.4f, 0.9f);
                    } else if (elapsed % 3 == 0) {
                        circle(level, mob, elapsed);
                    }
                } else if (elapsed % 100 == 0 && (mob.getTarget() == null || !mob.getTarget().isAlive())) {
                    ServerPlayer next = nearestVictim(level, e);                    // a body whose own AI lost its target is pointed at the nearest player
                    if (next != null && next.distanceToSqr(e) < 48.0 * 48.0) mob.setTarget(next);
                }
            }
        } else {
            a.missingTicks = a.spiritPos != null && level.isLoaded(a.spiritPos) ? a.missingTicks + 1 : 0;
            if (CatastropheRules.spiritGone(false, a.missingTicks >= 40)) return true;   // missing from loaded ground for two seconds: it died or was removed
        }
        if (elapsed % 20 != 0) return false;
        if (playerInSector(level, s)) {
            a.lastPlayerTick = now;
            return false;
        }
        if (!CatastropheRules.spiritAbandoned(now, a.lastPlayerTick, ThemeConfig.get(ThemeConfig.SPIRIT_MINUTES))) return false;
        if (e != null) {                                                             // nobody came back: the spirit is let go
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            e.discard();
        }
        return true;
    }

    private static boolean playerInSector(ServerLevel level, Sector s) {
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && s.containsRegion(p.blockPosition())) return true;
        return false;
    }

    private static ServerPlayer nearestVictim(ServerLevel level, Entity from) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || !p.isAlive()) continue;
            double d = p.distanceToSqr(from);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    /** The summoning circle: a ring of soul fire that tightens round the spirit while it is still held. */
    private static void circle(ServerLevel level, Mob mob, int elapsed) {
        double radius = 1.8 - 1.2 * Math.min(1.0, elapsed / (double) CatastropheRules.SPIRIT_SUMMON_TICKS);
        for (int i = 0; i < 10; i++) {
            double ang = (Math.PI * 2.0) * i / 10.0 + elapsed * 0.25;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX() + Math.cos(ang) * radius, mob.getY() + 0.1, mob.getZ() + Math.sin(ang) * radius, 1, 0.0, 0.05, 0.0, 0.02);
        }
        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 2, 0.3, 0.6, 0.3, 0.02);
    }
}
