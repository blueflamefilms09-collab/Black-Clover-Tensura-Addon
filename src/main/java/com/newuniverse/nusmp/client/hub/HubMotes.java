package com.newuniverse.nusmp.client.hub;

/**
 * Why: the hub's floating motes are a fixed pool of {@link #COUNT} specks allocated once with the screen (no entity, no particle, no list that grows): the draw reads these arrays and the step moves them, so a
 * frame allocates nothing and the cost is the same at any time. Pure (no Minecraft types) so a test can check that the pool never grows, every mote stays on the stage and the motion is deterministic from the
 * seed. Positions are stage units; time is in ticks.
 */
public final class HubMotes {
    public static final int COUNT = 24;

    public final float[] x = new float[COUNT], y = new float[COUNT], vx = new float[COUNT], vy = new float[COUNT], phase = new float[COUNT];
    public final int[] size = new int[COUNT];
    private final float w, h;

    public HubMotes(long seed, float w, float h) {
        this.w = w;
        this.h = h;
        long s = seed;
        for (int i = 0; i < COUNT; i++) {
            s = next(s);
            x[i] = (s >>> 8 & 0xFFFF) / 65535f * w;
            s = next(s);
            y[i] = (s >>> 8 & 0xFFFF) / 65535f * h;
            s = next(s);
            vx[i] = ((s >>> 8 & 0xFFFF) / 65535f - 0.5f) * 0.12f;
            s = next(s);
            vy[i] = -0.03f - (s >>> 8 & 0xFFFF) / 65535f * 0.09f;                       // they drift upwards
            s = next(s);
            phase[i] = (s >>> 8 & 0xFFFF) / 65535f;
            size[i] = i % 5 == 0 ? 2 : 1;
        }
    }

    private static long next(long s) { return s * 6364136223846793005L + 1442695040888963407L; }

    /** Moves every mote by {@code dtTicks} (a frame's worth, capped by the caller) and wraps it round the stage. */
    public void step(float dtTicks) {
        for (int i = 0; i < COUNT; i++) {
            x[i] += vx[i] * dtTicks;
            y[i] += vy[i] * dtTicks;
            if (x[i] < 0f) x[i] += w;
            else if (x[i] >= w) x[i] -= w;
            if (y[i] < 0f) y[i] += h;
            else if (y[i] >= h) y[i] -= h;
        }
    }

    /** The twinkle of mote {@code i} at {@code time} ticks, 0..1. */
    public float twinkle(int i, float time) { return 0.5f + 0.5f * (float) Math.sin((time / 70f + phase[i]) * (Math.PI * 2.0)); }
}
