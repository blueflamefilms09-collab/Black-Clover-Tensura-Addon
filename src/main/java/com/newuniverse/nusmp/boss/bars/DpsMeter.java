package com.newuniverse.nusmp.boss.bars;

/**
 * Why: the optional "damage per second" ticker of the bar (client option {@code bossbar.showDps}): how much of the boss's health bar the party took off in the last three seconds, as percent
 * per second. A fixed ring of one sample per tick, no allocation; only the drops count (a heal is not negative damage).
 */
public final class DpsMeter {
    public static final int WINDOW = 60;

    private final float[] samples = new float[WINDOW];
    private long lastTick = Long.MIN_VALUE;
    private float lastHp = 1f;
    private float sum;

    /** One game tick of input; a repeat of the same tick is ignored, a gap is treated as ticks without damage. */
    public void tick(long tick, float hp) {
        if (lastTick == Long.MIN_VALUE) {
            lastTick = tick;
            lastHp = hp;
            return;
        }
        if (tick <= lastTick) return;
        long gap = Math.min(WINDOW, tick - lastTick);
        for (long k = 1; k < gap; k++) put(lastTick + k, 0f);
        put(tick, Math.max(0f, lastHp - hp));
        lastTick = tick;
        lastHp = hp;
    }

    private void put(long tick, float v) {
        int i = (int) Math.floorMod(tick, (long) WINDOW);
        sum += v - samples[i];
        samples[i] = v;
    }

    /** Percent of the whole bar lost per second over the window (0 when idle). */
    public float percentPerSecond() { return Math.max(0f, sum) * 100f / (WINDOW / 20f); }

    public void reset() {
        java.util.Arrays.fill(samples, 0f);
        sum = 0f;
        lastTick = Long.MIN_VALUE;
    }
}
