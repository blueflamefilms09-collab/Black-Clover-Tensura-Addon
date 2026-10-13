package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicSlot;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** The runtime armour layers: right size, deterministic, and leaving the face (or the arms) alone where the look says so. */
class ArmorPainterTest {
    static int[] chest(Look l) { return ArmorPainter.paint(l, RelicSlot.CHEST, 0x6A7A9A, 0xD8B040, 0x80E0FF, 7); }

    static int[] head(Look l) { return ArmorPainter.paint(l, RelicSlot.HEAD, 0x6A7A9A, 0xD8B040, 0x80E0FF, 7); }

    static int alpha(int[] px, int x, int y) { return px[y * ArmorPainter.W + x] >>> 24; }

    static int opaque(int[] px, int x, int y, int w, int h) {
        int n = 0;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) if (alpha(px, x + i, y + j) == 255) n++;
        return n;
    }

    @Test
    void everyLookPaintsA64By32ImageOfOpaqueOrClearPixels() {
        for (Look l : Look.values()) {
            for (RelicSlot s : new RelicSlot[]{RelicSlot.CHEST, RelicSlot.HEAD}) {
                int[] px = ArmorPainter.paint(l, s, 0x808080, 0xC0A040, 0xFFFFFF, 3);
                assertEquals(64 * 32, px.length, l + " " + s);
                for (int p : px) assertTrue((p >>> 24) == 0 || (p >>> 24) == 255, l + " " + s + " has a half-clear pixel");
                assertTrue(Arrays.stream(px).anyMatch(p -> (p >>> 24) == 255), l + " " + s + " paints something");
            }
        }
    }

    @Test
    void theSamePaletteAndSeedPaintTheSamePixels() {
        assertArrayEquals(chest(Look.ROBE), chest(Look.ROBE));
        assertFalse(Arrays.equals(chest(Look.ROBE), chest(Look.PLATE)));
        assertFalse(Arrays.equals(chest(Look.ROBE), ArmorPainter.paint(Look.ROBE, RelicSlot.CHEST, 0x2A3A4A, 0xD8B040, 0x80E0FF, 7)));
        assertFalse(Arrays.equals(chest(Look.ROBE), ArmorPainter.paint(Look.ROBE, RelicSlot.CHEST, 0x6A7A9A, 0xD8B040, 0x80E0FF, 8)));
    }

    @Test
    void aChestPieceTouchesOnlyTheBodyAndArmAreas() {
        for (Look l : new Look[]{Look.PLATE, Look.ROBE, Look.LEATHER, Look.COAT, Look.SUIT, Look.CLOAK, Look.CAPE, Look.MANTLE}) {
            int[] px = chest(l);
            assertEquals(0, opaque(px, 0, 0, 32, 16), l + " leaves the head area clear");
            assertEquals(0, opaque(px, 0, 16, 16, 16), l + " leaves the leg area clear");
        }
    }

    @Test
    void aHeadPieceTouchesOnlyTheHeadArea() {
        for (Look l : new Look[]{Look.CIRCLET, Look.CROWN, Look.HAT, Look.HOOD, Look.PLATE}) {
            int[] px = head(l);
            assertEquals(0, opaque(px, 0, 16, 64, 16), l + " leaves the lower half clear");
            assertEquals(0, opaque(px, 32, 0, 32, 16), l + " leaves the right of the head area clear");
        }
    }

    @Test
    void aCircletLeavesTheFaceAndAHoodLeavesAnOpening() {
        int[] circlet = head(Look.CIRCLET);
        assertTrue(opaque(circlet, 8, 8, 8, 8) < 32, "a circlet is a thin band");
        assertEquals(0, alpha(circlet, 11, 14), "the face of the circlet is clear");
        int[] hood = head(Look.HOOD);
        assertEquals(0, alpha(hood, 11, 13), "the hood's front opens on the face");
        assertEquals(255, alpha(hood, 12, 2), "the hood covers the crown");
        assertTrue(opaque(hood, 0, 0, 32, 16) > 150);
    }

    @Test
    void aCrownCoversTheTopAndTheBandButNotTheFace() {
        int[] crown = head(Look.CROWN);
        assertEquals(255, alpha(crown, 12, 4), "the cap of the crown");
        assertEquals(0, alpha(crown, 12, 14), "the lower front is clear");
    }

    @Test
    void shadeKeepsAlphaAndStaysInRange() {
        int c = 0xFF336699;
        for (double f : new double[]{0.0, 0.5, 1.0, 1.5, 3.0}) {
            int s = ArmorPainter.shade(c, f);
            assertEquals(255, s >>> 24);
            for (int sh = 0; sh <= 16; sh += 8) assertTrue((s >> sh & 255) >= 0 && (s >> sh & 255) <= 255);
        }
        assertEquals(0xFF000000, ArmorPainter.shade(c, 0.0));
        assertEquals(0xFFFFFFFF, ArmorPainter.shade(c, 2.0));
    }
}
