package com.newuniverse.nusmp.perf;

/**
 * Why: some per-frame answers can be kept only as long as the resources they were read from do not change (does this texture file exist in the packs that are loaded?). A cache of such an answer
 * must be thrown away when the resources reload (F3+T, a resource pack switched). This counter is the signal: a reload listener the mod already has ({@code VfxDefinitions}) calls
 * {@link #bump()} on every resource reload, and a cache stores the epoch it was filled in and starts again when {@link #get()} is different. Pure (no game classes).
 */
public final class PerfEpoch {
    private PerfEpoch() {}

    private static volatile int epoch;

    /** The number of resource reloads seen so far. */
    public static int get() { return epoch; }

    /** A resource reload happened: every cache stamped with an older epoch is stale. */
    public static void bump() { epoch++; }
}
