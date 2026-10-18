package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the Magicule Flow minigame, "steady a flow meter with the mouse". The meter's needle (0..1000, the middle 500 is the perfect flow) is pushed around by a
 * drift made of three slow sine waves with seeded periods and phases; the player holds the mouse above or below the middle of the meter to push the needle up or
 * down. One sample of the mouse is taken per game tick (50 ms), so the input is {@code samples(d)} integers 0..1000 (500 = no push). The simulation, shared by the client
 * (which draws {@link Run#x}) and the server (which re-scores): the push {@code u = (c - 500) / 500} is limited to change by 0.35 per tick (a hand cannot teleport),
 * and each tick {@code x += drift + 30 u}, kept inside 0..1000 (the drift peaks at {@code 14 + 3 x difficulty}, below the 30 the hand can push). A tick scores 1 when the
 * needle is within 30 of the middle, falling linearly to 0 at {@code 200 - 20 x difficulty} away, and the tick's points are squared (a needle at the edge of the band is
 * worth little); the score is the mean over the ticks. Doing nothing scores about 620 at difficulty 1 and 300 at difficulty 5. The step lasts {@code 8000 + 1000 x difficulty} ms and the input must hold exactly one sample per tick, so
 * the input claims the whole duration: a result can not arrive faster than the real time it takes to play. No Minecraft classes.
 */
public final class MagiculeFlow implements Minigame {
    public static final int TICK_MS = 50;
    public static final double AUTHORITY = 30.0, SLEW = 0.35, ZONE = 30.0;

    @Override public MinigameType type() { return MinigameType.MAGICULE_FLOW; }

    public static int samples(int d) { return (8000 + 1000 * Minigame.clampDifficulty(d)) / TICK_MS; }

    /** How far from the middle (needle units) a tick still scores something. */
    public static double band(int d) { return 200.0 - 20.0 * Minigame.clampDifficulty(d); }

    /** The push on the needle in tick {@code k} when the player does nothing. */
    public static double drift(long seed, int d, int k) {
        double amp = 14.0 + 3.0 * Minigame.clampDifficulty(d);
        double[] w = {0.5, 0.3, 0.2};
        double sum = 0.0;
        for (int j = 0; j < 3; j++) {
            double period = 30.0 + 40.0 * ForgeRng.unit(seed, 10 + j, 0);
            double phase = 2.0 * Math.PI * ForgeRng.unit(seed, 20 + j, 0);
            sum += w[j] * StrictMath.sin(2.0 * Math.PI * k / period + phase);
        }
        return amp * sum;
    }

    /** The running state of one play: the needle, the last push and the points so far. */
    public static final class Run {
        private final long seed;
        private final int d;
        private int k;
        private double x = 500.0, uPrev = 0.0, total = 0.0;

        public Run(long seed, int d) {
            this.seed = seed;
            this.d = Minigame.clampDifficulty(d);
        }

        /** Advances one tick with the mouse sample {@code c} (0..1000) and returns the needle. */
        public double step(int c) {
            double u = (Math.max(0, Math.min(1000, c)) - 500) / 500.0;
            u = Math.max(uPrev - SLEW, Math.min(uPrev + SLEW, u));
            x = Math.max(0.0, Math.min(1000.0, x + drift(seed, d, k) + AUTHORITY * u));
            double off = Math.abs(x - 500.0);
            double s = off <= ZONE ? 1.0 : Math.max(0.0, 1.0 - (off - ZONE) / (band(d) - ZONE));
            total += s * s;
            uPrev = u;
            k++;
            return x;
        }

        public double needle() { return x; }

        public int ticks() { return k; }

        /** The score so far over the ticks played, 0..1000. */
        public int score(int ofTicks) { return (int) Math.round(Minigame.MAX_SCORE * total / Math.max(1, ofTicks)); }
    }

    @Override public int limitMillis(int d) { return samples(d) * TICK_MS; }

    @Override public int maxInputLength(int d) { return samples(d); }

    @Override
    public String validate(long seed, int d, int[] input) {
        if (input == null) return "no input";
        if (input.length != samples(d)) return "wrong number of samples";
        for (int c : input) if (c < 0 || c > 1000) return "a sample out of range";
        return null;
    }

    @Override
    public int score(long seed, int d, int[] input) {
        if (validate(seed, d, input) != null) return 0;
        Run r = new Run(seed, d);
        for (int c : input) r.step(c);
        return Math.max(0, Math.min(Minigame.MAX_SCORE, r.score(samples(d))));
    }

    @Override
    public long claimedMillis(long seed, int d, int[] input) { return input == null ? 0L : (long) input.length * TICK_MS; }

    @Override
    public int[] perfectInput(long seed, int d) {
        int n = samples(d);
        int[] out = new int[n];
        Run r = new Run(seed, d);
        double uPrev = 0.0;
        for (int k = 0; k < n; k++) {
            double want = -drift(seed, d, k) / AUTHORITY;
            double u = Math.max(uPrev - SLEW, Math.min(uPrev + SLEW, Math.max(-1.0, Math.min(1.0, want + (500.0 - r.needle()) / 1000.0))));
            out[k] = (int) Math.round(500.0 + 500.0 * u);
            r.step(out[k]);
            uPrev = Math.max(-1.0, Math.min(1.0, (out[k] - 500) / 500.0));
        }
        return out;
    }
}
