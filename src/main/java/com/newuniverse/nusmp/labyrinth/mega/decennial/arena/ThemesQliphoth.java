package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Element;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Opts;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Shell.Pal;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: the owner's canon for the Evil half is the Tree of Qliphoth (Black Clover): a huge magical channel between the human world and the Underworld, a root system divided into SEVEN levels,
 * a GATE between each, ten powerful devils stationed in them, the three Supreme Devils on the seventh and lowest. The Evil levels are cut into seven bands (docs/labyrinth/decennial_bosses.md) and
 * the boss of the gate level of a band stands in a GATE CHAMBER: a vast black-rooted hall whose far wall is a sealed gate that opens when the devil falls. The seven chambers are one builder with one
 * number: the deeper the gate, the more of its seals are lit, the more roots crawl over the walls and the floor, the more root nodes there are to break (each a bulb that bursts into lesser
 * devils), the more pools of black sap and the dimmer the light. The eighth hall, the ROOT THRONE, is where the three Supreme Devils receive the party: a trunk of black wood rising to the roof,
 * a throne of roots and all seven seals lit. The great gate of each hall is a LOCKED element: nothing a player does breaks it; the hall removes it when the boss falls, and what is behind it is the
 * ordinary Gatekeeper that a Mana Core opens.
 */
final class ThemesQliphoth {
    private ThemesQliphoth() {}

    private static Pal pal(int k) {
        return new Pal(b("deepslate"), b("blackstone"), b("deepslate_tiles"), b("crying_obsidian"), b("polished_blackstone_bricks"), b("cracked_polished_blackstone_bricks"),
                b("chiseled_polished_blackstone"), b("polished_blackstone"), b("chiseled_polished_blackstone"), "polished_blackstone_brick_slab", "polished_blackstone_brick_stairs",
                b("black_stained_glass"), b("crying_obsidian"), Math.max(3, 9 - (k + 1) / 2));
    }

    // ================================================================ the seven gate chambers
    static BossArenaStructure gate(int k, int size) {
        String theme = "qliphoth_gate_" + k;
        BossArenaStructure a = Shell.start(theme, size, 36, pal(k), new Opts(2, 0, 8, true));
        ArenaRng r = ArenaRng.of(theme, size);
        int c = a.c;
        roots(a, r, k);
        gateLeaf(a, k);
        // root nodes: bulbs on the floor that burst into lesser devils; breaking one also weakens the hold of the roots on the boss
        int nodes = Math.min(12, 3 + k);
        for (int[] q : Parts.ringSpots(a, Math.min(14.0, c - 7.0), nodes, 0.35 + k * 0.11)) if (Draw.awayFromGate(a, q[0], q[1])) node(a, q[0], q[1]);
        // pools of black sap
        int pools = 2 + (k + 1) / 2;
        for (int[] q : Parts.ringSpots(a, Math.min(17.5, c - 5.0), pools, 0.9 + k * 0.3)) {
            if (!Draw.awayFromGate(a, q[0], q[1])) continue;
            Parts.pool(a, q[0], q[1], 2.4, b("coal_block"), b("black_concrete"), b("muddy_mangrove_roots"));
            a.zone("sap", q[0], q[1], 4);
        }
        // trunks: great roots descending from the roof to the floor, more with every gate
        for (int[] q : Parts.ringSpots(a, Math.min(19.5, c - 3.5), 3 + (k + 1) / 2, 0.15)) {
            if (!Draw.awayFromGate(a, q[0], q[1])) continue;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                if (Math.abs(dx) + Math.abs(dz) == 2) continue;
                for (int y = 1; y < a.height - 1; y++) a.set(q[0] + dx, y, q[1] + dz, y < 4 ? b("mangrove_roots") : b("dark_oak_log"));
            }
        }
        a.spot(c - 9, c + 8);
        a.spot(c + 9, c + 8);
        a.zone("root", c, c + 12, 5);
        a.zone("root", c, c - 12, 5);
        a.zone("sap", c - 12, c, 4);
        a.zone("sap", c + 12, c, 4);
        return a;
    }

    // ================================================================ the root throne
    static BossArenaStructure rootThrone(int size) {
        BossArenaStructure a = Shell.start("qliphoth_root_throne", size, 40, pal(9), new Opts(2, 0, 8, true));
        ArenaRng r = ArenaRng.of("qliphoth_root_throne", size);
        int c = a.c;
        roots(a, r, 8);
        gateLeaf(a, 7);
        // the trunks of the Tree at the sides of the hall: black wood to the roof, flaring into roots at their foot
        for (int sd = -1; sd <= 1; sd += 2) {
            int tx = c + sd * (c - 9), tz = c - 6;
            cyl(a, tx, tz, 3.5, 1, a.height - 2, b("dark_oak_wood"));
            ring(a, tx, tz, 3.5, 5.6, 1, b("mangrove_roots"));
            ring(a, tx, tz, 5.0, 6.4, 0, b("muddy_mangrove_roots"));
        }
        // the throne of roots on three steps, north of the dais, its back to the great gate
        fill(a, c - 4, 1, c - 14, c + 4, 1, c - 10, b("polished_blackstone_bricks"));
        fill(a, c - 3, 2, c - 13, c + 3, 2, c - 10, b("polished_blackstone"));
        fill(a, c - 2, 3, c - 12, c + 2, 3, c - 11, b("blackstone"));
        a.set(c, 4, c - 11, stair("polished_blackstone_brick_stairs", "north", "bottom"));
        for (int y = 4; y <= 10; y++) {
            a.set(c, y, c - 12, y <= 6 ? b("crying_obsidian") : b("mangrove_roots"));
            a.set(c - 1, y, c - 12, b("mangrove_roots"));
            a.set(c + 1, y, c - 12, b("mangrove_roots"));
        }
        // root nodes in a ring, seven pools, the three sigils of the Supreme Devils on the floor
        for (int[] q : Parts.ringSpots(a, Math.min(15.0, c - 8.0), 10, 0.2)) if (Draw.awayFromGate(a, q[0], q[1])) node(a, q[0], q[1]);
        for (int[] q : Parts.ringSpots(a, Math.min(17.5, c - 5.0), 7, 0.6)) {
            if (!Draw.awayFromGate(a, q[0], q[1])) continue;
            Parts.pool(a, q[0], q[1], 2.4, b("coal_block"), b("black_concrete"), b("muddy_mangrove_roots"));
            a.zone("sap", q[0], q[1], 4);
        }
        for (int s = -1; s <= 1; s++) disc(a, c + s * 9, c + 8, 1.7, 0, b("crying_obsidian"));
        // a cage of roots round the walls
        for (int[] q : Parts.ringSpots(a, Math.min(21.5, c - 3.5), 14, 0.0)) {
            if (!Draw.awayFromGate(a, q[0], q[1])) continue;
            for (int y = 1; y < a.height - 1; y++) a.set(q[0], y, q[1], y < 3 ? b("muddy_mangrove_roots") : b("mangrove_roots"));
        }
        a.spot(c - 9, c + 8);
        a.spot(c + 9, c + 8);
        a.zone("root", c, c + 12, 5);
        a.zone("root", c, c - 6, 5);
        a.zone("sap", c - 12, c + 4, 4);
        a.zone("sap", c + 12, c + 4, 4);
        return a;
    }

    // ================================================================ parts
    /** A root node: a bulb of roots on a stalk, a shroomlight in it. Broken, it bursts into lesser devils and loosens the roots' hold on the boss. */
    private static void node(BossArenaStructure a, int x, int z) {
        Element e = a.beginElement(Kind.ROOT_NODE, Effect.ADDS, 2);
        a.set(x, 1, z, b("muddy_mangrove_roots"));
        a.set(x, 2, z, b("mangrove_roots"));
        a.set(x, 3, z, b("shroomlight"));
        a.set(x, 4, z, b("mangrove_roots"));
        a.set(x + 1, 2, z, b("mangrove_roots"));
        a.set(x - 1, 2, z, b("mangrove_roots"));
        a.set(x, 2, z + 1, b("mangrove_roots"));
        a.set(x, 2, z - 1, b("mangrove_roots"));
        a.endElement();
        e.bossShare = 0.01;
        a.spot(x, z);
        a.zone("root", x, z, 3);
    }

    /** Roots over the floor, up the walls and down from the roof; the number grows with the gate. */
    private static void roots(BossArenaStructure a, ArenaRng r, int k) {
        int c = a.c, size = a.size;
        // veins on the floor: random walks from the walls towards the middle
        for (int i = 0; i < 8 + k * 3; i++) {
            double ang = r.d() * Math.PI * 2;
            double x = c + Math.cos(ang) * (c - 3), z = c + Math.sin(ang) * (c - 3);
            for (int step = 0; step < c; step++) {
                double dx = c - x, dz = c - z, d = Math.hypot(dx, dz);
                if (d < 11) break;
                x += dx / d + (r.d() - 0.5) * 1.4;
                z += dz / d + (r.d() - 0.5) * 1.4;
                int ix = (int) Math.round(x), iz = (int) Math.round(z);
                if (Math.abs(ix - c) <= 2) continue;
                a.set(ix, 0, iz, r.chance(0.5) ? b("mangrove_roots") : b("muddy_mangrove_roots"));
            }
        }
        // roots on the walls: from a few blocks at the first gate to a thicket at the seventh
        for (int i = 4; i < size - 4; i++) {
            if (Math.abs(i - c) <= 3) continue;
            int h = 2 + r.next(2 + k);
            for (int y = 1; y <= h; y++) {
                if (r.chance(0.7)) a.set(i, y, 3, b("mangrove_roots"));
                if (r.chance(0.7)) a.set(i, y, size - 4, b("mangrove_roots"));
                if (r.chance(0.7)) a.set(3, y, i, b("mangrove_roots"));
                if (r.chance(0.7)) a.set(size - 4, y, i, b("mangrove_roots"));
            }
        }
        // roots hanging from the roof
        for (int i = 0; i < 10 + k * 4; i++) {
            int x = r.range(3, size - 4), z = r.range(3, size - 4);
            if (Draw.keepClear(a, x, z)) continue;
            int len = 3 + r.next(10);
            for (int y = a.height - 2; y > a.height - 2 - len; y--) a.set(x, y, z, y == a.height - 1 - len ? st("hanging_roots", "waterlogged=false") : b("mangrove_roots"));
        }
    }

    /** The great gate over the Gatekeeper: nine wide and fourteen high, a lintel of chiselled blackstone, seven slots of seals of which the first {@code lit} burn. LOCKED: the hall removes it when the boss falls. */
    private static void gateLeaf(BossArenaStructure a, int lit) {
        int c = a.c;
        Element g = a.beginElement(Kind.GATE, Effect.SHATTER, 0);
        g.locked = true;
        for (int x = c - 4; x <= c + 4; x++)
            for (int y = 1; y <= 14; y++)
                for (int z = 3; z <= 4; z++) {
                    boolean frame = x == c - 4 || x == c + 4 || y == 14;
                    String s = frame ? b("chiseled_polished_blackstone") : (x - c) % 2 == 0 ? b("deepslate_tiles") : b("polished_deepslate");
                    if (y == 10 && Math.abs(x - c) <= 3 && z == 3) s = (x - c) + 3 < lit ? b("crying_obsidian") : b("chiseled_polished_blackstone");
                    a.set(x, y, z, s);
                }
        for (int y = 1; y <= 13; y++) {
            a.set(c, y, 5, b("chain"));
        }
        a.endElement();
    }
}
