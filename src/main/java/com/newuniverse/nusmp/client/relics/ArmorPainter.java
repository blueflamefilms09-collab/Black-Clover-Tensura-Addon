package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicSlot;

/**
 * Paints the worn layer of a HEAD or CHEST relic: a 64x32 ARGB image in the layout of vanilla's humanoid armour texture, from the def's palette
 * (main material, trim, glow) and look. Pure arithmetic (no Minecraft types) so the layout is tested; the client turns the pixels into a texture
 * ({@link RelicTextures}). Everything is deterministic from the arguments: the same def paints the same pixels on every machine.
 * <p>
 * Layout (outer layer, x then y): head box at (0,0): top (8,0), bottom (16,0), right (0,8), front (8,8), left (16,8), back (24,8), each 8x8; body box at
 * (16,16): top (20,16), right (16,20), front (20,20), left (28,20), back (32,20), the sides 4 wide and 12 tall, front and back 8 wide; the arm box at (40,16):
 * top (44,16), right (40,20), front (44,20), left (48,20), back (52,20), 4 wide and 12 tall. The left arm mirrors the right one. A pixel with alpha 0 is
 * left out, which is how a circlet leaves the face alone.
 */
public final class ArmorPainter {
    private ArmorPainter() {}

    public static final int W = 64, H = 32;

    /** One painted texture: 64 x 32 ARGB pixels, row by row. */
    public static int[] paint(Look look, RelicSlot slot, int main, int trim, int glow, long seed) {
        Canvas c = new Canvas(seed);
        int m = 0xFF000000 | (main & 0xFFFFFF), t = 0xFF000000 | (trim & 0xFFFFFF), g = 0xFF000000 | (glow & 0xFFFFFF);
        if (slot == RelicSlot.HEAD) head(c, look, m, t, g);
        else body(c, look, m, t, g);
        return c.px;
    }

    // ================================================================ canvas
    /** The pixels and the few drawing moves the painter needs. */
    static final class Canvas {
        final int[] px = new int[W * H];
        final long seed;

        Canvas(long seed) { this.seed = seed; }

        void set(int x, int y, int argb) { if (x >= 0 && y >= 0 && x < W && y < H) px[y * W + x] = argb; }

        int get(int x, int y) { return x >= 0 && y >= 0 && x < W && y < H ? px[y * W + x] : 0; }

        void rect(int x, int y, int w, int h, int argb) { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) set(x + i, y + j, argb); }

        void clear(int x, int y, int w, int h) { rect(x, y, w, h, 0); }

        /** Cloth or metal: the colour shaded by height (lighter on top), with a little grain. */
        void material(int x, int y, int w, int h, int base, double top, double bottom, int grain) {
            for (int j = 0; j < h; j++) {
                double f = top + (bottom - top) * (h <= 1 ? 0 : j / (double) (h - 1));
                for (int i = 0; i < w; i++) set(x + i, y + j, shade(base, f + noise(x + i, y + j) * grain / 255.0));
            }
        }

        /** A border one pixel wide inside the rectangle. */
        void frame(int x, int y, int w, int h, int argb) {
            for (int i = 0; i < w; i++) { set(x + i, y, argb); set(x + i, y + h - 1, argb); }
            for (int j = 0; j < h; j++) { set(x, y + j, argb); set(x + w - 1, y + j, argb); }
        }

        void hline(int x, int y, int w, int argb) { for (int i = 0; i < w; i++) set(x + i, y, argb); }

        void vline(int x, int y, int h, int argb) { for (int j = 0; j < h; j++) set(x, y + j, argb); }

        /** A value in -1..1, the same for the same place and seed. */
        double noise(int x, int y) {
            long h = x * 73856093L ^ y * 19349663L ^ seed * 83492791L;
            h ^= h >>> 13;
            h *= 0x5bd1e9955bd1e995L;
            h ^= h >>> 15;
            return ((h & 0xFFFF) / 32767.5) - 1.0;
        }
    }

    /** The colour times a factor (above 1 lightens towards white a little, below 1 darkens); alpha is kept. */
    static int shade(int argb, double f) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        if (f >= 1.0) {
            double k = Math.min(1.0, f - 1.0);
            r += (int) ((255 - r) * k);
            g += (int) ((255 - g) * k);
            b += (int) ((255 - b) * k);
        } else {
            double k = Math.max(0.0, f);
            r = (int) (r * k);
            g = (int) (g * k);
            b = (int) (b * k);
        }
        return a << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    // ================================================================ the body (CHEST)
    private static final int[][] TORSO_FACES = {{20, 16, 8, 4}, {28, 16, 8, 4}, {16, 20, 4, 12}, {20, 20, 8, 12}, {28, 20, 4, 12}, {32, 20, 8, 12}};
    private static final int[][] ARM_FACES = {{44, 16, 4, 4}, {48, 16, 4, 4}, {40, 20, 4, 12}, {44, 20, 4, 12}, {48, 20, 4, 12}, {52, 20, 4, 12}};

    private static void body(Canvas c, Look look, int m, int t, int g) {
        switch (look == null ? Look.PLATE : look) {
            case ROBE -> robe(c, m, t, g);
            case LEATHER -> leather(c, m, t, g);
            case COAT, SUIT -> coat(c, look == Look.SUIT, m, t, g);
            case CLOAK, CAPE, MANTLE -> mantle(c, look, m, t, g);
            default -> plate(c, m, t, g);
        }
    }

    private static void plate(Canvas c, int m, int t, int g) {
        for (int[] f : TORSO_FACES) c.material(f[0], f[1], f[2], f[3], m, 1.12, 0.78, 14);
        for (int[] f : ARM_FACES) c.material(f[0], f[1], f[2], f[3], m, 1.10, 0.80, 14);
        // chest plate: a raised breast with a seam down the middle, a belly band and a trimmed rim
        c.vline(23, 20, 8, shade(m, 0.55));
        c.vline(24, 20, 8, shade(m, 0.55));
        c.hline(20, 27, 8, shade(m, 0.55));
        c.hline(20, 28, 8, t);
        c.hline(20, 29, 8, shade(t, 0.7));
        c.frame(20, 20, 8, 12, shade(t, 0.9));
        c.rect(23, 22, 2, 2, g);                                                       // the gem on the breast
        c.set(22, 22, shade(g, 0.6)); c.set(25, 23, shade(g, 0.6));
        // shoulders and cuffs
        for (int[] a : new int[][]{{44, 20}, {48, 20}, {40, 20}, {52, 20}}) {
            c.hline(a[0], a[1], 4, t);
            c.hline(a[0], a[1] + 1, 4, shade(t, 0.7));
            c.hline(a[0], a[1] + 9, 4, shade(m, 0.6));
            c.hline(a[0], a[1] + 10, 4, t);
            c.hline(a[0], a[1] + 11, 4, shade(t, 0.7));
        }
        c.rect(44, 16, 4, 4, t);
        c.rect(45, 17, 2, 2, shade(t, 1.15));
        rivets(c, shade(t, 1.2));
        c.rect(32, 20, 8, 12, shade(m, 0.85));                                         // the back: plain plate with the spine seam
        c.material(32, 20, 8, 12, m, 1.0, 0.72, 12);
        c.vline(35, 21, 10, shade(m, 0.5));
        c.vline(36, 21, 10, shade(m, 0.5));
        c.hline(32, 31, 8, t);
    }

    private static void rivets(Canvas c, int rivet) {
        for (int[] p : new int[][]{{20, 20}, {27, 20}, {20, 26}, {27, 26}, {44, 21}, {47, 21}, {40, 21}, {43, 21}, {48, 21}, {51, 21}}) c.set(p[0], p[1], rivet);
    }

    private static void robe(Canvas c, int m, int t, int g) {
        for (int[] f : TORSO_FACES) cloth(c, f, m);
        for (int[] f : ARM_FACES) cloth(c, f, m);
        // a sash and a hem, trimmed collar, cuffs, a glowing clasp
        c.rect(20, 25, 8, 2, shade(t, 0.9));
        c.hline(20, 27, 8, shade(t, 0.6));
        c.hline(20, 30, 8, t);
        c.hline(20, 31, 8, shade(t, 0.7));
        c.vline(23, 20, 5, t);
        c.vline(24, 20, 5, t);
        c.rect(23, 25, 2, 2, g);
        for (int[] a : new int[][]{{44, 20}, {48, 20}, {40, 20}, {52, 20}}) {
            c.hline(a[0], a[1] + 9, 4, shade(t, 0.8));
            c.hline(a[0], a[1] + 10, 4, t);
            c.hline(a[0], a[1] + 11, 4, shade(t, 0.65));
        }
        c.hline(20, 20, 8, t);
        c.hline(28, 20, 4, t);
        c.hline(32, 20, 8, t);
        c.hline(16, 20, 4, t);
        cloth(c, new int[]{32, 20, 8, 12}, shade(m, 0.9));
        c.hline(32, 30, 8, t);
        c.hline(32, 31, 8, shade(t, 0.7));
        c.rect(44, 16, 4, 4, shade(m, 1.1));
    }

    /** Cloth: lighter on top, soft vertical folds. */
    private static void cloth(Canvas c, int[] f, int base) {
        c.material(f[0], f[1], f[2], f[3], base, 1.08, 0.82, 10);
        for (int i = 1; i < f[2]; i += 3) for (int j = 0; j < f[3]; j++) c.set(f[0] + i, f[1] + j, shade(c.get(f[0] + i, f[1] + j), 0.88));
    }

    private static void leather(Canvas c, int m, int t, int g) {
        for (int[] f : TORSO_FACES) c.material(f[0], f[1], f[2], f[3], m, 1.06, 0.74, 20);
        for (int[] f : ARM_FACES) c.material(f[0], f[1], f[2], f[3], m, 1.04, 0.76, 20);
        // two straps crossing the chest with a buckle, stitched seams, bracers
        for (int i = 0; i < 12; i++) {
            int x = 20 + i * 8 / 12;
            c.set(x, 20 + i, shade(t, 0.9));
            c.set(x + 1, 20 + i, shade(t, 1.0));
            c.set(27 - i * 7 / 12, 20 + i, shade(t, 0.8));
        }
        c.rect(23, 25, 3, 2, g);
        c.frame(23, 25, 3, 2, shade(t, 1.1));
        for (int y = 20; y < 32; y += 2) { c.set(20, y, shade(m, 0.55)); c.set(27, y, shade(m, 0.55)); }
        for (int[] a : new int[][]{{44, 20}, {48, 20}, {40, 20}, {52, 20}}) {
            c.rect(a[0], a[1] + 7, 4, 4, shade(t, 0.8));
            c.hline(a[0], a[1] + 7, 4, t);
            c.hline(a[0], a[1] + 10, 4, shade(t, 0.6));
            c.set(a[0] + 1, a[1] + 8, shade(t, 1.25));
        }
        c.rect(44, 16, 4, 4, shade(t, 0.85));
        c.hline(32, 29, 8, t);
        c.hline(32, 30, 8, shade(t, 0.6));
    }

    private static void coat(Canvas c, boolean suit, int m, int t, int g) {
        for (int[] f : TORSO_FACES) c.material(f[0], f[1], f[2], f[3], m, suit ? 1.04 : 1.08, suit ? 0.9 : 0.8, suit ? 5 : 11);
        for (int[] f : ARM_FACES) c.material(f[0], f[1], f[2], f[3], m, 1.06, 0.84, suit ? 5 : 11);
        // lapels in a V, buttons down the middle, cuffs; the suit adds pinstripes
        for (int i = 0; i < 6; i++) { c.set(20 + i, 20 + i, t); c.set(27 - i, 20 + i, t); c.set(21 + i, 20 + i, shade(t, 0.75)); c.set(26 - i, 20 + i, shade(t, 0.75)); }
        for (int y = 26; y < 32; y += 2) { c.set(23, y, g); c.set(24, y, shade(g, 0.7)); }
        c.vline(23, 20, 6, shade(m, 0.55));
        c.vline(24, 20, 6, shade(m, 0.55));
        if (suit) for (int x = 20; x < 28; x += 2) for (int y = 26; y < 32; y++) c.set(x, y, shade(c.get(x, y), 1.1));
        for (int[] a : new int[][]{{44, 20}, {48, 20}, {40, 20}, {52, 20}}) {
            c.hline(a[0], a[1] + 9, 4, t);
            c.hline(a[0], a[1] + 10, 4, shade(t, 0.8));
            c.hline(a[0], a[1] + 11, 4, shade(t, 0.6));
        }
        c.hline(20, 31, 8, t);
        c.hline(32, 31, 8, t);
        c.hline(28, 31, 4, t);
        c.hline(16, 31, 4, t);
        c.vline(35, 21, 10, shade(m, 0.6));
        c.rect(44, 16, 4, 4, shade(m, 1.12));
    }

    /** A cloak, cape or mantle: broad shoulders and a long back, the front mostly left bare. */
    private static void mantle(Canvas c, Look look, int m, int t, int g) {
        boolean mantle = look == Look.MANTLE;
        // the back and the sides of the torso carry the cape; the front shows only the collar and a chain-clasp
        c.material(32, 20, 8, 12, m, 1.04, 0.7, 14);
        for (int i = 1; i < 8; i += 2) c.vline(32 + i, 20, 12, shade(m, 0.8));
        c.hline(32, 20, 8, t);
        c.hline(32, 21, 8, shade(t, 0.7));
        c.hline(32, 31, 8, t);
        c.material(16, 20, 4, 12, m, 1.0, 0.72, 12);
        c.material(28, 20, 4, 12, m, 1.0, 0.72, 12);
        c.material(20, 16, 8, 4, m, 1.1, 1.0, 10);
        c.material(28, 16, 8, 4, m, 0.9, 0.8, 8);
        c.hline(20, 20, 8, t);
        c.hline(20, 21, 8, shade(t, 0.7));
        c.rect(23, 21, 2, 2, g);
        c.set(22, 22, shade(t, 0.9));
        c.set(25, 22, shade(t, 0.9));
        // shoulders: the top and outer faces of both arms for a mantle, a collar-width strip for a cloak
        for (int[] f : ARM_FACES) {
            if (f[1] == 16 || f[3] == 12) {
                int h = mantle ? 7 : 3;
                if (f[1] == 16) c.material(f[0], f[1], f[2], f[3], m, 1.12, 1.0, 12);
                else { c.material(f[0], f[1], f[2], h, m, 1.06, 0.84, 12); c.hline(f[0], f[1] + h - 1, f[2], t); }
            }
        }
        c.rect(44, 16, 4, 4, shade(t, 0.9));
        c.rect(45, 17, 2, 2, shade(g, 0.9));
    }

    // ================================================================ the head (HEAD)
    private static final int[][] HEAD_FACES = {{8, 0, 8, 8}, {16, 0, 8, 8}, {0, 8, 8, 8}, {8, 8, 8, 8}, {16, 8, 8, 8}, {24, 8, 8, 8}};

    private static void head(Canvas c, Look look, int m, int t, int g) {
        switch (look == null ? Look.CIRCLET : look) {
            case HOOD -> hood(c, m, t, g);
            case CROWN -> crown(c, m, t, g);
            case HAT -> hat(c, m, t, g);
            case PLATE -> helm(c, m, t, g);
            default -> circlet(c, m, t, g);
        }
    }

    private static void circlet(Canvas c, int m, int t, int g) {
        for (int x : new int[]{0, 8, 16, 24}) {                                       // a thin band across the brow, round all four sides
            c.hline(x, 11, 8, shade(t, 1.05));
            c.hline(x, 12, 8, shade(t, 0.8));
        }
        c.rect(11, 10, 2, 3, g);                                                       // the gem on the brow
        c.set(11, 11, shade(g, 1.3));
        c.hline(8, 10, 3, shade(t, 0.9));
        c.hline(13, 10, 3, shade(t, 0.9));
        c.hline(28, 10, 1, shade(t, 0.9));
        c.set(26, 12, m);
    }

    private static void crown(Canvas c, int m, int t, int g) {
        c.material(8, 0, 8, 8, t, 1.1, 0.9, 10);                                       // the top: a gilded cap
        c.rect(11, 3, 2, 2, g);
        for (int x : new int[]{0, 8, 16, 24}) {
            c.material(x, 8, 8, 4, t, 1.12, 0.8, 12);                                  // the band, four pixels deep
            c.hline(x, 11, 8, shade(t, 0.6));
            for (int i = 0; i < 8; i += 2) { c.set(x + i, 8, shade(t, 1.3)); c.set(x + i, 9, shade(t, 1.1)); }   // the points
            c.set(x + 3, 9, g);
            c.set(x + 4, 9, shade(g, 0.8));
        }
        c.rect(11, 9, 2, 2, g);
        c.set(11, 9, shade(g, 1.3));
        c.rect(9, 10, 1, 1, m);
        c.rect(14, 10, 1, 1, m);
    }

    private static void hat(Canvas c, int m, int t, int g) {
        c.material(8, 0, 8, 8, m, 1.1, 0.9, 10);
        c.rect(9, 1, 6, 6, shade(m, 1.18));
        c.frame(8, 0, 8, 8, shade(m, 0.7));
        for (int x : new int[]{0, 8, 16, 24}) {
            c.material(x, 8, 8, 5, m, 1.05, 0.75, 10);                                // the crown of the hat, five deep
            c.hline(x, 12, 8, shade(t, 0.9));
            c.hline(x, 11, 8, t);
            c.hline(x, 10, 8, shade(t, 0.7));
        }
        c.rect(11, 10, 2, 3, g);
    }

    private static void hood(Canvas c, int m, int t, int g) {
        for (int[] f : HEAD_FACES) cloth(c, f, m);
        c.rect(8, 8, 8, 1, shade(m, 1.12));
        // the opening for the face, framed by a darker lining and a trimmed edge
        c.clear(9, 10, 6, 6);
        c.frame(9, 10, 6, 6, 0);
        c.hline(8, 9, 8, shade(t, 0.9));
        c.vline(8, 9, 7, shade(t, 0.75));
        c.vline(15, 9, 7, shade(t, 0.75));
        c.hline(9, 10, 6, shade(m, 0.5));
        c.vline(9, 11, 5, shade(m, 0.55));
        c.vline(14, 11, 5, shade(m, 0.55));
        c.set(11, 9, g);
        c.set(12, 9, shade(g, 0.8));
        c.hline(24, 15, 8, t);
        c.hline(0, 15, 8, t);
        c.hline(16, 15, 8, t);
    }

    private static void helm(Canvas c, int m, int t, int g) {
        for (int[] f : HEAD_FACES) c.material(f[0], f[1], f[2], f[3], m, 1.14, 0.78, 14);
        c.hline(8, 8, 8, t);
        c.hline(8, 9, 8, shade(t, 0.7));
        c.clear(10, 11, 4, 2);                                                         // the eye slit
        c.hline(9, 11, 6, shade(m, 0.5));
        c.hline(9, 12, 6, shade(m, 0.5));
        c.clear(10, 11, 4, 2);
        c.rect(11, 11, 2, 1, g);
        c.vline(11, 13, 3, shade(m, 0.55));
        c.vline(12, 13, 3, shade(m, 0.55));
        c.hline(0, 15, 8, t);
        c.hline(16, 15, 8, t);
        c.hline(24, 15, 8, t);
        c.vline(11, 0, 8, t);
        c.vline(12, 0, 8, shade(t, 0.7));
    }
}
