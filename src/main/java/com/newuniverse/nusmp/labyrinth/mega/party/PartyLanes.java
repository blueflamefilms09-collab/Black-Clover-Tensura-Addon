package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: the party lanes are the one piece of arithmetic the whole instancing rests on (two parties must never build into each other, and everything must stay inside
 * the world border), so the questions about them are asked here, in plain Java, where the tests can ask them for all 256 parties at once: where is the lane of a party
 * ({@link MegaGrid#laneZ(Polarity, int)} is the one definition; this class only reads it), which party and road does a z belong to (the guard and the admin commands
 * need the answer for a place that is in no sector), what is the largest and the smallest z a server with {@code n} parties can ever use, and do all those lanes fit
 * inside a world border of a given size. The road of a z is told apart by the middle between the two lane groups, and a z belongs to the nearest lane, so every z has
 * exactly one answer. Pure.
 */
public final class PartyLanes {
    private PartyLanes() {}

    /** How far a floor reaches from the z of its lane: half its largest region plus the reach of a door cap and the gap between regions. Far less than half a {@link MegaGrid#PARTY_STEP}. */
    public static final int REACH = MegaGrid.blocks(MegaGrid.REGION_MODULES) / 2 + 1 + MegaGrid.CAP_MARGIN + MegaGrid.REGION_GAP;

    /** The z of the lane of road {@code road} for party {@code party}: {@link MegaGrid#laneZ(Polarity, int)}. */
    public static int laneZ(Polarity road, int party) { return MegaGrid.laneZ(road, party); }

    /** The largest z any lane of a server that allows {@code maxParties} parties uses: the Abyssal lane of the last party. */
    public static int maxZ(int maxParties) { return MegaGrid.laneZ(Polarity.ABYSSAL, Math.max(1, Math.min(maxParties, MegaGrid.MAX_PARTIES)) - 1); }

    /** The smallest z any lane of such a server uses: the Holy lane of the last party (zero when there is only party 0). */
    public static int minZ(int maxParties) { return MegaGrid.laneZ(Polarity.HOLY, Math.max(1, Math.min(maxParties, MegaGrid.MAX_PARTIES)) - 1); }

    /**
     * The greatest distance from the origin (in blocks, along z) that a block of a floor of such a server can have: the farthest lane plus the reach of a floor. This is the
     * number to compare with the half size of the world border of the Mega-Dungeon (a floor also runs along x, up to {@code maxSectors} floors of 198 blocks).
     */
    public static long farthest(int maxParties) {
        long hi = (long) maxZ(maxParties) + REACH, lo = (long) -minZ(maxParties) + REACH;
        return Math.max(hi, lo);
    }

    /** Do all the lanes of a server with {@code maxParties} parties lie inside a world border whose half size (centre at 0) is {@code halfSize}? */
    public static boolean fits(int maxParties, double halfSize) { return farthest(maxParties) <= halfSize; }

    /**
     * How many parties' lanes fit inside a world border of the given half size (at most {@link MegaGrid#MAX_PARTIES}): the largest {@code n} for which {@link #fits}
     * holds, or 0 when not even the shared lane of party 0 does (a border smaller than the Abyssal road has always been; nothing is cut off because of that).
     */
    public static int partiesThatFit(double halfSize) {
        int n = 0;
        while (n < MegaGrid.MAX_PARTIES && fits(n + 1, halfSize)) n++;
        return n;
    }

    /** The road a z belongs to: the Abyssal lanes are all at or beyond the middle between the two groups, the Holy lanes before it; null never (every z has an answer). */
    public static Polarity roadAt(int z) { return z >= MegaGrid.LANE_Z / 2 ? Polarity.ABYSSAL : Polarity.HOLY; }

    /**
     * Does a region that spans {@code minZ .. maxZ} lie wholly in the band of party {@code party}'s lane (within half a {@link MegaGrid#PARTY_STEP} of it, on its road)? A
     * floor of the floor model always does. A sector of the old ten-floor model, whose chain winds through the dimension by the planner's choice, does unless it wandered more
     * than half a step sideways, and then it could meet the floors of the next party: the startup check and {@code party.lanes} use this to say so.
     */
    public static boolean insideBand(int minZ, int maxZ, int party) {
        return partyAt(minZ, MegaGrid.MAX_PARTIES) == party && partyAt(maxZ, MegaGrid.MAX_PARTIES) == party;
    }

    /**
     * The party whose lane is nearest to {@code z} on its road, or -1 when that is farther from party 0 than the last of {@code maxParties} parties' lanes plus half a
     * step (the z is then in nobody's lane). A z belongs to the lane within half a {@link MegaGrid#PARTY_STEP} of it, so there is no gap and no overlap.
     */
    public static int partyAt(int z, int maxParties) {
        int limit = Math.max(1, Math.min(maxParties, MegaGrid.MAX_PARTIES));
        long fromZero = roadAt(z) == Polarity.ABYSSAL ? (long) z - MegaGrid.LANE_Z : -(long) z;     // distance along the road's lane group: party k is at k * PARTY_STEP
        if (fromZero < -MegaGrid.PARTY_STEP / 2) return -1;
        long k = (fromZero + MegaGrid.PARTY_STEP / 2) / MegaGrid.PARTY_STEP;
        return k >= limit ? -1 : (int) k;
    }
}
