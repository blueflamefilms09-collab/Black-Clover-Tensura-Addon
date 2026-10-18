package com.newuniverse.nusmp.client.skilltree.layout;

/**
 * Why: every placement decision of the Skill Tree screen, and of the button that opens it from the existing menus, is one rectangle compared with another (does the panel fit the
 * window, does the new button touch a widget of the host menu, is the mouse over a node, is a box inside the scissor). Keeping that arithmetic in one tiny immutable value type with no
 * Minecraft import means the very same code runs in the game, in the unit tests and in the preview tool's layout dump, so a layout that passes the tests is the layout the player gets;
 * and an immutable record cannot hold state that drifts between frames, which matters for a screen that recomputes everything from the window size on every resize and every sync.
 * The test for "inside" uses exclusive right and bottom edges, the same convention as {@code GuiGraphics.fill}, so a rectangle drawn with a fill covers exactly the pixels it contains.
 *
 * @param x left edge in GUI pixels
 * @param y top edge in GUI pixels
 * @param w width in GUI pixels (a non-positive width makes the rectangle empty)
 * @param h height in GUI pixels (a non-positive height makes the rectangle empty)
 */
public record Rect(int x, int y, int w, int h) {

    public int right() { return x + w; }

    public int bottom() { return y + h; }

    public boolean isEmpty() { return w <= 0 || h <= 0; }

    /** True when the point lies in the rectangle (left and top edges inclusive, right and bottom exclusive). */
    public boolean contains(double px, double py) { return !isEmpty() && px >= x && py >= y && px < x + w && py < y + h; }

    /** True when the two rectangles share at least one pixel; an empty rectangle shares none. */
    public boolean intersects(Rect o) {
        if (isEmpty() || o.isEmpty()) return false;
        return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
    }

    /** True when the whole rectangle lies inside a {@code sw} by {@code sh} screen with at least {@code margin} pixels to every edge. */
    public boolean isInside(int sw, int sh, int margin) { return x >= margin && y >= margin && x + w <= sw - margin && y + h <= sh - margin; }

    /** This rectangle shrunk by {@code n} pixels on every side (grown when {@code n} is negative); the size never goes below zero. */
    public Rect inset(int n) { return new Rect(x + n, y + n, Math.max(0, w - 2 * n), Math.max(0, h - 2 * n)); }

    /** This rectangle moved by (dx, dy). */
    public Rect translate(int dx, int dy) { return new Rect(x + dx, y + dy, w, h); }
}
