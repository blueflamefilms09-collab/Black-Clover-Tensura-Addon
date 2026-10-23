package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.FuegoleonArmRules.View;

/**
 * Fuegoleon's flame arm in the Djinn-Equip way (3.1): the pure decisions of the new look, unit tested by FuegoleonArmDjinnRulesTest (no game classes, so the server and the tests can read it).
 *
 * <p><b>What the look is.</b> The owner's reference art shows the right arm below the shoulder as a solid, sculpted limb of flame (no armour): a bright orange body with a hot yellow core, a dark
 * red outline and flat cel bands, a shoulder cap, upper arm, elbow, forearm, wrist and a clenched fist, with flame tongues curling off the shoulder, the elbow (one big hooked tongue) and the
 * wrist, and a few drifting sparks. What it takes from the Djinn Equip is the way the Equip is built and shown: a SET OF PIECES (shoulder, upper arm, elbow, forearm, fist), each anchored on the
 * arm and each forged in after the other (the same {@code EquipAnimation} the Djinn armour uses) with a flash, here as a flame that kindles from the shoulder down to the fist.</p>
 *
 * <p><b>What the server decides is unchanged</b> ({@link FuegoleonArmRules}, {@code book.FuegoleonArm}: the {@code Aura.FUEGOLEON_ARM} lease); this class is only what the CLIENT draws, chosen by the
 * viewer's settings: the look ({@link Look}), the view ({@link View}), the strength ({@link State}), the level of detail and every budget of the new painter.</p>
 */
public final class FuegoleonArmDjinnRules {
    private FuegoleonArmDjinnRules() {}

    // ------------------------------------------------------------------ which look
    /** The looks of the arm a viewer can pick ({@code fuegoleonArm.look}): the flame limb (default), the 3.0.0 arm (molten box model, flame sheets, a mane of strands) or the plain molten model alone. */
    public enum Look { DJINN, V3, CLASSIC }

    public static final Look DEFAULT_LOOK = Look.DJINN;

    /** The look for a configured value: an unknown or missing one gives the default. */
    public static Look pick(Look configured) { return configured == null ? DEFAULT_LOOK : configured; }

    /** The look named by a text of the config or the command line (any case); the default for anything else. */
    public static Look parse(String name) {
        if (name != null) for (Look l : Look.values()) if (l.name().equalsIgnoreCase(name.trim())) return l;
        return DEFAULT_LOOK;
    }

    /** Whether the look draws the Djinn-style flame limb (so the dive's own arm and the old model stay out of its way). */
    public static boolean drawsLimb(Look look) { return pick(look) == Look.DJINN; }

    /** Whether the look draws the flame sheets and the mane strands of the 3.0.0 arm: V3 does, CLASSIC (the plain molten model) and DJINN do not. */
    public static boolean drawsSheets(Look look) { return pick(look) == Look.V3; }

    // ------------------------------------------------------------------ the pieces and their forge-in
    /** The pieces of the limb, shoulder to fist, in the order they are forged in; {@link #PIECE_FILES} are the model names of the {@code fuegoleonarm} geo key. */
    public enum Piece { SHOULDER, UPPER, ELBOW, FORE, FIST }

    public static final String GEO_KEY = "fuegoleonarm";
    public static final String[] PIECE_FILES = {"shoulder", "upper", "elbow", "fore", "fist"};

    /** The stagger of each piece's forge-in, in ticks after the arm is lit (the Djinn's rule: 0 to 12 ticks, each piece takes {@code EquipAnimation.FORGE_TICKS} = 8). The last piece is whole at tick 20. */
    public static final float[] PIECE_DELAY = {0f, 3f, 6f, 9f, 12f};
    public static final float FORGE_TICKS = 8f;

    /** Ticks from the lighting of the arm to its last piece being whole. */
    public static float forgeTotalTicks() { return PIECE_DELAY[PIECE_DELAY.length - 1] + FORGE_TICKS; }

    /** The pieces forge in from the shoulder side, shifted this far (model pixels, y down: negative = from above) before they settle. */
    public static final float[] PIECE_FROM_Y = {-2f, -4f, -5f, -6f, -7f};

    /** The flash of a piece being forged: 0 before and after, a bump peaking when the piece is a third assembled (0..1). {@code assembled} is {@code EquipAnimation.assembled}'s value. */
    public static float flash(float assembled) {
        if (assembled <= 0f || assembled >= 1f) return 0f;
        float x = assembled;
        return x < 0.35f ? x / 0.35f : 1f - (x - 0.35f) / 0.65f;
    }

    // ------------------------------------------------------------------ the strengths
    /** Quiet at rest (the contract is held), OUT when the Salamander is out (a brighter, longer flame), FLARE while the wearer casts or the Fire Spirit Dive runs. */
    public enum State { QUIET, OUT, FLARE }

    public static State state(boolean salamanderOut, boolean casting, boolean diving) {
        if (diving || casting) return State.FLARE;
        return salamanderOut ? State.OUT : State.QUIET;
    }

    /** 0 = nothing is drawn: the viewer turned the arm off, or the view is FLARE and the arm is only at rest. Otherwise 1 (the limb itself is solid at every strength; the numbers below do the rest). */
    public static float visible(View view, State s) {
        if (view == null || view == View.OFF) return 0f;
        if (view == View.FLARE && s == State.QUIET) return 0f;
        return 1f;
    }

    /** The glow pass of the limb and the haze: dim at rest, full in the flare. */
    public static float glow(State s) { return switch (s) { case QUIET -> 0.55f; case OUT -> 0.8f; case FLARE -> 1.0f; }; }

    /** The haze behind the arm (alpha at the middle). */
    public static float haze(State s) { return switch (s) { case QUIET -> 0.07f; case OUT -> 0.12f; case FLARE -> 0.2f; }; }

    /** How long the tongues are, as a share of their table length. */
    public static float tongueLength(State s) { return switch (s) { case QUIET -> 0.62f; case OUT -> 0.9f; case FLARE -> 1.25f; }; }

    /** How many tongues of the table (best first) are drawn at a strength, and how many sparks drift. */
    public static int tongueCount(State s) { return switch (s) { case QUIET -> 6; case OUT -> TONGUES; case FLARE -> TONGUES; }; }

    public static int sparkCount(State s) { return switch (s) { case QUIET -> 2; case OUT -> 5; case FLARE -> MAX_SPARKS; }; }

    /** The speed of the flicker of the tongue frames (frames a tick) and of the sway. */
    public static final float FRAME_SPEED = 0.42f, SWAY_SPEED = 0.17f;

    // ------------------------------------------------------------------ the tongue table
    public static final int TONGUES = 10, TONGUE_SEGMENTS = 3, FRAMES = 4, MAX_SPARKS = 8, MAX_FLASHES = 5, HAZE_QUADS = 1;

    /**
     * The flame tongues, best first (so a lower strength or detail level draws the first n). Each row: root x, y, z (arm frame, model pixels, y DOWN from the shoulder pivot, -x outward for the right
     * arm, +z backward), first heading dx, dy, dz, a curl (cx, cy, cz) that bends the heading as the tongue grows (the hook), length, width, the piece it belongs to ({@link Piece} ordinal: it grows
     * with that piece's forge-in) and a phase. Read by the painter every frame, never changed.
     */
    public static final float[][] TONGUE = {
            // x      y     z      dx    dy    dz     cx    cy    cz    len   wid   piece phase
            {-3.8f, 6.0f, 0.0f, -1.0f, -0.15f, 0.0f, 0.2f, -1.7f, 0.3f, 10f, 4.4f, 2, 0.0f},      // 0 the big hooked tongue off the elbow: out, then up and curling back
            {-1.5f, -3.0f, 0.0f, -0.3f, -1.0f, 0.0f, -1.5f, 0.2f, 0.0f, 8f, 4.0f, 0, 1.3f},      // 1 up off the shoulder, curling outward
            {-3.6f, 8.5f, 0.0f, -0.6f, -1.0f, 0.0f, -0.9f, 0.2f, 0.0f, 7f, 3.6f, 3, 2.1f},        // 2 wavy off the forearm, outer side
            {-2.5f, 11.6f, -2.0f, -0.3f, -1.0f, -0.2f, -0.6f, 0.0f, -0.4f, 7f, 4.2f, 4, 0.7f},    // 3 off the wrist and knuckles
            {1.0f, -2.2f, 1.5f, 0.2f, -1.0f, 0.5f, 0.6f, 0.0f, 1.2f, 8f, 5.0f, 0, 3.0f},          // 4 up and back off the shoulder
            {1.8f, 6.2f, 2.0f, 0.4f, -0.3f, 1.0f, 0.0f, -1.2f, 0.5f, 8f, 4.2f, 2, 2.6f},          // 5 trailing back off the elbow
            {1.5f, 9.0f, 0.0f, 0.5f, -1.0f, 0.0f, 0.7f, 0.0f, 0.0f, 7f, 3.6f, 3, 4.0f},           // 6 wavy off the forearm, inner side
            {-4.0f, 3.0f, 0.5f, -1.0f, -0.3f, 0.0f, -0.2f, -1.4f, 0.0f, 7f, 4.2f, 1, 1.9f},        // 7 a small lick off the upper arm
            {-1.0f, 9.5f, 2.8f, 0.0f, -0.6f, 1.0f, 0.0f, -1.2f, 0.6f, 7f, 3.6f, 3, 3.6f},         // 8 trailing back off the forearm
            {0.5f, 13.4f, -3.0f, 0.0f, -1.0f, -0.3f, 0.3f, 0.0f, -0.5f, 6f, 4.0f, 4, 5.1f},        // 9 a small one from the knuckles
    };
    public static final int T_X = 0, T_Y = 1, T_Z = 2, T_DX = 3, T_DY = 4, T_DZ = 5, T_CX = 6, T_CY = 7, T_CZ = 8, T_LEN = 9, T_WID = 10, T_PIECE = 11, T_PHASE = 12;

    /** Where on the arm each piece's centre is (arm frame, model pixels, y down): the forge flash and the grow-in are about this point. x, y, z. */
    public static final float[][] PIECE_CENTRE = {
            {-1f, -0.3f, 0f}, {-1f, 3.0f, 0f}, {-1f, 6.0f, 0f}, {-1f, 8.8f, 0f}, {-1f, 12.2f, -0.6f}};

    // ------------------------------------------------------------------ level of detail and the per-frame cap
    public enum Lod { FULL, HALF, CULLED }

    /** Up to this many blocks the whole look (FULL), up to the second the limb with a few tongues and no glow pass (HALF); beyond it nothing (the effects of far players are skipped). */
    public static final double NEAR = 24.0, FAR = 48.0;

    public static Lod lod(double distSq) {
        if (distSq <= NEAR * NEAR) return Lod.FULL;
        if (distSq <= FAR * FAR) return Lod.HALF;
        return Lod.CULLED;
    }

    /** How many arms one frame may draw at the full level (the rest of a crowd of Fuegoleons draw HALF). The wearer's own first person arm is not counted. */
    public static final int FULL_PER_FRAME = 4;

    /**
     * The per-frame cap of full arms (pure, one instance per painter, render thread only). A frame is told by its clock {@code key} (game time and partial tick) or, when the clock does not move (the
     * game is paused), by the first painter coming round again: the first arm of a frame starts the count over, so a paused crowd keeps the same {@link #FULL_PER_FRAME} full arms instead of losing all of them.
     */
    public static final class Cap {
        private long key = Long.MIN_VALUE;
        private int firstId = Integer.MIN_VALUE, used;

        /** May the arm of entity {@code id} draw at the full level in the frame {@code key}? */
        public boolean admit(long frameKey, int id) {
            if (frameKey != key || id == firstId) {
                key = frameKey;
                firstId = id;
                used = 0;
            }
            if (used >= FULL_PER_FRAME) return false;
            used++;
            return true;
        }
    }

    public static int tonguesAt(State s, Lod lod) {
        if (lod == Lod.CULLED) return 0;
        int n = tongueCount(s);
        return lod == Lod.HALF ? Math.min(n, 5) : n;
    }

    public static int segmentsAt(Lod lod) { return lod == Lod.HALF ? 2 : TONGUE_SEGMENTS; }

    public static int sparksAt(State s, Lod lod) { return lod == Lod.FULL ? Math.min(sparkCount(s), MAX_SPARKS) : 0; }

    // ------------------------------------------------------------------ budgets (pinned by the test and the doc)
    /** The most cubes of the limb's models (all five pieces); 24 vertices a cube, body pass and glow pass. The test counts the cubes of the real files. */
    public static final int LIMB_CUBES_MAX = 26;

    /** The vertices of the Djinn's arm assembly the limb is held under: pauldron + vambrace + gauntlet of a unique set = 151 + 100 + 80 quads = 331 quads = 1324 vertices. */
    public static final int DJINN_ARM_VERTICES = 1324;

    /** Quads of the procedural part at the full level: tongues x segments, sparks, the haze, the forge flashes. */
    public static int worstQuads() { return TONGUES * TONGUE_SEGMENTS + MAX_SPARKS + HAZE_QUADS + MAX_FLASHES; }

    /** Vertices of one arm at the full level for a limb of {@code cubes} cubes (two passes: body and glow) plus the procedural quads. */
    public static int worstVertices(int cubes) { return cubes * 24 * 2 + worstQuads() * 4; }

    /** Packets per wearer: the lease of the arm is renewed at most this often (the server rule is unchanged). */
    public static double packetsPerSecond() { return FuegoleonArmRules.packetsPerSecond(); }
}
