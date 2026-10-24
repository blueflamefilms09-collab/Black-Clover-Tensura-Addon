package com.newuniverse.nusmp.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Star Magic VFX, version 2 (docs/vfx_remake/star_ingame.md): the pure half. No Minecraft class, so it runs in the unit tests, in the CI self-test on a dedicated server and in the headless preview; the painters
 * ({@code vfx.client.layer.StarMagicLayer} for the seven effect shapes, {@code client.prop.StarPropPainter} for the placed stars and the pane) read every number from here.
 *
 * <p>The look is the owner's mix of A "anime cel" and B "volumetric glow", no C meshes. A (flat baked colour, alpha blended, stepped two-frame timing) is the base of the star node, the constellation
 * lines, Quartile Scutum and the whip. B (soft additive bloom) is Conjunction, the far-distance sprites of the stars, the dome, and the beam of Quartile Hasta (whose head is an A spear). The palette is a
 * PLACEHOLDER (gold-white and starlight blue): no canon still of Star Magic was available to the author.
 *
 * <p>Two CLIENT options (file {@code nusmp-star-v2-client.toml}, mirrors only the client writes; nothing is sent over the network for them): {@code vfx.element.star} = {@link Style#V2} (default) or
 * {@link Style#V1} (the seven shapes are drawn as the old {@code STAR_MAGIC} starlight beam and a placed star is a plain glow) and {@code vfx.element.starReducedFlashes} (default false; true drops the hard
 * flash stars, the speed lines and the flare to a soft fade and stops the twinkling).
 *
 * <p>Cost (rules 9 and 11): no packet and no server work of its own. The worst case of every activation is {@link #worstQuads} (each under {@link #capQuads}); a placed star is {@value #NODE_QUADS_NEAR} quads near,
 * {@value #NODE_QUADS_FAR} when far or when more than {@value #NODE_NEAR_PER_FRAME} stars are in view, and nothing beyond {@value #NODE_MAX_BLOCKS} blocks; a pane is at most {@value #PANE_QUADS} quads.
 */
public final class StarV2 {
    private StarV2() {}

    public enum Style {
        /** The new look (default). */
        V2,
        /** The look before this change: the seven shapes draw as the old starlight beam, a star is a plain glow. */
        V1
    }

    public static final Style DEFAULT_STYLE = Style.V2;
    public static final String CONFIG_FILE = "nusmp-star-v2-client.toml";
    /** Table {@code vfx.element}, key {@code star}. */
    public static final String CONFIG_PATH = "vfx.element.star";
    /** Table {@code vfx.element}, key {@code starReducedFlashes}. */
    public static final String CONFIG_PATH_REDUCED = "vfx.element.starReducedFlashes";
    public static final boolean DEFAULT_REDUCED_FLASHES = false;

    private static volatile Style style = DEFAULT_STYLE;
    private static volatile boolean reducedFlashes = DEFAULT_REDUCED_FLASHES;

    public static Style style() { return style; }

    /** Written by the client config only. */
    public static void setStyle(Style s) { style = s == null ? DEFAULT_STYLE : s; }

    public static boolean v2Look() { return style == Style.V2; }

    public static boolean reducedFlashes() { return reducedFlashes; }

    /** Written by the client config only. */
    public static void setReducedFlashes(boolean on) { reducedFlashes = on; }

    public static Style parse(String text) {
        if (text == null) return DEFAULT_STYLE;
        String t = text.strip().toUpperCase(Locale.ROOT);
        for (Style s : Style.values()) if (s.name().equals(t)) return s;
        return DEFAULT_STYLE;
    }

    // ------------------------------------------------------------------------------------------------ the shapes
    /** The shapes the new layer owns (it registers for exactly these). STAR_MAGIC is not one of them: it stays with {@code ElementIdentityLayer} for every other page and caller. */
    public static final List<VfxShape> SHAPES = List.of(VfxShape.STAR_NODE, VfxShape.STAR_LINK, VfxShape.STAR_CONJ, VfxShape.STAR_PANE, VfxShape.STAR_LANCE, VfxShape.STAR_WHIP, VfxShape.STAR_DOME);

    public static boolean owns(VfxShape s) { return SHAPES.contains(s); }

    /** Style of a shape: true = A "anime cel" (alpha-blended baked colour), false = B "volumetric glow" (additive soft sprites). The lance mixes both (an A spear head, a B beam tail). */
    public static boolean isCel(VfxShape s) {
        return switch (s) {
            case STAR_CONJ, STAR_DOME -> false;
            default -> true;
        };
    }

    public static final int NODE_COMET_SPARKS = 4, LINK_SPARKS = 2, CONJ_DUST = 3, PANE_SHARDS = 8, LANCE_SPARKS = 3, WHIP_PIECES = 3, DOME_STARS = 6, DOME_DUST = 3;

    /** The most quads one activation can add (every part at full detail, every timed part showing at once). */
    public static int worstQuads(VfxShape s) {
        return switch (s) {
            case STAR_NODE -> 1 + 1 + 1 + NODE_COMET_SPARKS;                                           // comet streak, head glow, pop star, sparks
            case STAR_LINK -> 1 + 1 + LINK_SPARKS + 2;                                                  // line, soft ribbon under it, end sparks, two end halos
            case STAR_CONJ -> 2 + 2 + 1 + CONJ_DUST + 1 + 1 + 1 + 1;                                    // two flares, two rings, line, dust, rays (hard), star pop (burst + bloom), speed lines
            case STAR_PANE -> Math.max(3, 3 + PANE_SHARDS);                                             // a hit: ring, star, glow; a break: ring, flash, glow and the shards
            case STAR_LANCE -> 1 + 1 + 1 + 1 + 1 + LANCE_SPARKS + 1;                                    // tail, core, head flare, launch star, comet trail, sparks, afterglow
            case STAR_WHIP -> WHIP_PIECES + 1 + 1;                                                      // ribbon pieces, soft ribbon, end spark
            case STAR_DOME -> 1 + 1 + DOME_STARS + DOME_DUST + 1;                                       // ground sigil, ring, rim stars, dust, rays (hard)
            default -> 0;
        };
    }

    /** The peak quads the layer may ever add for one activation; the activation's vertex budget is this times four (never over the engine's 400). */
    public static int capQuads(VfxShape s) {
        return switch (s) {
            case STAR_NODE -> 12;
            case STAR_LINK -> 8;
            case STAR_CONJ -> 18;
            case STAR_PANE -> 12;
            case STAR_LANCE -> 14;
            case STAR_WHIP -> 10;
            case STAR_DOME -> 18;
            default -> 0;
        };
    }

    public static int vertexBudget(VfxShape s) { return capQuads(s) * 4; }

    // ------------------------------------------------------------------------------------------------ the placed star and the pane (prop painter)
    public static final int NODE_QUADS_NEAR = 7, NODE_QUADS_FAR = 2, NODE_NEAR_PER_FRAME = 48, PANE_QUADS = 8;
    public static final double NODE_FAR_BLOCKS = 24.0, NODE_MAX_BLOCKS = 64.0;

    /** How many quads a placed star draws: {@code distance} from the camera in blocks, {@code drawnNear} the stars already drawn in full this frame. */
    public static int nodeQuads(double distance, int drawnNear) {
        if (distance > NODE_MAX_BLOCKS) return 0;
        if (distance > NODE_FAR_BLOCKS || drawnNear >= NODE_NEAR_PER_FRAME) return NODE_QUADS_FAR;
        return NODE_QUADS_NEAR;
    }

    /** The most quads one frame can ever spend on placed stars and panes in view on a crowded server: 120 stars (20 mages of 6) with at most 48 in full. */
    public static int worstFrameQuads(int stars, int panes) {
        int near = Math.min(stars, NODE_NEAR_PER_FRAME);
        return near * NODE_QUADS_NEAR + (stars - near) * NODE_QUADS_FAR + panes * PANE_QUADS;
    }

    // ------------------------------------------------------------------------------------------------ timing and the reduced-flashes rules
    /** Ticks one two-frame step is held: 2 normally (cel timing), 7 with reduced flashes (at most three changes a second). */
    public static int framePeriod(boolean reduced) { return reduced ? 7 : 2; }

    public static int frame(int age, int offset, int n, boolean reduced) {
        int k = Math.max(0, age) / framePeriod(reduced) + offset;
        return ((k % n) + n) % n;
    }

    public static float framesPerSecond(boolean reduced) { return 20f / framePeriod(reduced); }

    /** Alpha of a HARD flash (the burst star, the speed lines): full for the first {@code ticks} ticks, then gone; never drawn with reduced flashes. */
    public static float hardFlash(int age, int ticks, boolean reduced) { return reduced ? 0f : (age >= 0 && age < ticks ? 1f : 0f); }

    /** The soft replacement of a flash: only with reduced flashes, a slow fade that starts at 0.5 and never rises. */
    public static float softFlash(int age, int ticks, boolean reduced) {
        if (!reduced || age < 0) return 0f;
        return Math.max(0f, 0.5f * (1f - age / (2f * Math.max(1, ticks))));
    }

    /** A twinkle 1 - amount .. 1; constant with reduced flashes. */
    public static float twinkle(float age, float rate, float amount, boolean reduced) {
        return reduced ? 1f - amount * 0.5f : 1f - amount * 0.5f + amount * 0.5f * (float) Math.sin(age * rate);
    }

    /** Roll of a spinning sprite: none with reduced flashes. */
    public static float spin(float angle, boolean reduced) { return reduced ? 0f : angle; }

    // ------------------------------------------------------------------------------------------------ textures
    public static final String PREFIX = "stv_";
    public static final String[] TEXTURE_NAMES = {
            "a_star4", "a_spark", "a_sigil", "a_line", "a_pane", "a_ring", "a_shard", "a_burst", "a_whip", "a_beam", "a_crack", "a_rays",
            "b_glow", "b_glow_blue", "b_flare", "b_star", "b_beam", "b_ring", "b_rays", "b_dust_a", "b_dust_b", "b_dust_c", "b_trail", "b_sigil"};

    public static String fileName(String name) { return PREFIX + name; }

    /** Every texture of the set as a path under assets/nusmp (24 files). */
    public static List<String> allTextures() {
        List<String> out = new ArrayList<>();
        for (String n : TEXTURE_NAMES) out.add("textures/particle/" + fileName(n) + ".png");
        return out;
    }

    public static int[] size(String name) {
        return switch (name) {
            case "a_line" -> new int[]{256, 32};
            case "a_pane" -> new int[]{256, 192};
            case "a_beam", "b_beam" -> new int[]{512, 64};
            case "a_whip" -> new int[]{512, 48};
            case "b_trail" -> new int[]{256, 64};
            case "a_shard", "a_spark" -> new int[]{64, 64};
            case "a_burst" -> new int[]{160, 160};
            case "a_ring", "b_ring", "b_star" -> new int[]{192, 192};
            case "a_sigil", "a_rays", "b_rays", "b_flare", "b_sigil" -> new int[]{256, 256};
            default -> new int[]{128, 128};
        };
    }

    private static boolean needsClearCorners(String name, int w, int h) {
        return w == h && !name.equals("a_shard");
    }

    /** Checks one decoded texture (pure, shared by the unit test and the CI self-test): its size, some visible and some clear texels, clear corners for the square sprites. Null when fine. */
    public static String checkTexture(String name, int w, int h, int[] argb) {
        int[] want = size(name);
        if (w != want[0] || h != want[1]) return name + " is " + w + "x" + h + ", expected " + want[0] + "x" + want[1];
        int visible = 0, clear = 0;
        for (int px : argb) {
            int a = px >>> 24;
            if (a == 0) { clear++; continue; }
            if (a > 40) visible++;
        }
        if (visible == 0 || clear == 0) return name + " is empty or fully opaque";
        if (needsClearCorners(name, w, h)) {
            int[] corners = {0, w - 1, (h - 1) * w, h * w - 1};
            for (int c : corners) if ((argb[c] >>> 24) != 0) return name + " has a visible corner";
        }
        return null;
    }
}
