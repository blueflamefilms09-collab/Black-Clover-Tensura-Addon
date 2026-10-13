package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Role;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultOp.Kind;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultOp.Pal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Where the Dungeon's hidden rooms are and what they are made of, with no Minecraft in it. The world is reached through a
 * {@link VaultTerrain} (one classified block at a time, asked about only where a candidate needs it), so everything here is tested on
 * made-up caves.
 *
 * <p>Sites are deterministic per cell of a grid: a vault cell of 64 blocks holds a vault one time in four, a shrine cell of 96 blocks a
 * Descent Shrine a little more than one time in two, and every core cell of 128 blocks has one shrine above the Variable Core. A cell tries
 * up to {@link #ATTEMPTS} candidate columns in a fixed order; the candidate that finds a room carved into solid rock beside a cave wins, so
 * the same world always gives the same vault. The caller keeps the winner in its saved data, because building changes the rock the search
 * reads.
 */
public final class VaultPlan {
    private VaultPlan() {}

    /** The grids a site can belong to: plain vault cells, upper shrine cells, and the core cells (one shrine above each Variable Core). */
    public static final int GRID_VAULT = 0, GRID_GATE = 1, GRID_CORE = 2;
    /** Width of one cell of each grid, in blocks. */
    public static final int[] CELL = {64, 96, 128};
    public static final int VAULT_PERCENT = 25, GATE_PERCENT = 55, ATTEMPTS = 8, COLUMNS = 3;
    /** A search stops reading the world once this many heights fit (from the top down), so one attempt costs a bounded number of reads. */
    private static final int MAX_GOOD = 6;
    /** At least this share of a room's sampled shell must be solid rock, and at most these shares of its front wall may be open or odd rock. */
    private static final int SOLID_PERCENT = 82, WALL_OPEN_PERCENT = 35, WALL_OTHER_PERCENT = 20;

    // ---------------------------------------------------------------- hashing
    /** A well mixed 64-bit hash of three numbers and a salt. */
    static long mix(long a, long b, long c, long salt) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 32;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return h;
    }

    // ---------------------------------------------------------------- what a cell holds
    /** The kind of vault the 64-block cell holds, or null if it holds none. */
    public static VaultKind vaultAt(int cellX, int cellZ) {
        long h = mix(cellX, cellZ, 0, 101);
        if (Math.floorMod(h, 100L) >= VAULT_PERCENT) return null;
        return VaultKind.vaultByRoll((int) Math.floorMod(h >>> 8, 100L));
    }

    /** Does the 96-block cell hold a Descent Shrine? */
    public static boolean gateAt(int cellX, int cellZ) { return Math.floorMod(mix(cellX, cellZ, 0, 202), 100L) < GATE_PERCENT; }

    /** One try at a site: where its middle goes, which way it faces and how big it is. The height is found by the search. */
    public record Candidate(int grid, VaultKind kind, int centreX, int centreZ, int facing, int width, int height, int depth, boolean hidden, int variant) {
        /** The same candidate turned to face another way. */
        public Candidate withFacing(int f) { return new Candidate(grid, kind, centreX, centreZ, Math.floorMod(f, 4), width, height, depth, hidden, variant); }
    }

    /** The COLUMNS candidates of round `attempt` of a cell (rounds 0 .. ATTEMPTS-1), in the order they are tried. */
    public static List<Candidate> round(int grid, VaultKind kind, int cellX, int cellZ, int attempt) {
        List<Candidate> out = new ArrayList<>(COLUMNS);
        for (int i = 0; i < COLUMNS; i++) out.add(candidate(grid, kind, cellX, cellZ, attempt * COLUMNS + i));
        return out;
    }

    /** The attempt-th candidate of a cell. Stable. */
    public static Candidate candidate(int grid, VaultKind kind, int cellX, int cellZ, int attempt) {
        long h = mix(cellX, cellZ, attempt, 300 + grid * 17 + kind.ordinal());
        int facing = (int) (h & 3);
        int variant = (int) ((h >>> 33) & 0x7FFFFFFFL);
        int w, hh, d;
        switch (kind) {
            case PUZZLE -> { w = 7; hh = 5; d = 9; }
            case NUTRIENT -> { w = d = (variant & 1) == 1 ? 9 : 7; hh = 5; }
            case MINIBOSS -> { w = 9; hh = 6; d = 9; }
            default -> { w = 5; hh = 4; d = 5; }
        }
        boolean hidden = grid != GRID_GATE || Math.floorMod(variant >>> 3, 3) == 0;
        int size = CELL[grid];
        int x, z;
        if (grid == GRID_CORE) {
            double angle = ((h >>> 12) & 0xFFFFL) / 65536.0 * Math.PI * 2.0;
            double radius = 22.0 + ((h >>> 28) & 0x1FL) * (22.0 / 31.0);
            x = cellX * size + size / 2 + (int) Math.round(StrictMath.cos(angle) * radius);
            z = cellZ * size + size / 2 + (int) Math.round(StrictMath.sin(angle) * radius);
        } else {
            int margin = 13, span = size - 2 * margin;
            x = cellX * size + margin + (int) Math.floorMod(h >>> 8, (long) span);
            z = cellZ * size + margin + (int) Math.floorMod(h >>> 20, (long) span);
        }
        return new Candidate(grid, kind, x, z, facing, w, hh, d, hidden, variant);
    }

    /** The site of a candidate with its floor at height y. */
    public static VaultSite siteAt(Candidate c, int y, boolean hidden) {
        int sx = (c.facing() & 1) == 0 ? c.width() : c.depth(), sz = (c.facing() & 1) == 0 ? c.depth() : c.width();
        return new VaultSite(c.kind(), c.centreX() - sx / 2, y, c.centreZ() - sz / 2, c.width(), c.height(), c.depth(), c.facing(), hidden, c.variant());
    }

    // ---------------------------------------------------------------- the search
    public enum Status { FOUND, NONE, UNKNOWN }

    /** The outcome of a search: found (with the site), nothing here, or the world is not loaded enough to say. */
    public record Result(Status status, VaultSite site) {
        static final Result NONE = new Result(Status.NONE, null), UNKNOWN = new Result(Status.UNKNOWN, null);
    }

    /** May a room stand here? (The game says no to the Variable Core's cells and the like.) */
    @FunctionalInterface
    public interface Rules { boolean allowed(int x, int y, int z); }

    /**
     * Looks for a floor height at which the candidate's room fits: a doorway of open air (or water) in front of the front wall with a
     * solid floor under it, solid rock round the room, and a front wall that is mostly rock. Returns the site at the height the
     * candidate's own number picks among the first few (from the top down) that fit. If any block it needs is not loaded the answer is
     * UNKNOWN, never a guess.
     */
    public static Result evaluate(Candidate c, int minFloorY, int maxFloorY, Rules rules, VaultTerrain t) {
        List<VaultSite> good = new ArrayList<>();
        for (int y = maxFloorY; y >= minFloorY; y--) {
            VaultSite s = siteAt(c, y, c.hidden());
            int door = doorState(s, t);
            if (door < 0) return Result.UNKNOWN;
            if (door == 0) continue;
            if (door == 2 && !s.hidden()) s = siteAt(c, y, true);                  // water outside: the wall must be an airtight illusion
            int room = roomState(s, t, rules);
            if (room < 0) return Result.UNKNOWN;
            if (room == 1 && good.add(s) && good.size() >= MAX_GOOD) break;
        }
        if (good.isEmpty()) return Result.NONE;
        return new Result(Status.FOUND, good.get((int) Math.floorMod((long) (c.variant() >>> 7), (long) good.size())));
    }

    /**
     * One round of a cell's search: each of the round's columns, facing each of the four ways in turn (the candidate's own way first), through
     * {@link #evaluate}. The first that fits wins. If a block that a column needs is not loaded the round answers UNKNOWN (unless an earlier
     * column already fit), never a guess, and may be asked again later with the same result.
     */
    public static Result search(int grid, VaultKind kind, int cellX, int cellZ, int attempt, int minFloorY, int maxFloorY, Rules rules, VaultTerrain t) {
        for (Candidate c : round(grid, kind, cellX, cellZ, attempt)) {
            for (int f = 0; f < 4; f++) {
                Result r = evaluate(c.withFacing(c.facing() + f), minFloorY, maxFloorY, rules, t);
                if (r.status() != Status.NONE) return r;
            }
        }
        return Result.NONE;
    }

    /** -1 unknown, 0 no door here, 1 a dry doorway, 2 a flooded one. */
    private static int doorState(VaultSite s, VaultTerrain t) {
        int u = s.width() / 2, y = s.iy();
        int dx = s.worldX(u, -2), dz = s.worldZ(u, -2);
        int a = t.at(dx, y, dz);
        if (a == VaultTerrain.UNKNOWN) return -1;
        if (!VaultTerrain.open(a)) return 0;
        int b = t.at(dx, y + 1, dz);
        if (b == VaultTerrain.UNKNOWN) return -1;
        if (b != a) return 0;
        int c3 = t.at(s.worldX(u, -3), y, s.worldZ(u, -3));
        if (c3 == VaultTerrain.UNKNOWN) return -1;
        if (c3 != a) return 0;
        if (a == VaultTerrain.AIR) {
            int floor = t.at(dx, y - 1, dz);
            if (floor == VaultTerrain.UNKNOWN) return -1;
            return VaultTerrain.solid(floor) ? 1 : 0;
        }
        return 2;
    }

    /** -1 unknown, 0 the room does not fit, 1 it fits. */
    private static int roomState(VaultSite s, VaultTerrain t, Rules rules) {
        if (!rules.allowed(s.centreX(), s.iy(), s.centreZ())) return 0;
        int x0 = s.ix() - 1, x1 = s.maxX() + 1, z0 = s.iz() - 1, z1 = s.maxZ() + 1;
        for (int y : new int[]{s.iy() - 1, s.maxY() + 1}) {
            if (!rules.allowed(x0, y, z0) || !rules.allowed(x1, y, z0) || !rules.allowed(x0, y, z1) || !rules.allowed(x1, y, z1)) return 0;
        }
        int plane = 0, open = 0, other = 0;
        for (int u = 0; u < s.width(); u++) {
            for (int y = 0; y < s.height(); y++) {
                int code = t.at(s.worldX(u, -1), s.iy() + y, s.worldZ(u, -1));
                if (code == VaultTerrain.UNKNOWN) return -1;
                if (code == VaultTerrain.LAVA) return 0;
                plane++;
                if (VaultTerrain.open(code)) open++;
                else if (code == VaultTerrain.OTHER) other++;
            }
        }
        if (open * 100 > plane * WALL_OPEN_PERCENT || other * 100 > plane * WALL_OTHER_PERCENT) return 0;
        int total = (s.width() + 2) * (s.height() + 2) * (s.depth() + 1), maxHollow = total * (100 - SOLID_PERCENT) / 100, hollow = 0;
        boolean tooHollow = false;                                                  // stops reading after the slice in which the room turned out to be too hollow
        for (int u = -1; u <= s.width(); u++) {
            for (int y = -1; y <= s.height(); y++) {
                for (int w = 0; w <= s.depth(); w++) {                              // the shell and the hollow; the front wall (w = -1) was judged above
                    int code = t.at(s.worldX(u, w), s.iy() + y, s.worldZ(u, w));
                    if (code == VaultTerrain.UNKNOWN) return -1;
                    if (code == VaultTerrain.LAVA) return 0;
                    if (!VaultTerrain.solid(code) && ++hollow > maxHollow) tooHollow = true;
                }
            }
            if (tooHollow) break;
        }
        if (tooHollow) return 0;
        return 1;
    }

    // ---------------------------------------------------------------- the arrival chamber
    /**
     * Where to cut the sealed chamber that a Descent Gate opens in the flooded floor below: in the column (x, z), at the floor height in
     * [minFloorY, maxFloorY] and facing that fit best (the most solid rock round it, and an open doorway in front of its illusion wall).
     * UNKNOWN if something needed is not loaded; NONE if every height broke a rule.
     */
    public static Result chamber(int x, int z, int minFloorY, int maxFloorY, int seed, Rules rules, VaultTerrain t) {
        VaultSite best = null;
        int bestScore = -1;
        for (int y = minFloorY; y <= maxFloorY; y++) {
            for (int f = 0; f < 4; f++) {
                VaultSite s = new VaultSite(VaultKind.GATE, x - 2, y, z - 2, 5, 4, 5, f, true, seed);
                if (!rules.allowed(s.centreX(), y, s.centreZ())) continue;
                int total = 0, solid = 0;
                boolean lava = false;
                for (int u = -1; u <= 5; u++) {
                    for (int yy = -1; yy <= 4; yy++) {
                        for (int w = 0; w <= 5; w++) {
                            int code = t.at(s.worldX(u, w), y + yy, s.worldZ(u, w));
                            if (code == VaultTerrain.UNKNOWN) return Result.UNKNOWN;
                            if (code == VaultTerrain.LAVA) lava = true;
                            total++;
                            if (VaultTerrain.solid(code)) solid++;
                        }
                    }
                }
                int door = t.at(s.worldX(2, -2), y, s.worldZ(2, -2));
                if (door == VaultTerrain.UNKNOWN) return Result.UNKNOWN;
                if (lava || door == VaultTerrain.LAVA) continue;
                int score = solid * 1000 / Math.max(1, total) + (VaultTerrain.open(door) ? 300 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
        }
        return best == null ? Result.NONE : new Result(Status.FOUND, best);
    }

    // ---------------------------------------------------------------- layouts
    /** Collects the ops and spots of one layout in the room's own coordinates. */
    private static final class Builder {
        final VaultSite s;
        final List<VaultOp> ops = new ArrayList<>();
        final Map<Role, List<VaultLayout.Spot>> spots = new EnumMap<>(Role.class);

        Builder(VaultSite s) { this.s = s; }

        void box(Kind k, Pal p, int u1, int u2, int y1, int y2, int w1, int w2) {
            int xa = s.worldX(u1, w1), xb = s.worldX(u2, w2), za = s.worldZ(u1, w1), zb = s.worldZ(u2, w2);
            ops.add(new VaultOp(k, p, Math.min(xa, xb), s.iy() + y1, Math.min(za, zb), Math.max(xa, xb), s.iy() + y2, Math.max(za, zb)));
        }

        void put(Pal p, int u, int y, int w) { box(Kind.BOX, p, u, u, y, y, w, w); }

        void spot(Role r, int index, int u, int y, int w) {
            spots.computeIfAbsent(r, k -> new ArrayList<>()).add(new VaultLayout.Spot(r, index, s.worldX(u, w), s.iy() + y, s.worldZ(u, w)));
        }

        VaultLayout build() { return new VaultLayout(s, ops, spots); }
    }

    /** The shell, the front wall, the hollow and the door spot: what every room has. */
    private static Builder base(VaultSite s) {
        Builder b = new Builder(s);
        int w = s.width(), h = s.height(), d = s.depth();
        if (s.hidden()) b.box(Kind.BOX, Pal.ILLUSION, 0, w - 1, 0, h - 1, -1, -1);
        else b.box(Kind.SOFT, Pal.ROCK, 0, w - 1, 0, h - 1, -1, -1);
        b.box(Kind.SOFT, Pal.ROCK, -1, w, -1, -1, -1, d);                      // floor
        b.box(Kind.SOFT, Pal.ROCK, -1, w, h, h, -1, d);                        // ceiling
        b.box(Kind.SOFT, Pal.ROCK, -1, w, 0, h - 1, d, d);                     // back wall
        b.box(Kind.SOFT, Pal.ROCK, -1, -1, 0, h - 1, -1, d - 1);               // left wall
        b.box(Kind.SOFT, Pal.ROCK, w, w, 0, h - 1, -1, d - 1);                 // right wall
        b.box(Kind.CARVE, Pal.AIR, 0, w - 1, 0, h - 1, 0, d - 1);              // the hollow
        if (!s.hidden()) b.box(Kind.CARVE, Pal.AIR, w / 2, w / 2, 0, 1, -1, -1);   // an open doorway through the wall
        b.spot(Role.DOOR, 0, w / 2, 0, -2);
        return b;
    }

    private static void corners(Builder b) {
        int w = b.s.width(), h = b.s.height(), d = b.s.depth();
        b.put(Pal.SOUL_LANTERN_HANGING, 0, h - 1, 0);
        b.put(Pal.SOUL_LANTERN_HANGING, w - 1, h - 1, 0);
        b.put(Pal.SOUL_LANTERN_HANGING, 0, h - 1, d - 1);
        b.put(Pal.SOUL_LANTERN_HANGING, w - 1, h - 1, d - 1);
    }

    /** A small stable number 0..99 for a cell of a room. */
    static int cellRoll(int u, int w, int variant, int salt) { return (int) Math.floorMod(mix(u, w, variant, salt), 100L); }

    /** The layout of a vault or a Descent Shrine. */
    public static VaultLayout layout(VaultSite s) {
        Builder b = base(s);
        int w = s.width(), h = s.height(), d = s.depth();
        switch (s.kind()) {
            case PUZZLE -> {
                int[] us = {1, 3, 5}, ws = {2, 4, 6};
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        b.put(Pal.PLATE_IDLE, us[c], -1, ws[r]);
                        b.spot(Role.PLATE, r * 3 + c, us[c], -1, ws[r]);
                    }
                }
                b.box(Kind.BOX, Pal.BLACKSTONE_BRICKS, 2, 2, 0, 2, 7, 8);       // the niche for the cache
                b.box(Kind.BOX, Pal.BLACKSTONE_BRICKS, 4, 4, 0, 2, 7, 8);
                b.box(Kind.BOX, Pal.BLACKSTONE_BRICKS, 2, 4, 3, 3, 7, 8);
                b.box(Kind.BOX, Pal.IRON_BARS, 3, 3, 0, 2, 7, 7);               // the bars the solved puzzle lifts
                for (int y = 0; y < 3; y++) b.spot(Role.BARS, y, 3, y, 7);
                b.spot(Role.PEDESTAL, 0, 3, 0, 8);
                corners(b);
            }
            case NUTRIENT -> {
                b.box(Kind.BOX, Pal.MOSS, 0, w - 1, -1, -1, 0, d - 1);
                int blooms = 0;
                for (int u = 0; u < w; u++) {
                    for (int v = 1; v < d; v++) {
                        int r = cellRoll(u, v, s.variant(), 77);
                        if (r < 14 && blooms < 10) {
                            Pal age = Pal.values()[Pal.BLOOM_1.ordinal() + cellRoll(u, v, s.variant(), 78) % 3];
                            b.put(age, u, 0, v);
                            b.spot(Role.BLOOM, blooms++, u, 0, v);
                        } else if (r < 44) {
                            b.put(Pal.MOSS_CARPET, u, 0, v);
                        } else if (r >= 92) {
                            b.put(Pal.SPORE_BLOSSOM, u, h - 1, v);
                        }
                    }
                }
            }
            case MINIBOSS -> {
                b.box(Kind.BOX, Pal.BLACKSTONE_BRICKS, 3, 5, -1, -1, 6, 8);     // the dais
                b.put(Pal.CRYING_OBSIDIAN, 4, -1, 7);
                for (int[] p : new int[][]{{2, 2}, {6, 2}, {2, 6}, {6, 6}}) {
                    b.box(Kind.BOX, Pal.BLACKSTONE_BRICKS, p[0], p[0], 0, h - 2, p[1], p[1]);
                    b.put(Pal.SOUL_LANTERN_HANGING, p[0], h - 1, p[1]);
                }
                b.spot(Role.PEDESTAL, 0, 4, 0, 7);
                b.spot(Role.GUARDIAN, 0, 4, 0, 4);
            }
            default -> {
                shrine(b, Pal.GATE_DOWN);
            }
        }
        return b.build();
    }

    /** The layout of the sealed arrival chamber below a Descent Gate: a shrine whose gate leads back up, behind an illusion wall. */
    public static VaultLayout chamberLayout(VaultSite s) {
        Builder b = base(new VaultSite(VaultKind.GATE, s.ix(), s.iy(), s.iz(), s.width(), s.height(), s.depth(), s.facing(), true, s.variant()));
        shrine(b, Pal.GATE_UP);
        return b.build();
    }

    private static void shrine(Builder b, Pal gate) {
        b.box(Kind.BOX, Pal.BLACKSTONE_BRICKS, 1, 3, -1, -1, 2, 4);
        b.put(Pal.CRYING_OBSIDIAN, 2, -1, 3);
        b.put(gate, 2, 0, 3);
        b.spot(Role.GATE, 0, 2, 0, 3);
        b.spot(Role.STAND, 0, 2, 0, 1);
        corners(b);
    }
}
