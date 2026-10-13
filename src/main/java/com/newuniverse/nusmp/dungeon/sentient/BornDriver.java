package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a mob born from a wall does with its life. The mobs of Tensura think with brains of their own, which are left alone; this only
 * makes sure a born mob goes for the nearest player (survival or adventure, alive, within 32 blocks, a Juggernaut within 64), walks to
 * them, and, if it is in reach and its own brain has not struck for two seconds, strikes once. A born mob that no player has come near for
 * three minutes is let go (its room stays populated in the Dungeon's memory and can be born again). Every born mob loaded in the Dungeon is
 * kept in one list (at most 600), looked at once a second (at most 40 a tick), and every look is inside a try block: a mob of an unexpected
 * shape costs a log line, never the server.
 */
public final class BornDriver {
    private BornDriver() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final int MAX_TRACKED = 600, STEER_PER_TICK = 40, SWEEP_EVERY = 100;
    private static final double STEER_SPEED = 1.1;
    /** Persistent data: the game time the mob may next make its fallback blow. */
    private static final String STRIKE_AT = "nusmp_strike_at";

    private static final class Tracked {
        final Mob mob;
        int alone;

        Tracked(Mob mob) { this.mob = mob; }
    }

    private static final List<Tracked> TRACKED = new ArrayList<>();
    private static final Set<Integer> IDS = new HashSet<>();
    /** The kinds whose attack threw: they are left to their own brains. */
    private static final Set<EntityType<?>> CANNOT_STRIKE = new HashSet<>();
    private static final Set<String> WARNED = new HashSet<>();

    // ---------------------------------------------------------------- who is kept
    /** Adds a born mob to the list (called by the wall births, and for born mobs that load from disk). A Juggernaut also gets its boss bar. */
    public static void track(Mob m) {
        if (m == null || TRACKED.size() >= MAX_TRACKED || !IDS.add(m.getId())) return;
        TRACKED.add(new Tracked(m));
        if (m.getTags().contains(SentientTags.JUGGERNAUT)) JuggernautSpawner.adopt(m);
    }

    /** A born mob that was saved with its chunk joins the world again: it is looked after again. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m) || !LivingDungeon.in(e.getLevel())) return;
        if (m.getTags().contains(SentientTags.BORN)) track(m);
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        forgetAll();
        CANNOT_STRIKE.clear();
        WARNED.clear();
        SentientDeath.clear();
    }

    private static void forgetAll() {
        TRACKED.clear();
        IDS.clear();
    }

    // ---------------------------------------------------------------- counting (the caps and the rooms)
    /** How many born mobs are alive within the radius of the point. */
    static int countNear(ServerLevel level, double x, double y, double z, double radius) {
        int n = 0;
        double r2 = radius * radius;
        for (Tracked t : TRACKED) {
            Mob m = t.mob;
            if (m.level() == level && m.isAlive() && !m.isRemoved() && m.distanceToSqr(x, y, z) <= r2) n++;
        }
        return n;
    }

    /** How many born mobs of this room are alive inside the room's box. */
    static int countInRoom(ServerLevel level, long room) {
        int n = 0;
        for (Tracked t : TRACKED) {
            Mob m = t.mob;
            if (m.level() != level || !m.isAlive() || m.isRemoved()) continue;
            if (m.getPersistentData().contains(SentientTags.ROOM) && m.getPersistentData().getLong(SentientTags.ROOM) == room && RoomKey.contains(room, m.getBlockX(), m.getBlockY(), m.getBlockZ())) n++;
        }
        return n;
    }

    /** How many born mobs are being looked after. */
    static int count() { return TRACKED.size(); }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        SentientDeath.reap(tick);                                            // corpses that did not go by themselves
        if (TRACKED.isEmpty()) return;
        ServerLevel d = server.getLevel(LivingDungeon.DIMENSION);
        if (d == null || !SentientMode.active()) {
            if (tick % 200 == 0) forgetAll();                                // switched off: nothing is steered
            return;
        }
        if (tick % SWEEP_EVERY == 53) sweep(d);
        steer(d, tick);
    }

    /** One look a second at each: the list is cut into twenty slices by position, one slice a tick. */
    private static void steer(ServerLevel d, int tick) {
        int n = TRACKED.size(), done = 0;
        long now = d.getGameTime();
        for (int i = tick % 20; i < n && done < STEER_PER_TICK; i += 20) {
            Mob m = TRACKED.get(i).mob;
            done++;
            try {
                steerOne(d, m, now);
            } catch (RuntimeException | LinkageError failure) {
                warn("steering", m.getType(), failure);
            }
        }
    }

    private static void steerOne(ServerLevel d, Mob m, long now) {
        if (!m.isAlive() || m.isRemoved() || m.level() != d) return;
        double sight = StrikeRules.sight(m.getTags().contains(SentientTags.JUGGERNAUT));
        ServerPlayer target = null;
        LivingEntity current = m.getTarget();
        if (current instanceof ServerPlayer sp && Targets.valid(sp) && sp.level() == d && StrikeRules.keep(true, m.distanceToSqr(sp), sight)) target = sp;
        if (target == null) target = Targets.nearest(d, m, sight);
        if (target == null) return;
        if (m.getTarget() != target) aim(m, target);
        m.getNavigation().moveTo(target, STEER_SPEED);
        strike(m, target, now);
    }

    /** Tells the mob's own brain (when it has the memory) and the plain target slot who it is after; a brain of another shape just ignores it. */
    private static void aim(Mob m, LivingEntity target) {
        try {
            m.setTarget(target);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } catch (RuntimeException | LinkageError ignored) {
            // the walk and the strike do not depend on it
        }
    }

    /** The fallback blow, once in two seconds when in reach, unless the mob's own brain has just struck. */
    private static void strike(Mob m, ServerPlayer target, long now) {
        if (CANNOT_STRIKE.contains(m.getType())) return;
        boolean inReach = m.getBoundingBox().inflate(StrikeRules.REACH_XZ, StrikeRules.REACH_Y, StrikeRules.REACH_XZ).intersects(target.getBoundingBox());
        boolean ownBrain = target.getLastHurtByMob() == m && target.tickCount - target.getLastHurtByMobTimestamp() < 30;
        boolean touching = m.getBoundingBox().inflate(StrikeRules.TOUCH).intersects(target.getBoundingBox());
        boolean both = m.isInWaterOrBubble() && target.isInWaterOrBubble();
        if (!StrikeRules.mayStrike(now, m.getPersistentData().getLong(STRIKE_AT), inReach, ownBrain, isWaterMob(m), both, touching)) return;
        m.getPersistentData().putLong(STRIKE_AT, StrikeRules.nextStrike(now));
        m.getLookControl().setLookAt(target, 60f, 60f);
        m.swing(InteractionHand.MAIN_HAND);
        try {
            m.doHurtTarget(target);
        } catch (RuntimeException | LinkageError failure) {
            CANNOT_STRIKE.add(m.getType());                                  // no attack damage, or an attack of another shape
            warn("striking with", m.getType(), failure);
        }
    }

    private static boolean isWaterMob(Mob m) {
        if (m.getTags().contains(SentientTags.AQUA) || m.getType().is(Wildlife.AQUATIC)) return true;
        return JuggernautPlan.needsWater(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath());
    }

    /** Every hundred ticks: the dead and the gone are forgotten, and a mob no player has come near for three minutes goes back into the walls. */
    private static void sweep(ServerLevel d) {
        List<Tracked> keep = new ArrayList<>(TRACKED.size());
        for (Tracked t : TRACKED) {
            Mob m = t.mob;
            if (!m.isAlive() || m.isRemoved() || m.level() != d) continue;
            t.alone = LonelyClock.next(t.alone, Targets.anyPlayerWithin(d, m, LonelyClock.NEAR), SWEEP_EVERY);
            if (LonelyClock.expired(t.alone)) {
                m.discard();
                continue;
            }
            keep.add(t);
        }
        TRACKED.clear();
        IDS.clear();
        for (Tracked t : keep) {
            TRACKED.add(t);
            IDS.add(t.mob.getId());
        }
    }

    private static void warn(String what, EntityType<?> type, Throwable failure) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        if (WARNED.add(what + id + failure.getClass().getName())) LOG.warn("[nusmp] Trouble {} {} (logged once)", what, id, failure);
    }
}
