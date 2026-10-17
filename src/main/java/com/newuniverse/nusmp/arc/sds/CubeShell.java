package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Perfect Cube is Merlin's wall, and a wall that reflects everything for as long as she likes would be a stat check turned inside out. So it has a
 * price a party can pay and three ways round it, all stated here:
 * <ul>
 *   <li>It reflects SPELLS and SHOTS (anything that crosses the cube from beyond arm's reach), a fixed number of times (three, four at the Gates), then it shatters
 *       and leaves her staggered. Several blows in one instant (a multi-hit spell) cost ONE reflection, so a combo cannot empty it by accident.</li>
 *   <li>Blows of an arm or a blade cross it, dulled to 60% ("the cube denies the entry of everyone but her" is a push in the glue, not a wall of armour).</li>
 *   <li>Anti-magic breaks it outright, and a hit of it still lands in full.</li>
 * </ul>
 * The reflection is the base damage of a spell of her own sent back at the thrower: a share of what was thrown, never trivial and never more than a fixed
 * cap, so a thrower is stung in proportion without being one-shot by their own power. Pure: the glue says what the blow was.
 */
public final class CubeShell {
    public enum Result { CLOSED, DULLED, REFLECTED, BROKEN_BY_ANTI_MAGIC }

    /**
     * @param result      what happened
     * @param passed      the damage that still reaches her (all of it when closed or broken, 60% when dulled, 0 when reflected)
     * @param reflectBase the base damage of the spell sent back (0 unless this blow paid a reflection)
     * @param shattered   true when this blow ended the cube (the last charge, or anti-magic)
     */
    public record Verdict(Result result, float passed, float reflectBase, boolean shattered) {}

    public static final float DULL = 0.6f, REFLECT_SHARE = 0.35f, REFLECT_MIN = 3f, REFLECT_MAX = 14f;
    /** Blows within this many ticks of a paid reflection are reflected for nothing. */
    public static final int COMBO_TICKS = 8;

    private long until = Long.MIN_VALUE, lastPaid = Long.MIN_VALUE;
    private int charges;

    public void open(long now, int ticks, int charges) {
        this.until = now + Math.max(1, ticks);
        this.charges = Math.max(1, charges);
        this.lastPaid = Long.MIN_VALUE;
    }

    public boolean isOpen(long now) { return charges > 0 && now < until; }

    public int chargesLeft() { return charges; }

    public int ticksLeft(long now) { return isOpen(now) ? (int) (until - now) : 0; }

    /**
     * Judges one blow.
     *
     * @param spell     the blow is a spell
     * @param ranged    the blow came from beyond arm's reach (a projectile, or an attacker further than melee)
     * @param antiMagic the blow is anti-magic
     */
    public Verdict judge(long now, boolean spell, boolean ranged, boolean antiMagic, float amount) {
        if (!isOpen(now)) return new Verdict(Result.CLOSED, amount, 0f, false);
        if (antiMagic) {
            close();
            return new Verdict(Result.BROKEN_BY_ANTI_MAGIC, amount, 0f, true);
        }
        if (!spell && !ranged) return new Verdict(Result.DULLED, amount * DULL, 0f, false);
        if (lastPaid != Long.MIN_VALUE && now - lastPaid < COMBO_TICKS) return new Verdict(Result.REFLECTED, 0f, 0f, false);
        lastPaid = now;
        charges--;
        boolean ends = charges <= 0;
        if (ends) until = Long.MIN_VALUE;
        return new Verdict(Result.REFLECTED, 0f, reflectBase(amount), ends);
    }

    public void close() {
        charges = 0;
        until = Long.MIN_VALUE;
    }

    /** The base damage sent back for a blow of {@code amount}: a share of it between {@link #REFLECT_MIN} and {@link #REFLECT_MAX}. */
    public static float reflectBase(float amount) {
        float a = Float.isFinite(amount) && amount > 0f ? amount : 0f;
        return Math.max(REFLECT_MIN, Math.min(REFLECT_MAX, a * REFLECT_SHARE));
    }
}
