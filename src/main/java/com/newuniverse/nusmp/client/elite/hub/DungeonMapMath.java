package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.client.ui.scale.GuiScaleMath;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

/**
 * Why: every number of the Dungeon Map screen lives here as plain maths (no Minecraft classes), so a test proves them without a game: the layout of the title bar, the map frame and the legend,
 * the camera (centre + zoom in pixels per block) with its pan and zoom-about-the-pointer rules and limits, the colour of each room kind, and the backdrop rectangle that still covers the whole
 * window when the player's GUI size option has scaled the screen down (the screen is drawn inside a scaled pose by {@code GuiScale}, which also divides the mouse position, the drag distance and
 * the scroll position by that scale before they reach the screen, so the screen's own maths is always in its own unscaled coordinates).
 */
public final class DungeonMapMath {
    private DungeonMapMath() {}

    /** The title bar's height in GUI pixels (floor name, counts, Close button). */
    public static final int TITLE_H = 34;
    /** The height of one row of the bottom bar (legend rows, then the hint row). */
    public static final int ROW_H = 12;
    /** The padding between the bar's edge and its first row. */
    public static final int BAR_PAD = 3;
    public static final double MAX_ZOOM = 16.0, ABS_MIN_ZOOM = 0.05, DEFAULT_MIN_ZOOM = 0.2;
    /** How far (in blocks) past the outer room the camera centre may travel. */
    public static final double PAN_PAD = 8.0;
    /** The empty margin (pixels) kept round the floor when it is fitted. */
    public static final int FIT_MARGIN = 20;
    /** Zoom of one wheel notch. */
    public static final double WHEEL_STEP = 1.15;

    /** The rectangle of the map itself (the clip), between the title bar and the bottom bar. */
    public record Frame(int left, int top, int right, int bottom) {
        public int width() { return right - left; }

        public int height() { return bottom - top; }

        public double midX() { return (left + right) / 2.0; }

        public double midZ() { return (top + bottom) / 2.0; }

        public boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }

    /** The camera: the world point (block x / z) in the middle of the frame, and pixels per block. */
    public record Cam(double cx, double cz, double zoom) {}

    // ---- layout ----------------------------------------------------------------------------------------------------------------------------------

    /** The height of the bottom bar for this many legend rows: the legend rows, one hint row and the padding. */
    public static int bottomBarHeight(int legendRows) {
        return BAR_PAD * 2 + Math.max(1, legendRows) * ROW_H + ROW_H;
    }

    /** The map frame of a window: below the title bar, above the bottom bar; never smaller than 16 x 16. */
    public static Frame frame(int width, int height, int legendRows) {
        int top = TITLE_H, bottom = height - bottomBarHeight(legendRows);
        if (bottom < top + 16) bottom = top + 16;
        return new Frame(0, top, Math.max(16, width), bottom);
    }

    /**
     * Where each legend entry goes: {x, row} per entry, left to right from startX, wrapping to the next row when an entry would run past maxX. An entry wider than the space still gets a row of its own.
     * @param widths the pixel width of each entry (swatch + gap + text)
     */
    public static int[][] legendLayout(int[] widths, int startX, int maxX, int gap) {
        int[][] out = new int[widths.length][2];
        int x = startX, row = 0;
        for (int i = 0; i < widths.length; i++) {
            if (x > startX && x + widths[i] > maxX) { x = startX; row++; }
            out[i][0] = x;
            out[i][1] = row;
            x += widths[i] + gap;
        }
        return out;
    }

    /** The number of legend rows a layout from {@link #legendLayout} uses (at least 1). */
    public static int legendRows(int[][] layout) {
        int rows = 1;
        for (int[] e : layout) rows = Math.max(rows, e[1] + 1);
        return rows;
    }

    // ---- camera ----------------------------------------------------------------------------------------------------------------------------------

    /** The zoom that fits the floor's bounds {minX, minZ, maxX, maxZ} into the frame with the margin; a floor smaller than 16 blocks counts as 16. */
    public static double fitZoom(int[] bounds, Frame f) {
        double w = Math.max(16, bounds[2] - bounds[0] + 1), h = Math.max(16, bounds[3] - bounds[1] + 1);
        double z = Math.min((f.width() - FIT_MARGIN * 2) / w, (f.height() - FIT_MARGIN * 2) / h);
        if (Double.isNaN(z) || z <= 0) z = ABS_MIN_ZOOM;
        return Math.max(ABS_MIN_ZOOM, Math.min(MAX_ZOOM, z));
    }

    /** The smallest zoom allowed: the default floor, or the fitted zoom when the floor is so large that it needs less. */
    public static double minZoom(double fit) {
        return Math.max(ABS_MIN_ZOOM, Math.min(DEFAULT_MIN_ZOOM, fit));
    }

    public static double clampZoom(double zoom, double fit) {
        if (Double.isNaN(zoom)) return Math.max(minZoom(fit), Math.min(MAX_ZOOM, fit));
        return Math.max(minZoom(fit), Math.min(MAX_ZOOM, zoom));
    }

    /** The view that shows the whole floor, centred. */
    public static Cam fit(int[] bounds, Frame f) {
        return new Cam((bounds[0] + bounds[2] + 1) / 2.0, (bounds[1] + bounds[3] + 1) / 2.0, fitZoom(bounds, f));
    }

    /** The camera centre held to the floor's bounds plus {@link #PAN_PAD}, the zoom held to its limits. */
    public static Cam clamp(Cam c, int[] bounds, Frame f) {
        double fit = fitZoom(bounds, f);
        double z = clampZoom(c.zoom(), fit);
        double cx = Math.max(bounds[0] - PAN_PAD, Math.min(bounds[2] + 1 + PAN_PAD, Double.isNaN(c.cx()) ? 0 : c.cx()));
        double cz = Math.max(bounds[1] - PAN_PAD, Math.min(bounds[3] + 1 + PAN_PAD, Double.isNaN(c.cz()) ? 0 : c.cz()));
        return new Cam(cx, cz, z);
    }

    /** Dragging the map by a pixel distance moves the camera the other way. */
    public static Cam pan(Cam c, double dxPx, double dyPx, int[] bounds, Frame f) {
        return clamp(new Cam(c.cx() - dxPx / c.zoom(), c.cz() - dyPx / c.zoom(), c.zoom()), bounds, f);
    }

    /** Wheel zoom: the world point under the pointer stays under the pointer. */
    public static Cam zoomAbout(Cam c, double mx, double my, double scrollY, int[] bounds, Frame f) {
        double fit = fitZoom(bounds, f);
        double wx = worldX(c, f, mx), wz = worldZ(c, f, my);
        double z = clampZoom(c.zoom() * Math.pow(WHEEL_STEP, scrollY), fit);
        return clamp(new Cam(wx - (mx - f.midX()) / z, wz - (my - f.midZ()) / z, z), bounds, f);
    }

    /** Zoom by a factor about the middle of the frame (the + and - keys). */
    public static Cam zoomCentre(Cam c, double factor, int[] bounds, Frame f) {
        return clamp(new Cam(c.cx(), c.cz(), c.zoom() * factor), bounds, f);
    }

    /** "Centre on me": the camera on the player, zoomed in to at least minWanted pixels per block (a huge floor fitted tiny would otherwise show nothing of the player's surroundings). */
    public static Cam centreOn(Cam c, double worldX, double worldZ, double minWanted, int[] bounds, Frame f) {
        return clamp(new Cam(worldX, worldZ, Math.max(c.zoom(), minWanted)), bounds, f);
    }

    public static double screenX(Cam c, Frame f, double worldX) { return f.midX() + (worldX - c.cx()) * c.zoom(); }

    public static double screenZ(Cam c, Frame f, double worldZ) { return f.midZ() + (worldZ - c.cz()) * c.zoom(); }

    public static double worldX(Cam c, Frame f, double screenX) { return c.cx() + (screenX - f.midX()) / c.zoom(); }

    public static double worldZ(Cam c, Frame f, double screenZ) { return c.cz() + (screenZ - f.midZ()) / c.zoom(); }

    /** True when a screen box (x0, z0)-(x1, z1) touches the frame, so rooms far outside it are not drawn at all. */
    public static boolean visible(Frame f, int x0, int z0, int x1, int z1) {
        return x1 > f.left() && x0 < f.right() && z1 > f.top() && z0 < f.bottom();
    }

    // ---- GUI size option ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The rectangle {left, top, right, bottom}, in the screen's own coordinates, that covers the WHOLE window when the screen is drawn scaled by {@code scale} round the window's middle (the GUI size
     * option): a screen scaled to 85% draws its own 0..width at only 85% of the window, so a backdrop of 0..width left the HUD showing round the map. The bounds are rounded outwards.
     */
    public static int[] backdrop(int width, int height, double scale) {
        double s = scale > 0 && !Double.isNaN(scale) && !Double.isInfinite(scale) ? scale : 1.0;
        double pw = width / 2.0, ph = height / 2.0;
        return new int[] {(int) Math.floor(GuiScaleMath.toLocal(0, pw, s)) - 1, (int) Math.floor(GuiScaleMath.toLocal(0, ph, s)) - 1,
                (int) Math.ceil(GuiScaleMath.toLocal(width, pw, s)) + 1, (int) Math.ceil(GuiScaleMath.toLocal(height, ph, s)) + 1};
    }

    // ---- colours -----------------------------------------------------------------------------------------------------------------------------------

    /** The colour of a room kind (ARGB); a kind appended to the enum later reads as a hall. */
    public static int colour(int kind) {
        RoomKind[] kinds = RoomKind.values();
        RoomKind k = kind >= 0 && kind < kinds.length ? kinds[kind] : RoomKind.OTHER;
        return switch (k) {
            case LANDING -> 0xFF5CC878;
            case SAFE, STAIRWELL -> 0xFF4CA8D8;
            case CORRIDOR, JUNCTION, LINK, ONWARD -> 0xFF8C8A94;
            case HALL, OTHER, CAP -> 0xFFC8A870;
            case SET_PIECE -> 0xFF9A6CD0;
            case CAVERN -> 0xFF8A6A48;
            case TRIAL -> 0xFFE8903C;
            case TRAP -> 0xFFD84A4A;
            case PUZZLE -> 0xFFE8D84C;
            case TREASURE -> 0xFFF0C030;
            case ARENA -> 0xFFB02050;
            case ANTECHAMBER, VAULT -> 0xFFD070B0;
            default -> 0xFFC8A870;
        };
    }

    /** The unexplored look of a colour: two fifths of it over a dark base, opaque. */
    public static int dim(int argb) {
        int r = (argb >> 16 & 255) * 2 / 5, gr = (argb >> 8 & 255) * 2 / 5, b = (argb & 255) * 2 / 5;
        return 0xFF000000 | (r + 12) << 16 | (gr + 12) << 8 | (b + 14);
    }
}
