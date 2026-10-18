package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the Rune Alignment minigame, "rotate and align rune rings". There are {@code 2 + (difficulty >= 3) + (difficulty >= 5)} rings, locked one after the other
 * from the inside. Each ring has a seeded target angle and turns by itself at a seeded speed ({@code 40 + 25 x difficulty + 15 x ring} tenths of a degree per second,
 * either way), and the player turns it further by dragging the mouse around the rings' centre. The angle that counts is the player's angle plus the drift the ring has
 * made at the moment it is locked (a right click), so the player has to LEAD the ring. Angles are in tenths of a degree (0..3599). The input is two integers per
 * ring: the player's angle and the lock time in ms since the step started (0 for a ring not locked). A ring scores 1 within 3 degrees of its target, falling linearly to
 * 0 at {@code 44 - 4 x difficulty} degrees; the score is the mean over the rings. Rings are locked in order, the first not before 600 ms and each at least 500 ms after the
 * previous, all inside {@code 6000 x rings + 4000} ms; an unlocked ring scores 0 and so does every ring after it. The lock times are the time the input claims. No Minecraft classes.
 */
public final class RuneAlignment implements Minigame {
    public static final int FIRST_MS = 600, GAP_MS = 500, FULL = 3600, PERFECT = 30;

    @Override public MinigameType type() { return MinigameType.RUNE_ALIGNMENT; }

    public static int rings(int d) {
        int n = Minigame.clampDifficulty(d);
        return 2 + (n >= 3 ? 1 : 0) + (n >= 5 ? 1 : 0);
    }

    /** The target angle of ring {@code i}, tenths of a degree. */
    public static int target(long seed, int i) { return (int) (FULL * ForgeRng.unit(seed, 40, i)) % FULL; }

    /** The ring's own speed, tenths of a degree per second (negative turns the other way). */
    public static int omega(long seed, int d, int i) {
        int speed = 40 + 25 * Minigame.clampDifficulty(d) + 15 * i;
        return ForgeRng.unit(seed, 41, i) < 0.5 ? -speed : speed;
    }

    /** Tenths of a degree the ring has drifted by after {@code ms}. */
    public static double drift(long seed, int d, int i, double ms) { return omega(seed, d, i) * ms / 1000.0; }

    /** The angle that counts for a ring: the player's angle plus the drift, folded into 0..3599. */
    public static int effective(long seed, int d, int i, int playerAngle, int lockMs) {
        double a = playerAngle + drift(seed, d, i, lockMs);
        a = a - FULL * Math.floor(a / FULL);
        return (int) Math.round(a) % FULL;
    }

    /** Smallest distance between two angles, tenths of a degree. */
    public static int distance(int a, int b) {
        int x = Math.floorMod(a - b, FULL);
        return Math.min(x, FULL - x);
    }

    /** The widest error that still scores, tenths of a degree. */
    public static int tolerance(int d) { return 10 * (44 - 4 * Minigame.clampDifficulty(d)); }

    public static double accuracy(int err, int d) {
        if (err <= PERFECT) return 1.0;
        int tol = tolerance(d);
        if (err >= tol) return 0.0;
        return 1.0 - (double) (err - PERFECT) / (tol - PERFECT);
    }

    @Override public int limitMillis(int d) { return 6000 * rings(d) + 4000; }

    @Override public int maxInputLength(int d) { return 2 * rings(d); }

    @Override
    public String validate(long seed, int d, int[] input) {
        if (input == null) return "no input";
        int n = rings(d);
        if (input.length != 2 * n) return "wrong number of values";
        int prev = 0;
        boolean ended = false;
        for (int i = 0; i < n; i++) {
            int angle = input[2 * i], ms = input[2 * i + 1];
            if (angle < 0 || angle >= FULL) return "an angle out of range";
            if (ms == 0) {
                ended = true;
                continue;
            }
            if (ended) return "a ring locked after an unlocked one";
            if (ms < (i == 0 ? FIRST_MS : prev + GAP_MS)) return "a ring locked too soon";
            if (ms > limitMillis(d)) return "a ring locked after the time limit";
            prev = ms;
        }
        return null;
    }

    @Override
    public int score(long seed, int d, int[] input) {
        if (validate(seed, d, input) != null) return 0;
        int n = rings(d);
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            int ms = input[2 * i + 1];
            if (ms == 0) break;
            sum += accuracy(distance(effective(seed, d, i, input[2 * i], ms), target(seed, i)), d);
        }
        return (int) Math.round(Minigame.MAX_SCORE * sum / n);
    }

    @Override
    public long claimedMillis(long seed, int d, int[] input) {
        if (input == null) return 0L;
        long max = 0L;
        for (int i = 1; i < input.length; i += 2) max = Math.max(max, input[i]);
        return max;
    }

    @Override
    public int[] perfectInput(long seed, int d) {
        int n = rings(d);
        int[] out = new int[2 * n];
        for (int i = 0; i < n; i++) {
            int ms = FIRST_MS + GAP_MS * i;
            out[2 * i + 1] = ms;
            out[2 * i] = Math.floorMod((int) Math.round(target(seed, i) - drift(seed, d, i, ms)), FULL);
        }
        return out;
    }
}
