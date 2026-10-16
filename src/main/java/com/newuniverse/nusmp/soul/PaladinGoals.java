package com.newuniverse.nusmp.soul;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: a goal-driven mob (a zombie, a spider, a skeleton) decides who to fight with its target goals, and those would keep sending it
 * after the caster. A Paladin must serve its owner, so its own target goals are taken out and two of ours go in: one that picks the
 * foe the owner struck or was struck by, one that keeps it near the owner. The goal lists of a {@link Mob} are protected and the mod
 * has no access transformer (and the owner's file list for this work does not include the build files), so they are read once by
 * reflection under their official names and everything falls back cleanly if that ever fails: the per-tick drive and the
 * target-change event in {@link Paladins} do the same job for any mob, goals or not. The goals that were removed are kept (weakly,
 * so a discarded mob frees them) so a released Paladin can get them back when the config says it turns hostile again.
 */
final class PaladinGoals {
    private PaladinGoals() {}

    /** Marks the goals this class adds, so they can be found and removed again. */
    interface Marker {}

    private record Removed(int priority, Goal goal) {}

    private static final Map<Mob, List<Removed>> REMOVED = new WeakHashMap<>();
    private static final Field TARGETS = field("targetSelector"), GOALS = field("goalSelector");

    private static Field field(String name) {
        try {
            Field f = Mob.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    static GoalSelector targets(Mob m) { return read(TARGETS, m); }

    static GoalSelector goals(Mob m) { return read(GOALS, m); }

    private static GoalSelector read(Field f, Mob m) {
        if (f == null) return null;
        try { return (GoalSelector) f.get(m); } catch (ReflectiveOperationException | RuntimeException bad) { return null; }
    }

    static boolean installed(Mob m) {
        GoalSelector g = goals(m);
        if (g == null) return false;
        for (WrappedGoal w : g.getAvailableGoals()) if (w.getGoal() instanceof Marker) return true;
        return false;
    }

    /** Takes the mob's own target goals out (remembering them) and puts the two Paladin goals in. Safe to call twice. */
    static void install(Mob m) {
        GoalSelector t = targets(m), g = goals(m);
        if (t == null || g == null || installed(m)) return;
        List<Removed> kept = new ArrayList<>();
        for (WrappedGoal w : new ArrayList<>(t.getAvailableGoals())) if (!(w.getGoal() instanceof Marker)) kept.add(new Removed(w.getPriority(), w.getGoal()));
        synchronized (REMOVED) { REMOVED.put(m, kept); }
        t.removeAllGoals(goal -> !(goal instanceof Marker));
        t.addGoal(1, new DefendOwnerGoal(m));
        g.addGoal(2, new FollowOwnerGoal(m));
    }

    /** Takes the two Paladin goals out; with 'restore' the mob's own target goals come back (the config's "released Paladins turn hostile"). */
    static void uninstall(Mob m, boolean restore) {
        GoalSelector t = targets(m), g = goals(m);
        if (t == null || g == null) return;
        t.removeAllGoals(goal -> goal instanceof Marker);
        g.removeAllGoals(goal -> goal instanceof Marker);
        List<Removed> kept;
        synchronized (REMOVED) { kept = REMOVED.remove(m); }
        if (restore && kept != null) for (Removed r : kept) t.addGoal(r.priority(), r.goal());
    }

    /** Does the mob already have a goal that attacks (melee, bow, crossbow, spit ...)? Then the per-tick drive leaves the blows to it. */
    static boolean hasAttackGoal(Mob m) {
        GoalSelector g = goals(m);
        if (g == null) return false;
        for (WrappedGoal w : g.getAvailableGoals()) {
            String n = w.getGoal().getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (n.contains("attack") || n.contains("shoot") || n.contains("spit")) return true;
        }
        return false;
    }

    /** Picks the foe the owner fought lately (same decision as the drive). */
    static final class DefendOwnerGoal extends Goal implements Marker {
        private final Mob mob;
        private LivingEntity wanted;

        DefendOwnerGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount % 5 != 0) return false;
            ServerPlayer owner = Paladins.ownerPlayer(mob);
            if (owner == null) return false;
            wanted = Paladins.desiredTarget(mob, owner);
            return wanted != null && wanted != mob.getTarget();
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = Paladins.ownerPlayer(mob);
            LivingEntity t = mob.getTarget();
            return owner != null && t != null && Paladins.fair(mob, owner, t);
        }

        @Override
        public void start() { if (wanted != null) mob.setTarget(wanted); }

        @Override
        public void stop() { wanted = null; }
    }

    /** Keeps the Paladin near its owner when it has nothing to fight: walks back from 8 blocks, always from 24. */
    static final class FollowOwnerGoal extends Goal implements Marker {
        private final Mob mob;
        private ServerPlayer owner;

        FollowOwnerGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            owner = Paladins.ownerPlayer(mob);
            if (owner == null || owner.level() != mob.level()) return false;
            PaladinTargeting.Follow f = PaladinTargeting.follow(mob.distanceTo(owner), mob.getTarget() != null, true);
            return f == PaladinTargeting.Follow.WALK || f == PaladinTargeting.Follow.RETURN;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && owner.level() == mob.level() && mob.distanceTo(owner) > 3.5;
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(owner, 10f, mob.getMaxHeadXRot());
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(owner, 1.15);
        }

        @Override
        public void stop() { mob.getNavigation().stop(); owner = null; }
    }
}
