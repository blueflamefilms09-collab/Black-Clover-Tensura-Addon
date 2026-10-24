package com.newuniverse.nusmp.client.companion.gs;

/**
 * How the pure painter names an item to draw: a small integer the screen turns into an {@code ItemStack} (the pouch's stack, one of the player's inventory slots, the gift slot, a notebook entry). The
 * painter never holds an item itself, so it stays free of Minecraft types. Pure.
 */
public final class GsItemRef {
    private GsItemRef() {}

    /** Pouch slots 0..8. */
    public static final int POUCH = 0;
    /** The player's inventory index 0..35 (0..8 is the hotbar). */
    public static final int INVENTORY = 20;
    /** The item in the gift slot (the selected inventory index). */
    public static final int GIFT = 60;
    /** Notebook entries 0..7. */
    public static final int TASTE = 80;

    public static int pouch(int slot) { return POUCH + slot; }

    public static int inventory(int index) { return INVENTORY + index; }

    public static int taste(int i) { return TASTE + i; }

    public static boolean isPouch(int ref) { return ref >= POUCH && ref < POUCH + 9; }

    public static boolean isInventory(int ref) { return ref >= INVENTORY && ref < INVENTORY + 36; }

    public static boolean isTaste(int ref) { return ref >= TASTE && ref < TASTE + 8; }

    public static int inventoryIndex(int ref) { return ref - INVENTORY; }

    public static int tasteIndex(int ref) { return ref - TASTE; }
}
