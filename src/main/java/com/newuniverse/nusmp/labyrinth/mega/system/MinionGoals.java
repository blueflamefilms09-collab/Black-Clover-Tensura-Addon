package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.lang.reflect.Field;
import java.util.EnumSet;

/**
 * Why: a shadow must serve its owner like a tamed wolf does: follow, defend the owner against whoever hurt them, and help against whoever the owner hits.
 * Vanilla has exactly those three goals ({@code FollowOwnerGoal}, {@code OwnerHurtByTargetGoal}, {@code OwnerHurtTargetGoal}) but they only fit a
 * {@code TamableAnimal}, and a shadow can be any monster, so the three are written again for any {@link Mob}, the way {@code soul/PaladinGoals} does it for
 * Paladins (that class is package-private, so the idea is reused, not the code): the mob's own target goals come OUT (a zombie must stop hunting its owner) and
 * the two owner-driven ones go in, plus the follow goal below the mob's own attack goals so an attack always wins over walking. The goal lists of a {@code Mob}
 * are protected and the mod has no access transformer, so they are read once by reflection under their official names; if that ever fails, {@link #install}
 * says so and the per-ten-ticks drive in {@link ShadowMinions} does the same decisions for any mob, goal-driven or brain-driven (Tensura's), exactly as the
 * Soul Corpse and Paladin drives do. The goals never throw into the game loop: every owner lookup tolerates an offline owner.
 */
final class MinionGoals {
    private MinionGoals() {}

    /** Marks the goals this class adds, so they are recognised (and not removed) the next time. */
    interface Marker {}

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

    private static GoalSelector read(Field f, Mob m) {
        if (f == null) return null;
        try {
            return (GoalSelector) f.get(m);
        } catch (ReflectiveOperationException | RuntimeException bad) {
            return null;
        }
    }

    static GoalSelector targets(Mob m) { return read(TARGETS, m); }

    static GoalSelector goals(Mob m) { return read(GOALS, m); }

    /** Are the goal lists readable on this runtime at all? */
    static boolean available() { return TARGETS != null && GOALS != null; }

    static boolean installed(Mob m) {
        GoalSelector g = goals(m);
        if (g == null) return false;
        for (WrappedGoal w : g.getAvailableGoals()) if (w.getGoal() instanceof Marker) return true;
        return false;
    }

    /** Does the mob already have a goal that attacks (melee, bow, crossbow, spit ...)? Then the drive leaves the blows to it. */
    static boolean hasAttackGoal(Mob m) {
        GoalSelector g = goals(m);
        if (g == null) return false;
        for (WrappedGoal w : g.getAvailableGoals()) {
            String n = w.getGoal().getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (n.contains("attack") || n.contains("shoot") || n.contains("spit")) return true;
        }
        return false;
    }

    /** Takes the mob's own target goals out and puts the owner goals in. Safe to call twice. Returns false when the lists cannot be read (the drive then does the work). */
    static boolean install(Mob m) {
        GoalSelector t = targets(m), g = goals(m);
        if (t == null || g == null) return false;
        if (installed(m)) return true;
        t.removeAllGoals(goal -> !(goal instanceof Marker));
        t.addGoal(1, new OwnerHurtByGoal(m));
        t.addGoal(2, new OwnerHurtGoal(m));
        g.addGoal(3, new FollowGoal(m));
        return true;
    }

    /** Defends the owner: goes for whoever hurt them lately. */
    static final class OwnerHurtByGoal extends Goal implements Marker {
        private final Mob mob;
        private LivingEntity wanted;

        OwnerHurtByGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount % 5 != 0) return false;
            ServerPlayer owner = ShadowMinions.ownerPlayer(mob);
            if (owner == null) return false;
            LivingEntity by = owner.getLastHurtByMob();
            if (by == null || owner.tickCount - owner.getLastHurtByMobTimestamp() > 200 || by == mob.getTarget() || !ShadowMinions.fair(mob, owner, by)) return false;
            wanted = by;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = ShadowMinions.ownerPlayer(mob);
            LivingEntity t = mob.getTarget();
            return owner != null && t != null && ShadowMinions.fair(mob, owner, t);
        }

        @Override
        public void start() { if (wanted != null) mob.setTarget(wanted); }

        @Override
        public void stop() { wanted = null; }
    }

    /** Helps the owner: goes for whoever they hit lately. */
    static final class OwnerHurtGoal extends Goal implements Marker {
        private final Mob mob;
        private LivingEntity wanted;

        OwnerHurtGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount % 5 != 0) return false;
            ServerPlayer owner = ShadowMinions.ownerPlayer(mob);
            if (owner == null) return false;
            LivingEntity hit = owner.getLastHurtMob();
            if (hit == null || owner.tickCount - owner.getLastHurtMobTimestamp() > 200 || hit == mob.getTarget() || !ShadowMinions.fair(mob, owner, hit)) return false;
            wanted = hit;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = ShadowMinions.ownerPlayer(mob);
            LivingEntity t = mob.getTarget();
            return owner != null && t != null && ShadowMinions.fair(mob, owner, t);
        }

        @Override
        public void start() { if (wanted != null) mob.setTarget(wanted); }

        @Override
        public void stop() { wanted = null; }
    }

    /** Keeps the shadow near its owner when it has nothing to fight: walks back from 7 blocks, stops at 3. */
    static final class FollowGoal extends Goal implements Marker {
        private final Mob mob;
        private ServerPlayer owner;

        FollowGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            owner = ShadowMinions.ownerPlayer(mob);
            if (owner == null || !owner.isAlive() || owner.level() != mob.level() || mob.getTarget() != null) return false;
            return mob.distanceToSqr(owner) > 49.0;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && owner.level() == mob.level() && mob.getTarget() == null && mob.distanceToSqr(owner) > 9.0;
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(owner, 10f, mob.getMaxHeadXRot());
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(owner, 1.15);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            owner = null;
        }
    }
}
