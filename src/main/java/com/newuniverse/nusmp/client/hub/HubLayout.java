package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;

/**
 * Why: the hub is drawn on one fixed stage of {@link #STAGE_W} by {@link #STAGE_H} units that is scaled to the window (No Index's discipline), so a layout bug cannot depend on the player's window: it either exists on
 * the stage or it does not. All stage numbers live here as plain ints and floats with no Minecraft types, so {@code HubLayoutTest} can check, for a grid of window sizes and every node count, that the stage fits the
 * window, no two nodes overlap, every label stays inside the stage, and a click lands on the node that was drawn. The screen only reads these numbers.
 * <p>
 * Two views share the stage. HOME: the ring is large in the middle, each node has a label next to it. TAB: the ring shrinks to a rail on the left and the tab's page fills the panel on the right.
 */
public final class HubLayout {
    private HubLayout() {}

    public static final int STAGE_W = 400, STAGE_H = 240;
    /** The scale of the stage never goes under this (a tiny window still shows everything, just small) nor over {@link #MAX_SCALE}, and snaps down to a multiple of 1/8 so text stays crisp. */
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.5f;

    // ---------------------------------------------------------------- window <-> stage
    /** Where the stage sits in the window: its scale and the window coordinates of its top-left corner. */
    public record View(float scale, float ox, float oy) {
        public float sx(double windowX) { return (float) ((windowX - ox) / scale); }

        public float sy(double windowY) { return (float) ((windowY - oy) / scale); }

        public int wx(float stageX) { return Math.round(ox + stageX * scale); }

        public int wy(float stageY) { return Math.round(oy + stageY * scale); }
    }

    public static View view(int winW, int winH) {
        float s = Math.min(Math.max(1, winW) / (float) STAGE_W, Math.max(1, winH) / (float) STAGE_H);
        s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, (float) Math.floor(s * 8f) / 8f));
        return new View(s, (winW - STAGE_W * s) / 2f, (winH - STAGE_H * s) / 2f);
    }

    // ---------------------------------------------------------------- the ring
    public static final int HOME_CX = 200, HOME_CY = 116, HOME_R = 76, HOME_NODE = 17;
    public static final int RAIL_CX = 66, RAIL_CY = 130, RAIL_R = 48, RAIL_NODE = 11;

    private static final float[] HOME_X = new float[HubNodes.COUNT], HOME_Y = new float[HubNodes.COUNT], RAIL_X = new float[HubNodes.COUNT], RAIL_Y = new float[HubNodes.COUNT];

    static {
        for (int i = 0; i < HubNodes.COUNT; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / HubNodes.COUNT;
            HOME_X[i] = (float) (HOME_CX + HOME_R * Math.cos(a));
            HOME_Y[i] = (float) (HOME_CY + HOME_R * Math.sin(a));
            RAIL_X[i] = (float) (RAIL_CX + RAIL_R * Math.cos(a));
            RAIL_Y[i] = (float) (RAIL_CY + RAIL_R * Math.sin(a));
        }
    }

    public static float nodeX(boolean home, int i) { return home ? HOME_X[i] : RAIL_X[i]; }

    public static float nodeY(boolean home, int i) { return home ? HOME_Y[i] : RAIL_Y[i]; }

    public static int nodeR(boolean home) { return home ? HOME_NODE : RAIL_NODE; }

    public static int ringCx(boolean home) { return home ? HOME_CX : RAIL_CX; }

    public static int ringCy(boolean home) { return home ? HOME_CY : RAIL_CY; }

    public static int ringR(boolean home) { return home ? HOME_R : RAIL_R; }

    /** The node under the stage point, or -1. Nodes are far enough apart that at most one can match (tested). */
    public static int nodeAt(boolean home, float sx, float sy) {
        int r = nodeR(home) + 2;
        for (int i = 0; i < HubNodes.COUNT; i++) {
            float dx = sx - nodeX(home, i), dy = sy - nodeY(home, i);
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }

    /** True when the point is on the middle of the ring (in the TAB view this is the way back to the ring). */
    public static boolean onCore(boolean home, float sx, float sy) {
        int r = home ? 30 : RAIL_R - RAIL_NODE - 6;
        float dx = sx - ringCx(home), dy = sy - ringCy(home);
        return dx * dx + dy * dy <= r * r;
    }

    // ---------------------------------------------------------------- labels (HOME view)
    public static final int LABEL_MAX_W = 92, LABEL_H = 9, LABEL_GAP = 4;

    /** 0 = centred above the node, 1 = to its right, 2 = to its left, 3 = centred below it. */
    public static int labelSide(int i) {
        double a = -Math.PI / 2 + i * 2 * Math.PI / HubNodes.COUNT;
        double c = Math.cos(a), s = Math.sin(a);
        if (Math.abs(c) < 0.3) return s < 0 ? 0 : 3;
        return c > 0 ? 1 : 2;
    }

    /** The rectangle of the label of node {@code i} (HOME view) for a text {@code textW} wide (at most {@link #LABEL_MAX_W}). */
    public static Rect label(int i, int textW) {
        int w = Math.max(1, Math.min(LABEL_MAX_W, textW));
        int nx = Math.round(HOME_X[i]), ny = Math.round(HOME_Y[i]);
        return switch (labelSide(i)) {
            case 0 -> new Rect(nx - w / 2, ny - HOME_NODE - LABEL_GAP - LABEL_H, w, LABEL_H);
            case 1 -> new Rect(nx + HOME_NODE + LABEL_GAP, ny - LABEL_H / 2, w, LABEL_H);
            case 2 -> new Rect(nx - HOME_NODE - LABEL_GAP - w, ny - LABEL_H / 2, w, LABEL_H);
            default -> new Rect(nx - w / 2, ny + HOME_NODE + LABEL_GAP, w, LABEL_H);
        };
    }

    // ---------------------------------------------------------------- chrome
    /** The one line at the bottom of the stage: the description of the hovered node (HOME) or the key hints; at most {@link #LINE_W} wide. */
    public static final int LINE_W = 372, LINE_Y = 226;

    public static Rect line() { return new Rect((STAGE_W - LINE_W) / 2, LINE_Y, LINE_W, LABEL_H); }

    // ---------------------------------------------------------------- the page panel (TAB view)
    /** The frame of the page, its title line, and the scrolling body (the scrollbar sits in the 4 units right of the body). */
    public static final Rect PANEL = new Rect(132, 26, 264, 186), TITLE = new Rect(140, 32, 244, 10), BODY = new Rect(140, 48, 244, 160);
    public static final int SCROLLBAR_X = 387, SCROLLBAR_W = 3;

    // ================================================================ Hub v2: the framed window, the icon strip, the Codex page layout
    // (appended; every constant above stays, the tests of the old layout still pass. The screen reads the numbers below since 4.65.0.)

    /** The hub window size in percent of the screen (option {@code ui.hubSize}): the default, the range, and the scale under which text stops being readable. */
    public static final int DEFAULT_SIZE = 62, MIN_SIZE = 40, MAX_SIZE = 100;
    public static final float MIN_READ_SCALE = 0.75f;

    /**
     * Where the framed window (the whole 400 x 240 stage) sits for a screen of {@code winW} by {@code winH} GUI units and a size of {@code pct} percent. 100 = the old full-screen fit ({@link #view(int, int)}).
     * Under 100 the window takes about {@code pct}% of the width and {@code pct * 70 / 62}% of the height (62 -> 62% x 70%) but never more than {@code pct / 62} times the stage (62 -> 400 x 240 units), never
     * less than {@link #MIN_READ_SCALE} when the screen has room for that, snaps to eighths, and always fits the screen.
     */
    public static View view(int winW, int winH, int pct) {
        if (pct >= MAX_SIZE) return view(winW, winH);
        int p = Math.max(MIN_SIZE, pct);
        float w = Math.max(1, winW), h = Math.max(1, winH);
        float full = Math.min(w / STAGE_W, h / STAGE_H);                                        // the biggest stage the screen holds
        float s = Math.min(w * p / 100f / STAGE_W, h * Math.min(100f, p * 70f / 62f) / 100f / STAGE_H);
        s = Math.min(s, p / (float) DEFAULT_SIZE);
        s = Math.max(s, Math.min(MIN_READ_SCALE, full));
        s = Math.min(Math.round(s * 8f) / 8f, (float) Math.floor(full * 8f) / 8f);               // snap, but never past what fits
        s = Math.max(MIN_SCALE, s);
        return new View(s, (winW - STAGE_W * s) / 2f, (winH - STAGE_H * s) / 2f);
    }

    /** The sizes the Settings page steps through (percent). */
    public static final int[] SIZE_STEPS = {50, 62, 75, 100};

    /** The next size after {@code pct} in {@link #SIZE_STEPS}, wrapping from the last back to the first (a size that is not a step goes to the next bigger step). */
    public static int nextSize(int pct) {
        for (int s : SIZE_STEPS) if (s > pct) return s;
        return SIZE_STEPS[0];
    }

    /** The size of the window in screen (GUI) units; its top-left corner is {@code (view.ox(), view.oy())}. */
    public static int windowW(View v) { return Math.round(STAGE_W * v.scale()); }

    public static int windowH(View v) { return Math.round(STAGE_H * v.scale()); }

    // ---------------------------------------------------------------- the icon strip (page view): the eight nodes in a column on the left, the way back to the ring above
    public static final int STRIP_CX = 19, STRIP_NODE = 11, STRIP_TOP = 42, STRIP_BOTTOM = 210, BACK_CX = 19, BACK_CY = 17, BACK_R = 9;

    /** The distance between two strip nodes: 24 for eight nodes, tighter when more are appended so the column keeps its place. */
    public static float stripPitch() { return Math.min(24f, (STRIP_BOTTOM - STRIP_TOP) / (float) Math.max(1, HubNodes.COUNT - 1)); }

    public static float stripX(int i) { return STRIP_CX; }

    public static float stripY(int i) { return STRIP_TOP + i * stripPitch(); }

    /** The strip node under the stage point, or -1. */
    public static int stripNodeAt(float sx, float sy) {
        int r = STRIP_NODE + 1;
        for (int i = 0; i < HubNodes.COUNT; i++) {
            float dx = sx - stripX(i), dy = sy - stripY(i);
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }

    /** True on the round button above the strip (the way back to the ring). */
    public static boolean onBack(float sx, float sy) {
        float dx = sx - BACK_CX, dy = sy - BACK_CY;
        return dx * dx + dy * dy <= (BACK_R + 1) * (BACK_R + 1);
    }

    // ---------------------------------------------------------------- the page panel (page view)
    /** The frame of the page, its title line and the scrolling body of every page but Status and Codex; the scrollbar sits in the 5 units right of the body. */
    public static final Rect PANEL2 = new Rect(40, 24, 356, 192), TITLE2 = new Rect(48, 29, 340, 10), BODY2 = new Rect(48, 44, 338, 166);
    /** The Status page: the player panel on the left (not scrolled), the card list on the right. */
    public static final Rect PLAYER = new Rect(48, 44, 92, 166), STATUS_LIST = new Rect(146, 44, 240, 166);
    /** The Codex page: the navigation rail on the left (scrolls by itself), the page body on the right. */
    public static final Rect SUBRAIL = new Rect(48, 44, 80, 166), CODEX_BODY = new Rect(134, 44, 252, 166);
    public static final int SCROLLBAR2_GAP = 2, SCROLLBAR2_W = 3;
    /** The name of the hub, top left of the window, next to the back button. */
    public static final int NAME_X = 34, NAME_Y = 10;

    /** The scrolling body of the page of tab {@code t} (v2 layout). */
    public static Rect body(HubNodes.Tab t) {
        return switch (t) {
            case STATUS -> STATUS_LIST;
            case CODEX -> CODEX_BODY;
            default -> BODY2;
        };
    }
}
