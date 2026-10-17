package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Ban cannot die, in the series, because he drank from the Fountain of Youth. A boss that cannot die is a boss that cannot be beaten, and the
 * boss rules of this mod allow no free healing, so the immortality has to be a RULE of the fight with a price and a counter: he has up to three
 * lives; when a life ends he does not die, he falls and lies still for a window while the Fountain works ({@link #windowTicks}), then rises with
 * the next life, a smaller reservoir of health each time ({@link #RESERVOIR}: 100%, 60%, 45% of his health), and the third life is the last. The
 * window is the party's moment too: anti-magic, or a holy, soul or severing blow, cuts the Fountain, and the life he rises with is only
 * {@link #CUT_FACTOR} as big. Every rise goes through the framework's one sanctioned second wind (the scripted refill of the Gates and of Lucius's
 * stages), once per life, never a regeneration.
 *
 * <p>The class also lays the three lives end to end on the framework's power ladder ({@link #powerFraction}): the power steps (20, 40, 60, 80) run
 * across the first two lives and the Gates open exactly when he rises for the last time, so the fight builds towards its final form instead of
 * reaching it in the first life. Pure: it is a counter of lives and a clock, the boss applies it.
 */
public final class FountainOfYouth {
    /** The most lives he has. */
    public static final int MAX_LIVES = 3;
    /** The size of each life's health as a share of his base maximum health. */
    private static final double[] RESERVOIR = {1.00, 0.60, 0.45};
    /** The size of the next life's reservoir when the Fountain was cut during the window. */
    public static final double CUT_FACTOR = 0.40;
    /** Ticks he lies still after life 1 and after life 2 (10 s and 8 s). */
    private static final int[] WINDOW = {200, 160};
    /** The virtual power bar: the last life occupies 0.24 down to 0 (so the Gates, under 0.25, open the moment it begins); the earlier lives share 1.0 down to 0.27. */
    private static final double FINAL_TOP = 0.24, EARLIER_BOTTOM = 0.27;

    private final int lives;
    private int life = 1;
    private double reservoir = 1.0;
    private boolean falling, cut;
    private long windowEnd;

    /** A fountain with {@code lives} lives (1 = no revival at all, 3 = the full design). */
    public FountainOfYouth(int lives) { this.lives = Math.max(1, Math.min(MAX_LIVES, lives)); }

    public int lives() { return lives; }

    /** The life he is in, 1-based. */
    public int life() { return life; }

    public boolean finalLife() { return life >= lives; }

    /** The size of the current life's health as a share of his base maximum health. */
    public double reservoir() { return reservoir; }

    public boolean falling() { return falling; }

    public boolean cut() { return cut; }

    public long windowEnd() { return windowEnd; }

    public int windowLeft(long now) { return falling ? (int) Math.max(0L, windowEnd - now) : 0; }

    /** Ticks he lies still after the life {@code ofLife} (1 or 2). */
    public static int windowTicks(int ofLife) { return WINDOW[Math.max(0, Math.min(WINDOW.length - 1, ofLife - 1))]; }

    /** Is there another life after this one? */
    public boolean canRevive() { return life < lives; }

    /**
     * A fatal blow reached him. True when it becomes a fall (the window begins), false when it is a real death: the last life, a kill command or the
     * void ({@code adminKill}), or he is already down.
     */
    public boolean fall(long now, boolean adminKill) {
        if (adminKill || falling || !canRevive()) return false;
        falling = true;
        cut = false;
        windowEnd = now + windowTicks(life);
        return true;
    }

    /** The Fountain was cut during the window (anti-magic or a holy, soul or severing blow). True the first time only. */
    public boolean cutNow() {
        if (!falling || cut) return false;
        cut = true;
        return true;
    }

    /** Is the window over (he may rise)? */
    public boolean windowOver(long now) { return falling && now >= windowEnd; }

    /** The reservoir he will rise with: the next life's share, {@link #CUT_FACTOR} of it if the Fountain was cut. */
    public double nextReservoir() { return RESERVOIR[Math.min(life, RESERVOIR.length - 1)] * (cut ? CUT_FACTOR : 1.0); }

    /** He rises: the next life begins with {@link #nextReservoir}. */
    public void rise() {
        if (!falling) return;
        reservoir = nextReservoir();
        life++;
        falling = false;
        cut = false;
    }

    /** Back to the first life (a new fight). */
    public void reset() {
        life = 1;
        reservoir = 1.0;
        falling = false;
        cut = false;
        windowEnd = 0;
    }

    // ---------------------------------------------------------------- the power ladder
    /**
     * Top and bottom of the virtual power bar for a life (1-based). With one life the bar is the ordinary one (1 to 0). Otherwise the last life is
     * 0.24 .. 0 (so the Gates, which open under 0.25, open the moment it begins) and the earlier lives share 1.0 .. 0.27 by the size of their reservoirs.
     */
    public double top(int ofLife) {
        if (lives == 1) return 1.0;
        if (ofLife >= lives) return FINAL_TOP;
        return 1.0 - (1.0 - EARLIER_BOTTOM) * share(ofLife - 1);
    }

    public double bottom(int ofLife) {
        if (lives == 1 || ofLife >= lives) return 0.0;
        return 1.0 - (1.0 - EARLIER_BOTTOM) * share(ofLife);
    }

    /** The share of the earlier lives' reservoirs that lives 1 .. {@code upTo} make up. */
    private double share(int upTo) {
        double used = 0, all = 0;
        for (int k = 1; k < lives; k++) {
            all += RESERVOIR[k - 1];
            if (k <= upTo) used += RESERVOIR[k - 1];
        }
        return all <= 0 ? 0 : used / all;
    }

    /**
     * The health fraction the framework's power curve is shown: this life's band, filled by the share of health left in it. While he lies down the
     * life counts as empty, while the refill after a rise runs it counts as full (so the power neither drops nor jumps in between).
     */
    public double powerFraction(double healthShare, boolean refilling) {
        double h = falling ? 0.0 : refilling ? 1.0 : Math.max(0.0, Math.min(1.0, healthShare));
        return bottom(life) + (top(life) - bottom(life)) * h;
    }

    // ---------------------------------------------------------------- what cuts the Fountain
    /** Does this blow cut the Fountain? Anti-magic, or a damage type whose id names holy, soul, severance or spirit (the same blows that cut a Soul Paladin's tether). */
    public static boolean cutsFountain(String damageTypePath, boolean antiMagic) {
        if (antiMagic) return true;
        if (damageTypePath == null) return false;
        String p = damageTypePath.toLowerCase(java.util.Locale.ROOT);
        return p.contains("holy") || p.contains("soul") || p.contains("sever") || p.contains("spirit");
    }

    // ---------------------------------------------------------------- save data
    /** Everything worth saving, as plain values. */
    public record Saved(int life, double reservoir, boolean falling, boolean cut, int windowLeft) {}

    public Saved save(long now) { return new Saved(life, reservoir, falling, cut, windowLeft(now)); }

    /** Restores a saved state (tolerates nonsense: a life outside 1..lives is clamped). */
    public void load(Saved s, long now) {
        reset();
        if (s == null) return;
        life = Math.max(1, Math.min(lives, s.life()));
        reservoir = Math.max(0.05, Math.min(1.0, s.reservoir()));
        falling = s.falling() && life < lives;
        cut = falling && s.cut();
        windowEnd = falling ? now + Math.max(0, s.windowLeft()) : 0;
    }
}
