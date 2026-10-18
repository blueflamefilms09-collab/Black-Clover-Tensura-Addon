package com.newuniverse.nusmp.client.elite.forge;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: the minigames draw rings, dots and lines with nothing but filled rectangles (the GUI has no line or circle primitive), so these few helpers keep the views
 * short: a ring is a ring of small squares placed by angle, a line is a run of squares along it. Angles are in tenths of a degree, 0 at the top, growing clockwise (the
 * unit the rune rules use). All colours are ARGB.
 */
final class ForgeDraw {
    private ForgeDraw() {}

    static final int GOLD = 0xFFE8C468, CYAN = 0xFF5CD8FF, GREEN = 0xFF7CF0B0, RED = 0xFFFF6A6A, TEXT = 0xFFE6F2FF, DIM = 0xFF8FA3BF, FAINT = 0xFF5C6E88, ORANGE = 0xFFFF9A3C, VIOLET = 0xFF9A7CFF;

    static int alpha(int rgb, float a) { return ((int) (Math.max(0f, Math.min(1f, a)) * 255) << 24) | (rgb & 0xFFFFFF); }

    static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    static double sin(int tenths) { return Math.sin(Math.toRadians(tenths / 10.0)); }

    static double cos(int tenths) { return Math.cos(Math.toRadians(tenths / 10.0)); }

    /** A filled square of the given size centred on a point. */
    static void dot(GuiGraphics g, double x, double y, int size, int argb) {
        int h = size / 2;
        g.fill((int) Math.round(x) - h, (int) Math.round(y) - h, (int) Math.round(x) - h + size, (int) Math.round(y) - h + size, argb);
    }

    /** A straight line of dots. */
    static void line(GuiGraphics g, double x0, double y0, double x1, double y1, int size, int argb) {
        double d = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        int n = (int) Math.max(1, Math.ceil(d));
        for (int i = 0; i <= n; i++) dot(g, x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, size, argb);
    }

    /** A ring of dots around a centre. */
    static void ring(GuiGraphics g, double cx, double cy, double r, int size, int argb) {
        int n = (int) Math.max(24, Math.round(r * 2.4));
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            dot(g, cx + r * Math.sin(a), cy - r * Math.cos(a), size, argb);
        }
    }

    /** An arc between two angles (tenths of a degree, clockwise from the top). */
    static void arc(GuiGraphics g, double cx, double cy, double r, int fromTenths, int toTenths, int size, int argb) {
        int span = Math.floorMod(toTenths - fromTenths, 3600);
        int n = (int) Math.max(2, Math.round(r * span / 3600.0 * 2.4));
        for (int i = 0; i <= n; i++) {
            int a = fromTenths + span * i / n;
            dot(g, cx + r * sin(a), cy - r * cos(a), size, argb);
        }
    }

    /** A point on a circle. */
    static double px(double cx, double r, int tenths) { return cx + r * sin(tenths); }

    static double py(double cy, double r, int tenths) { return cy - r * cos(tenths); }

    /** A horizontal bar of the given fill 0..1 inside a dark well. */
    static void bar(GuiGraphics g, int x, int y, int w, int h, float fill, int argb) {
        g.fill(x, y, x + w, y + h, 0xCC060912);
        g.fill(x + 1, y + 1, x + 1 + Math.round((w - 2) * Math.max(0f, Math.min(1f, fill))), y + h - 1, argb);
        g.renderOutline(x, y, w, h, alpha(argb, 0.6f));
    }
}
