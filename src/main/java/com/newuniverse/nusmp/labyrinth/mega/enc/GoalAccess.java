package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.lang.reflect.Field;

/**
 * Why: a mob's goal list is a protected field of {@code Mob}, and the mod has no access transformer (the build files are not this feature's to
 * edit), so adding a goal from outside means reading the field by reflection under its official name, the way the Paladin code already does it.
 * Everything falls back cleanly if that ever fails: {@link #add} answers false, the wandering boss is then driven by the Director's own pass
 * alone (which walks it, aims it and releases it every half second without any goal), and nothing else changes. A goal that is already installed
 * is never installed twice (a mob that re-joins the world keeps its goals).
 */
final class GoalAccess {
    private GoalAccess() {}

    private static final Field GOALS = field("goalSelector");

    private static Field field(String name) {
        try {
            Field f = Mob.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    private static GoalSelector goals(Mob m) {
        if (GOALS == null) return null;
        try {
            return (GoalSelector) GOALS.get(m);
        } catch (ReflectiveOperationException | RuntimeException bad) {
            return null;
        }
    }

    /** Does the mob have a goal of this class? */
    static boolean has(Mob m, Class<? extends Goal> type) {
        GoalSelector g = goals(m);
        if (g == null) return false;
        for (WrappedGoal w : g.getAvailableGoals()) if (type.isInstance(w.getGoal())) return true;
        return false;
    }

    /** Adds the goal unless the mob already has one of its class. True when the mob has it afterwards. */
    static boolean add(Mob m, int priority, Goal goal) {
        GoalSelector g = goals(m);
        if (g == null) return false;
        if (has(m, goal.getClass())) return true;
        g.addGoal(priority, goal);
        return true;
    }
}
