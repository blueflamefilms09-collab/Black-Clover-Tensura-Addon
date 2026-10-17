package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Absolute Cancel is the answer to the owner's note that these fights must "transcend standard stat checks": a player's biggest spell is erased by
 * Merlin's open hand, and the party has to think instead of simply out-damaging her. To stay fair the rule is a small state machine with every
 * limit in the open, so it can be taught in one line and tested to the tick:
 * <ul>
 *   <li>It is a STANCE of a few seconds that she announces with a wind-up (the hand is drawn on the ground in a cone), not a passive.</li>
 *   <li>It erases a SPELL only: a blade, an arrow or a blast crosses it ("her hands cannot erase a sword").</li>
 *   <li>Only what comes at her FACE: an attacker outside the arc in front of her hand (65 degrees to each side, 85 at the Gates) is not reached.</li>
 *   <li>It has one charge (two at the Gates): the first spell is erased and DEVOURED (Gluttony: she keeps its power and sends part of it back), the stance
 *       then closes, so a cheap spell thrown first spends it.</li>
 *   <li>Anti-magic (a demon sword, the Anti-Magic Lord) breaks it: the hand that erases spells cannot erase that.</li>
 * </ul>
 * The class holds no game types: the glue tells it whether the blow was a spell, whether it was anti-magic, and the angle between her facing and the
 * attacker.
 */
public final class CancelStance {
    /** What the stance did to one blow. */
    public enum Result { CLOSED, NOT_A_SPELL, BEHIND, BROKEN, CANCELLED }

    /** The verdict: {@code erased} is the damage taken away (all of it when cancelled, else 0); {@code closedNow} is true when this blow closed the stance. */
    public record Verdict(Result result, float erased, boolean closedNow) {}

    public static final double HALF_ARC = Math.toRadians(65), HALF_ARC_GATES = Math.toRadians(85);
    /** The most spell power she can hold at once, and the share of it she sends back, and the least and most of that return (base damage). */
    public static final float DEVOUR_CAP = 60f, RETURN_SHARE = 0.55f, RETURN_MIN = 6f, RETURN_MAX = 26f;

    private long until = Long.MIN_VALUE;
    private int charges;
    private double halfArc = HALF_ARC;
    private float devoured;

    /** Opens the stance for {@code ticks} with {@code charges} spells it can erase and a front arc of {@code halfArc} radians to each side. */
    public void open(long now, int ticks, int charges, double halfArc) {
        this.until = now + Math.max(1, ticks);
        this.charges = Math.max(1, charges);
        this.halfArc = halfArc;
    }

    public boolean isOpen(long now) { return charges > 0 && now < until; }

    public int chargesLeft() { return charges; }

    public double halfArc() { return halfArc; }

    /** Ticks the stance has left (0 when closed). */
    public int ticksLeft(long now) { return isOpen(now) ? (int) (until - now) : 0; }

    /**
     * Judges one blow.
     *
     * @param spell       the blow is a spell (by the mod's definition)
     * @param antiMagic   the blow is anti-magic
     * @param offsetRad   the angle between the way her hand faces and the direction the blow came from, in radians (sign does not matter)
     * @param amount      the damage it would have done
     */
    public Verdict judge(long now, boolean spell, boolean antiMagic, double offsetRad, float amount) {
        if (!isOpen(now)) return new Verdict(Result.CLOSED, 0f, false);
        if (antiMagic) {
            close();
            return new Verdict(Result.BROKEN, 0f, true);
        }
        if (!spell) return new Verdict(Result.NOT_A_SPELL, 0f, false);
        if (Math.abs(offsetRad) > halfArc) return new Verdict(Result.BEHIND, 0f, false);
        float a = Float.isFinite(amount) && amount > 0f ? amount : 0f;
        devoured = Math.min(DEVOUR_CAP, devoured + a);
        charges--;
        boolean closed = charges <= 0;
        if (closed) until = Long.MIN_VALUE;
        return new Verdict(Result.CANCELLED, a, closed);
    }

    /** Closes the stance now (it keeps what it has devoured). */
    public void close() {
        charges = 0;
        until = Long.MIN_VALUE;
    }

    /** What she has devoured and not yet sent back. */
    public float devoured() { return devoured; }

    /** The base damage of the spell she sends back for {@code devoured} spell power: a share of it, never trivial, never huge (0 when nothing was devoured). */
    public static float returnBase(float devoured) {
        if (!(devoured > 0f)) return 0f;
        return Math.max(RETURN_MIN, Math.min(RETURN_MAX, devoured * RETURN_SHARE));
    }

    /** Takes the return: its base damage, and the devoured power is spent. */
    public float takeReturn() {
        float base = returnBase(devoured);
        devoured = 0f;
        return base;
    }

    /** A fresh fight. */
    public void reset() {
        close();
        devoured = 0f;
    }
}
