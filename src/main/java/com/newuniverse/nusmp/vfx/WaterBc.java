package com.newuniverse.nusmp.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Water VFX, the hybrid "BC" (C's 3D toon mesh cores wrapped in B's volumetric glass), built into the game (docs/vfx_remake/water_ingame.md, preview docs/vfx_remake/water_bc.md): the pure half. No Minecraft
 * class, so it runs in the unit tests, in the CI self-test on a dedicated server and in the headless preview; the painter ({@code vfx.client.layer.WaterBcLayer}) reads every number from here.
 *
 * <p>Seven effects are redrawn: the Sea Dragon's Roar and the Aqua Javelin / Tide Lance (both {@code WATERFX_CAST}, told apart by {@link #kindOf}), the Cradle and the Nest (both {@code WATERFX_ZONE}), the
 * impact {@code WATERFX_BURST}, and {@code WATER_SPLASH} and {@code WATER_RING} (which blood and paint pages reuse with their own colour, so those two keep a tint). The Valkyrie wings are deferred (the owner
 * has them remade separately as a 3D model of the anime armour): nothing of them is here, and the six old {@code WaterSpellLayer} shapes (WATER_DRAGON .. WATER_DRESS) are not taken over.
 *
 * <p>The setting is the CLIENT config {@code vfx.element.water} ({@code nusmp-water-bc-client.toml}): {@link Style#BC} (default) or {@link Style#V1} (exactly the old code for every shape, rule 2). It is a
 * mirror of the config that only the client writes; the dedicated server never reads it and nothing is sent over the network for it.
 *
 * <p>Cost (rules 9 and 11): no packet, no server work. Every effect is first admitted by {@link #admit} nearest first against a per-frame ceiling of {@link #FRAME_MESH_CEILING} mesh quads and
 * {@link #FRAME_SPRITE_CEILING} sprite quads for ALL water effects in view together (scaled down on the medium and low quality levels): the nearest ones get the full mesh (LOD0), then the light mesh (LOD1),
 * then the flat anime sprites (SPRITE), and what does not fit is not drawn. {@link #meshCap} and {@link #spriteCap} are the worst case of one effect in each tier (pinned against the real scene builder by
 * the unit test), so the admitted total can never pass the ceilings.
 */
public final class WaterBc {
    private WaterBc() {}

    public enum Style {
        /** The hybrid look (default). */
        BC,
        /** The look before this change, byte for byte the old layers. */
        V1
    }

    public static final Style DEFAULT_STYLE = Style.BC;
    public static final String CONFIG_FILE = "nusmp-water-bc-client.toml";
    /** The config path: table {@code vfx.element}, key {@code water}. */
    public static final String CONFIG_PATH = "vfx.element.water";

    private static volatile Style style = DEFAULT_STYLE;

    public static Style style() { return style; }

    /** Written by the client config only. */
    public static void setStyle(Style s) { style = s == null ? DEFAULT_STYLE : s; }

    /** True while the hybrid look is on. */
    public static boolean bcLook() { return style == Style.BC; }

    /** Reads the option text; anything unknown is the default. */
    public static Style parse(String text) {
        if (text == null) return DEFAULT_STYLE;
        String t = text.strip().toUpperCase(Locale.ROOT);
        for (Style s : Style.values()) if (s.name().equals(t)) return s;
        return DEFAULT_STYLE;
    }

    // ------------------------------------------------------------------------------------------------ the shapes and the seven looks
    /** The shapes the hybrid takes over (the layer registers for exactly these). */
    public static final List<VfxShape> SHAPES = List.of(VfxShape.WATERFX_CAST, VfxShape.WATERFX_ZONE, VfxShape.WATERFX_BURST, VfxShape.WATER_SPLASH, VfxShape.WATER_RING);
    /** Water shapes that this change never touches: the six old {@code WaterSpellLayer} shapes (the Valkyrie Dress among them: the wings are deferred). */
    public static final List<VfxShape> UNTOUCHED = List.of(VfxShape.WATER_DRAGON, VfxShape.WATER_CRADLE, VfxShape.WATER_BURST, VfxShape.WATER_JAVELIN, VfxShape.WATER_NEST, VfxShape.WATER_DRESS);

    public static boolean owns(VfxShape s) { return SHAPES.contains(s); }

    public static final int ROAR = 0, LANCE = 1, CRADLE = 2, NEST = 3, BURST = 4, SPLASH = 5, RING = 6, KINDS = 7;
    public static final String[] KIND_NAMES = {"roar", "lance", "cradle", "nest", "burst", "splash", "ring"};

    /** A cast that covers at least this many blocks per tick is the javelin / lance (the model flies 2.4 to 2.6 blocks a tick); the dragon flies at most 0.9 and the fan of dragons 0.92. */
    public static final float LANCE_BLOCKS_PER_TICK = 1.5f;

    /**
     * Which of the seven looks an effect gets. WATERFX_CAST is the dragon unless it flies fast ({@code length / duration >= LANCE_BLOCKS_PER_TICK}: Aqua Javelin, Valkyrie Tide Lance), WATERFX_ZONE is the
     * Cradle when it follows its caster ({@code following}: the Cradle is sent that way, the Nest and the Scale Mail are placed) and the Nest otherwise; the rest have one look. -1 for a shape that is not owned.
     */
    public static int kindOf(VfxShape s, double length, int duration, boolean following) {
        if (s == null) return -1;
        return switch (s) {
            case WATERFX_CAST -> length / Math.max(1, duration) >= LANCE_BLOCKS_PER_TICK ? LANCE : ROAR;
            case WATERFX_ZONE -> following ? CRADLE : NEST;
            case WATERFX_BURST -> BURST;
            case WATER_SPLASH -> SPLASH;
            case WATER_RING -> RING;
            default -> -1;
        };
    }

    // ------------------------------------------------------------------------------------------------ tiers, caps, the frame ceilings
    /** Full mesh (near), light mesh (far or crowded), flat anime sprites (over the ceiling), nothing. */
    public static final int LOD0 = 0, LOD1 = 1, SPRITE = 2, SKIP = 3;

    /** Worst-case mesh quads of one effect: [kind][LOD0, LOD1] (water_bc.md section 4; pinned by WaterBcTest against the real scene). */
    private static final int[][] MESH_CAP = {{340, 170}, {115, 70}, {180, 125}, {125, 80}, {220, 100}, {150, 65}, {135, 50}};
    /** Worst-case sprite quads of one effect: [kind][LOD0, LOD1, SPRITE]. */
    private static final int[][] SPRITE_CAP = {{56, 34, 22}, {40, 22, 22}, {24, 16, 14}, {28, 16, 12}, {32, 20, 14}, {18, 12, 10}, {8, 6, 4}};

    /** Mesh quads one effect may add in a tier (0 for the sprite tier and for SKIP). */
    public static int meshCap(int kind, int tier) { return tier == LOD0 || tier == LOD1 ? MESH_CAP[kind][tier] : 0; }

    /** Sprite quads one effect may add in a tier (0 for SKIP). */
    public static int spriteCap(int kind, int tier) { return tier >= LOD0 && tier <= SPRITE ? SPRITE_CAP[kind][tier] : 0; }

    /** The per-frame ceilings for all water effects in view together (full quality). */
    public static final int FRAME_MESH_CEILING = 1200, FRAME_SPRITE_CEILING = 500;
    /** LOD0 is given inside this many blocks (distance past the effect's reach), to at most this many effects, while the mesh total stays under this share of the ceiling. */
    public static final double LOD0_RANGE = 16.0;
    public static final int LOD0_MAX_IN_VIEW = 3;
    public static final float LOD0_SHARE = 0.7f;
    /** Reserved vertex ceiling of the vertex-allowance of the highest quality level (VfxQuality.HIGH.frameVertices): the ceilings scale with the player's quality level. */
    public static final int HIGH_FRAME_VERTICES = 40000;

    /** The share of the ceilings a quality level gets: 1 on high, less on medium and low (never below one tenth). */
    public static float qualityScale(int frameVertices) { return Math.max(0.1f, Math.min(1f, frameVertices / (float) HIGH_FRAME_VERTICES)); }

    public static int meshCeiling(float scale) { return Math.round(FRAME_MESH_CEILING * Math.max(0.1f, Math.min(1f, scale))); }

    public static int spriteCeiling(float scale) { return Math.round(FRAME_SPRITE_CEILING * Math.max(0.1f, Math.min(1f, scale))); }

    /** Test and preview hook: -1 (always, in the game) lets {@link #admit} decide; 0..3 forces every effect into that tier. The game never sets it. */
    private static volatile int forcedTier = -1;

    public static void forceTier(int tier) { forcedTier = tier >= 0 && tier <= SKIP ? tier : -1; }

    public static int forcedTier() { return forcedTier; }

    /**
     * Admits the effects of a frame nearest first. {@code dist[i]} is the distance of effect i past its reach, {@code kind[i]} its look; the tier of each goes to {@code tierOut[i]}; {@code done} is scratch
     * (at least n entries). Rules: LOD0 only for the 3 nearest effects, only inside {@link #LOD0_RANGE} blocks, only while the running mesh total (with this effect's worst case) stays under
     * {@link #LOD0_SHARE} of the mesh ceiling; else LOD1 when it fits under the ceilings; else the flat sprites when they fit the sprite ceiling; else nothing. The sums of the worst cases never pass the
     * ceilings, so the frame is bounded whatever the number of casters. Allocation free.
     */
    public static void admit(double[] dist, int[] kind, int n, int[] tierOut, boolean[] done, float scale) {
        int meshMax = meshCeiling(scale), spriteMax = spriteCeiling(scale), mesh = 0, sprite = 0;
        float lod0Mesh = meshMax * LOD0_SHARE;
        for (int i = 0; i < n; i++) done[i] = false;
        for (int rank = 0; rank < n; rank++) {
            int best = -1;
            for (int i = 0; i < n; i++) if (!done[i] && (best < 0 || dist[i] < dist[best])) best = i;
            done[best] = true;
            int k = kind[best], t;
            if (forcedTier >= 0) t = forcedTier;
            else if (dist[best] <= LOD0_RANGE && rank < LOD0_MAX_IN_VIEW && mesh + MESH_CAP[k][LOD0] <= lod0Mesh && sprite + SPRITE_CAP[k][LOD0] <= spriteMax) t = LOD0;
            else if (mesh + MESH_CAP[k][LOD1] <= meshMax && sprite + SPRITE_CAP[k][LOD1] <= spriteMax) t = LOD1;
            else if (sprite + SPRITE_CAP[k][SPRITE] <= spriteMax) t = SPRITE;
            else t = SKIP;
            tierOut[best] = t;
            mesh += meshCap(k, t);
            sprite += spriteCap(k, t);
        }
    }

    /** The vertex budget of one activation of the shape in the sprite buffer (the largest sprite cap of the looks the shape can take, 4 vertices a quad; never over the buffer's 400). */
    public static int vertexBudget(VfxShape s) {
        int q = 0;
        for (int k = 0; k < KINDS; k++) if (shapeOfKind(k) == s) for (int t = LOD0; t <= SPRITE; t++) q = Math.max(q, SPRITE_CAP[k][t]);
        return Math.min(400, q * 4);
    }

    private static VfxShape shapeOfKind(int k) {
        return switch (k) {
            case ROAR, LANCE -> VfxShape.WATERFX_CAST;
            case CRADLE, NEST -> VfxShape.WATERFX_ZONE;
            case BURST -> VfxShape.WATERFX_BURST;
            case SPLASH -> VfxShape.WATER_SPLASH;
            default -> VfxShape.WATER_RING;
        };
    }

    // ------------------------------------------------------------------------------------------------ tint
    /** Only these looks are reused by other magics with their own colour (blood, paint, the melee pages), so only they have grey twin textures and tinted palettes. */
    public static boolean tintable(int kind) { return kind == SPLASH || kind == RING; }

    /**
     * Whether an effect's colour is a water blue. Those draw with the native textures and the water palette (white vertex colour); every other colour (the blood and paint pages that reuse the splash and the
     * ring) draws the grey twin of the texture multiplied by that colour and the palette of the mesh turned to that colour.
     */
    public static boolean nativePalette(int argb) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max < 120 || max == min) return false;                          // dark or grey colours are tints
        float sat = (max - min) / (float) max;
        if (sat < 0.18f) return false;
        float h;
        if (max == g) h = 60f * (2f + (b - r) / (float) (max - min));
        else if (max == r) h = 60f * (((g - b) / (float) (max - min) + 6f) % 6f);
        else h = 60f * (4f + (r - g) / (float) (max - min));
        return h >= 165f && h <= 265f;
    }

    // ------------------------------------------------------------------------------------------------ assets
    public static final String PREFIX = "wbc_";
    public static final String MESH_FOLDER = "wbc_meshes";
    /** The sprite textures of the set (files {@code wbc_<name>.png}); the index is the texture id the scene hands to the painter. */
    public static final String[] SPRITES = {"b_body", "b_ripple", "b_crown", "b_drop", "b_bubble", "b_glint", "b_lance", "b_sphere", "b_dome", "b_swirl", "mist", "ribbon", "sheen", "shell", "glow",
            "a_body", "a_head", "a_drop", "a_ring", "a_crown", "a_lance", "a_sphere", "a_blob", "a_dome", "a_flash"};
    public static final int T_B_BODY = 0, T_B_RIPPLE = 1, T_B_CROWN = 2, T_B_DROP = 3, T_B_BUBBLE = 4, T_B_GLINT = 5, T_B_LANCE = 6, T_B_SPHERE = 7, T_B_DOME = 8, T_B_SWIRL = 9, T_MIST = 10, T_RIBBON = 11,
            T_SHEEN = 12, T_SHELL = 13, T_GLOW = 14, T_A_BODY = 15, T_A_HEAD = 16, T_A_DROP = 17, T_A_RING = 18, T_A_CROWN = 19, T_A_LANCE = 20, T_A_SPHERE = 21, T_A_BLOB = 22, T_A_DOME = 23, T_A_FLASH = 24;
    /** The sprites that have a grey twin {@code wbc_<name>_t.png} (the ones SPLASH and RING draw). */
    public static final int[] TINT_TWINS = {T_B_RIPPLE, T_B_CROWN, T_B_DROP, T_B_BUBBLE, T_MIST, T_A_CROWN, T_A_RING, T_A_BLOB};
    /** The one white cell the mesh quads sample (their colour is per vertex). */
    public static final String SOLID = "solid";

    public static boolean hasTwin(int sprite) {
        for (int t : TINT_TWINS) if (t == sprite) return true;
        return false;
    }

    /** File name without folder or extension: {@code wbc_b_body} or, for the grey twin, {@code wbc_b_body_t}. */
    public static String fileName(String name, boolean twin) { return PREFIX + name + (twin ? "_t" : ""); }

    /** Every texture of the set as a path under assets/nusmp: the sprites, their twins and the solid cell. */
    public static List<String> allTextures() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < SPRITES.length; i++) {
            out.add("textures/particle/" + fileName(SPRITES[i], false) + ".png");
            if (hasTwin(i)) out.add("textures/particle/" + fileName(SPRITES[i], true) + ".png");
        }
        out.add("textures/particle/" + fileName(SOLID, false) + ".png");
        return out;
    }

    /** The core pieces of the meshes ({@code assets/nusmp/wbc_meshes/bc_<name>.txt} and {@code bc_<name>_lo.txt}); the index is the piece id the scene hands to the painter. */
    public static final String[] PIECES = {"head_upper", "head_hull", "head_lower", "seg", "seg_b", "tail", "horn", "ripple", "lance", "sphere", "dome"};
    public static final int P_HEAD_UPPER = 0, P_HEAD_HULL = 1, P_HEAD_LOWER = 2, P_SEG = 3, P_SEG_B = 4, P_TAIL = 5, P_HORN = 6, P_RIPPLE = 7, P_LANCE = 8, P_SPHERE = 9, P_DOME = 10;

    public static String meshFile(int piece, boolean lo) { return "bc_" + PIECES[piece] + (lo ? "_lo" : ""); }

    public static List<String> allMeshes() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < PIECES.length; i++) for (int lo = 0; lo < 2; lo++) out.add(MESH_FOLDER + "/" + meshFile(i, lo == 1) + ".txt");
        return out;
    }

    /**
     * Checks one decoded sprite of the set (pure, shared by the unit test and the CI self-test): a real sprite with some opaque and some clear texels; a square one with clear corners when its name says ring,
     * ripple, glow, glint or flash; a grey twin has equal r, g and b in every live texel and the same silhouette is checked by the test against its colour original. Returns the problem, or null.
     */
    public static String checkTexture(String name, boolean twin, int w, int h, int[] argb) {
        if (w < 16 || h < 8 || argb.length != w * h) return name + " has an odd size " + w + "x" + h;
        int live = 0, clear = 0;
        for (int px : argb) {
            int a = px >>> 24;
            if (a == 0) clear++;
            else if (a > 8) live++;
            if (twin && a > 8 && (((px >> 16) & 255) != ((px >> 8) & 255) || ((px >> 8) & 255) != (px & 255))) return name + " grey twin has a coloured texel";
        }
        if (live == 0 || clear == 0) return name + " is empty or fully opaque";
        boolean roundSprite = name.contains("ring") || name.contains("ripple") || name.contains("glow") || name.contains("glint") || name.contains("flash");
        if (w == h && roundSprite) {
            int[] corners = {0, w - 1, (h - 1) * w, h * w - 1};
            for (int c : corners) if ((argb[c] >>> 24) > 16) return name + " has an opaque corner";
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ small colour helpers shared by the painter and the scene
    /** ARGB with this alpha (0..1) and the given rgb. */
    public static int argb(float alpha, int rgb) { return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (rgb & 0xFFFFFF); }
}
