package com.newuniverse.nusmp.labyrinth.mega;

/**
 * Why: the owner's brief speaks in FLOORS (Floor +1, Floor -1, "a colossal boss gates every 10th floor", "exactly one relic per 10 floors", "clear 5 rooms
 * on this floor"), while the engine of 1.54.0 builds SECTORS (one Region = one Landing, a maze of rooms, a Guardian, a Crucible). The two are reconciled
 * here, in one pure place: a sector is TEN floors ("bands" of its room graph, counted outward from the Landing), the Guardian's arena is the tenth floor, so
 * the sector Guardian IS the decennial boss and the Crucible behind it IS the "one relic per 10 floors". A floor number is signed (Holy positive, Abyssal
 * negative) and never 0 (Floor 0 is the Hub, outside every sector). Everything is integer arithmetic with no Minecraft import, so JUnit pins every rule and
 * the builders of the alignment lock, the System overlay, the encounter tables, the loot and the Rukh flow all use the same numbers. {@code perRegion} is a
 * parameter (the config key {@code floors.perRegion} feeds it, default {@link #DEFAULT_PER_REGION}) because a server may want shorter regions; every
 * overload without it uses the default.
 */
public final class FloorMap {
    private FloorMap() {}

    /** Floors per sector (region): the brief's "every 10th floor". */
    public static final int DEFAULT_PER_REGION = 10;

    /**
     * The signed floor number of band {@code band} (1-based, clamped to 1..perRegion) of the {@code chainIndex}-th (0-based) sector of a polarity.
     * Holy sector 0 covers +1..+10, Holy sector 1 covers +11..+20; Abyssal sector 0 covers -1..-10.
     */
    public static int floorOf(Polarity p, int chainIndex, int band, int perRegion) {
        int per = Math.max(1, perRegion);
        int b = Math.max(1, Math.min(per, band));
        return p.sign() * (Math.max(0, chainIndex) * per + b);
    }

    public static int floorOf(Polarity p, int chainIndex, int band) { return floorOf(p, chainIndex, band, DEFAULT_PER_REGION); }

    /** The 1-based band (1..perRegion) of a floor inside its sector, or 0 for Floor 0. */
    public static int band(int floor, int perRegion) {
        if (floor == 0) return 0;
        int per = Math.max(1, perRegion);
        return (Math.abs(floor) - 1) % per + 1;
    }

    public static int band(int floor) { return band(floor, DEFAULT_PER_REGION); }

    /** The 0-based index of the sector (within its polarity's chain) that holds the floor, or -1 for Floor 0. */
    public static int chainIndex(int floor, int perRegion) {
        if (floor == 0) return -1;
        return (Math.abs(floor) - 1) / Math.max(1, perRegion);
    }

    public static int chainIndex(int floor) { return chainIndex(floor, DEFAULT_PER_REGION); }

    /** The first and last floor of a sector (signed). */
    public static int firstFloor(Polarity p, int chainIndex, int perRegion) { return floorOf(p, chainIndex, 1, perRegion); }

    public static int lastFloor(Polarity p, int chainIndex, int perRegion) { return floorOf(p, chainIndex, perRegion, perRegion); }

    /** Is this a decennial floor (+-10, +-20 ...): the floor of a sector Guardian and, behind it, the Crucible? Floor 0 is not. */
    public static boolean isDecennial(int floor, int perRegion) { return floor != 0 && Math.abs(floor) % Math.max(1, perRegion) == 0; }

    public static boolean isDecennial(int floor) { return isDecennial(floor, DEFAULT_PER_REGION); }

    /**
     * The band (1..perRegion) of a room that is {@code depth} doorways from the Landing, given the depth of the Guardian's arena. The Landing (depth 0) is band 1;
     * the arena and everything beyond it (antechamber, vault, Way Onward) is the LAST band; the depths in between are spread evenly over bands 1..perRegion-1.
     * An unreachable room (negative depth) is the last band (it cannot be walked to, so it must not look like an early floor). A degenerate arena depth
     * (0 or less) makes every room the last band. Monotone: a deeper room never has a lower band.
     */
    public static int bandOfDepth(int depth, int arenaDepth, int perRegion) {
        int per = Math.max(1, perRegion);
        if (depth < 0 || arenaDepth <= 0 || depth >= arenaDepth || per == 1) return per;
        int open = per - 1;
        return 1 + (int) ((long) depth * open / arenaDepth);
    }

    public static int bandOfDepth(int depth, int arenaDepth) { return bandOfDepth(depth, arenaDepth, DEFAULT_PER_REGION); }

    /** The difficulty number of a floor for tables and scaling: its distance from the Hub (at least 1). */
    public static int threat(int floor) { return Math.max(1, Math.abs(floor)); }

    /** "+12", "-3", "0": the way a floor is written on screen and in chat. */
    public static String label(int floor) { return floor > 0 ? "+" + floor : Integer.toString(floor); }
}
