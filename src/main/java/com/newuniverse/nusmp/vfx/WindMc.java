package com.newuniverse.nusmp.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wind VFX in the Minecraft style, version C "pixel-anime" (docs/vfx_remake/wind_mc.md, docs/vfx_remake/wind_mc_ingame.md): the pure half. No Minecraft class, so it runs in the unit tests, in the CI self-test on a
 * dedicated server and in the headless preview; the painter ({@code vfx.client.layer.WindMcLayer}) reads every number from here.
 *
 * <p>Four effects are redrawn: Crescent Kamaitachi ({@code WIND_SLASH}, also used by six non-wind melee pages, so it keeps its tint), Slicing Wind Emperor ({@code WIND_EMPEROR}), Gale / Gust Lane
 * ({@code WIND_GALE} and {@code FX_WIND_GUST}, which had no layer at all and drew only dust) and the Wind Ring ({@code WIND_RING}). The Wind Tornado and Spirit of Zephyr are NOT part of this: their
 * layer ({@code WindSpellLayer}) keeps drawing them with exactly today's code in every setting.
 *
 * <p>The setting is the CLIENT config {@code vfx.element.wind} ({@code nusmp-wind-mc-client.toml}): {@link Style#MC_C} (default) or {@link Style#V1} (exactly the old code, for every shape, rule 2). It is a mirror
 * of the config that only the client writes; the dedicated server never reads it and nothing is sent over the network for it.
 *
 * <p>Cost (rules 9 and 11): no packet, no server work. The worst case per activation is {@link #worstQuads} (a handful of quads; the caps are the peaks of the code this replaces), every draw goes through the
 * vertex budget of the activation ({@link #vertexBudget}) and the distance level of detail of {@code VfxManager}.
 */
public final class WindMc {
    private WindMc() {}

    public enum Style {
        /** The pixel-anime look (default). */
        MC_C,
        /** The look before this change, byte for byte the old layers. */
        V1
    }

    public static final Style DEFAULT_STYLE = Style.MC_C;
    public static final String CONFIG_FILE = "nusmp-wind-mc-client.toml";
    /** The config path: table {@code vfx.element}, key {@code wind}. */
    public static final String CONFIG_PATH = "vfx.element.wind";

    private static volatile Style style = DEFAULT_STYLE;

    public static Style style() { return style; }

    /** Written by the client config only. */
    public static void setStyle(Style s) { style = s == null ? DEFAULT_STYLE : s; }

    /** True while the pixel-anime look is on. */
    public static boolean pixelLook() { return style == Style.MC_C; }

    /** Reads the option text; anything unknown is the default. */
    public static Style parse(String text) {
        if (text == null) return DEFAULT_STYLE;
        String t = text.strip().toUpperCase(Locale.ROOT);
        for (Style s : Style.values()) if (s.name().equals(t)) return s;
        return DEFAULT_STYLE;
    }

    // ------------------------------------------------------------------------------------------------ the shapes
    /** The shapes the pixel look takes over (the layer registers for exactly these). */
    public static final List<VfxShape> SHAPES = List.of(VfxShape.WIND_SLASH, VfxShape.WIND_RING, VfxShape.WIND_EMPEROR, VfxShape.WIND_GALE, VfxShape.FX_WIND_GUST);
    /** Wind shapes that this change must never touch. */
    public static final List<VfxShape> UNTOUCHED = List.of(VfxShape.WIND_TORNADO, VfxShape.WIND_ZEPHYR);

    public static boolean owns(VfxShape s) { return SHAPES.contains(s); }

    // ------------------------------------------------------------------------------------------------ what each effect draws (the painter reads these)
    public static final int SLASH_GHOSTS = 3, SLASH_RIBBONS = 2, SLASH_SPARKS = 2, SLASH_FLASH = 1;
    public static final int EMPEROR_GHOSTS = 3, EMPEROR_RIBBONS = 3, EMPEROR_SPARKS = 6, EMPEROR_RAYS = 6;
    /** a swallow is two quads: the swallow sprite and the ribbon trailing it */
    public static final int GALE_SWALLOWS = 4, GALE_STREAKS = 3, GALE_SPARKS = 2, GALE_LEAVES = 3;
    public static final int RING_DECALS = 2, RING_SWIRLS = 1, RING_RIBBONS = 6, RING_SPARKS = 4;

    /** The most quads one activation can add (every part at full detail). */
    public static int worstQuads(VfxShape s) {
        return switch (s) {
            case WIND_SLASH -> SLASH_GHOSTS + SLASH_RIBBONS + SLASH_SPARKS + SLASH_FLASH;
            case WIND_EMPEROR -> EMPEROR_GHOSTS + EMPEROR_RIBBONS + EMPEROR_SPARKS + EMPEROR_RAYS;
            case WIND_GALE, FX_WIND_GUST -> GALE_SWALLOWS * 2 + GALE_STREAKS + GALE_SPARKS + GALE_LEAVES;
            case WIND_RING -> RING_DECALS + RING_SWIRLS + RING_RIBBONS + RING_SPARKS;
            default -> 0;
        };
    }

    /** The peak quads of the code each shape replaced (measured over every tick of its life with the real old layers, docs/previews/vfx_remake/mc/wind_mc_ingame_budget.json): the pixel look stays under these. */
    public static int capQuads(VfxShape s) {
        return switch (s) {
            case WIND_SLASH -> 72;
            case WIND_EMPEROR -> 80;
            case WIND_GALE, FX_WIND_GUST -> 68;
            case WIND_RING -> 100;
            default -> 0;
        };
    }

    /** Vertex budget of one activation of the pixel look: the cap in quads, 4 vertices each (the vertex buffer enforces it). */
    public static int vertexBudget(VfxShape s) { return capQuads(s) * 4; }

    // ------------------------------------------------------------------------------------------------ the stepped timing of the style
    /** The age the pixel look draws at: the motion is "on twos" (held for two ticks), never interpolated by the frame's partial tick. */
    public static int onTwos(int age) { return age & ~1; }

    /** Flip-book frame (0 or 1): swaps every three ticks. */
    public static int frame(int age) { return (Math.max(0, age) / 3) & 1; }

    /** Alpha in four steps (0, 1/4 ... 1), never a smooth ramp. */
    public static float step4(float a) { return Math.round(Math.max(0f, Math.min(1f, a)) * 4f) / 4f; }

    /** Fade in over {@code in} ticks and out over the last {@code out}, in four steps. */
    public static float fade(int age, int duration, float in, float out) {
        float a = Math.min(1f, (age + 1) / Math.max(1e-3f, in)) * Math.min(1f, (duration - age) / Math.max(1e-3f, out));
        return step4(a);
    }

    /** ARGB with white colour and this alpha (0..1). */
    public static int white(float alpha) { return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | 0xFFFFFF; }

    // ------------------------------------------------------------------------------------------------ palette choice (tint)
    /**
     * Whether an effect's colour is a wind green. Those draw with the pack palette textures and a white vertex colour (the look the owner chose); every other colour (the melee pages that reuse
     * Crescent Slash with their own book colour) draws the grey twin of the texture multiplied by that colour, so the tint keeps working.
     */
    public static boolean nativePalette(int argb) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max < 140 || max == min) return false;                          // dark or grey colours are tints, not greens
        float sat = (max - min) / (float) max;
        if (sat < 0.18f) return false;
        float h;
        if (max == g) h = 60f * (2f + (b - r) / (float) (max - min));
        else if (max == r) h = 60f * (((g - b) / (float) (max - min) + 6f) % 6f);
        else h = 60f * (4f + (r - g) / (float) (max - min));
        return h >= 95f && h <= 175f;
    }

    // ------------------------------------------------------------------------------------------------ textures
    public static final String PREFIX = "wmc_";
    public static final String[] TEXTURE_NAMES = {"crescent", "ribbon", "swirl", "spark", "streak", "leaf", "ring", "flash"};
    public static final int FRAMES = 2;

    /** File name without folder or extension: {@code wmc_crescent_0} or, for the grey twin, {@code wmc_crescent_0_t}. */
    public static String fileName(String name, int frame, boolean tintable) { return PREFIX + name + "_" + frame + (tintable ? "_t" : ""); }

    /** Every texture of the set as a path under assets/nusmp (32 files: 8 names x 2 frames x pack palette / grey twin). */
    public static List<String> allTextures() {
        List<String> out = new ArrayList<>();
        for (String n : TEXTURE_NAMES) for (int f = 0; f < FRAMES; f++) for (int t = 0; t < 2; t++) out.add("textures/particle/" + fileName(n, f, t == 1) + ".png");
        return out;
    }

    /** The five RGB steps of the pack palette (shadow green outline, deep green, lime, mint rim, white core) and of the grey twin. */
    public static final int[] PACK_PALETTE = {0x37641E, 0x46A92C, 0x89F842, 0xCCEEBE, 0xF8FBF0};
    public static final int[] GREY_PALETTE = {0x525252, 0x8C8C8C, 0xC7C7C7, 0xEBEBEB, 0xFFFFFF};

    /** Size of a texture of the set: {width, height}. */
    public static int[] size(String name) {
        return switch (name) {
            case "ribbon" -> new int[]{32, 16};
            case "streak" -> new int[]{32, 8};
            case "spark", "leaf" -> new int[]{16, 16};
            default -> new int[]{32, 32};
        };
    }

    /**
     * Checks one decoded texture of the set (pure, shared by the unit test and the CI self-test): the size of its name, hard alpha only (0 or 255), only the five steps of its palette, some
     * opaque and some clear texels, and clear corners for the square sprites whose name says ring or spark. Returns the problem, or null when the texture is fine.
     */
    public static String checkTexture(String name, boolean tintable, int w, int h, int[] argb) {
        int[] want = size(name);
        if (w != want[0] || h != want[1]) return name + " is " + w + "x" + h + ", expected " + want[0] + "x" + want[1];
        int[] palette = tintable ? GREY_PALETTE : PACK_PALETTE;
        int opaque = 0, clear = 0;
        for (int px : argb) {
            int a = px >>> 24;
            if (a == 0) { clear++; continue; }
            if (a != 255) return name + " has a soft alpha " + a;
            opaque++;
            boolean ok = false;
            for (int c : palette) if ((px & 0xFFFFFF) == c) { ok = true; break; }
            if (!ok) return name + " has a colour outside its palette: " + Integer.toHexString(px & 0xFFFFFF);
        }
        if (opaque == 0 || clear == 0) return name + " is empty or fully opaque";
        if (w == h && (name.equals("ring") || name.equals("spark"))) {
            int[] corners = {0, w - 1, (h - 1) * w, h * w - 1};
            for (int c : corners) if ((argb[c] >>> 24) != 0) return name + " has an opaque corner";
        }
        return null;
    }

    /** True for a texture of the pixel set: the vertex buffer samples it with nearest-neighbour filtering (vanilla particle textures do too), everything else stays smooth. */
    public static boolean isPixelTexture(String path) {
        if (path == null) return false;
        int colon = path.indexOf(':');
        return path.startsWith("textures/particle/" + PREFIX, colon + 1);
    }
}
