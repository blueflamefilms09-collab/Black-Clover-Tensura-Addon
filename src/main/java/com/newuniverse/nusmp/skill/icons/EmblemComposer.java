package com.newuniverse.nusmp.skill.icons;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Why: the animated icons of Devil Magic and Devil Bond (docs/devil_double_magic.md) are composed at run time from small emblems, one for every grimoire magic and one for every devil attribute
 * ({@code textures/skill/emblem/magic|devil/<key>.png}, 16 x 16 white masks made by tools/gen_devil_emblems.py), instead of one painted file per pair (105 magics x 17 attributes). A player who holds both
 * a devil and a grimoire gets the devil's emblem in the middle of a dark disc, the magic's emblem as a badge in the corner, a ring in the devil's colour that breathes and a spark that circles it.
 *
 * <p>Pure and Minecraft-free (plain int arrays, ARGB), so it is unit tested and safe on a server. Bounded by construction: a frame is 32 x 32 = 1024 ints, a strip is {@link #FRAMES} frames, the client
 * keeps at most {@link #MAX_CACHE} strips and composes one only when the pair of keys changes, never per frame. The loop is closed: frame {@code FRAMES} would equal frame 0.
 */
public final class EmblemComposer {
    private EmblemComposer() {}

    public static final int SIZE = 32, EMB = 16, FRAMES = 8, MAX_CACHE = 8, FRAME_TICKS = 5;

    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9_]{0,40}");

    public static boolean validKey(String key) { return key != null && KEY.matcher(key).matches(); }

    public static String magicPath(String key) { return "textures/skill/emblem/magic/" + key + ".png"; }

    public static String devilPath(String key) { return "textures/skill/emblem/devil/" + key + ".png"; }

    /** The colour (0xRRGGBB) of a grimoire magic's soul family (the {@code soulType} of the magic). */
    public static int soulColor(String soul) {
        if (soul == null) return 0xB0B0B0;
        return switch (soul.toUpperCase(Locale.ROOT)) {
            case "FLAME" -> 0xFF6A2A;
            case "WATER" -> 0x3AA0FF;
            case "WIND" -> 0x7FE0C0;
            case "EARTH" -> 0xC8A050;
            case "LIGHT" -> 0xFFF0A0;
            case "DARKNESS" -> 0x8A4AD0;
            case "SPACE" -> 0x6A7AFF;
            case "TIME" -> 0xE0C060;
            case "BATTLE" -> 0xFF5A5A;
            case "FANTASY" -> 0xE070FF;
            default -> 0xB0B0B0;
        };
    }

    private static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t), g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, bl));
    }

    private static void put(int[] px, int x, int y, int rgb, float alpha) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE || alpha <= 0f) return;
        int i = y * SIZE + x, old = px[i];
        float oa = (old >>> 24) / 255f, na = Math.min(1f, alpha), outA = na + oa * (1 - na);
        if (outA <= 0f) return;
        int base = old & 0xFFFFFF;
        int c = oa <= 0f ? rgb : mix(base, rgb, na / outA);
        px[i] = ((int) (outA * 255f + 0.5f) << 24) | c;
    }

    /** The alpha (0..255) of mask pixel (x, y) or 0 outside / for a null or short mask. */
    private static int mask(int[] m, int x, int y, int w) {
        return m == null || x < 0 || y < 0 || x >= w || y >= w || y * w + x >= m.length ? 0 : m[y * w + x] >>> 24;
    }

    /**
     * Frame {@code k} (0 to {@code frames} - 1) of the loop, 32 x 32 ARGB. {@code devilMask} (16 x 16 ARGB, only its alpha is used) is the centre emblem tinted by {@code devilRgb}; {@code magicMask} may be null
     * (no grimoire: the devil alone) and is drawn as an 8 x 8 badge in {@code magicRgb}. The same arguments and {@code k} always give the same picture.
     */
    public static int[] frame(int[] devilMask, int devilRgb, int[] magicMask, int magicRgb, int k, int frames) {
        int n = Math.max(1, frames);
        double ph = 2.0 * Math.PI * (((k % n) + n) % n) / n;
        float pulse = 0.65f + 0.35f * (float) Math.sin(ph);
        int[] px = new int[SIZE * SIZE];
        double c = (SIZE - 1) / 2.0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double r = Math.hypot(x - c, y - c);
                if (r > 15.6) continue;
                if (r >= 13.0) {
                    float edge = (float) Math.min(1.0, (15.6 - r) / 0.8);
                    put(px, x, y, mix(devilRgb, 0xFFFFFF, 0.15f * pulse), (0.45f + 0.5f * pulse) * edge);
                } else {
                    put(px, x, y, mix(0x0C0812, devilRgb, (float) (0.10 + 0.20 * (r / 13.0))), 1f);
                }
            }
        }
        int ox = (SIZE - EMB) / 2, oy = (SIZE - EMB) / 2;
        int lit = mix(devilRgb, 0xFFFFFF, 0.5f);
        for (int y = 0; y < EMB; y++)
            for (int x = 0; x < EMB; x++) {
                int a = mask(devilMask, x, y, EMB);
                if (a > 0) put(px, ox + x, oy + y, lit, Math.min(1f, (a / 255f) * 1.25f) * (0.9f + 0.1f * pulse));
            }
        if (magicMask != null) {                                                              // the grimoire's badge, bottom right: a dark backing and its ring, then the emblem halved
            double bx = 23.0, by = 23.0;
            for (int y = 16; y < SIZE; y++)
                for (int x = 16; x < SIZE; x++) {
                    double r = Math.hypot(x - bx, y - by);
                    if (r <= 6.2) put(px, x, y, r >= 5.0 ? magicRgb : 0x0C0812, 1f);
                }
            int mlit = mix(magicRgb, 0xFFFFFF, 0.3f);
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) {
                    int a = Math.max(Math.max(mask(magicMask, 2 * x, 2 * y, EMB), mask(magicMask, 2 * x + 1, 2 * y, EMB)), Math.max(mask(magicMask, 2 * x, 2 * y + 1, EMB), mask(magicMask, 2 * x + 1, 2 * y + 1, EMB)));
                    if (a > 0) put(px, 19 + x, 19 + y, mlit, a / 255f);
                }
        }
        int sx = (int) Math.round(c + 14.2 * Math.cos(ph - Math.PI / 2)), sy = (int) Math.round(c + 14.2 * Math.sin(ph - Math.PI / 2));   // the spark that circles the ring
        for (int dy = 0; dy < 2; dy++)
            for (int dx = 0; dx < 2; dx++) put(px, sx + dx - 0, sy + dy - 0, 0xFFFFFF, 0.95f);
        return px;
    }

    /** All {@code frames} frames stacked top to bottom (32 x 32N): the strip the animated texture shows one frame at a time. */
    public static int[] strip(int[] devilMask, int devilRgb, int[] magicMask, int magicRgb, int frames) {
        int n = Math.max(1, Math.min(FRAMES, frames));
        int[] out = new int[SIZE * SIZE * n];
        for (int k = 0; k < n; k++) System.arraycopy(frame(devilMask, devilRgb, magicMask, magicRgb, k, n), 0, out, k * SIZE * SIZE, SIZE * SIZE);
        return out;
    }

    /** The cache key of a pair (devil attribute, grimoire magic or empty). */
    public static String cacheKey(String devilKey, String magicKey) { return devilKey + "|" + (magicKey == null ? "" : magicKey); }
}
