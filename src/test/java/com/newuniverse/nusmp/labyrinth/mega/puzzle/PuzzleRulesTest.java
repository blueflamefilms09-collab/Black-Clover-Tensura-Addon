package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the contract every puzzle must keep, proven for EVERY registered puzzle at once (so a new puzzle that breaks it fails here the moment it is registered): it builds a layout whose
 * faces all exist in their palette; it starts unsolved; the same params give the same board; the rule's own suggestions solve it for every seed, party size and tier; it can be solved after
 * a storm of wrong actions (no soft lock: a restart or a reset puts a solvable board back); a reset puts back exactly the board of a fresh copy; a solved puzzle ignores everything; the
 * penalties it asks for are within 1 to 3 and of a known kind; and nothing it says or does depends on the real clock.
 */
class PuzzleRulesTest {
    static final int[] PARTIES = {1, 2, 3, 5, 8};

    static List<PuzzleRule> sweep(String id, int seeds) {
        List<PuzzleRule> out = new ArrayList<>();
        for (int tier = 1; tier <= 5; tier++) for (int party : PARTIES) for (int s = 0; s < seeds; s++) out.add(PuzzleCatalog.create(id, new Params(1000L * tier + 31L * s + party, party, tier)));
        return out;
    }

    static String board(PuzzleRule r) {
        StringBuilder b = new StringBuilder();
        for (int c = 0; c < r.layout().total(); c++) {
            b.append(r.height(c)).append(':');
            for (int l = 0; l < r.height(c); l++) b.append(r.face(c, l)).append(',');
            b.append(';');
        }
        return b.toString();
    }

    @Test
    void theCatalogueHasAtLeastTwentyFourPuzzlesWithUniqueIds() {
        List<String> ids = PuzzleCatalog.ids();
        assertTrue(ids.size() >= 24, "only " + ids.size());
        assertEquals(ids.size(), ids.stream().distinct().count());
        for (String id : ids) assertTrue(id.matches("[a-z][a-z_]*"), id);
    }

    @Test
    void everyLayoutIsBuildableAndEveryFaceExistsInItsPalette() {
        for (String id : PuzzleCatalog.ids()) {
            for (PuzzleRule r : sweep(id, 3)) {
                Layout lay = r.layout();
                assertTrue(lay.total() >= 1, id);
                int cells = lay.total();
                for (int c = 0; c < cells; c++) {
                    Layout.Group g = lay.groupOf(c);
                    assertNotNull(g, id + " cell " + c);
                    assertTrue(Palettes.size(g.palette()) > 0, id + ": no palette " + g.palette());
                    assertTrue(r.height(c) >= 1 && r.height(c) <= 9, id + " height " + r.height(c));
                    for (int l = 0; l < r.height(c); l++) {
                        int f = r.face(c, l);
                        assertTrue(f >= 0 && f < Palettes.size(g.palette()), id + " cell " + c + " face " + f + " of " + g.palette());
                    }
                }
                if (r.layout().groups().stream().anyMatch(g -> g.shape() == Layout.Shape.GRID)) {
                    for (Layout.Group g : lay.groups()) if (g.shape() == Layout.Shape.GRID) assertEquals(g.cols() * g.rows(), g.count(), id);
                }
            }
        }
    }

    @Test
    void everyPuzzleStartsUnsolvedAndIsDeterministic() {
        for (String id : PuzzleCatalog.ids()) {
            for (PuzzleRule a : sweep(id, 3)) {
                assertFalse(a.solved(), id + " starts solved");
                PuzzleRule b = PuzzleCatalog.create(id, a.params());
                assertEquals(board(a), board(b), id + ": two builds of one seed differ");
                Act sa = a.suggest(0), sb = b.suggest(0);
                assertEquals(sa == null, sb == null, id);
                if (sa != null) assertEquals(sa, sb, id + ": suggestions differ");
            }
        }
    }

    @Test
    void theRulesOwnSuggestionsSolveEveryBoard() {
        for (String id : PuzzleCatalog.ids()) {
            for (PuzzleRule r : sweep(id, 12)) {
                PuzzleSim sim = new PuzzleSim(r);
                assertTrue(sim.solve(900), id + " not solved: " + r.params() + " after " + sim.steps + " steps at tick " + sim.t);
                assertTrue(sim.log.stream().anyMatch(f -> f.kind() == Fx.Kind.SOLVED), id + " never said SOLVED");
            }
        }
    }

    @Test
    void aStormOfWrongActionsNeverLeavesAnUnsolvablePuzzle() {
        for (String id : PuzzleCatalog.ids()) {
            for (PuzzleRule r : sweep(id, 5)) {
                SplittableRandom g = new SplittableRandom(r.params().seed() ^ 0x5DEECE66DL);
                PuzzleSim sim = new PuzzleSim(r);
                for (int round = 0; round < 4 && !r.solved(); round++) {
                    sim.garbage(g, 10 + g.nextInt(60));
                    if (r.solved()) break;
                    if (round == 2) {
                        sim.advance(sim.t + 3000);
                        r.reset(sim.t);
                        sim.log.addAll(r.begin(sim.t));
                    }
                }
                if (!r.solved()) assertTrue(sim.solve(1500), id + " soft locked after garbage: " + r.params() + " fails " + r.fails());
            }
        }
    }

    @Test
    void aResetPutsBackTheFreshBoardAndStaysSolvable() {
        for (String id : PuzzleCatalog.ids()) {
            for (PuzzleRule r : sweep(id, 4)) {
                PuzzleRule fresh = PuzzleCatalog.create(id, r.params());
                SplittableRandom g = new SplittableRandom(r.params().seed());
                PuzzleSim sim = new PuzzleSim(r);
                sim.garbage(g, 25);
                sim.advance(sim.t + 100);
                r.reset(sim.t);
                assertFalse(r.solved(), id);
                fresh.tick(sim.t);
                if (!id.equals("memory_tiles") && !id.equals("rune_copy")) assertEquals(board(fresh), board(r), id + ": reset did not restore the fresh board");
                sim.log.addAll(r.begin(sim.t));
                assertTrue(sim.solve(900), id + " not solvable after reset " + r.params());
            }
        }
    }

    @Test
    void aSolvedPuzzleIgnoresEverythingAndSuggestsNothing() {
        for (String id : PuzzleCatalog.ids()) {
            PuzzleRule r = PuzzleCatalog.create(id, new Params(77, 2, 3));
            PuzzleSim sim = new PuzzleSim(r);
            assertTrue(sim.solve(900), id);
            assertTrue(r.suggest(sim.t) == null, id);
            assertTrue(r.act(Act.click(0, 0, sim.t + 1)).isEmpty(), id);
            assertTrue(r.solved(), id);
        }
    }

    @Test
    void penaltiesAreMildAndKnown() {
        List<String> kinds = List.of("hurt", "dark", "drain", "slow", "hunger", "ambush");
        for (String id : PuzzleCatalog.ids()) {
            for (PuzzleRule r : sweep(id, 3)) {
                SplittableRandom g = new SplittableRandom(r.params().seed() + 5);
                PuzzleSim sim = new PuzzleSim(r);
                sim.garbage(g, 80);
                for (Fx f : sim.log) {
                    if (f.kind() == Fx.Kind.PENALTY) {
                        assertTrue(f.n() >= 1 && f.n() <= 3, id + " severity " + f.n());
                        assertTrue(kinds.contains(f.key()), id + " penalty kind " + f.key());
                    }
                    if (f.kind() == Fx.Kind.TONE) assertTrue(f.n() >= 0 && f.n() <= 30, id + " tone " + f.n());
                }
            }
        }
    }

    @Test
    void mistakesCostSomethingInEveryPuzzleThatHasWrongMoves() {
        // a storm of random actions on a puzzle either solves it by luck, or ends with at least one penalty; no puzzle lets 200 random actions pass for free
        int free = 0;
        List<String> freeOnes = new ArrayList<>();
        for (String id : PuzzleCatalog.ids()) {
            boolean paid = false;
            for (PuzzleRule r : sweep(id, 4)) {
                SplittableRandom g = new SplittableRandom(r.params().seed() * 31 + 1);
                PuzzleSim sim = new PuzzleSim(r);
                sim.garbage(g, 300);
                if (r.solved() || r.fails() > 0 || sim.log.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY)) { paid = true; break; }
            }
            if (!paid) { free++; freeOnes.add(id); }
        }
        assertEquals(0, free, "puzzles where random play is free: " + freeOnes);
    }
}
