package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a plan that looks right is not a plan that plays right, so every property that keeps a delver from being stuck is a check that a test runs over thousands of seeds and that the
 * generator itself obeys before it accepts a plan. The list: the rooms stand on distinct cells of the grid and never touch (so no wall cuts another room); there is exactly one entry, one
 * lair, one treasury and one key room; every door joins two rooms that are neighbours on the grid, with a corridor of at least three blocks (seven when it is a tall corridor beside a
 * ceiling-walker room, so the gravity gate has room); the treasury has ONE door and it is the sealed one, and it leads to the lair; the entry reaches every other room, the key room and
 * the lair included, WITHOUT crossing the seal, so the Sigil and the fight are always reachable and the seal is the only gate; the lair is at least three rooms deep; there is a loop
 * (the plan is not a tree) whenever it is big enough to hold one; a room with a gravity rule has plain, ordinary neighbours; and no trap room opens straight into the entry. Everything is a
 * function of the plan alone.
 */
public final class LayoutCheck {
    private LayoutCheck() {}

    /** The problems of a plan, in plain words; empty when it is sound. */
    public static List<String> validate(PocketLayout l) {
        List<String> out = new ArrayList<>();
        int n = l.rooms().size();
        if (n < 8) out.add("only " + n + " rooms");
        int g = l.grid();
        boolean[][] seen = new boolean[g][g];
        for (PocketLayout.Room r : l.rooms()) {
            if (r.gx() < 0 || r.gx() >= g || r.gz() < 0 || r.gz() >= g) {
                out.add("room " + r.id() + " is off the grid");
                continue;
            }
            if (seen[r.gx()][r.gz()]) out.add("two rooms on the cell " + r.gx() + "," + r.gz());
            seen[r.gx()][r.gz()] = true;
        }
        for (PocketLayout.Room a : l.rooms())
            for (PocketLayout.Room b : l.rooms()) {
                if (a.id() >= b.id()) continue;
                int dx = Math.abs(a.cx(g) - b.cx(g)), dz = Math.abs(a.cz(g) - b.cz(g));
                int need = a.half() + b.half() + 3;                                    // two walls and one block of air between
                if (dx < need && dz < need) out.add("rooms " + a.id() + " and " + b.id() + " touch");
            }
        int entries = l.count(PocketLayout.Kind.ENTRY), bosses = l.count(PocketLayout.Kind.BOSS), treasuries = l.count(PocketLayout.Kind.TREASURY), keys = l.count(PocketLayout.Kind.KEY);
        if (entries != 1 || bosses != 1 || treasuries != 1 || keys != 1) out.add("entry/boss/treasury/key counts are " + entries + "/" + bosses + "/" + treasuries + "/" + keys);
        if (!out.isEmpty() && (entries != 1 || bosses != 1 || treasuries != 1 || keys != 1)) return out;     // the rest asks for them
        int sealed = 0;
        for (PocketLayout.Door d : l.doors()) {
            PocketLayout.Room a = l.room(d.a()), b = l.room(d.b());
            if (d.a() >= d.b()) out.add("door " + d.a() + "-" + d.b() + " is not in order");
            if (Math.abs(a.gx() - b.gx()) + Math.abs(a.gz() - b.gz()) != 1) {
                out.add("door " + d.a() + "-" + d.b() + " joins rooms that are not neighbours");
                continue;
            }
            int len = l.corridorLength(a, b);
            int least = d.sealed() ? 3 : PocketLayout.tall(a, b) ? 7 : 3;
            if (len < least) out.add("the corridor " + d.a() + "-" + d.b() + " is " + len + " blocks long, less than " + least);
            if (d.sealed()) {
                sealed++;
                boolean ok = (d.a() == l.bossId() && d.b() == l.treasuryId()) || (d.b() == l.bossId() && d.a() == l.treasuryId());
                if (!ok) out.add("the sealed door does not join the lair and the treasury");
            } else if (d.a() == l.treasuryId() || d.b() == l.treasuryId()) {
                out.add("an open door leads into the treasury");
            }
        }
        if (sealed != 1) out.add(sealed + " sealed doors");
        if (l.degree(l.treasuryId(), true) != 1) out.add("the treasury has " + l.degree(l.treasuryId(), true) + " doors");
        int[] depth = LayoutGen.depths(n, l.doors(), l.entryId());
        for (PocketLayout.Room r : l.rooms()) {
            if (r.id() == l.treasuryId()) {
                if (depth[r.id()] >= 0) out.add("the treasury is reachable without crossing the seal");
                continue;
            }
            if (depth[r.id()] < 0) out.add("room " + r.id() + " (" + r.kind() + ") cannot be reached from the entry without crossing the seal");
        }
        if (depth[l.bossId()] >= 0 && depth[l.bossId()] < 3) out.add("the lair is only " + depth[l.bossId()] + " rooms from the entry");
        if (depth[l.keyId()] >= 0 && depth[l.keyId()] < 2) out.add("the key room is next to the entry");
        int openEdges = 0;
        for (PocketLayout.Door d : l.doors()) if (!d.sealed()) openEdges++;
        if (n >= 10 && openEdges - (n - 2) < 1) out.add("the plan is a tree: no loop to run round");
        for (PocketLayout.Room r : l.rooms()) {
            List<Integer> nbs = l.neighbours(r.id(), true);
            if (r.kind() != PocketLayout.Kind.TREASURY && l.neighbours(r.id(), false).isEmpty()) out.add("room " + r.id() + " has no door");
            if (r.kind().gravity()) {
                for (int nb : nbs) {
                    PocketLayout.Kind k = l.room(nb).kind();
                    if (k.plain() || k.gravity()) out.add("the " + r.kind() + " room " + r.id() + " stands beside a " + k + " room (" + nb + ")");
                }
            }
            if (r.kind() == PocketLayout.Kind.TRAP) for (int nb : nbs) if (l.room(nb).kind() == PocketLayout.Kind.ENTRY) out.add("a trap room opens into the entry");
            if (r.kind() == PocketLayout.Kind.TRAP && r.element().isEmpty()) out.add("trap room " + r.id() + " has no element");
        }
        return out;
    }

    /** Rooms reachable from the entry, optionally through the seal (the rule the tests of "the treasury is reachable once the seal is open" use). */
    public static int reachable(PocketLayout l, boolean throughSeal) {
        int n = l.rooms().size();
        boolean[] seen = new boolean[n];
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        q.add(l.entryId());
        seen[l.entryId()] = true;
        int count = 1;
        while (!q.isEmpty()) {
            int c = q.poll();
            for (int o : l.neighbours(c, throughSeal)) if (!seen[o]) {
                seen[o] = true;
                count++;
                q.add(o);
            }
        }
        return count;
    }
}
