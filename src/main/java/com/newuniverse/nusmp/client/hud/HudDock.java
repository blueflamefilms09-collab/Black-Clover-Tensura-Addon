package com.newuniverse.nusmp.client.hud;

import com.newuniverse.nusmp.client.heroweapon.HeroClientConfig;

/**
 * Why: the weapon, race and system panels (hero weapon, Lord weapons, Greed line, Beast Will, Saiyan, Shishou, Devil contract, relic key line) each picked their own spot on the screen, so
 * several at once overlapped and the screen filled with separate boxes (5.0.0, owner request). The dock gives them ONE place: a LEFT column and a RIGHT column along the bottom edge. Every panel that
 * is drawn asks {@link #take} for its spot while it draws; the panels stack upward in the order they are drawn, with a small gap, and a panel that is not shown takes no room. The pure part
 * ({@link #slot}) is arithmetic, so a test pins it. GUI Move still works: a panel the player has dragged keeps the saved place, the dock only decides the default. The client setting
 * {@code hud.unified} (nusmp-heroweapon-client.toml) false brings every panel back to the place it had before. Client only, allocation-free per frame except the 2-int result.
 */
public final class HudDock {
    private HudDock() {}

    public enum Side { LEFT, RIGHT }

    public static final int MARGIN = 6, GAP = 3, TOP_MIN = 4;

    private static int usedLeft, usedRight;

    /** Called once at the start of every frame (a layer below all others): the columns are empty again. */
    public static void begin() {
        usedLeft = 0;
        usedRight = 0;
    }

    public static boolean on() { return HeroClientConfig.unifiedHud(); }

    /** The top of a panel of height {@code h} in a column whose panels already use {@code used} pixels (pure). */
    public static int slot(int guiHeight, int used, int h) { return Math.max(TOP_MIN, guiHeight - MARGIN - used - h); }

    /** The left edge of a panel of width {@code w} on {@code side} (pure). */
    public static int edge(Side side, int guiWidth, int w) { return side == Side.LEFT ? MARGIN : Math.max(TOP_MIN, guiWidth - MARGIN - w); }

    /**
     * Reserves room for a panel and returns its top-left corner {x, y}. With the dock off (the setting) it returns {@code def} untouched, so the caller's own place is used.
     * The result array is fresh (two ints per panel per frame).
     */
    public static int[] take(Side side, int guiWidth, int guiHeight, int w, int h, int[] def) {
        if (!on()) return def;
        int used = side == Side.LEFT ? usedLeft : usedRight;
        int y = slot(guiHeight, used, h);
        if (side == Side.LEFT) usedLeft += h + GAP;
        else usedRight += h + GAP;
        return new int[]{edge(side, guiWidth, w), y};
    }
}
