package com.newuniverse.nusmp.arc.danmachi;

/**
 * Why: in the story the Black Goliath's hide shrugged off Mikoto's and Ouka's blades while the mages' combined spells did enormous damage, and Welf's and
 * Bell's heavy strikes were what turned the fight. That is the Black Hide: plain blows (a sword swing, an arrow) do half, anything else (a spell, a skill, a
 * technique) does full, and a heavy blow splits a plate of the hide; three plates split within six seconds break the hide for eight seconds, when everything
 * lands in full. The counter is learnt from the chat ("your blades glance off the black hide") and it rewards the mod's own heavy hitters, Argonaut's charge
 * above all. Pure, so the thresholds are tested; the boss class decides whether a blow was plain and feeds the state.
 */
public final class HideState {
    /** What a plain blow does while the hide holds. */
    public static final float PLAIN_FACTOR = 0.5f;
    /** A blow of this share of the boss's maximum health splits a plate. */
    public static final float PLATE_FRACTION = 0.015f;
    public static final int PLATES = 3, PLATE_WINDOW = 120, BROKEN_TICKS = 160;

    private final long[] plates = new long[PLATES];
    private long brokenUntil;

    public boolean broken(long now) { return now < brokenUntil; }

    public int brokenLeft(long now) { return (int) Math.max(0, brokenUntil - now); }

    /** Plates split within the window at {@code now}. */
    public int platesSplit(long now) {
        int n = 0;
        for (long t : plates) if (t > 0 && now + 1 - t <= PLATE_WINDOW) n++;
        return n;
    }

    /** The result of a blow: the factor on its damage and whether it split a plate or broke the hide. */
    public record Result(float factor, boolean splitPlate, boolean brokeHide) {}

    /**
     * A blow of {@code amount} landed on a boss of {@code maxHealth}. {@code plain} is true for a sword swing or an arrow, false for a spell, a skill or any
     * other kind of damage. A plain blow that is not heavy does half; a heavy one (and everything not plain) does full, and a heavy plain blow also splits
     * a plate.
     */
    public Result onHit(long now, boolean plain, float amount, float maxHealth) {
        if (broken(now) || !plain) return new Result(1.0f, false, false);
        boolean heavy = maxHealth > 0f && amount >= PLATE_FRACTION * maxHealth;
        if (!heavy) return new Result(PLAIN_FACTOR, false, false);
        for (int i = 0; i < plates.length - 1; i++) plates[i] = plates[i + 1];
        plates[plates.length - 1] = now + 1;                       // +1 so that tick 0 is not mistaken for "no plate yet"
        if (plates[0] > 0 && now + 1 - plates[0] <= PLATE_WINDOW) {
            brokenUntil = now + BROKEN_TICKS;
            java.util.Arrays.fill(plates, 0L);
            return new Result(1.0f, true, true);
        }
        return new Result(1.0f, true, false);
    }

    /** Puts the hide back (a new fight). */
    public void reset() {
        brokenUntil = 0;
        java.util.Arrays.fill(plates, 0L);
    }
}
