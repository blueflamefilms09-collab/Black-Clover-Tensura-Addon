package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

/**
 * Spirit Dive WIND, final design A (4.x, the owner picked "Version A" for both tiers): the pure part, unit tested without a game ({@code SpiritDiveWind4Test}).
 * <ul>
 *   <li><b>The 3D crown</b>: a real mesh (a thin ring with tall arrow-tip bars, 14 of them and a second lower ring for the character tier, 8 bars and one ring for the standard tier, each with a glow
 *       shell) floating {@link #HOVER} px above the head, turning slowly ({@link #SPIN} rad a tick), following the head's pitch only half ({@link #PITCH_FOLLOW}) so it floats instead of wobbling. Four
 *       meshes in {@code assets/nusmp/sdw4_meshes} (full and left half for the first dive), made by {@code tools/gen_sdw4_models.py}, read by {@link SpiritDiveWind3Mesh#getIn}.</li>
 *   <li><b>The calm steady state</b>: after the 22-tick entry the loud effects fade out and a quiet set stays (body light, crown, wings, wrist circle with a low pulse, sword at the hip, bow on the back,
 *       two sparkles, the ground ring dimmed). The loud effects (steady ribbons, the extra sparkles, the circle of crosses behind the back, the wrist sprite, a full strength ground ring) come back at full
 *       strength during the entry, the exit and for {@link #CAST_HOLD} ticks after a cast. {@link #energy} is that 1 (loud) .. 0 (calm) number; {@link #calmSpec} the quiet set.</li>
 * </ul>
 * Client option {@code spiritDive.calm} (default true) switches the calm set off (the previous 3.5 density, with the new crown).
 */
public final class SpiritDiveWind4Rules {
    private SpiritDiveWind4Rules() {}

    // ------------------------------------------------------------------ the crown
    /** Height of the ring above the head (head frame, px, y is DOWN): the head's top is -8 and the hat layer -8.5, so the ring floats about a hand's width (4 px) above the hat. */
    public static final float HOVER = -12.6f;
    /** The slow turn of the crown (rad a tick: one revolution in about 15 s), the bob (px) and its speed, the share of the head's pitch (x rotation) the crown follows. */
    public static final float SPIN = 0.021f, BOB = 0.45f, BOB_SPEED = 0.055f, PITCH_FOLLOW = 0.5f;
    /** Height of the soft glow disc under the ring (px above the ring, negative = up is -y), and its half size. */
    public static final float HALO_DY = 1.2f, HALO_SIZE = 10.0f;

    public static final String CROWN_YUNO = "crown_yuno", CROWN_STD = "crown_std", CROWN_YUNO_HALF = "crown_yuno_half", CROWN_STD_HALF = "crown_std_half";
    /** Every mesh of the final A (the generator writes exactly these). */
    public static final java.util.List<String> NAMES = java.util.List.of(CROWN_YUNO, CROWN_STD, CROWN_YUNO_HALF, CROWN_STD_HALF);

    public static String crownName(boolean character, boolean half) { return character ? (half ? CROWN_YUNO_HALF : CROWN_YUNO) : (half ? CROWN_STD_HALF : CROWN_STD); }

    public static SpiritDiveWind3Mesh crown(boolean character, boolean half) { return SpiritDiveWind3Mesh.getIn(SpiritDiveWind3Mesh.FOLDER4, crownName(character, half)); }

    /** The layers of the crown drawn: the character tier solid + glow + the lower ring, the standard tier solid + glow. */
    public static int crownMask(boolean character) { return character ? SpiritDiveWind3Rules.ALL : SpiritDiveWind3Rules.SOLID_GLOW; }

    /** Vertices of the 3D crown as drawn by the tier (half = the first dive of Wind). */
    public static int crownVerts(boolean character, boolean half) { return crown(character, half).vertsOf(crownMask(character)); }

    /** Vertices of the sprite crown of the pack look (what the 3D crown replaces at full detail). */
    public static int spriteCrownVerts(SpiritDiveWindRules.Spec p, boolean half) {
        return p.bars() <= 0 ? 0 : SpiritDiveWindGeometry.barCount(p.bars(), half) * SpiritDiveWindRules.SPRITE + p.rings() * SpiritDiveWindRules.BAND;
    }

    /** True when design A draws the 3D crown (full detail, a crowned stage); the mid distance keeps the cheap sprite crown. */
    public static boolean crown3d(Lod lod, Stage st) { return lod == Lod.FULL && SpiritDiveReelRules.crowned(Element.WIND, st); }

    /** Change in vertices of design A when the sprite crown of the pack look is swapped for the 3D crown (zero away from full detail). */
    public static int crownDelta(boolean character, Lod lod, Stage st) {
        if (!crown3d(lod, st)) return 0;
        boolean half = SpiritDivePieces.halfBody(Element.WIND, st);
        return crownVerts(character, half) - spriteCrownVerts(SpiritDiveWindRules.spec(character, lod), half);
    }

    // ------------------------------------------------------------------ the calm steady state
    /** Ticks the loud look stays after the entry is over before it has settled (a smooth fade), the ticks the loud look is held after a cast and the fade after that. */
    public static final float SETTLE_TICKS = 12f, CAST_HOLD = 40f, CAST_FADE = 20f;

    private static float smooth(float x) { x = x < 0f ? 0f : x > 1f ? 1f : x; return x * x * (3f - 2f * x); }

    /**
     * How loud the dive is, 1 (the full effects) .. 0 (the quiet steady state): 1 during the entry (the first 22 ticks), fading to 0 over {@link #SETTLE_TICKS} ticks after it; rising to 1 again as the exit
     * clock runs back ({@link SpiritDiveReelRules#clock} below 22); and back to 1 while a cast is going and for {@link #CAST_HOLD} ticks after it, fading over {@link #CAST_FADE}.
     *
     * @param t         ticks since the dive started
     * @param left      ticks left of the dive's window (the renderer's {@code left}; small at the end: the exit clock reads it)
     * @param sinceCast ticks since a cast was last seen (a huge number when none)
     */
    public static float energy(float t, float left, float sinceCast) {
        float u = SpiritDiveReelRules.clock(t, left);
        float settle = 1f - smooth((t - SpiritDiveReelRules.ENTRY_TICKS) / SETTLE_TICKS);
        float exit = 1f - smooth((u - (SpiritDiveReelRules.ENTRY_TICKS - 8f)) / 8f);
        float cast = sinceCast <= CAST_HOLD ? 1f : 1f - smooth((sinceCast - CAST_HOLD) / CAST_FADE);
        return Math.max(Math.max(settle, exit), cast);
    }

    /** Below this energy the quiet set is drawn (the loud effects have faded to nothing by then). */
    public static final float QUIET = 0.04f;

    /** The quiet set of a Spirit Dive Wind spec: no steady ribbons, no circle of crosses behind the back, a quarter of the sparkles (at least one when there were any). The rest is kept. */
    public static SpiritDiveWindRules.Spec calmSpec(SpiritDiveWindRules.Spec s) {
        return new SpiritDiveWindRules.Spec(s.bars(), s.rings(), s.leafSegs(), s.crosses(), s.wrists(), false, s.weapon(), s.sparks() == 0 ? 0 : Math.max(1, s.sparks() / 4), 0, 0, s.shellPasses());
    }

    /** Sparkles kept in the quiet set (the rest fade with the energy). */
    public static int calmSparks(SpiritDiveWindRules.Spec s) { return calmSpec(s).sparks(); }

    /** The sword's two crossed rings (its ornament layer) are drawn only while the dive is loud: in the quiet set the sword is the blade, guard, grip and its glow shell. */
    public static final boolean SWORD_RINGS_ONLY_LOUD = true;

    /** The alpha factor of the ground ring and of the wrist ring in the quiet set, and the pulse of the wrist ring (low: a breath, not a flash). */
    public static final float GROUND_CALM = 0.4f, WRIST_CALM = 0.5f, WRIST_PULSE = 0.1f;

    // ------------------------------------------------------------------ counting
    /**
     * Vertices of a Wind A dive that has passed its entry (no entry sprites), at a tier, level of detail and stage: {@code calm} false is the 3.5 density (steady ribbons, all sparkles, circle behind the
     * back, wrist sprites) with the new 3D crown; true is the quiet set. The bow is on the back (no arrow: a cast is loud and counted by {@link SpiritDiveWind3Rules#vertices}).
     */
    public static int steadyVertices(boolean character, Lod lod, Stage st, boolean calm) {
        if (lod == Lod.CULLED) return 0;
        SpiritDiveWindRules.Spec p = SpiritDiveWindRules.spec(character, lod);
        SpiritDiveWind3Rules.Spec s = SpiritDiveWind3Rules.spec(SpiritDiveWind3Rules.Ver.A, character, lod);
        SpiritDiveWindRules.Spec q = calm ? calmSpec(p) : p;
        boolean full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(Element.WIND, st);
        int v = SpiritDiveAnimeRules.shellVertices(Element.WIND, st, lod);
        if (SpiritDiveReelRules.crowned(Element.WIND, st) && p.bars() > 0) v += crown3d(lod, st) ? crownVerts(character, half) : spriteCrownVerts(p, half);
        v += Math.min(p.wrists(), SpiritDiveWindRules.arms(st)) * (SpiritDiveWindRules.RING_VERTS + (calm ? 0 : SpiritDiveWindRules.SPRITE));
        v += SpiritDiveWindRules.leaves(character, lod, st) * p.leafSegs() * 4 * 2 + SpiritDiveWindRules.crossesDrawn(character, lod, st) * SpiritDiveWindRules.SPRITE;
        if (full && q.backCircle()) v += SpiritDiveWindRules.RING_VERTS;
        if (full) v += (calm && s.sword() == 1 ? SpiritDiveWind3Rules.swordVertsCalm() : SpiritDiveWind3Rules.swordVerts(s.sword())) + (s.bow() > 0 ? SpiritDiveWind3Rules.bowVerts() : 0);
        if (full) v += q.sparks() * SpiritDiveWindRules.SPRITE + q.ribbons() * q.ribbonSegs() * 4;
        return v + SpiritDiveWindRules.RING_VERTS;
    }

    /** The worst steady vertices over the stages of a tier at full detail. */
    public static int worstSteady(boolean character, boolean calm) {
        int w = 0;
        for (Stage st : Stage.values()) w = Math.max(w, steadyVertices(character, Lod.FULL, st, calm));
        return w;
    }

    // ------------------------------------------------------------------ the cast clock
    /**
     * Ticks since a cast was last seen, per diver, for up to {@link #SLOTS} divers at once (an array keyed by entity id; the oldest slot is recycled): no map, no allocation per frame, render thread only.
     * Used by the painter to keep the loud look for {@link #CAST_HOLD} ticks after a cast.
     */
    public static final class CastClock {
        public static final int SLOTS = 16;
        private final int[] ids = new int[SLOTS];
        private final float[] last = new float[SLOTS];

        public CastClock() { clear(); }

        public void clear() {
            java.util.Arrays.fill(ids, Integer.MIN_VALUE);
            java.util.Arrays.fill(last, -1e9f);
        }

        /** Ticks since the diver {@code id} was last seen casting at the client clock {@code age}; {@code casting} records it now. A huge number when never (or when the clock went backwards). */
        public float since(int id, float age, boolean casting) {
            int slot = -1, oldest = 0;
            for (int i = 0; i < SLOTS; i++) {
                if (ids[i] == id) { slot = i; break; }
                if (last[i] < last[oldest]) oldest = i;
            }
            if (slot < 0) { slot = oldest; ids[slot] = id; last[slot] = -1e9f; }
            if (casting) last[slot] = age;
            float d = age - last[slot];
            if (d < 0f) { last[slot] = -1e9f; return 1e9f; }
            return Math.min(d, 1e9f);
        }
    }
}
