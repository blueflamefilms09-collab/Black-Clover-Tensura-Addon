package com.newuniverse.nusmp.labyrinth.squad;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Why: a mob's goal lists are protected fields of {@code Mob} and the mod has no access transformer, so a goal can only be added from outside by reading the field by its
 * official name, the way the Dungeon Master's wanderers, the Paladins and the shadow minions already do. The squad goals are ADDED to whatever the mob already has (a vanilla
 * zombie, a Tensura monster, a Mob Foundry mob with its six style goals): nothing is removed and no entity type changes, so the mob keeps its own way of fighting and the
 * squad only adds what it lacks. Every squad goal is installed on every member (a role is not known when the mob is created, and a goal answers "no" for a role that is
 * not its own), at priorities just above the usual melee and wander goals so they win while they have something to say. A mob that already has the goals (it re-joined
 * the world) gets none twice. If the field cannot be read the answer is false and {@link SquadManager} steers that member itself from the same pure decisions.
 */
final class GoalInstaller {
    private GoalInstaller() {}

    private static final Field GOALS = field("goalSelector"), TARGETS = field("targetSelector");

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

    /** Adds the squad goals; true when the mob has them afterwards. */
    static boolean install(Mob mob, UUID squadId) {
        GoalSelector goals = read(GOALS, mob), targets = read(TARGETS, mob);
        if (goals == null || targets == null) return false;
        if (has(mob, InterceptAttackGoal.class)) return true;
        goals.addGoal(0, new MoraleFleeGoal(mob, squadId));
        goals.addGoal(1, new SupportRetreatGoal(mob, squadId));
        goals.addGoal(1, new InterceptAttackGoal(mob, squadId));
        goals.addGoal(1, new FlankTargetGoal(mob, squadId));
        goals.addGoal(4, new FormationFollowGoal(mob, squadId));
        targets.addGoal(1, new SquadFocusGoal(mob, squadId));
        return true;
    }

    /** Does the mob have a goal of this class in its goal list (or, for the focus goal, its target list)? */
    static boolean has(Mob mob, Class<? extends Goal> type) {
        GoalSelector g = read(type == SquadFocusGoal.class ? TARGETS : GOALS, mob);
        if (g == null) return false;
        for (WrappedGoal w : g.getAvailableGoals()) if (type.isInstance(w.getGoal())) return true;
        return false;
    }

    /** Can this build read the goal fields at all? (The self-test says so in its detail.) */
    static boolean available() { return GOALS != null && TARGETS != null; }
}
