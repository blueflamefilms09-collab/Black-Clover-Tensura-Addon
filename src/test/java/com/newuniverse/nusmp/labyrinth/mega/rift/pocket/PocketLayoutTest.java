package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the generator of the exclusive pockets' floor plans must be deterministic (the same seed is the same tomb after a restart) and must never produce a plan a delver can get stuck in:
 * the treasury is reachable once the seal is open and ONLY then, every other room (the key room and the lair included) is reachable from the entry without crossing the seal, there is a
 * loop, no gravity room stands beside a plain one. These are checked over many seeds, on both grid sizes, with the heaviest zone counts the roster asks for.
 */
class PocketLayoutTest {
    private static LayoutGen.Params params(int grid, int rooms, int inv, int neg, int riv) {
        return new LayoutGen.Params(grid, rooms, 0.35, 10, inv, neg, riv, List.of("fire", "plant"));
    }

    @Test
    void theSamePlanComesFromTheSameSeed() {
        for (long s = 1; s <= 40; s++) {
            PocketLayout a = LayoutGen.generate(s * 7919L, params(7, 18, 2, 1, 1)), b = LayoutGen.generate(s * 7919L, params(7, 18, 2, 1, 1));
            assertEquals(a.rooms(), b.rooms(), "rooms of seed " + s);
            assertEquals(a.doors(), b.doors(), "doors of seed " + s);
        }
    }

    @Test
    void differentSeedsGiveDifferentTombs() {
        java.util.Set<String> shapes = new java.util.HashSet<>();
        for (long s = 1; s <= 60; s++) shapes.add(LayoutGen.generate(s, params(7, 18, 1, 1, 1)).doors().toString());
        assertTrue(shapes.size() >= 50, "only " + shapes.size() + " different door nets in 60 seeds");
    }

    @Test
    void everyPlanIsSoundOverManySeeds() {
        int[][] cases = {{7, 18, 2, 1, 1}, {7, 22, 3, 2, 2}, {7, 14, 1, 0, 0}, {7, 26, 3, 2, 3}, {5, 12, 1, 1, 0}, {5, 16, 1, 0, 1}, {7, 10, 0, 0, 0}};
        for (int[] c : cases) {
            LayoutGen.Params p = params(c[0], c[1], c[2], c[3], c[4]);
            for (long s = 0; s < 400; s++) {
                PocketLayout l = LayoutGen.generate(s * 104729L + 3, p);
                List<String> problems = LayoutCheck.validate(l);
                assertTrue(problems.isEmpty(), "seed " + s + " case " + java.util.Arrays.toString(c) + ": " + problems);
                assertTrue(LayoutGen.accepted(l, p), "seed " + s + " case " + java.util.Arrays.toString(c) + " lacks a requested zone: " + l.count(PocketLayout.Kind.INVERTED) + "/" + l.count(PocketLayout.Kind.NEGATED) + "/" + l.count(PocketLayout.Kind.RIVER));
            }
        }
    }

    @Test
    void theTreasuryIsOnlyReachedThroughTheSeal() {
        for (long s = 0; s < 200; s++) {
            PocketLayout l = LayoutGen.generate(s + 1, params(7, 20, 2, 1, 1));
            int n = l.rooms().size();
            assertEquals(n - 1, LayoutCheck.reachable(l, false), "everything but the treasury is reachable without the seal (seed " + s + ")");
            assertEquals(n, LayoutCheck.reachable(l, true), "everything is reachable once the seal is open (seed " + s + ")");
            assertEquals(1, l.degree(l.treasuryId(), true));
            assertEquals(0, l.degree(l.treasuryId(), false));
            assertEquals(l.bossId(), (int) l.neighbours(l.treasuryId(), true).get(0), "the lair is in front of the treasury");
        }
    }

    @Test
    void aBadPlanIsRefusedByTheChecker() {
        PocketLayout good = LayoutGen.generate(5L, params(7, 18, 1, 1, 1));
        assertTrue(LayoutCheck.validate(good).isEmpty());
        // an open door into the treasury: build a copy of the plan with the seal turned into an ordinary door
        java.util.List<PocketLayout.Door> doors = new java.util.ArrayList<>();
        for (PocketLayout.Door d : good.doors()) doors.add(d.sealed() ? new PocketLayout.Door(d.a(), d.b(), false) : d);
        PocketLayout open = new PocketLayout(good.seed(), good.grid(), good.rooms(), doors, good.entryId(), good.bossId(), good.treasuryId(), good.keyId());
        assertFalse(LayoutCheck.validate(open).isEmpty(), "a treasury that opens without the seal is refused");
        // a plan cut in two: drop every door of the entry
        doors = new java.util.ArrayList<>();
        for (PocketLayout.Door d : good.doors()) if (d.a() != good.entryId() && d.b() != good.entryId()) doors.add(d);
        PocketLayout cut = new PocketLayout(good.seed(), good.grid(), good.rooms(), doors, good.entryId(), good.bossId(), good.treasuryId(), good.keyId());
        assertFalse(LayoutCheck.validate(cut).isEmpty(), "an entry with no door is refused");
    }

    @Test
    void gravityRoomsKeepPlainNeighboursAndTallCorridorsHaveRoom() {
        for (long s = 0; s < 300; s++) {
            PocketLayout l = LayoutGen.generate(s * 31 + 9, params(7, 24, 3, 2, 2));
            for (PocketLayout.Room r : l.rooms()) {
                if (!r.kind().gravity()) continue;
                for (int nb : l.neighbours(r.id(), true)) {
                    PocketLayout.Kind k = l.room(nb).kind();
                    assertFalse(k.plain() || k.gravity(), "a " + r.kind() + " room beside a " + k + " room, seed " + s);
                }
            }
            for (PocketLayout.Door d : l.doors()) {
                PocketLayout.Room a = l.room(d.a()), b = l.room(d.b());
                int len = l.corridorLength(a, b);
                assertTrue(len >= (PocketLayout.tall(a, b) ? 7 : 3), "corridor " + len + " between " + a.kind() + " and " + b.kind());
            }
        }
    }

    @Test
    void roomsAreFoundByPositionAndStayInsideTheCell() {
        PocketLayout l = LayoutGen.generate(77L, params(7, 20, 1, 1, 1));
        for (PocketLayout.Room r : l.rooms()) {
            assertEquals(r.id(), l.roomAt(r.cx(l.grid()), r.cz(l.grid())));
            assertEquals(r.id(), l.roomAt(r.cx(l.grid()) + r.half(), r.cz(l.grid()) - r.half()));
            assertTrue(Math.abs(r.cx(l.grid())) + r.half() + 1 <= 96 - 8, "room " + r.id() + " is inside the 192 block cell");
            assertTrue(Math.abs(r.cz(l.grid())) + r.half() + 1 <= 96 - 8);
        }
        assertEquals(-1, l.roomAt(95, 95));
    }
}
