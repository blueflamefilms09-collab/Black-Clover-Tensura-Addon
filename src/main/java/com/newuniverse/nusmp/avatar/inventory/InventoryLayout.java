package com.newuniverse.nusmp.avatar.inventory;

/**
 * Why: the Avatar's inventory is one container of 32 slots with a fixed meaning for each index (main hand, off hand, four armour pieces, eight trinket slots,
 * eighteen pockets), shared by the server menu, the client menu, the screen and the save format. One pure class owns those indices and the pixel position of
 * every slot in the 176 x 236 screen, so the layout can be checked by a test (no two slots overlap, everything fits) and the screen and the menu cannot drift.
 */
public final class InventoryLayout {
    private InventoryLayout() {}

    public static final int MAIN = 0, OFF = 1, HEAD = 2, CHEST = 3, LEGS = 4, FEET = 5;
    /** The six equipment indices come first. */
    public static final int EQUIPMENT = 6;
    /**
     * The trinket slots (the Curios-like slots: rings, charms, belts, ... ) follow, then the pockets. Eight, because the relic system lets a named mob carry at most
     * eight relics in all ({@code relics_mobs.maxCarried} goes up to 8) and the trinkets ARE that carried pack, so the view must never be smaller than the pack.
     */
    public static final int TRINKET_START = EQUIPMENT, TRINKETS = 8;
    public static final int POCKET_START = TRINKET_START + TRINKETS, POCKETS = 18;
    public static final int SIZE = POCKET_START + POCKETS;

    /** The screen. */
    public static final int WIDTH = 176, HEIGHT = 236;
    /** The rectangle of the Avatar's portrait (x, y, w, h) and the row of command buttons. */
    public static final int PREVIEW_X = 28, PREVIEW_Y = 16, PREVIEW_W = 54, PREVIEW_H = 70;
    public static final int BUTTON_Y = 90, BUTTON_H = 14, BUTTON_W = 37, BUTTON_X0 = 8, BUTTON_STEP = 41, BUTTONS = 4;
    /** The player's own inventory below: three rows and the hotbar. */
    public static final int PLAYER_Y = 154, HOTBAR_Y = 212;
    /** A slot is 16 x 16 inside an 18 x 18 cell. */
    public static final int SLOT = 18;

    public static boolean isEquipment(int index) { return index >= 0 && index < EQUIPMENT; }

    public static boolean isTrinket(int index) { return index >= TRINKET_START && index < TRINKET_START + TRINKETS; }

    public static boolean isPocket(int index) { return index >= POCKET_START && index < POCKET_START + POCKETS; }

    public static boolean isArmor(int index) { return index >= HEAD && index <= FEET; }

    /** The top-left pixel of slot {@code index} (inside the 176 x 236 frame), as {x, y}; {0, 0} for an index out of range. */
    public static int[] position(int index) {
        if (index < 0 || index >= SIZE) return new int[]{0, 0};
        if (index == HEAD) return new int[]{8, 16};
        if (index == CHEST) return new int[]{8, 34};
        if (index == LEGS) return new int[]{8, 52};
        if (index == FEET) return new int[]{8, 70};
        if (index == MAIN) return new int[]{88, 16};
        if (index == OFF) return new int[]{106, 16};
        if (isTrinket(index)) {
            int k = index - TRINKET_START;
            return new int[]{88 + 18 * (k % 4), 40 + 18 * (k / 4)};
        }
        int k = index - POCKET_START;
        return new int[]{8 + 18 * (k % 9), 110 + 18 * (k / 9)};
    }

    /** The x of the command button {@code i} (0 .. BUTTONS-1). */
    public static int buttonX(int i) { return BUTTON_X0 + BUTTON_STEP * i; }

    /** The top-left of one cell of the player's inventory: {@code row} 0..2 is the main inventory, 3 the hotbar; {@code col} 0..8. */
    public static int[] playerCell(int row, int col) {
        return new int[]{8 + 18 * col, row >= 3 ? HOTBAR_Y : PLAYER_Y + 18 * row};
    }
}
