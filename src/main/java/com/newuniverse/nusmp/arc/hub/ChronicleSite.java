package com.newuniverse.nusmp.arc.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Chronicle (the lectern that lists the story, and the monument round it) and the Promenade (the paved ring on which the friendly characters of the arcs
 * stand, one post each) are new ground in the Sanctuary Hub, and every part of the mod that touches them (the builder that lays them, the guard that keeps them
 * whole, the NPC pass that stands characters on the posts, the lectern's click, the tests, the previews) must agree on one set of numbers. This pure class is that
 * set: where the ring and the rotunda are (outside the plaza's own box, inside the flattened disc of the Hub's terrain, off the two roads that run east and west to
 * the Spire and the Chasm), what every column of them is made of ({@link #column}), where the posts are and which way each faces, and what is protected.
 * Everything is in WORLD coordinates of the Sanctuary dimension; the paving is at {@link #PAVE_Y} (the plaza's own paving level), so a player stands at
 * {@code PAVE_Y + 1}. Nothing is a custom block: every part is a vanilla block, named as text so a test can check it exists.
 *
 * <p>The numbers, and why: the plaza's box is 33 x 33 (its corner is 22.6 blocks from the centre), the terrain is flat to a radius of 40, so the Promenade is a
 * band from radius 23.5 to 29.5, which clears the box and stays on flat ground; the east-west roads are left alone (no paving, no post within three blocks of them);
 * the rotunda sits on the band to the south, a few blocks beyond the Notice Pavilion that a newcomer faces on arrival, with an obelisk tall enough to be seen over
 * the pavilion's roof. The posts are the slots of a zig-zag along the band (every six degrees, alternately on an inner and an outer radius, 3.7 blocks from their
 * neighbours), nearest the Chronicle first.
 */
public final class ChronicleSite {
    private ChronicleSite() {}

    /** World y of the paving (the plaza's paving row): the same level as the plaza, so the ring and the plaza read as one place. */
    public static final int PAVE_Y = HubLayout.SURFACE_Y - 1;

    // ---- the Promenade
    public static final double RING_INNER = 23.5, RING_OUTER = 29.5, POST_INNER_R = 25.0, POST_OUTER_R = 27.6;
    /** The east-west roads: nothing is built within this many blocks of their line beyond the plaza's reach. */
    public static final int ROAD_HALF_WIDTH = 3, ROAD_FROM_X = 18;
    public static final double POST_STEP_DEG = 6.0;

    // ---- the Chronicle: a rotunda on the south of the band, its centre, its radius, its height
    public static final int ROT_X = 0, ROT_Z = 26;
    public static final double ROT_R = 5.5;
    public static final int COLUMN_HEIGHT = 6, OBELISK_TOP_DY = 15;
    /** The lectern's column. */
    public static final int LECTERN_X = ROT_X, LECTERN_Z = ROT_Z;
    /** How far (blocks) a column of the site may lie from the centre of the Hub: the whole site is inside the flat disc of the terrain generator. */
    public static final int MAX_RADIUS = 38;

    /** A place an NPC stands: the standing cell (feet) and the way it faces (degrees, as the game's yaw: 0 south, 90 west, 180 north, 270 east). */
    public record Post(int index, int x, int y, int z, float yaw) {}

    /** One block of a column: its height above the paving ({@code dy} 0 is the paving itself) and its role. */
    public record Cell(int dy, SitePart part) {}

    private static final List<Post> POSTS = buildPosts();
    private static final int[][] COLUMNS = {{4, 0}, {3, 3}, {-3, 3}, {-4, 0}, {3, -3}, {-3, -3}};
    private static final Map<SitePart, String> PALETTE = buildPalette();

    // ---------------------------------------------------------------- posts
    private static List<Post> buildPosts() {
        List<Post> all = new ArrayList<>();
        int slots = (int) Math.round(360.0 / POST_STEP_DEG);
        for (int k = 0; k < slots; k++) {
            double deg = k * POST_STEP_DEG, r = k % 2 == 0 ? POST_INNER_R : POST_OUTER_R, a = Math.toRadians(deg);
            int x = (int) Math.round(r * Math.cos(a)), z = (int) Math.round(r * Math.sin(a));
            if (onRoad(x, z) || nearRotunda(x, z, 9.0)) continue;
            float yaw = (float) Math.toDegrees(Math.atan2(x, -z));
            all.add(new Post(0, x, PAVE_Y + 1, z, yaw));
        }
        // nearest the Chronicle first: by the angle from the direction of the rotunda (south, 90 degrees), the inner radius before the outer one on a tie
        List<Post> sorted = new ArrayList<>(all);
        sorted.sort((p, q) -> {
            int c = Double.compare(angleFromSouth(p), angleFromSouth(q));
            return c != 0 ? c : Integer.compare(Math.abs(p.x()) + Math.abs(p.z()), Math.abs(q.x()) + Math.abs(q.z()));
        });
        List<Post> out = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Post p = sorted.get(i);
            out.add(new Post(i, p.x(), p.y(), p.z(), p.yaw()));
        }
        return Collections.unmodifiableList(out);
    }

    private static double angleFromSouth(Post p) {
        double deg = Math.toDegrees(Math.atan2(p.z(), p.x()));
        double d = Math.abs(deg - 90.0);
        return d > 180.0 ? 360.0 - d : d;
    }

    /** Every post, nearest the Chronicle first; the index of a post is its place in this list and never changes while the constants do not. */
    public static List<Post> posts() { return POSTS; }

    /** The post with this index, or null. */
    public static Post post(int index) { return index >= 0 && index < POSTS.size() ? POSTS.get(index) : null; }

    // ---------------------------------------------------------------- where
    /** Is this column on one of the two roads that leave the plaza to the east and the west? */
    public static boolean onRoad(int x, int z) { return Math.abs(z) <= ROAD_HALF_WIDTH && Math.abs(x) >= ROAD_FROM_X; }

    private static boolean nearRotunda(int x, int z, double within) {
        double dx = x - ROT_X, dz = z - ROT_Z;
        return dx * dx + dz * dz <= within * within;
    }

    /** Is the column on the rotunda's floor (a circle of {@link #ROT_R} round its centre)? */
    public static boolean inRotunda(int x, int z) { return nearRotunda(x, z, ROT_R); }

    /** Is the column on the paved band of the Promenade (and not on a road or in the rotunda)? */
    public static boolean inRing(int x, int z) {
        double r = Math.sqrt((double) x * x + (double) z * z);
        return r >= RING_INNER && r <= RING_OUTER && !onRoad(x, z) && !inRotunda(x, z);
    }

    /** Is the cell in the volume the guard keeps whole: the ring (paving to six blocks over it), the rotunda (to the top of the obelisk and a little more)? */
    public static boolean protectedCell(int x, int y, int z) {
        if (y < PAVE_Y - 2) return false;
        if (nearRotunda(x, z, ROT_R + ROT_SLACK)) return y <= PAVE_Y + OBELISK_TOP_DY + 3;
        double r = Math.sqrt((double) x * x + (double) z * z);
        return r >= RING_INNER - RING_SLACK && r <= RING_OUTER + RING_SLACK && !onRoad(x, z) && y <= PAVE_Y + 6;
    }

    /** How far past the rotunda's floor and the paved band the guarded volume reaches (a column of slack, so nothing is built hard against the edge). */
    public static final double RING_SLACK = 1.0, ROT_SLACK = 1.5;

    /** Is the column within {@code margin} blocks of the rotunda's guarded footprint (its floor plus {@link #ROT_SLACK})? A circle test; the roads are not excluded: this is a distance, not a build rule. */
    public static boolean nearSiteRotunda(int x, int z, int margin) { return nearRotunda(x, z, ROT_R + ROT_SLACK + margin); }

    /** Is the column within {@code margin} blocks of the paved band's guarded footprint (the band plus {@link #RING_SLACK})? A distance, roads not excluded. */
    public static boolean nearRing(int x, int z, int margin) {
        double r = Math.sqrt((double) x * x + (double) z * z);
        return r >= RING_INNER - RING_SLACK - margin && r <= RING_OUTER + RING_SLACK + margin;
    }

    /** Is the column within {@code margin} blocks of the guarded footprint (ring or rotunda)? What the builder clears (margin 0) and what a bucket or a flame is refused near. */
    public static boolean nearSite(int x, int z, int margin) { return nearSiteRotunda(x, z, margin) || nearRing(x, z, margin); }

    /**
     * Is the cell within {@code margin} blocks of the volume the guard keeps whole? A bucket and a flame do their damage after the click, a few blocks away,
     * so they are asked with a margin ({@code margin 0} is the guarded footprint of {@link #protectedCell} with the roads included).
     */
    public static boolean near(int x, int y, int z, int margin) {
        return y >= PAVE_Y - 2 - margin && y <= PAVE_Y + OBELISK_TOP_DY + 3 + margin && nearSite(x, z, margin);
    }

    /** The area the builder looks at: x and z from {@code -MAX_RADIUS} to {@code MAX_RADIUS}. */
    public static boolean inBuildArea(int x, int z) { return Math.abs(x) <= MAX_RADIUS && Math.abs(z) <= MAX_RADIUS; }

    // ---------------------------------------------------------------- the columns
    /** The cells of one column of the site, bottom to top, or an empty list when the column is not part of it. The paving is dy 0. */
    public static List<Cell> column(int x, int z) {
        List<Cell> out = new ArrayList<>(4);
        if (inRotunda(x, z)) {
            rotunda(x, z, out);
            return out;
        }
        if (!inRing(x, z)) return out;
        double r = Math.sqrt((double) x * x + (double) z * z);
        if (isPost(x, z)) {
            out.add(new Cell(0, SitePart.POST_TILE));
            return out;
        }
        out.add(new Cell(0, r < RING_INNER + 1.0 || r > RING_OUTER - 1.0 ? SitePart.PAVE_EDGE : ((x + z) & 1) == 0 ? SitePart.PAVE_A : SitePart.PAVE_B));
        if (isLampPost(x, z)) {
            out.add(new Cell(1, SitePart.LAMP_WALL));
            out.add(new Cell(2, SitePart.LAMP_WALL));
            out.add(new Cell(3, SitePart.LAMP));
        }
        return out;
    }

    private static boolean isPost(int x, int z) {
        for (Post p : POSTS) if (p.x() == x && p.z() == z) return true;
        return false;
    }

    /** The lamp posts stand on the outer edge of the band, between the NPC posts (offset three degrees, every twelve). */
    private static final int[][] LAMP_POSTS = buildLampPosts();

    private static int[][] buildLampPosts() {
        List<int[]> out = new ArrayList<>();
        for (int k = 0; k < 30; k++) {
            double a = Math.toRadians(k * 12.0 + 3.0);
            int x = (int) Math.round(29.0 * Math.cos(a)), z = (int) Math.round(29.0 * Math.sin(a));
            if (!onRoad(x, z) && !nearRotunda(x, z, ROT_R + 1.0)) out.add(new int[] {x, z});
        }
        return out.toArray(new int[0][]);
    }

    private static boolean isLampPost(int x, int z) {
        for (int[] p : LAMP_POSTS) if (p[0] == x && p[1] == z) return true;
        return false;
    }

    /** The lamp posts' columns, for the tests. */
    public static List<int[]> lampPosts() { return List.of(LAMP_POSTS); }

    private static void rotunda(int x, int z, List<Cell> out) {
        int ox = x - ROT_X, oz = z - ROT_Z;
        double d = Math.sqrt((double) ox * ox + (double) oz * oz);
        SitePart floor = d <= 2.5 ? SitePart.ROT_CENTER : Math.abs(d - 3.5) <= 0.5 ? SitePart.ROT_INLAY : d >= 4.9 ? SitePart.PAVE_EDGE : SitePart.ROT_FLOOR;
        out.add(new Cell(0, floor));
        if (ox == 0 && oz == 0) {                                                                     // the lectern, facing the plaza
            out.add(new Cell(1, SitePart.LECTERN));
            return;
        }
        for (int[] c : COLUMNS) {
            if (c[0] != ox || c[1] != oz) continue;
            out.add(new Cell(1, SitePart.COLUMN_BAND));
            for (int dy = 2; dy <= COLUMN_HEIGHT - 1; dy++) out.add(new Cell(dy, SitePart.COLUMN));
            out.add(new Cell(COLUMN_HEIGHT, SitePart.COLUMN_BAND));
            out.add(new Cell(COLUMN_HEIGHT + 1, SitePart.COLUMN_CAP));
            out.add(new Cell(COLUMN_HEIGHT + 2, SitePart.COLUMN_LAMP));
            return;
        }
        if (Math.abs(ox) <= 1 && oz >= 2 && oz <= 4) {                                                // the obelisk behind the lectern: a 3 x 3 base and a tall shaft
            out.add(new Cell(1, SitePart.OBELISK_BASE));
            out.add(new Cell(2, SitePart.OBELISK_BASE));
            if (ox == 0 && oz == 3) {
                for (int dy = 3; dy <= OBELISK_TOP_DY - 2; dy++) out.add(new Cell(dy, dy == 5 || dy == 9 ? SitePart.OBELISK_BAND : SitePart.OBELISK));
                out.add(new Cell(OBELISK_TOP_DY - 1, SitePart.OBELISK_LAMP));
                out.add(new Cell(OBELISK_TOP_DY, SitePart.OBELISK_TOP));
            }
            return;
        }
        if (d >= 3.8 && d <= 4.9 && !(oz < 0 && Math.abs(ox) <= 1)) out.add(new Cell(COLUMN_HEIGHT + 1, SitePart.COLUMN_CAP));   // the beam that joins the capitals, open to the north: the way in
    }

    // ---------------------------------------------------------------- the blocks
    /** The block (as text, see {@code BlockSpec}) that plays each part. */
    public static Map<SitePart, String> palette() { return PALETTE; }

    private static Map<SitePart, String> buildPalette() {
        Map<SitePart, String> m = new EnumMap<>(SitePart.class);
        m.put(SitePart.PAVE_A, "stone_bricks");
        m.put(SitePart.PAVE_B, "polished_andesite");
        m.put(SitePart.PAVE_EDGE, "smooth_stone");
        m.put(SitePart.POST_TILE, "chiseled_stone_bricks");
        m.put(SitePart.LAMP_WALL, "stone_brick_wall");
        m.put(SitePart.LAMP, "lantern[hanging=false]");
        m.put(SitePart.ROT_FLOOR, "smooth_quartz");
        m.put(SitePart.ROT_CENTER, "chiseled_quartz_block");
        m.put(SitePart.ROT_INLAY, "gold_block");
        m.put(SitePart.COLUMN, "quartz_pillar[axis=y]");
        m.put(SitePart.COLUMN_BAND, "chiseled_quartz_block");
        m.put(SitePart.COLUMN_CAP, "chiseled_quartz_block");
        m.put(SitePart.COLUMN_LAMP, "lantern[hanging=false]");
        m.put(SitePart.LECTERN, "lectern[facing=north,has_book=true]");
        m.put(SitePart.OBELISK_BASE, "quartz_bricks");
        m.put(SitePart.OBELISK, "quartz_pillar[axis=y]");
        m.put(SitePart.OBELISK_BAND, "chiseled_quartz_block");
        m.put(SitePart.OBELISK_LAMP, "sea_lantern");
        m.put(SitePart.OBELISK_TOP, "gold_block");
        return Collections.unmodifiableMap(m);
    }
}
