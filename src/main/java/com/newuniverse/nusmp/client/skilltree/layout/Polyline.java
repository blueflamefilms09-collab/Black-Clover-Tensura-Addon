package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the canvas draws every link in three ways (solid, dashed for a sealed route, and partly filled while the line "runs" into a node the player has just bought), and the GUI
 * toolkit can only fill axis-aligned rectangles. Each of those is therefore a question about distance along an axis-aligned polyline: how long is it, where is the point a given
 * fraction along, which stretches are the dashes. Answering them here, on plain {@code int} arrays, keeps the draw code to a loop of {@code fill} calls and makes the dash phase and
 * the fill fraction exactly testable; the dash pattern continues across a bend instead of restarting, which is what a dashed line looks like when it is drawn by hand. Every
 * method assumes axis-aligned pieces, which is all {@link LineRouter} produces.
 */
public final class Polyline {
    private Polyline() {}

    /** The total length of an axis-aligned polyline {@code x0, y0, x1, y1, ...}. */
    public static int length(int[] pts) {
        int len = 0;
        for (int i = 0; i + 3 < pts.length; i += 2) len += Math.abs(pts[i + 2] - pts[i]) + Math.abs(pts[i + 3] - pts[i + 1]);
        return len;
    }

    /**
     * The first {@code fraction} (0 to 1) of the polyline, measured along its length, as a polyline of its own. 0 gives the start point twice; 1 or more gives a copy of the whole
     * line. Used to draw a link that is filling toward its child.
     */
    public static int[] prefix(int[] pts, float fraction) {
        if (pts.length < 4) return pts.clone();
        if (fraction >= 1f) return pts.clone();
        int target = Math.round(length(pts) * Math.max(0f, fraction));
        List<Integer> out = new ArrayList<>();
        out.add(pts[0]);
        out.add(pts[1]);
        int walked = 0;
        for (int i = 0; i + 3 < pts.length; i += 2) {
            int x0 = pts[i], y0 = pts[i + 1], x1 = pts[i + 2], y1 = pts[i + 3];
            int len = Math.abs(x1 - x0) + Math.abs(y1 - y0);
            if (walked + len <= target) {
                out.add(x1);
                out.add(y1);
                walked += len;
                continue;
            }
            int rest = target - walked;
            out.add(x0 + Integer.signum(x1 - x0) * rest);
            out.add(y0 + Integer.signum(y1 - y0) * rest);
            walked = target;
            break;
        }
        if (out.size() == 2) {                                              // a fraction of 0: the start point, twice
            out.add(pts[0]);
            out.add(pts[1]);
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /**
     * The dashes of the polyline as separate axis-aligned pieces {@code x0, y0, x1, y1, ...} (each piece of length at most {@code dash}, separated by {@code gap}); the pattern runs
     * on across bends, and {@code phase} slides it along the line so a caller can make the dashes march.
     */
    public static int[] dashes(int[] pts, int dash, int gap, int phase) {
        int d = Math.max(1, dash), period = d + Math.max(0, gap);
        List<Integer> out = new ArrayList<>();
        int travelled = 0;
        for (int i = 0; i + 3 < pts.length; i += 2) {
            int x0 = pts[i], y0 = pts[i + 1], x1 = pts[i + 2], y1 = pts[i + 3];
            int len = Math.abs(x1 - x0) + Math.abs(y1 - y0);
            if (len == 0) continue;
            int ux = Integer.signum(x1 - x0), uy = Integer.signum(y1 - y0);
            int s = 0;
            while (s < len) {
                int pos = Math.floorMod(travelled + s + phase, period);
                if (pos < d) {
                    int e = Math.min(len, s + (d - pos));
                    out.add(x0 + ux * s);
                    out.add(y0 + uy * s);
                    out.add(x0 + ux * e);
                    out.add(y0 + uy * e);
                    s = e;
                } else {
                    s = Math.min(len, s + (period - pos));
                }
            }
            travelled += len;
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }
}
