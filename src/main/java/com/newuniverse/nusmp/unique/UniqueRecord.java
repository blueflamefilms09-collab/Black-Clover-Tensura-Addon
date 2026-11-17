package com.newuniverse.nusmp.unique;

import java.util.UUID;

/**
 * Why: what the world remembers about ONE unique item: the state, the last holder, the last known place, when it was last seen (game ticks for the grace clocks, real milliseconds for the absence rule), whether it is currently
 * unaccounted for, how often it was released into the pool and the release times (the flap guard). {@code epoch} counts the returns to the pool: a copy stamped with an older epoch is an old copy and dissolves. Plain mutable data
 * owned by {@link UniqueData}; only the server thread touches it. Pure, no Minecraft types.
 */
public final class UniqueRecord {
    public static final long NO_BLOCK = Long.MIN_VALUE;
    /** How many release times are kept for the flap guard. */
    public static final int STAMPS = 8;

    public final String id;
    public UniqueState state = UniqueState.POOL;
    public UUID holder, entity;
    public String dim = "";
    public double x, y, z;
    public long block = NO_BLOCK;
    public long seenTick, seenMs, savedMs;
    /** Game tick at which the copy stopped being accounted for, or -1 while it is. */
    public long missingSince = -1L;
    /** True when a destroying removal was seen (the short confirm clock applies), false when it only vanished from a scan (the full grace applies). */
    public boolean confirmed;
    public int epoch, releases;
    public long lastReleaseMs;
    public String lastReason = "";
    public final long[] stamps = new long[STAMPS];
    public int stampCount;

    public UniqueRecord(String id) { this.id = id; }

    public boolean missing() { return missingSince >= 0L && state != UniqueState.POOL; }
}
