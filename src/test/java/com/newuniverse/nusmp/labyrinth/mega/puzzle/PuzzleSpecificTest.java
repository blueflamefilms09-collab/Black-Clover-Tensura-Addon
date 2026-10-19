package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.Constellation;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.MimicChests;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.NumberLock;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.PlateSequence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the generic sweep proves that the rule's own hint always wins, but the hint knows the answer. For the puzzles that are won by READING or REASONING the test must also prove that what the
 * party can see is enough: a bot that uses only the faces, the heights and the text the rule shows (never the hidden state) must find the answer. Here are those bots (number lock, mimic
 * chests, torch order, weight scales, tilting maze, pit jump), plus the specific costs and rules of a few puzzles (a wrong plate costs, a wrong key bites, a pit is shallow and returns).
 */
class PuzzleSpecificTest {
    private static PuzzleRule make(String id, long seed, int party, int tier) { return PuzzleCatalog.create(id, new Params(seed, party, tier)); }

    // ---------------------------------------------------------------- number lock: the clues fix exactly one code
    @Test
    void theNumberLockCluesAlwaysFixExactlyOneCode() {
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 80; s++) {
            NumberLock r = (NumberLock) make("number_lock", 500L * tier + s, 1 + s % 4, tier);
            assertEquals(1, r.solutions(), "tier " + tier + " seed " + s);
            assertTrue(r.clueCount() >= 3 && r.clueCount() <= 6, "clues " + r.clueCount());
        }
    }

    // ---------------------------------------------------------------- mimic chests: the inscriptions fix exactly one chest
    @Test
    void aDeductionBotFindsTheRealMimicChestFromTheInscriptionsAlone() {
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 80; s++) {
            MimicChests r = (MimicChests) make("mimic_chests", 90L * tier + s, 1, tier);
            int c = r.layout().total();
            int[] type = new int[c], at = new int[c];
            for (int i = 0; i < c; i++) {
                List<Fx> fx = r.act(Act.click(0, i, 1));
                Fx say = fx.stream().filter(f -> f.kind() == Fx.Kind.SAY).findFirst().orElseThrow();
                type[i] = say.key().equals("inscr.in") ? 0 : 1;
                at[i] = say.more().get(1).charAt(0) - 'A';
            }
            int found = -1, count = 0;
            for (int loc = 0; loc < c; loc++) {
                int truths = 0;
                for (int i = 0; i < c; i++) if (type[i] == 0 ? loc == at[i] : loc != at[i]) truths++;
                if (truths == 1) { found = loc; count++; }
            }
            assertEquals(1, count, "ambiguous inscriptions, seed " + s);
            assertEquals(r.realChest(), found);
        }
    }

    @Test
    void aMimicIsSpentAndCostsButTheRealChestStaysOpenable() {
        MimicChests r = (MimicChests) make("mimic_chests", 3, 2, 2);
        int wrong = r.realChest() == 0 ? 1 : 0;
        List<Fx> fx = r.act(Act.sneakClick(1, wrong, 10));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.MIMIC));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY && f.n() == 2 && f.key().equals("ambush")));
        assertEquals(1, r.face(wrong, 0));
        List<Fx> again = r.act(Act.sneakClick(1, wrong, 100));
        assertTrue(again.stream().noneMatch(f -> f.kind() == Fx.Kind.MIMIC || f.kind() == Fx.Kind.PENALTY), "a spent mimic bit twice");
        r.act(Act.sneakClick(0, r.realChest(), 200));
        assertTrue(r.solved());
    }

    // ---------------------------------------------------------------- torch order: the recited rules fix exactly one order
    @Test
    void aDeductionBotLightsTheBraziersInTheOnlyOrderTheRulesAllow() {
        String[] names = {"red", "blue", "green", "yellow", "purple", "orange", "cyan", "white"};
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 40; s++) {
            PuzzleRule r = make("torch_order", 700L * tier + s, 1 + s % 5, tier);
            int n = r.layout().groups().get(0).count();
            int[] colourOf = new int[n];
            for (int i = 0; i < n; i++) colourOf[i] = r.face(i, 0);                          // faces 0..7 are the colour indices of the palette
            List<int[]> rules = new ArrayList<>();
            for (Fx f : r.act(Act.click(0, n, 1))) {
                if (!f.kind().equals(Fx.Kind.SAY) || !f.key().equals("rule")) continue;
                int a = -1, b = -1;
                for (int i = 0; i < n; i++) {
                    if (f.more().get(0).equals("$colour." + names[colourOf[i]])) a = i;
                    if (f.more().get(1).equals("$colour." + names[colourOf[i]])) b = i;
                }
                rules.add(new int[] {a, b});
            }
            // count the linear extensions by brute force over permutations: there must be exactly one
            int perms = countOrders(n, rules);
            assertEquals(1, perms, "torch order tier " + tier + " seed " + s);
            PuzzleSim sim = new PuzzleSim(r);
            assertTrue(sim.solve(200));
        }
    }

    private static int countOrders(int n, List<int[]> rules) {
        return extend(new int[n], new boolean[n], 0, rules);
    }

    private static int extend(int[] pos, boolean[] used, int k, List<int[]> rules) {
        int n = pos.length;
        if (k == n) {
            for (int[] r : rules) if (pos[r[0]] > pos[r[1]]) return 0;
            return 1;
        }
        int total = 0;
        for (int i = 0; i < n; i++) {
            if (used[i]) continue;
            used[i] = true;
            pos[i] = k;
            total += extend(pos, used, k + 1, rules);
            used[i] = false;
        }
        return total;
    }

    // ---------------------------------------------------------------- weight scales: heights are the weights, a split exists, all different
    @Test
    void theWeightStonesShowTheirWeightsAndASplitExists() {
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 60; s++) {
            PuzzleRule r = make("weight_scales", 40L * tier + s, 1, tier);
            int n = r.layout().groups().get(0).count();
            int[] w = new int[n];
            int total = 0;
            for (int i = 0; i < n; i++) { w[i] = r.height(i); total += w[i]; }
            assertEquals(0, total % 2);
            assertEquals(n, java.util.Arrays.stream(w).distinct().count());
            int[] sol = null;
            for (int m = 1; m < (1 << n) - 1 && sol == null; m++) {
                int sum = 0;
                for (int i = 0; i < n; i++) if ((m >> i & 1) == 1) sum += w[i];
                if (sum * 2 == total) { sol = new int[n]; for (int i = 0; i < n; i++) sol[i] = (m >> i & 1) == 1 ? 1 : 2; }
            }
            assertTrue(sol != null, "no split");
            PuzzleSim sim = new PuzzleSim(r);
            for (int i = 0; i < n; i++) for (int k = 0; k < sol[i]; k++) sim.play(Act.click(0, i, sim.t));   // the bot clicks each stone to its pan (1 click = left, 2 = right)
            assertTrue(r.solved(), "the visible weights did not balance");
        }
    }

    // ---------------------------------------------------------------- water level: the goal is a real measuring job
    @Test
    void theWaterGoalIsNeverASingleFillAndNeedsSeveralClicks() {
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 60; s++) {
            PuzzleRule r = make("water_level", 61L * tier + s, 1, tier);
            int tanks = r.layout().groups().get(0).count();
            int goal = r.height(tanks + 2);
            for (int i = 0; i < tanks; i++) assertTrue(r.height(i) != goal, "the goal equals tank " + i);
            PuzzleSim sim = new PuzzleSim(r);
            assertTrue(sim.solve(60));
            assertTrue(sim.steps >= 4, "solved in only " + sim.steps + " clicks");
        }
    }

    // ---------------------------------------------------------------- the tilting maze is readable from its faces
    @Test
    void aBotThatOnlyReadsTheMazeFacesCanSolveIt() {
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 40; s++) {
            PuzzleRule r = make("gravity_maze", 11L * tier + s, 1, tier);
            Layout.Group g = r.layout().groups().get(0);
            int w = g.cols(), h = g.rows();
            boolean[] wall = new boolean[w * h];
            int ball = -1, goal = -1;
            for (int i = 0; i < w * h; i++) {
                int f = r.face(i, 0);
                wall[i] = f == 1;
                if (f == 2) ball = i;
                if (f == 3) goal = i;
            }
            assertTrue(ball >= 0 && goal >= 0);
            int[] dr = {-1, 0, 1, 0}, dc = {0, 1, 0, -1};
            // plain BFS over resting places, written independently of the rule
            int[] prevMove = new int[w * h], prevCell = new int[w * h];
            java.util.Arrays.fill(prevMove, -2);
            java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
            prevMove[ball] = -1;
            q.add(ball);
            while (!q.isEmpty() && prevMove[goal] == -2) {
                int cur = q.poll();
                for (int d = 0; d < 4; d++) {
                    int rr = cur / w, cc = cur % w;
                    while (true) {
                        int nr = rr + dr[d], nc = cc + dc[d];
                        if (nr < 0 || nr >= h || nc < 0 || nc >= w || wall[nr * w + nc]) break;
                        rr = nr;
                        cc = nc;
                        if (rr * w + cc == goal) break;
                    }
                    int nx = rr * w + cc;
                    if (prevMove[nx] != -2) continue;
                    prevMove[nx] = d;
                    prevCell[nx] = cur;
                    q.add(nx);
                }
            }
            assertTrue(prevMove[goal] != -2, "the hole cannot be reached by reading the faces");
            List<Integer> moves = new ArrayList<>();
            for (int c = goal; c != ball; c = prevCell[c]) moves.add(0, prevMove[c]);
            PuzzleSim sim = new PuzzleSim(r);
            for (int d : moves) sim.play(Act.click(0, w * h + d, sim.t));
            assertTrue(r.solved(), "the marble did not drop into the hole");
        }
    }

    // ---------------------------------------------------------------- pit jump: the legend names a safe colour in every row
    @Test
    void aBotThatOnlyReadsTheLegendCrossesThePitField() {
        for (int tier = 1; tier <= 5; tier++) for (int s = 0; s < 40; s++) {
            PuzzleRule r = make("pit_jump", 13L * tier + s, 1, tier);
            Layout.Group field = r.layout().groups().get(0);
            int cols = field.cols(), rows = field.rows(), base = cols * rows;
            PuzzleSim sim = new PuzzleSim(r);
            for (int row = 0; row < rows; row++) {
                int want = r.face(base + row, 0);
                int pick = -1;
                for (int c = 0; c < cols; c++) if (r.face(row * cols + c, 0) == want) { pick = c; break; }
                assertTrue(pick >= 0, "row " + row + " has no safe tile");
                sim.play(Act.stepOn(0, row * cols + pick, sim.t, 0));
            }
            assertTrue(r.solved());
            assertEquals(0, r.fails(), "a reader should not fall");
        }
    }

    @Test
    void aWrongPitTileCostsAndComesBack() {
        PuzzleRule r = make("pit_jump", 5, 1, 1);
        Layout.Group field = r.layout().groups().get(0);
        int cols = field.cols(), base = cols * field.rows();
        int want = r.face(base, 0), bad = -1;
        for (int c = 0; c < cols; c++) if (r.face(c, 0) != want) bad = c;
        if (bad < 0) return;                                                           // a row where every tile is safe cannot be tested this way
        List<Fx> fx = r.act(Act.stepOn(0, bad, 10, 0));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PIT));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY));
        assertEquals(3, r.face(bad, 0));
        r.tick(10 + 130);
        assertTrue(r.face(bad, 0) != 3, "the tile did not come back");
    }

    // ---------------------------------------------------------------- rune copy: the master is shown, fades on deeper floors and the oracle shows it again
    @Test
    void theRuneMasterIsInViewAtAnyClockAndFadesOnDeeperFloors() {
        for (long start : new long[] {0L, 5_000_000L}) for (int tier = 1; tier <= 5; tier++) {
            PuzzleRule r = make("rune_copy", 3L * tier, 1, tier);
            int n = (r.layout().total() - 2) / 2;
            r.begin(start);
            r.tick(start + 5);
            for (int i = 0; i < n; i++) assertTrue(r.face(i, 0) != 6, "the master is hidden from the start (tier " + tier + ", start " + start + ")");
            r.tick(start + 600);
            boolean hidden = r.face(0, 0) == 6;
            assertEquals(tier > 1, hidden, "tier " + tier + (hidden ? " hid" : " kept") + " its master after 30 seconds");
            if (hidden) {
                r.act(Act.click(0, 2 * n + 1, start + 700));                                    // the oracle stone
                r.tick(start + 705);
                assertTrue(r.face(0, 0) != 6, "the oracle did not show the master again");
            }
        }
    }

    // ---------------------------------------------------------------- the costs and the rules of a few puzzles
    @Test
    void aWrongPlateCostsAndTheRightOrderStillWins() {
        PlateSequence r = (PlateSequence) make("plate_sequence", 8, 1, 1);
        int[] order = r.order();
        int wrong = -1;
        for (int i = 0; i < r.layout().groups().get(0).count(); i++) if (i != order[0]) { wrong = i; break; }
        List<Fx> fx = r.act(Act.stepOn(0, wrong, 5, 0));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY && f.key().equals("hurt")));
        for (int i = 0; i < order.length; i++) r.act(Act.stepOn(0, order[i], 10 + i, 0));
        assertTrue(r.solved());
    }

    @Test
    void aWrongKeyBitesAndTheRightChainOpensEveryLock() {
        PuzzleRule r = make("key_and_lock", 4, 1, 3);
        int k = r.layout().groups().get(0).count();
        List<Fx> fx = r.act(Act.click(0, 1, 1));                                          // key 1 is sealed until lock 0 is open
        assertTrue(fx.stream().noneMatch(f -> f.kind() == Fx.Kind.GIVE));
        fx = r.act(Act.click(0, 0, 2));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.GIVE && f.key().equals("key0")));
        fx = r.act(Act.clickWith(0, k + 1, "key0", 3));                                    // key 0 into lock 1: bites
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY));
        PuzzleSim sim = new PuzzleSim(r);
        assertTrue(sim.solve(100));
    }

    @Test
    void anOfferingOfTheWrongThingIsRefusedWithoutBeingConsumedAndTheRightOneIsAccepted() {
        PuzzleRule r = make("riddle_offering", 6, 1, 1);
        int consumed = 0;
        long t = 1;
        for (String token : Tokens.ITEMS) {
            List<Fx> fx = r.act(Act.clickWith(0, 0, token, t));
            t += 100;
            boolean eat = fx.stream().anyMatch(f -> f.kind() == Fx.Kind.CONSUME);
            boolean cost = fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY);
            if (eat) { consumed++; assertFalse(cost, "the right offering also cost something"); }
            else if (consumed == 0) assertTrue(cost, "a wrong offering was free: " + token);
        }
        assertEquals(1, consumed, "exactly one of the eleven things answers the first riddle");
    }

    @Test
    void theRiddlesAreShuffledAndAWrongAnswerBringsADifferentRiddle() {
        PuzzleRule r = make("riddle_door", 21, 1, 3);
        Fx first = r.begin(0).stream().filter(f -> f.kind() == Fx.Kind.ASK).findFirst().orElseThrow();
        assertEquals(3, first.more().size());
        int wrongPos = -1;
        for (int i = 0; i < 3; i++) if (!first.more().get(i).endsWith(".a0")) { wrongPos = i; break; }
        List<Fx> fx = r.act(Act.choose(0, wrongPos, 5));
        assertTrue(fx.stream().anyMatch(f -> f.kind() == Fx.Kind.PENALTY));
        Fx second = fx.stream().filter(f -> f.kind() == Fx.Kind.ASK).findFirst().orElseThrow();
        assertFalse(second.key().equals(first.key()), "the same riddle came back after a wrong answer");
    }

    @Test
    void theConstellationThreadSnapsWhenItWouldCrossAndAnyNonCrossingFigureWins() {
        for (int s = 0; s < 40; s++) {
            Constellation r = (Constellation) make("constellation", 17L + s, 1, 1 + s % 5);
            PuzzleSim sim = new PuzzleSim(r);
            assertTrue(sim.solve(60));
            assertEquals(r.layout().total(), r.links().size(), "a closed figure has as many threads as stars");
        }
    }

    @Test
    void theEscortFailsWhenNobodyLightsTheWayAndWinsWhenSomebodyDoes() {
        PuzzleRule idle = make("escort_spirit", 2, 1, 5);
        long t = 0;
        boolean cost = false;
        for (int i = 0; i < 400 && !cost; i++) {
            t += 5;
            for (Fx f : idle.tick(t)) cost |= f.kind() == Fx.Kind.PENALTY;
        }
        assertTrue(cost, "an unattended escort should be caught by the shade");
        assertFalse(idle.solved());
        PuzzleSim sim = new PuzzleSim(make("escort_spirit", 2, 1, 5));
        assertTrue(sim.solve(60));
        assertEquals(0, sim.r.fails());
    }

    @Test
    void theLanesPutACaughtDelverBackAtTheStartWithoutLockingAnything() {
        for (String id : new String[] {"sentry_stealth", "pendulum_crossing"}) {
            PuzzleRule r = make(id, 31, 1, 3);
            int w = r.layout().groups().get(0).cols();
            long t = 0;
            r.act(Act.stepOn(0, w * 1 + 0, t, 0));                                         // onto the first lane row and stay
            boolean back = false;
            for (int i = 0; i < 800 && !back; i++) {
                t += 5;
                for (Fx f : r.tick(t)) back |= f.kind() == Fx.Kind.RETURN;
            }
            assertTrue(back, id + ": standing in a lane for a minute should be noticed at some point");
            PuzzleSim sim = new PuzzleSim(make(id, 31, 1, 3));
            assertTrue(sim.solve(60), id);
        }
    }
}
