package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Opts;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Pal;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: the halls of the light: the cathedral, the celestial court, the sun temple, the hanging gardens, the sky bridge, the crystal sanctum, the moonlit glade and the storm spire. They are white,
 * gold, green and violet where the halls of the dark are black and red, they are the brightest rooms of the dungeon (the hidden light grid is 13 to 15), and their interactive elements are the
 * guardians the light keeps: statues that wake as living armour, sacred braziers, columns that can be felled, blooms that burst into wisps and crystals that ring.
 */
final class ThemesHoly {
    private ThemesHoly() {}

    // ================================================================ the cathedral
    static BossArenaStructure cathedral(int size) {
        Pal p = new Pal(b("stone_bricks"), b("smooth_quartz"), b("polished_diorite"), b("chiseled_quartz_block"), b("quartz_bricks"), b("smooth_quartz"), b("chiseled_quartz_block"), b("quartz_pillar"),
                b("chiseled_quartz_block"), "quartz_slab", "quartz_stairs", b("white_stained_glass"), b("sea_lantern"), 14);
        BossArenaStructure a = Shell.start("cathedral", size, 36, p, new Opts(0, 1, 0, true));
        ArenaRng r = ArenaRng.of("cathedral", size);
        int c = a.c;
        Parts.lane(a, b("white_carpet"));
        // pews: rows of stairs either side of the lane, facing the altar
        for (int z = 6; z < size - 6; z += 3) {
            if (Math.abs(z - c) <= 10) continue;
            for (int s = -1; s <= 1; s += 2) {
                for (int k = 4; k <= 9; k++) a.set(c + s * k, 1, z, stair("spruce_stairs", z < c ? "north" : "south", "bottom"));
            }
        }
        // the colour of the windows: tall lancets of coloured glass in the two long walls
        String[] glass = {"red", "blue", "yellow", "green", "purple", "light_blue"};
        int w = 0;
        for (int z = 7; z < size - 7; z += 6) {
            if (Math.abs(z - c) <= 2) continue;
            String g = b(glass[w++ % glass.length] + "_stained_glass");
            for (int s = 0; s < 2; s++) {
                int x = s == 0 ? 1 : size - 2;
                for (int y = 4; y <= 17; y++) fill(a, x, y, z - 1, x, y, z + 1, g);
                fill(a, x, 18, z, x, 19, z, g);
            }
        }
        // the nave's columns: they can be felled
        int span = c - 8;
        for (int k = -span / 6; k <= span / 6; k++) {
            if (k == 0) continue;
            for (int s = -1; s <= 1; s += 2) Parts.pillar(a, c + s * 12, c + k * 6, 22, b("quartz_pillar"), b("chiseled_quartz_block"), b("gold_block"), true, slab("quartz_slab", "bottom"));
        }
        // statues of the saints that wake as armour
        for (int[] q : Parts.ringSpots(a, Math.min(17.0, c - 5.0), 6, 0.5)) {
            Parts.stack(a, Kind.STATUE, Effect.ADDS, 1, q[0], q[1], b("smooth_stone"), b("quartz_pillar"), b("quartz_pillar"), b("chiseled_quartz_block"));
            a.zone("beam", q[0], q[1], 3);
        }
        // sacred braziers by the altar rail
        for (int[] q : Parts.ringSpots(a, 10.5, 6, 0.2)) Parts.brazier(a, q[0], q[1], b("quartz_bricks"), st("lantern", "hanging=false"));
        // censers on chains
        for (int i = -1; i <= 1; i += 2) for (int z = c - 14; z <= c + 14; z += 14) Parts.hanging(a, c + i * 6, z, 7, b("chain"), st("lantern", "hanging=true"));
        Parts.banners(a, "white", 9, 9);
        Parts.banners(a, "yellow", 20, 18);
        a.zone("beam", c, c + 13, 4);
        a.zone("beam", c, c - 13, 4);
        return a;
    }

    // ================================================================ the celestial court
    static BossArenaStructure celestialCourt(int size) {
        Pal p = new Pal(b("quartz_bricks"), b("smooth_quartz"), b("quartz_block"), b("gold_block"), b("quartz_bricks"), b("smooth_quartz"), b("chiseled_quartz_block"), b("quartz_pillar"),
                b("gold_block"), "quartz_slab", "quartz_stairs", b("light_blue_stained_glass"), b("sea_lantern"), 15);
        BossArenaStructure a = Shell.start("celestial_court", size, 38, p, new Opts(2, 2, 0, true));
        ArenaRng r = ArenaRng.of("celestial_court", size);
        int c = a.c;
        // terraces: a stepped ring of quartz between radius 12 and 16, climbing and falling again
        for (int z = 3; z < size - 3; z++)
            for (int x = 3; x < size - 3; x++) {
                double d = dist(a, x, z);
                if (Math.abs(x - c) <= 2) continue;
                if (d > 12 && d <= 13.5) a.set(x, 1, z, slab("quartz_slab", "bottom"));
                else if (d > 13.5 && d <= 16) a.set(x, 1, z, b("quartz_block"));
                else if (d > 16 && d <= 17.5) a.set(x, 1, z, slab("quartz_slab", "bottom"));
            }
        // twelve columns of the court on the terrace, each can be felled
        int n = 0;
        for (int[] q : Parts.ringSpots(a, Math.min(14.8, c - 5.0), 12, 0.13)) {
            if (Math.abs(q[0] - c) <= 3) continue;
            if (Parts.pillar(a, q[0], q[1], 20, b("quartz_pillar"), b("gold_block"), b("chiseled_quartz_block"), false, slab("quartz_slab", "bottom")) != null) n++;
        }
        // statues of archons round the walls, aether crystals on the terrace
        for (int[] q : Parts.ringSpots(a, Math.min(19.0, c - 4.0), 8, 0.4)) {
            Parts.stack(a, Kind.STATUE, Effect.ADDS, 1, q[0], q[1], b("gold_block"), b("quartz_pillar"), b("quartz_pillar"), b("chiseled_quartz_block"), b("gold_block"));
            a.zone("beam", q[0], q[1], 3);
        }
        for (int[] q : Parts.ringSpots(a, 15.0, 4, Math.PI / 4)) {
            Parts.single(a, Kind.CRYSTAL, Effect.SHATTER, 1, q[0], 2, q[1], b("light_blue_stained_glass"));
            a.set(q[0], 3, q[1], b("sea_lantern"));
            a.zone("judgment", q[0], q[1], 4);
        }
        // clouds drifting under the roof
        for (int i = 0; i < size / 2; i++) {
            int x = r.range(4, size - 5), z = r.range(4, size - 5), y = a.height - 8 - r.next(4);
            fill(a, x - 1, y, z - 1, x + 1, y, z + 1, b("white_wool"));
        }
        for (int i = 4; i < size - 4; i += 6) {
            if (Math.abs(i - c) <= 3) continue;
            Parts.hanging(a, i, 4, 8, b("chain"), b("gold_block"));
            Parts.hanging(a, i, size - 5, 8, b("chain"), b("gold_block"));
        }
        Parts.banners(a, "light_blue", 10, 9);
        Parts.banners(a, "yellow", 22, 18);
        a.zone("judgment", c, c, 8);
        return a;
    }

    // ================================================================ the sun temple
    static BossArenaStructure sunTemple(int size) {
        Pal p = new Pal(b("sandstone"), b("smooth_sandstone"), b("cut_sandstone"), b("gold_block"), b("cut_sandstone"), b("smooth_sandstone"), b("chiseled_sandstone"), b("cut_sandstone"),
                b("chiseled_sandstone"), "cut_sandstone_slab", "sandstone_stairs", b("yellow_stained_glass"), b("glowstone"), 15);
        BossArenaStructure a = Shell.start("sun_temple", size, 36, p, new Opts(2, 1, 8, true));
        ArenaRng r = ArenaRng.of("sun_temple", size);
        int c = a.c;
        // the rays of the sun in the floor
        for (int k = 0; k < 16; k++) {
            double ang = k * Math.PI / 8;
            for (double d = 9; d < c - 3; d += 0.5) {
                int x = (int) Math.round(c + Math.cos(ang) * d), z = (int) Math.round(c + Math.sin(ang) * d);
                a.set(x, 0, z, (k & 1) == 0 ? b("gold_block") : b("orange_glazed_terracotta"));
            }
        }
        // the ziggurat steps round the dais
        for (int z = c - 13; z <= c + 13; z++)
            for (int x = c - 13; x <= c + 13; x++) {
                double d = Math.hypot(x - c, z - c);
                if (Math.abs(x - c) <= 2) continue;
                if (d > 10.5 && d <= 12) a.set(x, 1, z, slab("cut_sandstone_slab", "bottom"));
            }
        // obelisks in the corners: they can be felled
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) Parts.pillar(a, c + sx * (c - 8), c + sz * (c - 8), 24, b("cut_sandstone"), b("chiseled_sandstone"), b("gold_block"), true,
                slab("cut_sandstone_slab", "bottom"));
        // eight braziers of the sun
        for (int[] q : Parts.ringSpots(a, Math.min(15.0, c - 6.0), 8, 0.0)) {
            Parts.brazier(a, q[0], q[1], b("cut_sandstone"), st("lantern", "hanging=false"));
            a.zone("sun", q[0], q[1], 4);
        }
        // sphinx-like guardians: statues of sandstone that wake
        for (int[] q : Parts.ringSpots(a, Math.min(19.0, c - 4.0), 4, Math.PI / 4)) Parts.stack(a, Kind.STATUE, Effect.ADDS, 2, q[0], q[1], b("chiseled_sandstone"), b("cut_sandstone"), b("cut_sandstone"),
                b("chiseled_sandstone"));
        Parts.banners(a, "orange", 9, 8);
        Parts.banners(a, "yellow", 16, 16);
        a.zone("sun", c, c + 12, 5);
        a.zone("sun", c, c - 12, 5);
        return a;
    }

    // ================================================================ the hanging gardens
    static BossArenaStructure hangingGardens(int size) {
        Pal p = new Pal(b("dirt"), b("grass_block"), b("moss_block"), b("stone_bricks"), b("stone_bricks"), b("mossy_stone_bricks"), b("chiseled_stone_bricks"), b("birch_log"),
                b("stripped_birch_log"), "stone_brick_slab", "stone_brick_stairs", b("light_blue_stained_glass"), b("ochre_froglight"), 13);
        BossArenaStructure a = Shell.start("hanging_gardens", size, 34, p, new Opts(1, 1, 8, true));
        ArenaRng r = ArenaRng.of("hanging_gardens", size);
        int c = a.c;
        Parts.lane(a, b("light_gray_carpet"));
        // terrace rings and flower beds
        for (int z = 3; z < size - 3; z++)
            for (int x = 3; x < size - 3; x++) {
                double d = dist(a, x, z);
                if (Math.abs(x - c) <= 2) continue;
                if (d > 13 && d <= 15) a.set(x, 1, z, b("moss_block"));
                if (d > 15 && d <= 15.8) a.set(x, 1, z, b("stone_bricks"));
                if (d > 13 && d <= 15 && r.chance(0.35)) a.set(x, 2, z, r.pick(b("poppy"), b("dandelion"), b("azure_bluet"), b("oxeye_daisy"), b("cornflower"), b("allium"), b("pink_petals")));
            }
        // four fountains
        for (int[] q : Parts.ringSpots(a, Math.min(17.0, c - 6.0), 4, Math.PI / 4)) {
            Parts.pool(a, q[0], q[1], 2.4, b("stone_bricks"), b("water"), b("chiseled_stone_bricks"));
            a.set(q[0], 1, q[1], b("chiseled_stone_bricks"));
            a.zone("bloom", q[0], q[1], 4);
        }
        // cherry and azalea trees
        Parts.scatter(a, r, size / 8, 4, 7, (x, z) -> {
            int h = 5 + r.next(3);
            for (int y = 1; y <= h; y++) a.set(x, y, z, b("cherry_log"));
            for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 2; dy++) {
                if (Math.abs(dx) + Math.abs(dz) + dy > 4 || (dx == 0 && dz == 0 && dy == 0)) continue;
                Parts.soft(a, x + dx, h + dy, z + dz, st("cherry_leaves", "persistent=true"));
            }
        });
        // thorn blooms: they burst into wisps
        for (int[] q : Parts.ringSpots(a, Math.min(11.0, c - 9.0), 8, 0.2)) {
            Parts.stack(a, Kind.BLOOM, Effect.ADDS, 1, q[0], q[1], st("flowering_azalea_leaves", "persistent=true"), st("flowering_azalea_leaves", "persistent=true"));
            a.zone("bloom", q[0], q[1], 3);
        }
        // hanging vines and lanterns from the roof
        for (int i = 0; i < size; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Draw.keepClear(a, x, z)) continue;
            int len = 3 + r.next(7);
            for (int y = a.height - 2; y > a.height - 2 - len; y--) a.set(x, y, z, st("cave_vines_plant", "berries=false"));
        }
        for (int i = 6; i < size - 6; i += 10) if (Math.abs(i - c) > 3) Parts.hanging(a, i, c, 6, b("chain"), st("lantern", "hanging=true"));
        a.zone("bloom", c, c + 12, 4);
        a.zone("bloom", c, c - 12, 4);
        return a;
    }

    // ================================================================ the sky bridge
    static BossArenaStructure skyBridge(int size) {
        Pal p = new Pal(b("quartz_bricks"), b("quartz_block"), b("smooth_quartz"), b("light_blue_concrete"), b("quartz_bricks"), b("smooth_quartz"), b("chiseled_quartz_block"), b("quartz_pillar"),
                b("chiseled_quartz_block"), "quartz_slab", "quartz_stairs", b("light_blue_stained_glass"), b("sea_lantern"), 15);
        BossArenaStructure a = Shell.start("sky_bridge", size, 38, p, new Opts(3, 1, 0, true));
        ArenaRng r = ArenaRng.of("sky_bridge", size);
        int c = a.c;
        // four sky pits between the arms of the bridge: three blocks down to a floor of cloud (the fall is harmless)
        for (int z = 3; z < size - 3; z++)
            for (int x = 3; x < size - 3; x++) {
                double d = dist(a, x, z);
                if (Math.abs(x - c) <= 3 || Math.abs(z - c) <= 3 || d <= 14 || d > c - 4.5) continue;
                for (int y = -2; y <= 0; y++) a.air(x, y, z);
                a.set(x, -3, z, b("white_wool"));
            }
        // rails of the bridge
        for (int i = 3; i < size - 3; i++) {
            if (Math.abs(i - c) <= 3 || dist(a, i, c) <= 14) continue;
            for (int s = -1; s <= 1; s += 2) {
                a.set(i, 1, c + s * 3, slab("quartz_slab", "bottom"));
                a.set(c + s * 3, 1, i, slab("quartz_slab", "bottom"));
            }
        }
        // wind towers at the corners of the central disc: they can be felled
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) Parts.pillar(a, c + sx * 11, c + sz * 11, 20, b("quartz_pillar"), b("chiseled_quartz_block"), b("gold_block"), true,
                slab("quartz_slab", "bottom"));
        // wind lanterns at the ends of the arms
        for (int[] q : new int[][] {{c, 5}, {c, size - 6}, {5, c}, {size - 6, c}}) {
            Parts.brazier(a, q[0] + 2, q[1] + 2, b("quartz_bricks"), st("lantern", "hanging=false"));
            Parts.brazier(a, q[0] - 2, q[1] - 2, b("quartz_bricks"), st("lantern", "hanging=false"));
            a.zone("gust", q[0], q[1], 5);
            a.spot(q[0] + 3, q[1] + 3);
            a.spot(q[0] - 3, q[1] - 3);
        }
        // clouds beyond the rails, in the pits
        for (int i = 0; i < size; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            double d = dist(a, x, z);
            if (Math.abs(x - c) > 4 && Math.abs(z - c) > 4 && d > 15 && d < c - 5) fill(a, x - 1, -3, z - 1, x + 1, -2, z + 1, b("white_wool"));
        }
        Parts.banners(a, "light_blue", 12, 12);
        a.zone("gust", c, c, 12);
        return a;
    }

    // ================================================================ the crystal sanctum
    static BossArenaStructure crystalSanctum(int size) {
        Pal p = new Pal(b("calcite"), b("calcite"), b("smooth_quartz"), b("amethyst_block"), b("calcite"), b("amethyst_block"), b("chiseled_quartz_block"), b("quartz_pillar"), b("amethyst_block"),
                "quartz_slab", "quartz_stairs", b("purple_stained_glass"), b("sea_lantern"), 13);
        BossArenaStructure a = Shell.start("crystal_sanctum", size, 34, p, new Opts(0, 1, 8, true));
        ArenaRng r = ArenaRng.of("crystal_sanctum", size);
        int c = a.c;
        // clusters growing from the floor and the walls
        for (int i = 0; i < size * 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Draw.keepClear(a, x, z)) continue;
            a.set(x, 1, z, st("amethyst_cluster", "facing=up"));
        }
        for (int i = 4; i < size - 4; i += 3) {
            if (Math.abs(i - c) <= 3) continue;
            a.set(i, 2 + r.next(8), 3, st("amethyst_cluster", "facing=south"));
            a.set(i, 2 + r.next(8), size - 4, st("amethyst_cluster", "facing=north"));
            a.set(3, 2 + r.next(8), i, st("amethyst_cluster", "facing=east"));
            a.set(size - 4, 2 + r.next(8), i, st("amethyst_cluster", "facing=west"));
        }
        // ten ringing crystals: they shatter into a shower of shards and wisps
        for (int[] q : Parts.ringSpots(a, Math.min(15.5, c - 6.0), 10, 0.15)) {
            Parts.single(a, Kind.CRYSTAL, Effect.SHATTER, 1, q[0], 1, q[1], b("amethyst_block"));
            for (int y = 2; y <= 4; y++) a.set(q[0], y, q[1], y == 4 ? st("amethyst_cluster", "facing=up") : b("amethyst_block"));
            a.zone("shard", q[0], q[1], 3);
        }
        // great spires of the sanctum: decorative, unbreakable
        for (int[] q : Parts.ringSpots(a, Math.min(20.0, c - 3.5), 6, 0.5)) Parts.spire(a, q[0], q[1], 1, 12 + r.next(6), b("amethyst_block"), st("amethyst_cluster", "facing=up"));
        // budding amethyst in the roof
        for (int i = 0; i < size / 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (!Draw.keepClear(a, x, z)) a.set(x, a.height - 2, z, b("budding_amethyst"));
        }
        a.zone("shard", c, c + 12, 4);
        a.zone("shard", c, c - 12, 4);
        return a;
    }

    // ================================================================ the moonlit glade
    static BossArenaStructure moonlitGlade(int size) {
        Pal p = new Pal(b("dirt"), b("grass_block"), b("moss_block"), b("stone_bricks"), b("deepslate_tiles"), b("deepslate_bricks"), b("chiseled_deepslate"), b("stone"), b("chiseled_stone_bricks"),
                "stone_brick_slab", "stone_brick_stairs", b("light_blue_stained_glass"), b("pearlescent_froglight"), 10);
        BossArenaStructure a = Shell.start("moonlit_glade", size, 30, p, new Opts(1, 0, 0, true));
        ArenaRng r = ArenaRng.of("moonlit_glade", size);
        int c = a.c;
        // hedges of leaves round the wall hide the stone
        for (int i = 3; i < size - 3; i++) {
            if (Math.abs(i - c) <= 3) continue;
            for (int y = 1; y <= 4; y++) {
                a.set(i, y, 2, st("azalea_leaves", "persistent=true"));
                a.set(i, y, size - 3, st("azalea_leaves", "persistent=true"));
                a.set(2, y, i, st("azalea_leaves", "persistent=true"));
                a.set(size - 3, y, i, st("azalea_leaves", "persistent=true"));
            }
        }
        // stars on the roof
        for (int i = 0; i < size * 2; i++) a.set(r.range(3, size - 4), a.height - 2, r.range(3, size - 4), b("end_rod"));
        // the ring of standing stones: tall, can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(15.5, c - 6.0), 8, 0.2)) Parts.pillar(a, q[0], q[1], 11, b("stone"), b("stone_bricks"), b("chiseled_stone_bricks"), false, slab("stone_slab", "bottom"));
        // a pond in the north, moon totems that wake as guardians, grass and flowers
        Parts.pool(a, c - 14, c + 12, 4.2, b("clay"), b("water"), b("mossy_cobblestone"));
        a.zone("moon", c - 14, c + 12, 5);
        for (int[] q : Parts.ringSpots(a, Math.min(19.0, c - 4.0), 4, Math.PI / 4)) {
            Parts.stack(a, Kind.TOTEM, Effect.ADDS, 1, q[0], q[1], b("mossy_stone_bricks"), b("stone_bricks"), b("chiseled_stone_bricks"), b("end_rod"));
            a.zone("moon", q[0], q[1], 3);
        }
        Parts.scatter(a, r, size / 3, 3, 2, (x, z) -> a.set(x, 1, z, r.pick(b("short_grass"), b("fern"), b("lily_of_the_valley"), b("azure_bluet"), b("blue_orchid"), b("oxeye_daisy"))));
        Parts.scatter(a, r, 4, 4, 9, (x, z) -> {
            int h = 6 + r.next(3);
            for (int y = 1; y <= h; y++) a.set(x, y, z, b("cherry_log"));
            for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 2; dy++) if (Math.abs(dx) + Math.abs(dz) + dy <= 4)
                Parts.soft(a, x + dx, h + dy, z + dz, st("cherry_leaves", "persistent=true"));
        });
        a.zone("moon", c + 12, c - 12, 5);
        return a;
    }

    // ================================================================ the storm spire
    static BossArenaStructure stormSpire(int size) {
        Pal p = new Pal(b("tuff_bricks"), b("waxed_oxidized_cut_copper"), b("waxed_weathered_cut_copper"), b("waxed_copper_block"), b("waxed_oxidized_cut_copper"), b("tuff_bricks"),
                b("waxed_chiseled_copper"), b("waxed_oxidized_cut_copper"), b("waxed_chiseled_copper"), "waxed_oxidized_cut_copper_slab", "waxed_oxidized_cut_copper_stairs", b("white_stained_glass"),
                b("sea_lantern"), 13);
        BossArenaStructure a = Shell.start("storm_spire", size, 40, p, new Opts(2, 0, 8, true));
        ArenaRng r = ArenaRng.of("storm_spire", size);
        int c = a.c;
        // lightning rod towers: they can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(16.0, c - 5.0), 8, 0.2)) {
            Parts.pillar(a, q[0], q[1], 16, st("lightning_rod", "facing=up"), b("waxed_cut_copper"), st("lightning_rod", "facing=up"), false, slab("waxed_cut_copper_slab", "bottom"));
            a.zone("bolt", q[0], q[1], 3);
        }
        // copper bulbs and rods along the walls
        for (int i = 5; i < size - 5; i += 4) {
            if (Math.abs(i - c) <= 3) continue;
            a.set(i, 6, 2, b("waxed_copper_block"));
            a.set(i, 7, 2, st("lightning_rod", "facing=up"));
            a.set(i, 6, size - 3, b("waxed_copper_block"));
            a.set(i, 7, size - 3, st("lightning_rod", "facing=up"));
        }
        // charged crystals that shatter into sparks
        for (int[] q : Parts.ringSpots(a, 12.0, 4, Math.PI / 4)) {
            Parts.single(a, Kind.CRYSTAL, Effect.SHATTER, 1, q[0], 1, q[1], b("waxed_copper_block"));
            a.set(q[0], 2, q[1], b("sea_lantern"));
            a.zone("bolt", q[0], q[1], 4);
        }
        // clouds under the roof, hanging chains
        for (int i = 0; i < size / 2; i++) {
            int x = r.range(4, size - 5), z = r.range(4, size - 5), y = a.height - 9 - r.next(5);
            fill(a, x - 2, y, z - 1, x + 2, y, z + 1, b("light_gray_wool"));
        }
        Parts.banners(a, "light_gray", 9, 12);
        a.zone("bolt", c, c + 12, 5);
        a.zone("bolt", c, c - 12, 5);
        return a;
    }
}
