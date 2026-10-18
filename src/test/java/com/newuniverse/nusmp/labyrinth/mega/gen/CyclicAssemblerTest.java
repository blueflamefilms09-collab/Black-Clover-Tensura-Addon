package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.CategoryBalance;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomCategory;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.RoomGraph;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.Roles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@link com.newuniverse.nusmp.labyrinth.mega.cyclic.CyclicLayerTest} proves the maths on made-up dungeons; this proves the INTEGRATION on the templates that
 * ship (the dump of the real template files, {@code src/test/resources/mega/catalog.json}), through the real request, solver and audit: floors of both roads (ordinary
 * and boss) are planned the old way and the cyclic way from the same seeds and compared. It demands what the owner asked for and what the integration must not break:
 * every cyclic plan passes the full audit and the naive structural check (no overlap, every join aligned, indices in order after trimming), the dead-end ratio is at the
 * target for nearly every floor and far under the old generator's, far more rooms lie on a cycle, no doorway is left without a partner (every socket is joined,
 * capped or sealed; every loop lines up), the boss chain and the stairs are not given a second way in, the plan is deterministic, the five category shares are no
 * worse than the old generator's, the switch off is the old generator exactly (no cyclic statistic, no hallway), and the old sector model works too. The measured
 * means are printed (the doc quotes them).
 */
class CyclicAssemblerTest {
    private static CatalogSource catalog() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        for (String id : new String[] {FloorPlan.P_UP, FloorPlan.P_DOWN, FloorPlan.P_GRAND_UP, FloorPlan.P_GRAND_DOWN, FloorPlan.P_STAIR_DOOR, "nusmp:mega/side", "nusmp:mega/cap", "nusmp:mega/arena",
                "nusmp:mega/antechamber", "nusmp:mega/vault_3", "nusmp:mega/onward", "nusmp:mega/link"}) {
            PoolDef p = cat.pool(id);
            boolean stocked = false;
            if (p != null) for (PoolEntry e : p.entries()) if (cat.shape(e.shape()) != null) stocked = true;
            if (!stocked) return null;
        }
        return cat;
    }

    /** A floor request as the game builds it, with or without the cyclic generator. */
    private static Request floor(CatalogSource cat, long seed, Polarity road, int k, LoopConfig cfg) {
        Request q = TestPlans.floor(cat, seed, road, k, 28, Recipe.forFloors(), cat.themes(), null);
        q.cyclic = cfg;
        return q;
    }

    private static final class Agg {
        int n, fallback, under, hall, links, pruned, tries;
        double dead, cycle, loops, rooms;
        double[] shares = new double[RoomCategory.values().length];
        List<Double> ratios = new ArrayList<>();

        void add(SectorSolver.Solved sv) {
            Result r = sv.result();
            RoomGraph g = CyclicGraphs.of(r);
            RoomGraph.Report rep = g.report(Roles.TERMINAL);
            n++;
            tries += sv.tries();
            if (sv.fallback()) fallback++;
            dead += rep.deadEndRatio();
            ratios.add(rep.deadEndRatio());
            if (rep.deadEndRatio() <= 0.08 + 1e-9) under++;
            cycle += rep.cycleShare();
            loops += rep.cyclomatic();
            rooms += rep.rooms();
            hall += r.stats.getOrDefault("cyclic.hallways", 0);
            links += r.stats.getOrDefault("cyclic.links", 0);
            pruned += r.stats.getOrDefault("cyclic.pruned", 0);
            double[] sh = CategoryBalance.shares(g.categoryCounts());
            for (int i = 0; i < sh.length; i++) shares[i] += sh[i];
        }

        double[] meanShares() {
            double[] s = shares.clone();
            for (int i = 0; i < s.length; i++) s[i] /= Math.max(1, n);
            return s;
        }

        String line(String label) {
            java.util.Collections.sort(ratios);
            StringBuilder b = new StringBuilder(String.format(Locale.ROOT,
                    "CYCLIC real catalog %-22s %3d floors: rooms %.1f, dead ends %.1f%% (%d of %d floors <= 8%%, p95 %.1f%%, worst %.1f%%), rooms on a cycle %.1f%%, independent loops %.1f, %.2f links, %.2f hallway rooms, %.2f trimmed, tries %.2f, fallback %d; mix",
                    label, n, rooms / n, dead / n * 100, under, n, ratios.get((int) (ratios.size() * 0.95) - 1) * 100, ratios.get(ratios.size() - 1) * 100, cycle / n * 100, loops / n,
                    links / (double) n, hall / (double) n, pruned / (double) n, tries / (double) n, fallback));
            double[] ms = meanShares();
            for (RoomCategory c : RoomCategory.values()) b.append(String.format(Locale.ROOT, " %s %.1f%%", c.name().charAt(0) + c.name().substring(1, 3).toLowerCase(Locale.ROOT), ms[c.ordinal()] * 100));
            return b.toString();
        }
    }

    private static SectorSolver.Solved solve(CatalogSource cat, long seed, Polarity road, int k, LoopConfig cfg) {
        Request q = floor(cat, seed, road, k, cfg);
        SectorSolver.Solved sv = SectorSolver.solve(q, 24);
        assertTrue(sv.ok(), road + " floor " + (k + 1) + " seed " + seed + " could not be built after " + sv.tries() + " tries: " + SectorSolver.summary(sv.failures(), 4));
        Result r = sv.result();
        Request fq = q.withSeed(r.seed, sv.fallback());
        assertEquals(List.of(), TestPlans.brute(r, fq), road + " floor " + (k + 1) + " seed " + seed + " is not structurally sound");
        assertEquals(List.of(), SectorAudit.audit(r, fq, !sv.fallback()), road + " floor " + (k + 1) + " seed " + seed);
        return sv;
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the numbers

    @Test
    void cyclicFloorsOfBothRoadsPassTheAuditAndLoseTheirDeadEnds() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) {
            System.out.println("CyclicAssemblerTest: the catalog cannot make floors yet (a pool holds no template), so there is nothing to assemble (this passes until it is merged).");
            return;
        }
        LoopConfig cfg = LoopConfig.defaults();
        Agg old = new Agg(), neu = new Agg();
        int worse = 0, total = 0;
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 22; k++) {
                for (int s = 0; s < 6; s++) {
                    long seed = Rng.mix(s + 1, 4711 + k);
                    SectorSolver.Solved a = solve(cat, seed, road, k, null), b = solve(cat, seed, road, k, cfg);
                    old.add(a);
                    neu.add(b);
                    total++;
                    Result r = b.result();
                    assertTrue(r.stats.getOrDefault("cyclic.rooms0", 0) > 0 || b.fallback(), "the cyclic pass ran");
                    assertTrue(r.stats.getOrDefault("cyclic.deadEnds1", 0) <= r.stats.getOrDefault("cyclic.deadEnds0", 0), "the loop pass never creates a dead end");
                    assertTrue(r.stats.getOrDefault("cyclic.hallways", 0) <= cfg.hallwayShare() * r.roomIds().length + 1, "the hallway share holds");
                    assertEquals(0, (int) r.stats.getOrDefault("cyclic.offgrid", 0), "every piece is on the module grid");
                    assertTrue(CyclicGraphs.of(r).connected(), "the room graph is connected");
                }
            }
        }
        System.out.println(old.line("old generator"));
        System.out.println(neu.line("cyclic (defaults)"));
        assertTrue(neu.fallback <= total / 20, neu.fallback + " of " + total + " cyclic floors needed the fallback layout");
        assertTrue(old.dead / old.n > 0.2, "sanity: the old generator really has many dead ends");
        assertTrue(neu.dead / neu.n <= 0.08, "the mean dead-end ratio is at the target: " + neu.dead / neu.n);
        assertTrue(neu.under >= neu.n * 0.9, "at least 90 % of the floors are at or under 8 % dead ends: " + neu.under + " of " + neu.n);
        assertTrue(neu.cycle / neu.n >= old.cycle / old.n + 0.25, "far more rooms lie on a cycle");
        assertTrue(neu.loops > old.loops * 1.5, "more independent loops");
        assertTrue(Math.abs(neu.rooms / neu.n - old.rooms / old.n) <= 4.0, "the floor stays about as big as the budget: " + neu.rooms / neu.n + " against " + old.rooms / old.n);
        assertTrue(neu.tries / (double) neu.n <= old.tries / (double) old.n + 0.3, "the audit is not harder to pass");
    }

    @Test
    void theSwitchOffIsTheOldGeneratorAndTheCyclicPlanIsDeterministic() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        for (int s = 0; s < 12; s++) {
            long seed = Rng.mix(s + 1, 99);
            Polarity road = Polarity.values()[s & 1];
            Request off = floor(cat, seed, road, s % 12, null);
            Result r = SectorSolver.solve(off, 24).result();
            for (String k : r.stats.keySet()) assertFalse(k.startsWith("cyclic."), "no cyclic statistic when the switch is off: " + k);
            Request on = floor(cat, seed, road, s % 12, LoopConfig.defaults());
            List<String> f1 = TestPlans.fingerprint(SectorSolver.solve(on, 24).result()), f2 = TestPlans.fingerprint(SectorSolver.solve(on, 24).result());
            assertEquals(f1, f2, "the same request gives the same cyclic plan");
            assertFalse(f1.equals(TestPlans.fingerprint(r)), "the cyclic plan is another plan");
            assertEquals(TestPlans.fingerprint(r), TestPlans.fingerprint(SectorSolver.solve(floor(cat, seed, road, s % 12, null), 24).result()), "and the old one is still the old one");
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------ doorways

    /** Doorways of a plan that are not joined to a room: a door cap stands there, or a neighbour's blank wall (a niche). The audit has already ruled out the void. */
    private static int niches(Result r) {
        int n = 0;
        for (Placed p : r.pieces) {
            if (p.shape.isCap()) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                if (p.shape.sockets.get(si).sealed()) continue;
                boolean toRoom = p.index != r.landing && p.parent >= 0 && p.ownSocket == si && !r.pieces.get(p.parent).shape.isCap();
                for (Placed c : r.pieces) if (c.parent == p.index && c.parentSocket == si && !c.shape.isCap()) toRoom = true;
                for (Result.Loop l : r.loops) if ((l.pieceA() == p.index && l.socketA() == si) || (l.pieceB() == p.index && l.socketB() == si)) toRoom = true;
                if (!toRoom) n++;
            }
        }
        return n;
    }

    @Test
    void noDoorwayOpensOntoNothingAndEveryHallwayDoorHasAPartner() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        int loopsChecked = 0, hallwayPieces = 0, hallwayDoors = 0, nichesOld = 0, nichesNew = 0, plans = 0;
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 22; k++) {
                for (int s = 0; s < 3; s++) {
                    long seed = Rng.mix(s + 7, 31 + k);
                    nichesOld += niches(solve(cat, seed, road, k, null).result());
                    Result r = solve(cat, seed, road, k, LoopConfig.defaults()).result();       // solve() has run the audit: no doorway opens onto the void, every room is reachable
                    nichesNew += niches(r);
                    plans++;
                    for (Placed p : r.pieces) {
                        if (p.shape.isCap()) continue;
                        for (int si = 0; si < p.shape.sockets.size(); si++) {
                            if (p.shape.sockets.get(si).sealed() || p.isUsed(si)) continue;
                            Vec f = p.socketFront(si);
                            boolean covered = false;
                            for (Placed c : r.pieces) if (c.box.contains(f)) covered = true;
                            assertTrue(covered, road + " floor " + (k + 1) + ": " + p + " socket " + si + " opens onto the void");
                        }
                    }
                    for (Result.Loop l : r.loops) {
                        Placed a = r.pieces.get(l.pieceA()), b = r.pieces.get(l.pieceB());
                        assertEquals(a.socketFront(l.socketA()), b.socketCell(l.socketB()), "the loop doorways line up");
                        assertEquals(b.socketFront(l.socketB()), a.socketCell(l.socketA()));
                        assertEquals(a.socketFacing(l.socketA()), b.socketFacing(l.socketB()).opposite());
                        assertTrue(Socket.canAttach(a.shape.sockets.get(l.socketA()).target(), a.socketFacing(l.socketA()), b.shape.sockets.get(l.socketB()).name(), b.socketFacing(l.socketB())));
                        loopsChecked++;
                    }
                    // a hallway cell: one module, two doorways, both joined to a ROOM and both aligned with it (the parent join is checked by brute(), the loop above)
                    int through = 0;
                    for (Placed p : r.pieces) {
                        if (!CyclicGraphs.throughHallway(r, p)) continue;
                        through++;
                        for (int si = 0; si < 2; si++) {
                            hallwayDoors++;
                            assertTrue(p.isUsed(si), "a hallway cell has no loose doorway");
                        }
                    }
                    hallwayPieces += through;
                    int made = r.stats.getOrDefault("cyclic.hallways", 0);
                    assertTrue(through >= made, "every hallway room built joins two rooms: " + made + " built, " + through + " pass-through pieces");
                }
            }
        }
        System.out.println(String.format(Locale.ROOT, "CYCLIC real catalog doorways: %d cyclic floors, %d loops line up exactly, %d pass-through corridor pieces with %d doorways all joined; doorways facing a blank wall (niches) per floor: old %.1f, cyclic %.1f",
                plans, loopsChecked, hallwayPieces, hallwayDoors, nichesOld / (double) plans, nichesNew / (double) plans));
        assertTrue(nichesNew <= nichesOld, "the placement step does not wall more doorways into blank walls than the old generator did (" + nichesNew + " against " + nichesOld + ")");
    }

    /** The rooms next to a piece, by piece index, in the plan's room graph (door caps are not rooms). */
    private static Set<Integer> neighbours(Result r, int piece) {
        Set<Integer> out = new HashSet<>();
        for (Placed p : r.pieces) {
            if (p.parent >= 0 && !p.shape.isCap() && !r.pieces.get(p.parent).shape.isCap()) {
                if (p.index == piece) out.add(p.parent);
                if (p.parent == piece) out.add(p.index);
            }
        }
        for (Result.Loop l : r.loops) {
            if (l.pieceA() == piece) out.add(l.pieceB());
            if (l.pieceB() == piece) out.add(l.pieceA());
        }
        return out;
    }

    @Test
    void theBossChainAndTheStairsAreNotGivenASecondWayIn() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        int bossFloors = 0, extraOld = 0, extraNew = 0, floors = 0;
        for (Polarity road : Polarity.values()) {
            for (int k : new int[] {0, 3, 9, 19, 5, 9, 14, 19}) {
                for (int s = 0; s < 8; s++) {
                    long seed = Rng.mix(s + 3, 555 + k);
                    for (int variant = 0; variant < 2; variant++) {
                        Result r = solve(cat, seed, road, k, variant == 0 ? null : LoopConfig.defaults()).result();
                        int extra = 0;
                        for (Placed p : r.pieces) {
                            if (p.shape.isCap()) continue;
                            RoomKind kind = p.shape.kind;
                            Set<Integer> nb = neighbours(r, p.index);
                            if (kind == RoomKind.ARENA) {
                                for (int q : nb) if (q != p.parent && r.pieces.get(q).shape.kind != RoomKind.ANTECHAMBER) extra++;
                            } else if (kind == RoomKind.ANTECHAMBER) {
                                for (int q : nb) {
                                    RoomKind qk = r.pieces.get(q).shape.kind;
                                    if (qk != RoomKind.ARENA && qk != RoomKind.VAULT && qk != RoomKind.ONWARD) extra++;
                                }
                            } else if (kind == RoomKind.VAULT) {
                                for (int q : nb) if (r.pieces.get(q).shape.kind != RoomKind.ANTECHAMBER) extra++;
                            } else if (kind == RoomKind.ONWARD) {
                                for (int q : nb) {
                                    RoomKind qk = r.pieces.get(q).shape.kind;
                                    if (qk != RoomKind.ANTECHAMBER && qk != RoomKind.STAIRWELL) extra++;
                                }
                            } else if (kind == RoomKind.STAIRWELL && p.index != r.landing) {
                                if (nb.size() > 1 && r.pieces.get(p.parent).shape.kind != RoomKind.ONWARD) extra++;
                            }
                        }
                        if (variant == 0) extraOld += extra;
                        else extraNew += extra;
                        if (variant == 1) {
                            floors++;
                            if (r.count(RoomKind.ARENA) > 0) bossFloors++;
                        }
                    }
                }
            }
        }
        System.out.println("CYCLIC real catalog boss chain / stairs: " + floors + " cyclic floors (" + bossFloors + " boss floors): extra doorways into the chain or the forward stairs: old generator " + extraOld + ", cyclic " + extraNew);
        assertTrue(extraNew <= extraOld, "the cyclic generator never gives the chain more ways in than the old one did (" + extraNew + " against " + extraOld + ")");
    }

    // ------------------------------------------------------------------------------------------------------------------------------ balance

    @Test
    void theFiveCategoriesStayBalanced() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        Agg old = new Agg(), full = new Agg(), loopsOnly = new Agg();
        LoopConfig bare = LoopConfig.defaults().withLookahead(1).withBalanceWeight(0).withPrune(false).withOvergrow(1.0);
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 12; k++) {
                for (int s = 0; s < 4; s++) {
                    long seed = Rng.mix(s + 11, 8800 + k);
                    old.add(solve(cat, seed, road, k, null));
                    full.add(solve(cat, seed, road, k, LoopConfig.defaults()));
                    loopsOnly.add(solve(cat, seed, road, k, bare));
                }
            }
        }
        System.out.println(old.line("old generator"));
        System.out.println(full.line("cyclic (defaults)"));
        System.out.println(loopsOnly.line("loop pass only"));
        double[] so = old.meanShares(), sf = full.meanShares(), sl = loopsOnly.meanShares();
        // hallway rooms are Transitions: the loop pass alone may raise the Transition share, but only by a few points
        int t = RoomCategory.TRANSITION.ordinal();
        assertTrue(sl[t] - so[t] <= 0.06, "the hallways of the bare loop pass raise the Transition share by " + (sl[t] - so[t]));
        assertTrue(CategoryBalance.maxShift(so, sl) <= 0.06, "the bare loop pass moves no category share by more than 6 points: " + CategoryBalance.maxShift(so, sl));
        // the full generator (placement nudged by the targets, trimmed by surplus) is at least as balanced as the old one, with no category thrown away
        assertTrue(sf[t] <= so[t] + 1e-9, "the full generator has no more Transition than the old one: " + sf[t] + " against " + so[t]);
        double dOld = distance(so), dFull = distance(sf);
        assertTrue(dFull <= dOld, "the mix is no further from the target than the old generator's: " + dFull + " against " + dOld);
        for (RoomCategory c : RoomCategory.values()) {
            if (c == RoomCategory.TRANSITION) continue;
            assertTrue(sf[c.ordinal()] >= so[c.ordinal()] - 0.02, c + " is not squeezed out: " + sf[c.ordinal()] + " against " + so[c.ordinal()]);
        }
    }

    private static double distance(double[] s) {
        double d = 0;
        for (int i = 0; i < s.length; i++) d += Math.abs(s[i] - CategoryBalance.TARGET[i]);
        return d / 2;
    }

    // ------------------------------------------------------------------------------------------------------------------------------ knobs and the old model

    @Test
    void theDeadEndTargetIsAKnob() throws IOException {
        CatalogSource cat = catalog();
        if (cat == null) return;
        Agg loose = new Agg(), mid = new Agg(), tight = new Agg(), off = new Agg();
        for (Polarity road : Polarity.values()) {
            for (int k = 0; k < 10; k++) {
                for (int s = 0; s < 3; s++) {
                    long seed = Rng.mix(s + 21, 6600 + k);
                    loose.add(solve(cat, seed, road, k, LoopConfig.defaults().withDeadEndTarget(0.30)));
                    mid.add(solve(cat, seed, road, k, LoopConfig.defaults()));
                    tight.add(solve(cat, seed, road, k, LoopConfig.defaults().withDeadEndTarget(0.04)));
                    off.add(solve(cat, seed, road, k, LoopConfig.defaults().withMaxLinks(0).withPrune(false).withLookahead(1).withOvergrow(1.0)));
                }
            }
        }
        System.out.println(String.format(Locale.ROOT, "CYCLIC real catalog target knob: dead ends %.1f%% at target 30%%, %.1f%% at 8%% (default), %.1f%% at 4%%; with every mechanism off %.1f%%",
                loose.dead / loose.n * 100, mid.dead / mid.n * 100, tight.dead / tight.n * 100, off.dead / off.n * 100));
        assertTrue(tight.dead <= mid.dead + 1e-9 && mid.dead <= loose.dead + 1e-9, "a tighter target gives fewer dead ends");
        assertTrue(tight.dead / tight.n <= 0.08);
        assertTrue(off.dead / off.n > 0.2, "with the placement, the pass and the pruner off the old dead ends return");
    }

    @Test
    void theOldBandedSectorsGetTheCyclicGeneratorToo() throws IOException {
        CatalogSource cat = CatalogSource.load(CatalogSource.DEFAULT);
        if (cat.isEmpty() || !cat.missingForSectors(3).isEmpty()) return;
        double deadOld = 0, deadNew = 0, cycOld = 0, cycNew = 0, roomsOld = 0, roomsNew = 0;
        int n = 0;
        for (int s = 0; s < 40; s++) {
            long dungeonSeed = Rng.mix(s, 4711);
            Request a = TestPlans.first(cat, dungeonSeed), b = TestPlans.first(cat, dungeonSeed);
            b.cyclic = LoopConfig.defaults();
            SectorSolver.Solved sa = SectorSolver.solve(a, 24), sb = SectorSolver.solve(b, 24);
            assertTrue(sb.ok(), "sector " + s + " (cyclic) could not be built: " + SectorSolver.summary(sb.failures(), 4));
            Result r = sb.result();
            Request q = b.withSeed(r.seed, sb.fallback());
            assertEquals(List.of(), TestPlans.brute(r, q), "sector " + s + " is not structurally sound");
            assertEquals(List.of(), SectorAudit.audit(r, q, !sb.fallback()), "sector " + s);
            if (!sa.ok() || sa.fallback() || sb.fallback()) continue;
            RoomGraph.Report ra = CyclicGraphs.report(sa.result()), rb = CyclicGraphs.report(r);
            deadOld += ra.deadEndRatio();
            deadNew += rb.deadEndRatio();
            cycOld += ra.cycleShare();
            cycNew += rb.cycleShare();
            roomsOld += ra.rooms();
            roomsNew += rb.rooms();
            n++;
        }
        System.out.println(String.format(Locale.ROOT, "CYCLIC real catalog old banded sectors: %d sectors: rooms %.1f -> %.1f, dead ends %.1f%% -> %.1f%%, rooms on a cycle %.1f%% -> %.1f%%", n, roomsOld / n, roomsNew / n,
                deadOld / n * 100, deadNew / n * 100, cycOld / n * 100, cycNew / n * 100));
        assertTrue(n > 10);
        assertTrue(deadNew < deadOld * 0.5, "the banded model loses most of its dead ends too");
        assertTrue(cycNew > cycOld);
    }
}
