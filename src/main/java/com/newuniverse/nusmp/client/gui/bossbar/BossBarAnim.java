package com.newuniverse.nusmp.client.gui.bossbar;

import com.newuniverse.nusmp.boss.bar.BossBarColors;

/**
 * The animation state of one mod boss bar (pure: no Minecraft types, deterministic for a seed). The overlay feeds it once per game tick
 * with the bar's progress, phase and tint ({@link #update}), then asks for the numbers of the frame ({@link #frame}):
 * <ul>
 *   <li><b>Shake / glitch</b>: when the progress falls by more than {@link #DROP_TRIGGER} within {@link #WINDOW} ticks the bar shakes for
 *       {@link #SHAKE_TICKS} (a decaying sinusoid, at most {@link #MAX_SHAKE_PX} px on each axis), a white flash covers its first ticks and up to
 *       {@link #MAX_SLICES} horizontal slices of the fill are displaced by a few pixels, new ones every tick (seeded by the tick).</li>
 *   <li><b>Colour morph</b>: a new tint (a phase step) blends from the colour on screen to the new one over {@link #MORPH_TICKS}.</li>
 *   <li><b>Shatter</b>: a phase change splits the bar into {@link #COLUMNS} columns that fly apart and fall (seeded velocities) during the first
 *       half of {@link #SHATTER_TICKS} and come back together during the second half.</li>
 * </ul>
 * Nothing is allocated after construction. Times are game ticks; the frame time is a tick plus the partial tick.
 */
public final class BossBarAnim {
    public static final int SHAKE_TICKS = 12, MORPH_TICKS = 40, SHATTER_TICKS = 30, WINDOW = 10, COLUMNS = 12, MAX_SLICES = 4;
    public static final float DROP_TRIGGER = 0.04f, MAX_SHAKE_PX = 4f, FLASH_TICKS = 4f;
    /** Rows of the fill (the slices are rows of it). */
    public static final int FILL_ROWS = 7;

    private static final float GRAVITY = 0.18f, SPREAD = 0.35f, MAX_FALL_PX = 24f, MAX_ASIDE_PX = 40f;

    private final long seed;
    private final float[] history = new float[WINDOW + 1];
    private boolean started;
    private long lastTick;
    private float lastProgress;

    // phase and colour
    private int phase;
    private boolean rage;
    private int fromP, fromS, toP, toS;
    private long morphStart = Long.MIN_VALUE / 2;

    // events
    private long shakeStart = Long.MIN_VALUE / 2, shatterStart = Long.MIN_VALUE / 2;
    private float shakeAmp;
    private long glitchSeed, shatterSeed;

    // frame outputs
    private float shakeX, shakeY, flash, morph, shatter, shatterAlpha = 1f;
    private int primary, secondary, sliceCount;
    private final int[] sliceY = new int[MAX_SLICES], sliceH = new int[MAX_SLICES], sliceDx = new int[MAX_SLICES];
    private final float[] colDx = new float[COLUMNS], colDy = new float[COLUMNS];

    public BossBarAnim(long seed) {
        this.seed = seed;
    }

    // ================================================================ per tick

    /**
     * One game tick of input (a repeat of the same tick is ignored). The first call only sets the starting values: a bar that has just
     * appeared neither shakes nor morphs.
     */
    public void update(long tick, float progress, int phase, int primaryRgb, int secondaryRgb, boolean rage) {
        this.rage = rage;
        if (!started) {
            started = true;
            lastTick = tick;
            lastProgress = progress;
            java.util.Arrays.fill(history, progress);
            this.phase = phase;
            fromP = toP = primary = primaryRgb & 0xFFFFFF;
            fromS = toS = secondary = secondaryRgb & 0xFFFFFF;
            return;
        }
        if (tick <= lastTick) return;
        if (tick - lastTick > WINDOW) java.util.Arrays.fill(history, lastProgress);
        else for (long k = lastTick + 1; k < tick; k++) history[slot(k)] = lastProgress;
        history[slot(tick)] = progress;
        lastTick = tick;
        lastProgress = progress;

        float max = progress;
        for (float h : history) if (h > max) max = h;
        float drop = max - progress;
        if (drop > DROP_TRIGGER) {
            shakeStart = tick;
            shakeAmp = Math.max(1.5f, Math.min(MAX_SHAKE_PX, 1.5f + drop * 25f));
            glitchSeed = mix(seed ^ (tick * 0x9E3779B97F4A7C15L));
            java.util.Arrays.fill(history, progress);               // the next hit is measured from here
        }

        int np = primaryRgb & 0xFFFFFF, ns = secondaryRgb & 0xFFFFFF;
        boolean phaseChanged = phase != this.phase;
        if (phaseChanged || np != toP || ns != toS) {
            float k = morphK(tick);
            fromP = BossBarColors.lerp(fromP, toP, k);
            fromS = BossBarColors.lerp(fromS, toS, k);
            toP = np;
            toS = ns;
            morphStart = tick;
        }
        if (phaseChanged) {
            this.phase = phase;
            shatterStart = tick;
            shatterSeed = mix(seed ^ 0xD1B54A32D192ED03L ^ (tick * 0xC2B2AE3D27D4EB4FL));
        }
    }

    private static int slot(long tick) {
        return (int) Math.floorMod(tick, (long) (WINDOW + 1));
    }

    private float morphK(double time) {
        double k = (time - morphStart) / MORPH_TICKS;
        if (k <= 0) return 0f;
        if (k >= 1) return 1f;
        return (float) (k * k * (3 - 2 * k));
    }

    // ================================================================ per frame

    /** Works out the frame at 'time' (tick + partial tick). Every getter below is valid until the next call. */
    public void frame(double time) {
        // shake, flash, glitch slices
        double age = time - shakeStart;
        sliceCount = 0;
        if (age >= 0 && age < SHAKE_TICKS) {
            double decay = 1 - age / SHAKE_TICKS;
            decay *= decay;
            shakeX = (float) (shakeAmp * decay * Math.cos(age * 2.6));
            shakeY = (float) (shakeAmp * decay * Math.sin(age * 3.4 + 0.7));
            flash = age < FLASH_TICKS ? 0.75f * (float) (1 - age / FLASH_TICKS) : 0f;
            long tk = (long) Math.floor(time);
            long h = mix(glitchSeed + tk * 0x9E3779B97F4A7C15L);
            int n = 1 + (int) ((h >>> 8) & 3) / 2 + (shakeAmp > 3f ? 1 : 0);          // 1 .. 3 slices, a heavy hit may add one
            n = Math.min(MAX_SLICES, n);
            for (int i = 0; i < n; i++) {
                h = mix(h + 0xA0761D6478BD642FL);
                int y = (int) ((h >>> 4) % (FILL_ROWS - 1));
                int rows = 1 + (int) ((h >>> 20) % 3);
                int dx = 1 + (int) ((h >>> 32) % (1 + Math.round(2 + 6 * decay)));
                sliceY[i] = y;
                sliceH[i] = Math.min(rows, FILL_ROWS - y);
                sliceDx[i] = ((h >>> 50) & 1) == 0 ? dx : -dx;
            }
            sliceCount = n;
        } else {
            shakeX = shakeY = flash = 0f;
        }

        // colour morph
        morph = morphK(time);
        primary = BossBarColors.lerp(fromP, toP, morph);
        secondary = BossBarColors.lerp(fromS, toS, morph);

        // shatter and reform
        double s = (time - shatterStart) / SHATTER_TICKS;
        if (s >= 0 && s < 1) {
            shatter = (float) s;
            double apart = Math.min(s, 0.5) * SHATTER_TICKS;                       // ticks of flight, at most half the time
            double back = s < 0.5 ? 1.0 : 1.0 - ease((s - 0.5) * 2);               // 1 while apart, easing to 0 as it reassembles
            for (int i = 0; i < COLUMNS; i++) {
                long h = mix(shatterSeed + i * 0x9E3779B97F4A7C15L);
                float vx = (i - (COLUMNS - 1) / 2f) * SPREAD + (unit(h) - 0.5f) * 0.6f;
                float vy = -unit(mix(h)) * 1.2f;
                double dx = vx * apart, dy = vy * apart + 0.5 * GRAVITY * apart * apart;
                colDx[i] = (float) (Math.max(-MAX_ASIDE_PX, Math.min(MAX_ASIDE_PX, dx)) * back);
                colDy[i] = (float) (Math.max(-MAX_FALL_PX, Math.min(MAX_FALL_PX, dy)) * back);
            }
            shatterAlpha = (float) (1 - 0.45 * Math.sin(Math.PI * s));
        } else {
            shatter = 0f;
            shatterAlpha = 1f;
            for (int i = 0; i < COLUMNS; i++) colDx[i] = colDy[i] = 0f;
        }
    }

    private static double ease(double k) {
        return k * k * (3 - 2 * k);
    }

    /** SplitMix64 finaliser: the cheap deterministic hash behind every random number here. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static float unit(long h) {
        return (h >>> 40) / (float) (1L << 24);
    }

    // ================================================================ outputs of the last frame

    public float shakeX() { return shakeX; }
    public float shakeY() { return shakeY; }
    /** White flash strength 0 .. 0.75. */
    public float flash() { return flash; }
    public int sliceCount() { return sliceCount; }
    /** First row of slice i (0 .. FILL_ROWS-1). */
    public int sliceY(int i) { return sliceY[i]; }
    public int sliceH(int i) { return sliceH[i]; }
    /** Pixels the slice is moved sideways (negative = left). */
    public int sliceDx(int i) { return sliceDx[i]; }
    /** The colours on screen now (morphing), 0xRRGGBB. */
    public int primary() { return primary; }
    public int secondary() { return secondary; }
    /** Morph progress 0 .. 1 (1 when settled). */
    public float morph() { return morph; }
    /** Shatter progress 0 .. 1; 0 when no shatter runs. */
    public float shatter() { return shatter; }
    public boolean shattering() { return shatter > 0f; }
    public float colDx(int i) { return colDx[i]; }
    public float colDy(int i) { return colDy[i]; }
    /** Opacity of the bar during a shatter (dips to 0.55 at the widest point). */
    public float shatterAlpha() { return shatterAlpha; }
    public int phase() { return phase; }
    public boolean rage() { return rage; }
    public boolean shaking() { return shakeX != 0f || shakeY != 0f || flash > 0f; }
}
