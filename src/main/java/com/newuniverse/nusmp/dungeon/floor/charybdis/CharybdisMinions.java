package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * The Charybdis Spawn's megalodons (the "smaller megalodons" of the design): the real Tensura megalodon, looked up by id at run time (nothing
 * happens if Tensura or the creature is absent). They tear loose from the basin wall near the players, at most {@link CharybdisTable#MINION_CAP} at
 * once, each marked with the scoreboard tag {@code nusmp_charybdis_minion}, the glowing outline and the id of the boss that called it. They are driven
 * from outside, because the Tensura brain owns the creature (set the target, walk to it, strike in reach), their blows on a player are capped to
 * a share of the player's health, they leave no loot, and they go with the boss.
 */
public final class CharybdisMinions {
    private CharybdisMinions() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The persistent mark on a megalodon: the UUID of the boss that called it. */
    static final String KEY_BOSS = "nusmp_charybdis_boss";
    private static final String MEGALODON = "tensura:megalodon";
    /** How far a megalodon sees a foe (blocks), and how often the driver may ask it for a blow (ticks). */
    private static final double SIGHT = 48;
    private static final int STRIKE_EVERY = 40;
    /** The tries to find room for one megalodon: offsets round the ring (radians) and up or down (blocks). */
    private static final double[] RING_OFFSET = {0, 0.12, -0.12, 0, 0, 0.25, -0.25};
    private static final int[] HEIGHT_OFFSET = {0, 0, 0, 2, -2, 0, 0};
    /** When each megalodon may next be asked for a blow (game ticks); only the megalodons that are alive are kept. */
    private static final Map<UUID, Long> NEXT_STRIKE = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    static boolean isMinion(Entity e) { return e != null && e.getTags().contains(CharybdisTable.TAG_MINION); }

    static boolean isBoss(Entity e) { return e != null && e.getTags().contains(CharybdisTable.TAG_BOSS); }

    /** The state held in statics is dropped when the world closes. */
    static void clear() {
        NEXT_STRIKE.clear();
        WARNED.clear();
    }

    /** The world is closing: nothing of it (the entities, their schedules) may stay behind in the statics. */
    public static void onServerStopping(ServerStoppingEvent event) { clear(); }

    // ================================================================ who is out
    /** The live megalodons this boss called (found by their mark, in the basin's volume). */
    static List<Mob> alive(ServerLevel level, CharybdisSpawnBoss boss) {
        String me = boss.getUUID().toString();
        Vec3 c = boss.arenaCenter();
        AABB box = new AABB(c, c).inflate(CharybdisTable.ARENA_OUTER, CharybdisTable.ARENA_HEIGHT, CharybdisTable.ARENA_OUTER);
        return level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && isMinion(m) && me.equals(m.getPersistentData().getString(KEY_BOSS)));
    }

    // ================================================================ calling them
    /** Where the next megalodons will tear loose: on the wall ring, straddling the way to the target (none if the cap is reached). */
    static List<Vec3> wallSpots(CharybdisSpawnBoss boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel level)) return List.of();
        int count = CharybdisMath.minionsToSummon(alive(level, boss).size());
        if (count <= 0) return List.of();
        Vec3 c = boss.arenaCenter();
        double floor = c.y - CharybdisTable.ARENA_HOVER, towards = Math.atan2(target.getZ() - c.z, target.getX() - c.x);
        double y = Mth.clamp(target.getY(), floor + 3, floor + 14);
        List<Vec3> out = new ArrayList<>();
        for (double a : CharybdisMath.wallAngles(count, towards, 0.9)) {
            double[] p = CharybdisMath.onRing(c.x, c.z, a, CharybdisTable.ARENA_R - 3);
            out.add(new Vec3(p[0], y, p[1]));
        }
        return out;
    }

    /**
     * Summon Megalodons: the wall cracks where the wind-up marked (stone, bubbles, a rumble) and a moment later a megalodon swims out of each spot
     * and goes for the target. If the cap is reached nothing new comes, and the ones already out are sent at the target together.
     */
    static void summon(CharybdisSpawnBoss boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        List<Vec3> spots = boss.takeSpots();
        int room = CharybdisMath.minionsToSummon(alive(level, boss).size(), boss.gatesOpen());
        if (spots.size() > room) spots = spots.subList(0, room);
        if (spots.isEmpty() && room > 0) spots = wallSpots(boss, target);
        for (Mob m : alive(level, boss)) aim(m, target);
        if (spots.isEmpty()) {                                           // a full pack: it only calls them in
            level.playSound(null, boss.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0f, 0.8f);
            return;
        }
        boss.announce(CharybdisText.megalodons());
        for (Vec3 s : spots) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PRISMARINE_BRICKS.defaultBlockState()), s.x, s.y, s.z, 30, 0.6, 0.8, 0.6, 0.1);
            Fx.spray(level, FxThemes.BOSS_CHARYBDIS, FxOld.BUBBLE_COLUMN_UP, s.x, s.y, s.z, 20, 0.7, 1.0, 0.7, 0.1);
            level.playSound(null, BlockPos.containing(s), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
            final Vec3 at = s;
            CharybdisVfx.summonStrike(boss, s);
            boss.later(14, CharybdisMoves.safe("megalodon", () -> {
                if (!boss.isAlive() || alive(level, boss).size() >= CharybdisTable.MINION_CAP + (boss.gatesOpen() ? 1 : 0)) return;
                LivingEntity tg = boss.getTarget();
                spawnAt(level, boss, at, tg != null && tg.isAlive() ? tg : target);
            }));
        }
    }

    /** Makes one megalodon at a free spot at (or near) 'spot'; null if the creature is not there or there is no room. */
    static Mob spawnAt(ServerLevel level, CharybdisSpawnBoss boss, Vec3 spot, LivingEntity target) {
        EntityType<?> type = Wildlife.typeOf(MEGALODON);
        if (type == null) {
            if (WARNED.add("absent")) LOG.warn("[nusmp] There is no tensura:megalodon, so the Charybdis Spawn calls none");
            return null;
        }
        Vec3 c = boss.arenaCenter();
        double base = Math.atan2(spot.z - c.z, spot.x - c.x), r = Math.hypot(spot.x - c.x, spot.z - c.z);
        for (int k = 0; k < RING_OFFSET.length; k++) {
            double[] p = CharybdisMath.onRing(c.x, c.z, base + RING_OFFSET[k], r);
            Mob m = spawnType(level, type, BlockPos.containing(p[0], spot.y + HEIGHT_OFFSET[k], p[1]));
            if (m == null) continue;
            m.addTag(CharybdisTable.TAG_MINION);
            m.setGlowingTag(true);
            m.getPersistentData().putString(KEY_BOSS, boss.getUUID().toString());
            aim(m, target);
            Fx.spray(level, FxThemes.BOSS_CHARYBDIS, FxOld.BUBBLE_COLUMN_UP, m.getX(), m.getY() + 0.5, m.getZ(), 16, 0.6, 0.6, 0.6, 0.1);
            return m;
        }
        return null;
    }

    /**
     * The one way a megalodon is made: create, place, finalize (as an event spawn, so no spawn hook meddles), mark it as staying, add. Null if the
     * type is not a mob, there is no room for it, or the mob's own setup fails (logged once per kind of failure).
     */
    private static Mob spawnType(ServerLevel level, EntityType<?> type, BlockPos at) {
        try {
            Entity made = type.create(level);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            if (!level.noCollision(m.getBoundingBox())) {
                m.discard();
                return null;
            }
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            level.addFreshEntity(m);
            return m;
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.add(Wildlife.idOf(type) + ":" + ex.getClass().getName())) LOG.warn("[nusmp] Could not spawn {}", Wildlife.idOf(type), ex);
            return null;
        }
    }

    // ================================================================ driving them
    private static void aim(Mob m, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        try {
            if (m.getTarget() != target) {
                m.setTarget(target);
                m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    /** About every fifth tick while the boss lives: each megalodon takes the nearest foe, swims to it and bites it when in reach. */
    static void drive(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        List<Mob> mine = alive(level, boss);
        if (mine.isEmpty()) {
            NEXT_STRIKE.clear();
            return;
        }
        List<LivingEntity> foes = boss.foes(SIGHT);
        long now = level.getGameTime();
        Set<UUID> keep = new HashSet<>();
        for (Mob m : mine) {
            keep.add(m.getUUID());
            LivingEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (LivingEntity f : foes) {
                double d = m.distanceToSqr(f);
                if (d < bestD) {
                    bestD = d;
                    best = f;
                }
            }
            if (best == null) continue;
            try {
                aim(m, best);
                m.getNavigation().moveTo(best, 1.15);
                strike(m, best, now);
            } catch (RuntimeException | LinkageError ignored) {
                // as above
            }
        }
        NEXT_STRIKE.keySet().retainAll(keep);
    }

    /** One bite every two seconds when in reach, unless the creature's own brain has just done so. */
    private static void strike(Mob m, LivingEntity target, long now) {
        if (now < NEXT_STRIKE.getOrDefault(m.getUUID(), 0L)) return;
        if (!m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(target.getBoundingBox())) return;
        NEXT_STRIKE.put(m.getUUID(), now + STRIKE_EVERY);
        if (target.getLastHurtByMob() == m && target.tickCount - target.getLastHurtByMobTimestamp() < 30) return;
        m.swing(InteractionHand.MAIN_HAND);
        m.doHurtTarget(target);
    }

    // ================================================================ sending them away
    private static void poof(ServerLevel level, Entity m) {
        Fx.spray(level, FxThemes.BOSS_CHARYBDIS, FxOld.BUBBLE_COLUMN_UP, m.getX(), m.getY() + 0.6, m.getZ(), 20, 0.6, 0.6, 0.6, 0.1);
        Fx.spray(level, FxThemes.BOSS_CHARYBDIS, FxOld.SQUID_INK, m.getX(), m.getY() + 0.6, m.getZ(), 10, 0.5, 0.5, 0.5, 0.03);
    }

    /** The boss is gone (it fell, or its fight was abandoned): its megalodons go with it. */
    static void dismiss(ServerLevel level, CharybdisSpawnBoss boss) {
        for (Mob m : alive(level, boss)) {
            poof(level, m);
            m.discard();
        }
        NEXT_STRIKE.clear();
    }

    // ================================================================ events
    /**
     * Loot-free: the megalodons leave nothing (the event is cancelled, so the Dungeon's own drop handler never sees it: no carcass, no stones to
     * farm). The leviathan's own drops are cleared but the event lives on, so the Dungeon's drop handler after this one still gives its stones.
     */
    public static void onDrops(LivingDropsEvent e) {
        Entity v = e.getEntity();
        if (isMinion(v)) e.setCanceled(true);
        else if (isBoss(v)) e.getDrops().clear();
    }

    /** A megalodon's blow on a player takes at most a share of the player's health: its strength is large, but no single bite kills. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer victim) || !isMinion(e.getSource().getEntity())) return;
            e.setAmount(CharybdisTable.capMinion(e.getAmount(), victim.getMaxHealth()));
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.add("cap")) LOG.warn("[nusmp] Could not cap the blow of a megalodon (this is logged once)", ex);
        }
    }

    /** Used by the boss's alliance test: the megalodons are its own. */
    static boolean isFriend(Entity other) { return isMinion(other); }
}
