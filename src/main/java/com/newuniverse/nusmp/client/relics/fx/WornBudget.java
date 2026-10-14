package com.newuniverse.nusmp.client.relics.fx;

/**
 * The vertex budget of one player's worn relics in one frame. The owner's worst case is about 6000 vertices per player in view; the cap comes from the quality
 * option ({@link FxQuality#vertexCap}). Pieces are given best first (highest grade first): each model is drawn while it fits, then the extras (shell pass,
 * motes, rings) are granted in the same order while they fit. Pure.
 */
public final class WornBudget {
    private WornBudget() {}

    /**
     * @param base   vertices of each relic's model (all passes of the model itself)
     * @param extra  vertices of each relic's extras
     * @param cap    the most vertices in all
     * @param drawn  out: true if the relic's model is drawn
     * @param extras out: true if the relic's extras are drawn (never without the model)
     * @return the vertices that will be drawn
     */
    public static int allocate(int[] base, int[] extra, int cap, boolean[] drawn, boolean[] extras) {
        return allocate(base, extra, base.length, cap, drawn, extras);
    }

    /** The same for the first {@code n} entries of arrays that may be longer (the draw loop reuses fixed arrays). */
    public static int allocate(int[] base, int[] extra, int n, int cap, boolean[] drawn, boolean[] extras) {
        int used = 0;
        for (int i = 0; i < n; i++) {
            drawn[i] = used + base[i] <= cap;
            extras[i] = false;
            if (drawn[i]) used += base[i];
        }
        for (int i = 0; i < n; i++) {
            if (drawn[i] && used + extra[i] <= cap) {
                extras[i] = true;
                used += extra[i];
            }
        }
        return used;
    }
}
