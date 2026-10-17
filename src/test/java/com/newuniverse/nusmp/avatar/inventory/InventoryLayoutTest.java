package com.newuniverse.nusmp.avatar.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The screen layout of the Avatar's inventory: the indices are a stable save format, every slot fits the frame and no two cells overlap. */
class InventoryLayoutTest {
    @Test
    void theIndicesAreTheSaveFormat() {
        assertEquals(0, InventoryLayout.MAIN);
        assertEquals(1, InventoryLayout.OFF);
        assertEquals(2, InventoryLayout.HEAD);
        assertEquals(5, InventoryLayout.FEET);
        assertEquals(6, InventoryLayout.TRINKET_START);
        assertEquals(8, InventoryLayout.TRINKETS, "as many as the relic pack of a named mob can hold");
        assertEquals(14, InventoryLayout.POCKET_START);
        assertEquals(18, InventoryLayout.POCKETS);
        assertEquals(32, InventoryLayout.SIZE);
    }

    @Test
    void everyIndexBelongsToExactlyOneGroup() {
        for (int i = 0; i < InventoryLayout.SIZE; i++) {
            int groups = (InventoryLayout.isEquipment(i) ? 1 : 0) + (InventoryLayout.isTrinket(i) ? 1 : 0) + (InventoryLayout.isPocket(i) ? 1 : 0);
            assertEquals(1, groups, "slot " + i);
        }
        assertFalse(InventoryLayout.isEquipment(-1));
        assertFalse(InventoryLayout.isPocket(InventoryLayout.SIZE));
        assertTrue(InventoryLayout.isArmor(InventoryLayout.CHEST));
        assertFalse(InventoryLayout.isArmor(InventoryLayout.MAIN));
    }

    @Test
    void everySlotFitsTheFrame() {
        for (int i = 0; i < InventoryLayout.SIZE; i++) {
            int[] p = InventoryLayout.position(i);
            assertTrue(p[0] >= 0 && p[0] + InventoryLayout.SLOT <= InventoryLayout.WIDTH, "x of slot " + i);
            assertTrue(p[1] >= 0 && p[1] + InventoryLayout.SLOT <= InventoryLayout.HEIGHT, "y of slot " + i);
        }
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 9; col++) {
                int[] p = InventoryLayout.playerCell(row, col);
                assertTrue(p[0] + InventoryLayout.SLOT <= InventoryLayout.WIDTH && p[1] + InventoryLayout.SLOT <= InventoryLayout.HEIGHT, "player cell " + row + "," + col);
            }
    }

    private static boolean overlap(int[] a, int[] b) {
        return a[0] < b[0] + InventoryLayout.SLOT && b[0] < a[0] + InventoryLayout.SLOT && a[1] < b[1] + InventoryLayout.SLOT && b[1] < a[1] + InventoryLayout.SLOT;
    }

    @Test
    void noTwoCellsOverlapAndNoneCoversThePortraitOrTheButtons() {
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for (int i = 0; i < InventoryLayout.SIZE; i++) cells.add(InventoryLayout.position(i));
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) cells.add(InventoryLayout.playerCell(row, col));
        for (int i = 0; i < cells.size(); i++)
            for (int j = i + 1; j < cells.size(); j++) assertFalse(overlap(cells.get(i), cells.get(j)), "cells " + i + " and " + j);
        int[] preview = {InventoryLayout.PREVIEW_X, InventoryLayout.PREVIEW_Y, InventoryLayout.PREVIEW_W, InventoryLayout.PREVIEW_H};
        for (int[] c : cells) {
            boolean hitsPreview = c[0] < preview[0] + preview[2] && preview[0] < c[0] + InventoryLayout.SLOT && c[1] < preview[1] + preview[3] && preview[1] < c[1] + InventoryLayout.SLOT;
            assertFalse(hitsPreview, "a slot covers the portrait at " + c[0] + "," + c[1]);
            for (int b = 0; b < InventoryLayout.BUTTONS; b++) {
                int bx = InventoryLayout.buttonX(b);
                boolean hitsButton = c[0] < bx + InventoryLayout.BUTTON_W && bx < c[0] + InventoryLayout.SLOT
                        && c[1] < InventoryLayout.BUTTON_Y + InventoryLayout.BUTTON_H && InventoryLayout.BUTTON_Y < c[1] + InventoryLayout.SLOT;
                assertFalse(hitsButton, "a slot covers button " + b);
            }
        }
        assertTrue(InventoryLayout.buttonX(InventoryLayout.BUTTONS - 1) + InventoryLayout.BUTTON_W <= InventoryLayout.WIDTH);
    }

    @Test
    void outOfRangeIndicesGetTheOrigin() {
        assertEquals(0, InventoryLayout.position(-1)[0]);
        assertEquals(0, InventoryLayout.position(InventoryLayout.SIZE)[1]);
    }
}
