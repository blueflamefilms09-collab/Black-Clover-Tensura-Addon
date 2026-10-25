package com.newuniverse.nusmp.client.relics.worn.lib;

/**
 * 3.9.0: how many wearers may be drawn with the blocky x3 armour models at once. Wearers are drawn one after the other in a frame; the first {@code limit} that ask inside a short window
 * (about one frame) get the x3 models, the rest wear the classic ones (the LOD form). The window starts again when it is older than {@link #WINDOW_NS}, so the cost of x3 is at most
 * {@code limit} wearers per frame whatever the crowd, and the order of the entities decides who, so nobody flickers between the two. Pure: the clock is a parameter. Render thread only.
 */
public final class X3Gate {
    private X3Gate() {}

    public static final long WINDOW_NS = 30_000_000L;
    private static long start;
    private static int used;

    /** True when one more wearer may be drawn at x3 detail now. */
    public static boolean allow(long nowNs, int limit) {
        if (nowNs - start > WINDOW_NS || nowNs < start) {
            start = nowNs;
            used = 0;
        }
        if (used >= limit) return false;
        used++;
        return true;
    }

    /** Forgets the window (a test, a resource reload). */
    public static void reset() {
        start = 0L;
        used = 0;
    }
}
