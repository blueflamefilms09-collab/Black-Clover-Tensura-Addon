package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Element;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Placed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.newuniverse.nusmp.labyrinth.mega.decennial.arena.Draw.*;

/**
 * Why: the halls share a vocabulary of PARTS bigger than a gesture and smaller than a theme: a scatter of graves, a ring of pillars that can be brought down, a row of braziers, a coffin, a
 * chandelier on its chain, a standing banner. Each part knows the three things a fight needs of it (what blocks it is made of, whether it is an interactive element and what breaking it does, which
 * hazard zone or add spot it adds) so that a theme is a handful of calls and the guarantees (an element's cells are exactly the cells that may break; a toppling pillar knows the strip of floor it
 * crushes and the rubble it leaves; a spot is on the floor) are kept once, here.
 */
public final class Parts {
    private Parts() {}

    // ================================================================ placing
    /**
     * Calls {@code put} for up to {@code n} random columns that are outside the lane and the boss clearing, at least {@code margin} from the walls and {@code gap} from each other (and from the columns
     * already taken in this call). Returns how many were placed.
     */
    public static int scatter(BossArenaStructure a, ArenaRng rng, int n, int margin, int gap, BiConsumer<Integer, Integer> put) {
        List<int[]> taken = new ArrayList<>();
        int placed = 0;
        for (int tries = 0; tries < n * 40 && placed < n; tries++) {
            int x = rng.range(2 + margin, a.size - 3 - margin), z = rng.range(2 + margin, a.size - 3 - margin);
            if (keepClear(a, x, z) || dist(a, x, z) <= 10.5 || Math.abs(z - 2) < 4 || Math.abs(z - (a.size - 3)) < 4 || !free(a, x, z)) continue;
            boolean ok = true;
            for (int[] t : taken) if (Math.abs(t[0] - x) < gap && Math.abs(t[1] - z) < gap) { ok = false; break; }
            if (!ok) continue;
            taken.add(new int[] {x, z});
            put.accept(x, z);
            placed++;
        }
        return placed;
    }

    /** Is the column empty of anything solid at the heights a part stands at (the floor level, head height, and the height of a roof or a canopy)? */
    public static boolean free(BossArenaStructure a, int x, int z) {
        return !a.solidAt(x, 1, z) && !a.solidAt(x, 2, z) && !a.solidAt(x, 8, z);
    }

    /** {@code n} columns evenly round a circle of radius r about the middle, starting at {@code phase} radians. */
    public static int[][] ringSpots(BossArenaStructure a, double r, int n, double phase) {
        int[][] out = new int[n][];
        for (int i = 0; i < n; i++) {
            double ang = phase + i * Math.PI * 2.0 / n;
            out[i] = new int[] {(int) Math.round(a.c + Math.cos(ang) * r), (int) Math.round(a.c + Math.sin(ang) * r)};
        }
        return out;
    }

    // ================================================================ elements
    /**
     * A pillar that can be brought down: a shaft with a base and a capital from the floor to height {@code h}. Broken (or toppled by the hall), its cells vanish, the strip of floor along the way it falls
     * (towards the middle of the hall) is crushed, and {@code rubble} (a slab or stair block text) is left lying in that strip. Returns the element, or null when its cells were all refused.
     */
    public static Element pillar(BossArenaStructure a, int x, int z, int h, String shaft, String base, String cap, boolean thick, String rubble) {
        Element e = a.beginElement(Kind.PILLAR, Effect.TOPPLE, 0);
        Draw.pillar(a, x, z, 1, h, shaft, base, cap, thick);
        a.endElement();
        if (e.cells.isEmpty()) return null;
        int dx = Integer.signum(a.c - x), dz = Integer.signum(a.c - z);
        if (Math.abs(a.c - x) >= Math.abs(a.c - z)) dz = 0; else dx = 0;
        int reach = Math.max(4, Math.min(h, 12));
        int ex = clamp(x + dx * reach, 3, a.size - 4), ez = clamp(z + dz * reach, 3, a.size - 4);
        int w = thick ? 2 : 1;
        e.strip = new int[] {Math.min(x, ex) - (dx == 0 ? w : 0), Math.min(z, ez) - (dz == 0 ? w : 0), Math.max(x, ex) + (dx == 0 ? w : 0), Math.max(z, ez) + (dz == 0 ? w : 0)};
        for (int i = 2; i <= reach; i++) {
            int rx = x + dx * i, rz = z + dz * i;
            if (Math.abs(rx - a.c) <= 2 && Math.abs(rz - a.c) <= 2) continue;                           // never on the dais
            if (a.solidAt(rx, 1, rz) || a.isElementCell(rx, 1, rz)) continue;                            // never on something that already stands there
            if ((i & 1) == 0 || i == reach) e.rubble.add(new Placed(rx, 1, rz, rubble));
        }
        a.zone("pillar", x, z, 3);
        return e;
    }

    /** A brazier: a short pedestal with a lantern on top. Breaking it bursts it (no adds). */
    public static Element brazier(BossArenaStructure a, int x, int z, String pedestal, String lantern) {
        Element e = a.beginElement(Kind.BRAZIER, Effect.SHATTER, 0);
        a.set(x, 1, z, pedestal);
        a.set(x, 2, z, pedestal);
        a.set(x, 3, z, lantern);
        a.endElement();
        a.zone("brazier", x, z, 2);
        return e;
    }

    /** A coffin or sarcophagus lying along x or z: three base blocks and a lid of slabs. Breaking it lets {@code adds} of the dead out. */
    public static Element coffin(BossArenaStructure a, int x, int z, boolean alongX, int adds, String body, String lid) {
        Element e = a.beginElement(Kind.COFFIN, Effect.ADDS, adds);
        for (int i = -1; i <= 1; i++) {
            int px = alongX ? x + i : x, pz = alongX ? z : z + i;
            a.set(px, 1, pz, body);
            a.set(px, 2, pz, lid);
        }
        a.endElement();
        a.spot(x, z);
        a.zone("grave", x, z, 2);
        return e;
    }

    /** A chandelier on a chain from the ceiling: breaking it brings it down (a burst of the size of its ring). */
    public static Element chandelier(BossArenaStructure a, int x, int z, String chainBlock, String lamp) {
        Element e = a.beginElement(Kind.CHANDELIER, Effect.SHATTER, 0);
        int top = a.height - 2, low = a.height - 8;
        for (int y = low + 1; y <= top; y++) a.set(x, y, z, chainBlock);
        a.set(x, low, z, lamp);
        a.set(x + 1, low, z, lamp);
        a.set(x - 1, low, z, lamp);
        a.set(x, low, z + 1, lamp);
        a.set(x, low, z - 1, lamp);
        a.endElement();
        a.zone("chandelier", x, z, 3);
        return e;
    }

    /** A single-block interactive element (a crystal, a mirror panel, a pod): breaking it does {@code effect}. */
    public static Element single(BossArenaStructure a, Kind kind, Effect effect, int adds, int x, int y, int z, String spec) {
        Element e = a.beginElement(kind, effect, adds);
        a.set(x, y, z, spec);
        a.endElement();
        a.spot(x, z);
        return e;
    }

    /** A vertical stack element of blocks (a shelf, a statue on a plinth, a totem): the specs from y=1 up. */
    public static Element stack(BossArenaStructure a, Kind kind, Effect effect, int adds, int x, int z, String... specs) {
        Element e = a.beginElement(kind, effect, adds);
        for (int i = 0; i < specs.length; i++) a.set(x, 1 + i, z, specs[i]);
        a.endElement();
        a.spot(x, z);
        return e;
    }

    // ================================================================ decoration (not interactive)
    /** A banner hung on the inside of a wall column: {@code facing} is the direction the banner faces (into the room). */
    public static void banner(BossArenaStructure a, int x, int y, int z, String colour, String facing) { a.set(x, y, z, st(colour + "_wall_banner", "facing=" + facing)); }

    /** Banners along all four walls every {@code step} columns at height y. */
    public static void banners(BossArenaStructure a, String colour, int y, int step) {
        for (int i = 6; i < a.size - 6; i += step) {
            if (Math.abs(i - a.c) <= 3) continue;
            banner(a, i, y, 2, colour, "south");
            banner(a, i, y, a.size - 3, colour, "north");
            banner(a, 2, y, i, colour, "east");
            banner(a, a.size - 3, y, i, colour, "west");
        }
    }

    /** A carpet lane along the hall's axis from the entry to the dais and from the dais to the Gatekeeper. */
    public static void lane(BossArenaStructure a, String carpet) {
        for (int z = 3; z <= a.size - 4; z++) {
            if (Math.hypot(0, z - a.c) <= 9.0) continue;                           // the dais keeps its stone
            for (int x = a.c - 1; x <= a.c + 1; x++) a.set(x, 1, z, carpet);
        }
    }

    /** A dead tree: a trunk of {@code log}, three bare branches of {@code wood}. */
    public static void deadTree(BossArenaStructure a, int x, int z, int h, String log, String wood) {
        for (int y = 1; y <= h; y++) a.set(x, y, z, log);
        a.set(x + 1, h - 1, z, wood);
        a.set(x + 2, h, z, wood);
        a.set(x - 1, h - 2, z, wood);
        a.set(x - 2, h - 1, z, wood);
        a.set(x, h - 1, z + 1, wood);
        a.set(x, h, z + 2, wood);
    }

    /** A stalagmite or icicle column: a tapering stack of {@code block} with {@code tip} on top. */
    public static void spire(BossArenaStructure a, int x, int z, int y0, int h, String block, String tip) {
        for (int y = 0; y < h; y++) {
            a.set(x, y0 + y, z, block);
            if (y < h / 3) {
                a.set(x + 1, y0 + y, z, block);
                a.set(x - 1, y0 + y, z, block);
                a.set(x, y0 + y, z + 1, block);
                a.set(x, y0 + y, z - 1, block);
            }
        }
        a.set(x, y0 + h, z, tip);
    }

    /** A hanging chain with a lamp at the bottom from the ceiling. */
    public static void hanging(BossArenaStructure a, int x, int z, int len, String chainBlock, String lamp) {
        int top = a.height - 2;
        for (int y = top; y > top - len; y--) a.set(x, y, z, chainBlock);
        a.set(x, top - len, z, lamp);
    }

    /** A raised block platform (a step or plinth) of w x d with its corner at (x, z), height 1. */
    public static void plinth(BossArenaStructure a, int x, int z, int w, int d, String block) { fill(a, x, 1, z, x + w - 1, 1, z + d - 1, block); }

    /** A sunken pool in the floor: removes the floor block at depth 1 and lays {@code bottom}, then fills {@code liquid} up to the floor row; the rim is {@code rim}. */
    public static void pool(BossArenaStructure a, double cx, double cz, double r, String bottom, String liquid, String rim) {
        ring(a, cx, cz, r, r + 1.2, 0, rim);
        disc(a, cx, cz, r, -1, bottom);
        disc(a, cx, cz, r, 0, liquid);
    }

    /** Writes a block only where the cell is empty (air, nothing or a passable block) and belongs to no element: decoration that must not overwrite what another part built. */
    public static boolean soft(BossArenaStructure a, int x, int y, int z, String spec) {
        if (!a.inside(x, y, z) || a.isElementCell(x, y, z)) return false;
        String cur = a.get(x, y, z);
        if (cur != null && Solidity.solid(cur)) return false;
        return a.set(x, y, z, spec);
    }

    /** Takes the rubble out of every element that would now lie on something solid (a pillar built later stands where an earlier pillar's rubble was meant to fall). Run once at the end of a build. */
    public static void pruneRubble(BossArenaStructure a) {
        for (Element e : a.elements()) e.rubble.removeIf(r -> a.solidAt(r.x(), r.y(), r.z()) || a.isElementCell(r.x(), r.y(), r.z()));
    }

    /** Clamp. */
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
