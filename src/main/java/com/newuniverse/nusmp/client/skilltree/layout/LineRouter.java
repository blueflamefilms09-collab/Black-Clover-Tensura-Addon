package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.List;

/**
 * Why: the link from a parent to a child is the one thing on the canvas that tells the player where a skill comes from, and it has to stay readable with no help from the author of
 * the JSON (who only gives grid coordinates). Orthogonal lines with a single bend read as a tree diagram, share their first stretch when several children hang off one parent (so a
 * branch looks like a branch), and can be drawn with plain one-pixel fills, which is all the GUI toolkit offers without a texture. A diagonal would need a rotated quad and would
 * alias; a curve would need a texture per angle. The router tries the two single-bend shapes in a fixed order (horizontal first, then vertical, which puts the line out of the parent's
 * side and into the child's top or bottom) and only when a third node sits on both of them falls back to a three-segment line through the gap between the two columns, so the shape is
 * predictable and a line never runs through an unrelated node when a clean path exists. Everything is integer content-pixel arithmetic, which is what makes the result exactly
 * testable and identical in the game and in the preview.
 */
public final class LineRouter {
    private LineRouter() {}

    /** Clearance kept between a line and the box of a node that is not one of its two ends. */
    public static final int CLEARANCE = 2;

    /**
     * The polyline from the edge of the parent box to the edge of the child box as content pixels {@code x0, y0, x1, y1, ...} (always axis-aligned, never with a zero-length piece
     * except when both centres are the same point, which gives the single point twice).
     *
     * @param px        parent centre x
     * @param py        parent centre y
     * @param cx        child centre x
     * @param cy        child centre y
     * @param half      half the side of a node box
     * @param obstacles boxes {@code {left, top, right, bottom}} (right and bottom exclusive) of the other nodes, which the line should not cross
     */
    public static int[] route(int px, int py, int cx, int cy, int half, List<int[]> obstacles) {
        int dx = cx - px, dy = cy - py;
        if (dx == 0 && dy == 0) return new int[]{px, py, cx, cy};
        int sx = Integer.signum(dx), sy = Integer.signum(dy);
        if (dy == 0) return new int[]{px + sx * half, py, cx - sx * half, cy};
        if (dx == 0) return new int[]{px, py + sy * half, cx, cy - sy * half};
        int[] horizontalFirst = {px + sx * half, py, cx, py, cx, cy - sy * half};
        if (clear(horizontalFirst, obstacles)) return horizontalFirst;
        int[] verticalFirst = {px, py + sy * half, px, cy, cx - sx * half, cy};
        if (clear(verticalFirst, obstacles)) return verticalFirst;
        int fromX = px + sx * half, toX = cx - sx * half;
        int mid = fromX + (toX - fromX) / 2;
        int[] throughGap = {fromX, py, mid, py, mid, cy, toX, cy};
        if (clear(throughGap, obstacles)) return throughGap;
        return horizontalFirst;
    }

    /** True when no piece of the polyline comes within {@link #CLEARANCE} pixels of an obstacle box. */
    static boolean clear(int[] pts, List<int[]> obstacles) {
        for (int i = 0; i + 3 < pts.length; i += 2) {
            for (int[] b : obstacles) if (hits(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], b)) return false;
        }
        return true;
    }

    private static boolean hits(int x0, int y0, int x1, int y1, int[] box) {
        int l = box[0] - CLEARANCE, t = box[1] - CLEARANCE, r = box[2] + CLEARANCE, b = box[3] + CLEARANCE;
        int minX = Math.min(x0, x1), maxX = Math.max(x0, x1), minY = Math.min(y0, y1), maxY = Math.max(y0, y1);
        if (y0 == y1) return y0 >= t && y0 < b && minX < r && maxX > l;               // horizontal piece
        return x0 >= l && x0 < r && minY < b && maxY > t;                              // vertical piece
    }
}
