package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the nested panel has to sit next to Tensura's Ability Selection screen, and the one thing that cannot be known here is where Tensura puts its own widgets (its classes are API stubs, nothing
 * can be run). So no pixel position of Tensura's is guessed anywhere. The only inputs are values the screen can be ASKED at runtime: its frame (the static {@code guiLeft/guiTop/guiRight/guiBottom} the
 * template screen computes in {@code init}), and, optionally, where its list ends (the scroll-bar getters {@code getScrollBarX/Y/TotalSpace} of the {@code IScrollBar} it implements), plus the window
 * size. From those this class chooses a rectangle, and every choice is checked, not assumed: the panel must fit the window, must not overlap Tensura's frame when it is docked beside it, and must lie inside
 * the frame when it is placed "inside"; a mode that does not fit is skipped, and if nothing fits (or the frame values are missing or nonsense, which is what a failed reflection gives) the panel is pinned to
 * the right edge of the window, which is always valid. The configured word ({@code panelPlacement}) only chooses which mode is tried first. The admin switch gets its own small rectangle by the same
 * rule: next to the frame, never over the panel or the frame, else in a corner of the window. Pure (no Minecraft type), so the invariants are tested over a grid of window and frame sizes.
 */
public final class MenuPlacement {
    private MenuPlacement() {}

    public static final int MARGIN = 4, GAP = 4, MIN_W = 150, MAX_W = 320, MIN_BELOW_H = 90, MIN_H = 150;

    public enum Mode { RIGHT, LEFT, INSIDE, BELOW, EDGE }

    /** The screen's frame in GUI pixels; right and bottom are exclusive edges. */
    public record Frame(int left, int top, int right, int bottom) {
        public int w() { return right - left; }

        public int h() { return bottom - top; }

        /** True when the values describe a real box inside the window (a failed reflection or an unset static gives zeros or garbage). */
        public boolean valid(int sw, int sh) { return w() >= 32 && h() >= 32 && left >= 0 && top >= 0 && right <= sw && bottom <= sh; }

        public Rect rect() { return new Rect(left, top, w(), h()); }
    }

    /** Where Tensura's list ends: the x just right of its scroll bar, and the top and height of the scroll track. */
    public record Inner(int listRight, int top, int height) {}

    public record Result(Rect panel, Mode mode) {}

    public static Result place(int sw, int sh, Frame frame, Inner inner, String preference, int wantedWidth) {
        int w0 = Math.max(MIN_W, Math.min(MAX_W, wantedWidth));
        if (frame == null || !frame.valid(sw, sh)) return edge(sw, sh, w0);
        List<Mode> order = new ArrayList<>(List.of(Mode.RIGHT, Mode.LEFT, Mode.INSIDE, Mode.BELOW));
        Mode first = parse(preference);
        if (first != null) {
            order.remove(first);
            order.add(0, first);
        }
        for (Mode m : order) {
            Rect r = switch (m) {
                case RIGHT -> right(sw, sh, frame, w0);
                case LEFT -> left(sw, sh, frame, w0);
                case INSIDE -> inside(sw, sh, frame, inner, w0);
                case BELOW -> below(sw, sh, frame, w0);
                case EDGE -> null;
            };
            if (r != null) return new Result(r, m);
        }
        return edge(sw, sh, w0);
    }

    /** The mode a config word names, or null for {@code auto} and anything unknown. */
    public static Mode parse(String word) {
        if (word == null) return null;
        return switch (word) {
            case "right" -> Mode.RIGHT;
            case "left" -> Mode.LEFT;
            case "inside" -> Mode.INSIDE;
            case "below" -> Mode.BELOW;
            default -> null;
        };
    }

    private static Rect right(int sw, int sh, Frame f, int w0) {
        int x = f.right() + GAP, avail = sw - MARGIN - x;
        if (avail < MIN_W) return null;
        return column(sh, f, x, Math.min(w0, avail));
    }

    private static Rect left(int sw, int sh, Frame f, int w0) {
        int avail = f.left() - GAP - MARGIN;
        if (avail < MIN_W) return null;
        int w = Math.min(w0, avail);
        return column(sh, f, f.left() - GAP - w, w);
    }

    /** A tall panel level with the frame (at least {@link #MIN_H} tall, kept inside the window). */
    private static Rect column(int sh, Frame f, int x, int w) {
        int h = Math.max(MIN_H, Math.min(f.h(), sh - 2 * MARGIN));
        int y = Math.max(MARGIN, Math.min(f.top(), sh - MARGIN - h));
        return new Rect(x, y, w, h);
    }

    private static Rect inside(int sw, int sh, Frame f, Inner in, int w0) {
        if (in == null) return null;
        int x0 = in.listRight() + GAP, x1 = f.right() - GAP, w = Math.min(x1 - x0, w0);
        if (w < MIN_W || in.height() < 100) return null;
        Rect r = new Rect(x0, in.top(), w, in.height());
        return r.isInside(sw, sh, 0) && f.rect().inset(-1).contains(r.x(), r.y()) && f.rect().inset(-1).contains(r.right() - 1, r.bottom() - 1) ? r : null;
    }

    private static Rect below(int sw, int sh, Frame f, int w0) {
        int y = f.bottom() + GAP, availH = sh - MARGIN - y;
        if (availH < MIN_BELOW_H) return null;
        int x = Math.max(MARGIN, f.left()), w = Math.min(Math.max(f.w(), w0), sw - MARGIN - x);
        if (w < MIN_W) return null;
        return new Rect(x, y, w, Math.min(availH, MIN_H));
    }

    private static Result edge(int sw, int sh, int w0) {
        int w = Math.max(40, Math.min(w0, sw - 2 * MARGIN));
        return new Result(new Rect(Math.max(0, sw - MARGIN - w), MARGIN, w, Math.max(60, sh - 2 * MARGIN)), Mode.EDGE);
    }

    /**
     * Where the small ADMIN INSPECT switch goes: above the frame, else below it, else in a corner of the window; the first spot that lies inside the window and overlaps neither the frame nor the panel.
     */
    public static Rect toggleRect(int sw, int sh, Frame frame, Rect panel, int w, int h) {
        List<Rect> candidates = new ArrayList<>();
        boolean f = frame != null && frame.valid(sw, sh);
        if (f) {
            candidates.add(new Rect(frame.left(), frame.top() - h - 2, w, h));
            candidates.add(new Rect(frame.left(), frame.bottom() + 2, w, h));
        }
        candidates.add(new Rect(MARGIN, MARGIN, w, h));
        candidates.add(new Rect(MARGIN, sh - h - MARGIN, w, h));
        for (Rect c : candidates) {
            if (!c.isInside(sw, sh, 0)) continue;
            if (panel != null && c.intersects(panel)) continue;
            if (f && c.intersects(frame.rect())) continue;
            return c;
        }
        return new Rect(MARGIN, MARGIN, w, h);
    }
}
