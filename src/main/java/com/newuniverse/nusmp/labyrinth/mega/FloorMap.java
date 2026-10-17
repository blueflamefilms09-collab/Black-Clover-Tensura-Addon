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
 *
 * <p>1.58.0 (the floor model, docs/labyrinth/mega_v2.md section 1): the owner found the floors "together" (ten floors laid flat in one sector never felt
 * like going up or down), so a sector is now exactly ONE floor ({@link #DEFAULT_PER_SECTOR}, config {@code floors.perSector}): the party starts at the foot of
 * a stair and must find the stair that leads on. Everything the old model computed is still here and unchanged (a world that has ten-floor sectors keeps
 * them); the new helpers answer the questions the floor model adds: is this a floor-model sector, is this a boss floor (every {@link #BOSS_EVERY}th, whatever the
 * sector length), which decade does a sector belong to (a theme belongs to a decade of floors).
 */
public final class FloorMap {
    private FloorMap() {}

    /**
     * Floors per sector (region) of the OLD banded model: the brief's "every 10th floor". Kept (and used as the default of a {@code Sector} that was saved before 1.58.0)
     * because a world that already has sectors of ten floors keeps them (owner's rule 2: add the new, keep the old).
     */
    public static final int DEFAULT_PER_REGION = 10;

    /**
     * 1.58.0: floors per sector of the floor model, the default: a sector IS one floor, a level of its own with an arrival stair and a forward stair (D&D style).
     * {@code floors.perSector} in the floors config feeds it; any value above 1 keeps the old banding code alive.
     */
    public static final int DEFAULT_PER_SECTOR = 1;

    /** 1.58.0: a boss floor, a Guardian, a Crucible and a new theme come every this many floors (the brief's "every 10th floor", independent of how floors are grouped into sectors). */
    public static final int BOSS_EVERY = 10;

    /** 1.58.0: does a sector with this many floors follow the floor model (one floor, one level)? */
    public static boolean isFloorModel(int perRegion) { return perRegion <= 1; }

    /** 1.58.0: the floors per sector a config value asks for, kept in the range the code supports (1 = the floor model, up to 20 = the old banding). */
    public static int floorsPerSector(int configured) { return Math.max(1, Math.min(20, configured)); }

    /** 1.58.0: is this a boss floor (|floor| % 10 == 0)? Floor 0 (the Hub) is not. */
    public static boolean isBossFloor(int floor) { return floor != 0 && Math.abs(floor) % BOSS_EVERY == 0; }

    /**
     * 1.58.0: the signed floor a floor-model sector stands for (its only floor): the road's sign times {@code chainIndex + 1}. Sector {@code k} of the Holy road is
     * floor {@code k + 1}, of the Abyssal road floor {@code -(k + 1)}.
     */
    public static int floorOfSector(Polarity p, int chainIndex) { return floorOf(p, chainIndex, 1, 1); }

    /**
     * 1.58.0: the 0-based decade (floors 1..10 are decade 0, 11..20 decade 1 ...) a sector of {@code perRegion} floors belongs to. A theme belongs to a decade
     * of floors, so every sector of the decade wears it. With ten floors per sector this is the chain index itself (each sector is its own decade, the 1.55.0 rule).
     */
    public static int decadeOf(int chainIndex, int perRegion) {
        return (int) ((long) Math.max(0, chainIndex) * Math.max(1, perRegion) / BOSS_EVERY);
    }

    /** 1.58.0: is this sector the first of its decade (the one that draws a new theme, or takes the one the Crucible's relic chose)? */
    public static boolean startsDecade(int chainIndex, int perRegion) {
        return (long) Math.max(0, chainIndex) * Math.max(1, perRegion) % BOSS_EVERY == 0;
    }

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

    /**
     * Is this a decennial floor (+-10, +-20 ...): the floor of a sector Guardian and, behind it, the Crucible? Floor 0 is not. A sector of {@code perRegion} floors
     * ends in a decennial floor every {@code perRegion} floors (the old banded model); a sector of ONE floor (the floor model) has a boss only every
     * {@link #BOSS_EVERY} floors, so a one-floor sector must not read "every floor is decennial".
     */
    public static boolean isDecennial(int floor, int perRegion) { return floor != 0 && Math.abs(floor) % (isFloorModel(perRegion) ? BOSS_EVERY : perRegion) == 0; }

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
