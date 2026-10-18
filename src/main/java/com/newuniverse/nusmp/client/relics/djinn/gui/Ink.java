package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * The pen every part of the panel draws with: panel-relative coordinates (0,0 is the panel's top-left corner), a clip rectangle (nothing is ever
 * drawn outside the panel, whatever the animation does), a master alpha for the summon intro, and a few shapes made of fills. One instance is reused
 * for every frame; it holds no allocation.
 */
final class Ink {
    private Canvas cv;
    private int ox, oy, clipX1, clipY1, clipX2, clipY2;
    private float alpha = 1f;
    /** Rectangles handed to the canvas since {@link #begin}: the cost counter used by the tests. */
    int fills;

    void begin(Canvas c, int originX, int originY, float masterAlpha) {
        cv = c;
        ox = originX;
        oy = originY;
        clipX1 = 0;
        clipY1 = 0;
        clipX2 = GuiLayout.W;
        clipY2 = GuiLayout.H;
        alpha = masterAlpha;
        fills = 0;
    }

    /** Restricts drawing to these rows (the intro unfolds the panel from its middle). */
    void clipRows(int y1, int y2) {
        clipY1 = Math.max(0, y1);
        clipY2 = Math.min(GuiLayout.H, y2);
    }

    float alpha() { return alpha; }

    void fill(int x1, int y1, int x2, int y2, int argb) {
        if (x1 < clipX1) x1 = clipX1;
        if (y1 < clipY1) y1 = clipY1;
        if (x2 > clipX2) x2 = clipX2;
        if (y2 > clipY2) y2 = clipY2;
        if (x2 <= x1 || y2 <= y1) return;
        int a = (int) (((argb >>> 24) & 255) * alpha);
        if (a <= 0) return;
        fills++;
        cv.fill(ox + x1, oy + y1, ox + x2, oy + y2, (a << 24) | (argb & 0xFFFFFF));
    }

    void dot(int x, int y, int argb) { fill(x, y, x + 1, y + 1, argb); }

    void box(int x, int y, int w, int h, int argb) { fill(x, y, x + w, y + h, argb); }

    /** A 1 pixel outline. */
    void frame(int x, int y, int w, int h, int argb) {
        fill(x, y, x + w, y + 1, argb);
        fill(x, y + h - 1, x + w, y + h, argb);
        fill(x, y + 1, x + 1, y + h - 1, argb);
        fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    /** A straight line as short fills (steps of one pixel on the longer axis). */
    void line(int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx - dy;
        for (int i = 0; i < 256; i++) {
            fill(x0, y0, x0 + 1, y0 + 1, argb);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    /** Dots along an ellipse: n of them from angle a0 stepping da, each size x size. */
    void arc(float cx, float cy, float rx, float ry, float a0, float da, int n, int size, int argb) {
        for (int i = 0; i < n; i++) {
            float a = a0 + da * i;
            int x = Math.round(cx + GuiAnim.cos(a) * rx), y = Math.round(cy + GuiAnim.sin(a) * ry);
            fill(x, y, x + size, y + size, argb);
        }
    }

    /** A filled disc as one fill a row. */
    void disc(int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.floor(Math.sqrt(r * r + r - dy * dy));
            fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, argb);
        }
    }

    /** A plus-shaped twinkle of arm length {@code arm}. */
    void star(int x, int y, int arm, int argb) {
        fill(x - arm, y, x + arm + 1, y + 1, argb);
        fill(x, y - arm, x + 1, y + arm + 1, argb);
    }

    void text(String s, int x, int y, int argb) {
        if (s == null || s.isEmpty()) return;
        int a = (int) (((argb >>> 24) & 255) * alpha);
        if (a <= 8 || y < clipY1 || y + 8 > clipY2) return;
        cv.text(s, ox + x, oy + y, (a << 24) | (argb & 0xFFFFFF));
    }

    int textWidth(String s) { return cv.textWidth(s); }
}
