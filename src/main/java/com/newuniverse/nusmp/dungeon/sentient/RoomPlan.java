package com.newuniverse.nusmp.dungeon.sentient;

/**
 * What a room of the Dungeon births when a player first walks into it: whether it is populated at all and how many mobs come out of its
 * walls. Rooms are rolled once and stay as rolled (the roll is a stable hash of the room and the world seed, never a random call), so a
 * room that was empty stays empty and the same room always rolls the same. Tunnels and nooks are not rooms. Pure.
 */
public final class RoomPlan {
    private RoomPlan() {}

    /** The answer: whether the room is populated, and how many mobs are born into it (0 when it is not). */
    public record Birth(boolean populated, int count) {
        static final Birth NONE = new Birth(false, 0);
    }

    /** Fewer standable spots than this and the place is a tunnel or a nook, not a room; no room ever holds more than HARD_CAP. */
    public static final int MIN_SPOTS = 10, HARD_CAP = 8;

    /** The share of the rooms of a floor that are populated, in percent. */
    public static int percent(FloorKind kind) {
        return switch (kind) {
            case CATACOMBS -> 70;
            case WATERWAYS, UNDERWORLD -> 60;
            default -> 0;
        };
    }

    /** The least mobs a populated room of the floor births (0 for a floor with no room births). */
    public static int least(FloorKind kind) {
        return switch (kind) {
            case CATACOMBS -> 5;
            case WATERWAYS -> 4;
            case UNDERWORLD -> 3;
            default -> 0;
        };
    }

    /** The most mobs a populated room of the floor births: the design's cap of 8 on the Upper Catacombs. */
    public static int most(FloorKind kind) {
        return switch (kind) {
            case CATACOMBS -> 8;
            case WATERWAYS -> 6;
            case UNDERWORLD -> 5;
            default -> 0;
        };
    }

    public static Birth decide(FloorKind kind, long roomKey, long worldSeed, int standableSpots) { return decide(kind, roomKey, worldSeed, standableSpots, 1.0); }

    /**
     * @param scale the dungeon.roomPopulation setting: it multiplies the count; the count is then held to a third of the standable spots and to {@link #HARD_CAP}
     */
    public static Birth decide(FloorKind kind, long roomKey, long worldSeed, int standableSpots, double scale) {
        int percent = percent(kind);
        if (percent <= 0 || standableSpots < MIN_SPOTS) return Birth.NONE;
        long h = mix(roomKey, worldSeed, kind.ordinal() + 1L);
        if (Math.floorMod(h, 100L) >= percent) return Birth.NONE;
        int least = least(kind), most = most(kind);
        int base = least + (int) Math.floorMod(mix(h, worldSeed, 77L), (long) (most - least + 1));
        int scaled = (int) Math.round(base * Math.max(0.0, scale));
        int cap = Math.min(HARD_CAP, standableSpots / 3);
        return new Birth(true, Math.max(1, Math.min(scaled, cap)));
    }

    /** A stable 64-bit hash of three numbers (the SplitMix64 finalizer). */
    static long mix(long a, long b, long salt) {
        long z = a * 0x9E3779B97F4A7C15L ^ Long.rotateLeft(b, 31) * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
