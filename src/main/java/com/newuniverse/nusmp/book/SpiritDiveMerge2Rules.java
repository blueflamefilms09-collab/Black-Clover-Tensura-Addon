package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

import java.util.ArrayList;
import java.util.List;

/**
 * Spirit Dive MERGE 2 (the owner asked for four improvements), the pure rules, common code with no Minecraft class so the tests, the self-test and the preview scripts count the numbers the client draws:
 * <ol>
 *   <li><b>Slim spirit models</b> ({@link #slimMesh}): smooth low-poly meshes (tapered limbs, curved bodies, real wings, fins, tail) in {@code assets/nusmp/sdm2_meshes} made by
 *       {@code tools/gen_sdm2_models.py}, a solid layer and a glow shell; the old boxy meshes ({@code sdm_meshes}) stay and come back with the client option {@code spiritDive.mergeModels = BOXY}.</li>
 *   <li><b>Calm entry</b> ({@link #flashAlpha}, {@link #flashSizeK}, {@link #ribbonK}): the look's own entry (white flash sphere, spiral ribbons, cage of circles) is turned down to a third while the
 *       spirit merges (clock 8..17) so the merge reads; the flash at the very start (0..4) and a short final burst after the contact (17..20) stay. Option {@code spiritDive.calmEntry}.</li>
 *   <li><b>First person merge</b> ({@link #fpPath}, {@link #fpScale}, {@link #fpFlashRadius}): the wearer sees the spirit come in at the upper side of the view and flow down into the chest, curving past
 *       the bottom edge of the screen; every size is clamped so the spirit never covers more than {@link #FP_MAX_COVERAGE} of the screen. Option {@code spiritDive.mergeFirstPerson}.</li>
 *   <li><b>Smaller, more distinct crowns</b> ({@link #crownName}, {@link #CROWN_K}): about 62 percent of the old width and height, closer to the head, element specific shapes in
 *       {@code sdm2_meshes/crown_*} made by {@code tools/gen_sdm2_crowns.py}. Option {@code spiritDive.crownDistinct}.</li>
 * </ol>
 */
public final class SpiritDiveMerge2Rules {
    private SpiritDiveMerge2Rules() {}

    // ------------------------------------------------------------------ the options
    /** The client option {@code spiritDive.mergeModels}: which spirit meshes the merge draws. */
    public enum Models { SLIM, BOXY }

    public static final Models DEFAULT_MODELS = Models.SLIM;
    public static final boolean DEFAULT_CALM_ENTRY = true, DEFAULT_FIRST_PERSON = true, DEFAULT_CROWN = true;

    public static final String FOLDER = "sdm2_meshes";

    // ------------------------------------------------------------------ 1: the slim spirit models
    /** The vertex budgets of a slim ghost (solid + glow): the character tier, the standard tier, the faint inner silhouette (full level of detail only). */
    public static final int CHAR_VERTS = 900, STD_VERTS = 500, CORE_VERTS = 300;
    /** The ghost is drawn in two additive layers: the solid body at this share of the ghost's alpha, the soft glow shell at this share. */
    public static final float SOLID_K = 0.92f, GLOW_K = 0.42f;

    /** The name of the slim ghost mesh: {@code <element>_char} or {@code <element>_std} (Earth has no character mesh). */
    public static String slimMesh(Element e, boolean character) {
        return SpiritDiveMergeRules.name(e) + (character && SpiritDiveMergeRules.hasCharacterMesh(e) ? "_char" : "_std");
    }

    public static String slimCore(Element e) { return SpiritDiveMergeRules.name(e) + "_core"; }

    public static SpiritDiveWind3Mesh mesh(String name) { return SpiritDiveWind3Mesh.getIn(FOLDER, name); }

    /** Every slim spirit mesh file (the generator writes exactly these). */
    public static List<String> meshes() {
        List<String> l = new ArrayList<>();
        for (Element e : Element.values()) {
            l.add(slimMesh(e, false));
            if (SpiritDiveMergeRules.hasCharacterMesh(e)) l.add(slimMesh(e, true));
            l.add(slimCore(e));
        }
        return l;
    }

    /** Vertices of the ghost mesh drawn for a tier with the chosen models (both layers of the slim mesh; the boxy mesh is one layer). */
    public static int ghostVerts(Element e, boolean character, Models models) {
        return models == Models.SLIM ? mesh(slimMesh(e, character)).totalVerts() : SpiritDiveMergeMesh.get(SpiritDiveMergeRules.ghostMesh(e, character)).totalVerts();
    }

    public static int coreVerts(Element e, Models models) {
        return models == Models.SLIM ? mesh(slimCore(e)).totalVerts() : SpiritDiveMergeMesh.get(SpiritDiveMergeRules.coreMesh(e)).totalVerts();
    }

    /** The budget of a ghost for a tier. */
    public static int ghostBudget(boolean character) { return character ? CHAR_VERTS : STD_VERTS; }

    /**
     * {@link SpiritDiveMergeRules#vertices} for the chosen models: the ghost mesh and the strips and the contact sprites and the cover while it flows, the inner silhouette once it has started (FULL only).
     */
    public static int vertices(Element e, boolean character, Lod lod, float m, boolean ghostRange, Models models) {
        if (lod == Lod.CULLED) return 0;
        int v = 0;
        boolean sprites = SpiritDiveMergeRules.flash(m) > 0.01f || SpiritDiveMergeRules.ring(m) > 0.01f, cover = SpiritDiveMergeRules.bodyGlow(m) > 0.01f;
        if (ghostRange && SpiritDiveMergeRules.flowing(m)) {
            v += ghostVerts(e, character, models) + SpiritDiveMergeRules.strips(character) * SpiritDiveMergeRules.STRIP_SEGS * 4;
            if (sprites) v += SpiritDiveMergeRules.FLASH_SPRITES * 4;
            if (cover) v += SpiritDiveMergeRules.COVER_VERTS;
        } else if (ghostRange && (sprites || cover)) {
            if (sprites) v += SpiritDiveMergeRules.FLASH_SPRITES * 4;
            if (cover) v += SpiritDiveMergeRules.COVER_VERTS;
        }
        if (lod == Lod.FULL && m > 15.05f) v += coreVerts(e, models);
        return v;
    }

    /** The worst case of one diver over the whole clock with the chosen models. */
    public static int worst(Element e, boolean character, Models models) {
        int w = 0;
        for (float m = 0f; m <= SpiritDiveMergeRules.ENTRY_TICKS; m += 0.25f) w = Math.max(w, vertices(e, character, Lod.FULL, m, true, models));
        return w;
    }

    // ------------------------------------------------------------------ 2: the calm entry
    /** The window of the merge in which the look's own entry is turned down, and the strength it is turned down to (a third). */
    public static final float WINDOW_FROM = 8f, WINDOW_TO = 17f, CALM = 0.30f;
    /** The flash at the very start stays full until this clock, then fades to {@link #CALM} by {@link #WINDOW_FROM}. */
    public static final float START_FULL = 4f;
    /** The short final burst after the contact (clock {@link #BURST_AT} +- {@link #BURST_HALF}, so inside 17..20) and its top strength and size. */
    public static final float BURST_AT = 18.4f, BURST_HALF = 1.4f, BURST_ALPHA = 0.9f, BURST_SIZE = 0.9f;

    private static float smooth(float x) { return SpiritDiveMergeRules.smooth(x); }

    /** 1 at the start (clock up to 4), falling to {@link #CALM} by clock 8 and staying there. */
    public static float startK(float u) { return 1f - (1f - CALM) * smooth((u - START_FULL) / (WINDOW_FROM - START_FULL)); }

    /** The final burst, 0..1: a bell round {@link #BURST_AT}, zero outside 17..19.8. */
    public static float burst(float u) { float d = 1f - Math.abs(u - BURST_AT) / BURST_HALF; return d <= 0f ? 0f : d; }

    /**
     * The alpha of the entry's white flash for the clock {@code u}: {@code flash} and {@code reveal} are the look's own bells ({@link SpiritDiveReelRules#flash}, {@link SpiritDiveReelRules#reveal}). Full
     * for clock 0..4, a third in the merge window (8..17), the final burst after it.
     */
    public static float flashAlpha(float u, float flash, float reveal) {
        return Math.max(Math.max(flash * startK(u), reveal * CALM), burst(u) * BURST_ALPHA);
    }

    /** The multiplier of the flash's size (radius): 1 at the start, {@link #CALM}-ish (0.35) in the window, {@link #BURST_SIZE} for the burst. */
    public static float flashSizeK(float u) {
        float w = 1f - (1f - 0.35f) * smooth((u - START_FULL) / (WINDOW_FROM - START_FULL));
        float b = burst(u);
        return b > 0f ? Math.max(w, 0.35f + (BURST_SIZE - 0.35f) * smooth(b * 1.2f)) : w;
    }

    /** The multiplier of the spiral ribbons' and the cage of circles' alpha: 1 up to clock 6, {@link #CALM} from 8 on (they fade out by themselves from 13). */
    public static float ribbonK(float u) { return 1f - (1f - CALM) * smooth((u - 6f) / 2f); }

    // ------------------------------------------------------------------ 3: the first person merge
    /** At most this share of the screen (area of the bounding square of anything drawn for the merge) may be covered. */
    public static final float FP_MAX_COVERAGE = 0.35f;
    /** The spirit's size in the view (blocks across its longest side before the share of the clock) and the top alpha (a share of the third person ghost). */
    public static final float FP_SIZE = 0.8f, FP_ALPHA_K = 0.6f, FP_FLASH_ALPHA = 0.55f;
    /** Camera space (x to the left, y up, z forward, blocks): where the spirit appears (upper side, in front) and the chest (below the eyes, just behind the near plane). */
    public static final float FP_START_X = 0.72f, FP_START_Y = 0.46f, FP_START_Z = 1.45f, FP_CHEST_X = 0f, FP_CHEST_Y = -0.78f, FP_CHEST_Z = 0.16f;

    /** The place of the spirit in camera space at the clock {@code m} as {@code out[0..2] = x (left), y (up), z (forward)}; {@code side} is +1 or -1 ({@link SpiritDiveMergeRules#side}). */
    public static void fpPath(float m, float side, float[] out) {
        float a = m < SpiritDiveMergeRules.APPEAR_END ? smooth(m / SpiritDiveMergeRules.APPEAR_END) : 1f;
        float e = SpiritDiveMergeRules.flowEase(m);
        float x = side * FP_START_X, y = FP_START_Y + 0.12f * (1f - a), z = FP_START_Z;
        float arc = (float) Math.sin(Math.PI * e);
        out[0] = x * (1f - e) + FP_CHEST_X * e + side * 0.30f * arc;
        out[1] = y * (1f - e) + FP_CHEST_Y * e + 0.10f * arc;
        out[2] = z * (1f - e) + FP_CHEST_Z * e;
    }

    /** The share of the screen the square of side {@code extent} (blocks) covers at {@code dist} blocks for a vertical field of view {@code fovDeg} and an aspect ratio. */
    public static float coverage(float extent, float dist, float fovDeg, float aspect) {
        float h = 2f * Math.max(dist, 0.05f) * (float) Math.tan(Math.toRadians(Math.max(5f, Math.min(fovDeg, 170f))) / 2.0);
        return extent * extent / (h * h * Math.max(aspect, 0.5f));
    }

    /** The largest extent (blocks) that keeps {@link #coverage} at or below the cap. */
    public static float maxExtent(float dist, float fovDeg, float aspect) {
        float h = 2f * Math.max(dist, 0.05f) * (float) Math.tan(Math.toRadians(Math.max(5f, Math.min(fovDeg, 170f))) / 2.0);
        return (float) Math.sqrt(FP_MAX_COVERAGE * h * h * Math.max(aspect, 0.5f));
    }

    /** The extent (blocks across) of the spirit at the clock {@code m}, at {@code dist} blocks from the eye: its size shrinks as it flows in, and is clamped to the coverage cap. */
    public static float fpExtent(float m, float dist, float fovDeg, float aspect, boolean character) {
        float base = FP_SIZE * (character ? 1.2f : 1f) * SpiritDiveMergeRules.ghostScale(m);
        return Math.min(base, maxExtent(dist, fovDeg, aspect));
    }

    /** The scale of the (32 px) mesh for that extent: px to blocks, so a mesh of 32 px is {@code extent} blocks across. */
    public static float fpScale(float extent) { return extent / 32f; }

    /** The alpha of the spirit in first person: the third person ghost's alpha, a share of it, faded out as it nears the eye (so it never blocks the view). */
    public static float fpAlpha(float m, float dist) {
        float near = smooth((dist - 0.35f) / 0.5f);
        return SpiritDiveMergeRules.ghostAlpha(m) * SpiritDiveMergeRules.GHOST_ALPHA * FP_ALPHA_K * near;
    }

    /** The radius (blocks) of the soft flash at the chest, clamped so its square never covers more than the cap at that distance. */
    public static float fpFlashRadius(float m, float dist, float fovDeg, float aspect) {
        float want = 0.16f + 0.30f * SpiritDiveMergeRules.flash(m);
        return Math.min(want, maxExtent(dist, fovDeg, aspect) * 0.5f);
    }

    /** The alpha of that flash: the contact bell, at most {@link #FP_FLASH_ALPHA}. */
    public static float fpFlashAlpha(float m) { return FP_FLASH_ALPHA * SpiritDiveMergeRules.flash(m); }

    /** Vertices drawn in first person at the clock {@code m}: the slim (or boxy) ghost while it flows, the flash sprite at the contact; never the inner silhouette. */
    public static int fpVertices(Element e, boolean character, float m, Models models) {
        int v = 0;
        if (SpiritDiveMergeRules.flowing(m)) v += ghostVerts(e, character, models);
        if (SpiritDiveMergeRules.flash(m) > 0.01f) v += 4;
        return v;
    }

    // ------------------------------------------------------------------ 4: the crowns
    /** The share of the old crown's width and height the new crowns have; the old crowns drawn by sprites or cubes (looks B, C, PACK, ANIME) are scaled by it about the head's top. */
    public static final float CROWN_K = 0.62f;
    /** The head's top in the head frame (px, y down): the pivot of the scale. */
    public static final float HEAD_TOP = -8f;
    /** The new mesh crown (head frame, px, y down): the ring floats this high (the old crown: -12.6), a hair of bob, no spin (the front bar stays at the front), the share of the head's pitch it follows. */
    public static final float HOVER = -10.4f, BOB = 0.3f, BOB_SPEED = 0.055f, SPIN = 0f, PITCH_FOLLOW = 0.5f;
    /** The old Wind A crown's ring radius and tallest bar (px) and the new one's, for the tests. */
    public static final float OLD_RING = 5.3f, OLD_TALL = 12.8f, NEW_RING = 3.3f, NEW_TALL = 9.4f;
    /** Bars: standard / character; the half crown (first dive of Wind) keeps the left half. */
    public static final int BARS_STD = 5, BARS_CHAR = 7;
    /** The vertex budgets of a crown mesh: the standard tier, the character tier. */
    public static final int CROWN_STD_VERTS = 480, CROWN_CHAR_VERTS = 620;

    /** The crown mesh name: {@code crown_<element>_<char|std>[_half]}; the half crown only exists for Wind. */
    public static String crownName(Element e, boolean character, boolean half) {
        return "crown_" + SpiritDiveMergeRules.name(e) + "_" + (character && SpiritDiveMergeRules.hasCharacterMesh(e) ? "char" : "std") + (half && e == Element.WIND ? "_half" : "");
    }

    public static SpiritDiveWind3Mesh crown(Element e, boolean character, boolean half) { return mesh(crownName(e, character, half)); }

    /** Every crown mesh file (the generator writes exactly these). */
    public static List<String> crownMeshes() {
        List<String> l = new ArrayList<>();
        for (Element e : Element.values()) {
            l.add(crownName(e, false, false));
            if (SpiritDiveMergeRules.hasCharacterMesh(e)) l.add(crownName(e, true, false));
            if (e == Element.WIND) { l.add(crownName(e, false, true)); l.add(crownName(e, true, true)); }
        }
        return l;
    }

    /** Vertices of a new crown as drawn (solid + glow; no second ring). */
    public static int crownVerts(Element e, boolean character, boolean half) { return crown(e, character, half).totalVerts(); }

    public static int crownBudget(boolean character) { return character ? CROWN_CHAR_VERTS : CROWN_STD_VERTS; }

    /** Vertices the new Wind A crown saves against the old one (negative: it is cheaper). */
    public static int windCrownDelta(boolean character, boolean half) { return crownVerts(Element.WIND, character, half) - SpiritDiveWind4Rules.crownVerts(character, half); }

    /** The width / height of a crown scaled by {@link #CROWN_K}, as a share of the old (for the tests). */
    public static float scaled(float old) { return old * CROWN_K; }
}
