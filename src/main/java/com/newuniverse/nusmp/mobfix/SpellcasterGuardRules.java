package com.newuniverse.nusmp.mobfix;

import java.util.List;

/**
 * Why: the vanilla Illusioner's blindness spell and the Evoker's fang spell read the target again at the moment they cast, and crash the server with a NullPointerException when it is gone (3.65.1). The
 * goal only re-checks its target on every second tick, so a spell of this mod that makes mobs forget their target ({@code setTarget(null)}) lands in that gap often enough to be seen in a dungeon. This
 * class is the pure part: which goals need a target, and when one may go on casting. No Minecraft type, so it is unit tested.
 */
public final class SpellcasterGuardRules {
    private SpellcasterGuardRules() {}

    /** The vanilla spell goals that dereference the target while casting (matched by the end of the class name: the classes are not public). */
    public static final List<String> NEED_A_TARGET = List.of("Illusioner$IllusionerBlindnessSpellGoal", "Evoker$EvokerAttackSpellGoal");

    public static boolean needsTarget(String className) {
        if (className == null) return false;
        for (String n : NEED_A_TARGET) if (className.endsWith(n)) return true;
        return false;
    }

    /** A spell goal that needs a target may start or go on only while the caster has a living one. */
    public static boolean mayCast(boolean hasTarget, boolean targetAlive) { return hasTarget && targetAlive; }
}
