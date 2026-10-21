package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicSlot;

/**
 * The 1.110.0 worn layer of a HEAD, CHEST or FEET relic: the same 64x32 vanilla humanoid layout and the very same covered pixels as {@link ArmorPainter} (so nothing clips with the
 * player model or its sleeves and every coverage rule of the old painter still holds), repainted cleaner. Used while the client switch {@code visual.upgrade} is on; the old painter
 * stays and answers when it is off. Pure arithmetic, deterministic from the arguments.
 * <ol>
 *   <li><b>Palette ramps</b>: every body pixel of the old painting is snapped to a five step ramp of the main colour or of the trim colour (shadows lean cool, lights lean warm), which
 *       removes the grain that made the old layer look muddy and keeps one palette per relic.</li>
 *   <li><b>Bevel</b>: each face of the boxes gets a lit top and left edge and a dark bottom and right edge.</li>
 *   <li><b>Aligned trim</b>: the yoke row and the hem rows of the torso and of both arms are one trim line, so the panels of the chest and the sleeves meet.</li>
 *   <li><b>Emblem</b>: a small sigil (one of four, picked by the relic) in the glow colour with a trim outline on the breast (on the back for a cloak, cape or mantle), a gem on a
 *       helmet's brow; the emblem is part of the glow channel ({@link #paintGlow}), which the worn layer draws again at full brightness through {@code ArmorGlow}.</li>
 * </ol>
 */
public final class ArmorPainterV2 {
    private ArmorPainterV2() {}

    private static final double[] RAMP = {0.60, 0.80, 1.0, 1.16, 1.34};
    private static final String[][] EMBLEMS = {
            {".##.", "####", "####", ".##."},
            {".##.", "####", ".##.", ".##."},
            {"####", "#..#", "#..#", ".##."},
            {"#..#", ".##.", ".##.", "#..#"}};

    /** One painted texture: 64 x 32 ARGB pixels. Covers exactly the pixels {@link ArmorPainter#paint} covers. */
    public static int[] paint(Look look, RelicSlot slot, int main, int trim, int glow, long seed) { return build(look, slot, main, trim, glow, seed).px; }

    /** The glow channel of the same texture (clear where nothing glows), aligned pixel for pixel with {@link #paint}. */
    public static int[] paintGlow(Look look, RelicSlot slot, int main, int trim, int glow, long seed) {
        ArmorPainter.Canvas c = build(look, slot, main, trim, glow, seed);
        int[] out = new int[ArmorPainter.W * ArmorPainter.H];
        for (int i = 0; i < out.length; i++) if (c.glowMask[i]) out[i] = ArmorPainter.shade(c.px[i], 1.12);
        return out;
    }

    /** The main colour lifted when it is so dark that the shading would be lost (a relic of a black palette still reads as a shaped piece). */
    static int lift(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, mx = Math.max(r, Math.max(g, b));
        if (mx == 0) return 0x2A2A32;
        if (mx >= 70) return rgb & 0xFFFFFF;
        double k = 70.0 / mx;
        return Math.min(255, (int) (r * k)) << 16 | Math.min(255, (int) (g * k)) << 8 | Math.min(255, (int) (b * k));
    }

    private static int mix(int a, int b, double k) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * k) << 16 | (int) (ag + (bg - ag) * k) << 8 | (int) (ab + (bb - ab) * k);
    }

    private static int[] ramp(int rgb) {
        int base = 0xFF000000 | rgb;
        int[] out = new int[RAMP.length];
        for (int i = 0; i < RAMP.length; i++) {
            int c = ArmorPainter.shade(base, RAMP[i]);
            out[i] = RAMP[i] < 1.0 ? mix(c, 0x1A1030, 0.14 * (1.0 - RAMP[i]) / 0.4) : RAMP[i] > 1.0 ? mix(c, 0xFFF0D0, 0.10 * (RAMP[i] - 1.0) / 0.34) : c;
        }
        return out;
    }

    private static int dist(int a, int b) {
        int dr = (a >> 16 & 255) - (b >> 16 & 255), dg = (a >> 8 & 255) - (b >> 8 & 255), db = (a & 255) - (b & 255);
        return dr * dr + dg * dg + db * db;
    }

    private static int snap(int p, int[] mainRamp, int[] trimRamp) {
        int best = p, bd = Integer.MAX_VALUE;
        for (int[] r : new int[][]{mainRamp, trimRamp}) for (int c : r) {
            int d = dist(p, c);
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    private static final int[][] FACES = {
            {20, 16, 8, 4}, {28, 16, 8, 4}, {16, 20, 4, 12}, {20, 20, 8, 12}, {28, 20, 4, 12}, {32, 20, 8, 12},          // the torso
            {44, 16, 4, 4}, {48, 16, 4, 4}, {40, 20, 4, 12}, {44, 20, 4, 12}, {48, 20, 4, 12}, {52, 20, 4, 12},          // the arm
            {8, 0, 8, 8}, {16, 0, 8, 8}, {0, 8, 8, 8}, {8, 8, 8, 8}, {16, 8, 8, 8}, {24, 8, 8, 8},                      // the head
            {4, 16, 4, 4}, {8, 16, 4, 4}, {0, 20, 4, 12}, {4, 20, 4, 12}, {8, 20, 4, 12}, {12, 20, 4, 12}};              // the leg box (boots)

    static ArmorPainter.Canvas build(Look look, RelicSlot slot, int main, int trim, int glow, long seed) {
        ArmorPainter.Canvas c = ArmorPainter.painted(look, slot, lift(main), trim & 0xFFFFFF, glow, seed);
        int[] mr = ramp(lift(main)), tr = ramp(trim & 0xFFFFFF);
        int W = ArmorPainter.W, H = ArmorPainter.H;
        // 1. one palette
        int[] before = c.px.clone();
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int p = before[y * W + x];
            if (p >>> 24 != 255 || c.glowAt(x, y)) continue;
            c.set(x, y, snap(p, mr, tr));
        }
        // 2. bevel on every face that is painted
        for (int[] f : FACES) bevel(c, f[0], f[1], f[2], f[3]);
        boolean cape = look == Look.CLOAK || look == Look.CAPE || look == Look.MANTLE;
        int trimLine = tr[3], trimDark = tr[1];
        if (slot == RelicSlot.CHEST) {
            // 3. the yoke row and the hem rows of the torso and the arms are one line
            for (int[] f : FACES) {
                if (f[1] < 20 || f[0] < 16 || f[3] != 12) continue;
                for (int i = 0; i < f[2]; i++) {
                    c.setIfOpaque(f[0] + i, f[1], trimLine);
                    c.setIfOpaque(f[0] + i, f[1] + 10, trimLine);
                    c.setIfOpaque(f[0] + i, f[1] + 11, trimDark);
                }
            }
            // 4. the emblem: the breast (the back for a cloak)
            emblem(c, cape ? 34 : 22, 22, (int) ((seed >>> 3 ^ seed) & 3), tr, glow);
        } else if (slot == RelicSlot.HEAD) {
            for (int x : new int[]{0, 8, 16, 24}) {                                    // the lower edge of the band round the head
                for (int i = 0; i < 8; i++) for (int j = 8; j < 15; j++) {
                    if (c.get(x + i, j) >>> 24 == 255 && c.get(x + i, j + 1) >>> 24 == 0 && !c.glowAt(x + i, j)) c.set(x + i, j, trimDark);
                }
            }
            if (c.get(11, 9) >>> 24 == 255 || c.get(12, 9) >>> 24 == 255) gem(c, 11, 9, tr, glow);
        } else if (slot == RelicSlot.FEET) {
            for (int[] f : new int[][]{{0, 20}, {4, 20}, {8, 20}, {12, 20}}) {         // a cuff line under the knee of every side
                for (int i = 0; i < 4; i++) c.setIfOpaque(f[0] + i, 24, trimLine);
            }
        }
        return c;
    }

    private static void bevel(ArmorPainter.Canvas c, int x, int y, int w, int h) {
        if (w < 4 || h < 3) return;
        for (int i = 0; i < w; i++) {
            lightIf(c, x + i, y, 1.16);
            lightIf(c, x + i, y + h - 1, 0.76);
        }
        for (int j = 1; j < h - 1; j++) {
            lightIf(c, x, y + j, 1.07);
            lightIf(c, x + w - 1, y + j, 0.86);
        }
    }

    private static void lightIf(ArmorPainter.Canvas c, int x, int y, double f) {
        int p = c.get(x, y);
        if (p >>> 24 == 255 && !c.glowAt(x, y)) c.set(x, y, ArmorPainter.shade(p, f));
    }

    private static void emblem(ArmorPainter.Canvas c, int x0, int y0, int which, int[] tr, int glow) {
        String[] shape = EMBLEMS[which & 3];
        int g = 0xFF000000 | (glow & 0xFFFFFF);
        for (int j = -1; j <= shape.length; j++) for (int i = -1; i <= shape[0].length(); i++) {          // the outline first, then the glyph over it
            boolean in = j >= 0 && j < shape.length && i >= 0 && i < shape[0].length() && shape[j].charAt(i) == '#';
            if (!in) c.setIfOpaque(x0 + i, y0 + j, tr[1]);
        }
        for (int j = 0; j < shape.length; j++) for (int i = 0; i < shape[0].length(); i++) {
            if (shape[j].charAt(i) != '#') continue;
            c.setIfOpaque(x0 + i, y0 + j, i == 0 || j == 0 ? ArmorPainter.shade(g, 1.3) : g);
        }
    }

    private static void gem(ArmorPainter.Canvas c, int x0, int y0, int[] tr, int glow) {
        int g = 0xFF000000 | (glow & 0xFFFFFF);
        for (int j = -1; j <= 2; j++) for (int i = -1; i <= 2; i++) if (j < 0 || j > 1 || i < 0 || i > 1) c.setIfOpaque(x0 + i, y0 + j, tr[1]);
        c.setIfOpaque(x0, y0, ArmorPainter.shade(g, 1.3));
        c.setIfOpaque(x0 + 1, y0, g);
        c.setIfOpaque(x0, y0 + 1, g);
        c.setIfOpaque(x0 + 1, y0 + 1, ArmorPainter.shade(g, 0.8));
    }
}
