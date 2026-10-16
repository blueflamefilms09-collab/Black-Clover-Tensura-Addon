package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: the 3D portal is one design drawn at three detail tiers (near and far, full and reduced particles, and a tier small enough
 * for the shared 400-vertex effect buffer). A spec is just the counts; {@link PortalMesh} builds the tables from it once and
 * {@link PortalComposer} turns it into quads, so detail is a data choice and the vertex budget of every tier can be tested.
 *
 * @param ringSegments   runic segments on the outer ring (and the soft band under them when {@code ringBase})
 * @param ringBase       whether the soft band under the runic ring is drawn
 * @param shells         nested depth shells (3..5)
 * @param shellSectors   sectors round each shell
 * @param horizonRings   rings of the event-horizon disc
 * @param horizonSectors sectors round the horizon disc, the dark backdrop and the rim glow
 * @param bolts          rim-lightning arcs (some are dark each step)
 * @param boltSegments   quads in one arc
 * @param threadSegments quads in the thread between a pair (drawn by one gate of the pair)
 */
public record PortalSpec(int tier, int ringSegments, boolean ringBase, int shells, int shellSectors, int horizonRings, int horizonSectors,
                         int bolts, int boltSegments, int threadSegments) {

    /** Full, reduced and small. Tier 2 including its thread fits the 400-vertex buffer. */
    public static final PortalSpec[] TIERS = {
            new PortalSpec(0, 24, true, 5, 20, 3, 32, 8, 3, 8),
            new PortalSpec(1, 16, true, 4, 14, 2, 20, 5, 2, 6),
            new PortalSpec(2, 12, false, 3, 8, 2, 10, 3, 2, 6)
    };

    public static PortalSpec tier(int t) { return TIERS[Math.max(0, Math.min(TIERS.length - 1, t))]; }

    /** Quads of the gate itself: backdrop, shells, horizon, rim glow, runes, base band, bolts (every bolt counted lit). */
    public int gateQuads() {
        return horizonSectors                     // backdrop
                + shells * shellSectors
                + horizonRings * horizonSectors
                + horizonSectors                  // rim glow
                + ringSegments                    // runes
                + (ringBase ? ringSegments : 0)
                + bolts * boltSegments;
    }

    public int gateVertices() { return gateQuads() * 4; }

    /** Upper bound for gate plus thread. */
    public int maxVertices() { return (gateQuads() + threadSegments) * 4; }

    /** The highest-detail tier (starting at {@code wanted}) whose worst case fits {@code vertexBudget}. */
    public static PortalSpec fitting(int wanted, int vertexBudget) {
        int t = Math.max(0, Math.min(TIERS.length - 1, wanted));
        while (t < TIERS.length - 1 && TIERS[t].maxVertices() > vertexBudget) t++;
        return TIERS[t];
    }
}
