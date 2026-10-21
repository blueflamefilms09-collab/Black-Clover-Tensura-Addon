package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicSlot;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 1.110.0 armour layer: same layout and coverage as the older painter, cleaner, with an aligned glow channel; plus the generator of the before / after previews. */
class ArmorPainterV2Test {
    private static final int[][] PALETTES = {{0x6A2A7A, 0xD9A441, 0xFF4FD8}, {0x101010, 0x303030, 0x40FFFF}, {0xF0F0F0, 0xC0C0C0, 0xFFFF80}, {0x2040A0, 0x90B0E0, 0x60FFA0}, {0x802020, 0x402020, 0xFF8020}};

    private record Case(Look look, RelicSlot slot) {}

    private static List<Case> cases() {
        List<Case> out = new ArrayList<>();
        for (Look l : new Look[]{Look.CROWN, Look.CIRCLET, Look.HAT, Look.HOOD, Look.PLATE}) out.add(new Case(l, RelicSlot.HEAD));
        for (Look l : new Look[]{Look.PLATE, Look.ROBE, Look.LEATHER, Look.COAT, Look.SUIT, Look.CLOAK, Look.CAPE, Look.MANTLE}) out.add(new Case(l, RelicSlot.CHEST));
        for (Look l : new Look[]{Look.BOOTS, Look.SANDALS, Look.GREAVES}) out.add(new Case(l, RelicSlot.FEET));
        return out;
    }

    private static int opaque(int[] px) {
        int n = 0;
        for (int p : px) if (p >>> 24 != 0) n++;
        return n;
    }

    private static boolean anyOpaque(int[] px, int x, int y, int w, int h) {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) if (px[(y + j) * ArmorPainter.W + x + i] >>> 24 != 0) return true;
        return false;
    }

    @Test
    void everyLookAndPaletteIsA64By32ImageThatCoversExactlyWhatTheOlderPainterCovers() {
        for (Case c : cases()) for (int[] pal : PALETTES) {
            int[] old = ArmorPainter.paint(c.look(), c.slot(), pal[0], pal[1], pal[2], 7L);
            int[] now = ArmorPainterV2.paint(c.look(), c.slot(), pal[0], pal[1], pal[2], 7L);
            assertEquals(ArmorPainter.W * ArmorPainter.H, now.length);
            assertTrue(opaque(now) > 0, c + " is fully transparent");
            for (int i = 0; i < old.length; i++) assertEquals(old[i] >>> 24 != 0, now[i] >>> 24 != 0, c + " covers a different pixel " + i);
            for (int p : now) assertTrue(p >>> 24 == 0 || p >>> 24 == 255, "alpha is 0 or 255");
        }
    }

    @Test
    void noPieceOfASlotIsFullyTransparent() {
        for (Case c : cases()) for (int[] pal : PALETTES) {
            int[] now = ArmorPainterV2.paint(c.look(), c.slot(), pal[0], pal[1], pal[2], 3L);
            if (c.slot() == RelicSlot.CHEST) {
                assertTrue(anyOpaque(now, 16, 16, 24, 16), c + " has no torso");
                assertTrue(anyOpaque(now, 40, 16, 16, 16), c + " has no arm");
            } else if (c.slot() == RelicSlot.HEAD) {
                assertTrue(anyOpaque(now, 0, 0, 32, 16), c + " has no head");
            } else {
                assertTrue(anyOpaque(now, 0, 16, 16, 16), c + " has no boots");
            }
        }
    }

    @Test
    void theGlowChannelLiesOnPaintedPixelsAndIsNeverEmpty() {
        for (Case c : cases()) for (int[] pal : PALETTES) {
            int[] px = ArmorPainterV2.paint(c.look(), c.slot(), pal[0], pal[1], pal[2], 11L);
            int[] glow = ArmorPainterV2.paintGlow(c.look(), c.slot(), pal[0], pal[1], pal[2], 11L);
            assertEquals(px.length, glow.length);
            int n = 0;
            for (int i = 0; i < px.length; i++) {
                if (glow[i] >>> 24 == 0) continue;
                n++;
                assertTrue(px[i] >>> 24 != 0, c + ": a glow pixel outside the layer at " + i);
            }
            assertTrue(n > 0, c + ": no glow at all");
        }
    }

    @Test
    void aChestPieceCarriesAnEmblemInTheGlowChannel() {
        for (Look l : new Look[]{Look.PLATE, Look.ROBE, Look.LEATHER, Look.COAT, Look.SUIT}) {
            int[] glow = ArmorPainterV2.paintGlow(l, RelicSlot.CHEST, 0x6A2A7A, 0xD9A441, 0xFF4FD8, 5L);
            int n = 0;
            for (int j = 22; j < 26; j++) for (int i = 22; i < 26; i++) if (glow[j * ArmorPainter.W + i] >>> 24 != 0) n++;
            assertTrue(n >= 6, l + ": the breast emblem has only " + n + " glow pixels");
        }
    }

    @Test
    void theSleevesAndTheTorsoShareOneTrimLine() {
        int[] px = ArmorPainterV2.paint(Look.PLATE, RelicSlot.CHEST, 0x6A2A7A, 0xD9A441, 0xFF4FD8, 5L);
        int yoke = px[20 * ArmorPainter.W + 21];
        assertEquals(yoke & 0xFFFFFF, px[20 * ArmorPainter.W + 45] & 0xFFFFFF, "yoke row: torso and sleeve differ");
        assertEquals(px[30 * ArmorPainter.W + 21] & 0xFFFFFF, px[30 * ArmorPainter.W + 45] & 0xFFFFFF, "hem row: torso and sleeve differ");
    }

    @Test
    void itIsDeterministicAndDiffersFromTheOlderPainting() {
        for (Case c : cases()) {
            int[] a = ArmorPainterV2.paint(c.look(), c.slot(), 0x6A2A7A, 0xD9A441, 0xFF4FD8, 9L), b = ArmorPainterV2.paint(c.look(), c.slot(), 0x6A2A7A, 0xD9A441, 0xFF4FD8, 9L);
            assertTrue(java.util.Arrays.equals(a, b), c + " is not deterministic");
            assertFalse(java.util.Arrays.equals(a, ArmorPainter.paint(c.look(), c.slot(), 0x6A2A7A, 0xD9A441, 0xFF4FD8, 9L)), c + " is the same as the older layer");
        }
    }

    @Test
    void aVeryDarkPaletteIsLiftedSoTheShapeStillReads() {
        int[] px = ArmorPainterV2.paint(Look.PLATE, RelicSlot.CHEST, 0x000000, 0x101010, 0x40FFFF, 1L);
        int bright = 0;
        for (int p : px) if (p >>> 24 != 0 && ((p >> 16 & 255) + (p >> 8 & 255) + (p & 255)) > 90) bright++;
        assertTrue(bright > 40, "a black relic is still shaded, bright pixels: " + bright);
        assertNotEquals(0x000000, ArmorPainterV2.lift(0x000000));
    }

    /** Writes docs/previews/relic_armour/ when the environment variable NUSMP_PREVIEW is set (the unwrapped layers, before and after, 8 times larger, on a checkerboard). */
    @Test
    void writesThePreviewsWhenAsked() throws IOException {
        if (System.getenv("NUSMP_PREVIEW") == null) return;
        File dir = new File("docs/previews/relic_armour");
        dir.mkdirs();
        int scale = 8, cols = 3;
        List<Case> pick = new ArrayList<>();
        pick.add(new Case(Look.CROWN, RelicSlot.HEAD));
        pick.add(new Case(Look.PLATE, RelicSlot.HEAD));
        pick.add(new Case(Look.PLATE, RelicSlot.CHEST));
        pick.add(new Case(Look.ROBE, RelicSlot.CHEST));
        pick.add(new Case(Look.MANTLE, RelicSlot.CHEST));
        pick.add(new Case(Look.GREAVES, RelicSlot.FEET));
        int w = ArmorPainter.W * scale, h = ArmorPainter.H * scale;
        BufferedImage sheet = new BufferedImage(w * 2 + 30, (h + 10) * pick.size() + 10, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = sheet.createGraphics();
        g.setColor(new java.awt.Color(0x20202A));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int row = 0;
        int[] pal = PALETTES[0];
        for (Case c : pick) {
            for (int v = 0; v < 2; v++) {
                int[] px = v == 0 ? ArmorPainter.paint(c.look(), c.slot(), pal[0], pal[1], pal[2], 5L) : ArmorPainterV2.paint(c.look(), c.slot(), pal[0], pal[1], pal[2], 5L);
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int back = ((x / 16 + y / 16) & 1) == 0 ? 0xFF3A3A44 : 0xFF2C2C34;
                    int p = px[(y / scale) * ArmorPainter.W + x / scale];
                    img.setRGB(x, y, p >>> 24 == 0 ? back : p);
                }
                g.drawImage(img, 10 + v * (w + 10), 10 + row * (h + 10), null);
                ImageIOHolder.write(img, new File(dir, (v == 0 ? "before_" : "after_") + c.slot().name().toLowerCase() + "_" + c.look().name().toLowerCase() + ".png"));
            }
            row++;
        }
        g.dispose();
        ImageIOHolder.write(sheet, new File(dir, "before_after_sheet.png"));
        assertTrue(new File(dir, "before_after_sheet.png").exists());
    }

    private static final class ImageIOHolder {
        static void write(BufferedImage img, File f) throws IOException { javax.imageio.ImageIO.write(img, "png", f); }
    }
}
