package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.blackclover.Devil;

/**
 * The numbers and choices of the creatures of the Underworld Gate, as plain values so they are tested without the game: how many Dark
 * Disciples the ruin carries and how fast they come back, what share of a Devil's power each channels, which Devil, where they may appear,
 * and what the Gatekeeper gets. Pure.
 *
 * <p>The Devils of the ruin are real mobs of Tensura (greater and lesser daemons); the Disciples are the followers of the Zogratis family,
 * who channel Lucifero (gravity, the way Dante does), Megicula (curse, the way Vanica does) or Beelzebub (space, the way Zenon does).
 */
public final class UnderworldRules {
    private UnderworldRules() {}

    /** The most Disciples at once, the least a player count gives, the share each extra player adds. */
    public static final int CAP_MAX = 12, CAP_BASE = 6, CAP_PER_PLAYER = 2;
    /** Game ticks between a Disciple's coming and the next: shortest when the ruin is nearly empty, longest when it is nearly full. */
    public static final int RESPAWN_FAST = 600, RESPAWN_SLOW = 1200;
    /** A new Disciple appears at least this far (blocks) from every player in the hall, and never in sight of one closer than that. */
    public static final double APPEAR_MIN = 26.0;
    /** The share of a Devil's power a Disciple channels: from a tenth where the ruin begins to two fifths at the Gate. */
    public static final float FRACTION_MIN = 0.10f, FRACTION_MAX = 0.40f;
    /** The Devils of lesser rank that stand guard when the hall is made. */
    public static final int LESSER_DAEMONS = 5;
    /** The Gatekeeper's strength: shares added to its health and its blows, a pinch of resistance to being thrown about, armour. */
    public static final double KEEPER_HEALTH = 0.5, KEEPER_DAMAGE = 0.25, KEEPER_KNOCKBACK = 0.4, KEEPER_ARMOUR = 4.0;
    /** How far from its post the Gatekeeper may wander without a target before it goes back (blocks), and how far a player sees its bar. */
    public static final double KEEPER_LEASH = 14.0, BAR_RANGE = 48.0;
    /** A Devil of lesser rank is kept between these hunger values, so the ecosystem's rules neither starve it nor put it to sleep: it is topped up to the second when it falls under the first. */
    public static final int KEEP_LOW = 52, KEEP_FULL = 62;

    /** How many Disciples the ruin carries for this many players in it (0 when it is empty). */
    public static int discipleCap(int players) {
        if (players <= 0) return 0;
        return Math.min(CAP_MAX, CAP_BASE + CAP_PER_PLAYER * (players - 1));
    }

    /** How many Disciples stand in the hall when it is made. */
    public static int initialDisciples(int players) { return Math.max(3, discipleCap(Math.max(1, players)) / 2); }

    /** Game ticks until the next Disciple comes, or -1 if the ruin is at its cap. */
    public static int respawnDelay(int alive, int cap) {
        if (cap <= 0 || alive >= cap) return -1;
        int missing = cap - alive;
        return RESPAWN_SLOW - (RESPAWN_SLOW - RESPAWN_FAST) * missing / cap;
    }

    /** The share (0.10 to 0.40, in steps of 0.05) of a Devil's power a Disciple channels where the depth into the ruin is `depth` (0 at the shell, 1 at the Gate). */
    public static float fraction(double depth) {
        double d = Math.max(0.0, Math.min(1.0, depth));
        double raw = FRACTION_MIN + (FRACTION_MAX - FRACTION_MIN) * d;
        float stepped = (float) (Math.round(raw / 0.05) * 0.05);
        return Math.max(FRACTION_MIN, Math.min(FRACTION_MAX, stepped));
    }

    /** The share as a whole percent (10 to 40). */
    public static int percent(float fraction) { return Math.round(fraction * 100f); }

    /** The Devil the n-th Disciple channels: Lucifero (gravity, Dante's), Megicula (curse, Vanica's), Beelzebub (space, Zenon's), in turn. */
    public static Devil channel(int n) {
        return switch (Math.floorMod(n, 3)) {
            case 0 -> Devil.LUCIFERO;
            case 1 -> Devil.MEGICULA;
            default -> Devil.BEELZEBUB;
        };
    }

    /**
     * Which of the spots (x, z) a new Disciple appears at: the ones at least APPEAR_MIN blocks from every player, one chosen by `salt`; -1 if
     * none is. Players are given as (x, z) pairs.
     */
    public static int pickSpot(double[][] spots, double[][] players, long salt) {
        int[] ok = new int[spots.length];
        int n = 0;
        for (int i = 0; i < spots.length; i++) {
            boolean far = true;
            for (double[] p : players) {
                double dx = spots[i][0] - p[0], dz = spots[i][1] - p[1];
                if (dx * dx + dz * dz < APPEAR_MIN * APPEAR_MIN) far = false;
            }
            if (far) ok[n++] = i;
        }
        return n == 0 ? -1 : ok[(int) Math.floorMod(salt, (long) n)];
    }

    /**
     * Does the hall place its own Devils of lesser rank and Dark Disciples when it is built, and bring Disciples back while players are in it?
     * Not while the Sentient Dungeon is on: then the rooms of the Dungeon are born from their walls, and Type C's low-tier Devils and Disciples
     * come from those births. With the sentient switch off it is exactly the hall of 1.21.0. The Gatekeeper and the Devil Avatar stay either way.
     */
    public static boolean staticPopulation(boolean sentient) { return !sentient; }

    /**
     * Which Devil Avatar guards the hall of a core cell: 0 the Avatar of Lucifero (violet, gravity), 1 the Avatar of Megicula (crimson, curses),
     * by a hash of the cell, so a hall is always guarded by the same one and both are met across the Variable Core.
     */
    public static int avatarVariant(int cellX, int cellZ) {
        long h = cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ 0x5851F42D4C957F2DL;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 29;
        return (int) (h & 1L);
    }

    /** Should the Gatekeeper walk back to its post: it has no target and is farther from it than the leash? */
    public static boolean shouldReturn(double distanceToPost, boolean hasTarget) { return !hasTarget && distanceToPost > KEEPER_LEASH; }

    /** Does a Devil of lesser rank need feeding (its hunger is under KEEP_LOW)? */
    public static boolean needsFeeding(int hunger) { return hunger < KEEP_LOW; }
}
