package com.newuniverse.nusmp.labyrinth.mega.rift;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the blueprint of the instanced hidden realm (the pocket of a Type A rift) as plain data, so what the builder places can be counted, flood-filled and checked without a game (1.101.0,
 * docs/labyrinth/pocket_fix.md; the geometry of 1.73.0 is kept block for block and one chamber is added). Five rooms: the ANTECHAMBER where the player arrives (the exit rift in its far niche), a
 * short CORRIDOR, the HALL (the arena of the secret boss, or the four coloured plates of the puzzle), the VAULT behind a barred door (three chests and a second exit rift) and, new, the LEVER CHAMBER in
 * the Hall's north wall: a small room that is not behind the bars, with the release lever on its far wall between two lit sea lanterns, a sign over it and a lantern on each side of it. Coordinates are
 * LOCAL: x east, z south, the cell's centre is (0, 0), and y is relative to the floor row (the first row of air is 0, the floor is -1, the ceiling of a room of height h is h). The fills are in the
 * order they are placed: every shell first, the doorways (air) last, so nothing is open before everything stands. The builder maps a {@link Mat} to a block; the doorways are air over wall, which
 * the Dungeon Guard refuses to anybody but the mod's own builders, so the builder runs them inside the guard's internal span.
 */
public final class RealmPlan {
    /** The roles of the blocks. */
    public enum Mat { AIR, WALL, FLOOR, CEIL, LIGHT, PILLAR, BARS }

    /** A filled box, corners inclusive, local x and z and relative y. */
    public record Fill(int x0, int y0, int z0, int x1, int y1, int z1, Mat mat) {
        public long volume() { return (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1); }
    }

    /** A room's interior box and what it is for. */
    public record Room(String id, int x0, int z0, int x1, int z1, int h) {
        public boolean contains(double x, double z) { return x >= x0 && x < x1 + 1 && z >= z0 && z < z1 + 1; }

        public int cx() { return (x0 + x1) / 2; }

        public int cz() { return (z0 + z1) / 2; }
    }

    /** A doorway: the two rooms it joins (by id) and the box that is air. */
    public record Door(String a, String b, int x0, int y0, int z0, int x1, int y1, int z1) {}

    /** The point features. */
    public enum Type { ARRIVAL, EXIT, EXIT_VAULT, BOSS_SPAWN, LEVER, SIGN, LANTERN, PLATE, ORDER, CHEST, VAULT_LAMP }

    /** A feature: its type, local position and a tag (a facing letter N/E/S/W for a lever or sign, the plate's index, the text key of a sign). */
    public record Feature(Type type, int x, int y, int z, String tag) {}

    public static final String ANTECHAMBER = "antechamber", CORRIDOR = "corridor", HALL = "hall", VAULT = "vault", LEVER_ROOM = "lever_chamber";

    private final List<Fill> ops = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<Feature> features = new ArrayList<>();
    private final List<int[]> bars = new ArrayList<>();
    private final boolean boss;
    private final long seed;
    private int shellOps;

    private RealmPlan(boolean boss, long seed) {
        this.boss = boss;
        this.seed = seed;
    }

    public boolean boss() { return boss; }

    public long seed() { return seed; }

    public List<Fill> ops() { return ops; }

    /** How many of the first fills are shells (rooms); the rest are doorways, bars, lights and pillars. */
    public int shellOps() { return shellOps; }

    public List<Room> rooms() { return rooms; }

    public List<Door> doors() { return doors; }

    public List<Feature> features() { return features; }

    /** The cells of the barred door, as {x, y, z} local. */
    public List<int[]> bars() { return bars; }

    public Room room(String id) {
        for (Room r : rooms) if (r.id().equals(id)) return r;
        return null;
    }

    public List<Feature> all(Type t) {
        List<Feature> out = new ArrayList<>();
        for (Feature f : features) if (f.type() == t) out.add(f);
        return out;
    }

    public Feature first(Type t) {
        for (Feature f : features) if (f.type() == t) return f;
        return null;
    }

    /** The room that holds a local point (interior), or null. */
    public Room roomAt(double x, double z) {
        for (Room r : rooms) if (r.contains(x, z)) return r;
        return null;
    }

    /** The extreme local coordinates the plan touches: {minX, minZ, maxX, maxZ}. */
    public int[] extent() {
        int[] e = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (Fill f : ops) {
            e[0] = Math.min(e[0], f.x0());
            e[1] = Math.min(e[1], f.z0());
            e[2] = Math.max(e[2], f.x1());
            e[3] = Math.max(e[3], f.z1());
        }
        return e;
    }

    public int maxY() {
        int m = 0;
        for (Fill f : ops) m = Math.max(m, f.y1());
        return m;
    }

    // ---------------------------------------------------------------- the maker
    /** The plan of a realm of a kind and seed. Pure and deterministic. */
    public static RealmPlan of(boolean boss, long seed) {
        RealmPlan p = new RealmPlan(boss, seed);
        p.room(ANTECHAMBER, -23, -4, -16, 4, 6);
        p.room(CORRIDOR, -14, -1, -13, 1, 4);
        p.room(HALL, -11, -11, 11, 11, 11);
        p.room(VAULT, 13, -3, 19, 3, 6);
        p.room(LEVER_ROOM, -3, -19, 3, -14, 6);
        p.shellOps = p.ops.size();
        // the doorways: antechamber -> corridor -> hall (one run of air through both walls and the gap), hall -> lever chamber (two walls thick)
        p.door(ANTECHAMBER, CORRIDOR, -15, 0, -1, -15, 3, 1);
        p.door(CORRIDOR, HALL, -12, 0, -1, -12, 3, 1);
        p.door(HALL, LEVER_ROOM, -1, 0, -12, 1, 3, -12);
        p.door(HALL, LEVER_ROOM, -1, 0, -13, 1, 3, -13);
        for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy < 3; dy++) {                  // the barred door between the Hall and the Vault (x = 12)
            p.ops.add(new Fill(12, dy, dz, 12, dy, dz, Mat.BARS));
            p.bars.add(new int[]{12, dy, dz});
        }
        for (int[] c : new int[][]{{-10, -10}, {10, -10}, {-10, 10}, {10, 10}}) p.ops.add(new Fill(c[0], 0, c[1], c[0], 9, c[1], Mat.PILLAR));
        for (int i = -9; i <= 9; i += 6) for (int j = -9; j <= 9; j += 6) p.ops.add(new Fill(i, 10, j, i, 10, j, Mat.LIGHT));
        p.ops.add(new Fill(-19, 5, 0, -19, 5, 0, Mat.LIGHT));
        p.ops.add(new Fill(16, 5, 0, 16, 5, 0, Mat.LIGHT));
        p.ops.add(new Fill(0, 5, -17, 0, 5, -17, Mat.LIGHT));                                // the lever chamber's ceiling lamp
        // the doorways come after the lights and pillars so nothing a pillar stands in is open; air over wall is the last of the shell work
        for (Door d : p.doors) p.ops.add(new Fill(d.x0(), d.y0(), d.z0(), d.x1(), d.y1(), d.z1(), Mat.AIR));
        // the lever: on the far (north) wall of the chamber, flanked by two lit sea lanterns set in the wall, a sign over it, a lantern each side on the floor
        p.features.add(new Feature(Type.ARRIVAL, -19, 0, 0, ""));
        p.features.add(new Feature(Type.EXIT, -22, 0, 0, ""));
        p.features.add(new Feature(Type.EXIT_VAULT, 15, 0, 3, ""));
        p.features.add(new Feature(Type.VAULT_LAMP, 16, 5, -3, ""));
        p.features.add(new Feature(Type.LEVER, 0, 1, -19, "S"));
        p.features.add(new Feature(Type.SIGN, 0, 2, -19, "S:lever"));
        p.features.add(new Feature(Type.LANTERN, -2, 0, -19, ""));
        p.features.add(new Feature(Type.LANTERN, 2, 0, -19, ""));
        p.ops.add(new Fill(-2, 1, -20, -2, 1, -20, Mat.LIGHT));
        p.ops.add(new Fill(2, 1, -20, 2, 1, -20, Mat.LIGHT));
        p.features.add(new Feature(Type.SIGN, 11, 2, 2, "W:vault"));                         // over the bars, on the Hall's east wall
        p.features.add(new Feature(Type.SIGN, 3, 2, -11, "S:chamber"));                      // beside the doorway to the lever chamber
        for (int i = 0; i < 3; i++) p.features.add(new Feature(Type.CHEST, 18, 0, -2 + i * 2, "W"));
        if (boss) {
            p.features.add(new Feature(Type.BOSS_SPAWN, 4, 0, 0, ""));
        } else {
            int[][] at = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
            for (int i = 0; i < 4; i++) p.features.add(new Feature(Type.PLATE, at[i][0], 0, at[i][1], String.valueOf(i)));
            for (int i = 0; i < 4; i++) p.features.add(new Feature(Type.LANTERN, at[i][0] + (at[i][0] < 0 ? -1 : 1), 0, at[i][1], ""));      // a lit lantern beside each plate
            p.features.add(new Feature(Type.SIGN, 11, 2, -2, "W:plates"));                  // what the plates are, beside the colours on the east wall
            int[] order = RiftRules.puzzleOrder(seed);
            for (int i = 0; i < 4; i++) p.features.add(new Feature(Type.ORDER, 11, 4, -3 + i * 2, String.valueOf(order[i])));
        }
        return p;
    }

    private void room(String id, int x0, int z0, int x1, int z1, int h) {
        rooms.add(new Room(id, x0, z0, x1, z1, h));
        ops.add(new Fill(x0 - 1, -1, z0 - 1, x1 + 1, -1, z1 + 1, Mat.FLOOR));
        ops.add(new Fill(x0 - 1, h, z0 - 1, x1 + 1, h, z1 + 1, Mat.CEIL));
        ops.add(new Fill(x0 - 1, 0, z0 - 1, x1 + 1, h - 1, z0 - 1, Mat.WALL));
        ops.add(new Fill(x0 - 1, 0, z1 + 1, x1 + 1, h - 1, z1 + 1, Mat.WALL));
        ops.add(new Fill(x0 - 1, 0, z0, x0 - 1, h - 1, z1, Mat.WALL));
        ops.add(new Fill(x1 + 1, 0, z0, x1 + 1, h - 1, z1, Mat.WALL));
        ops.add(new Fill(x0, 0, z0, x1, h - 1, z1, Mat.AIR));
    }

    private void door(String a, String b, int x0, int y0, int z0, int x1, int y1, int z1) { doors.add(new Door(a, b, x0, y0, z0, x1, y1, z1)); }

    // ---------------------------------------------------------------- queries (pure checks)
    /** The blocks of the plan as roles on a sparse grid: later fills overwrite earlier ones, as the builder does. Key = packed x, y, z (local). */
    public java.util.Map<Long, Mat> voxels() { return voxels(false); }

    /**
     * The same, as the world would hold them. With {@code guardRefuses} the Dungeon Guard's rule is applied to every fill: a change that takes a held block away (air over anything that is not air) is
     * refused and leaves the old block, which is what happened to every doorway of the realm in 1.73.0 to 1.100.0.
     */
    public java.util.Map<Long, Mat> voxels(boolean guardRefuses) {
        java.util.Map<Long, Mat> m = new java.util.HashMap<>();
        for (Fill f : ops)
            for (int y = f.y0(); y <= f.y1(); y++) for (int z = f.z0(); z <= f.z1(); z++) for (int x = f.x0(); x <= f.x1(); x++) {
                long k = key(x, y, z);
                Mat old = m.get(k);
                if (guardRefuses && f.mat() == Mat.AIR && old != null && old != Mat.AIR) continue;
                m.put(k, f.mat());
            }
        return m;
    }

    public static long key(int x, int y, int z) { return ((long) (x + 4096) << 40) | ((long) (y + 64) << 20) | (long) (z + 4096); }

    /** The {x, y, z} a {@link #key} was made from. */
    public static int[] unkey(long k) { return new int[]{(int) ((k >> 40) & 0xFFFFF) - 4096, (int) ((k >> 20) & 0xFFFFF) - 64, (int) (k & 0xFFFFF) - 4096}; }

    /** True when a cell is passable for a walking player in the voxels (air, or not set at all inside a room). */
    private static boolean open(java.util.Map<Long, Mat> v, int x, int y, int z) {
        Mat m = v.get(key(x, y, z));
        return m == Mat.AIR;
    }

    /** The ids of the rooms reachable on foot from the arrival (a flood fill over air), with the bars closed or, with {@code barsOpen}, lifted. */
    public java.util.Set<String> reachable(boolean barsOpen) { return reachable(barsOpen, false); }

    /** The same, with the Dungeon Guard refusing every carve ({@code guardRefuses}) or not. */
    public java.util.Set<String> reachable(boolean barsOpen, boolean guardRefuses) {
        java.util.Map<Long, Mat> v = voxels(guardRefuses);
        if (barsOpen) for (int[] b : bars) v.put(key(b[0], b[1], b[2]), Mat.AIR);
        Feature a = first(Type.ARRIVAL);
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        q.add(new int[]{a.x(), a.y(), a.z()});
        seen.add(key(a.x(), a.y(), a.z()));
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            Room r = roomAt(c[0] + 0.5, c[2] + 0.5);
            if (r != null) out.add(r.id());
            for (int[] d : dirs) {
                int nx = c[0] + d[0], ny = c[1] + d[1], nz = c[2] + d[2];
                if (!open(v, nx, ny, nz) || !seen.add(key(nx, ny, nz))) continue;
                q.add(new int[]{nx, ny, nz});
            }
        }
        return out;
    }
}
