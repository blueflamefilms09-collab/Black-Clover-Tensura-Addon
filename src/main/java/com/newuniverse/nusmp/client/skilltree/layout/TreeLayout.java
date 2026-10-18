package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a route is data (every node carries a grid {@code x}/{@code y} chosen by whoever wrote the JSON), and the screen has to turn that grid into pixels, keep the player's pan and
 * zoom inside sensible bounds, find the node under the mouse and draw a line from every parent to its child. Doing it in one pure class that works on the node records themselves
 * (they have no Minecraft types) is what lets a new magic arrive as a datapack and still look right with no screen code, and what lets the tests lay out the shape of the four
 * shipped trees and hit-test their edges without a client. The mapping is a plain affine transform, {@code screen = pan + content * zoom}, so panning is addition, zooming about
 * the cursor is one formula, and every clamp is per axis; the tree is positioned relative to its own bounding box, so a route whose nodes start at column 3 does not open with an
 * empty strip on the left. Node centres sit on a fixed pitch a little larger than the node box, which leaves a corridor between neighbours for the connecting lines.
 */
public final class TreeLayout {
    /** Side of a node box, and the distance between the centres of two neighbouring grid cells, in content pixels at zoom 1. */
    public static final int NODE = 22, CELL_X = 38, CELL_Y = 32;
    /** Empty margin around the nodes inside the content, at zoom 1. */
    public static final int PAD = 12;
    /** The zoom range the view allows. */
    public static final float MIN_ZOOM = 0.4f, MAX_ZOOM = 2.0f;

    /** A line from a parent node to its child: the node indexes, the parent level the child asks for, and the route as content-pixel points (x0, y0, x1, y1, ...). */
    public record Edge(int parent, int child, int requiredLevel, int[] points) {}

    private final int[] cx, cy;
    private final int width, height;

    private TreeLayout(int[] cx, int[] cy, int width, int height) {
        this.cx = cx; this.cy = cy; this.width = width; this.height = height;
    }

    /** Lays out the nodes in their list order (index {@code i} of every method is {@code nodes.get(i)}). */
    public static TreeLayout of(List<SkillNode> nodes) {
        int n = nodes.size();
        int[] cx = new int[n], cy = new int[n];
        if (n == 0) return new TreeLayout(cx, cy, 2 * PAD, 2 * PAD);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (SkillNode nd : nodes) {
            minX = Math.min(minX, nd.x()); maxX = Math.max(maxX, nd.x());
            minY = Math.min(minY, nd.y()); maxY = Math.max(maxY, nd.y());
        }
        for (int i = 0; i < n; i++) {
            cx[i] = PAD + NODE / 2 + (nodes.get(i).x() - minX) * CELL_X;
            cy[i] = PAD + NODE / 2 + (nodes.get(i).y() - minY) * CELL_Y;
        }
        return new TreeLayout(cx, cy, 2 * PAD + NODE + (maxX - minX) * CELL_X, 2 * PAD + NODE + (maxY - minY) * CELL_Y);
    }

    public int size() { return cx.length; }

    /** Width of the whole tree including its margin, in content pixels at zoom 1. */
    public int contentWidth() { return width; }

    public int contentHeight() { return height; }

    /** Centre of node {@code i} in content pixels. */
    public int centerX(int i) { return cx[i]; }

    public int centerY(int i) { return cy[i]; }

    /** The box of node {@code i} in content pixels. */
    public Rect box(int i) { return new Rect(cx[i] - NODE / 2, cy[i] - NODE / 2, NODE, NODE); }

    // ------------------------------------------------------------------ edges
    /**
     * Every parent link of every node as a routed line. Parents that name a node the route does not have are skipped (the loader rejects them; a stale client copy must not crash the
     * screen). The line of an edge avoids the boxes of the other nodes where it can ({@link LineRouter}).
     */
    public List<Edge> edges(List<SkillNode> nodes) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) index.put(nodes.get(i).id(), i);
        List<int[]> boxes = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            Rect b = box(i);
            boxes.add(new int[]{i, b.x(), b.y(), b.right(), b.bottom()});
        }
        List<Edge> out = new ArrayList<>();
        for (int c = 0; c < nodes.size() && c < size(); c++) {
            for (SkillNode.Parent p : nodes.get(c).parents()) {
                Integer pi = index.get(p.node());
                if (pi == null || pi == c || pi >= size()) continue;
                List<int[]> obstacles = new ArrayList<>();
                for (int[] b : boxes) if (b[0] != pi && b[0] != c) obstacles.add(new int[]{b[1], b[2], b[3], b[4]});
                out.add(new Edge(pi, c, p.level(), LineRouter.route(cx[pi], cy[pi], cx[c], cy[c], NODE / 2, obstacles)));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ view mapping and hit testing
    /**
     * The node under a point, or -1. The point is in viewport pixels (origin at the viewport's top-left corner); {@code slop} widens every box a little so thin gaps do not feel dead.
     * When boxes overlap (a careless data file) the later node wins, because it is drawn on top.
     */
    public int hit(double px, double py, float zoom, float panX, float panY, float slop) {
        float half = NODE * zoom / 2f;
        for (int i = size() - 1; i >= 0; i--) {
            float sx = panX + cx[i] * zoom, sy = panY + cy[i] * zoom;
            if (px >= sx - half - slop && px < sx + half + slop && py >= sy - half - slop && py < sy + half + slop) return i;
        }
        return -1;
    }

    /** The zoom at which the whole tree fits a viewport of {@code vw} by {@code vh}, kept within [{@code minZoom}, {@code maxZoom}]. */
    public float fitZoom(int vw, int vh, float minZoom, float maxZoom) {
        float z = Math.min(vw / (float) width, vh / (float) height);
        return Math.max(minZoom, Math.min(maxZoom, z));
    }

    /**
     * The view a route first opens with, as {@code {zoom, panX, panY}}: the whole tree when it fits at a readable zoom (not below 0.6), otherwise the roots at the left with the first
     * node centred vertically, because the first node of a route is a root and that is where a player starts reading. The pan is already legal.
     */
    public float[] initialView(int vw, int vh) {
        float zoom = fitZoom(vw, vh, 0.6f, 1f);
        float panY = size() > 0 ? vh / 2f - cy[0] * zoom : 0f;
        return new float[]{zoom, clampPanX(0f, zoom, vw), clampPanY(panY, zoom, vh)};
    }

    /** The view that shows the whole tree (down to the minimum zoom), as {@code {zoom, panX, panY}}: what the Fit control does. */
    public float[] fitView(int vw, int vh) {
        float zoom = fitZoom(vw, vh, MIN_ZOOM, 1f);
        return new float[]{zoom, clampPanX(0f, zoom, vw), clampPanY(0f, zoom, vh)};
    }

    /** The zoom kept within the allowed range. */
    public static float clampZoom(float z) { return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z)); }

    /** The horizontal pan kept legal: a tree narrower than the viewport is centred, a wider one may not leave a gap at either edge. */
    public float clampPanX(float pan, float zoom, int viewW) { return clampAxis(pan, width * zoom, viewW); }

    /** The vertical pan kept legal, see {@link #clampPanX}. */
    public float clampPanY(float pan, float zoom, int viewH) { return clampAxis(pan, height * zoom, viewH); }

    private static float clampAxis(float pan, float content, int view) {
        if (content <= view) return (view - content) / 2f;
        return Math.max(view - content, Math.min(0f, pan));
    }

    /**
     * The pan that keeps the content point under {@code anchor} (a viewport coordinate on the same axis) fixed while the zoom changes from {@code zoomOld} to {@code zoomNew}: the
     * point the player is looking at stays where it is, which is what zooming "toward the cursor" means.
     */
    public static float panForZoomAt(float pan, float zoomOld, float zoomNew, float anchor) {
        return anchor - (anchor - pan) * (zoomNew / zoomOld);
    }
}
