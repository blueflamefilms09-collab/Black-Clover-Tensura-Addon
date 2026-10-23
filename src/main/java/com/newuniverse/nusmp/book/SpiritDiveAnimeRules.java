package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

/**
 * Spirit Dive, ANIME look (the {@code ANIME} value of the client option {@code spiritDive.look}, the default since 3.2): everything about it that can be decided without a game, pure and unit
 * tested ({@code SpiritDiveAnimeTest}). The painters ({@code client.aura.SpiritDiveAnime*}) read these tables and nothing else, so the worst case of a dive is exactly what is counted here.
 *
 * <p>What the look is, in words (owner request: "what the ANIME shows"): the wearer's own body and clothes stay visible, bathed in the light of the spirit: a smooth shell that hugs every body part
 * (energy streaks flowing along the limbs, a bright rim at the silhouette), glowing hair (a tinted cap on the head and curved, tapered locks that sway), glowing eyes, a thin crown-like halo of light
 * behind the head, and big smooth plume sheets sweeping out from the shoulder blades (Wind: pale green-white plumes; Fire: a flame cloak and flame hair, cel-banded like Fuegoleon's arm; Water:
 * blue-silver ribbons and a ring halo; Earth: grey-gold rock shards and a crystal halo). The character tier (Yuno, Fuegoleon, Noelle) adds more plumes, longer hair, hand and foot trails, a second halo
 * and the faint silhouette of the spirit behind the player. Entry: a flash, the shell growing from the feet up over {@link #ENTRY_TICKS} ticks, the hair lifting, the halo opening with a ring
 * pulse, a burst of the element; the exit reverses it.</p>
 *
 * <p>Cost model (a vertex is a corner of a quad, four per quad): see {@link #vertices}. The ceiling is the 3.0.0 V2 dive's measured worst case {@link #V2_WORST_VERTICES}; {@link #CAP_CHARACTER} and
 * {@link #CAP_STANDARD} are what this look is allowed to draw and what the unit test sums the tables against.</p>
 */
public final class SpiritDiveAnimeRules {
    private SpiritDiveAnimeRules() {}

    /** The worst case the V2 remake drew per diver (docs/magic/spirit_dives.md: about 7,700 vertices). This look must stay below it. */
    public static final int V2_WORST_VERTICES = 7700;
    /** What this look may draw per diver at the full level of detail (vertices): the character tier and the standard tier. */
    public static final int CAP_CHARACTER = 6200, CAP_STANDARD = 2400;

    // ------------------------------------------------------------------ entry and exit
    /** Ticks the entry lasts: the shell grows from the feet up in this time, the hair lifts, the halo opens. */
    public static final int ENTRY_TICKS = 18;
    /** Ticks the exit lasts (it is the same animation backwards; the server keeps the lease 20 ticks after the end, see EquipAnimation.SHATTER_WINDOW). */
    public static final int EXIT_TICKS = 18;

    private static float clamp01(float x) { return x < 0f ? 0f : x > 1f ? 1f : x; }

    /** Smoothstep of x in 0..1. */
    public static float smooth(float x) { x = clamp01(x); return x * x * (3f - 2f * x); }

    /**
     * How far the dive is on, 0..1: the entry over {@link #ENTRY_TICKS} ticks since the start ({@code t}) and the exit over {@link #EXIT_TICKS} ticks before the end ({@code left}); the smaller of the two.
     */
    public static float progress(float t, float left) {
        float in = clamp01(t / ENTRY_TICKS), out = clamp01(left / EXIT_TICKS);
        return Math.min(in, out);
    }

    /** The strength of one body part's shell while the dive grows from the feet up: {@code order} 0 = legs, 1 = body and arms, 2 = head; each part takes 0.4 of the progress, staggered by 0.3. */
    public static float partAlpha(float progress, int order) { return smooth((progress - 0.3f * order) / 0.4f); }

    /** The flash of the first ticks (white, at the chest): 1 at the start, 0 from tick 6. */
    public static float flash(float t) { return t <= 0f ? 1f : clamp01(1f - t / 6f); }

    /** The hair lift of the entry: the locks are short and straight up at first and grow to their length, overshooting a little. 0..1.1. */
    public static float hairLift(float progress) { float p = clamp01(progress); return smooth(p) * (1f + 0.12f * (float) Math.sin(p * Math.PI)); }

    /** The size of the plumes and the halo while they open: grows from 0.15 to 1 with a small overshoot (the burst), then settles. */
    public static float openScale(float progress) { float p = clamp01(progress); return 0.15f + 0.85f * smooth(p) + 0.12f * (float) Math.sin(p * Math.PI) * (1f - p); }

    /** The ring pulse of the halo opening (0..1 while it runs, 0 outside the entry's middle): a ring expanding from the feet over ticks 3 to 15. */
    public static float ringPulse(float t) { return t <= 3f || t >= 15f ? 0f : (t - 3f) / 12f; }

    /** The burst of the element (columns of wind or flame at the feet): 1 at tick 2, gone at tick 12. */
    public static float burst(float t) { return t <= 0f || t >= 12f ? 0f : (float) Math.sin(clamp01(t / 12f) * Math.PI); }

    // ------------------------------------------------------------------ the flowing shell
    /** Steps of the quantised scroll of the body shell: one cached render type per step and element (no render type is made per frame). */
    public static final int SWIRL_STEPS = 24;
    /** Ticks one full scroll of the shell texture takes, per element (WIND, WATER, FIRE, EARTH): wind quick, fire rising, water slow, earth very slow. */
    public static final float[] SWIRL_PERIOD = {30f, 52f, 36f, 90f};
    /** The direction of the scroll along the limbs, per element: -1 flows up the body, +1 down (fire rises, water runs down, wind rises, earth creeps down). */
    public static final int[] SWIRL_DIR = {-1, 1, -1, 1};

    /** The scroll step of the shell at the renderer's clock ({@code age}, ticks) for {@code el}: 0..{@link #SWIRL_STEPS}-1. */
    public static int swirlStep(Element el, float age) {
        float ph = (age / SWIRL_PERIOD[el.ordinal()]) % 1f;
        if (ph < 0f) ph += 1f;
        return Math.min(SWIRL_STEPS - 1, (int) (ph * SWIRL_STEPS));
    }

    /** The v offset of a swirl step (the texture scrolls along the limbs; {@code SWIRL_DIR} gives the sign). */
    public static float swirlV(Element el, int step) { return SWIRL_DIR[el.ordinal()] * (step / (float) SWIRL_STEPS); }

    // ------------------------------------------------------------------ shell geometry (pixels of the model)
    /** Local centre and half extent of each shell part (pixels), the vanilla player model: index 0 head, 1 body, 2 right arm, 3 left arm, 4 right leg, 5 left leg; slim arms are 3 wide. */
    public static final float[][] PART_BOX = {
            {0f, -4f, 0f, 4f, 4f, 4f},     // head (and the hat layer, grown 0.5)
            {0f, 6f, 0f, 4f, 6f, 2f},      // body
            {-1f, 4f, 0f, 2f, 6f, 2f},     // right arm (wide)
            {1f, 4f, 0f, 2f, 6f, 2f},      // left arm (wide)
            {0f, 6f, 0f, 2f, 6f, 2f},      // right leg
            {0f, 6f, 0f, 2f, 6f, 2f}};     // left leg

    /** Passes of the shell by level of detail: 3 = inner wash + rim hull + soft hull; 2 = inner + rim; 1 = rim only (the aura outline); 0 culled. */
    public static int shellPasses(Lod lod) { return switch (lod) { case FULL -> 3; case MID -> 2; case FAR -> 1; case CULLED -> 0; }; }

    /** Inflation of each shell pass in pixels of the model: inner wash, rim hull, soft hull. */
    public static final float[] INFLATE = {0.35f, 0.8f, 2.4f};
    /** Vertices of one drawn model part (a cuboid: 6 quads). */
    public static final int PART_VERTICES = 24;
    /** The six parts the shell is made of (head, body, two arms, two legs): the base layers of the player model; the clothes layers are not drawn again. */
    public static final int MODEL_PARTS = 6;

    /** Parts the shell draws for a stage, counted as the six body parts of {@link #PART_BOX} (the half wind dive: the left arm and leg; the {@link SpiritDivePieces#shellParts} mask) (never the head: its glow is the hair, the halo and the eyes). */
    public static int shellModelParts(Element el, Stage st) {
        int mask = SpiritDivePieces.shellParts(el, st), n = 0;
        for (int bit : new int[]{SpiritDivePieces.SHELL_BODY, SpiritDivePieces.SHELL_ARM_R, SpiritDivePieces.SHELL_ARM_L, SpiritDivePieces.SHELL_LEG_R, SpiritDivePieces.SHELL_LEG_L}) if ((mask & bit) != 0) n++;
        return n;
    }

    /**
     * Vertices of the shell at a level of detail: FULL draws the cover (the light on the clothes, the head too), the wash, the rim hull and the soft hull; MID the cover, the wash and the rim hull;
     * FAR the rim hull alone (the aura outline). The head is only in the cover.
     */
    public static int shellVertices(Element el, Stage st, Lod lod) {
        int n = shellModelParts(el, st);
        int heads = SpiritDivePieces.full(st) ? 1 : 0;
        return PART_VERTICES * switch (lod) { case FULL -> 4 * n + heads; case MID -> 3 * n + heads; case FAR -> n; case CULLED -> 0; };
    }

    // ------------------------------------------------------------------ the tiers
    /**
     * What one dive draws at a level of detail: the plume rows of the element's table that are used, the layers of each plume, its segments, hair locks, hand and foot trails, halos, whether the
     * eyes and the hair cap are drawn, whether the spirit silhouette is drawn behind the player.
     */
    public record Spec(int plumeRows, int plumeLayers, int plumeSegs, int locks, int lockSegs, int trails, int trailSegs, int halos, boolean eyes, boolean cap, boolean backdrop, boolean ground) {}

    private static final Spec[][] SPEC = {
            // standard tier: FULL, MID, FAR, CULLED
            {new Spec(2, 2, 8, 9, 4, 0, 0, 1, true, true, false, true), new Spec(2, 1, 5, 0, 0, 0, 0, 1, true, true, false, true), new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false),
                    new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false)},
            // character tier
            {new Spec(3, 2, 10, 14, 5, 4, 6, 2, true, true, true, true), new Spec(3, 2, 6, 6, 3, 0, 0, 1, true, true, false, true), new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false),
                    new Spec(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false)}};

    public static Spec spec(boolean character, Lod lod) { return SPEC[character ? 1 : 0][lod.ordinal()]; }

    /** Vertices per plume layer pass: segments x 4. A plume is drawn on both sides of the body (x2), as two passes (translucent body and additive glow). */
    public static final int PLUME_PASSES = 2;

    /**
     * Quads of the spirit silhouette behind the player (character tier), per element ordinal (WIND, WATER, FIRE, EARTH): the cubes of the geo model times six, from the files listed in
     * {@link #BACKDROP_MODEL} (the asset test counts them again from the files).
     */
    public static final int[] BACKDROP_QUADS = {468 + 24, 762, 354, 144};
    /** The geo models drawn as the backdrop: (key, name) pairs by element ordinal; wind draws two (the fairy and its wings). */
    public static final String[][] BACKDROP_MODEL = {{"spirit:sylph_fairy", "spirit:sylph_fairy_wings"}, {"spirit:undine_mermaid"}, {"spirit:salamander"}, {"spirit:gnome"}};

    /** Extra sprites of the entry (flash, ring pulse, burst columns, ground ring, the sigil of the ground): at most this many quads while it runs. */
    public static final int ENTRY_QUADS = 14;

    /** Vertices of the eyes (two quads and two glows), the halo (one quad each) and the ground ring (one quad), per the spec. */
    public static int smallVertices(Spec s) { return (s.eyes() ? 4 : 0) * 4 + s.halos() * 4 + (s.ground() ? 8 : 0); }

    /**
     * The most vertices one dive of this tier draws at a level of detail, counted the way the painter draws: the shell passes over the transformed parts, the plumes (both sides, layers, segments,
     * two passes), the locks and trails (one pass of additive strips... the locks two passes), the hair cap (one hat part), the small sprites, the silhouette of the spirit and the entry sprites.
     */
    public static int vertices(Element el, boolean character, Lod lod, Stage st) {
        Spec s = spec(character, lod);
        int v = shellVertices(el, st, lod);
        int rows = Math.min(s.plumeRows(), SpiritDiveAnimeGeometry.rows(el, character));
        int sides = SpiritDivePieces.halfBody(el, st) ? 1 : 2;
        v += rows * sides * s.plumeLayers() * s.plumeSegs() * 4 * PLUME_PASSES;
        v += s.locks() * s.lockSegs() * 4 * PLUME_PASSES;
        v += s.trails() * s.trailSegs() * 4 * PLUME_PASSES;
        if (s.cap()) v += PART_VERTICES;
        v += smallVertices(s);
        if (s.backdrop()) v += BACKDROP_QUADS[el.ordinal()] * 4 + 8;                      // the spirit, and the circle of the element standing behind the back (two quads)
        if (SpiritDivePieces.halfBody(el, st) && s.ground()) v += 4;                                   // the circle of crosses round the left wrist
        if (lod == Lod.FULL) v += ENTRY_QUADS * 4;
        return v;
    }

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

    // ------------------------------------------------------------------ the colours (RGB)
    /** The colours of one dive: shell tint, rim, glow, hair, eyes, halo (the cel-shaded textures carry their own bands; these tint the additive passes). */
    public record Pal(int tint, int rim, int glow, int hair, int eye, int halo) {}

    /** Palette of a dive: the character tier reads the colour grading of the character, the standard tier the element's own (all from {@link SpiritDiveTier}). */
    public static Pal pal(Element el, Grade g, boolean saint) {
        Grade use = SpiritDiveTier.effective(el, g);
        int glow = SpiritDiveTier.glow(el, use, false), prim = SpiritDiveTier.primary(el, use), acc = SpiritDiveTier.accent(el, use);
        return switch (el) {
            case WIND -> new Pal(glow, 0xE8FFF4, glow, use == Grade.YUNO ? 0xB8FFD8 : 0x9CF2C4, 0xE8FFB0, 0xE8FFF4);
            case FIRE -> new Pal(0xFF5A18, 0xFFE4A0, glow, 0xFF6A1C, 0xFFE070, 0xFFC060);
            case WATER -> new Pal(0x86BEF8, 0xE4F6FF, glow, use == Grade.NOELLE ? 0x7CC4FF : 0x6CB8F8, 0xE8F6FF, 0xD8F0FF);
            case EARTH -> new Pal(0xC9A25C, 0xFFE9A8, 0xFFB040, 0xC8A860, 0xFFC050, 0xFFD878);
        };
    }

    /** Eyes: Earth's glow amber, the others the colour of the palette; a dive with a helmet on the head draws no eyes and no cap (the painter asks). */

    // ------------------------------------------------------------------ the textures
    /** Texture paths (under assets/nusmp/textures) by element ordinal (WIND, WATER, FIRE, EARTH); the generator is tools/gen_spirit_dive_anime_textures.py. */
    public static String swirlTexture(Element el) { return "aura/sda_swirl_" + name(el); }
    public static String plumeTexture(Element el) { return "aura/sda_plume_" + name(el); }
    public static String plumeGlowTexture(Element el) { return "aura/sda_plume_glow_" + name(el); }
    public static String lockTexture(Element el) { return "aura/sda_lock_" + name(el); }
    public static String lockGlowTexture(Element el) { return "aura/sda_lock_glow_" + name(el); }
    public static String haloTexture(Element el) { return "aura/sda_halo_" + name(el); }
    public static String crownTexture(Element el) { return "aura/sda_crown_" + name(el); }
    public static String hairTexture(Element el) { return "aura/sda_hair_" + name(el); }
    public static final String RIM_TEXTURE = "aura/sda_rim", SOFT_TEXTURE = "aura/sda_soft", EYE_TEXTURE = "aura/sda_eye", BURST_TEXTURE = "aura/sda_burst";

    public static String name(Element el) { return el.name().toLowerCase(java.util.Locale.ROOT); }

    /** Every texture path the look uses (the asset test checks that each file exists and is clean under SafeAdditive where it is drawn additive). */
    public static java.util.List<String> allTextures() {
        java.util.ArrayList<String> l = new java.util.ArrayList<>();
        for (Element el : Element.values()) {
            l.add(swirlTexture(el)); l.add(plumeTexture(el)); l.add(plumeGlowTexture(el)); l.add(lockTexture(el)); l.add(lockGlowTexture(el)); l.add(haloTexture(el)); l.add(crownTexture(el)); l.add(hairTexture(el));
        }
        l.add(RIM_TEXTURE); l.add(SOFT_TEXTURE); l.add(EYE_TEXTURE); l.add(BURST_TEXTURE);
        return l;
    }

    /** The textures drawn with additive blending: they must carry no colour under alpha 0 (SafeAdditive). */
    public static boolean additive(String path) {
        return path.contains("_glow_") || path.contains("_halo_") || path.contains("_crown_") || path.contains("_hair_") || path.contains("_swirl_") || path.equals(RIM_TEXTURE) || path.equals(SOFT_TEXTURE)
                || path.equals(EYE_TEXTURE) || path.equals(BURST_TEXTURE);
    }

    // ------------------------------------------------------------------ the look option
    /** The ANIME look is the default of the client option {@code spiritDive.look}. */
    public static final SpiritDiveDjinnPieces.Look DEFAULT_LOOK = SpiritDiveDjinnPieces.Look.ANIME;

    /** Parses the option text; anything unknown is the default (ANIME). */
    public static SpiritDiveDjinnPieces.Look parse(String s) {
        if (s == null) return DEFAULT_LOOK;
        for (SpiritDiveDjinnPieces.Look l : SpiritDiveDjinnPieces.Look.values()) if (l.name().equalsIgnoreCase(s.trim())) return l;
        return DEFAULT_LOOK;
    }

    // ------------------------------------------------------------------ first person
    /** In first person only the arms glow, lightly: this alpha factor of the shell on the visible arm and hand. */
    public static final float FIRST_PERSON_ALPHA = 0.5f;
}
