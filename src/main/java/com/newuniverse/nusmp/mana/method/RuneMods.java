package com.newuniverse.nusmp.mana.method;

/**
 * The rune modifiers of the Mana Method, a tiny bit mask (pure). In the lore a rune array gives a spell a command: "pursue" (HOMING) or "grow" (GROWTH). The mask is stored in the
 * spell's NBT and sent in the packet; bits are only ever APPENDED.
 */
public final class RuneMods {
    private RuneMods() {}

    public static final int NONE = 0, HOMING = 1, GROWTH = 2, ALL = HOMING | GROWTH;

    public static boolean has(int mods, int bit) { return (mods & bit) != 0; }

    /** Anything outside the known bits is dropped (a packet or a tag cannot invent a modifier). */
    public static int sanitize(int mods) { return mods & ALL; }

    public static int count(int mods) { return Integer.bitCount(sanitize(mods)); }

    public static String describe(int mods) {
        int m = sanitize(mods);
        if (m == NONE) return "plain";
        if (m == ALL) return "pursue + grow";
        return m == HOMING ? "pursue" : "grow";
    }
}
