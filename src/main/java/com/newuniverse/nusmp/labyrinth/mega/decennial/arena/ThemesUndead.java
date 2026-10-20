package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Opts;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Pal;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: the halls of the dead and the bled: the graveyard of the Lich (the owner's first example: a decaying graveyard whose BREAKABLE TOMBSTONES spawn adds), the ruined throne room of the Fallen King (the
 * second example: DESTRUCTIBLE PILLARS), the bone cathedral, the blood arena, the library of forbidden lore and the vampire's ballroom. Each is a hardcoded plan: a palette, a skeleton from {@link Shell},
 * and the features that make it that place, with the interactive elements the fight uses and the hazard zones the lair actions aim at.
 */
final class ThemesUndead {
    private ThemesUndead() {}

    // ================================================================ the graveyard
    /** A decaying graveyard: podzol and coarse dirt, mossy walls, rows of tombstones that spawn adds when broken, a mausoleum with coffins, dead trees, lamp posts. */
    static BossArenaStructure graveyard(int size) {
        Pal p = new Pal(b("cobbled_deepslate"), b("podzol"), b("coarse_dirt"), b("mossy_cobblestone"), b("mossy_stone_bricks"), b("cracked_stone_bricks"), b("chiseled_stone_bricks"),
                b("mossy_stone_bricks"), b("chiseled_stone_bricks"), "mossy_stone_brick_slab", "mossy_stone_brick_stairs", b("tinted_glass"), b("verdant_froglight"), 8);
        BossArenaStructure a = Shell.start("graveyard", size, 30, p, new Opts(1, 0, 8, true));
        ArenaRng r = ArenaRng.of("graveyard", size);
        int c = a.c;
        // moss and bone dust on the ground
        for (int i = 0; i < size * 3; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Math.abs(x - c) > 2 && dist(a, x, z) > 9) a.set(x, 0, z, r.chance(0.5) ? b("moss_block") : b("mossy_cobblestone"));
        }
        // the mausoleum in the north-east corner: a stone house with a door towards the lane and two sarcophagi
        int mx = size - 15, mz = 4;
        shell(a, mx, 1, mz, mx + 9, 7, mz + 9, b("stone_bricks"));
        fill(a, mx + 1, 1, mz + 1, mx + 8, 6, mz + 8, BossArenaStructure.AIR);
        fill(a, mx, 1, mz + 4, mx, 3, mz + 5, BossArenaStructure.AIR);                            // the door, on the lane side
        fill(a, mx - 1, 8, mz - 1, mx + 10, 8, mz + 10, slab("stone_brick_slab", "bottom"));
        fill(a, mx, 8, mz, mx + 9, 8, mz + 9, b("mossy_stone_bricks"));
        Parts.coffin(a, mx + 4, mz + 3, true, 2, b("polished_deepslate"), slab("deepslate_tile_slab", "bottom"));
        Parts.coffin(a, mx + 4, mz + 6, true, 2, b("polished_deepslate"), slab("deepslate_tile_slab", "bottom"));
        a.set(mx + 7, 1, mz + 4, b("soul_lantern"));
        // dead trees at the walls
        int[][] trees = {{6, size - 8}, {8, 12}, {size - 8, size - 9}, {c - 14, size - 6}, {c + 15, size - 7}, {7, c}};
        for (int[] t : trees) Parts.deadTree(a, t[0], t[1], 6 + r.next(3), b("dark_oak_log"), b("dark_oak_wood"));
        // the tombstones: the breakable ones spawn the dead
        int n = size * size / 85;
        Parts.scatter(a, r, n, 3, 4, (x, z) -> {
            boolean grand = r.chance(0.35);
            String base = grand ? b("mossy_cobblestone") : b("mossy_stone_bricks");
            String top = grand ? st("cobblestone_wall", "up=true") : st("stone_brick_wall", "up=true");
            Draw.tombstone(a, x, z, grand ? 2 : 1, base, top);
            a.zone("grave", x, z, 2);
        });
        // unbreakable crypt slabs and fence plots, for the look
        for (int i = 0; i < size / 3; i++) {
            int x = r.range(4, size - 5), z = r.range(4, size - 5);
            if (Draw.keepClear(a, x, z) || a.get(x, 1, z) != null) continue;
            a.set(x, 1, z, slab("mossy_cobblestone_slab", "bottom"));
        }
        // lamp posts along the lane
        for (int z = 8; z < size - 6; z += 8) {
            if (Math.abs(z - c) < 10) continue;
            for (int s = -1; s <= 1; s += 2) {
                if (!Parts.free(a, c + s * 4, z)) continue;
                for (int y = 1; y <= 3; y++) a.set(c + s * 4, y, z, st("stone_brick_wall", "up=true"));
                a.set(c + s * 4, 4, z, st("soul_lantern", "hanging=false"));
            }
        }
        a.spot(c - 8, c + 8);
        a.spot(c + 8, c + 8);
        a.zone("fog", c - 12, c, 5);
        a.zone("fog", c + 12, c, 5);
        return a;
    }

    // ================================================================ the ruined throne room
    /** A ruined throne room: a long carpet, a shattered throne, twelve destructible pillars that fall towards the middle, tattered banners and chandeliers that can be shot down. */
    static BossArenaStructure ruinedThroneRoom(int size) {
        Pal p = new Pal(b("deepslate_bricks"), b("polished_blackstone"), b("polished_blackstone_bricks"), b("gilded_blackstone"), b("polished_blackstone_bricks"), b("cracked_polished_blackstone_bricks"),
                b("chiseled_polished_blackstone"), b("polished_blackstone"), b("chiseled_polished_blackstone"), "polished_blackstone_brick_slab", "polished_blackstone_brick_stairs", b("gray_stained_glass"),
                b("shroomlight"), 9);
        BossArenaStructure a = Shell.start("ruined_throne_room", size, 32, p, new Opts(0, 0, 0, true));
        ArenaRng r = ArenaRng.of("ruined_throne_room", size);
        int c = a.c;
        Parts.lane(a, b("red_carpet"));
        // cracks in the floor
        for (int i = 0; i < size * 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Math.abs(x - c) > 1) a.set(x, 0, z, b("cracked_polished_blackstone_bricks"));
        }
        // the shattered throne on the north side of the dais: a seat, a broken back, a three-step plinth
        int tz = c - 12;
        fill(a, c - 2, 1, tz - 1, c + 2, 1, tz + 3, b("polished_blackstone_bricks"));
        fill(a, c - 1, 2, tz, c + 1, 2, tz + 2, b("polished_blackstone"));
        a.set(c, 3, tz + 1, stair("polished_blackstone_brick_stairs", "south", "bottom"));
        for (int y = 3; y <= 7; y++) {
            a.set(c, y, tz, y <= 5 ? b("blackstone") : b("cracked_polished_blackstone_bricks"));
            if (y <= 6) a.set(c - 1, y, tz, y <= 4 ? b("blackstone") : b("polished_blackstone_brick_wall"));
        }
        for (int y = 3; y <= 4; y++) a.set(c + 1, y, tz, b("blackstone"));                      // the right side of the back has fallen
        a.set(c + 1, 1, tz + 4, stair("polished_blackstone_brick_stairs", "west", "bottom"));  // a piece of it on the floor
        a.set(c + 2, 1, tz + 4, slab("polished_blackstone_brick_slab", "bottom"));
        // twelve destructible pillars, six a side, each a three-by-three shaft that falls towards the middle
        int pillars = 0, span = c - 7;
        for (int k = -span / 5; k <= span / 5; k++) {
            if (k == 0) continue;
            for (int sd = -1; sd <= 1; sd += 2) {
                if (Parts.pillar(a, c + sd * 11, c + k * 5, 18, b("polished_blackstone_bricks"), b("chiseled_polished_blackstone"), b("gilded_blackstone"), true,
                        slab("polished_blackstone_brick_slab", "bottom")) != null) pillars++;
            }
        }
        // chandeliers on chains
        for (int s = -1; s <= 1; s += 2) for (int z = c - 6; z <= c + 6; z += 12) Parts.chandelier(a, c + s * 5, z, b("chain"), st("lantern", "hanging=true"));
        // banners
        Parts.banners(a, "red", 8, 7);
        Parts.banners(a, "black", 12, 14);
        // the court's two rows of kneeling statues of the guard: decoration against the walls
        for (int z = 8; z < size - 8; z += 5) {
            if (Math.abs(z - c) < 5) continue;
            for (int s = -1; s <= 1; s += 2) {
                int x = s < 0 ? 4 : size - 5;
                a.set(x, 1, z, b("polished_blackstone"));
                a.set(x, 2, z, b("wither_skeleton_skull"));
            }
        }
        a.spot(c - 6, c + 14);
        a.spot(c + 6, c + 14);
        a.spot(c - 6, c - 14);
        a.spot(c + 6, c - 14);
        a.zone("bone", c, c + 14, 4);
        return a;
    }

    // ================================================================ the bone cathedral
    /** A cathedral of bone: bone-block columns and rib vaults, ossuary niches of skulls, eight sarcophagi that release the dead, a ring of candles round the dais. */
    static BossArenaStructure boneCathedral(int size) {
        Pal p = new Pal(b("deepslate_bricks"), b("smooth_quartz"), b("calcite"), b("bone_block"), b("bone_block"), b("calcite"), b("chiseled_quartz_block"), b("bone_block"), b("quartz_pillar"),
                "quartz_slab", "quartz_stairs", b("white_stained_glass"), b("sea_lantern"), 9);
        BossArenaStructure a = Shell.start("bone_cathedral", size, 34, p, new Opts(0, 2, 7, true));
        ArenaRng r = ArenaRng.of("bone_cathedral", size);
        int c = a.c;
        // the nave: two rows of tall bone columns joined by rib arches
        for (int z = c - 15; z <= c + 15; z += 6) {
            if (Math.abs(z - c) < 4) continue;
            for (int s = -1; s <= 1; s += 2) {
                int x = c + s * 9;
                Draw.pillar(a, x, z, 1, 22, axis("bone_block", "y"), b("quartz_pillar"), b("chiseled_quartz_block"), false);
                for (int k = 1; k <= 8; k++) a.set(x - s * k, 22 + (k + 1) / 2, z, b("bone_block"));        // the rib that bends towards the middle
            }
        }
        // ossuary niches in the side walls: skulls on shelves
        for (int z = 6; z < size - 6; z += 4) {
            if (Math.abs(z - c) < 3) continue;
            for (int s = 0; s < 2; s++) {
                int x = s == 0 ? 2 : size - 3;
                a.set(x, 1, z, slab("stone_brick_slab", "bottom"));
                a.set(x, 2, z, b("skeleton_skull"));
                a.set(x, 4, z, b("skeleton_skull"));
                a.set(x, 3, z, slab("stone_brick_slab", "top"));
            }
        }
        // eight sarcophagi in two rows, the dead step out of them when they are broken
        int[][] cofs = {{c - 14, c - 8}, {c + 14, c - 8}, {c - 14, c + 8}, {c + 14, c + 8}, {c - 17, c}, {c + 17, c}, {c - 8, c + 17}, {c + 8, c + 17}};
        for (int[] q : cofs) Parts.coffin(a, q[0], q[1], r.chance(0.5), 2, b("polished_diorite"), slab("polished_diorite_slab", "bottom"));
        // candles round the dais
        for (int[] q : Parts.ringSpots(a, 10.5, 16, 0.2)) a.set(q[0], 1, q[1], st("candle", "candles=3,lit=true"));
        // a bell in the north porch, ribs hanging from the roof
        for (int i = 0; i < 6; i++) Parts.hanging(a, c + (i - 3) * 5 + 2, c + (i % 2 == 0 ? -10 : 10), 5 + r.next(5), b("chain"), b("skeleton_skull"));
        Parts.banners(a, "white", 9, 9);
        a.zone("bone", c - 10, c, 4);
        a.zone("bone", c + 10, c, 4);
        a.zone("bone", c, c + 11, 4);
        a.zone("bone", c, c - 11, 4);
        return a;
    }

    // ================================================================ the blood arena
    /** A sunken arena of red brick and nether wart, with pools of blood, galleries behind iron bars, urns of blood that burst into imps and banners the colour of a wound. */
    static BossArenaStructure bloodArena(int size) {
        Pal p = new Pal(b("nether_bricks"), b("red_nether_bricks"), b("nether_wart_block"), b("crimson_planks"), b("red_nether_bricks"), b("cracked_nether_bricks"), b("chiseled_nether_bricks"),
                b("red_nether_bricks"), b("chiseled_nether_bricks"), "red_nether_brick_slab", "red_nether_brick_stairs", b("red_stained_glass"), b("shroomlight"), 10);
        BossArenaStructure a = Shell.start("blood_arena", size, 28, p, new Opts(2, 2, 8, true));
        ArenaRng r = ArenaRng.of("blood_arena", size);
        int c = a.c;
        // four pools of blood sunk into the floor
        for (int[] q : Parts.ringSpots(a, 13.5, 4, Math.PI / 4)) {
            Parts.pool(a, q[0], q[1], 3.2, b("red_nether_bricks"), b("redstone_block"), b("crimson_nylium"));
            a.zone("blood", q[0], q[1], 4);
        }
        // the galleries: a raised ledge round the wall with iron bars in front
        for (int i = 4; i < size - 4; i++) {
            if (Math.abs(i - c) <= 3) continue;
            for (int y = 1; y <= 2; y++) {
                a.set(i, y, 3, b("red_nether_bricks"));
                a.set(i, y, size - 4, b("red_nether_bricks"));
                a.set(3, y, i, b("red_nether_bricks"));
                a.set(size - 4, y, i, b("red_nether_bricks"));
            }
            a.set(i, 3, 3, b("iron_bars"));
            a.set(i, 3, size - 4, b("iron_bars"));
            a.set(3, 3, i, b("iron_bars"));
            a.set(size - 4, 3, i, b("iron_bars"));
        }
        // urns of blood: they burst into imps
        for (int[] q : Parts.ringSpots(a, Math.min(17.0, c - 6.0), 8, 0.1)) {
            Parts.stack(a, Kind.URN, Effect.ADDS, 2, q[0], q[1], b("red_nether_bricks"), b("red_nether_brick_wall"), b("crimson_hyphae"));
            a.zone("blood", q[0], q[1], 3);
        }
        // spikes of dripstone, hanging from the ceiling like teeth
        for (int i = 0; i < size / 2; i++) {
            int x = r.range(4, size - 5), z = r.range(4, size - 5);
            if (Draw.keepClear(a, x, z)) continue;
            a.set(x, a.height - 2, z, st("pointed_dripstone", "thickness=tip,vertical_direction=down"));
        }
        Parts.banners(a, "red", 9, 7);
        Parts.banners(a, "black", 14, 14);
        return a;
    }

    // ================================================================ the library of forbidden lore
    /** Aisles of bookshelves, lecterns, chained tomes, chandeliers; the shelves can be broken and what is written in them gets out. */
    static BossArenaStructure forbiddenLibrary(int size) {
        Pal p = new Pal(b("deepslate_tiles"), b("dark_oak_planks"), b("spruce_planks"), b("polished_deepslate"), b("stripped_dark_oak_log"), b("dark_oak_planks"), b("chiseled_bookshelf"),
                b("dark_oak_log"), b("stripped_dark_oak_log"), "dark_oak_slab", "dark_oak_stairs", b("purple_stained_glass"), b("sea_lantern"), 9);
        BossArenaStructure a = Shell.start("forbidden_library", size, 32, p, new Opts(0, 0, 0, true));
        ArenaRng r = ArenaRng.of("forbidden_library", size);
        int c = a.c;
        Parts.lane(a, b("purple_carpet"));
        // rows of shelves, running north-south, with a gap for the lane and the clearing
        for (int x = 5; x < size - 5; x += 4) {
            if (Math.abs(x - c) <= 3) continue;
            for (int z = 6; z < size - 6; z++) {
                if (Draw.dist(a, x, z) <= 9.5) continue;
                if (z % 9 == 0) continue;                                                      // a cross aisle
                boolean breakable = (x + z) % 5 == 0;
                if (breakable) {
                    Parts.stack(a, Kind.SHELF, Effect.ADDS, 1, x, z, b("bookshelf"), b("bookshelf"), b("bookshelf"), b("bookshelf"));
                    a.zone("tome", x, z, 3);
                } else {
                    for (int y = 1; y <= 5; y++) a.set(x, y, z, y == 5 ? b("dark_oak_planks") : b("bookshelf"));
                }
            }
        }
        // lecterns and tables at the ends of the aisles
        for (int x = 6; x < size - 6; x += 8) {
            if (Math.abs(x - c) <= 3) continue;
            a.set(x, 1, 4, b("lectern"));
            a.set(x, 1, size - 5, b("lectern"));
        }
        // chandeliers
        for (int s = -1; s <= 1; s += 2) for (int z = c - 12; z <= c + 12; z += 12) if (z != c) Parts.chandelier(a, c + s * 6, z, b("chain"), st("soul_lantern", "hanging=true"));
        Parts.banners(a, "purple", 9, 10);
        a.spot(c - 10, c + 10);
        a.spot(c + 10, c - 10);
        a.zone("tome", c, c + 12, 4);
        a.zone("tome", c, c - 12, 4);
        return a;
    }

    // ================================================================ the vampire's ballroom
    /** A gothic ballroom: a black and white floor, tall windows, chandeliers, a red carpet, and four coffins on the dais steps for the vampire's brides. */
    static BossArenaStructure vampireBallroom(int size) {
        Pal p = new Pal(b("deepslate_bricks"), b("polished_diorite"), b("black_concrete"), b("polished_blackstone"), b("stone_bricks"), b("cracked_stone_bricks"), b("chiseled_stone_bricks"),
                b("polished_andesite"), b("chiseled_stone_bricks"), "stone_brick_slab", "stone_brick_stairs", b("red_stained_glass"), b("shroomlight"), 10);
        BossArenaStructure a = Shell.start("vampire_ballroom", size, 32, p, new Opts(0, 2, 7, true));
        int c = a.c;
        Parts.lane(a, b("red_carpet"));
        // tall windows of red glass in the side walls
        for (int z = 7; z < size - 7; z += 6) {
            if (Math.abs(z - c) <= 2) continue;
            for (int s = 0; s < 2; s++) {
                int x = s == 0 ? 1 : size - 2;
                for (int y = 4; y <= 15; y++) fill(a, x, y, z - 1, x, y, z + 1, b("red_stained_glass"));
            }
        }
        // chandeliers, the ballroom's six
        for (int s = -1; s <= 1; s += 2) for (int z = c - 12; z <= c + 12; z += 12) Parts.chandelier(a, c + s * 8, z, b("chain"), st("lantern", "hanging=true"));
        // four coffins at the corners of the dais
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) Parts.coffin(a, c + sx * 13, c + sz * 10, false, 2, b("black_concrete"), slab("polished_blackstone_slab", "bottom"));
        // a stone dancing floor ring, candles on tall stands
        for (int[] q : Parts.ringSpots(a, 15.0, 12, 0.3)) {
            for (int y = 1; y <= 3; y++) a.set(q[0], y, q[1], st("andesite_wall", "up=true"));
            a.set(q[0], 4, q[1], st("candle", "candles=4,lit=true"));
        }
        Parts.banners(a, "red", 8, 8);
        a.zone("bat", c - 12, c, 5);
        a.zone("bat", c + 12, c, 5);
        a.zone("blood", c, c + 14, 4);
        return a;
    }
}
