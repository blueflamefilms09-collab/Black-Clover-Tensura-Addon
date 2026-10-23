package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

/**
 * Spirit Dive, REEL look (the {@code REEL} value of the client option {@code spiritDive.look}, the default since 3.3): the V2 design (a crown of crystal blocks with a gold ring halo, a hex-membrane
 * wing, gold rings on the limbs) executed the way the owner's two showcase videos of another Black Clover Minecraft mod do it. Everything about it that can be decided without a game lives here,
 * pure and unit tested ({@code SpiritDiveReelTest}); the painters ({@code client.aura.SpiritDiveReel*}) read these tables and nothing else, so the worst case of a dive is exactly what is counted here.
 *
 * <p>What the videos show, in words (Yuno / Wind and Noelle / Water): the camera pushes in, the mage raises an arm, white light and thin white-gold spiral ribbons whirl up round the body like a tornado of
 * lines, a sphere of crossing circles closes round them, a huge flash floods the picture, and the final form stands in the clearing: the wearer's own clothes and black hair, a crown ring of glowing teal
 * CUBES with vertical bars and white lights, glowing cube GAUNTLETS on both forearms, a translucent feather / leaf WING angled back and up, thin white rings orbiting at hip height, a soft ring on the ground
 * and, for the character, a huge spirit sword with a magic circle at its hilt (Yuno) or a spiky ball of water blades, a thin halo and a crystal spire (Noelle, whose whole body is a mosaic of glowing blue block
 * patches and whose wings are a comb of many parallel feather blades).</p>
 *
 * <p>Cost model (a vertex is a corner of a quad, four per quad): {@link #vertices}. The ceiling is the 3.0.0 V2 dive's measured worst case {@link #V2_WORST_VERTICES} and, ideally, the ANIME look's
 * {@link #ANIME_WORST_VERTICES}; {@link #CAP_CHARACTER} and {@link #CAP_STANDARD} are what this look is allowed to draw.</p>
 */
public final class SpiritDiveReelRules {
    private SpiritDiveReelRules() {}

    /** The worst case the V2 remake drew per diver (docs/magic/spirit_dives.md), and the ANIME look's heaviest character dive (docs/magic/spirit_dives_anime.md). */
    public static final int V2_WORST_VERTICES = 7700, ANIME_WORST_VERTICES = 5384;
    /** What this look may draw per diver at the full level of detail (vertices): the character tier and the standard tier, the entry sprites included. */
    public static final int CAP_CHARACTER = 3200, CAP_STANDARD = 2400;

    // ------------------------------------------------------------------ the timeline
    /** Ticks the entry lasts (the same 22 ticks as {@link SpiritDiveTier#ENTRY_TICKS}, the server's entry window). */
    public static final int ENTRY_TICKS = 22;
    /** Ticks the exit lasts (the server keeps the lease 20 ticks after the end, see EquipAnimation.SHATTER_WINDOW). The exit is the entry played backwards over these ticks. */
    public static final int EXIT_TICKS = 20;

    private static float clamp01(float x) { return x < 0f ? 0f : x > 1f ? 1f : x; }

    /** Smoothstep of x in 0..1. */
    public static float smooth(float x) { x = clamp01(x); return x * x * (3f - 2f * x); }

    /**
     * The clock of the transformation, 0 (nothing) .. {@link #ENTRY_TICKS} (complete): the entry runs forward over the first 22 ticks since the start ({@code t}), the exit runs the same clock backwards over
     * the last {@link #EXIT_TICKS} ticks before the end ({@code left}); the smaller of the two. Every effect of the look is a function of this one number.
     */
    public static float clock(float t, float left) {
        float in = t < 0f ? 0f : Math.min(t, ENTRY_TICKS), out = left < 0f ? 0f : Math.min(left * ENTRY_TICKS / EXIT_TICKS, ENTRY_TICKS);
        return Math.min(in, out);
    }

    /** True while the exit plays (the clock is run down by the end of the dive, not by its start). */
    public static boolean exiting(float t, float left) { return left * ENTRY_TICKS / EXIT_TICKS < Math.min(t, ENTRY_TICKS); }

    /** The body assembling, 0..1: the cube patches switch on from the feet to the head between clock 2 and 18. */
    public static float assemble(float u) { return clamp01((u - 2f) / 16f); }

    /** The patch level (1..{@link #MOSAIC_LEVELS}) of a body part group that has {@code from} (0..1) as its own start inside {@link #assemble}: the mosaic texture that is drawn. */
    public static int mosaicLevel(float assemble, float from, float span) {
        float p = clamp01((assemble - from) / span);
        return Math.max(1, Math.min(MOSAIC_LEVELS, 1 + (int) (p * MOSAIC_LEVELS)));
    }

    /** Mosaic textures per element: the patches that are on at each step of the assembling (level 1 the first few at the feet, the last level all of them). */
    public static final int MOSAIC_LEVELS = 5;

    /** The second burst when the final form appears out of the light (the flash of the videos that floods the screen and clears): a bell round clock 13.5. */
    public static float reveal(float u) { return clamp01(1f - Math.abs(u - 13.5f) / 3.5f); }

    /** The white flash at the chest: 0 at the start, up to 1 by clock 1.2, gone by clock 8. */
    public static float flash(float u) { return u <= 0f ? 0f : u < 1.2f ? u / 1.2f : clamp01(1f - (u - 1.2f) / 6.8f); }

    /** How far the spiral ribbons have travelled up the body, 0..1 (fast at first, slowing: ease out) between clock 0 and 15. */
    public static float ribbonTravel(float u) { float p = clamp01(u / 15f); return 1f - (1f - p) * (1f - p); }

    /** The strength of the ribbons: in over the first 2 ticks, out between clock 13 and 21. */
    public static float ribbonAlpha(float u) { return smooth(u / 2f) * (1f - smooth((u - 13f) / 8f)); }

    /** The expanding ring on the ground, 0..1 over clock 2..16 (0 outside). */
    public static float groundPulse(float u) { return u <= 2f || u >= 16f ? 0f : (u - 2f) / 14f; }

    /** The sphere of crossing circles, 0..1: in from clock 3, out by clock 18. */
    public static float cage(float u) { return smooth((u - 3f) / 4f) * (1f - smooth((u - 12f) / 6f)); }

    /** The size of the cage of circles (0.6 .. 1.15 as it closes round the body). */
    public static float cageScale(float u) { return 0.6f + 0.55f * smooth((u - 3f) / 14f); }

    /** The crown igniting, 0..1 over clock 11..19 (each cube starts later than the one before: {@link #crownCube}). */
    public static float crown(float u) { return clamp01((u - 11f) / 8f); }

    /** One cube of the crown (index {@code i} of {@code n}): it switches on {@code i/n} of the way through the ignition; 0..1, with the pop of a block that appears (a 25 percent overshoot). */
    public static float crownCube(float u, int i, int n) {
        float p = clamp01((crown(u) * 1.6f - 0.6f * i / Math.max(1, n)) * 1.0f);
        return p <= 0f ? 0f : popBack(p);
    }

    /** The wings unfolding, 0..1 over clock 9..20. */
    public static float wing(float u) { return clamp01((u - 9f) / 11f); }

    /** The size of the wings while they unfold: 0 .. 1 with an overshoot of about 12 percent, settling at 1 (easeOutBack). */
    public static float wingScale(float u) {
        float p = wing(u);
        if (p <= 0f) return 0f;
        float c1 = 1.1f, c3 = c1 + 1f, q = p - 1f;
        return 1f + c3 * q * q * q + c1 * q * q;
    }

    private static float popBack(float p) {
        float c1 = 1.4f, c3 = c1 + 1f, q = p - 1f;
        return Math.max(0f, 1f + c3 * q * q * q + c1 * q * q);
    }

    /** The gauntlets popping on, 0..1 over clock 5..14 (with the pop). */
    public static float gauntlet(float u) { float p = clamp01((u - 5f) / 9f); return p <= 0f ? 0f : popBack(p); }

    /** The weapon of the character tier being drawn out of its circle, 0..1 over clock 13..22. */
    public static float weapon(float u) { return smooth((u - 13f) / 9f); }

    /** The steady parts (rings, ground ring, sparks, gold bands) fade in with clock 6..16. */
    public static float settle(float u) { return smooth((u - 6f) / 10f); }

    // ------------------------------------------------------------------ the tiers and the levels of detail
    /**
     * What one dive draws at a level of detail. Counts are per dive (not per side) unless the field says so.
     *
     * @param ribbons      spiral ribbons of the entry
     * @param ribbonSegs   segments of one ribbon (at most {@link SpiritDiveAnimeGeometry#MAX_SEGS})
     * @param cage         crossing circles of the entry
     * @param crownCubes   cubes of the crown ring
     * @param crownBars    vertical bars of the crown (each two crossed planes) and the white light on each
     * @param goldRings    gold ring halos of the crown (flat rings)
     * @param bands        gold bands on the limbs (upper arms and thighs)
     * @param leaves       leaf wings per side (Wind)
     * @param leafSegs     segments of a leaf
     * @param blades       comb / flame / shard blades per side (Water, Fire, Earth)
     * @param bladeSegs    segments of a blade
     * @param orbit        thin orbiting rings
     * @param sparks       drifting fragments
     * @param gauntlet     0 none, 1 a box and a fist cube, 2 the full gauntlet (band, glow copy)
     * @param shellPasses  passes of the body shell: 3 = mosaic + rim hull + soft hull, 2 = mosaic + rim, 1 = the rim alone
     * @param weapon       the character's weapon (sword, spiky ball) and its circle
     * @param halo         the thin halo ring over the head (Noelle)
     * @param spire        the crystal spire (Noelle)
     * @param ground       the ring on the ground
     */
    public record Spec(int ribbons, int ribbonSegs, int cage, int crownCubes, int crownBars, int goldRings, int bands, int leaves, int leafSegs, int blades, int bladeSegs, int orbit, int sparks,
                       int gauntlet, int shellPasses, boolean weapon, boolean halo, boolean spire, boolean ground) {}

    private static final Spec NONE = new Spec(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false);

    /** Per element the blade count and the leaf count of a tier at FULL: {leaves, blades} standard then character. WIND draws leaves, the others a fan of blades. */
    private static final int[][][] WING = {
            /* WIND  */ {{2, 0}, {2, 0}},
            /* WATER */ {{0, 9}, {0, 15}},
            /* FIRE  */ {{0, 6}, {0, 9}},
            /* EARTH */ {{0, 6}, {0, 6}}};

    /** The spec of a tier at a level of detail for an element (the wing numbers depend on the element). */
    public static Spec spec(Element el, boolean character, Lod lod) {
        int c = character ? 1 : 0;
        int leaves = WING[el.ordinal()][c][0], blades = WING[el.ordinal()][c][1];
        return switch (lod) {
            case FULL -> character
                    ? new Spec(10, 12, 5, 12, 6, 2, 4, leaves, 8, blades, 2, 3, 8, 2, 3, true, true, true, true)
                    : new Spec(8, 12, 3, 8, 4, 1, 4, leaves, 8, blades, 2, 2, 5, 2, 3, false, false, false, true);
            case MID -> character
                    ? new Spec(4, 6, 0, 8, 0, 1, 0, Math.min(leaves, 1), 5, (blades + 1) / 2, 1, 1, 0, 1, 2, false, false, false, true)
                    : new Spec(4, 6, 0, 8, 0, 1, 0, Math.min(leaves, 1), 5, (blades + 1) / 2, 1, 1, 0, 1, 2, false, false, false, true);
            case FAR -> new Spec(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, false, false, false, true);
            case CULLED -> NONE;
        };
    }

    // ------------------------------------------------------------------ counting
    /** Vertices of one drawn model part of the shell (a cuboid: 6 quads). */
    public static final int PART_VERTICES = 24;
    /** Vertices of a box (6 quads), a band (4 outward quads), a crossed pair of planes, a sprite and a ring seen from both sides. */
    public static final int BOX = 24, BAND = 16, CROSS = 8, SPRITE = 4, RING = 8;

    /** Arms of the shell mask of a stage (the gauntlets sit on them). */
    public static int arms(Element el, Stage st) {
        int mask = SpiritDivePieces.shellParts(el, st), n = 0;
        if ((mask & SpiritDivePieces.SHELL_ARM_R) != 0) n++;
        if ((mask & SpiritDivePieces.SHELL_ARM_L) != 0) n++;
        return n;
    }

    /** Body parts of the shell mask (torso, arms, legs; never the head). */
    public static int shellModelParts(Element el, Stage st) { return SpiritDiveAnimeRules.shellModelParts(el, st); }

    /** The sides a stage draws wings and the crown on: 1 for the first dive of Wind (the left half), 2 otherwise. */
    public static int sides(Element el, Stage st) { return SpiritDivePieces.halfBody(el, st) ? 1 : 2; }

    /** True when the stage draws the head effects (the crown): a full dive, or the first dive of Wind (half a crown). */
    public static boolean crowned(Element el, Stage st) { return SpiritDivePieces.full(st) || SpiritDivePieces.halfBody(el, st); }

    /** Wing blades / leaves of the first dive of the other elements: a short fan, one side only (the half dive dresses the arms and adds one small wing). */
    public static int halfBlades(int blades) { return Math.max(3, blades / 2); }

    /**
     * The most vertices one dive of this tier draws at a level of detail, counted the way the painter draws: the shell, the gauntlets, the crown (cubes, bars, white lights, gold rings), the gold
     * bands, the wings (two passes: the body and the glow), the orbit rings, the sparks, the ground ring, the weapon and the halo of the character, and, only during the entry, the ribbons, the cage of circles and
     * the flash ({@link #entryVertices}).
     */
    public static int vertices(Element el, boolean character, Lod lod, Stage st) {
        Spec s = spec(el, character, lod);
        if (lod == Lod.CULLED) return 0;
        int v = PART_VERTICES * s.shellPasses() * shellModelParts(el, st);
        int arms = arms(el, st), sides = sides(el, st);
        boolean full = SpiritDivePieces.full(st);
        // the gauntlets: forearm box, fist cube; the full gauntlet adds the band and a bigger glow copy of the box
        v += arms * (s.gauntlet() == 0 ? 0 : s.gauntlet() == 1 ? 2 * BOX : 3 * BOX + BAND);
        if (crowned(el, st) && s.crownCubes() > 0) {
            int cubes = SpiritDiveReelGeometry.crownCount(s.crownCubes(), sides == 1), bars = SpiritDiveReelGeometry.barCount(s.crownBars(), sides == 1);
            v += cubes * BOX * 2 + bars * 2 * SPRITE + s.goldRings() * RING;                            // the cubes are drawn twice: the block and its additive glow copy
            if (el == Element.WATER && s.halo()) v += RING + SPRITE;                                              // the thin halo ring over the head and its bloom
        }
        if (full && s.bands() > 0) v += s.bands() * BAND;
        // the wings: leaves are two passes of strips, blades too
        int wingSides = full ? sides : 1;
        int leaves = s.leaves() == 0 ? 0 : full ? s.leaves() : 1;
        int blades = s.blades() == 0 ? 0 : full ? s.blades() : halfBlades(s.blades());
        v += wingSides * leaves * s.leafSegs() * 4 * 2 + wingSides * blades * s.bladeSegs() * 4 * 2;
        if (full) v += s.orbit() * RING + s.sparks() * SPRITE;
        if (s.ground()) v += RING;
        if (full && s.weapon()) v += weaponVertices(el);
        if (full && el == Element.WATER && s.spire()) v += 3 * CROSS + SPRITE;
        if (lod != Lod.FAR) v += ribbonVertices(s);
        if (lod == Lod.FULL) v += entryVertices(s) - ribbonVertices(s);
        return v;
    }

    /** Vertices of the weapon of the character at its steady state: Yuno's sword (a strip, its glow, its circle), Fuegoleon's flame blade, Noelle's ball of water blades (two discs of ten). */
    public static int weaponVertices(Element el) {
        return switch (el) {
            case WIND -> 3 * SPRITE;                                          // body strip, glow strip, the circle
            case FIRE -> 3 * SPRITE;
            case WATER -> 2 * SPIKES_PER_DISC * SPRITE + 2 * SPRITE;          // two discs of blades and a bright core
            case EARTH -> 0;
        };
    }

    /** Blades of one disc of Noelle's spiky ball of water. */
    public static final int SPIKES_PER_DISC = 10;

    /** The vertices of the entry-only sprites of a spec: ribbons (segments x 4), the circles of the cage (both faces), the flash (a star and a soft glow), the ring pulse on the ground. */
    public static int entryVertices(Spec s) { return ribbonVertices(s) + s.cage() * RING + 3 * SPRITE + RING; }

    /** The ribbons of the entry: segments x 4 vertices, two passes (the core and the soft glow). */
    public static int ribbonVertices(Spec s) { return s.ribbons() * s.ribbonSegs() * 4 * RIBBON_PASSES; }

    /** Passes of a ribbon: the bright core and a wide soft glow. */
    public static final int RIBBON_PASSES = 2;

    /** The worst case over the elements and stages of a tier at the full level of detail. */
    public static int worstVertices(boolean character) {
        int worst = 0;
        for (Element el : Element.values()) {
            if (character && el == Element.EARTH) continue;                    // Earth has no character tier
            for (Stage st : Stage.values()) worst = Math.max(worst, vertices(el, character, Lod.FULL, st));
        }
        return worst;
    }

    /** True when the tier of this grade draws the character look: the same rule as the other looks. */
    public static boolean character(Element el, Grade g) { return SpiritDiveTier.character(el, g); }

    // ------------------------------------------------------------------ the mosaic and the shell
    /** The vertex alpha of the mosaic pass by element and tier: Noelle's body is a solid blue crystal armour, the other tiers wear lighter patches over their clothes. */
    public static float mosaicAlpha(Element el, boolean character) { return el == Element.WATER ? (character ? 0.95f : 0.85f) : character ? 0.9f : 0.75f; }

    // ------------------------------------------------------------------ the colours (RGB)
    /** The colours of one dive: the body of the cubes and patches, the glow, the bright light (white lights, bars, ribbons), the rim of the shell and the gold of the rings. */
    public record Pal(int body, int glow, int light, int rim, int gold) {}

    /** Palette of a dive: the character tier reads the colour grading of the character, the standard tier the element's own (all from {@link SpiritDiveTier}). */
    public static Pal pal(Element el, Grade g) {
        Grade use = SpiritDiveTier.effective(el, g);
        int glow = SpiritDiveTier.glow(el, use, false), prim = SpiritDiveTier.primary(el, use), acc = SpiritDiveTier.accent(el, use);
        return switch (el) {
            case WIND -> new Pal(prim, glow, 0xF4FFFA, 0xE8FFF4, 0xFFE08A);
            case WATER -> new Pal(prim, glow, 0xF2FAFF, 0xE4F6FF, 0xFFE08A);
            case FIRE -> new Pal(0xFF6A18, glow, 0xFFF0C0, 0xFFE4A0, 0xFFD060);
            case EARTH -> new Pal(0xC9A25C, 0xFFB040, acc, 0xFFE9A8, 0xFFD878);
        };
    }

    // ------------------------------------------------------------------ the textures
    /** Texture paths (under assets/nusmp/textures) by element; the generator is tools/gen_spirit_dive_reel_textures.py. */
    public static String cubeTexture(Element el) { return "aura/sdr_cube_" + name(el); }
    public static String barTexture(Element el) { return "aura/sdr_bar_" + name(el); }
    public static String wingTexture(Element el) { return "aura/sdr_wing_" + name(el); }
    public static String wingGlowTexture(Element el) { return "aura/sdr_wing_glow_" + name(el); }
    public static String circleTexture(Element el) { return "aura/sdr_circle_" + name(el); }
    public static String mosaicTexture(Element el, int level) { return "aura/sdr_mosaic_" + name(el) + "_" + level; }
    public static final String RIBBON = "aura/sdr_ribbon", RING_TEX = "aura/sdr_ring", RING_GOLD = "aura/sdr_ring_gold", GROUND = "aura/sdr_ground", FLASH = "aura/sdr_flash", SPARK = "aura/sdr_spark",
            HALO_RING = "aura/sdr_halo_ring", GOLD = "aura/sdr_gold", SWORD_WIND = "aura/sdr_sword_wind", SWORD_FIRE = "aura/sdr_sword_fire", SPIKE_WATER = "aura/sdr_spike_water",
            SPIRE_WATER = "aura/sdr_spire_water";

    public static String name(Element el) { return el.name().toLowerCase(java.util.Locale.ROOT); }

    /** The textures of the ANIME look this look shares (the rim hull and the soft hull of the shell). */
    public static final String SHARED_RIM = SpiritDiveAnimeRules.RIM_TEXTURE, SHARED_SOFT = SpiritDiveAnimeRules.SOFT_TEXTURE;

    /** Every texture path the look uses (the asset test checks that each file exists and is clean under SafeAdditive where it is drawn additive). */
    public static java.util.List<String> allTextures() {
        java.util.ArrayList<String> l = new java.util.ArrayList<>();
        for (Element el : Element.values()) {
            l.add(cubeTexture(el)); l.add(barTexture(el)); l.add(wingTexture(el)); l.add(wingGlowTexture(el)); l.add(circleTexture(el));
            for (int k = 1; k <= MOSAIC_LEVELS; k++) l.add(mosaicTexture(el, k));
        }
        l.add(RIBBON); l.add(RING_TEX); l.add(RING_GOLD); l.add(GROUND); l.add(FLASH); l.add(SPARK); l.add(HALO_RING); l.add(GOLD); l.add(SWORD_WIND); l.add(SWORD_FIRE); l.add(SPIKE_WATER); l.add(SPIRE_WATER);
        return l;
    }

    /** Textures of the ANIME look the REEL look draws too (so they must stay). */
    public static java.util.List<String> sharedTextures() { return java.util.List.of(SHARED_RIM, SHARED_SOFT); }

    // ------------------------------------------------------------------ the look option
    /** The REEL look is the default of the client option {@code spiritDive.look} since 3.3. */
    public static final SpiritDiveDjinnPieces.Look DEFAULT_LOOK = SpiritDiveDjinnPieces.Look.REEL;

    /** Parses the option text; anything unknown is the default (REEL). */
    public static SpiritDiveDjinnPieces.Look parse(String s) {
        if (s == null) return DEFAULT_LOOK;
        for (SpiritDiveDjinnPieces.Look l : SpiritDiveDjinnPieces.Look.values()) if (l.name().equalsIgnoreCase(s.trim())) return l;
        return DEFAULT_LOOK;
    }

    // ------------------------------------------------------------------ the camera
    /** The client option {@code spiritDive.cameraPush} is NOT part of this look: the camera is never touched (the owner may ask for it later). */
    public static final boolean CAMERA_PUSH = false;
}
