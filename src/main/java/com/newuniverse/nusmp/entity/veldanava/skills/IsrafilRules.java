package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Kind;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Mode;

import java.util.List;

/**
 * Why: the numbers of Israfil, Lord of Trials (docs/veldanava_skills.md) in one place that names no Minecraft type, so the skill, the events, the unit tests and the CI self-test read the same
 * table. Ten modes like every Veldanava skill: four toggles (Spatial Domination, Trial of Endurance, Labyrinthian Mind, Weight of Consequence), five actives (Dimensional Labyrinth, Weight of
 * Sins, Spatial Collapse, Coordinate Shift, Rift Walk - held) and the mastery active The Grand Trumpet. Costs are in Magicule; Rift Walk's cost is per second held.
 */
public final class IsrafilRules {
    private IsrafilRules() {}

    public static final JudgmentRules.Spec ISRAFIL = new JudgmentRules.Spec("israfil", "Israfil", "Lord of Trials", List.of(
            new Mode(0, "spatial_domination", "Spatial Domination", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(1, "trial_of_endurance", "Trial of Endurance", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(2, "labyrinthian_mind", "Labyrinthian Mind", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(3, "weight_of_consequence", "Weight of Consequence", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(4, "dimensional_labyrinth", "Dimensional Labyrinth", Kind.PRESS, 6500, 4800, 25, 15, false),
            new Mode(5, "weight_of_sins", "Weight of Sins", Kind.CROUCH_PRESS, 4000, 2900, 15, 9, false),
            new Mode(6, "spatial_collapse", "Spatial Collapse", Kind.PRESS, 9500, 7200, 35, 22, false),
            new Mode(7, "coordinate_shift", "Coordinate Shift", Kind.CROUCH_PRESS, 3000, 1500, 10, 5, false),
            new Mode(8, "rift_walk", "Rift Walk", Kind.HELD, 1200, 800, 20, 12, true),
            new Mode(9, "grand_trumpet", "The Grand Trumpet", Kind.PRESS, 60000, 60000, 180, 180, false)));

    // ---------------------------------------------------------------- Spatial Domination (toggle 0)
    /** Movement speed bonus (a multiplier on the base speed), the longest and the shortest distance of the natural blink, the pause between two blinks (ticks), and how far a mastered body bends a blow. */
    public static final double SPEED_BONUS = 0.30, BLINK_MIN = 3.0, BLINK_MAX = 7.0, DEFLECT_RANGE = 4.0, DEFLECT_PHYSICAL = 0.5;
    public static final int BLINK_GAP_TICKS = 120;

    // ---------------------------------------------------------------- Trial of Endurance (toggle 1)
    /** The share of a blow that is stored, the most that can be stored, how fast it drains (per second), what each stored point is worth, the caps on the gifts, and the debt a blow puts on its source. */
    public static final double STORE_SHARE = 0.35, STORE_MAX = 120.0, STORE_DRAIN_PER_SECOND = 2.0, STORE_HP_PER_POINT = 0.5, STORE_HP_MAX = 40.0, STORE_TOUGHNESS_PER_POINT = 0.1, STORE_TOUGHNESS_MAX = 8.0;
    public static final int STORE_DEBT_MAX = 6;

    // ---------------------------------------------------------------- Weight of Consequence (toggle 3)
    public static final double WEIGHT_RADIUS = 20.0, WEIGHT_MIN_SPEED = 0.02;
    public static final int WEIGHT_PERIOD_TICKS = 20, WEIGHT_DEBT = 1, WEIGHT_SLOW_TICKS = 40, MAX_WEIGHT_TARGETS = 16;

    // ---------------------------------------------------------------- the actives
    /** Dimensional Labyrinth: the range to the target, the cage radius, how long it holds (ticks), the pulse (ticks), and the damage share of one pulse. */
    public static final double LABYRINTH_RANGE = 40.0, LABYRINTH_RADIUS = 4.5, LABYRINTH_PULSE_SHARE = 0.01;
    public static final int LABYRINTH_TICKS = 160, LABYRINTH_PULSE_TICKS = 20;
    /**
     * Dimensional Labyrinth as a pocket dimension (3.14.0): the maze is {@code cells} x {@code cells} (normal / mastered), a wall moves every {@code shift} ticks (normal / mastered) after a warning of
     * {@link #POCKET_WARN_TICKS}, a player stays {@code ticks} (a mob longer), the exit moves every {@link #POCKET_EXIT_EVERY} shifts, at most {@link #POCKET_SLOTS} mazes stand on a server, and a creature
     * that has left one cannot be pulled into another for {@link #POCKET_IMMUNE_TICKS}.
     */
    public static final int POCKET_CELLS = 9, POCKET_CELLS_MASTERED = 11, POCKET_SHIFT_TICKS = 60, POCKET_SHIFT_TICKS_MASTERED = 36, POCKET_WARN_TICKS = 16, POCKET_EXIT_EVERY = 3;
    public static final int POCKET_PLAYER_TICKS = 400, POCKET_MOB_TICKS = 600, POCKET_MASTERED_PERCENT = 150, POCKET_SLOTS = 8, POCKET_IMMUNE_TICKS = 600;
    public static final int POCKET_MEMBERS = 1, POCKET_MEMBERS_MASTERED = 3, POCKET_DEBT = 5;
    public static final double POCKET_GATHER_RADIUS = 6.0, POCKET_PULSE_SHARE = 0.01;

    public static int pocketCells(boolean mastered) { return mastered ? POCKET_CELLS_MASTERED : POCKET_CELLS; }

    public static int pocketShiftTicks(boolean mastered) { return mastered ? POCKET_SHIFT_TICKS_MASTERED : POCKET_SHIFT_TICKS; }

    public static int pocketMembers(boolean mastered) { return mastered ? POCKET_MEMBERS_MASTERED : POCKET_MEMBERS; }

    /** How long one creature stays in the maze: a player's time is shorter than a mob's, and mastery lengthens both by half. */
    public static int pocketTicks(boolean player, boolean mastered) {
        int base = player ? POCKET_PLAYER_TICKS : POCKET_MOB_TICKS;
        return mastered ? base * POCKET_MASTERED_PERCENT / 100 : base;
    }
    /** Weight of Sins: range, base damage, damage per stack of debt, per kill of the target (a player), the cap of that last part, and how long the crush lasts (ticks; "permanent" is ten minutes, a death clears it). */
    public static final double SINS_RANGE = 40.0, SINS_BASE_DAMAGE = 60.0, SINS_PER_DEBT = 8.0, SINS_PER_KILL = 0.4, SINS_KILL_CAP = 80.0;
    public static final int SINS_CRUSH_TICKS = 12000;
    /** Spatial Collapse: range, radius of the implosion, base damage, creatures hit, the pull strength, the Nihility Collapse field's radius and length (ticks) and the share of maximum health one pulse takes. */
    public static final double COLLAPSE_RANGE = 40.0, COLLAPSE_RADIUS = 6.5, COLLAPSE_DAMAGE = 140.0, COLLAPSE_PULL = 0.9, NIHILITY_RADIUS = 5.0, NIHILITY_PULSE_SHARE = 0.02;
    public static final int COLLAPSE_MAX_TARGETS = 16, NIHILITY_TICKS = 120, NIHILITY_PULSE_TICKS = 10;
    /** Coordinate Shift: the range to the target or the projectile, the most creatures that drop the owner's aggro. */
    public static final double SHIFT_RANGE = 48.0, SHIFT_PROJECTILE_RANGE = 32.0, SHIFT_AGGRO_RADIUS = 32.0;
    public static final int SHIFT_MAX_AGGRO = 24;
    /** Rift Walk: the longest hold in seconds (normal / mastered), the second at which the next cost is taken, and how long the body is checked after it. */
    public static final int RIFT_SECONDS = 4, RIFT_SECONDS_MASTERED = 8;
    /** The Grand Trumpet: the radius, the creatures it touches, how long Event Silence lasts (ticks), and the longest cooldown it sets on an enemy's skills (seconds). */
    public static final double TRUMPET_RADIUS = 100.0;
    public static final int TRUMPET_MAX_TARGETS = 48, TRUMPET_SILENCE_TICKS = 200, TRUMPET_COOLDOWN_SECONDS = 60, TRUMPET_MAX_MODES = 16;

    // ---------------------------------------------------------------- pure helpers
    /** The part of a blow that Trial of Endurance stores: a share of the blow, never above what still fits. */
    public static double stored(double blow, double alreadyStored) {
        double add = Math.max(0.0, blow) * STORE_SHARE;
        return Math.max(0.0, Math.min(STORE_MAX - Math.max(0.0, alreadyStored), add));
    }

    /** What the store has become after {@code seconds} of draining. */
    public static double drained(double store, double seconds) {
        return Math.max(0.0, store - STORE_DRAIN_PER_SECOND * Math.max(0.0, seconds));
    }

    /** The extra maximum health the store gives, in hit points. */
    public static double storeHealth(double store) {
        return Math.min(STORE_HP_MAX, Math.max(0.0, store) * STORE_HP_PER_POINT);
    }

    /** The extra armour toughness the store gives. */
    public static double storeToughness(double store) {
        return Math.min(STORE_TOUGHNESS_MAX, Math.max(0.0, store) * STORE_TOUGHNESS_PER_POINT);
    }

    /** The debt stacks a blow puts on the one who dealt it: one per ten points of damage, at least one when there was damage, at most {@link #STORE_DEBT_MAX}. */
    public static int debtOnAttacker(double blow) {
        if (blow <= 0.0) return 0;
        return (int) Math.max(1, Math.min(STORE_DEBT_MAX, Math.round(blow / 10.0)));
    }

    /** The damage of Weight of Sins: the base plus the debt and the kills of the target (the kills count for a player only; the caller passes 0 for a mob). */
    public static double sinsDamage(int debt, int kills) {
        double byKills = Math.min(SINS_KILL_CAP, Math.max(0, kills) * SINS_PER_KILL);
        return SINS_BASE_DAMAGE + Math.max(0, debt) * SINS_PER_DEBT + byKills;
    }

    /** The longest Rift Walk in seconds for the state of the skill. */
    public static int riftSeconds(boolean mastered) { return mastered ? RIFT_SECONDS_MASTERED : RIFT_SECONDS; }

    /** The Magicule a held mode costs for one second. */
    public static double riftCostPerSecond(boolean mastered, double scale) { return JudgmentRules.cost(ISRAFIL.mode(8), mastered, scale); }

    /** The cooldown (seconds) the Trumpet puts on an enemy skill: the longest of what it has and the cap. */
    public static int trumpetCooldown(int current) { return Math.max(TRUMPET_COOLDOWN_SECONDS, current); }

    /** True when a creature has a mind-altering effect: the id path of the effect names charm, fear, confusion and the like. */
    public static boolean mindEffect(String effectPath) {
        if (effectPath == null) return false;
        String p = effectPath.toLowerCase(java.util.Locale.ROOT);
        for (String key : MIND_WORDS) if (p.contains(key)) return true;
        return false;
    }

    private static final String[] MIND_WORDS = {"charm", "fear", "confus", "nausea", "hypno", "dominat", "mind", "control", "sleep", "terror", "madness", "taming", "possess"};
}
