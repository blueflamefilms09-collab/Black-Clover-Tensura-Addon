package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.enc.Band;

/**
 * Why: every decision of the room lockdown that can be made without a level is here as a pure function, so the rules are lines of a unit test and the game classes
 * ({@link LockdownDirector}, {@link LockdownBarrierBlockEntity}) only gather the facts and carry out the verdict. {@link #decide} is the order of the gates a room
 * must pass before it closes, from the cheapest to the most specific, and each refusal has its own name so the status command and the self-test can say WHY a room
 * did not lock (the owner cannot attach a debugger to a running game: "nothing locked" must always have a reason). The rest is arithmetic: how a room's rule is
 * raised by depth and by a bigger party (the wave grows with the floor and the people, never past HARD: the ambush band belongs to the ambush director), the
 * deterministic share of a common kind that locks, and "is this player deep enough inside to be locked in" (a player standing in the doorway is never sealed in a
 * wall). Pure.
 */
public final class LockdownMath {
    private LockdownMath() {}

    /** Why a room did, or did not, lock. {@link #ARM} is the only one that closes it. */
    public enum Verdict {
        ARM, OFF, PEACEFUL, SAFE_ROOM, NOT_LOCKABLE, CHANCE, TOO_SHALLOW, TOO_SMALL, CLEARED, ACTIVE, COOLDOWN, NOBODY, NOT_INSIDE;

        public boolean arms() { return this == ARM; }
    }

    /** Everything {@link #decide} reads. {@code cooldownLeft} is in ticks (0 = none); {@code depth} is the absolute floor number (0 = unknown). */
    public record Facts(boolean enabled, boolean peaceful, boolean safeRoom, boolean lockable, boolean chanceOk, int depth, int minFloor, int area, int minArea,
                        boolean cleared, boolean active, long cooldownLeft, boolean anyoneToLock, boolean deepInside) {}

    /** The gates in the order they are asked. */
    public static Verdict decide(Facts f) {
        if (!f.enabled()) return Verdict.OFF;
        if (f.peaceful()) return Verdict.PEACEFUL;
        if (f.safeRoom()) return Verdict.SAFE_ROOM;
        if (!f.lockable()) return Verdict.NOT_LOCKABLE;
        if (!f.chanceOk()) return Verdict.CHANCE;
        if (f.depth() > 0 && f.depth() < f.minFloor()) return Verdict.TOO_SHALLOW;
        if (f.area() < f.minArea()) return Verdict.TOO_SMALL;
        if (f.cleared()) return Verdict.CLEARED;
        if (f.active()) return Verdict.ACTIVE;
        if (f.cooldownLeft() > 0L) return Verdict.COOLDOWN;
        if (!f.anyoneToLock()) return Verdict.NOBODY;
        if (!f.deepInside()) return Verdict.NOT_INSIDE;
        return Verdict.ARM;
    }

    // ---------------------------------------------------------------- how hard
    /** From this absolute floor on, a locked room's wave is one band stronger. */
    public static final int DEEP_FLOOR = 20;
    /** A party of this many survivors or more makes the wave one band stronger. */
    public static final int BIG_PARTY = 4;

    /**
     * The band of a locked room's wave: the rule's own, one up on a deep floor, one up for a big party, never past HARD (an ambush belongs to the ambush director, and
     * a locked room is a fight you cannot walk away from, so it must stay winnable).
     */
    public static Band band(Band base, int depth, int partySize) {
        Band b = base == null || base == Band.NOTHING ? Band.STANDARD : base;
        if (depth >= DEEP_FLOOR) b = b.up();
        if (partySize >= BIG_PARTY) b = b.up();
        Band cap = base == Band.AMBUSH ? Band.AMBUSH : Band.HARD;            // a data file may ask for an ambush-sized wave outright; the depth never does
        return b.ordinal() > cap.ordinal() ? cap : b;
    }

    /** Is there another wave after the one just beaten? */
    public static boolean moreWaves(int wave, int waves) { return wave < waves; }

    /** Waves: the rule's own, one more from floor 40 (capped at {@link LockdownRule#MAX_WAVES}). */
    public static int waves(int base, int depth) {
        int w = Math.max(1, base) + (depth >= 2 * DEEP_FLOOR ? 1 : 0);
        return Math.min(LockdownRule.MAX_WAVES, w);
    }

    /**
     * Does this room belong to the share of its kind that locks? Decided once from the sector's seed and the room's id (a splitmix of the two), so the same room is
     * a challenge room every time it is looked at, on every restart, and the share is {@code chance} of the rooms on average.
     */
    public static boolean chanceOk(long sectorSeed, int roomId, double chance) {
        if (chance >= 1.0) return true;
        if (chance <= 0.0) return false;
        long z = sectorSeed ^ (0x9E3779B97F4A7C15L * (roomId + 1L));
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        double u = (z >>> 11) * 0x1.0p-53;
        return u < chance;
    }

    // ---------------------------------------------------------------- where
    /**
     * Is the point at least {@code inset} blocks inside the box on both horizontal axes? A room locks only when someone is well past its doorway: the wall layers of
     * a room are two thick and the doorway is in them, so the default inset of 4 puts the trigger two blocks into the room proper.
     */
    public static boolean deepInside(int minX, int minZ, int maxX, int maxZ, double x, double z, int inset) {
        return x >= minX + inset && x < maxX + 1 - inset && z >= minZ + inset && z < maxZ + 1 - inset;
    }

    /** The footprint of a box in blocks. */
    public static int area(int minX, int minZ, int maxX, int maxZ) { return Math.max(0, maxX - minX + 1) * Math.max(0, maxZ - minZ + 1); }

    /** The key of a room in the saved data and the cooldown table: sector id in the high half, room id in the low half. */
    public static long key(int sectorId, int roomId) { return ((long) sectorId << 32) | (roomId & 0xFFFFFFFFL); }

    public static int sectorOf(long key) { return (int) (key >> 32); }

    public static int roomOf(long key) { return (int) key; }
}
