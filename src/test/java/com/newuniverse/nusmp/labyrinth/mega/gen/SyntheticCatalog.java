package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MarkerSpec;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the real templates are being drawn by other builders while this one is written, and an assembler cannot be tuned on nothing. This is a
 * stand-in for them that obeys the same connector contract (9-block modules, 48 high, doors in the first row above the floor on the outer wall layer at
 * module centres, door caps 3 x 6 x 1..8 with their jigsaw at (1,1,0), the boss chain Landing -> arena -> antechamber -> vault + Way Onward, the tunnel
 * segments, the pool names of the design document) with shapes and pool weights that look like the reference tabletop map: many small rooms, a few
 * big ones, corridors with side doors, one set piece. It is a {@link PoolSource} for the unit tests and can write itself in the {@code catalog.json}
 * format so the simulator and the catalog reader are exercised on the same data path as the real catalog. It is test code: nothing in the game reads it.
 */
final class SyntheticCatalog implements PoolSource {
    private final Map<String, Shape> shapes = new LinkedHashMap<>();
    private final Map<String, PoolDef> pools = new LinkedHashMap<>();

    @Override
    public PoolDef pool(String id) { return pools.get(id); }

    @Override
    public Shape shape(String id) { return shapes.get(id); }

    Map<String, Shape> shapes() { return shapes; }

    Map<String, PoolDef> pools() { return pools; }

    void add(Shape s) { shapes.put(s.id, s); }

    void pool(String id, int emptyWeight, String fallback, Object... idAndWeight) {
        List<PoolEntry> e = new ArrayList<>();
        for (int i = 0; i < idAndWeight.length; i += 2) e.add(new PoolEntry((String) idAndWeight[i], (Integer) idAndWeight[i + 1], "nusmp:mega_interior"));
        pools.put(id, new PoolDef(id, e, emptyWeight, fallback));
    }

    // ---------------------------------------------------------------- shape builder

    /** A template under construction: sockets and markers in the template frame. */
    static final class B {
        final String id;
        final RoomKind kind;
        final int sx, sz;
        final List<Socket> sockets = new ArrayList<>();
        final List<ShapeMarker> markers = new ArrayList<>();
        int max;

        B(String id, RoomKind kind, int mx, int mz) {
            this.id = id;
            this.kind = kind;
            this.sx = mx * MegaGrid.MODULE;
            this.sz = mz * MegaGrid.MODULE;
        }

        /** A door in the first row above the floor at {@code offset} along the face {@code side} ('n','e','s','w'). */
        B door(char side, int offset, String name, String target, String pool, String finalState) {
            Vec cell;
            Facing f;
            switch (side) {
                case 'n' -> { cell = new Vec(offset, MegaGrid.DOOR_ROW, 0); f = Facing.NORTH; }
                case 's' -> { cell = new Vec(offset, MegaGrid.DOOR_ROW, sz - 1); f = Facing.SOUTH; }
                case 'w' -> { cell = new Vec(0, MegaGrid.DOOR_ROW, offset); f = Facing.WEST; }
                default -> { cell = new Vec(sx - 1, MegaGrid.DOOR_ROW, offset); f = Facing.EAST; }
            }
            sockets.add(new Socket(cell, f, name, target, pool, 0, 0, finalState));
            return this;
        }

        B door(char side, int offset) { return door(side, offset, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_SIDE, "minecraft:air"); }

        /** An ordinary door at every module centre of the given sides. */
        B all(String sides) {
            for (char c : sides.toCharArray()) {
                int edge = (c == 'n' || c == 's') ? sx : sz;
                for (int k = 0; k < edge / MegaGrid.MODULE; k++) door(c, k * MegaGrid.MODULE + MegaGrid.CENTER);
            }
            return this;
        }

        B all() { return all("nesw"); }

        B marker(MarkerKind k, int x, int y, int z, String... kv) {
            Map<String, String> args = new LinkedHashMap<>();
            for (int i = 0; i + 1 < kv.length; i += 2) args.put(kv[i], kv[i + 1]);
            markers.add(new ShapeMarker(new MarkerSpec(k, args), new Vec(x, y, z)));
            return this;
        }

        B max(int n) { max = n; return this; }

        Shape build() { return new Shape(id, sx, MegaGrid.HEIGHT, sz, kind, sockets, markers, max); }
    }

    B shape(String id, RoomKind kind, int mx, int mz) { return new B(id, kind, mx, mz); }

    void put(B b) { add(b.build()); }

    /** A door cap: 3 wide, 6 high, {@code depth} deep, its jigsaw at (1,1,0) facing north, a sealed end. */
    void cap(String id, int depth) {
        add(new Shape(id, 3, 6, depth, RoomKind.CAP, List.of(new Socket(new Vec(1, 1, 0), Facing.NORTH, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, 0, 0, "minecraft:air")), List.of(), 0));
    }

    // ---------------------------------------------------------------- the standard stand-in

    /** The stand-in set: everything a sector needs, with weights that give a dense, loopy, tabletop-style map. */
    static SyntheticCatalog standard() {
        SyntheticCatalog c = new SyntheticCatalog();
        final String M = "nusmp:mega/";
        for (int d = 1; d <= 8; d++) c.cap(M + "cap/alcove_" + d, d);
        c.pool(MegaGrid.P_CAP, 0, MegaGrid.P_EMPTY, M + "cap/alcove_1", 2, M + "cap/alcove_2", 2, M + "cap/alcove_3", 2, M + "cap/alcove_4", 2, M + "cap/alcove_5", 2,
                M + "cap/alcove_6", 2, M + "cap/alcove_7", 1, M + "cap/alcove_8", 1);

        // landings
        c.put(c.shape(M + "landing/foyer_a", RoomKind.LANDING, 3, 3).all().marker(MarkerKind.ARRIVAL, 13, 9, 13));
        c.put(c.shape(M + "landing/foyer_b", RoomKind.LANDING, 3, 2).all().marker(MarkerKind.ARRIVAL, 13, 9, 9));
        c.put(c.shape(M + "landing/foyer_c", RoomKind.LANDING, 1, 1).all().marker(MarkerKind.ARRIVAL, 4, 9, 4));
        c.pool(MegaGrid.P_LANDING, 0, MegaGrid.P_CAP, M + "landing/foyer_a", 3, M + "landing/foyer_b", 2, M + "landing/foyer_c", 1);

        // halls and the like
        c.put(c.shape(M + "hall/cell_1x1", RoomKind.HALL, 1, 1).all());
        c.put(c.shape(M + "hall/chamber_2x1", RoomKind.HALL, 2, 1).all());
        c.put(c.shape(M + "hall/chamber_1x2", RoomKind.HALL, 1, 2).all());
        c.put(c.shape(M + "hall/hall_2x2", RoomKind.HALL, 2, 2).all());
        c.put(c.shape(M + "hall/hall_3x2", RoomKind.HALL, 3, 2).all());
        c.put(c.shape(M + "hall/great_3x3", RoomKind.HALL, 3, 3).all());
        c.put(c.shape(M + "corridor/straight_1x3", RoomKind.CORRIDOR, 1, 3).door('n', 4).door('s', 4));
        c.put(c.shape(M + "corridor/ribs_1x3", RoomKind.CORRIDOR, 1, 3).all());
        c.put(c.shape(M + "corridor/long_1x5", RoomKind.CORRIDOR, 1, 5).door('n', 4).door('s', 4).door('e', 22).door('w', 22));
        c.put(c.shape(M + "junction/cross_1x1", RoomKind.JUNCTION, 1, 1).all());
        c.put(c.shape(M + "junction/tee_1x1", RoomKind.JUNCTION, 1, 1).door('n', 4).door('e', 4).door('w', 4));
        c.put(c.shape(M + "safe/hearth_2x2", RoomKind.SAFE, 2, 2).all());
        c.put(c.shape(M + "safe/camp_1x2", RoomKind.SAFE, 1, 2).all());
        c.put(c.shape(M + "trial/archway_3x3", RoomKind.TRIAL, 3, 3).door('n', 13).door('s', 13));
        c.put(c.shape(M + "treasure/closet_1x1", RoomKind.TREASURE, 1, 1).door('s', 4));
        c.put(c.shape(M + "treasure/cache_2x1", RoomKind.TREASURE, 2, 1).door('s', 4).door('s', 13));
        c.put(c.shape(M + "set/rotunda_5x5", RoomKind.SET_PIECE, 5, 5).all().max(1));
        c.put(c.shape(M + "cavern/grotto_3x3", RoomKind.CAVERN, 3, 3).all());
        c.put(c.shape(M + "trap/pit_1x2", RoomKind.TRAP, 1, 2).door('n', 4).door('s', 4));
        c.put(c.shape(M + "puzzle/sigils_2x2", RoomKind.PUZZLE, 2, 2).all());
        c.pool(MegaGrid.P_SIDE, 2, MegaGrid.P_CAP,
                M + "hall/cell_1x1", 4, M + "hall/chamber_2x1", 5, M + "hall/chamber_1x2", 5, M + "hall/hall_2x2", 5, M + "hall/hall_3x2", 3, M + "hall/great_3x3", 2,
                M + "corridor/straight_1x3", 5, M + "corridor/ribs_1x3", 4, M + "corridor/long_1x5", 2, M + "junction/cross_1x1", 3, M + "junction/tee_1x1", 2,
                M + "safe/hearth_2x2", 1, M + "safe/camp_1x2", 1, M + "trial/archway_3x3", 2, M + "treasure/closet_1x1", 3, M + "treasure/cache_2x1", 1,
                M + "set/rotunda_5x5", 1, M + "cavern/grotto_3x3", 2, M + "trap/pit_1x2", 1, M + "puzzle/sigils_2x2", 2);
        c.pool(MegaGrid.P_SAFE, 0, MegaGrid.P_CAP, M + "safe/hearth_2x2", 2, M + "safe/camp_1x2", 1);

        // the boss chain
        c.put(c.shape(M + "arena/pit_5x5", RoomKind.ARENA, 5, 5).door('s', 22).door('e', 22).door('w', 22)
                .door('n', 22, MegaGrid.CHAIN, MegaGrid.CHAIN, MegaGrid.P_ANTECHAMBER, "minecraft:air")
                .marker(MarkerKind.GUARDIAN_SPAWN, 22, 9, 22).marker(MarkerKind.GATEKEEPER, 22, 10, 2));
        c.pool(MegaGrid.P_ARENA, 0, MegaGrid.P_CAP, M + "arena/pit_5x5", 1);
        c.put(c.shape(M + "antechamber/vestibule_3x3", RoomKind.ANTECHAMBER, 3, 3)
                .door('s', 13, MegaGrid.CHAIN, MegaGrid.CHAIN, MegaGrid.P_EMPTY, "minecraft:air")
                .door('w', 13, MegaGrid.VAULT, MegaGrid.VAULT, MegaGrid.P_VAULT, "minecraft:air")
                .door('e', 13, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_ONWARD, "minecraft:air")
                .door('n', 13));
        c.put(c.shape(M + "antechamber/vestibule_2x2", RoomKind.ANTECHAMBER, 2, 2)
                .door('s', 4, MegaGrid.CHAIN, MegaGrid.CHAIN, MegaGrid.P_EMPTY, "minecraft:air")
                .door('w', 13, MegaGrid.VAULT, MegaGrid.VAULT, MegaGrid.P_VAULT, "minecraft:air")
                .door('e', 13, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_ONWARD, "minecraft:air"));
        c.pool(MegaGrid.P_ANTECHAMBER, 0, MegaGrid.P_CAP, M + "antechamber/vestibule_3x3", 3, M + "antechamber/vestibule_2x2", 1);
        for (int n = 1; n <= 3; n++) {
            String name = new String[] {"", "one", "two", "three"}[n];
            B v = c.shape(M + "vault/" + name + "_a", RoomKind.VAULT, 3, 3).door('s', 13, MegaGrid.VAULT, MegaGrid.VAULT, MegaGrid.P_EMPTY, "minecraft:air")
                    .marker(MarkerKind.VAULT_CENTER, 13, 9, 13);
            for (int p = 0; p < n; p++) v.marker(MarkerKind.PEDESTAL, 8 + 5 * p, 9, 13, "slot", String.valueOf(p));
            c.put(v);
            c.pool(MegaGrid.P_VAULT + "_" + n, 0, MegaGrid.P_CAP, M + "vault/" + name + "_a", 1);
        }
        c.pool(MegaGrid.P_VAULT, 0, MegaGrid.P_CAP, M + "vault/one_a", 1, M + "vault/two_a", 1, M + "vault/three_a", 1);
        final String seal = "minecraft:cobbled_deepslate";
        c.put(c.shape(M + "onward/straight_a", RoomKind.ONWARD, 1, 2).door('s', 4, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('n', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 4, 10, 0, "facing", "north"));
        c.put(c.shape(M + "onward/straight_b", RoomKind.ONWARD, 1, 3).door('s', 4, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('n', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 4, 10, 0, "facing", "north"));
        c.put(c.shape(M + "onward/left_a", RoomKind.ONWARD, 1, 1).door('s', 4, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('w', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 0, 10, 4, "facing", "west"));
        c.put(c.shape(M + "onward/right_a", RoomKind.ONWARD, 1, 1).door('s', 4, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('e', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 8, 10, 4, "facing", "east"));
        c.pool(SectorPlanner.POOL_STRAIGHT, 0, MegaGrid.P_CAP, M + "onward/straight_a", 2, M + "onward/straight_b", 1);
        c.pool(SectorPlanner.POOL_LEFT, 0, MegaGrid.P_CAP, M + "onward/left_a", 1);
        c.pool(SectorPlanner.POOL_RIGHT, 0, MegaGrid.P_CAP, M + "onward/right_a", 1);
        c.pool(MegaGrid.P_ONWARD, 0, MegaGrid.P_CAP, M + "onward/straight_a", 1, M + "onward/left_a", 1, M + "onward/right_a", 1);

        // the tunnel
        c.put(c.shape(M + "link/straight_a", RoomKind.LINK, 1, 1).door('n', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_LINK, "minecraft:air")
                .door('s', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_LINK, "minecraft:air"));
        c.put(c.shape(M + "link/waystation_a", RoomKind.LINK, 3, 1).door('n', 13, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_LINK, "minecraft:air")
                .door('s', 13, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_LINK, "minecraft:air").door('w', 4).door('e', 4));
        c.pool(MegaGrid.P_LINK, 0, MegaGrid.P_CAP, M + "link/straight_a", 6, M + "link/waystation_a", 1);
        return c;
    }

    // ---------------------------------------------------------------- the stand-in with the REAL boss chain

    /**
     * {@link #standard()} with the boss chain drawn to the size and handedness of the templates that ship (the Guardian's hall five modules across with only its
     * entry and its chain door, ONE antechamber whose Way Onward is always on the left of the walk and whose vault is on the right, Ways Onward of three
     * modules or one by four, vaults of three modules). Why a second stand-in: the first one was drawn before any real template existed, with small chain pieces and
     * extra doors, and the assembler passed on it while it could not build a single sector from the real templates (the distance rule measured the door the
     * Guardian's hall hangs from, which at the edge of the band kept free for the chain is always too close to the Landing; the real hall is five modules across).
     * Tests on this catalog keep that from coming back without needing the real templates in the repository.
     */
    static SyntheticCatalog realSized() {
        SyntheticCatalog c = standard();
        final String M = "nusmp:mega/";
        c.put(c.shape(M + "arena/pit_5x5", RoomKind.ARENA, 5, 5).door('s', 22)
                .door('n', 22, MegaGrid.CHAIN, MegaGrid.CHAIN, MegaGrid.P_ANTECHAMBER, "minecraft:air")
                .marker(MarkerKind.GUARDIAN_SPAWN, 22, 9, 22).marker(MarkerKind.GATEKEEPER, 22, 10, 2));
        c.pool(MegaGrid.P_ARENA, 0, MegaGrid.P_CAP, M + "arena/pit_5x5", 1);
        c.put(c.shape(M + "antechamber/vestibule_3x3", RoomKind.ANTECHAMBER, 3, 3)
                .door('s', 13, MegaGrid.CHAIN, MegaGrid.CHAIN, MegaGrid.P_EMPTY, "minecraft:air")
                .door('e', 13, MegaGrid.VAULT, MegaGrid.VAULT, MegaGrid.P_VAULT, "minecraft:air")
                .door('w', 13, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_ONWARD, "minecraft:air"));
        c.pool(MegaGrid.P_ANTECHAMBER, 0, MegaGrid.P_CAP, M + "antechamber/vestibule_3x3", 1);
        final String seal = "minecraft:cobbled_deepslate";
        c.put(c.shape(M + "onward/straight_a", RoomKind.ONWARD, 1, 4).door('s', 4, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('n', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 4, 10, 0, "facing", "north"));
        c.put(c.shape(M + "onward/left_a", RoomKind.ONWARD, 3, 3).door('s', 22, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('w', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 0, 10, 4, "facing", "west"));
        c.put(c.shape(M + "onward/right_a", RoomKind.ONWARD, 3, 3).door('s', 4, MegaGrid.ONWARD, MegaGrid.ONWARD, MegaGrid.P_EMPTY, "minecraft:air")
                .door('e', 4, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, seal).marker(MarkerKind.EXPANSION_SOCKET, 26, 10, 4, "facing", "east"));
        c.pool(SectorPlanner.POOL_STRAIGHT, 0, MegaGrid.P_CAP, M + "onward/straight_a", 1);
        c.pool(SectorPlanner.POOL_LEFT, 0, MegaGrid.P_CAP, M + "onward/left_a", 1);
        c.pool(SectorPlanner.POOL_RIGHT, 0, MegaGrid.P_CAP, M + "onward/right_a", 1);
        c.pool(MegaGrid.P_ONWARD, 0, MegaGrid.P_CAP, M + "onward/straight_a", 1, M + "onward/left_a", 1, M + "onward/right_a", 1);
        return c;
    }

    // ---------------------------------------------------------------- the stand-in with stairwells (the floor model, 1.58.0)

    /**
     * {@link #realSized()} plus the pieces of the floor model: the four kinds of stairwell (one door on the east face, an ARRIVAL cell and a STAIR_EXIT cell each; the
     * grand ones are two modules across) and the pool the arrival stairwell's door draws its first room from. Why a stand-in as well as the real stairwells: the
     * floor assembler is tested on thousands of seeds in unit tests, and those tests must not depend on a template generator having run.
     */
    static SyntheticCatalog floors() {
        SyntheticCatalog c = realSized();
        final String M = "nusmp:mega/";
        String[][] kinds = {{"up", "up"}, {"down", "down"}, {"grand_up", "up"}, {"grand_down", "down"}};
        for (String[] k : kinds) {
            boolean grand = k[0].startsWith("grand");
            int side = grand ? 2 : 1;
            for (String v : new String[] {"a", "b"}) {
                String id = M + "stairwell/" + k[0] + "_" + v;
                B b = c.shape(id, RoomKind.STAIRWELL, side, side).door('e', 4, MegaGrid.DOOR, MegaGrid.DOOR, FloorPlan.P_STAIR_DOOR, "minecraft:air")
                        .marker(MarkerKind.ARRIVAL, 5, 9, 4).marker(MarkerKind.STAIR_EXIT, 3, 9, 4, "dir", k[1]);
                c.put(b);
            }
        }
        c.pool(FloorPlan.P_UP, 0, MegaGrid.P_CAP, M + "stairwell/up_a", 3, M + "stairwell/up_b", 2);
        c.pool(FloorPlan.P_DOWN, 0, MegaGrid.P_CAP, M + "stairwell/down_a", 3, M + "stairwell/down_b", 2);
        c.pool(FloorPlan.P_GRAND_UP, 0, MegaGrid.P_CAP, M + "stairwell/grand_up_a", 1, M + "stairwell/grand_up_b", 1);
        c.pool(FloorPlan.P_GRAND_DOWN, 0, MegaGrid.P_CAP, M + "stairwell/grand_down_a", 1, M + "stairwell/grand_down_b", 1);
        c.pool(FloorPlan.P_STAIR_DOOR, 0, MegaGrid.P_CAP,
                M + "hall/cell_1x1", 2, M + "hall/chamber_2x1", 3, M + "hall/chamber_1x2", 3, M + "hall/hall_2x2", 4, M + "corridor/straight_1x3", 5, M + "corridor/ribs_1x3", 4,
                M + "corridor/long_1x5", 2, M + "junction/cross_1x1", 3, M + "junction/tee_1x1", 3);
        return c;
    }

    // ---------------------------------------------------------------- the catalog.json format

    /** This catalog in the format of {@code tools/labyrinth/mega_catalog.py}, so {@link CatalogSource} and the simulator read it like the real one. */
    String toJson() {
        StringBuilder b = new StringBuilder("{\n \"floorRow\": 8,\n \"height\": 48,\n \"module\": 9,\n \"pools\": {");
        boolean first = true;
        for (PoolDef p : pools.values()) {
            b.append(first ? "\n" : ",\n");
            first = false;
            b.append("  ").append(PlanJson.str(p.id())).append(": {\"emptyWeight\": ").append(p.emptyWeight()).append(", \"fallback\": ").append(PlanJson.str(p.fallback())).append(", \"elements\": [");
            for (int i = 0; i < p.entries().size(); i++) {
                PoolEntry e = p.entries().get(i);
                b.append(i > 0 ? ", " : "").append("{\"template\": ").append(PlanJson.str(e.shape())).append(", \"weight\": ").append(e.weight()).append('}');
            }
            b.append("]}");
        }
        b.append("\n },\n \"templates\": {");
        first = true;
        for (Shape s : shapes.values()) {
            b.append(first ? "\n" : ",\n");
            first = false;
            b.append("  ").append(PlanJson.str(s.id)).append(": {\"kind\": ").append(PlanJson.str(s.kind.name())).append(", \"size\": [").append(s.sx).append(", ").append(s.sy).append(", ").append(s.sz).append("], \"sockets\": [");
            for (int i = 0; i < s.sockets.size(); i++) {
                Socket k = s.sockets.get(i);
                b.append(i > 0 ? ", " : "").append("{\"x\": ").append(k.cell().x()).append(", \"y\": ").append(k.cell().y()).append(", \"z\": ").append(k.cell().z())
                        .append(", \"facing\": ").append(PlanJson.str(k.facing().lower())).append(", \"name\": ").append(PlanJson.str(k.name())).append(", \"target\": ").append(PlanJson.str(k.target()))
                        .append(", \"pool\": ").append(PlanJson.str(k.pool())).append(", \"sel\": ").append(k.selection()).append(", \"place\": ").append(k.placement())
                        .append(", \"final\": ").append(PlanJson.str(k.finalState())).append('}');
            }
            b.append("], \"markers\": [");
            for (int i = 0; i < s.markers.size(); i++) {
                ShapeMarker m = s.markers.get(i);
                b.append(i > 0 ? ", " : "").append("{\"kind\": ").append(PlanJson.str(m.spec().kind().id())).append(", \"x\": ").append(m.cell().x()).append(", \"y\": ").append(m.cell().y())
                        .append(", \"z\": ").append(m.cell().z()).append(", \"args\": {");
                int a = 0;
                for (Map.Entry<String, String> e : m.spec().args().entrySet()) b.append(a++ > 0 ? ", " : "").append(PlanJson.str(e.getKey())).append(": ").append(PlanJson.str(e.getValue()));
                b.append("}}");
            }
            b.append("]}");
        }
        b.append("\n }\n}\n");
        return b.toString();
    }
}
