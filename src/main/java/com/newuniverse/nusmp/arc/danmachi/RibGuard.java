package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: Udaeus's own defence in the story is its ribs: it moves them to block the blows aimed at it, but when the guard is in the wrong place a single crack
 * slows it. That is the fight's mechanic and it needs a counter the players can learn. This is the rule as a small pure state machine, so it can be tested
 * without a game and the boss class only feeds it the angle of each blow:
 * <ul>
 *   <li>The ribs face the side the last blow came from. A blow from inside the guard's arc ({@value #HALF_ARC_DEG} degrees to each side) is blocked and does
 *       30% of its damage. The first blow of a fight, and every blow while the ribs are still turning, lands in full.</li>
 *   <li>A blow from outside the arc while the guard has settled is a flank: it lands in full, cracks a rib, and the guard starts to turn to the new side
 *       (it takes {@link #reaction} ticks, shorter at higher power). A few cracks in a short time break the cage: Udaeus staggers, the magic stone behind
 *       the sternum shows, and everything hurts it for {@link #STAGGER_TICKS} ticks.</li>
 *   <li>So one fighter hammering from one side is blocked for ever; two fighters on opposite sides, or one who keeps circling, crack the cage in seconds.
 *       The Rib Cage Cry closes the guard completely for a while (a wider arc and 20% damage), and the cracks are forgotten after {@link #CRACK_MEMORY} ticks
 *       without a flank.</li>
 * </ul>
 */
public final class RibGuard {
    public static final double HALF_ARC_DEG = 65.0, CLOSED_HALF_ARC_DEG = 100.0;
    public static final float BLOCK_FACTOR = 0.30f, CLOSED_FACTOR = 0.20f, STAGGER_FACTOR = 1.6f;
    public static final int STAGGER_TICKS = 70, CRACK_MEMORY = 200;

    /** What a blow met. */
    public enum Verdict { OPEN, BLOCKED, FLANK, STAGGERED }

    /** The answer for one blow: what it met, the factor on its damage, whether it cracked a rib and whether that crack broke the cage. */
    public record Result(Verdict verdict, float factor, boolean cracked, boolean broke) {}

    private double guardYaw;
    private boolean aimed;
    private long settledAt, staggerUntil, closedUntil, lastCrackAt;
    private int cracks;

    /** Ticks the ribs need to turn to a new side: 14 at the opening, 2 less for every power step (6 at the Gates). */
    public static int reaction(int power) { return 14 - 2 * PowerCurve.stage(power); }

    /** Cracks that break the cage: 3, and 4 once the Gates are open (the ribs are tougher then). */
    public static int crackLimit(int power) { return PowerCurve.gatesOpen(power) ? 4 : 3; }

    public boolean staggered(long now) { return now < staggerUntil; }

    public boolean closed(long now) { return now < closedUntil; }

    public int cracks() { return cracks; }

    /** The side the ribs face (a yaw in degrees), meaningful once a blow has been seen. */
    public double guardYaw() { return guardYaw; }

    public boolean aimed() { return aimed; }

    public int staggerLeft(long now) { return (int) Math.max(0, staggerUntil - now); }

    /** The Rib Cage Cry: the guard is shut tight for {@code ticks} ticks. */
    public void close(long now, int ticks) { closedUntil = Math.max(closedUntil, now + Math.max(0, ticks)); }

    /**
     * A blow from an attacker standing at {@code attackerYaw} (the yaw of the direction from Udaeus to the attacker, in degrees).
     *
     * @param hasAttacker false for damage with no one behind it (fire, poison, the void): the ribs cannot block what they cannot see
     */
    public Result onHit(long now, double attackerYaw, int power, boolean hasAttacker) {
        if (!hasAttacker) return new Result(Verdict.OPEN, 1.0f, false, false);
        if (now < staggerUntil) return new Result(Verdict.STAGGERED, STAGGER_FACTOR, false, false);
        if (staggerUntil > 0) {                                  // the cage has mended: it begins again, facing nobody
            staggerUntil = 0;
            aimed = false;
            cracks = 0;
        }
        if (cracks > 0 && now - lastCrackAt > CRACK_MEMORY) cracks = 0;
        if (!aimed) {
            guardYaw = attackerYaw;
            aimed = true;
            settledAt = now + reaction(power);
            return new Result(Verdict.OPEN, 1.0f, false, false);
        }
        if (now < settledAt) return new Result(Verdict.OPEN, 1.0f, false, false);
        boolean shut = now < closedUntil;
        double arc = shut ? CLOSED_HALF_ARC_DEG : HALF_ARC_DEG;
        if (DmGeometry.yawDiffDeg(attackerYaw, guardYaw) <= arc) return new Result(Verdict.BLOCKED, shut ? CLOSED_FACTOR : BLOCK_FACTOR, false, false);
        cracks++;
        lastCrackAt = now;
        guardYaw = attackerYaw;
        settledAt = now + reaction(power);
        if (cracks >= crackLimit(power)) {
            cracks = 0;
            staggerUntil = now + STAGGER_TICKS;
            return new Result(Verdict.FLANK, 1.0f, true, true);
        }
        return new Result(Verdict.FLANK, 1.0f, true, false);
    }

    /** Forgets everything (a new fight). */
    public void reset() {
        aimed = false;
        guardYaw = 0;
        settledAt = 0;
        staggerUntil = 0;
        closedUntil = 0;
        lastCrackAt = 0;
        cracks = 0;
    }
}
