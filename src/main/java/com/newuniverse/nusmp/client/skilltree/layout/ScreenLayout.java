package com.newuniverse.nusmp.client.skilltree.layout;

/**
 * Why: the Skill Tree is one screen with five regions (header, route list, canvas strip and canvas, detail card, footer) that must not overlap at any window size, and the buttons
 * that sit in them are vanilla widgets whose positions Minecraft asks for in {@code init()} and again on every resize. Computing every rectangle from only the window size, in one
 * pure class, is what lets the screen, the unit tests and the preview tool agree on the same numbers (the preview runs this very class), and it is what makes a resize safe: nothing is
 * remembered between calls, so nothing can go stale. Widths follow the window in proportion but are clamped, because a route list wider than its longest name or a detail card wider
 * than a readable line only wastes the space the node canvas needs; the whole panel is capped, because on a 1920 x 1080 window at GUI scale 1 a panel stretched across the monitor
 * stops looking like a menu. Minecraft never gives a GUI smaller than 320 x 240 (its scale rule), and the clamps below hold from there up; smaller windows get a squeezed but
 * non-negative layout instead of an exception.
 */
public final class ScreenLayout {
    /** Smallest gap between the panel and the window edge. */
    public static final int MARGIN = 8;
    /** The panel never grows beyond this, so it keeps the proportions of a menu on huge windows. */
    public static final int MAX_W = 640, MAX_H = 400;
    /** Inner padding of the panel and the gap between regions. */
    public static final int PAD = 6, GAP = 4;
    /** Heights of the header row, the footer line, the canvas strip and of every button. */
    public static final int HEADER_H = 20, FOOTER_H = 10, STRIP_H = 22, BUTTON_H = 16;
    /** Column width bounds: the route list, the detail card, and the least width the canvas may be squeezed to before the columns give way. */
    public static final int LEFT_MIN = 80, LEFT_MAX = 120, RIGHT_MIN = 96, RIGHT_MAX = 156, CANVAS_MIN = 100;
    /** How narrow the columns may become when even the minimum canvas does not fit. */
    public static final int LEFT_FLOOR = 60, RIGHT_FLOOR = 80;
    /** The band at the foot of the route column that holds the "Set active" button: the button, and the gap above it. */
    public static final int ACTIONS_H = BUTTON_H + 2;

    public final Rect panel, header, routes, strip, canvas, detail, footer;
    /** The scrolling part of the route column (above the "Set active" band). */
    public final Rect routeList;
    /** Widgets: Close in the header, "Set active" at the foot of the route column, "Buy" at the bottom of the detail card. */
    public final Rect closeButton, setActiveButton, buyButton;
    /** The scrolling text area of the detail card (above the Buy button). */
    public final Rect detailText;
    /** The three view chips at the right end of the canvas strip: zoom out, zoom in, fit. They sit in the strip, not over the canvas, so they never cover a node. */
    public final Rect zoomOut, zoomIn, zoomFit;

    private ScreenLayout(Rect panel, Rect header, Rect routes, Rect routeList, Rect strip, Rect canvas, Rect detail, Rect footer, Rect closeButton, Rect setActiveButton,
                         Rect buyButton, Rect detailText, Rect zoomOut, Rect zoomIn, Rect zoomFit) {
        this.panel = panel; this.header = header; this.routes = routes; this.routeList = routeList; this.strip = strip; this.canvas = canvas; this.detail = detail; this.footer = footer;
        this.closeButton = closeButton; this.setActiveButton = setActiveButton; this.buyButton = buyButton; this.detailText = detailText;
        this.zoomOut = zoomOut; this.zoomIn = zoomIn; this.zoomFit = zoomFit;
    }

    /** All rectangles for a window of {@code width} by {@code height} GUI pixels ({@code Screen.width} and {@code Screen.height}). */
    public static ScreenLayout of(int width, int height) {
        int pw = Math.max(Math.min(width, 160), Math.min(MAX_W, width - 2 * MARGIN));
        int ph = Math.max(Math.min(height, 120), Math.min(MAX_H, height - 2 * MARGIN));
        int px = (width - pw) / 2, py = (height - ph) / 2;
        Rect panel = new Rect(px, py, pw, ph);

        int inner = Math.max(1, pw - 2 * PAD);
        int left = clamp(Math.round(inner * 0.23f), LEFT_MIN, LEFT_MAX);
        int right = clamp(Math.round(inner * 0.27f), RIGHT_MIN, RIGHT_MAX);
        int canvasW = inner - left - right - 2 * GAP;
        if (canvasW < CANVAS_MIN) {                                   // give width back to the canvas, the detail card first, then the route list
            int deficit = CANVAS_MIN - canvasW;
            int take = Math.max(0, Math.min(deficit, right - RIGHT_FLOOR));
            right -= take;
            deficit -= take;
            take = Math.max(0, Math.min(deficit, left - LEFT_FLOOR));
            left -= take;
            canvasW = inner - left - right - 2 * GAP;
        }
        canvasW = Math.max(1, canvasW);

        int bodyTop = py + PAD + HEADER_H + GAP;
        int bodyH = Math.max(1, ph - 2 * PAD - HEADER_H - FOOTER_H - 2 * GAP);
        int x0 = px + PAD;
        int midX = x0 + left + GAP;
        Rect header = new Rect(x0, py + PAD, inner, HEADER_H);
        Rect routes = new Rect(x0, bodyTop, left, bodyH);
        Rect strip = new Rect(midX, bodyTop, canvasW, STRIP_H);
        Rect canvas = new Rect(midX, bodyTop + STRIP_H + GAP, canvasW, Math.max(1, bodyH - STRIP_H - GAP));
        Rect detail = new Rect(midX + canvasW + GAP, bodyTop, right, bodyH);
        Rect footer = new Rect(x0, bodyTop + bodyH + GAP, inner, FOOTER_H);

        Rect close = new Rect(header.right() - 48, header.y() + (HEADER_H - BUTTON_H) / 2, 48, BUTTON_H);
        Rect routeList = new Rect(routes.x(), routes.y(), routes.w(), Math.max(1, routes.h() - ACTIONS_H));
        Rect setActive = new Rect(routes.x() + 3, routes.bottom() - 1 - BUTTON_H, Math.max(1, routes.w() - 6), BUTTON_H);
        Rect buy = new Rect(detail.x() + 6, detail.bottom() - 6 - BUTTON_H, Math.max(1, detail.w() - 12), BUTTON_H);
        int textTop = detail.y() + 6;
        Rect detailText = new Rect(detail.x() + 6, textTop, Math.max(1, detail.w() - 12), Math.max(1, buy.y() - 4 - textTop));

        int chipY = strip.y() + (STRIP_H - 13) / 2;
        Rect fit = new Rect(strip.right() - 4 - 22, chipY, 22, 13);
        Rect zin = new Rect(fit.x() - 2 - 13, chipY, 13, 13);
        Rect zout = new Rect(zin.x() - 2 - 13, chipY, 13, 13);
        return new ScreenLayout(panel, header, routes, routeList, strip, canvas, detail, footer, close, setActive, buy, detailText, zout, zin, fit);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
