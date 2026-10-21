package com.newuniverse.nusmp.labyrinth.loot.backpack;

/**
 * Why: the screen and the menu must agree, to the pixel, on where every slot is, and the layout is arithmetic: the vanilla chest grid (a 176-wide panel, slots on an 18-pixel
 * pitch, the container rows from y = 18, the player's inventory under them) for any number of rows, so the same numbers serve a three-row leather backpack and a seven-row
 * netherite one. Keeping it pure lets a test pin that no two slots overlap, that every slot is inside the panel, and that the player's inventory is where the player expects it
 * (the rows of the vanilla chest, so a mouse that knows a chest knows this). Pure.
 */
public final class BackpackLayout {
    private BackpackLayout() {}

    public static final int WIDTH = 176, SLOT = 18, LEFT = 8, TOP = 18;

    /** The panel's height for this many container rows. */
    public static int height(int rows) { return 114 + rows * SLOT; }

    /** The x of the container slot with this index. */
    public static int slotX(int index) { return LEFT + (index % BackpackTier.COLUMNS) * SLOT; }

    /** The y of the container slot with this index. */
    public static int slotY(int index) { return TOP + (index / BackpackTier.COLUMNS) * SLOT; }

    /** The y of the first row of the player's inventory (the three rows above the hotbar). */
    public static int inventoryY(int rows) { return TOP + rows * SLOT + 13; }

    /** The y of the hotbar. */
    public static int hotbarY(int rows) { return inventoryY(rows) + 3 * SLOT + 4; }

    /** The y of the "Inventory" label. */
    public static int inventoryLabelY(int rows) { return inventoryY(rows) - 11; }
}
