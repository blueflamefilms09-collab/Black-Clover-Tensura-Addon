package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Opts;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Pal;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: eight halls belong to neither road alone: a forge can be a saint's or a devil's, a frozen keep a guardian's or a tyrant's, a flooded temple, a dragon's hoard, a colosseum, a hall of mirrors,
 * a clockwork vault and an observatory of the void serve either side. They are the halls the roster hands to both Holy and Evil bosses, so each is drawn neutral (no banners of a faith) and
 * differs by its shape and the element it offers: valves and smelter columns, ice pillars and frost crystals, drowned guardians, treasure urns and stalagmites, the colosseum's arcade, mirrors that
 * crack into shadows, copper pylons, void shards.
 */
final class ThemesShared {
    private ThemesShared() {}

    // ================================================================ the volcanic forge
    static BossArenaStructure volcanicForge(int size) {
        Pal p = new Pal(b("blackstone"), b("polished_blackstone_bricks"), b("smooth_basalt"), b("gilded_blackstone"), b("blackstone"), b("cracked_polished_blackstone_bricks"),
                b("chiseled_polished_blackstone"), b("polished_blackstone"), b("gilded_blackstone"), "polished_blackstone_brick_slab", "polished_blackstone_brick_stairs", b("orange_stained_glass"),
                b("shroomlight"), 13);
        BossArenaStructure a = Shell.start("volcanic_forge", size, 36, p, new Opts(0, 2, 8, true));
        ArenaRng r = ArenaRng.of("volcanic_forge", size);
        int c = a.c;
        // two channels of lava crossing the hall, a bridge on the lane
        int off = Math.min(12, c - 6);
        for (int zz : new int[] {c - off, c + off}) {
            for (int x = 4; x < size - 4; x++) {
                if (Math.abs(x - c) <= 3) continue;
                a.set(x, -1, zz, b("blackstone"));
                a.set(x, 0, zz, b("lava"));
                a.set(x, 0, zz - 1, b("obsidian"));
                a.set(x, 0, zz + 1, b("obsidian"));
            }
            a.zone("geyser", c - 10, zz, 4);
            a.zone("geyser", c + 10, zz, 4);
        }
        // hearths and anvils along the long walls
        for (int z = 7; z < size - 7; z += 5) {
            if (Math.abs(z - c) <= 3) continue;
            a.set(4, 1, z, st("blast_furnace", "facing=east,lit=true"));
            a.set(size - 5, 1, z, st("blast_furnace", "facing=west,lit=true"));
            a.set(5, 1, z + 1, st("anvil", "facing=north"));
            a.set(size - 6, 1, z + 1, st("anvil", "facing=north"));
        }
        // valves of the forge: broken, they let off their steam and the heat goes out of the hall for a moment
        for (int[] q : Parts.ringSpots(a, Math.min(13.5, c - 7.0), 6, 0.5)) {
            Parts.stack(a, Kind.VALVE, Effect.SHATTER, 0, q[0], q[1], b("iron_block"), b("chiseled_polished_blackstone"), b("iron_bars"));
            a.zone("geyser", q[0], q[1], 3);
        }
        // smelter columns: they can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(18.5, c - 4.5), 6, 0.2)) Parts.pillar(a, q[0], q[1], 20, b("polished_blackstone"), b("gilded_blackstone"), b("chiseled_polished_blackstone"), true,
                slab("polished_blackstone_slab", "bottom"));
        for (int[] q : Parts.ringSpots(a, 9.5, 4, 0.8)) Parts.brazier(a, q[0], q[1], b("blackstone"), st("lantern", "hanging=false"));
        for (int i = 6; i < size - 6; i += 8) if (Math.abs(i - c) > 3) Parts.hanging(a, i, c - 4, 9, b("chain"), b("lava_cauldron"));
        Parts.banners(a, "orange", 10, 14);
        a.zone("forge", c, c + 14, 4);
        return a;
    }

    // ================================================================ the frozen keep
    static BossArenaStructure frozenKeep(int size) {
        Pal p = new Pal(b("stone_bricks"), b("snow_block"), b("white_terracotta"), b("blue_ice"), b("stone_bricks"), b("packed_ice"), b("chiseled_stone_bricks"), b("packed_ice"), b("blue_ice"),
                "stone_brick_slab", "stone_brick_stairs", b("light_blue_stained_glass"), b("sea_lantern"), 11);
        BossArenaStructure a = Shell.start("frozen_keep", size, 34, p, new Opts(1, 0, 8, true));
        ArenaRng r = ArenaRng.of("frozen_keep", size);
        int c = a.c;
        Parts.lane(a, b("light_blue_carpet"));
        // drifts of snow
        for (int i = 0; i < size * 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Math.abs(x - c) > 2 && dist(a, x, z) > 9) a.set(x, 1, z, st("snow", "layers=" + (1 + r.next(3))));
        }
        // ice pillars: they can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(14.0, c - 6.0), 8, 0.2)) Parts.pillar(a, q[0], q[1], 17, b("packed_ice"), b("blue_ice"), b("blue_ice"), true, slab("stone_brick_slab", "bottom"));
        // frost crystals
        for (int[] q : Parts.ringSpots(a, Math.min(18.5, c - 4.5), 6, 0.6)) {
            Parts.single(a, Kind.CRYSTAL, Effect.SHATTER, 1, q[0], 1, q[1], b("blue_ice"));
            a.set(q[0], 2, q[1], b("blue_ice"));
            a.set(q[0], 3, q[1], st("amethyst_cluster", "facing=up"));
            a.zone("frost", q[0], q[1], 4);
        }
        // icicles hanging from the roof
        for (int i = 0; i < size; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Draw.keepClear(a, x, z)) continue;
            int len = 2 + r.next(4);
            for (int k = 0; k < len; k++) a.set(x, a.height - 2 - k, z, k == len - 1 ? st("pointed_dripstone", "thickness=tip,vertical_direction=down") : b("packed_ice"));
        }
        // frost braziers in the corners, spruce beams under the roof
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) Parts.brazier(a, c + sx * (c - 5), c + sz * (c - 5), b("stone_bricks"), st("soul_lantern", "hanging=false"));
        Parts.banners(a, "light_blue", 9, 12);
        a.zone("frost", c, c + 12, 5);
        a.zone("frost", c, c - 12, 5);
        return a;
    }

    // ================================================================ the sunken temple
    static BossArenaStructure sunkenTemple(int size) {
        Pal p = new Pal(b("dark_prismarine"), b("prismarine_bricks"), b("dark_prismarine"), b("prismarine"), b("prismarine_bricks"), b("prismarine"), b("dark_prismarine"), b("prismarine"),
                b("dark_prismarine"), "prismarine_brick_slab", "prismarine_brick_stairs", b("cyan_stained_glass"), b("sea_lantern"), 11);
        BossArenaStructure a = Shell.start("sunken_temple", size, 32, p, new Opts(0, 2, 8, true));
        ArenaRng r = ArenaRng.of("sunken_temple", size);
        int c = a.c;
        // four flooded corners
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int x = c + sx * (c - 10), z = c + sz * (c - 10);
            Parts.pool(a, x, z, 5.5, b("prismarine"), b("water"), b("dark_prismarine"));
            a.set(x, 0, z + 1, b("seagrass"));
            a.zone("tide", x, z, 5);
        }
        // columns of the temple: they can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(15.5, c - 6.0), 6, 0.5)) Parts.pillar(a, q[0], q[1], 18, b("prismarine"), b("dark_prismarine"), b("sea_lantern"), true, slab("prismarine_brick_slab", "bottom"));
        // drowned guardians in niches
        for (int[] q : Parts.ringSpots(a, Math.min(19.0, c - 4.0), 4, Math.PI / 4)) {
            Parts.stack(a, Kind.STATUE, Effect.ADDS, 2, q[0], q[1], b("dark_prismarine"), b("prismarine_bricks"), b("prismarine_bricks"), b("prismarine"));
            a.zone("tide", q[0], q[1], 3);
        }
        // coral on the walls, lanterns of the sea on chains
        for (int i = 4; i < size - 4; i += 5) {
            if (Math.abs(i - c) <= 3) continue;
            String coral = r.pick(b("tube_coral_block"), b("brain_coral_block"), b("bubble_coral_block"), b("fire_coral_block"), b("horn_coral_block"));
            a.set(i, 1, 3, coral);
            a.set(i, 1, size - 4, coral);
            a.set(3, 1, i, coral);
            a.set(size - 4, 1, i, coral);
        }
        for (int i = 6; i < size - 6; i += 8) if (Math.abs(i - c) > 3) Parts.hanging(a, i, c + 6, 7, b("chain"), b("sea_lantern"));
        a.zone("tide", c, c + 12, 5);
        a.zone("tide", c, c - 12, 5);
        return a;
    }

    // ================================================================ the dragon's hoard
    static BossArenaStructure dragonHoardCavern(int size) {
        Pal p = new Pal(b("stone"), b("stone"), b("andesite"), b("raw_gold_block"), b("stone"), b("andesite"), b("tuff"), b("stone"), b("deepslate"), "stone_slab", "stone_stairs", b("orange_stained_glass"),
                b("ochre_froglight"), 10);
        BossArenaStructure a = Shell.start("dragon_hoard_cavern", size, 40, p, new Opts(1, 0, 0, true));
        ArenaRng r = ArenaRng.of("dragon_hoard_cavern", size);
        int c = a.c;
        // the ribs of a long dead dragon along the west wall
        for (int k = 0; k < 5; k++) {
            int z = c - 12 + k * 6;
            if (Math.abs(z - c) < 4) continue;
            for (int y = 1; y <= 9; y++) {
                a.set(4, y, z, b("bone_block"));
                a.set(4 + (y > 6 ? y - 6 : 0), y, z, b("bone_block"));
            }
        }
        // mounds of gold in the four quarters
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int mx = c + sx * (c - 12), mz = c + sz * (c - 12);
            for (int dz = -7; dz <= 7; dz++) for (int dx = -7; dx <= 7; dx++) {
                double d = Math.hypot(dx, dz);
                if (Draw.keepClear(a, mx + dx, mz + dz)) continue;
                int h = (int) Math.round(5 - d * 0.7 + r.d() * 1.2);
                for (int y = 1; y <= h; y++) a.set(mx + dx, y, mz + dz, y == h && r.chance(0.2) ? r.pick(b("emerald_block"), b("diamond_block"), b("raw_gold_block")) : b("gold_block"));
            }
            a.zone("hoard", mx, mz, 6);
        }
        // stalagmites: they can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(17.0, c - 5.0), 8, 0.35)) Parts.pillar(a, q[0], q[1], 14, b("stone"), b("cobblestone"), b("stone"), true, slab("stone_slab", "bottom"));
        // treasure urns among the coins: they burst into loot thieves
        Parts.scatter(a, r, 8, 5, 6, (x, z) -> {
            Parts.single(a, Kind.URN, Effect.ADDS, 1, x, 1, z, b("decorated_pot"));
            a.zone("hoard", x, z, 3);
        });
        // stalactites from the roof, the ribs of a dead dragon
        for (int i = 0; i < size; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Draw.keepClear(a, x, z)) continue;
            int len = 2 + r.next(6);
            for (int k = 0; k < len; k++) a.set(x, a.height - 2 - k, z, k == len - 1 ? st("pointed_dripstone", "thickness=tip,vertical_direction=down") : b("dripstone_block"));
        }
        a.zone("hoard", c, c + 12, 5);
        return a;
    }

    // ================================================================ the colosseum
    static BossArenaStructure colosseum(int size) {
        Pal p = new Pal(b("sandstone"), b("smooth_sandstone"), b("cut_sandstone"), b("red_sandstone"), b("sandstone"), b("cut_sandstone"), b("chiseled_sandstone"), b("cut_sandstone"),
                b("chiseled_sandstone"), "cut_sandstone_slab", "sandstone_stairs", b("yellow_stained_glass"), b("glowstone"), 13);
        BossArenaStructure a = Shell.start("colosseum", size, 38, p, new Opts(1, 1, 0, true));
        ArenaRng r = ArenaRng.of("colosseum", size);
        int c = a.c;
        double seatIn = c - 8.0, seatOut = c - 3.0;
        // the stands: three rising rings of stairs round the pit, a wall in front, gates through them on the axes
        for (int z = 3; z < size - 3; z++)
            for (int x = 3; x < size - 3; x++) {
                double d = dist(a, x, z);
                if (Math.abs(x - c) <= 3 || Math.abs(z - c) <= 3) continue;
                if (d > seatIn - 1 && d <= seatIn) for (int y = 1; y <= 3; y++) a.set(x, y, z, b("sandstone"));
                else if (d > seatIn && d <= seatIn + 1.7) { fill(a, x, 1, z, x, 1, z, b("cut_sandstone")); a.set(x, 2, z, stair("sandstone_stairs", opposite(towardsCentre(a, x, z)), "bottom")); }
                else if (d > seatIn + 1.7 && d <= seatIn + 3.4) { fill(a, x, 1, z, x, 2, z, b("cut_sandstone")); a.set(x, 3, z, stair("sandstone_stairs", opposite(towardsCentre(a, x, z)), "bottom")); }
                else if (d > seatIn + 3.4 && d <= seatOut + 2) { fill(a, x, 1, z, x, 3, z, b("cut_sandstone")); a.set(x, 4, z, stair("sandstone_stairs", opposite(towardsCentre(a, x, z)), "bottom")); }
            }
        // the arcade in front of the stands: eight columns that can be felled
        for (int[] q : Parts.ringSpots(a, seatIn - 2.0, 8, Math.PI / 8)) Parts.pillar(a, q[0], q[1], 12, b("cut_sandstone"), b("chiseled_sandstone"), b("chiseled_sandstone"), false, slab("cut_sandstone_slab", "bottom"));
        // gladiator statues in the stands, braziers on the rim of the pit
        for (int[] q : Parts.ringSpots(a, seatIn + 1.5, 4, Math.PI / 4)) Parts.stack(a, Kind.STATUE, Effect.ADDS, 1, q[0], q[1] , b("chiseled_sandstone"), b("cut_sandstone"), b("cut_sandstone"), b("chiseled_sandstone"));
        for (int[] q : Parts.ringSpots(a, seatIn - 4.0, 8, 0.0)) {
            Parts.brazier(a, q[0], q[1], b("sandstone"), st("lantern", "hanging=false"));
            a.zone("sand", q[0], q[1], 4);
        }
        Parts.banners(a, "red", 9, 8);
        Parts.banners(a, "yellow", 16, 16);
        a.zone("sand", c, c + 11, 5);
        a.zone("sand", c, c - 11, 5);
        return a;
    }

    // ================================================================ the hall of mirrors
    static BossArenaStructure mirrorHall(int size) {
        Pal p = new Pal(b("quartz_bricks"), b("polished_diorite"), b("smooth_quartz"), b("light_gray_concrete"), b("quartz_bricks"), b("smooth_quartz"), b("chiseled_quartz_block"), b("quartz_pillar"),
                b("gold_block"), "quartz_slab", "quartz_stairs", b("glass"), b("sea_lantern"), 13);
        BossArenaStructure a = Shell.start("mirror_hall", size, 34, p, new Opts(0, 2, 8, true));
        ArenaRng r = ArenaRng.of("mirror_hall", size);
        int c = a.c;
        // mirror panels fill the inner wall layer between the pilasters
        for (int i = 5; i < size - 5; i += 6) {
            if (Math.abs(i - c) <= 3) continue;
            for (int y = 3; y <= 12; y++) {
                fill(a, i - 1, y, 1, i + 1, y, 1, b("light_gray_stained_glass"));
                fill(a, i - 1, y, size - 2, i + 1, y, size - 2, b("light_gray_stained_glass"));
                fill(a, 1, y, i - 1, 1, y, i + 1, b("light_gray_stained_glass"));
                fill(a, size - 2, y, i - 1, size - 2, y, i + 1, b("light_gray_stained_glass"));
            }
        }
        // free-standing mirrors: they crack into shadows
        for (int[] q : Parts.ringSpots(a, Math.min(15.0, c - 5.0), 8, 0.2)) {
            Parts.stack(a, Kind.MIRROR, Effect.ADDS, 1, q[0], q[1], b("tinted_glass"), b("tinted_glass"), b("tinted_glass")).bossShare = 0.01;
            a.set(q[0], 4, q[1], b("gold_block"));
            a.zone("shard", q[0], q[1], 3);
        }
        // prism chandeliers
        for (int s = -1; s <= 1; s += 2) for (int z = c - 12; z <= c + 12; z += 12) if (z != c) Parts.chandelier(a, c + s * 6, z, b("chain"), b("light_gray_stained_glass"));
        for (int[] q : Parts.ringSpots(a, 10.5, 8, 0.4)) a.set(q[0], 1, q[1], b("glass"));
        Parts.banners(a, "white", 14, 12);
        a.zone("shard", c, c + 12, 5);
        a.zone("shard", c, c - 12, 5);
        return a;
    }

    // ================================================================ the clockwork vault
    static BossArenaStructure clockworkVault(int size) {
        Pal p = new Pal(b("deepslate_bricks"), b("waxed_cut_copper"), b("waxed_exposed_cut_copper"), b("iron_block"), b("waxed_exposed_cut_copper"), b("waxed_cut_copper"), b("waxed_chiseled_copper"),
                b("waxed_copper_block"), b("iron_block"), "waxed_cut_copper_slab", "waxed_cut_copper_stairs", b("light_blue_stained_glass"), b("sea_lantern"), 11);
        BossArenaStructure a = Shell.start("clockwork_vault", size, 36, p, new Opts(0, 2, 8, true));
        ArenaRng r = ArenaRng.of("clockwork_vault", size);
        int c = a.c;
        // great gears on the long walls: a ring with eight spokes
        for (int gz : new int[] {c - 12, c + 12}) {
            for (int s = 0; s < 2; s++) {
                int x = s == 0 ? 2 : size - 3;
                int cy = 12;
                for (int dy = -7; dy <= 7; dy++) for (int dz = -7; dz <= 7; dz++) {
                    double d = Math.hypot(dy, dz);
                    boolean tooth = d > 6.2 && d <= 7.4 && ((int) Math.round(Math.atan2(dy, dz) * 8 / Math.PI) & 1) == 0;
                    if ((d > 5 && d <= 6.2) || tooth || (d <= 1.4) || (d <= 5 && (Math.abs(dy) <= 0 || Math.abs(dz) <= 0 || Math.abs(Math.abs(dy) - Math.abs(dz)) <= 0)))
                        a.set(x, cy + dy, gz + dz, b("iron_block"));
                }
            }
        }
        // steam valves: broken, they stop venting
        for (int[] q : Parts.ringSpots(a, Math.min(13.0, c - 7.0), 6, 0.3)) {
            Parts.stack(a, Kind.VALVE, Effect.SHATTER, 0, q[0], q[1], b("iron_block"), b("waxed_chiseled_copper"), b("iron_bars"));
            a.zone("vent", q[0], q[1], 3);
        }
        // copper pylons: they can be felled
        for (int[] q : Parts.ringSpots(a, Math.min(18.0, c - 5.0), 6, 0.6)) Parts.pillar(a, q[0], q[1], 22, b("waxed_copper_block"), b("iron_block"), b("waxed_chiseled_copper"), true, slab("waxed_cut_copper_slab", "bottom"));
        // pistons and observers in the walls, hanging gears
        for (int i = 5; i < size - 5; i += 4) {
            if (Math.abs(i - c) <= 3) continue;
            a.set(i, 3, 2, st("piston", "facing=south,extended=false"));
            a.set(i, 3, size - 3, st("piston", "facing=north,extended=false"));
            a.set(2, 3, i, st("observer", "facing=east"));
            a.set(size - 3, 3, i, st("observer", "facing=west"));
        }
        for (int[] q : Parts.ringSpots(a, 9.0, 4, 0.8)) {
            Parts.hanging(a, q[0], q[1], 5, b("chain"), b("iron_block"));
            ring(a, q[0], q[1], 2.0, 3.0, a.height - 8, b("iron_block"));
        }
        Parts.banners(a, "light_gray", 9, 12);
        a.zone("vent", c, c + 12, 4);
        a.zone("vent", c, c - 12, 4);
        return a;
    }

    // ================================================================ the observatory of the void
    static BossArenaStructure voidObservatory(int size) {
        Pal p = new Pal(b("obsidian"), b("black_concrete"), b("obsidian"), b("purpur_block"), b("end_stone_bricks"), b("purpur_block"), b("chiseled_polished_blackstone"), b("purpur_pillar"),
                b("purpur_block"), "purpur_slab", "purpur_stairs", b("black_stained_glass"), b("sea_lantern"), 8);
        BossArenaStructure a = Shell.start("void_observatory", size, 38, p, new Opts(3, 1, 8, true));
        ArenaRng r = ArenaRng.of("void_observatory", size);
        int c = a.c;
        // stars in the floor
        for (int i = 0; i < size * 4; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Math.abs(x - c) > 1) a.set(x, 0, z, r.chance(0.7) ? b("light_gray_concrete") : b("white_concrete"));
        }
        // the orrery: three orbits of gold orbs on end rods round the altar
        for (int orbit = 0; orbit < 3; orbit++) {
            double rad = 11 + orbit * 3.0;
            int y = 8 + orbit * 3;
            for (int[] q : Parts.ringSpots(a, Math.min(rad, c - 4.0), 4 + orbit * 2, orbit)) {
                a.set(q[0], y, q[1], b("gold_block"));
                a.set(q[0], y + 1, q[1], b("end_rod"));
            }
        }
        // two telescopes
        for (int s = -1; s <= 1; s += 2) {
            int x = c + s * (c - 9), z = c + 6;
            fill(a, x - 1, 1, z - 1, x + 1, 1, z + 1, b("polished_blackstone"));
            a.set(x, 2, z, b("dark_prismarine"));
            for (int k = 0; k < 9; k++) a.set(x - s * k, 3 + k, z, b("dark_prismarine"));
            a.set(x - s * 9, 12, z, b("end_rod"));
        }
        // void shards: they burst into a rush of endermites
        for (int[] q : Parts.ringSpots(a, Math.min(16.0, c - 5.0), 6, 0.2)) {
            Parts.single(a, Kind.CRYSTAL, Effect.SHATTER, 1, q[0], 1, q[1], b("black_concrete"));
            a.set(q[0], 2, q[1], b("purple_stained_glass"));
            a.set(q[0], 3, q[1], b("end_rod"));
            a.zone("void", q[0], q[1], 4);
        }
        Parts.banners(a, "black", 12, 14);
        a.zone("void", c, c + 12, 5);
        a.zone("void", c, c - 12, 5);
        return a;
    }
}
