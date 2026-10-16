package com.newuniverse.nusmp.boss.god;

import java.util.Map;

/**
 * Why: 1.45.0 made the god-tier Lucius a fight that only a fully maxed party could survive, and the owner asked for it to be "a little
 * balanced": about 10 to 15 minutes for two to four players, deaths from mistakes (telegraphs) and not from sums. The numbers that
 * decide that are scattered over a dozen pure classes, so the rules that are about <i>how hard</i> (the difficulty master, the cooldown
 * floors, the hit grace, the stage-change grace) are collected here, without Minecraft types and with a test, and the glue only asks.
 * <ul>
 *   <li><b>Difficulty</b> ({@code luciusDifficulty}, 0.5 easy, 1.0 normal, 1.5 hard): multiplies his damage numbers, and divides his
 *       cooldowns (a harder Lucius acts more often). Clamped, so a hand-edited config can never produce a NaN or a zero cooldown.</li>
 *   <li><b>Cooldown floors</b>: whatever the difficulty, a counter-mechanic never comes back faster than its floor (EMP 25 s,
 *       severance 30 s, the starvation pulse 12 s, the ultimate reversal 20 s, the spatial swap 20 s, the checkmate counter 14 s per
 *       target). The floors are what a "hard" setting cannot break.</li>
 *   <li><b>Hit grace</b>: after a true hit a player cannot be hit by another one for {@value #HIT_GRACE_TICKS} ticks (half a second),
 *       so two overlapping telegraphs never add up inside the same blink.</li>
 *   <li><b>Stage-change grace</b>: the aura, the EMP, the starvation drain and their effects are off for the invulnerable change and
 *       for {@value #STAGE_GRACE_TICKS} ticks (10 s) after it, so the players can heal and breathe before the next act.</li>
 * </ul>
 */
public final class LuciusBalance {
    private LuciusBalance() {}

    public static final double DIFFICULTY_MIN = 0.5, DIFFICULTY_MAX = 1.5, DIFFICULTY_DEFAULT = 0.8;
    public static final int HIT_GRACE_TICKS = 10, STAGE_GRACE_TICKS = 200;
    /** The shared pressure cap of the god tier: 18% of a player's maximum health a second (the 1.42.0 fight keeps 30%). */
    public static final double GOD_PRESSURE_SHARE = 0.18, CLASSIC_PRESSURE_SHARE = 0.30;

    /** Floors in ticks by chain id (and the two counter ids): the least time between two uses at any difficulty. */
    private static final Map<String, Integer> FLOORS = Map.of(
            "relic_emp", 500,
            "severance_bind", 600,
            "starvation_pulse", 240,
            "checkmate_counter", 280,
            "ultimate_reversal", 400,
            "spatial_swap", 400);

    /** The difficulty clamped to its range (NaN and garbage become the default). */
    public static double clampDifficulty(double d) {
        if (Double.isNaN(d)) return DIFFICULTY_DEFAULT;
        return Math.max(DIFFICULTY_MIN, Math.min(DIFFICULTY_MAX, d));
    }

    /** The factor on his damage numbers: the difficulty itself. */
    public static double damageFactor(double difficulty) { return clampDifficulty(difficulty); }

    /** The floor in ticks of a cooldown id, 0 when it has none. */
    public static int floorOf(String id) { return id == null ? 0 : FLOORS.getOrDefault(id, 0); }

    /**
     * A cooldown at a difficulty: the base divided by the difficulty (harder = faster), never under the id's floor and never under the
     * base's own floor when the base is already shorter than the floor (then the floor is the base: a floor never lengthens by itself
     * a cooldown that nobody asked to be that long). A non-positive base stays 0.
     */
    public static int cooldown(String id, int baseTicks, double difficulty) {
        if (baseTicks <= 0) return 0;
        int scaled = (int) Math.round(baseTicks / clampDifficulty(difficulty));
        int floor = Math.min(floorOf(id), baseTicks);
        return Math.max(floor, scaled);
    }

    /** The cooldown of the counters whose base is itself the floor (the swap, the reversal, the checkmate counter per target). */
    public static int counterCooldown(String id, double difficulty) { return cooldown(id, floorOf(id), difficulty); }

    /** The pressure share: the god tier's 18% or the 1.42.0 fight's 30%. */
    public static double pressureShare(boolean godTier) { return godTier ? GOD_PRESSURE_SHARE : CLASSIC_PRESSURE_SHARE; }

    /** Is this tick inside the stage-change grace that started at {@code graceStart}? */
    public static boolean inStageGrace(long now, long graceUntil) { return now < graceUntil; }

    /** The tick the stage-change grace ends, for a change that began at {@code now} and whose invulnerable part lasts {@code refillTicks}. */
    public static long stageGraceUntil(long now, int refillTicks) { return now + Math.max(0, refillTicks) + STAGE_GRACE_TICKS; }
}
