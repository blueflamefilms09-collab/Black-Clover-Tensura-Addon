package com.newuniverse.nusmp.client.ui.gs;

/**
 * Where the alert crest sits: one of the four screen corners plus a free offset from it (inward, in GUI pixels). The player moves it on the
 * crest screen ({@code /nusmp_gui_crest}); the choice is stored in the client config only and never sent anywhere. Pure maths: the same
 * place always resolves to a rectangle that lies fully inside the screen, whatever the window or GUI scale is now.
 */
public final class GsCrestPos {
    private GsCrestPos() {}

    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public boolean right() { return this == TOP_RIGHT || this == BOTTOM_RIGHT; }

        public boolean bottom() { return this == BOTTOM_LEFT || this == BOTTOM_RIGHT; }

        /** Reads the config text (any case, '-' or ' ' for '_'); anything unknown is TOP_RIGHT. */
        public static Corner parse(String s) {
            if (s != null) {
                String t = s.trim().replace('-', '_').replace(' ', '_');
                for (Corner c : values()) if (c.name().equalsIgnoreCase(t)) return c;
            }
            return TOP_RIGHT;
        }
    }

    /** The gap between a screen edge and the crest when the offset is zero. */
    public static final int MARGIN = 6;
    /** Offsets in the config are held to this range (a corrupt file cannot send the crest to a huge number). */
    public static final int MAX_OFFSET = 4000;

    /** The stored choice. */
    public record Place(Corner corner, int dx, int dy) {
        public static final Place DEFAULT = new Place(Corner.TOP_RIGHT, 0, 0);
    }

    /** The top-left of the crest rectangle (w x h) for this place on a screen of sw x sh, always fully on screen. */
    public static int[] resolve(Place p, int sw, int sh, int w, int h) {
        int dx = clampInt(p.dx(), -MAX_OFFSET, MAX_OFFSET), dy = clampInt(p.dy(), -MAX_OFFSET, MAX_OFFSET);
        int x = p.corner().right() ? sw - w - MARGIN - dx : MARGIN + dx;
        int y = p.corner().bottom() ? sh - h - MARGIN - dy : MARGIN + dy;
        return new int[]{clampInt(x, 0, Math.max(0, sw - w)), clampInt(y, 0, Math.max(0, sh - h))};
    }

    /**
     * The place for a crest dropped with its top-left at (x, y): the corner of the quarter of the screen its centre is in, and the offset from
     * that corner. {@link #resolve} of the result gives back (x, y) (after clamping into the screen).
     */
    public static Place fromPosition(int x, int y, int sw, int sh, int w, int h) {
        int cx = clampInt(x, 0, Math.max(0, sw - w)), cy = clampInt(y, 0, Math.max(0, sh - h));
        boolean right = cx + w / 2 >= sw / 2, bottom = cy + h / 2 >= sh / 2;
        Corner c = bottom ? (right ? Corner.BOTTOM_RIGHT : Corner.BOTTOM_LEFT) : (right ? Corner.TOP_RIGHT : Corner.TOP_LEFT);
        int dx = right ? sw - w - MARGIN - cx : cx - MARGIN;
        int dy = bottom ? sh - h - MARGIN - cy : cy - MARGIN;
        return new Place(c, dx, dy);
    }

    /** The same place held to the config range. */
    public static Place sanitize(Place p) {
        return new Place(p.corner() == null ? Corner.TOP_RIGHT : p.corner(), clampInt(p.dx(), -MAX_OFFSET, MAX_OFFSET), clampInt(p.dy(), -MAX_OFFSET, MAX_OFFSET));
    }

    private static int clampInt(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }
}
