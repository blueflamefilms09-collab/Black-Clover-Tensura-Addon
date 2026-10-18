package com.newuniverse.nusmp.compat.maps;

/**
 * Why: the geometry behind the client helper {@code MapHudAvoid}: where a typical minimap sits (a square in the top-right corner, in GUI pixels) and how far a
 * HUD box that defaults to that corner has to move to stay clear of it. PURE (plain ints), so it is unit-tested; the helper adds only the mod-list check and the config.
 */
public final class MinimapRect {
    private MinimapRect() {}

    /** Gap between the screen edge and the minimap, and between the minimap and what is moved, in GUI pixels. */
    public static final int MARGIN = 4;

    /** A rectangle in GUI pixels. */
    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }

        public int bottom() { return y + h; }

        public boolean overlaps(Rect o) { return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }
    }

    /** The minimap square of {@code size} pixels in the top-right corner of a {@code guiWidth} x {@code guiHeight} screen; the size is clamped to what fits. */
    public static Rect topRight(int guiWidth, int guiHeight, int size) {
        int s = Math.max(0, Math.min(size, Math.min(guiWidth, guiHeight)));
        return new Rect(Math.max(0, guiWidth - s - MARGIN), MARGIN, s, s);
    }

    /**
     * The x for a HUD box of {@code w} x {@code h} whose top-left is at ({@code x}, {@code y}): unchanged when it does not touch the minimap, otherwise moved left so it
     * ends {@link #MARGIN} pixels before the minimap (never below 0). The y is not moved.
     */
    public static int avoidX(int x, int y, int w, int h, Rect minimap) {
        if (minimap == null || minimap.w() <= 0 || !new Rect(x, y, w, h).overlaps(minimap)) return x;
        return Math.max(0, minimap.x() - MARGIN - w);
    }

    /**
     * The y for a box that hangs from the top edge at x, w, h and cannot go left: moved down below the minimap when it overlaps. Offered for layers that cannot shift sideways.
     */
    public static int avoidY(int x, int y, int w, int h, Rect minimap) {
        if (minimap == null || minimap.w() <= 0 || !new Rect(x, y, w, h).overlaps(minimap)) return y;
        return minimap.bottom() + MARGIN;
    }
}
