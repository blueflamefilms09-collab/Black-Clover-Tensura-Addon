package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;

/**
 * Mode Armour, VALKYRIE DRESS v2: the per-frame admission of the wearers (rule 11), like {@code WaterBc#admit} but with the armour's OWN pool of {@value #POOL_QUADS} mesh quads (4,800 vertices) for all wearers in
 * view together, whatever their number; the pool of the Water effects ({@code WaterBc.FRAME_MESH_CEILING}) is separate. Pure: the unit test pins it, the CI self-test repeats it on the dedicated server, the docs quote it.
 *
 * <p>Rules: the wearers are taken nearest first; each one tries, in this order, the best level its distance allows (NEAR inside 16 blocks, MID to 32, FAR to 64, nothing beyond) with the lance (only at NEAR
 * and MID, and only when the wearer wants one), then the same level without the lance, then the next level down, and so on; the first choice whose quads still fit under the running total is taken. A wearer that
 * fits at no level is not drawn. So the sum of the admitted quads can never pass the pool, and a crowd of wearers costs the pool, not more. Allocation free.</p>
 */
public final class ModeArmorValkyrieBudget {
    private ModeArmorValkyrieBudget() {}

    /** The pool on the HIGH quality level; the MEDIUM and LOW levels get {@link #poolFor}. */
    public static final int POOL_QUADS = 1200;
    /** Wearers considered in one frame (the nearest ones; the rest are not drawn). */
    public static final int MAX_WEARERS = 16;
    public static final int NONE = -1;

    /** The pool of a quality level, from its per-frame vertex allowance (VfxQuality.frameVertices: 40000 HIGH, 14000 MEDIUM, 6000 LOW): 1200, 720 or 360 quads. */
    public static int poolFor(int frameVertices) { return frameVertices >= 40000 ? POOL_QUADS : frameVertices >= 14000 ? 720 : 360; }

    /**
     * Admits the wearers. {@code distSq[i]} = squared distance of wearer i to the camera, {@code form[i]} = its Form ordinal, {@code wantsLance[i]} = it wears a lance; the results are
     * {@code levelOut[i]} (0 NEAR, 1 MID, 2 FAR or {@link #NONE}) and {@code lanceOut[i]}. {@code done} is scratch (at least n entries). Returns the admitted quads.
     */
    public static int admit(double[] distSq, int[] form, boolean[] wantsLance, int n, int[] levelOut, boolean[] lanceOut, boolean[] done, int pool) {
        int used = 0;
        for (int i = 0; i < n; i++) {
            done[i] = false;
            levelOut[i] = NONE;
            lanceOut[i] = false;
        }
        for (int rank = 0; rank < n; rank++) {
            int best = -1;
            for (int i = 0; i < n; i++) if (!done[i] && (best < 0 || distSq[i] < distSq[best])) best = i;
            done[best] = true;
            used += place(distSq[best], Form.values()[form[best]], wantsLance[best], used, pool, levelOut, lanceOut, best);
        }
        return used;
    }

    /** The first choice of one wearer that fits {@code pool - used}; returns its quads (0 = not drawn) and writes the level and lance of {@code slot}. */
    public static int place(double distSq, Form f, boolean wantsLance, int used, int pool, int[] levelOut, boolean[] lanceOut, int slot) {
        int top = ModeArmorValkyrieRules.levelOf(distSq);
        if (top < 0) return 0;
        for (int level = top; level <= 2; level++) {
            int armour = ModeArmorValkyrieRules.armourQuads(f, level);
            if (wantsLance && ModeArmorValkyrieRules.lanceAt(level) && used + armour + ModeArmorValkyrieRules.lanceQuads(level) <= pool) {
                levelOut[slot] = level;
                lanceOut[slot] = true;
                return armour + ModeArmorValkyrieRules.lanceQuads(level);
            }
            if (used + armour <= pool) {
                levelOut[slot] = level;
                lanceOut[slot] = false;
                return armour;
            }
        }
        return 0;
    }

    /** The worst case of one wearer at a level (armour + lance where it is drawn). */
    public static int wearerQuads(Form f, int level, boolean lance) { return ModeArmorValkyrieRules.armourQuads(f, level) + (lance ? ModeArmorValkyrieRules.lanceQuads(level) : 0); }
}
