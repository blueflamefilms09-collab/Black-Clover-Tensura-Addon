package com.newuniverse.nusmp.client.skilltree;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.skilltree.layout.NodeState;
import com.newuniverse.nusmp.client.skilltree.layout.Polyline;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.skilltree.layout.ScreenLayout;
import com.newuniverse.nusmp.client.skilltree.layout.TreeLayout;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.NodeStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the canvas is the part of the screen the player looks at, and it has to do four jobs from one set of numbers: draw the parent lines and the nodes (in the route's colour, greyed
 * and dashed when the route is sealed), keep the view (pan and zoom) legal, turn a mouse position into a node, and animate a purchase. Everything it knows about geometry comes from
 * the pure {@link TreeLayout} and {@link Polyline}; this class only turns those numbers into {@code GuiGraphics} calls, so the layout is tested without a client and the drawing has
 * nothing left to get wrong except looks. It is stateless apart from the {@link View} the screen hands it (one per route, remembered by the screen between openings so the player comes
 * back to where they were), because the data it draws is the synced copy and must be read fresh every frame: a sync packet then updates the picture in place with no refresh step.
 * Nothing is drawn with a texture: lines are one-pixel-thick fills and nodes are the tree skin's frames with the item icon on top, so the cost is a few hundred fills per frame and the
 * same picture can be re-created by the preview tool.
 */
final class NodeCanvas {
    private NodeCanvas() {}

    /** How long, in ticks, the line toward a freshly bought node takes to fill, and how long the purchase pulse lasts. */
    static final float FILL_TICKS = 14f, PULSE_TICKS = 18f;
    private static final float ZOOM_STEP = 1.2f;
    private static final int GOLD_RGB = 0xE8C468;

    /** The player's view of one route: where it is panned, how far it is zoomed, and the running purchase animations. Remembered by the screen per route. */
    static final class View {
        float zoom = 1f, panX, panY;
        boolean placed;
        /** Node index to the tick the purchase pulse started, and child index to the tick its incoming lines started filling. */
        final Map<Integer, Float> pulse = new HashMap<>(), fill = new HashMap<>();
    }

    /** Everything one frame of the canvas needs; the screen builds it. {@code rgb} is the route colour as 0xRRGGBB. */
    record Frame(Rect vp, RouteShape shape, View view, NodeState[] states, int rgb, boolean unlocked, int selected, int hover, float now) {}

    // ------------------------------------------------------------------ the view
    /** First placement of a route: see {@link TreeLayout#initialView}. */
    static void place(View v, TreeLayout t, Rect vp) {
        float[] iv = t.initialView(vp.w(), vp.h());
        v.zoom = iv[0];
        v.panX = iv[1];
        v.panY = iv[2];
        v.placed = true;
    }

    /** Fits the whole tree in the viewport (down to the minimum zoom): see {@link TreeLayout#fitView}. */
    static void fit(View v, TreeLayout t, Rect vp) {
        float[] fv = t.fitView(vp.w(), vp.h());
        v.zoom = fv[0];
        v.panX = fv[1];
        v.panY = fv[2];
    }

    /** Keeps the pan legal for the current zoom and viewport (called every frame, so a resize or a zoom never leaves a gap). */
    static void clamp(View v, TreeLayout t, Rect vp) {
        v.panX = t.clampPanX(v.panX, v.zoom, vp.w());
        v.panY = t.clampPanY(v.panY, v.zoom, vp.h());
    }

    /** Multiplies the zoom by {@code factor} about the screen point (ax, ay), so the spot under the cursor stays put. */
    static void zoomBy(View v, TreeLayout t, Rect vp, float factor, double ax, double ay) {
        float nz = TreeLayout.clampZoom(v.zoom * factor);
        if (nz == v.zoom) return;
        v.panX = TreeLayout.panForZoomAt(v.panX, v.zoom, nz, (float) (ax - vp.x()));
        v.panY = TreeLayout.panForZoomAt(v.panY, v.zoom, nz, (float) (ay - vp.y()));
        v.zoom = nz;
        clamp(v, t, vp);
    }

    /** One zoom step in (true) or out (false) about the centre of the viewport. */
    static void step(View v, TreeLayout t, Rect vp, boolean in) {
        zoomBy(v, t, vp, in ? ZOOM_STEP : 1f / ZOOM_STEP, vp.x() + vp.w() / 2.0, vp.y() + vp.h() / 2.0);
    }

    /** The node under the mouse, or -1. */
    static int nodeAt(double mx, double my, Rect vp, View v, TreeLayout t) {
        if (!vp.contains(mx, my)) return -1;
        return t.hit(mx - vp.x(), my - vp.y(), v.zoom, v.panX, v.panY, 2f);
    }

    /** Starts the purchase animation of node {@code node} (its pulse, and the fill of its incoming lines when it was bought for the first time). */
    static void bought(View v, int node, int levelBefore, float now) {
        if (!UiTheme.motion()) return;                                   // with motion off the clock is frozen and an animation would never end
        v.pulse.put(node, now);
        if (levelBefore == 0) v.fill.put(node, now);
    }

    // ------------------------------------------------------------------ drawing: the background
    /** The canvas well, a faint magic circle in the route's colour and a dotted grid that moves with the pan. Drawn before the widgets. */
    static void drawBackground(GuiGraphics g, Rect vp, int rgb, boolean unlocked, View v, float now) { drawBackground(g, vp, rgb, unlocked, v, now, null); }

    /** 1.82.0: the same with the route, whose picture (if it has one) is drawn over the well and under the circle, the dots and the nodes. */
    static void drawBackground(GuiGraphics g, Rect vp, int rgb, boolean unlocked, View v, float now, com.newuniverse.nusmp.skilltree.data.SkillRoute route) {
        int accent = unlocked ? rgb : (TreeSkin.SEALED & 0xFFFFFF);
        TreeSkin.well(g, vp, accent);
        if (route != null) TreeBackground.draw(g, vp, route, v.panX, v.panY, unlocked);
        if (!TreeSkin.anime()) return;
        g.enableScissor(vp.x() + 1, vp.y() + 1, vp.right() - 1, vp.bottom() - 1);
        MagicCircle.draw(g, vp.x() + vp.w() / 2f, vp.y() + vp.h() / 2f, Math.min(vp.w(), vp.h()) * 0.62f, now, accent, unlocked ? 0.12f : 0.06f, MagicCircle.CALM, 1f);
        if (!unlocked) {                                                 // a sealed route: one big faint padlock behind the greyed tree
            int k = Math.max(3, Math.min(vp.w() / 14, vp.h() / 20));
            int px = vp.x() + (vp.w() - 6 * k) / 2, py = vp.y() + (vp.h() - 9 * k) / 2;
            g.pose().pushPose();
            g.pose().translate(px, py, 0f);
            g.pose().scale(k, k, 1f);
            TreeSkin.padlock(g, 0, 0, UiMath.withAlpha(accent, 0.10f), UiMath.withAlpha(0x000000, 0.18f));
            g.pose().popPose();
        }
        int step = Math.max(10, Math.round(16f * v.zoom));
        int ox = Math.floorMod(Math.round(v.panX), step), oy = Math.floorMod(Math.round(v.panY), step);
        int dot = UiMath.withAlpha(accent, 0.10f);
        for (int y = vp.y() + oy; y < vp.bottom(); y += step)
            for (int x = vp.x() + ox; x < vp.right(); x += step) g.fill(x, y, x + 1, y + 1, dot);
        g.disableScissor();
    }

    // ------------------------------------------------------------------ drawing: lines and nodes
    /** Draws the lines and the nodes of the frame, clipped to the viewport. */
    static void draw(GuiGraphics g, Font font, Frame f) {
        Rect vp = f.vp();
        g.enableScissor(vp.x() + 1, vp.y() + 1, vp.right() - 1, vp.bottom() - 1);
        drawEdges(g, f);
        int n = f.shape().layout.size();
        for (int i = 0; i < n; i++) if (i != f.selected()) drawNode(g, font, f, i);
        if (f.selected() >= 0 && f.selected() < n) drawNode(g, font, f, f.selected());            // the selected node on top of its neighbours
        g.disableScissor();
    }

    private static int[] toScreen(int[] pts, Frame f) {
        View v = f.view();
        int[] out = new int[pts.length];
        for (int i = 0; i + 1 < pts.length; i += 2) {
            out[i] = f.vp().x() + Math.round(v.panX + pts[i] * v.zoom);
            out[i + 1] = f.vp().y() + Math.round(v.panY + pts[i + 1] * v.zoom);
        }
        return out;
    }

    private static void drawEdges(GuiGraphics g, Frame f) {
        View v = f.view();
        int thin = Math.max(1, Math.round(v.zoom)), thick = Math.max(1, Math.round(1.5f * v.zoom));
        int phase = UiTheme.motion() ? (int) (f.now() * 0.5f) : 0;
        for (TreeLayout.Edge e : f.shape().edges) {
            int[] pts = toScreen(e.points(), f);
            int childLevel = f.states()[e.child()].level(), parentLevel = f.states()[e.parent()].level();
            if (!f.unlocked()) {
                pieces(g, Polyline.dashes(pts, 3, 3, 0), thin, UiMath.withAlpha(TreeSkin.SEALED, 0.40f));
                continue;
            }
            Float started = v.fill.get(e.child());
            float p = started == null ? 1f : UiMath.clamp01((f.now() - started) / FILL_TICKS);
            if (started != null && p >= 1f) v.fill.remove(e.child());
            boolean lit = childLevel >= 1;
            boolean open = !lit && parentLevel >= e.requiredLevel();
            if (lit && p >= 1f) {
                if (TreeSkin.anime()) chain(g, pts, thick + 2, UiMath.withAlpha(f.rgb(), 0.16f));        // a soft glow under the lit line
                chain(g, pts, thick, UiMath.withAlpha(f.rgb(), 0.95f));
            } else if (lit) {                                           // filling: the unlit line first, then the lit part running toward the child
                chain(g, pts, thin, UiMath.withAlpha(f.rgb(), 0.45f));
                chain(g, Polyline.prefix(pts, UiMath.easeOutCubic(p)), thick, UiMath.withAlpha(f.rgb(), 0.95f));
            } else if (open) {
                pieces(g, Polyline.dashes(pts, 3, 2, phase), thin, UiMath.withAlpha(f.rgb(), 0.65f));      // an open path: marching dashes
            } else {
                chain(g, pts, thin, UiMath.withAlpha(0x5C6E88, 0.45f));
            }
        }
    }

    /** Fills a chain of axis-aligned pieces {@code x0, y0, x1, y1, x2, y2, ...} (each point is the start of the next piece), {@code t} pixels thick. */
    private static void chain(GuiGraphics g, int[] pts, int t, int argb) {
        for (int i = 0; i + 3 < pts.length; i += 2) piece(g, pts[i], pts[i + 1], pts[i + 2], pts[i + 3], t, argb);
    }

    /** Fills separate axis-aligned pieces {@code x0, y0, x1, y1, x0', y0', ...} (a dash list), {@code t} pixels thick. */
    private static void pieces(GuiGraphics g, int[] pts, int t, int argb) {
        for (int i = 0; i + 3 < pts.length; i += 4) piece(g, pts[i], pts[i + 1], pts[i + 2], pts[i + 3], t, argb);
    }

    private static void piece(GuiGraphics g, int x0, int y0, int x1, int y1, int t, int argb) {
        int half = t / 2;
        if (y0 == y1) g.fill(Math.min(x0, x1), y0 - half, Math.max(x0, x1) + 1, y0 - half + t, argb);
        else g.fill(x0 - half, Math.min(y0, y1), x0 - half + t, Math.max(y0, y1) + 1, argb);
    }

    private static void drawNode(GuiGraphics g, Font font, Frame f, int i) {
        View v = f.view();
        TreeLayout t = f.shape().layout;
        SkillNode node = f.shape().route.nodes().get(i);
        NodeState st = f.states()[i];
        int s = Math.max(8, Math.round(TreeLayout.NODE * v.zoom));
        int x = f.vp().x() + Math.round(v.panX + (t.centerX(i) - TreeLayout.NODE / 2f) * v.zoom);
        int y = f.vp().y() + Math.round(v.panY + (t.centerY(i) - TreeLayout.NODE / 2f) * v.zoom);
        if (x + s < f.vp().x() - 6 || x > f.vp().right() + 6 || y + s < f.vp().y() - 6 || y > f.vp().bottom() + 6) return;
        int rgb = f.unlocked() ? f.rgb() : (TreeSkin.SEALED & 0xFFFFFF);
        float body, edge;
        int frameRgb = rgb;
        float dim;
        switch (st.status()) {
            case ROUTE_LOCKED -> { body = 0.50f; edge = 0.40f; dim = 0.55f; }
            case PARENTS_LOCKED -> { body = 0.42f; edge = 0.34f; dim = 0.45f; }
            case TOO_EXPENSIVE -> { body = 0.55f; edge = 0.62f; dim = 0.12f; }
            case AVAILABLE -> { body = 0.70f; edge = 1f; dim = 0f; }
            case OWNED -> { body = 0.90f; edge = 1f; dim = 0f; }
            default -> { body = 0.95f; edge = 1f; dim = 0f; frameRgb = GOLD_RGB; }
        }
        if (st.canBuy() && TreeSkin.anime()) {                           // a node that can be bought (or upgraded) right now glows
            float p = UiMath.pulse(f.now(), 36f, i * 0.13f);
            g.renderOutline(x - 2, y - 2, s + 4, s + 4, UiMath.withAlpha(rgb, 0.08f + 0.22f * p));
            g.renderOutline(x - 1, y - 1, s + 2, s + 2, UiMath.withAlpha(rgb, 0.18f + 0.34f * p));
        }
        TreeSkin.nodeFrame(g, x, y, s, frameRgb, body, edge);
        if (i == f.hover()) g.fill(x + 1, y + 1, x + s - 1, y + s - 1, UiMath.withAlpha(0xFFFFFF, 0.10f));
        Float pulseStart = v.pulse.get(i);
        float pt = pulseStart == null ? 1f : (f.now() - pulseStart) / PULSE_TICKS;
        if (pulseStart != null && pt >= 1f) v.pulse.remove(i);
        if (pulseStart != null && pt < 1f) {                             // a bought node flashes and throws two rings
            g.fill(x, y, x + s, y + s, UiMath.withAlpha(0xFFFFFF, 0.45f * (1f - pt)));
            int grow = Math.round(3f + 12f * UiMath.easeOutCubic(pt));
            g.renderOutline(x - grow, y - grow, s + 2 * grow, s + 2 * grow, UiMath.withAlpha(0xFFFFFF, 0.75f * (1f - pt)));
            int grow2 = Math.round(2f + 7f * UiMath.easeOutCubic(pt));
            g.renderOutline(x - grow2, y - grow2, s + 2 * grow2, s + 2 * grow2, UiMath.withAlpha(rgb, 0.8f * (1f - pt)));
        }

        float scale = s / (float) TreeLayout.NODE;
        int is = Math.round(16f * scale), ix = x + (s - is) / 2, iy = y + (s - is) / 2;
        ItemStack stack = ItemIcons.of(node.icon(), f.shape().route.icon());
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + s / 2f, y + s / 2f, 0f);
        pose.scale(scale, scale, 1f);
        g.renderItem(stack, -8, -8);                                      // items are drawn at a depth of about 150: everything over them is lifted to 200
        pose.popPose();
        pose.pushPose();
        pose.translate(0f, 0f, 200f);
        if (dim > 0f) g.fill(ix, iy, ix + is, iy + is, UiMath.withAlpha(0x05070E, dim));
        if (st.level() > 0) badge(g, font, x, y, s, v.zoom, st);
        pose.popPose();
        if (i == f.selected()) TreeSkin.selection(g, x, y, s, UiMath.withAlpha(0xFFFFFF, 0.55f + 0.35f * UiMath.pulse(f.now(), 30f, 0f)));
    }

    /** The level of a bought node: a "n/max" pill in the corner when the zoom is readable, a thin bar along the bottom when it is not. */
    private static void badge(GuiGraphics g, Font font, int x, int y, int s, float zoom, NodeState st) {
        boolean maxed = st.status() == NodeStatus.MAXED;
        if (zoom >= 0.75f) {
            String text = maxed ? "MAX" : st.level() + "/" + st.max();
            if (font.width(text) > s - 3) text = String.valueOf(st.level());
            int tw = font.width(text);
            int x1 = x + s - 1, x0 = x1 - tw - 3, y1 = y + s - 1, y0 = y1 - 9;
            g.fill(x0, y0, x1, y1, UiMath.withAlpha(0x03050A, 0.85f));
            g.drawString(font, text, x0 + 2, y0 + 1, maxed ? TreeSkin.gold() : 0xFFFFFFFF, false);
        } else {
            int bx0 = x + 2, bx1 = x + s - 2, by0 = y + s - 3, by1 = y + s - 1;
            g.fill(bx0, by0, bx1, by1, UiMath.withAlpha(0x03050A, 0.85f));
            g.fill(bx0, by0, bx0 + Math.round((bx1 - bx0) * Math.min(1f, st.level() / (float) Math.max(1, st.max()))), by1, maxed ? TreeSkin.gold() : 0xFFFFFFFF);
        }
    }

    /** The three view chips in the corner of the canvas (zoom out, zoom in, fit). Drawn after the nodes so they stay on top. */
    static void drawChips(GuiGraphics g, Font font, ScreenLayout lay, int mx, int my) {
        chip(g, font, lay.zoomOut, "-", mx, my);
        chip(g, font, lay.zoomIn, "+", mx, my);
        chip(g, font, lay.zoomFit, SkillTreeLang.str(SkillTreeLang.FIT), mx, my);
    }

    private static void chip(GuiGraphics g, Font font, Rect r, String label, int mx, int my) {
        boolean hover = r.contains(mx, my);
        TreeSkin.chip(g, r, false, hover, UiTheme.CYAN & 0xFFFFFF);
        g.drawCenteredString(font, label, r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2 + 1, hover ? UiTheme.WHITE : TreeSkin.text());
    }
}
