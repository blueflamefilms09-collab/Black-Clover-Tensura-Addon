package com.newuniverse.nusmp.unique;

/**
 * Why: the numbers and decisions of the Unique Guardian that need no game, so they are unit tests: when a copy that is not accounted for counts as gone, when a holder has been away long enough, the flap guard, and the
 * void rescue line. Pure, no Minecraft types.
 */
public final class UniqueRules {
    private UniqueRules() {}

    /** A copy whose item entity was destroyed is confirmed gone after this many ticks without being seen anywhere (a hopper or a pick-up mod may still be handing it over). */
    public static final int CONFIRM_TICKS = 100;
    /** At most this many returns to the pool per unique in one real hour (a loop of "found, destroyed, found, destroyed" cannot flap). */
    public static final int MAX_RELEASES_PER_HOUR = 3;
    public static final long HOUR_MS = 3_600_000L, DAY_MS = 86_400_000L, SEEN_WRITE_MS = 60_000L;

    public enum Verdict { NONE, RELEASE }

    public static long graceTicks(int seconds) { return Math.max(1, seconds) * 20L; }

    public static long absenceMs(int days) { return Math.max(0, days) * DAY_MS; }

    /** Is a copy that stopped being accounted for at {@code r.missingSince} gone for good at {@code nowTick}? */
    public static Verdict verdict(UniqueRecord r, long nowTick, long graceTicks) {
        if (r == null || !r.missing()) return Verdict.NONE;
        long wait = r.confirmed ? Math.min(CONFIRM_TICKS, graceTicks) : graceTicks;
        return nowTick - r.missingSince >= wait ? Verdict.RELEASE : Verdict.NONE;
    }

    /** The holder of a HELD copy has not been seen for {@code absenceMs} (0 = rule off) and is not online: the copy is gone for good. */
    public static boolean absent(UniqueRecord r, boolean holderOnline, long nowMs, long absenceMs) {
        return r != null && absenceMs > 0 && r.state == UniqueState.HELD && r.holder != null && !holderOnline && r.seenMs > 0 && nowMs - r.seenMs >= absenceMs;
    }

    /** The flap guard: may this unique return to the pool again now? */
    public static boolean mayRelease(UniqueRecord r, long nowMs) {
        if (r == null) return false;
        int n = 0;
        for (int i = 0; i < r.stampCount; i++) if (nowMs - r.stamps[i] < HOUR_MS) n++;
        return n < MAX_RELEASES_PER_HOUR;
    }

    /** An item entity this low is about to be deleted by the void: lift it back to the surface. */
    public static boolean inVoid(double y, int minBuildHeight) { return y < minBuildHeight + 1.0; }

    /** A stamped copy (charter grimoire, dragon ball) made before the last return to the pool is an old copy. {@code stamp} < 0 means unstamped. */
    public static boolean oldCopy(int stamp, int epoch) { return stamp >= 0 && stamp < epoch; }

    /** Is this text a safe unique id (kind prefix, lower case word characters, colons, dashes)? */
    public static boolean validId(String id) { return id != null && id.length() >= 1 && id.length() <= 96 && id.matches("[a-z0-9_:.\\-]+"); }
}
