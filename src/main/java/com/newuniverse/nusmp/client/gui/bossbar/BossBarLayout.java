package com.newuniverse.nusmp.client.gui.bossbar;

/** The vertical layout of the boss bars and the small maths of the fill (pure; the numbers are vanilla's, plus the taller mod bar). */
public final class BossBarLayout {
    private BossBarLayout() {}

    /** Width of every bar, and the y of the first one (the bar's top; its name sits 9 px above). */
    public static final int BAR_W = 182, START_Y = 12;
    /** Distance from one vanilla bar to the next (10 + font height 9) and from one mod bar to the next (frame, pips and a gap). */
    public static final int VANILLA_STEP = 19, MOD_STEP = 24;

    public static int step(boolean mod) { return mod ? MOD_STEP : VANILLA_STEP; }

    /** Vanilla draws a bar, moves down, and stops once the next position reaches a third of the screen height. */
    public static boolean continues(int nextY, int guiHeight) {
        return nextY < guiHeight / 3;
    }

    /** Pixels of the fill for a progress 0 .. 1: vanilla's {@code Mth.lerpDiscrete(progress, 0, 182)}. */
    public static int fillWidth(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        return (int) Math.floor(p * (BAR_W - 1)) + (p > 0f ? 1 : 0);
    }

    /** Whole percent for the text: a bar with any health left never reads 0. */
    public static int percent(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        int v = Math.round(p * 100f);
        return p > 0f && v == 0 ? 1 : v;
    }
}
