package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: a stair is a promise about two places, and the promise must be the same for every road and every floor or a party ends up on the wrong level: the stair
 * that leads BACK from floor {@code k} of a road arrives at the forward stairwell of floor {@code k - 1} (or in the Hub, Floor 0, from the first floor), the stair that
 * leads ON arrives at the arrival stairwell of floor {@code k + 1}. Which floor that is and whether it is a place yet is arithmetic on chain indices and state
 * names, and where exactly a player has to stand to use a stair is arithmetic on a marker cell; both are written here, on plain numbers, so {@code StairTravel} only
 * carries them out and a test pins every corner (the first floor, a floor that is still being generated, a failed floor, a player standing just beside the pad).
 */
public final class StairLinks {
    private StairLinks() {}

    /** Where a stair leads: the Hub, or the floor with this chain index (arriving at its arrival stairwell or at its forward stairwell). */
    public record Dest(boolean hub, int chainIndex, boolean arrivesAtForward) {
        public static Dest toHub() { return new Dest(true, -1, false); }
    }

    /** The destination of the stair that leads back from the floor with this chain index: the Hub from the first floor, else the forward stairwell of the floor before. */
    public static Dest back(int chainIndex) {
        return chainIndex <= 0 ? Dest.toHub() : new Dest(false, chainIndex - 1, true);
    }

    /** The destination of the stair that leads on from the floor with this chain index: the arrival stairwell of the next floor. */
    public static Dest forward(int chainIndex) { return new Dest(false, Math.max(0, chainIndex) + 1, false); }

    /** Can a player stand on a floor in this state ({@code Sector.State} name)? A floor that is still being generated, or failed, is no place yet. */
    public static boolean playable(String state) { return state != null && !"GENERATING".equals(state) && !"FAILED".equals(state); }

    /** How far from the middle of the exit cell a player may stand sideways to use the stair: the landing pad is 3 x 3 blocks, so the whole pad counts. */
    public static final double PAD_RADIUS = 1.45;
    /** How far below / above the marker cell's floor a player's feet may be (a jump, a slab). */
    public static final double PAD_BELOW = 0.6, PAD_ABOVE = 1.6;

    /**
     * Is a player whose feet are at {@code (x, y, z)} on the landing pad of the stair whose STAIR_EXIT marker cell is {@code (mx, my, mz)}? The marker stands in the
     * air cell above the middle of the pad; the player's feet are inside that cell (y from {@code my}) when they stand on the pad.
     */
    public static boolean onPad(double x, double y, double z, int mx, int my, int mz) {
        return Math.abs(x - (mx + 0.5)) <= PAD_RADIUS && Math.abs(z - (mz + 0.5)) <= PAD_RADIUS && y >= my - PAD_BELOW && y <= my + PAD_ABOVE;
    }
}
