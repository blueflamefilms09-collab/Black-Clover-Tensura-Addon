package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the blueprint of one exclusive pocket as plain data, so what the builder places can be counted and checked without a game. A plan is made from a {@link PocketLayout} alone (the
 * block palette comes later, when the builder turns a material ROLE into a block), as an ordered list of box fills (rooms first, with the entry room before all others so the delver has a
 * floor under their feet at once; then the corridors; the doorways last, so nothing is open until everything stands), the point features (the exit rift, the arrival, the boss and key spawn,
 * trap tiles, relic pedestals, chests, spawn spots), the gravity zones (the inverted rooms and their corridors, the weightless rooms) and the rooms' boxes. Every coordinate is LOCAL to the
 * pocket's cell (its centre is 0, 0), except y, which is absolute. Floors are one block thick at {@code Y0 - 1}; a room's air is {@code Y0 .. Y0 + height - 1} and its ceiling slab is at
 * {@code Y0 + height}. A fill is never bigger than a shell (floor, ceiling, wall), never the air of a room: the cell is void already, so a dungeon of 20 rooms is about 30,000 blocks, not 300,000.
 */
public final class PocketPlan {
    /** The y of the first block of air above a room's floor. */
    public static final int Y0 = 40;

    /** The roles of the blocks; the builder maps each to a block of the pocket's palette. */
    public enum Mat { AIR, WALL, FLOOR, CEIL, PILLAR, ACCENT, LIGHT, GLASS, LIQUID, SEAL }

    /** A filled box, corners inclusive, in local x and z and absolute y. */
    public record Fill(int x0, int y0, int z0, int x1, int y1, int z1, Mat mat) {
        public Fill {
            int a, b;
            if (x0 > x1) { a = x0; x0 = x1; x1 = a; }
            if (y0 > y1) { a = y0; y0 = y1; y1 = a; }
            if (z0 > z1) { b = z0; z0 = z1; z1 = b; }
        }

        public long volume() { return (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1); }
    }

    public enum Type { EXIT_RIFT, ARRIVAL, BOSS_SPAWN, KEY_SPAWN, TRAP, PEDESTAL_PRIMARY, PEDESTAL_SECONDARY, CHEST_MINOR, CHEST_TREASURE, SEAL_BLOCK, AMBIENT }

    /** A point feature: its type, local position, and a tag (the element of a trap, the room id of a chest). */
    public record Feature(Type type, int x, int y, int z, String tag) {}

    /** A box in which the gravity rule of {@code kind} (INVERTED or NEGATED) holds; {@code room} is the room that owns it (its corridors included). */
    public record Zone(PocketLayout.Kind kind, int room, int x0, int y0, int z0, int x1, int y1, int z1) {
        public boolean contains(double x, double y, double z) { return x >= x0 && x < x1 + 1 && y >= y0 && y < y1 + 1 && z >= z0 && z < z1 + 1; }
    }

    /** One room's box: the interior {@code x0..x1, z0..z1} and {@code y0..y1}, and the spots mobs may be put down at. */
    public record RoomBox(int id, PocketLayout.Kind kind, int x0, int z0, int x1, int z1, int y0, int y1, List<int[]> spots) {
        public boolean contains(double x, double y, double z) { return x >= x0 && x < x1 + 1 && z >= z0 && z < z1 + 1 && y >= y0 - 1 && y < y1 + 2; }
    }

    private final PocketLayout layout;
    private final List<Fill> ops = new ArrayList<>(), doorways = new ArrayList<>();
    private final List<Feature> features = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<RoomBox> boxes = new ArrayList<>();
    private int stageA;
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, maxY = Y0;

    private PocketPlan(PocketLayout layout) { this.layout = layout; }

    public PocketLayout layout() { return layout; }

    /** The fills in the order to place them (shells, then the doorways carved last). */
    public List<Fill> ops() { return ops; }

    /** How many of the first fills make the entry room whole (shell, lights, pillars, the exit niche): the builder places those at once, before the delver arrives, and the rest over the next ticks. */
    public int stageA() { return stageA; }

    public List<Feature> features() { return features; }

    public List<Zone> zones() { return zones; }

    public List<RoomBox> boxes() { return boxes; }

    public RoomBox box(int roomId) { return boxes.get(roomId); }

    public int minX() { return minX; }

    public int maxX() { return maxX; }

    public int minZ() { return minZ; }

    public int maxZ() { return maxZ; }

    public int maxY() { return maxY; }

    /** The first feature of a type (or null). */
    public Feature first(Type type) {
        for (Feature f : features) if (f.type() == type) return f;
        return null;
    }

    public List<Feature> all(Type type) {
        List<Feature> out = new ArrayList<>();
        for (Feature f : features) if (f.type() == type) out.add(f);
        return out;
    }

    /** The total number of blocks the fills place (doorways included; a block placed twice counts twice). */
    public long volume() {
        long v = 0;
        for (Fill f : ops) v += f.volume();
        return v;
    }

    // ---------------------------------------------------------------- the maker
    /** Makes the plan of a layout. Pure and deterministic: the same layout gives the same plan. */
    public static PocketPlan of(PocketLayout l) {
        PocketPlan p = new PocketPlan(l);
        int g = l.grid();
        // the entry room first, so the delver has a floor at once; then the rest in id order
        List<PocketLayout.Room> order = new ArrayList<>();
        order.add(l.entry());
        for (PocketLayout.Room r : l.rooms()) if (r.id() != l.entryId()) order.add(r);
        p.room(l.entry(), g);
        p.decorateRoom(l.entry(), g);                                                   // the entry room is whole before anything else: lights, pillars, the exit niche
        p.stageA = p.ops.size();
        for (PocketLayout.Room r : order) if (r.id() != l.entryId()) p.room(r, g);
        for (PocketLayout.Room r : l.rooms()) p.boxes.add(p.makeBox(r, g));
        for (PocketLayout.Door d : l.doors()) p.corridor(l.room(d.a()), l.room(d.b()), d.sealed());
        for (PocketLayout.Door d : l.doors()) p.carve(l.room(d.a()), l.room(d.b()), d.sealed());
        p.ops.addAll(p.doorways);
        p.decorate(g);
        return p;
    }

    private void add(int x0, int y0, int z0, int x1, int y1, int z1, Mat mat) {
        Fill f = new Fill(x0, y0, z0, x1, y1, z1, mat);
        ops.add(f);
        minX = Math.min(minX, Math.min(f.x0(), f.x1()));
        maxX = Math.max(maxX, Math.max(f.x0(), f.x1()));
        minZ = Math.min(minZ, Math.min(f.z0(), f.z1()));
        maxZ = Math.max(maxZ, Math.max(f.z0(), f.z1()));
        maxY = Math.max(maxY, f.y1());
    }

    private void door(int x0, int y0, int z0, int x1, int y1, int z1) { doorways.add(new Fill(x0, y0, z0, x1, y1, z1, Mat.AIR)); }

    private void feature(Type t, int x, int y, int z, String tag) { features.add(new Feature(t, x, y, z, tag)); }

    // ---------------------------------------------------------------- rooms
    private RoomBox makeBox(PocketLayout.Room r, int g) {
        int cx = r.cx(g), cz = r.cz(g), h = r.half();
        List<int[]> spots = new ArrayList<>();
        Random rnd = new Random(RiftRules.mix(layout.seed(), 0x5B07L + r.id()));
        int want = switch (r.kind()) {
            case COMBAT -> 14;
            case TRAP, RUIN -> 8;
            case KEY -> 6;
            case BOSS -> 6;
            case INVERTED, NEGATED -> 8;
            default -> 0;
        };
        for (int tries = 0; spots.size() < want && tries < want * 12; tries++) {
            int dx = rnd.nextInt(2 * h - 3) - (h - 2), dz = rnd.nextInt(2 * h - 3) - (h - 2);
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && r.kind() != PocketLayout.Kind.BOSS) continue;      // the middle of a room stays free for the party
            if (Math.abs(dx) >= h - 2 && Math.abs(dz) >= h - 2 && r.kind() == PocketLayout.Kind.COMBAT) continue;   // not inside a pillar
            if (r.kind() == PocketLayout.Kind.TRAP && trapTile(dx, dz, h)) continue;
            spots.add(new int[]{cx + dx, cz + dz});
        }
        return new RoomBox(r.id(), r.kind(), cx - h, cz - h, cx + h, cz + h, Y0, Y0 + r.height() - 1, spots);
    }

    /** Is the local offset (dx, dz) on a trap tile of a trap room of half width h? */
    static boolean trapTile(int dx, int dz, int h) {
        for (int[] t : trapOffsets(h)) if (Math.abs(dx - t[0]) <= 1 && Math.abs(dz - t[1]) <= 1) return true;
        return false;
    }

    /** The trap tile centres of a trap room, as offsets from its centre: the four diagonal ones, kept off the lines the doors are on. */
    static int[][] trapOffsets(int h) {
        int a = Math.max(3, h - 2);
        return new int[][]{{-a, -a}, {a, -a}, {-a, a}, {a, a}};
    }

    private void room(PocketLayout.Room r, int g) {
        int cx = r.cx(g), cz = r.cz(g), h = r.half(), H = r.height(), w = h + 1;
        boolean river = r.kind() == PocketLayout.Kind.RIVER;
        int thick = river ? 3 : 1;
        add(cx - w, Y0 - 1, cz - w, cx + w, Y0 - 1, cz + w, Mat.FLOOR);
        add(cx - w, Y0 + H, cz - w, cx + w, Y0 + H + thick - 1, cz + w, Mat.CEIL);
        add(cx - w, Y0, cz - w, cx + w, Y0 + H - 1, cz - w, Mat.WALL);
        add(cx - w, Y0, cz + w, cx + w, Y0 + H - 1, cz + w, Mat.WALL);
        add(cx - w, Y0, cz - h, cx - w, Y0 + H - 1, cz + h, Mat.WALL);
        add(cx + w, Y0, cz - h, cx + w, Y0 + H - 1, cz + h, Mat.WALL);
    }

    // ---------------------------------------------------------------- corridors and doorways
    /** The geometry of the corridor between two rooms: the axis (true = along x), the first and last tile along it, the lateral centre. */
    private record Run(boolean alongX, int first, int last, int lateral, PocketLayout.Room low, PocketLayout.Room high) {
        int len() { return last - first + 1; }
    }

    private Run run(PocketLayout.Room a, PocketLayout.Room b) {
        int g = layout.grid();
        boolean alongX = a.gz() == b.gz();
        PocketLayout.Room low = alongX ? (a.gx() < b.gx() ? a : b) : (a.gz() < b.gz() ? a : b);
        PocketLayout.Room high = low == a ? b : a;
        int lowPlane = (alongX ? low.cx(g) : low.cz(g)) + low.half() + 1, highPlane = (alongX ? high.cx(g) : high.cz(g)) - high.half() - 1;
        return new Run(alongX, lowPlane + 1, highPlane - 1, alongX ? low.cz(g) : low.cx(g), low, high);
    }

    private void box(Run r, int t0, int t1, int l0, int l1, int y0, int y1, Mat mat) {
        if (r.alongX()) add(t0, y0, r.lateral() + l0, t1, y1, r.lateral() + l1, mat);
        else add(r.lateral() + l0, y0, t0, r.lateral() + l1, y1, t1, mat);
    }

    private void corridor(PocketLayout.Room a, PocketLayout.Room b, boolean sealed) {
        Run r = run(a, b);
        int wd = sealed ? PocketLayout.SEAL_WIDTH : PocketLayout.DOOR_WIDTH, half = wd / 2;
        int hc = sealed ? PocketLayout.SEAL_HEIGHT : PocketLayout.tall(a, b) ? Math.max(a.kind() == PocketLayout.Kind.INVERTED ? a.height() : 0, b.kind() == PocketLayout.Kind.INVERTED ? b.height() : 0) : PocketLayout.DOOR_HEIGHT;
        int f = r.first(), l = r.last();
        box(r, f, l, -half - 1, half + 1, Y0 - 1, Y0 - 1, Mat.FLOOR);
        box(r, f, l, -half - 1, half + 1, Y0 + hc, Y0 + hc, Mat.CEIL);
        box(r, f, l, -half - 1, -half - 1, Y0, Y0 + hc - 1, Mat.WALL);
        box(r, f, l, half + 1, half + 1, Y0, Y0 + hc - 1, Mat.WALL);
        if (hc > Math.min(r.low().height(), r.high().height())) {                         // a tall corridor beside a shorter room: its end is closed up to the corridor's own height
            box(r, f - 1, f - 1, -half - 1, half + 1, Y0, Y0 + hc - 1, Mat.WALL);
            box(r, l + 1, l + 1, -half - 1, half + 1, Y0, Y0 + hc - 1, Mat.WALL);
        }
        if (PocketLayout.tall(a, b) && !sealed) gateFrame(r, a, b, half, hc);
        if (sealed) {                                                                       // the seal: two layers of condensed mana in the lair's doorway, a frame round it
            PocketLayout.Room boss = a.kind() == PocketLayout.Kind.BOSS ? a : b;
            boolean bossLow = boss == r.low();
            int plane = bossLow ? f - 1 : l + 1, inner = bossLow ? f : l;
            box(r, plane, plane, -half, half, Y0, Y0 + PocketLayout.SEAL_HEIGHT - 1, Mat.SEAL);
            box(r, inner, inner, -half, half, Y0, Y0 + PocketLayout.SEAL_HEIGHT - 1, Mat.SEAL);
            for (int lat = -half; lat <= half; lat++)
                for (int y = Y0; y < Y0 + PocketLayout.SEAL_HEIGHT; y++) {
                    feature(Type.SEAL_BLOCK, r.alongX() ? plane : r.lateral() + lat, y, r.alongX() ? r.lateral() + lat : plane, "");
                    feature(Type.SEAL_BLOCK, r.alongX() ? inner : r.lateral() + lat, y, r.alongX() ? r.lateral() + lat : inner, "");
                }
            box(r, plane, plane, -half - 1, -half - 1, Y0, Y0 + PocketLayout.SEAL_HEIGHT, Mat.ACCENT);
            box(r, plane, plane, half + 1, half + 1, Y0, Y0 + PocketLayout.SEAL_HEIGHT, Mat.ACCENT);
            box(r, plane, plane, -half - 1, half + 1, Y0 + PocketLayout.SEAL_HEIGHT, Y0 + PocketLayout.SEAL_HEIGHT, Mat.ACCENT);
        } else if (hc == PocketLayout.DOOR_HEIGHT && (r.len() % 2 == 1) && ((a.id() + b.id()) % 3 == 0)) {
            // a lantern over the middle of some corridors
            int mid = (f + l) / 2;
            box(r, mid, mid, 0, 0, Y0 + hc, Y0 + hc, Mat.LIGHT);
        }
    }

    /** The frame that marks where the gravity of an inverted room begins in a tall corridor, one tile inside the landing tile at the far end. */
    private void gateFrame(Run r, PocketLayout.Room a, PocketLayout.Room b, int half, int hc) {
        PocketLayout.Room inv = a.kind() == PocketLayout.Kind.INVERTED ? a : b;
        boolean invLow = inv == r.low();
        int landing = invLow ? r.last() : r.first(), gate = invLow ? r.last() - 1 : r.first() + 1;
        box(r, gate, gate, -half, half, Y0 - 1, Y0 - 1, Mat.ACCENT);
        box(r, gate, gate, -half, half, Y0 + hc, Y0 + hc, Mat.ACCENT);
        box(r, gate, gate, -half - 1, -half - 1, Y0, Y0 + hc - 1, Mat.ACCENT);
        box(r, gate, gate, half + 1, half + 1, Y0, Y0 + hc - 1, Mat.ACCENT);
        box(r, gate, gate, -half - 1, -half - 1, Y0 + 2, Y0 + 2, Mat.LIGHT);
        box(r, gate, gate, half + 1, half + 1, Y0 + 2, Y0 + 2, Mat.LIGHT);
        box(r, gate, gate, -half - 1, -half - 1, Y0 + hc - 3, Y0 + hc - 3, Mat.LIGHT);
        box(r, gate, gate, half + 1, half + 1, Y0 + hc - 3, Y0 + hc - 3, Mat.LIGHT);
        feature(Type.AMBIENT, r.alongX() ? landing : r.lateral(), Y0, r.alongX() ? r.lateral() : landing, "landing");
    }

    private void carve(PocketLayout.Room a, PocketLayout.Room b, boolean sealed) {
        Run r = run(a, b);
        int wd = sealed ? PocketLayout.SEAL_WIDTH : PocketLayout.DOOR_WIDTH, half = wd / 2;
        for (PocketLayout.Room side : new PocketLayout.Room[]{r.low(), r.high()}) {
            boolean low = side == r.low();
            int plane = low ? r.first() - 1 : r.last() + 1;
            int dh = sealed ? PocketLayout.SEAL_HEIGHT : PocketLayout.DOOR_HEIGHT;
            int y0 = side.kind() == PocketLayout.Kind.INVERTED && !sealed ? Y0 + side.height() - dh : Y0;
            if (r.alongX()) door(plane, y0, r.lateral() - half, plane, y0 + dh - 1, r.lateral() + half);
            else door(r.lateral() - half, y0, plane, r.lateral() + half, y0 + dh - 1, plane);
        }
    }

    // ---------------------------------------------------------------- what stands in the rooms
    private void decorate(int g) {
        for (PocketLayout.Room r : layout.rooms()) if (r.id() != layout.entryId()) decorateRoom(r, g);
    }

    private void decorateRoom(PocketLayout.Room r, int g) {
        {
            int cx = r.cx(g), cz = r.cz(g), h = r.half(), H = r.height();
            Random rnd = new Random(RiftRules.mix(layout.seed(), 0xDEC0L + r.id()));
            switch (r.kind()) {
                case ENTRY -> {
                    feature(Type.EXIT_RIFT, cx - h + 2, Y0, cz - h + 2, "");
                    feature(Type.ARRIVAL, cx, Y0, cz, "");
                    add(cx - h + 1, Y0 - 1, cz - h + 1, cx - h + 3, Y0 - 1, cz - h + 3, Mat.ACCENT);
                    lights(r, g, 4);
                    for (int[] c : new int[][]{{h - 1, h - 1}, {-h + 1, h - 1}, {h - 1, -h + 1}}) add(cx + c[0], Y0, cz + c[1], cx + c[0], Y0 + H - 2, cz + c[1], Mat.PILLAR);
                }
                case COMBAT -> {
                    for (int sx = -1; sx <= 1; sx += 2)
                        for (int sz = -1; sz <= 1; sz += 2) add(cx + sx * (h - 2), Y0, cz + sz * (h - 2), cx + sx * (h - 2), Y0 + H - 1, cz + sz * (h - 2), Mat.PILLAR);
                    add(cx - 1, Y0 - 1, cz - h + 1, cx + 1, Y0 - 1, cz + h - 1, Mat.ACCENT);
                    add(cx - h + 1, Y0 - 1, cz - 1, cx + h - 1, Y0 - 1, cz + 1, Mat.ACCENT);
                    lights(r, g, 3);
                }
                case TRAP -> {
                    for (int[] t : trapOffsets(h)) {
                        add(cx + t[0] - 1, Y0 - 1, cz + t[1] - 1, cx + t[0] + 1, Y0 - 1, cz + t[1] + 1, Mat.ACCENT);
                        feature(Type.TRAP, cx + t[0], Y0, cz + t[1], r.element());
                    }
                    int ca = Math.max(3, h - 2);
                    feature(Type.CHEST_MINOR, cx + ca + 1, Y0, cz + ca + 1, "trapped");           // loot on a rune: the chest is guarded by the tile it stands on
                    lights(r, g, 3);
                }
                case INVERTED -> {
                    // the floor of the room is the ceiling for those who walk on its ceiling: a river laid in it under glass, a garden of accent blocks, lights both sides
                    add(cx - h + 1, Y0 - 1, cz - 1, cx + h - 1, Y0 - 1, cz + 1, Mat.GLASS);
                    add(cx - h + 1, Y0 - 2, cz - 1, cx + h - 1, Y0 - 2, cz + 1, Mat.LIQUID);
                    add(cx - h + 1, Y0 - 3, cz - 1, cx + h - 1, Y0 - 3, cz + 1, Mat.WALL);
                    add(cx - h + 1, Y0 - 2, cz - 2, cx + h - 1, Y0 - 2, cz - 2, Mat.WALL);
                    add(cx - h + 1, Y0 - 2, cz + 2, cx + h - 1, Y0 - 2, cz + 2, Mat.WALL);
                    add(cx - h, Y0 - 2, cz - 1, cx - h, Y0 - 2, cz + 1, Mat.WALL);
                    add(cx + h, Y0 - 2, cz - 1, cx + h, Y0 - 2, cz + 1, Mat.WALL);
                    for (int i = -h + 2; i <= h - 2; i += 4) {
                        add(cx + i, Y0 - 1, cz + 3, cx + i, Y0 - 1, cz + 3, Mat.LIGHT);
                        add(cx + i, Y0 - 1, cz - 3, cx + i, Y0 - 1, cz - 3, Mat.LIGHT);
                        add(cx + i, Y0 + H, cz + 3, cx + i, Y0 + H, cz + 3, Mat.LIGHT);
                        add(cx + i, Y0 + H, cz - 3, cx + i, Y0 + H, cz - 3, Mat.LIGHT);
                    }
                    for (int k = 0; k < 10; k++) {
                        int dx = rnd.nextInt(2 * h - 1) - (h - 1), dz = rnd.nextInt(2 * h - 1) - (h - 1);
                        add(cx + dx, Y0 - 1, cz + dz, cx + dx, Y0 - 1, cz + dz, Mat.ACCENT);
                        add(cx + dx, Y0 + H, cz + dz, cx + dx, Y0 + H, cz + dz, Mat.ACCENT);
                    }
                    int cy0 = Y0 + H - 4;
                    zones.add(new Zone(PocketLayout.Kind.INVERTED, r.id(), cx - h, Y0, cz - h, cx + h, Y0 + H - 1, cz + h));
                    for (int nb : layout.neighbours(r.id(), true)) corridorZone(r, layout.room(nb), g);
                    feature(Type.AMBIENT, cx, cy0, cz, "inverted");
                }
                case NEGATED -> {
                    int platforms = 6;
                    for (int k = 0; k < platforms; k++) {
                        int dx = rnd.nextInt(2 * h - 5) - (h - 3), dz = rnd.nextInt(2 * h - 5) - (h - 3), y = Y0 + 3 + (k * (H - 6)) / platforms + rnd.nextInt(2);
                        add(cx + dx - 1, y, cz + dz - 1, cx + dx + 1, y, cz + dz + 1, Mat.ACCENT);
                        add(cx + dx, y, cz + dz, cx + dx, y, cz + dz, Mat.LIGHT);
                    }
                    add(cx - 2, Y0 + H - 3, cz - 2, cx + 2, Y0 + H - 3, cz + 2, Mat.ACCENT);
                    feature(Type.CHEST_MINOR, cx, Y0 + H - 2, cz, "high");
                    add(cx, Y0, cz, cx, Y0 + H - 4, cz, Mat.GLASS);
                    add(cx, Y0 + 2, cz, cx, Y0 + 2, cz, Mat.LIGHT);
                    lights(r, g, 4);
                    zones.add(new Zone(PocketLayout.Kind.NEGATED, r.id(), cx - h, Y0, cz - h, cx + h, Y0 + H - 1, cz + h));
                }
                case RIVER -> {
                    int y = Y0 + H;                                                       // the ceiling slab is three thick: glass underneath, water over it, stone round and over
                    add(cx - h, y, cz - 1, cx + h, y, cz + 1, Mat.GLASS);
                    add(cx - h, y + 1, cz - 1, cx + h, y + 1, cz + 1, Mat.LIQUID);
                    add(cx - h, y + 1, cz - 2, cx + h, y + 1, cz - 2, Mat.CEIL);
                    add(cx - h, y + 1, cz + 2, cx + h, y + 1, cz + 2, Mat.CEIL);
                    for (int sx = -1; sx <= 1; sx += 2)
                        for (int sz = -1; sz <= 1; sz += 2) add(cx + sx * (h - 2), Y0, cz + sz * (h - 2), cx + sx * (h - 2), Y0 + H - 1, cz + sz * (h - 2), Mat.PILLAR);
                    add(cx - h + 1, Y0 - 1, cz - 3, cx + h - 1, Y0 - 1, cz - 3, Mat.ACCENT);
                    add(cx - h + 1, Y0 - 1, cz + 3, cx + h - 1, Y0 - 1, cz + 3, Mat.ACCENT);
                    lights(r, g, 4);
                }
                case RUIN -> {
                    for (int k = 0; k < 6; k++) {
                        int dx = rnd.nextInt(2 * h - 3) - (h - 2), dz = rnd.nextInt(2 * h - 3) - (h - 2);
                        if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) continue;
                        add(cx + dx, Y0, cz + dz, cx + dx, Y0 + rnd.nextInt(H - 3), cz + dz, Mat.PILLAR);
                    }
                    feature(Type.CHEST_MINOR, cx + h - 2, Y0, cz - h + 2, "ruin");
                    lights(r, g, 2);
                }
                case KEY -> {
                    add(cx - 2, Y0 - 1, cz - 2, cx + 2, Y0 - 1, cz + 2, Mat.ACCENT);
                    feature(Type.KEY_SPAWN, cx, Y0, cz, "");
                    for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) add(cx + sx * (h - 1), Y0, cz + sz * (h - 1), cx + sx * (h - 1), Y0 + H - 2, cz + sz * (h - 1), Mat.PILLAR);
                    lights(r, g, 3);
                }
                case BOSS -> {
                    feature(Type.BOSS_SPAWN, cx, Y0, cz, "");
                    for (int k = 0; k < 8; k++) {
                        double a = Math.PI * 2 * k / 8 + Math.PI / 8;
                        int px = (int) Math.round(Math.cos(a) * (h - 2)), pz = (int) Math.round(Math.sin(a) * (h - 2));
                        add(cx + px, Y0, cz + pz, cx + px, Y0 + H - 3, cz + pz, Mat.PILLAR);
                        add(cx + px, Y0 + H - 2, cz + pz, cx + px, Y0 + H - 2, cz + pz, Mat.LIGHT);
                    }
                    add(cx - 4, Y0 - 1, cz - 4, cx + 4, Y0 - 1, cz + 4, Mat.ACCENT);
                    add(cx - 8, Y0 - 1, cz, cx + 8, Y0 - 1, cz, Mat.ACCENT);
                    add(cx, Y0 - 1, cz - 8, cx, Y0 - 1, cz + 8, Mat.ACCENT);
                    lights(r, g, 5);
                }
                case TREASURY -> {
                    add(cx - 2, Y0 - 1, cz - 2, cx + 2, Y0 - 1, cz + 2, Mat.ACCENT);
                    int[] away = awayFromBoss(r);                                           // unit step from the door toward the far wall
                    int px = -away[1], pz = away[0];                                         // the lateral direction
                    feature(Type.PEDESTAL_PRIMARY, cx, Y0, cz, "");
                    feature(Type.PEDESTAL_SECONDARY, cx + away[0] * 2 + px * 3, Y0, cz + away[1] * 2 + pz * 3, "");
                    for (int i = -1; i <= 1; i++) feature(Type.CHEST_TREASURE, cx + away[0] * (h - 1) + px * i * 3, Y0, cz + away[1] * (h - 1) + pz * i * 3, "t" + (i + 1));
                    for (int sa = -1; sa <= 1; sa += 2) {
                        int ox = away[0] * (h - 1) + px * sa * (h - 1), oz = away[1] * (h - 1) + pz * sa * (h - 1);
                        add(cx + ox, Y0, cz + oz, cx + ox, Y0 + H - 1, cz + oz, Mat.PILLAR);
                    }
                    lights(r, g, 3);
                }
                case ALCOVE -> {
                    feature(Type.CHEST_MINOR, cx, Y0, cz - 1, "alcove");
                    lights(r, g, 1);
                }
            }
        }
    }

    /** The unit step from the treasury's door (the side that faces the lair) toward its far wall, as {dx, dz}. */
    private int[] awayFromBoss(PocketLayout.Room treasury) {
        PocketLayout.Room boss = layout.boss();
        int dx = Integer.signum(boss.gx() - treasury.gx()), dz = Integer.signum(boss.gz() - treasury.gz());
        return new int[]{-dx, -dz};
    }

    /** The gravity zone a door of an inverted room gives: the corridor tiles from the room's wall up to, but not including, the landing tile at the neighbour's end. */
    private void corridorZone(PocketLayout.Room inv, PocketLayout.Room other, int g) {
        PocketLayout.Door d = layout.doorBetween(inv.id(), other.id());
        if (d == null || d.sealed()) return;
        Run r = run(inv, other);
        boolean invLow = inv == r.low();
        int a = invLow ? r.first() : r.first() + 1, b = invLow ? r.last() - 1 : r.last();
        int half = PocketLayout.DOOR_WIDTH / 2, hc = Math.max(inv.height(), 1);
        if (b < a) return;
        if (r.alongX()) zones.add(new Zone(PocketLayout.Kind.INVERTED, inv.id(), a, Y0, r.lateral() - half, b, Y0 + hc - 1, r.lateral() + half));
        else zones.add(new Zone(PocketLayout.Kind.INVERTED, inv.id(), r.lateral() - half, Y0, a, r.lateral() + half, Y0 + hc - 1, b));
    }

    /** Lights set into the ceiling slab of a room, {@code n} by {@code n} on a loose grid. */
    private void lights(PocketLayout.Room r, int g, int n) {
        int cx = r.cx(g), cz = r.cz(g), h = r.half(), H = r.height();
        int step = Math.max(2, (2 * h) / (n + 1));
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= n; j++) {
                int x = cx - h + i * step, z = cz - h + j * step;
                if (x > cx + h - 1 || z > cz + h - 1) continue;
                add(x, Y0 + H, z, x, Y0 + H, z, Mat.LIGHT);
            }
    }

    // ---------------------------------------------------------------- queries
    /** The gravity zone kind at a local point: INVERTED, NEGATED, or null. Inverted zones win where two overlap. */
    public PocketLayout.Kind gravityAt(double x, double y, double z) {
        PocketLayout.Kind found = null;
        for (Zone zone : zones) {
            if (!zone.contains(x, y, z)) continue;
            if (zone.kind() == PocketLayout.Kind.INVERTED) return PocketLayout.Kind.INVERTED;
            found = zone.kind();
        }
        return found;
    }
}
