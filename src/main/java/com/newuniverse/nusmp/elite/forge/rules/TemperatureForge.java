package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the Temperature Forge minigame, "keep a heat gauge in the band". The gauge (0..1000) starts at 300 and rises 22 per tick while the player holds the mouse
 * button (the bellows) and falls 14 per tick while it is released. A band, a window of half width {@code 100 - 9 x difficulty}, drifts up and down around 600 on a
 * seeded slow sine ({@code 140} either way) and the player keeps the gauge inside it. One sample of the button per game tick (50 ms): the input is
 * {@code samples(d)} integers, 0 or 1. The first 20 ticks are a warm-up and score nothing; after that a tick scores 1 inside the band and falls linearly to 0 at 160
 * outside it; the score is the mean over the scored ticks. A button cannot flip faster than every 2 ticks (100 ms): a flip sooner than that after the previous
 * one is ignored (the physical limit is applied in the simulation, shared by the client and the server, so an honest twitch is not punished and a bot cannot flip
 * every tick). The step lasts {@code 9000 + 1000 x difficulty} ms and holds exactly one sample per tick, so it claims its whole duration. No Minecraft classes.
 */
public final class TemperatureForge implements Minigame {
    public static final int TICK_MS = 50, WARMUP = 20, MIN_RUN = 2;
    public static final double HEAT = 22.0, COOL = 14.0, START = 300.0, FALLOFF = 160.0;

    @Override public MinigameType type() { return MinigameType.TEMPERATURE_FORGE; }

    public static int samples(int d) { return (9000 + 1000 * Minigame.clampDifficulty(d)) / TICK_MS; }

    public static double halfBand(int d) { return 100.0 - 9.0 * Minigame.clampDifficulty(d); }

    /** The middle of the band in tick {@code k}. */
    public static double center(long seed, int k) {
        double period = 100.0 + 100.0 * ForgeRng.unit(seed, 30, 0);
        double phase = 2.0 * Math.PI * ForgeRng.unit(seed, 31, 0);
        return 600.0 + 140.0 * StrictMath.sin(2.0 * Math.PI * k / period + phase);
    }

    /** The running state of one play. */
    public static final class Run {
        private final long seed;
        private final int d;
        private int k, state, since = MIN_RUN;
        private double h = START, total = 0.0;
        private int counted;

        public Run(long seed, int d) {
            this.seed = seed;
            this.d = Minigame.clampDifficulty(d);
        }

        public Run copy() {
            Run r = new Run(seed, d);
            r.k = k;
            r.state = state;
            r.since = since;
            r.h = h;
            r.total = total;
            r.counted = counted;
            return r;
        }

        /** Advances one tick with the button sample (0 or 1) and returns the gauge. */
        public double step(int pressed) {
            int want = pressed != 0 ? 1 : 0;
            if (want != state && since >= MIN_RUN) {
                state = want;
                since = 0;
            } else {
                since++;
            }
            h = Math.max(0.0, Math.min(1000.0, h + (state == 1 ? HEAT : -COOL)));
            if (k >= WARMUP) {
                double out = Math.max(0.0, Math.abs(h - center(seed, k)) - halfBand(d));
                total += Math.max(0.0, 1.0 - out / FALLOFF);
                counted++;
            }
            k++;
            return h;
        }

        public double gauge() { return h; }

        public boolean heating() { return state == 1; }

        public int ticks() { return k; }

        /** The score so far over the scored ticks (the warm-up does not count), 0..1000. */
        public double score() { return counted == 0 ? 0.0 : Minigame.MAX_SCORE * total / counted; }
    }

    @Override public int limitMillis(int d) { return samples(d) * TICK_MS; }

    @Override public int maxInputLength(int d) { return samples(d); }

    @Override
    public String validate(long seed, int d, int[] input) {
        if (input == null) return "no input";
        if (input.length != samples(d)) return "wrong number of samples";
        for (int c : input) if (c != 0 && c != 1) return "a sample that is not 0 or 1";
        return null;
    }

    @Override
    public int score(long seed, int d, int[] input) {
        if (validate(seed, d, input) != null) return 0;
        Run r = new Run(seed, d);
        for (int c : input) r.step(c);
        return Math.max(0, Math.min(Minigame.MAX_SCORE, (int) Math.round(r.score())));
    }

    @Override
    public long claimedMillis(long seed, int d, int[] input) { return input == null ? 0L : (long) input.length * TICK_MS; }

    @Override
    public int[] perfectInput(long seed, int d) {
        int n = samples(d);
        int[] out = new int[n];
        Run r = new Run(seed, d);
        for (int k = 0; k < n; k++) {
            Run on = r.copy(), off = r.copy();
            double c = center(seed, k);
            double hOn = on.step(1), hOff = off.step(0);
            int pick = Math.abs(hOn - c) < Math.abs(hOff - c) ? 1 : 0;
            out[k] = pick;
            r.step(pick);
        }
        return out;
    }
}
