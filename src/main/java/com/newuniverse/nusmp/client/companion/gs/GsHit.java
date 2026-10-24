package com.newuniverse.nusmp.client.companion.gs;

/**
 * The numbers that name what is under the pointer on the Great Spirit talk screen. The layout answers "what is at this point" with one of these; the painter lights it, the screen acts on it. Pure.
 */
public final class GsHit {
    private GsHit() {}

    public static final int NONE = -1;
    /** Tabs 0..3. */
    public static final int TAB = 0;
    public static final int CLOSE = 10, DIALOGUE = 11, PORTRAIT = 12, METER = 13, MOOD = 14;
    public static final int GIFT_SLOT = 20, GIVE = 21, LIKES = 22, FIVE = 23, FOLLOW = 24, PANEL = 25;
    /** Stances 0..2. */
    public static final int STANCE = 30;
    public static final int USE = 40, PUT = 41, TAKE_ALL = 42, RING = 43;
    /** Pouch slots 0..8. */
    public static final int POUCH = 50;
    /** The hotbar strip of the Pouch tab, slots 0..8. */
    public static final int HOTBAR = 60;
    /** The picker's slots, one per inventory index 0..35. */
    public static final int PICKER = 100;
    /** The notebook's items 0..7. */
    public static final int TASTE = 150;
    /** The plain rows, first row = ROW. */
    public static final int ROW = 200;

    public static boolean isRow(int hit) { return hit >= ROW; }

    public static int rowIndex(int hit) { return hit - ROW; }

    public static boolean isPicker(int hit) { return hit >= PICKER && hit < PICKER + 36; }

    public static boolean isPouch(int hit) { return hit >= POUCH && hit < POUCH + 9; }

    public static boolean isHotbar(int hit) { return hit >= HOTBAR && hit < HOTBAR + 9; }

    public static boolean isStance(int hit) { return hit >= STANCE && hit < STANCE + 3; }

    public static boolean isTab(int hit) { return hit >= TAB && hit < TAB + 4; }

    public static boolean isTaste(int hit) { return hit >= TASTE && hit < TASTE + 8; }
}
