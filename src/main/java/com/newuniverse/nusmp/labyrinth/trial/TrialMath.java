package com.newuniverse.nusmp.labyrinth.trial;

/**
 * Why: every number of a trial that can be decided without a level lives here as a pure function over ints and doubles, so the rules
 * (the box a Sealed Archway watches, how a mob count grows with the party, how high a barrier column rises, when a player is standing in a
 * doorway) are unit-tested in {@code TrialMathTest} without Minecraft on the classpath, and {@link SealedArchwayBlockEntity} is only the
 * plumbing round them. The clamps are here too, because a template author's NBT ({@code radius:9}) is data from a file and the block entity
 * must survive any value in it: a radius of 0 would watch nothing, a radius of 200 would scan forty thousand blocks every half second.
 */
public final class TrialMath {
    private TrialMath() {}

    /** The defaults a template may leave out: the box is 17 wide, 8 deep and 2 high over the keystone, and an abandoned trial resets after a minute. */
    public static final int DEFAULT_RADIUS = 8, DEFAULT_HEIGHT = 8, DEFAULT_RESET_SECONDS = 60;
    public static final int MIN_RADIUS = 1, MAX_RADIUS = 32, MIN_HEIGHT = 1, MAX_HEIGHT = 48, MIN_RESET_SECONDS = 10, MAX_RESET_SECONDS = 600;
    /** How far the box reaches ABOVE the keystone: the arch sits at the top of a doorway, the room is below it, two blocks of headroom is enough. */
    public static final int ABOVE = 2;
    /** A barrier column rises at most this many cells over its Ward Rune (a doorway is 2..4 tall; anything higher is a wall with a hole in it). */
    public static final int MAX_COLUMN = 4;
    /** No single mob entry ever grows past this, whatever the party size: a room of 13 zombies is a lag source, not a trial. */
    public static final int MAX_COUNT = 12;
    /** The server ticker runs the trial logic every tenth tick: a half-second reaction is invisible to a player and ten times cheaper. */
    public static final int TICK_EVERY = 10;
    /** A spawned mob that cannot be found in a loaded chunk this many checks in a row is dead (entity loading after a chunk load is asynchronous). */
    public static final int MISSING_STRIKES = 3;
    /** Where mobs stand when a chamber has no Spawn Glyph at all: the floor two blocks out from the keystone, in the eight directions. */
    public static final int[][] FALLBACK_OFFSETS = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}, {2, -2}, {-2, 2}};

    /** An inclusive block box. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        /** An entity position (its feet) is inside when the block it stands in is. */
        public boolean contains(double x, double y, double z) {
            return contains(floor(x), floor(y), floor(z));
        }

        public int volume() { return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1); }
    }

    /** The box a keystone at (cx, cy, cz) watches: radius to each side, {@code height} blocks down and {@link #ABOVE} up. */
    public static Box box(int cx, int cy, int cz, int radius, int height) {
        int r = clampRadius(radius), h = clampHeight(height);
        return new Box(cx - r, cy - h, cz - r, cx + r, cy + ABOVE, cz + r);
    }

    public static int clampRadius(int radius) { return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius)); }

    public static int clampHeight(int height) { return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height)); }

    public static int clampResetSeconds(int seconds) { return Math.max(MIN_RESET_SECONDS, Math.min(MAX_RESET_SECONDS, seconds)); }

    public static int resetTicks(int seconds) { return clampResetSeconds(seconds) * 20; }

    /**
     * How many of one mob entry a party gets: the base count plus half the base for every player past the first (rounded down), never
     * more than {@link #MAX_COUNT}. One player: base. Two: base + base/2. A base of 0 (or less) spawns nothing whatever the party.
     */
    public static int scaledCount(int baseCount, int players) {
        if (baseCount <= 0) return 0;
        int p = Math.max(1, players);
        int n = baseCount + (int) Math.floor((p - 1) * baseCount * 0.5);
        return Math.min(MAX_COUNT, n);
    }

    /** How many barrier cells a Ward Rune gets when {@code airAbove} consecutive cells over it are air: all of them, up to {@link #MAX_COLUMN}. */
    public static int columnHeight(int airAbove) { return Math.max(0, Math.min(MAX_COLUMN, airAbove)); }

    /** As {@link #columnHeight(int)} with the cap given: 7 for a doorway cut to 7 x 7 ({@link ArchwayDoorways#columnCap}). */
    public static int columnHeight(int airAbove, int cap) { return Math.max(0, Math.min(cap, airAbove)); }

    /** Is the block cell (px, py, pz) one of the {@code height} barrier cells over the rune at (rx, ry, rz)? */
    public static boolean inColumn(int rx, int ry, int rz, int height, int px, int py, int pz) {
        return height > 0 && px == rx && pz == rz && py > ry && py <= ry + height;
    }

    /**
     * Does an entity's bounding box touch any of the {@code height} barrier cells over the rune at (rx, ry, rz)? The column occupies
     * x in [rx, rx+1), y in [ry+1, ry+1+height), z in [rz, rz+1); a box touching its faces exactly does not overlap.
     */
    public static boolean overlapsColumn(int rx, int ry, int rz, int height, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (height <= 0) return false;
        return maxX > rx && minX < rx + 1 && maxZ > rz && minZ < rz + 1 && maxY > ry + 1 && minY < ry + 1 + height;
    }

    /** Has a trial been abandoned: no player for at least {@code resetTicks} ticks? */
    public static boolean abandoned(long now, long lastPlayerTick, int resetTicks) { return now - lastPlayerTick >= resetTicks; }

    /** Should the trial logic run this tick? Archways are spread over the ten-tick cycle by their position so a floor of them never ticks together. */
    public static boolean dueThisTick(long gameTime, long posSeed) { return Math.floorMod(gameTime + (posSeed & 7), TICK_EVERY) == 0; }

    public static int floor(double v) { return (int) Math.floor(v); }
}
