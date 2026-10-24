package com.newuniverse.nusmp.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fire VFX, version 2 (docs/vfx_remake/fire_ingame.md): the pure half. No Minecraft class, so it runs in the unit tests, in the CI self-test on a dedicated server and in the headless preview; the painter
 * ({@code vfx.client.layer.FireV2Layer}) reads every number from here.
 *
 * <p>Eleven fire shapes are redrawn, as a mix the owner chose per effect: <b>A "anime cel"</b> (flat baked-colour flame textures drawn with normal alpha: Leo Rugiens, Sol Linea, Ignis Columna, the flame
 * explosion, Wild Bursting Flame, the Flame Zone, the Flame Burst and the Flame Explosion) and <b>B "volumetric glow"</b> (soft additive puffs: Spiral Flame, the Flame Cast and the Flame Trail). Ignis Columna
 * is the pillar plus a defensive flame vortex round it; Wild Bursting Flame is a ground circle of flame with fireballs launched in every direction; the sun-lion sigil of the Flame Zone and the charge sigil of
 * the Flame Cast are kept exactly as they were.
 *
 * <p>Two CLIENT options (file {@code nusmp-fire-v2-client.toml}, mirrors that only the client writes; the dedicated server never reads them and nothing is sent over the network for them):
 * {@code vfx.element.fire} = {@link Style#V2} (default) or {@link Style#V1} (exactly the old code for every shape, rule 2) and {@code vfx.element.fireReducedFlashes} (default false; true drops the hard
 * flash stars and radial rays to a soft fade and slows the flip-book to at most three changes a second).
 *
 * <p>Cost (rules 9 and 11): no packet, no server work. The worst case per activation is {@link #worstQuads} (each is under {@link #capQuads}, the peak of the code it replaces), every draw goes through the
 * vertex budget of the activation ({@link #vertexBudget}) and the distance / particle level of detail of {@code VfxManager} (counts shrink with {@code ctx.seg}).
 */
public final class FireV2 {
    private FireV2() {}

    public enum Style {
        /** The new look (default): anime cel mixed with volumetric glow. */
        V2,
        /** The look before this change, byte for byte the old layers. */
        V1
    }

    public static final Style DEFAULT_STYLE = Style.V2;
    public static final String CONFIG_FILE = "nusmp-fire-v2-client.toml";
    /** The config path of the look: table {@code vfx.element}, key {@code fire}. */
    public static final String CONFIG_PATH = "vfx.element.fire";
    /** The config path of the photosensitivity option: table {@code vfx.element}, key {@code fireReducedFlashes}. */
    public static final String CONFIG_PATH_REDUCED = "vfx.element.fireReducedFlashes";
    public static final boolean DEFAULT_REDUCED_FLASHES = false;

    private static volatile Style style = DEFAULT_STYLE;
    private static volatile boolean reducedFlashes = DEFAULT_REDUCED_FLASHES;

    public static Style style() { return style; }

    /** Written by the client config only. */
    public static void setStyle(Style s) { style = s == null ? DEFAULT_STYLE : s; }

    /** True while the new look is on. */
    public static boolean v2Look() { return style == Style.V2; }

    public static boolean reducedFlashes() { return reducedFlashes; }

    /** Written by the client config only. */
    public static void setReducedFlashes(boolean on) { reducedFlashes = on; }

    /** Reads the option text; anything unknown is the default. */
    public static Style parse(String text) {
        if (text == null) return DEFAULT_STYLE;
        String t = text.strip().toUpperCase(Locale.ROOT);
        for (Style s : Style.values()) if (s.name().equals(t)) return s;
        return DEFAULT_STYLE;
    }

    // ------------------------------------------------------------------------------------------------ the shapes
    /** The shapes the new look takes over (the layer registers for exactly these). */
    public static final List<VfxShape> SHAPES = List.of(VfxShape.FIRE_LION, VfxShape.FIRE_SPEAR, VfxShape.FIRE_PILLAR, VfxShape.FIRE_BURST, VfxShape.FIRE_SPIRAL, VfxShape.FIRE_WILD,
            VfxShape.FLAMEFX_CAST, VfxShape.FLAMEFX_ZONE, VfxShape.FLAMEFX_BURST, VfxShape.FLAME_TRAIL, VfxShape.FLAME_EXPLOSION);
    /** Fire shapes that this change must never touch (the Calderos eruption effect and the Demon Fire family have their own layers). */
    public static final List<VfxShape> UNTOUCHED = List.of(VfxShape.FX_FIRE_ERUPTION, VfxShape.DEMON_FIRE_FX1, VfxShape.DEMON_FIRE_FX2, VfxShape.DEMON_FIRE_FX3);

    public static boolean owns(VfxShape s) { return SHAPES.contains(s); }

    /** Style of a shape: true = A "anime cel" (alpha-blended baked colour), false = B "volumetric glow" (additive soft puffs). */
    public static boolean isCel(VfxShape s) {
        return switch (s) {
            case FIRE_SPIRAL, FLAMEFX_CAST, FLAME_TRAIL -> false;
            default -> true;
        };
    }

    // ------------------------------------------------------------------------------------------------ what each effect draws (the painter reads these)
    // FIRE_LION (A)
    public static final int LION_TONGUES = 7, LION_CHIPS = 9;
    // FIRE_SPEAR (A)
    public static final int SPEAR_TONGUES = 5, SPEAR_CHIPS = 7;
    // FIRE_PILLAR (A, plus the vortex)
    public static final int PILLAR_TONGUES = 5, PILLAR_LOOSE = 7, PILLAR_CHIPS = 9, VORTEX_STRANDS = 3, VORTEX_PIECES = 3;
    // FIRE_BURST (A)
    public static final int BURST_TONGUES = 10, BURST_CHIPS = 12, BURST_SMOKE = 3;
    // FIRE_SPIRAL (B)
    public static final int SPIRAL_STRANDS = 3, SPIRAL_POINTS = 11, SPIRAL_EMBERS = 12;
    // FIRE_WILD (A, redone: a ground circle of flame, fireballs with a light beam thrown in every direction)
    public static final int WILD_WAVES = 3, WILD_BALLS = 5, WILD_UPRIGHTS = 12, WILD_CHIPS = 8;
    // FLAMEFX_CAST (B, the charge sigil is the old one)
    public static final int CAST_PUFFS = 6, CAST_EMBERS = 10;
    // FLAMEFX_ZONE (A flames round the old sigil)
    public static final int ZONE_TONGUES = 14, ZONE_INNER = 6, ZONE_CHIPS = 8;
    // FLAMEFX_BURST (A)
    public static final int FXBURST_TONGUES = 10, FXBURST_CHIPS = 10, FXBURST_SMOKE = 3;
    // FLAME_TRAIL (B)
    public static final int TRAIL_PUFFS = 7, TRAIL_EMBERS = 12;
    // FLAME_EXPLOSION (A)
    public static final int EXPL_BALLS = 6, EXPL_TONGUES = 8, EXPL_CHIPS = 10, EXPL_SMOKE = 3;

    /** The most quads one activation can add (every part at full detail, every timed part showing at once). */
    public static int worstQuads(VfxShape s) {
        return switch (s) {
            case FIRE_LION -> 1 + 1 + 2 + LION_TONGUES + 1 + 1 + LION_CHIPS;                                                         // halo, rays, two strips, tongues, head, flash, chips
            case FIRE_SPEAR -> 1 + 2 + SPEAR_TONGUES + 1 + 1 + SPEAR_CHIPS;                                                           // halo, two strips, tongues, ball, flash, chips
            case FIRE_PILLAR -> 1 + 1 + PILLAR_TONGUES + PILLAR_LOOSE + PILLAR_CHIPS + 1 + VORTEX_STRANDS * VORTEX_PIECES;           // glow, ring, tongues, loose, chips, swirl decal, vortex strips
            case FIRE_BURST -> 1 + 1 + 1 + 1 + 1 + BURST_TONGUES + BURST_CHIPS + BURST_SMOKE;                                       // halo, rays, ring, ball, flash, tongues, chips, smoke
            case FIRE_SPIRAL -> 1 + 1 + SPIRAL_STRANDS * SPIRAL_POINTS + 1 + 1 + SPIRAL_EMBERS;                                      // halo, core streak, puffs, head puff, head halo, embers
            case FIRE_WILD -> 1 + 1 + 1 + 1 + WILD_UPRIGHTS + WILD_WAVES * WILD_BALLS * 3 + WILD_CHIPS;                              // glow, floor glow, floor, ring, flame wall, (ball + glow + beam) per ball, chips
            case FLAMEFX_CAST -> 3 + 1 + 2 + CAST_PUFFS + 1 + 1 + CAST_EMBERS + 2;                                                   // old sigil (lion, ring, glow), halo, streaks, puffs, head, head halo, embers, arrival (rays, ring)
            case FLAMEFX_ZONE -> 3 + ZONE_TONGUES + ZONE_INNER + ZONE_CHIPS;                                                         // old glow, lion sigil, ring, tongues, inner tongues, chips
            case FLAMEFX_BURST -> 1 + 1 + 1 + 1 + FXBURST_TONGUES + FXBURST_CHIPS + FXBURST_SMOKE;                                  // halo, rays, flash, ring, tongues, chips, smoke
            case FLAME_TRAIL -> 1 + 2 + TRAIL_PUFFS + 1 + 1 + TRAIL_EMBERS;                                                          // halo, two streaks, puffs, head puff, head halo, embers
            case FLAME_EXPLOSION -> 1 + 1 + 1 + EXPL_BALLS + EXPL_TONGUES + 1 + EXPL_CHIPS + EXPL_SMOKE;                             // halo, ring, rays, balls, tongues, flash, chips, smoke
            default -> 0;
        };
    }

    /** The peak quads of the code each shape replaced (peak of every tick of its life through the real old layers, docs/previews/vfx_remake/fire_new/fire_ingame_budget.json): the new look stays under these. */
    public static int capQuads(VfxShape s) {
        return switch (s) {
            case FIRE_LION -> 66;
            case FIRE_SPEAR -> 49;
            case FIRE_PILLAR -> 55;
            case FIRE_BURST -> 61;
            case FIRE_SPIRAL -> 67;
            case FIRE_WILD -> 93;
            case FLAMEFX_CAST -> 45;
            case FLAMEFX_ZONE -> 39;
            case FLAMEFX_BURST -> 54;
            case FLAME_TRAIL -> 41;
            case FLAME_EXPLOSION -> 73;
            default -> 0;
        };
    }

    /** Vertex budget of one activation of the new look: the cap in quads, 4 vertices each (the vertex buffer enforces it). */
    public static int vertexBudget(VfxShape s) { return capQuads(s) * 4; }

    // ------------------------------------------------------------------------------------------------ timing (flip-book) and the reduced-flashes rules
    /** Ticks one flip-book frame is held: 2 normally (the cel look), 7 with reduced flashes (at most three changes a second). */
    public static int framePeriod(boolean reduced) { return reduced ? 7 : 2; }

    /** Flip-book frame 0..n-1 of tick {@code age} (offset i staggers the sprites): {@code floor(age / period + i) mod n}. */
    public static int frame(int age, int offset, int n, boolean reduced) {
        int k = Math.max(0, age) / framePeriod(reduced) + offset;
        return ((k % n) + n) % n;
    }

    /** Frame changes per second at 20 ticks a second. */
    public static float framesPerSecond(boolean reduced) { return 20f / framePeriod(reduced); }

    /** Alpha of a HARD flash sprite (the pale star / spoke burst): full for the first {@code ticks} ticks, then gone; never drawn with reduced flashes. */
    public static float hardFlash(int age, int ticks, boolean reduced) { return reduced ? 0f : (age >= 0 && age < ticks ? 1f : 0f); }

    /** Alpha of the soft replacement of a flash (a plain halo): only with reduced flashes, a slow fade that starts at 0.5 and never rises. */
    public static float softFlash(int age, int ticks, boolean reduced) {
        if (!reduced || age < 0) return 0f;
        return Math.max(0f, 0.5f * (1f - age / (2f * Math.max(1, ticks))));
    }

    /** A pulse 1 - amount .. 1 at {@code rate} radians per tick; constant 1 - amount / 2 with reduced flashes (no pulsing light). */
    public static float pulse(float age, float rate, float amount, boolean reduced) {
        return reduced ? 1f - amount * 0.5f : 1f - amount * 0.5f + amount * 0.5f * (float) Math.sin(age * rate);
    }

    /** Roll of a spinning sprite: with reduced flashes the hard sprites do not spin (a turning star strobes). */
    public static float spin(float angle, boolean reduced) { return reduced ? 0f : angle; }

    // ------------------------------------------------------------------------------------------------ palette choice (tint)
    /**
     * Whether an effect's colour is a fire colour (red, orange, gold, yellow). Those draw the baked fire textures with a white vertex colour (the look the owner chose); every other colour (the pages that
     * reuse the fire shapes with a blue, green, purple or black flame) draws the grey twin of the texture multiplied by that colour, so the tint keeps working.
     */
    public static boolean nativeFire(int argb) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max < 150 || max == min) return false;                           // dark or grey colours are tints, not fire
        float sat = (max - min) / (float) max;
        if (sat < 0.35f) return false;
        float h;
        if (max == r) h = 60f * (((g - b) / (float) (max - min) + 6f) % 6f);
        else if (max == g) h = 60f * (2f + (b - r) / (float) (max - min));
        else h = 60f * (4f + (r - g) / (float) (max - min));
        return h <= 65f || h >= 345f;
    }

    // ------------------------------------------------------------------------------------------------ textures
    public static final String PREFIX = "fnw_";
    /** The 34 textures (A = cel, B = glow); each has a grey twin {@code _t} except the two smoke puffs. */
    public static final String[] TEXTURE_NAMES = {
            "a_tongue_a", "a_tongue_b", "a_tongue_c", "a_ball_a", "a_ball_b", "a_ball_c", "a_body_a", "a_body_b", "a_body_c", "a_lion_a", "a_lion_b", "a_swoosh_a", "a_swoosh_b",
            "a_ring", "a_rays", "a_flash", "a_smoke_a", "a_smoke_b", "a_chip_a", "a_chip_b", "a_orb_a", "a_orb_b", "a_beam", "a_floor", "a_swirl",
            "b_ember", "b_halo", "b_puff_a", "b_puff_b", "b_puff_c", "b_puff_d", "b_rays", "b_ring", "b_streak"};

    /** The textures that have no grey twin: the smoke stays the same dark brown-grey whatever the colour of the spell. */
    public static boolean noTwin(String name) { return name.contains("smoke"); }

    /** File name without folder or extension: {@code fnw_a_tongue_a} or, for the grey twin, {@code fnw_a_tongue_a_t}. */
    public static String fileName(String name, boolean tintable) { return PREFIX + name + (tintable ? "_t" : ""); }

    /** Every texture of the set as a path under assets/nusmp (34 files + 32 twins = 66). */
    public static List<String> allTextures() {
        List<String> out = new ArrayList<>();
        for (String n : TEXTURE_NAMES) {
            out.add("textures/particle/" + fileName(n, false) + ".png");
            if (!noTwin(n)) out.add("textures/particle/" + fileName(n, true) + ".png");
        }
        return out;
    }

    /** Size of a texture of the set: {width, height}. */
    public static int[] size(String name) {
        return switch (name) {
            case "a_tongue_a", "a_tongue_b", "a_tongue_c" -> new int[]{128, 256};
            case "a_body_a", "a_body_b", "a_body_c" -> new int[]{256, 128};
            case "a_lion_a", "a_lion_b" -> new int[]{512, 320};
            case "a_swoosh_a", "a_swoosh_b" -> new int[]{256, 96};
            case "a_ring", "a_rays", "a_floor", "a_swirl", "b_halo", "b_rays", "b_ring" -> new int[]{256, 256};
            case "a_chip_a", "a_chip_b" -> new int[]{16, 16};
            case "b_ember" -> new int[]{32, 32};
            case "a_beam" -> new int[]{256, 32};
            case "b_streak" -> new int[]{256, 64};
                        default -> new int[]{128, 128};
        };
    }

    /** Square sprites that must have clear corners (they are drawn on a card: an opaque corner would show as a square). */
    private static boolean needsClearCorners(String name, int w, int h) {
        if (w != h) return false;
        return name.contains("ring") || name.contains("rays") || name.contains("halo") || name.contains("spark") || name.contains("puff") || name.contains("orb") || name.contains("ball")
                || name.contains("flash") || name.contains("floor") || name.contains("swirl") || name.contains("ember") || name.contains("smoke") || name.contains("chip");
    }

    /**
     * Checks one decoded texture of the set (pure, shared by the unit test and the CI self-test): the size of its name, some visible and some clear texels, clear corners for the square sprites, no colour
     * under a fully transparent texel that is black-and-white-wrong (bled colour is fine), and for a grey twin red = green = blue everywhere. Returns the problem, or null when the texture is fine.
     */
    public static String checkTexture(String name, boolean tintable, int w, int h, int[] argb) {
        int[] want = size(name);
        if (w != want[0] || h != want[1]) return name + " is " + w + "x" + h + ", expected " + want[0] + "x" + want[1];
        int visible = 0, clear = 0;
        for (int px : argb) {
            int a = px >>> 24;
            if (a == 0) { clear++; continue; }
            if (a > 40) visible++;
            if (tintable) {
                int r = (px >> 16) & 255, g = (px >> 8) & 255, b = px & 255;
                if (r != g || g != b) return name + " twin is not grey";
            }
        }
        if (visible == 0 || clear == 0) return name + " is empty or fully opaque";
        if (needsClearCorners(name, w, h)) {
            int[] corners = {0, w - 1, (h - 1) * w, h * w - 1};
            for (int c : corners) if ((argb[c] >>> 24) != 0) return name + " has a visible corner";
        }
        return null;
    }
}
