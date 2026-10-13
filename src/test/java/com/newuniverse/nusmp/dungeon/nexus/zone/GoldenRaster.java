package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A stand-in world for the Golden City's tests: a block grid round the garden whose every cell starts as air (the worst case: a cave all
 * round), onto which the garden's ops, the old city's ops and the Treasury's ops are applied in the order the Nexus builder applies them.
 * Blocks are names; the city's palette entries are named "city:" and the palette entry.
 */
final class GoldenRaster {
    /** Blocks that neither stop a creature nor water: the plant life, carpets, hanging things, the doorway, water itself. */
    static final Set<String> OPEN = Set.of("minecraft:air", "minecraft:water", "minecraft:short_grass", "minecraft:fern", "minecraft:moss_carpet", "minecraft:poppy",
            "minecraft:dandelion", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip",
            "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:torchflower",
            "minecraft:pink_petals", "minecraft:glow_lichen", "minecraft:cave_vines", "minecraft:cave_vines_plant", "minecraft:hanging_roots", "minecraft:spore_blossom",
            "minecraft:lily_pad", "minecraft:lantern", "minecraft:vine", "minecraft:amethyst_cluster", "minecraft:small_amethyst_bud", "minecraft:azalea", "minecraft:flowering_azalea");

    final int x0, y0, z0, sx, sy, sz;
    final int[] cells;
    final Map<String, Integer> ids = new HashMap<>();
    final List<String> names = new java.util.ArrayList<>();

    /** A raster for a garden: the footprint grown by 4, from 8 under the floor to 4 over the roof. */
    GoldenRaster(ZonePlan p) {
        x0 = p.footprint.minX() - 4;
        z0 = p.footprint.minZ() - 4;
        y0 = p.floorY - 12;
        sx = p.footprint.maxX() + 4 - x0 + 1;
        sz = p.footprint.maxZ() + 4 - z0 + 1;
        sy = p.topY + 4 - y0 + 1;
        cells = new int[sx * sy * sz];
        id("minecraft:air");
        id("minecraft:bedrock");
    }

    int id(String name) {
        return ids.computeIfAbsent(name, n -> {
            names.add(n);
            return names.size() - 1;
        });
    }

    int index(int x, int y, int z) { return ((y - y0) * sz + (z - z0)) * sx + (x - x0); }

    boolean in(int x, int y, int z) { return x >= x0 && x < x0 + sx && y >= y0 && y < y0 + sy && z >= z0 && z < z0 + sz; }

    String at(int x, int y, int z) { return in(x, y, z) ? names.get(cells[index(x, y, z)]) : "minecraft:air"; }

    void set(int x, int y, int z, String name) { cells[index(x, y, z)] = id(name); }

    boolean open(int x, int y, int z) { return OPEN.contains(at(x, y, z)); }

    /** Applies a Nexus op as the glue does: a carve clears all but bedrock, a box fills (never over bedrock), fill-air only where there is air. */
    void apply(NexusOp o) {
        int block = o.kind() == NexusOp.Kind.CARVE ? 0 : id(o.block());
        for (int y = o.y1(); y <= o.y2(); y++) {
            for (int z = o.z1(); z <= o.z2(); z++) {
                for (int x = o.x1(); x <= o.x2(); x++) {
                    if (!in(x, y, z)) throw new AssertionError("op reaches outside the volume: " + o);
                    int i = index(x, y, z);
                    if (cells[i] == 1) continue;
                    if (o.kind() == NexusOp.Kind.FILL_AIR && cells[i] != 0) continue;
                    cells[i] = block;
                }
            }
        }
    }

    void apply(ZonePlan p) { for (NexusOp o : p.ops) apply(o); }

    /** Applies the old city's ops: the palette entries become blocks named "city:NAME" (water is water, air is air). */
    void apply(CityPlan city) {
        for (CityPlan.Op o : city.ops) {
            int block;
            if (o.kind() == CityPlan.Kind.CARVE || o.pal() == CityPlan.Pal.AIR) block = 0;
            else if (o.pal() == CityPlan.Pal.WATER) block = id("minecraft:water");
            else block = id("city:" + o.pal().name());
            for (int y = o.y1(); y <= o.y2(); y++) {
                for (int z = o.z1(); z <= o.z2(); z++) {
                    for (int x = o.x1(); x <= o.x2(); x++) {
                        if (!in(x, y, z)) throw new AssertionError("a city op reaches outside the volume: " + o);
                        int i = index(x, y, z);
                        if (cells[i] != 1) cells[i] = block;
                    }
                }
            }
        }
    }

    /** The whole build in the builder's order: the garden, the old city, the Treasury. */
    static GoldenRaster built(int cellX, int cellZ) {
        ZonePlan plan = GoldenCityPlan.of(cellX, cellZ);
        GoldenRaster r = new GoldenRaster(plan);
        r.apply(plan);
        r.apply(GoldenGrid.of(cellX, cellZ).city);
        r.apply(GoldenCityPlan.treasury(cellX, cellZ));
        return r;
    }

    // ---------------------------------------------------------------- what a creature can do
    /** Can something stand with its feet in this cell: the cell and the one above are open, and the floor under it is not (or the cell is water)? */
    boolean standable(int x, int y, int z) {
        if (at(x, y, z).equals("minecraft:vine")) return true;                    // ivy is climbed: one can hang on it anywhere
        if (!open(x, y, z) || !open(x, y + 1, z)) return false;
        return !open(x, y - 1, z) || at(x, y, z).equals("minecraft:water") || at(x, y - 1, z).equals("minecraft:water");
    }

    /** The feet cells reachable on foot (a step up or down of one block at a time) from a start cell. */
    boolean[] reach(int sx0, int sy0, int sz0) {
        boolean[] seen = new boolean[cells.length];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (!in(sx0, sy0, sz0) || !standable(sx0, sy0, sz0)) return seen;
        seen[index(sx0, sy0, sz0)] = true;
        queue.add(new int[]{sx0, sy0, sz0});
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int k = 0; k < 4; k++) {
                int nx = c[0] + dx[k], nz = c[2] + dz[k];
                for (int dy = 1; dy >= -1; dy--) {
                    int ny = c[1] + dy;
                    if (!in(nx, ny, nz) || seen[index(nx, ny, nz)] || !standable(nx, ny, nz)) continue;
                    if (dy == 1 && !open(c[0], c[1] + 2, c[2])) continue;               // a step up needs room over the head
                    seen[index(nx, ny, nz)] = true;
                    queue.add(new int[]{nx, ny, nz});
                    break;
                }
            }
            if (at(c[0], c[1], c[2]).equals("minecraft:vine")) {                      // up and down the ivy
                for (int dy = -1; dy <= 1; dy += 2) {
                    int ny = c[1] + dy;
                    if (in(c[0], ny, c[2]) && !seen[index(c[0], ny, c[2])] && standable(c[0], ny, c[2])) {
                        seen[index(c[0], ny, c[2])] = true;
                        queue.add(new int[]{c[0], ny, c[2]});
                    }
                }
            }
        }
        return seen;
    }

    boolean reached(boolean[] seen, int x, int y, int z) { return in(x, y, z) && seen[index(x, y, z)]; }
}
