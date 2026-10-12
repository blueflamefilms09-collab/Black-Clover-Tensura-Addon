package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Fall;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Gate;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Grove;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenGrid.Site;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Golden City zone, planned (docs/living_dungeon.md section 5, zone 4): a pure, deterministic plan with no Minecraft imports. The cell
 * goes in, a {@link ZonePlan} comes out, built from the {@link GoldenGrid} of the cell: the biosphere's cavern, its shell, its ground and
 * everything that grows on it. The old Golden City ({@code dungeon/city/CityPlan}) is not in these ops: it follows as the next stage
 * ({@link GoldenStages}), then the Treasury under its plaza ({@link GoldenTreasury}), so that the city is the centrepiece of the garden.
 *
 * <p>Ops are ordered in phases: carve the cavern; foundation, shell, ceiling cap and ground; structures (waterfalls, gates, trees,
 * mushrooms, groves, geodes); details (ground cover, lily pads, what hangs from the ceiling). Everything is written in offsets from the
 * middle of the core cell, and all of it lies within {@link GoldenGrid#HALF} blocks of it.
 */
final class GoldenCityPlan {
    private GoldenCityPlan() {}

    private static final String DEEPSLATE = "minecraft:deepslate", DIRT = "minecraft:dirt", CLAY = "minecraft:clay", MUD = "minecraft:mud", WATER = "minecraft:water",
            PATH = "minecraft:dirt_path", COARSE = "minecraft:coarse_dirt";
    private static final String[] TOPS = {"minecraft:grass_block", "minecraft:moss_block", "minecraft:podzol", "minecraft:rooted_dirt", GoldenFlora.FROGLIGHT};

    private static final Map<Long, ZonePlan> PLANS = new LinkedHashMap<Long, ZonePlan>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ZonePlan> eldest) { return size() > 3; }
    };
    private static final Map<Long, ZonePlan> TREASURIES = new LinkedHashMap<Long, ZonePlan>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ZonePlan> eldest) { return size() > 3; }
    };

    private static long key(int cellX, int cellZ) { return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL); }

    /** The plan of the garden of the core cell (cellX, cellZ). */
    static synchronized ZonePlan of(int cellX, int cellZ) {
        ZonePlan p = PLANS.get(key(cellX, cellZ));
        if (p == null) {
            p = build(GoldenGrid.of(cellX, cellZ));
            PLANS.put(key(cellX, cellZ), p);
        }
        return p;
    }

    /** A plan worked out afresh, not the cached one (for tests that compare two runs). */
    static ZonePlan fresh(int cellX, int cellZ) { return build(GoldenGrid.fresh(cellX, cellZ)); }

    /** The ops of the Treasury (a plan only for its ops, in the frame of the city's middle). */
    static synchronized ZonePlan treasury(int cellX, int cellZ) {
        ZonePlan p = TREASURIES.get(key(cellX, cellZ));
        if (p == null) {
            p = GoldenTreasury.ops(GoldenGrid.of(cellX, cellZ).city, cellX, cellZ);
            TREASURIES.put(key(cellX, cellZ), p);
        }
        return p;
    }

    // ---------------------------------------------------------------- runs of equal columns
    private interface Key { Object at(int u, int v); }

    private interface Emit { void run(int u1, int u2, int v, Object key); }

    /** Walks each row of the grid and calls `emit` for every run of columns with the same non-null key. */
    private static void rows(Key key, Emit emit) {
        for (int v = -GoldenGrid.OFF; v <= GoldenGrid.OFF; v++) {
            int u = -GoldenGrid.OFF;
            while (u <= GoldenGrid.OFF) {
                Object k = key.at(u, v);
                if (k == null) {
                    u++;
                    continue;
                }
                int u2 = u;
                while (u2 + 1 <= GoldenGrid.OFF && k.equals(key.at(u2 + 1, v))) u2++;
                emit.run(u, u2, v, k);
                u = u2 + 1;
            }
        }
    }

    // ---------------------------------------------------------------- the plan
    private static ZonePlan build(GoldenGrid g) {
        final int f = g.floorY;
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.GOLDEN_CITY, g.cellX, g.cellZ, g.cx, g.cz)
                .floorY(f).topY(f + GoldenGrid.CEIL_MAX + GoldenGrid.WALL).footprint(Footprint.Shape.RECT, GoldenGrid.HALF, GoldenGrid.HALF);
        NexusOps o = b.ops();
        carve(o, g);
        o.phase();
        foundation(o, g);
        shell(o, g);
        ceilingCap(o, g);
        ground(o, g);
        o.phase();
        structures(o, g);
        o.phase();
        details(o, g);
        spots(b, g);
        return b.build();
    }

    /** Phase 0: the cavern, from just above the floor to the ceiling of each column. */
    private static void carve(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        rows((u, v) -> g.g(u, v) ? g.ceil(u, v) : null, (u1, u2, v, h) -> o.carve(u1, f + 1, v, u2, f + (Integer) h, v));
    }

    /** The rock under the floor, in the garden and the shell, and the dirt under the soil. */
    private static void foundation(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        rows((u, v) -> g.garden(u, v) ? Boolean.TRUE : null, (u1, u2, v, k) -> o.box(DEEPSLATE, u1, f - GoldenGrid.FOUNDATION, v, u2, f - 3, v));
        rows((u, v) -> g.g(u, v) ? Boolean.TRUE : null, (u1, u2, v, k) -> o.box(DIRT, u1, f - 2, v, u2, f - 1, v));
    }

    /** The paving of a path: trodden earth, a little coarse dirt, and now and then a golden froglight set flush in it, so the paths glow. */
    private static String pathBlock(int roll) { return roll < 7 ? GoldenFlora.FROGLIGHT : roll < 20 ? COARSE : PATH; }

    private static String skin(long seed, int u, int v) {
        double r = GoldenRng.unit(seed, u >> 1, v >> 1, 55);
        if (r < 0.34) return DEEPSLATE;
        if (r < 0.58) return "minecraft:tuff";
        if (r < 0.72) return "minecraft:moss_block";
        if (r < 0.80) return "minecraft:calcite";
        if (r < 0.94) return "minecraft:smooth_basalt";
        return "minecraft:dripstone_block";
    }

    /** The wall of the cavern: three columns of rock all round, the first of them skinned in stone that looks grown. */
    private static void shell(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        rows((u, v) -> g.shellAt(u, v) >= 2 ? (Object) g.shellTop[u + GoldenGrid.OFF][v + GoldenGrid.OFF] : null, (u1, u2, v, top) -> o.box(DEEPSLATE, u1, f - 2, v, u2, f + (Integer) top, v));
        for (int v = -GoldenGrid.OFF; v <= GoldenGrid.OFF; v++) {
            for (int u = -GoldenGrid.OFF; u <= GoldenGrid.OFF; u++) {
                if (g.shellAt(u, v) != 1) continue;
                o.box(skin(g.seed, u, v), u, f - 2, v, u, f + g.shellTop[u + GoldenGrid.OFF][v + GoldenGrid.OFF], v);
            }
        }
    }

    /** The roof of the cavern: a skin of glowing and growing stone, then rock. */
    private static void ceilingCap(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        rows((u, v) -> g.g(u, v) ? g.ceil(u, v) : null, (u1, u2, v, h) -> o.box(DEEPSLATE, u1, f + (Integer) h + 2, v, u2, f + (Integer) h + GoldenGrid.WALL, v));
        rows((u, v) -> g.g(u, v) ? (Object) (g.ceil(u, v) + "|" + GoldenFlora.ceilingSkin(g.seed, u, v)) : null, (u1, u2, v, k) -> {
            String s = (String) k;
            int bar = s.indexOf('|');
            o.box(s.substring(bar + 1), u1, f + Integer.parseInt(s.substring(0, bar)) + 1, v, u2, f + Integer.parseInt(s.substring(0, bar)) + 1, v);
        });
    }

    /** The ground: soil, grass and moss over the hills, paths, pools and their shores, and the soil of the roof garden over the old city. */
    private static void ground(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        // the soil under the hills, and the top block of every column outside the old city's walls (and over its roof)
        rows((u, v) -> {
            if (!g.g(u, v) || (g.inCity(u, v, 0) && !g.isRoof(u, v))) return null;
            byte k = g.coverAt(u, v);
            if (k == GoldenGrid.POOL || k == GoldenGrid.SHORE) return null;
            int base = g.isRoof(u, v) ? g.roofBase : 0;
            return g.groundAt(u, v) > base ? (Object) g.groundAt(u, v) : null;
        }, (u1, u2, v, h) -> o.box(DIRT, u1, f + ((Integer) h >= g.roofBase ? g.roofBase : 0), v, u2, f + (Integer) h - 1, v));
        rows((u, v) -> {
            if (!g.g(u, v) || (g.inCity(u, v, 0) && !g.isRoof(u, v))) return null;
            byte k = g.coverAt(u, v);
            if (k == GoldenGrid.POOL || k == GoldenGrid.SHORE) return null;
            int h = g.groundAt(u, v);
            String top = k == GoldenGrid.PATH ? pathBlock(GoldenRng.hash(g.seed, u, v, 57) % 100) : TOPS[g.groundMaterial(u, v)];
            return h + "|" + top;
        }, (u1, u2, v, k) -> {
            String s = (String) k;
            int bar = s.indexOf('|');
            int h = Integer.parseInt(s.substring(0, bar));
            o.box(s.substring(bar + 1), u1, f + h, v, u2, f + h, v);
        });
        rows((u, v) -> g.g(u, v) && g.coverAt(u, v) == GoldenGrid.POOL && !g.isRoof(u, v) ? Boolean.TRUE : null, (u1, u2, v, k) -> {
            o.box(CLAY, u1, f - 2, v, u2, f - 2, v);
            o.box(WATER, "level=0", u1, f - 1, v, u2, f, v);
        });
        rows((u, v) -> g.g(u, v) && g.coverAt(u, v) == GoldenGrid.POOL && g.isRoof(u, v) ? Boolean.TRUE : null,
                (u1, u2, v, k) -> o.box(WATER, "level=0", u1, f + g.roofBase, v, u2, f + g.roofBase, v));           // a pond on the roof: one layer of water on the roof's own stone
        rows((u, v) -> g.g(u, v) && g.coverAt(u, v) == GoldenGrid.SHORE ? Boolean.TRUE : null, (u1, u2, v, k) -> o.box(MUD, u1, f - 1, v, u2, f, v));
    }

    // ---------------------------------------------------------------- phase 2: structures
    private static void structures(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        for (Fall w : g.falls) {                                          // a spring in the wall, a thin stream, and the pool it lands in
            o.put(WATER, "level=0", w.srcU(), f + w.height(), w.srcV());
            o.put(WATER, "level=1", w.u(), f + w.height(), w.v());
            o.box(WATER, "level=8", w.u(), f + 1, w.v(), w.u(), f + w.height() - 1, w.v());
        }
        for (Site s : g.sites) {
            if (s.kind() == GoldenGrid.GREAT_TREE || s.kind() == GoldenGrid.TREE) GoldenFlora.tree(o, g, s);
            else GoldenFlora.mushroom(o, g, s);
        }
        for (Grove gr : g.groves) GoldenFlora.grove(o, g, gr);
        GoldenFlora.geodes(o, g);
        for (Gate gate : g.gates) gate(o, g, gate);
        hedge(o, g);
        ivy(o, g);                                                         // last: an ivy ladder runs on through a crown that hangs over it
    }

    /** The sides of a block an ivy clings to, as a property string (bit 1 north, 2 east, 4 south, 8 west of the cell it stands in). */
    static String ivyFaces(int mask) {
        StringBuilder sb = new StringBuilder();
        String[] names = {"north", "east", "south", "west"};
        for (int k = 0; k < 4; k++) {
            if ((mask & (1 << k)) == 0) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(names[k]).append("=true");
        }
        return sb.toString();
    }

    /** Ivy on the old city's wall: whole ladders from the ground up to the layer over the roof, and drapes that hang from the roof's edge. */
    private static void ivy(NexusOps o, GoldenGrid g) {
        final int f = g.floorY, top = f + g.roofBase;
        for (int[] c : g.ivy) {
            int bottom = c[3] == 1 ? f + 1 : Math.max(f + GoldenGrid.IVY_FROM, top - 3 - GoldenRng.hash(g.seed, c[0], c[1], 97) % 9);
            o.box("minecraft:vine", ivyFaces(c[2]), c[0], bottom, c[1], c[0], top, c[1]);
        }
    }

    /** A hedge of flowering azalea round the roof so nobody walks off it, left open where an ivy ladder comes up. */
    private static void hedge(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        int[] dx = {0, 1, 0, -1}, dz = {-1, 0, 1, 0};
        Set<Long> landing = new HashSet<>();
        for (int[] c : g.ivy) {
            if (c[3] != 1) continue;
            for (int k = 0; k < 4; k++) {
                if ((c[2] & (1 << k)) == 0) continue;
                for (int du = -1; du <= 1; du++) for (int dv = -1; dv <= 1; dv++) landing.add(((long) (c[0] + dx[k] + du + GoldenGrid.OFF) << 20) | (c[1] + dz[k] + dv + GoldenGrid.OFF));
            }
        }
        for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
            for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                if (!g.isRoofEdge(u, v) || landing.contains(((long) (u + GoldenGrid.OFF) << 20) | (v + GoldenGrid.OFF))) continue;
                o.box(GoldenFlora.FLOWERING_LEAVES, GoldenFlora.PERSISTENT, u, f + g.groundAt(u, v) + 1, v, u, f + g.groundAt(u, v) + 2, v);
            }
        }
    }

    /** The garden side of a gate: an arch of mossy bricks over the last cells of the spoke, with a lantern on each pillar. */
    private static void gate(NexusOps o, GoldenGrid g, Gate gate) {
        final int f = g.floorY;
        for (int l = -1; l <= 1; l += 2) {
            int u = GoldenGrid.sideU(gate.side(), gate.tP(), gate.w() + l), v = GoldenGrid.sideV(gate.side(), gate.tP(), gate.w() + l);
            o.box("minecraft:mossy_stone_bricks", u, f + 1, v, u, f + 4, v);
            o.put("minecraft:lantern", "hanging=false", u, f + 5, v);
        }
        for (int l = -1; l <= 1; l++) {
            int u = GoldenGrid.sideU(gate.side(), gate.tP(), gate.w() + l), v = GoldenGrid.sideV(gate.side(), gate.tP(), gate.w() + l);
            o.put("minecraft:chiseled_stone_bricks", u, f + 4, v);
        }
        o.put("minecraft:lantern", "hanging=true", gate.u(), f + 3, gate.v());
    }

    // ---------------------------------------------------------------- phase 3: details
    private static void details(NexusOps o, GoldenGrid g) {
        final int f = g.floorY;
        for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
            for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                if (!g.g(u, v)) continue;
                byte k = g.coverAt(u, v);
                if (k == GoldenGrid.POOL) {
                    boolean spring = false;
                    for (Fall w : g.falls) if (w.u() == u && w.v() == v) spring = true;
                    if (!spring && GoldenRng.hash(g.seed, u, v, 58) % 100 < 16) o.put("minecraft:lily_pad", u, f + g.groundAt(u, v) + 1, v);
                    continue;
                }
                if (!BiosphereCycle.mayWrite(k) && k != GoldenGrid.LICHEN) continue;
                BiosphereCycle.Cover c = BiosphereCycle.cover(g.seed, k, u, v, 0L);
                if (!c.air()) o.put(c.block(), c.props(), u, f + g.groundAt(u, v) + 1, v);
            }
        }
        for (GoldenGrid.Vine vi : g.vines) {                                // the vines under the crowns, as long as the cycle has them at time zero
            int len = BiosphereCycle.vineLength(g.seed, vi.u(), vi.v(), vi.max(), 0L);
            String berries = BiosphereCycle.ripe(g.seed, vi.u(), vi.v(), 0L) ? "berries=true" : "berries=false";
            int top = f + vi.anchor();
            if (len > 1) o.box("minecraft:cave_vines_plant", berries, vi.u(), top - len + 1, vi.v(), vi.u(), top - 1, vi.v());
            o.put("minecraft:cave_vines", "age=25," + berries, vi.u(), top - len, vi.v());
        }
        GoldenFlora.ceilingGrowth(o, g);
    }

    // ---------------------------------------------------------------- relics, gates, entrances, creatures
    private static void spots(ZonePlan.Builder b, GoldenGrid g) {
        final int f = g.floorY;
        ZonePlan.Spot central = GoldenTreasury.centralSpot(g.city);
        b.centralRelic(central.x(), central.y(), central.z());
        for (ZonePlan.Spot s : GoldenTreasury.minorSpots(g.city)) b.minorRelic(s.x(), s.y(), s.z());
        ZonePlan.Spot gate = GoldenTreasury.gateSpot(g.city);
        b.gate(gate.x(), gate.y(), gate.z());
        for (Gate gt : g.gates) {
            b.entrance(g.cx + gt.u(), f + 1, g.cz + gt.v());
            int back = Math.max(gt.tRing(), gt.tP() - 3);
            b.spawn(ZonePlan.ROLE_ARRIVAL, g.cx + GoldenGrid.sideU(gt.side(), back, gt.w()), f + 1, g.cz + GoldenGrid.sideV(gt.side(), back, gt.w()));
        }
        for (int[] s : g.city.sentinels) b.spawn(ZonePlan.ROLE_GUARD, s[0], s[1], s[2]);
        List<int[]> stand = g.stand;
        for (int i = 0; i < stand.size(); i++) {
            int[] s = stand.get(i);
            b.spawn(i % 2 == 0 ? "prey" : "apex", g.cx + s[0], f + s[2], g.cz + s[1]);
        }
    }

    /** Which of the five relics (an index into the relic enum) the central pedestal holds in this cell; the four minor ones hold the others. */
    static int centralRelicIndex(int cellX, int cellZ, int relics) {
        if (relics <= 0) return 0;
        return GoldenRng.hash(GoldenRng.mix(cellX, cellZ), 5, 5, 91) % relics;
    }
}
