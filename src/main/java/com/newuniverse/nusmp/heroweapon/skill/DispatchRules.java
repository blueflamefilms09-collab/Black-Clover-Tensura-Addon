package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;

/**
 * Why: the dispatcher's checks come in a fixed order, and the order is the player's experience of refusal (the weapon is not out, then no technique on the key,
 * then the cooldown, then the lock between two techniques, then a wind-up still running). Writing the order once, as a pure function of plain facts, makes it a
 * test table instead of a chain of ifs in game code, and the cooldown arithmetic (the level shortens it, but never below the floor the owner's balance budget
 * sets) lives with it.
 */
public final class DispatchRules {
    private DispatchRules() {}

    /** Why a technique was not cast, in the order the checks run; {@link #NONE} means go ahead. */
    public enum Refusal { NONE, NOT_SUMMONED, NOT_HELD, NO_SKILL, COOLDOWN, LOCK, BUSY }

    /**
     * The first check that fails. {@code summoned}: the weapon is out (or the player is exempt from the loan); {@code held}: it is in the main hand;
     * {@code hasSkill}: the active form lists a registered skill on this input; {@code cooldownLeft} and {@code lockLeft} in ticks; {@code windingUp}: the
     * player already has a technique in its wind-up.
     */
    public static Refusal check(boolean summoned, boolean held, boolean hasSkill, int cooldownLeft, int lockLeft, boolean windingUp) {
        if (!summoned) return Refusal.NOT_SUMMONED;
        if (!held) return Refusal.NOT_HELD;
        if (!hasSkill) return Refusal.NO_SKILL;
        if (windingUp) return Refusal.BUSY;
        if (cooldownLeft > 0) return Refusal.COOLDOWN;
        if (lockLeft > 0) return Refusal.LOCK;
        return Refusal.NONE;
    }

    /**
     * The cooldown a cast starts: the skill's own ticks shortened by the bond level (down to 0.75 at level 100), but never below {@code floor} (the config's
     * 20 ticks). A skill that asks for less than the floor gets the floor; a skill that asks for nothing at all (0) still gets the floor, because a technique
     * with no cooldown at all would be a way around the balance budget.
     */
    public static int cooldownTicks(int base, int level, int floor) {
        int scaled = (int) Math.round(Math.max(0, base) * HeroLevels.cooldownScale(level));
        return Math.max(Math.max(1, floor), scaled);
    }

    /** The creative-mode shortcut: a quarter of the cooldown, never below {@code floor} ticks (admins testing a weapon should not wait a minute per technique). */
    public static int creativeTicks(int ticks, int floor) { return Math.max(floor, ticks / 4); }
}
