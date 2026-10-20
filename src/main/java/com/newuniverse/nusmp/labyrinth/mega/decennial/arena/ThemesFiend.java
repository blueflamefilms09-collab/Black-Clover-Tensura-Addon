package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Element;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Opts;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Pal;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: the halls of the pit and the hollow: the hell pit (lava pools and soul cages), the ring of the abyssal rift, the witch's hollow tree, the plague swamp, the spider's den and the mind
 * flayers' hive. They share the cruel end of the dungeon's palette; what tells them apart is their geometry (a trench with four bridges, a hollow trunk, a bog of pools, a web of silk, a brain
 * of pods) and the interactive element each one hands the party: soul cages that burst into imps, rift shards, hag totems that weaken the boss when felled, spore pods, egg sacs, ganglia.
 */
final class ThemesFiend {
    private ThemesFiend() {}

    // ================================================================ the hell pit
    static BossArenaStructure hellPit(int size) {
        Pal p = new Pal(b("blackstone"), b("polished_blackstone_bricks"), b("blackstone"), b("crying_obsidian"), b("nether_bricks"), b("cracked_nether_bricks"), b("chiseled_nether_bricks"),
                b("nether_bricks"), b("chiseled_nether_bricks"), "nether_brick_slab", "nether_brick_stairs", b("orange_stained_glass"), b("shroomlight"), 12);
        BossArenaStructure a = Shell.start("hell_pit", size, 34, p, new Opts(2, 2, 8, true));
        ArenaRng r = ArenaRng.of("hell_pit", size);
        int c = a.c;
        // four pools of lava, rimmed in blackstone, one in each corner of the pit
        for (int[] q : Parts.ringSpots(a, Math.min(14.5, c - 8.0), 4, Math.PI / 4)) {
            Parts.pool(a, q[0], q[1], 3.6, b("blackstone"), b("lava"), b("blackstone"));
            a.zone("geyser", q[0], q[1], 4);
        }
        // basalt columns standing in the heat
        Parts.scatter(a, r, size / 6, 4, 5, (x, z) -> {
            int h = 3 + r.next(4);
            for (int y = 1; y <= h; y++) a.set(x, y, z, axis("basalt", "y"));
        });
        // eight braziers of hellfire on a ring
        for (int[] q : Parts.ringSpots(a, Math.min(18.0, c - 5.0), 8, 0.4)) Parts.brazier(a, q[0], q[1], b("nether_bricks"), st("lantern", "hanging=false"));
        // soul cages: an iron cage with a soul in it, the imps get out when it breaks
        for (int[] q : Parts.ringSpots(a, Math.min(11.5, c - 9.0), 6, 0.75)) {
            Element e = a.beginElement(Kind.URN, Effect.ADDS, 2);
            for (int y = 1; y <= 3; y++) a.set(q[0], y, q[1], y == 2 ? b("soul_lantern") : b("iron_bars"));
            a.endElement();
            a.spot(q[0], q[1]);
            a.zone("geyser", q[0], q[1], 3);
        }
        // chained cages hanging from the roof
        for (int[] q : Parts.ringSpots(a, 9.0, 6, 0.2)) Parts.hanging(a, q[0], q[1], 6 + r.next(6), b("chain"), b("iron_bars"));
        Parts.banners(a, "red", 9, 8);
        Parts.banners(a, "black", 14, 16);
        a.zone("geyser", c, c + 12, 4);
        a.zone("geyser", c, c - 12, 4);
        return a;
    }

    // ================================================================ the ring of the abyssal rift
    static BossArenaStructure abyssRiftRing(int size) {
        Pal p = new Pal(b("deepslate"), b("deepslate_tiles"), b("polished_deepslate"), b("crying_obsidian"), b("polished_blackstone"), b("cracked_polished_blackstone_bricks"), b("chiseled_polished_blackstone"),
                b("polished_blackstone"), b("chiseled_polished_blackstone"), "polished_blackstone_slab", "polished_blackstone_stairs", b("purple_stained_glass"), b("crying_obsidian"), 7);
        BossArenaStructure a = Shell.start("abyss_rift_ring", size, 36, p, new Opts(0, 0, 8, true));
        ArenaRng r = ArenaRng.of("abyss_rift_ring", size);
        int c = a.c;
        double in = 11.5, out = Math.min(19.5, c - 5.0);
        // the rift: a trench round the middle, three blocks deep, crossed by four bridges that follow the axes
        for (int z = 3; z < size - 3; z++)
            for (int x = 3; x < size - 3; x++) {
                double d = dist(a, x, z);
                if (d <= in || d > out) continue;
                if (Math.abs(x - c) <= 2 || Math.abs(z - c) <= 2) continue;                    // the bridges
                for (int y = -2; y <= 0; y++) a.air(x, y, z);
                a.set(x, -3, z, r.chance(0.25) ? b("crying_obsidian") : b("sculk"));
                if (r.chance(0.05)) a.set(x, -2, z, b("sculk_vein"));
            }
        // stairs up out of the rift at eight places
        for (int[] q : Parts.ringSpots(a, in + 0.5, 8, 0.4)) a.set(q[0], -2, q[1], stair("polished_blackstone_stairs", towardsCentre(a, q[0], q[1]), "bottom"));
        for (int[] q : Parts.ringSpots(a, out - 0.5, 8, 0.4 + Math.PI / 8)) a.set(q[0], -2, q[1], stair("polished_blackstone_stairs", opposite(towardsCentre(a, q[0], q[1])), "bottom"));
        // rift shards on the outer ring: they shatter into imps
        for (int[] q : Parts.ringSpots(a, out + 2.5, 8, 0.2)) {
            Element e = a.beginElement(Kind.CRYSTAL, Effect.SHATTER, 2);
            a.set(q[0], 1, q[1], b("amethyst_block"));
            a.set(q[0], 2, q[1], st("amethyst_cluster", "facing=up"));
            a.endElement();
            a.spot(q[0], q[1]);
            a.zone("rift", q[0], q[1], 3);
        }
        // shards of obsidian floating above the trench
        for (int i = 0; i < size / 3; i++) {
            double ang = r.d() * Math.PI * 2, rad = in + 1 + r.d() * (out - in - 2);
            int x = (int) Math.round(c + Math.cos(ang) * rad), z = (int) Math.round(c + Math.sin(ang) * rad), y = 3 + r.next(8);
            a.set(x, y, z, r.chance(0.3) ? b("crying_obsidian") : b("obsidian"));
            if (r.chance(0.4)) a.set(x, y + 1, z, b("obsidian"));
        }
        for (int[] q : Parts.ringSpots(a, (in + out) / 2.0, 8, 0.0)) a.zone("rift", q[0], q[1], 4);
        Parts.banners(a, "purple", 10, 12);
        return a;
    }

    // ================================================================ the witch's hollow
    static BossArenaStructure witchHollow(int size) {
        Pal p = new Pal(b("mud"), b("moss_block"), b("podzol"), b("mud_bricks"), b("dark_oak_log"), b("mangrove_roots"), b("stripped_dark_oak_log"), b("dark_oak_log"), b("stripped_dark_oak_log"),
                "dark_oak_slab", "dark_oak_stairs", b("lime_stained_glass"), b("shroomlight"), 7);
        BossArenaStructure a = Shell.start("witch_hollow", size, 28, p, new Opts(1, 0, 7, true));
        ArenaRng r = ArenaRng.of("witch_hollow", size);
        int c = a.c;
        // the ring of cauldrons round the dais
        for (int[] q : Parts.ringSpots(a, 11.0, 4, Math.PI / 4)) {
            a.set(q[0], 1, q[1], st("water_cauldron", "level=3"));
            a.zone("cauldron", q[0], q[1], 3);
        }
        // hag totems: felling one weakens the boss and wakes what it held
        for (int[] q : Parts.ringSpots(a, Math.min(17.0, c - 6.0), 6, 0.3)) {
            Element e = Parts.stack(a, Kind.TOTEM, Effect.SHATTER, 1, q[0], q[1], b("dark_oak_log"), b("dark_oak_log"), b("carved_pumpkin"), b("skeleton_skull"));
            e.bossShare = 0.02;
            a.zone("cauldron", q[0], q[1], 3);
        }
        // rings of mushrooms and bottles, roots and vines coming down
        Parts.scatter(a, r, size / 5, 3, 3, (x, z) -> {
            a.set(x, 1, z, r.chance(0.5) ? b("red_mushroom") : b("brown_mushroom"));
            a.set(x + 1, 1, z, b("brown_mushroom"));
        });
        for (int i = 0; i < size; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Draw.keepClear(a, x, z)) continue;
            int len = 2 + r.next(8);
            for (int y = a.height - 2; y > a.height - 2 - len; y--) a.set(x, y, z, y == a.height - 1 - len ? st("cave_vines", "age=25,berries=true") : st("cave_vines_plant", "berries=true"));
        }
        // gnarled roots along the walls
        for (int i = 4; i < size - 4; i += 5) {
            if (Math.abs(i - c) <= 3) continue;
            for (int k = 0; k < 3; k++) {
                a.set(i, 1 + k, 3, b("mangrove_roots"));
                a.set(i, 1 + k, size - 4, b("mangrove_roots"));
                a.set(3, 1 + k, i, b("mangrove_roots"));
                a.set(size - 4, 1 + k, i, b("mangrove_roots"));
            }
        }
        // webs between the trunks
        for (int i = 0; i < size / 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (!Draw.keepClear(a, x, z)) a.set(x, 4 + r.next(5), z, b("cobweb"));
        }
        a.zone("bog", c - 14, c + 6, 4);
        a.zone("bog", c + 14, c - 6, 4);
        return a;
    }

    // ================================================================ the plague swamp
    static BossArenaStructure plagueSwamp(int size) {
        Pal p = new Pal(b("mud"), b("mud"), b("moss_block"), b("packed_mud"), b("mud_bricks"), b("moss_block"), b("packed_mud"), b("mangrove_log"), b("stripped_mangrove_log"), "mud_brick_slab",
                "mud_brick_stairs", b("green_stained_glass"), b("verdant_froglight"), 9);
        BossArenaStructure a = Shell.start("plague_swamp", size, 30, p, new Opts(1, 0, 8, true));
        ArenaRng r = ArenaRng.of("plague_swamp", size);
        int c = a.c;
        // pools of foul water with lily pads
        for (int[] q : Parts.ringSpots(a, Math.min(15.0, c - 7.0), 5, 0.5)) {
            Parts.pool(a, q[0], q[1], 3.4, b("mud"), b("water"), b("mangrove_roots"));
            a.set(q[0] + 1, 1, q[1], b("lily_pad"));
            a.zone("bog", q[0], q[1], 4);
        }
        // dead mangroves with their roots
        Parts.scatter(a, r, size / 7, 4, 6, (x, z) -> {
            int h = 5 + r.next(4);
            for (int y = 1; y <= h; y++) a.set(x, y, z, b("mangrove_log"));
            a.set(x + 1, 1, z, b("mangrove_roots"));
            a.set(x - 1, 1, z, b("mangrove_roots"));
            a.set(x, 1, z + 1, b("mangrove_roots"));
            a.set(x, h, z + 1, b("mangrove_log"));
            a.set(x + 1, h + 1, z + 1, b("mangrove_log"));
            a.set(x, h + 1, z - 1, b("vine"));
        });
        // spore pods: great mushrooms that burst into a cloud and let the rot out
        for (int[] q : Parts.ringSpots(a, Math.min(19.0, c - 4.5), 8, 0.2)) {
            Element e = a.beginElement(Kind.SHROOM, Effect.SHATTER, 2);
            for (int y = 1; y <= 3; y++) a.set(q[0], y, q[1], b("mushroom_stem"));
            fill(a, q[0] - 1, 4, q[1] - 1, q[0] + 1, 4, q[1] + 1, b("brown_mushroom_block"));
            a.set(q[0], 5, q[1], b("brown_mushroom_block"));
            a.endElement();
            a.spot(q[0], q[1]);
            a.zone("bog", q[0], q[1], 4);
        }
        // barrels and bones, a rotting cart
        Parts.scatter(a, r, size / 9, 3, 6, (x, z) -> {
            a.set(x, 1, z, b("barrel"));
            a.set(x + 1, 1, z, b("bone_block"));
        });
        Parts.banners(a, "lime", 9, 12);
        return a;
    }

    // ================================================================ the spider's den
    static BossArenaStructure spiderDen(int size) {
        Pal p = new Pal(b("deepslate"), b("blackstone"), b("deepslate_tiles"), b("polished_blackstone"), b("tuff_bricks"), b("tuff"), b("chiseled_tuff"), b("polished_tuff"), b("chiseled_tuff"),
                "tuff_brick_slab", "tuff_brick_stairs", b("gray_stained_glass"), b("sea_lantern"), 6);
        BossArenaStructure a = Shell.start("spider_den", size, 30, p, new Opts(1, 0, 8, true));
        ArenaRng r = ArenaRng.of("spider_den", size);
        int c = a.c;
        // silk pillars wrapped in webs
        for (int[] q : Parts.ringSpots(a, Math.min(16.0, c - 6.0), 8, 0.4)) {
            for (int y = 1; y <= 24; y++) a.set(q[0], y, q[1], y % 5 == 0 ? b("white_wool") : b("polished_tuff"));
            for (int k = 0; k < 6; k++) a.set(q[0] + r.next(3) - 1, 3 + k * 3, q[1] + r.next(3) - 1, b("cobweb"));
        }
        // egg sacs: they burst into spiderlings
        Parts.scatter(a, r, size / 4, 3, 5, (x, z) -> {
            Element e = a.beginElement(Kind.EGG_SAC, Effect.ADDS, 3);
            a.set(x, 1, z, b("white_wool"));
            a.set(x, 2, z, b("white_wool"));
            a.set(x, 3, z, b("cobweb"));
            a.endElement();
            a.spot(x, z);
            a.zone("nest", x, z, 3);
        });
        // webs: a cloud of them in the outer ring, none on the lane or round the altar
        for (int i = 0; i < size * 3; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4), y = 1 + r.next(6);
            if (!Draw.keepClear(a, x, z) && dist(a, x, z) > 12) Parts.soft(a, x, y, z, b("cobweb"));
        }
        // cocoons hanging from the roof
        for (int i = 0; i < size / 3; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (!Draw.keepClear(a, x, z)) Parts.hanging(a, x, z, 4 + r.next(6), b("chain"), b("white_wool"));
        }
        a.zone("web", c - 13, c + 5, 5);
        a.zone("web", c + 13, c - 5, 5);
        return a;
    }

    // ================================================================ the mind flayers' hive
    static BossArenaStructure mindHive(int size) {
        Pal p = new Pal(b("deepslate"), b("purple_terracotta"), b("magenta_terracotta"), b("crying_obsidian"), b("purple_terracotta"), b("magenta_terracotta"), b("chiseled_polished_blackstone"),
                b("mushroom_stem"), b("purpur_block"), "purpur_slab", "purpur_stairs", b("magenta_stained_glass"), b("shroomlight"), 8);
        BossArenaStructure a = Shell.start("mind_hive", size, 32, p, new Opts(2, 0, 8, true));
        ArenaRng r = ArenaRng.of("mind_hive", size);
        int c = a.c;
        // brain pods on stalks: ganglia of the hive, felling one stuns the hive's mind (a share of the boss's health) and frees thralls
        for (int[] q : Parts.ringSpots(a, Math.min(15.0, c - 6.0), 6, 0.2)) {
            Element e = a.beginElement(Kind.SHROOM, Effect.ADDS, 2);
            for (int y = 1; y <= 4; y++) a.set(q[0], y, q[1], b("mushroom_stem"));
            fill(a, q[0] - 1, 5, q[1] - 1, q[0] + 1, 7, q[1] + 1, b("brain_coral_block"));
            a.endElement();
            e.bossShare = 0.015;
            a.spot(q[0], q[1]);
            a.zone("psi", q[0], q[1], 4);
        }
        // fungal trees
        Parts.scatter(a, r, size / 6, 4, 5, (x, z) -> {
            int h = 5 + r.next(5);
            for (int y = 1; y <= h; y++) Parts.soft(a, x, y, z, b("mushroom_stem"));
            for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
                Parts.soft(a, x + dx, h + 1, z + dz, b("red_mushroom_block"));
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) Parts.soft(a, x + dx, h + 2, z + dz, b("red_mushroom_block"));
            }
        });
        // veins of crying obsidian and amethyst in the floor, spores in the air
        for (int i = 0; i < size * 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Math.abs(x - c) > 1 && dist(a, x, z) > 9) a.set(x, 0, z, r.chance(0.5) ? b("crying_obsidian") : b("amethyst_block"));
        }
        for (int i = 0; i < size / 2; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (!Draw.keepClear(a, x, z)) a.set(x, 1, z, st("amethyst_cluster", "facing=up"));
        }
        Parts.banners(a, "magenta", 10, 14);
        a.zone("psi", c, c + 13, 5);
        a.zone("psi", c, c - 13, 5);
        return a;
    }
}
