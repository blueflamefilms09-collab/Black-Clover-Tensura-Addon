package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the audits are the generator's own referee: a floor that reaches the world has passed {@link SectorAudit}, and a floor that is saved is judged again by
 * {@link SavedAudit} on every {@code /multiverse mega audit} and by the CI smoke test. A referee that is only ever shown good floors proves nothing, so this test breaks
 * ONE invariant of the floor model at a time (a boss chain on an ordinary floor, a boss floor without one, stairs next to the door, too few rooms, a missing exit marker,
 * an exit in the wrong stairwell, two stairs in one cell, a third stairwell, a missing one) and checks that the audit names exactly that, and that the unbroken floor passes.
 */
class FloorAuditTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.floors();

    private static Result plan(Request q) {
        SectorSolver.Solved s = SectorSolver.solve(q, 24);
        assertTrue(s.ok(), "the floor must be solved: " + SectorSolver.summary(s.failures(), 3));
        assertFalse(s.fallback());
        return s.result();
    }

    private static boolean says(List<String> failures, String fragment) {
        for (String f : failures) if (f.contains(fragment)) return true;
        return false;
    }

    private static String listing(List<String> failures) { return String.join(" | ", failures); }

    /** A request of the same floor but judged by another floor spec (the plan stays as it was). */
    private static Request asFloor(Request q, Request.Floor fl) {
        Request c = q.withSeed(q.seed, false);
        c.floor = fl;
        return c;
    }

    // ================================================================================================================ the plan, judged by SectorAudit

    @Test
    void anUnbrokenOrdinaryFloorAndBossFloorPassTheFullAudit() {
        for (Polarity road : Polarity.values()) {
            for (int chain : new int[] {0, 2, 9}) {
                Request q = TestPlans.floor(CAT, 11, road, chain);
                Result r = plan(q);
                assertEquals(List.of(), SectorAudit.audit(r, q, true), road.id() + " floor " + (chain + 1));
            }
        }
    }

    @Test
    void everyFloorHasASafeZoneWithinReachOfTheArrivalAndTheAuditNamesItWhenItDoesNot() {
        for (Polarity road : Polarity.values()) {
            for (int chain : new int[] {0, 3, 9}) {
                Request q = TestPlans.floor(CAT, 33, road, chain);
                Result r = plan(q);
                int safe = 0;
                for (Placed p : r.pieces) if (p.shape.kind == RoomKind.SAFE) safe++;
                assertTrue(safe >= 1, road.id() + " floor " + (chain + 1) + " has a Safe Zone");
                Request strict = q.withSeed(q.seed, false);
                strict.recipe = q.recipe.copy();
                strict.recipe.extraSafe = safe + 1;                      // asks for one more than the floor has
                assertTrue(says(SectorAudit.audit(r, strict, true), "extra safe room(s)"), "the audit names a missing Safe Zone");
            }
        }
    }

    @Test
    void aBossChainOnAnOrdinaryFloorIsNamed() {
        Request boss = TestPlans.floor(CAT, 5, Polarity.HOLY, 9);
        Result r = plan(boss);
        Request.Floor fl = boss.floor;
        assertTrue(fl.boss());
        List<String> bad = SectorAudit.audit(r, asFloor(boss, new Request.Floor(false, fl.arrivalPool(), fl.forwardPool(), fl.rooms(), fl.minRooms())), true);
        assertTrue(says(bad, "an ordinary floor has 1 arena room(s)"), listing(bad));
        assertTrue(says(bad, "only a boss floor has the boss chain"), listing(bad));
    }

    @Test
    void aBossFloorWithoutTheChainIsNamed() {
        Request ord = TestPlans.floor(CAT, 5, Polarity.ABYSSAL, 3);
        Result r = plan(ord);
        Request.Floor fl = ord.floor;
        assertFalse(fl.boss());
        List<String> bad = SectorAudit.audit(r, asFloor(ord, new Request.Floor(true, fl.arrivalPool(), fl.forwardPool(), fl.rooms(), fl.minRooms())), true);
        assertTrue(says(bad, "expected exactly one arena, found 0"), listing(bad));
        assertTrue(says(bad, "no GATEKEEPER marker"), listing(bad));
    }

    @Test
    void stairsThatAreNextToTheDoorAreNamedInTheFullAuditOnly() {
        Request q = TestPlans.floor(CAT, 9, Polarity.HOLY, 1);
        Result r = plan(q);
        Request tight = q.withSeed(q.seed, false);
        tight.recipe = q.recipe.copy();
        tight.recipe.stairMinWalk = 60;
        List<String> strict = SectorAudit.audit(r, tight, true);
        assertTrue(says(strict, "doorways from the arrival (the recipe wants 60)"), listing(strict));
        assertFalse(says(SectorAudit.audit(r, tight, false), "doorways from the arrival"), "the fallback layout skips the pacing checks");
    }

    @Test
    void aFloorWithTooFewRoomsIsNamed() {
        Request q = TestPlans.floor(CAT, 9, Polarity.ABYSSAL, 0);
        Result r = plan(q);
        Request.Floor fl = q.floor;
        List<String> bad = SectorAudit.audit(r, asFloor(q, new Request.Floor(false, fl.arrivalPool(), fl.forwardPool(), 100, 90)), true);
        assertTrue(says(bad, "(the floor wants 90)"), listing(bad));
    }

    @Test
    void theAuditJudgesAFloorByItsSpecAndAnOldSectorByTheOldRules() {
        Request q = TestPlans.floor(CAT, 9, Polarity.HOLY, 0);
        Result r = plan(q);
        Request old = q.withSeed(q.seed, false);
        old.floor = null;
        List<String> bad = SectorAudit.audit(r, old, true);
        assertTrue(says(bad, "not a Landing"), "a stairwell is no Landing for the old audit: " + listing(bad));
    }

    @Test
    void theFallbackFloorIsStillAFloorWithTwoStairwellsAndPassesTheStructuralAudit() {
        for (Polarity road : Polarity.values()) {
            for (int chain : new int[] {2, 9}) {
                for (long seed = 1; seed <= 12; seed++) {
                    Request q = TestPlans.floor(CAT, seed, road, chain);
                    Request fb = q.withSeed(seed, true);
                    Result r = MegaAssembler.assemble(fb);
                    String who = road.id() + " floor " + (chain + 1) + " seed " + seed;
                    assertTrue(r.assembled(), who + " must assemble as a fallback: " + r.failure);
                    assertEquals(List.of(), SectorAudit.audit(r, fb, false), who + " (the fallback skips the pacing checks, never the structural ones)");
                    int stairwells = 0;
                    for (Placed p : r.pieces) if (p.shape.kind == RoomKind.STAIRWELL) stairwells++;
                    assertEquals(2, stairwells, who);
                    assertEquals(q.floor.boss() ? 1 : 0, r.count(RoomKind.ARENA), who + ": the boss chain is for boss floors only");
                }
            }
        }
    }

    // ================================================================================================================ the saved floor, judged by SavedAudit

    private static final class V {
        SectorView v;
        List<SectorFill.RoomOut> rooms;
        List<SectorView.MarkerView> markers;
        Vec arrival, stairBack, stairForward, stairForwardPad;

        V(SectorView v) {
            this.v = v;
            rooms = new ArrayList<>(v.rooms());
            markers = new ArrayList<>(v.markers());
            arrival = v.arrival();
            stairBack = v.stairBack();
            stairForward = v.stairForward();
            stairForwardPad = v.stairForwardPad();
        }

        SectorView build() {
            return new SectorView(v.id(), v.state(), v.region(), rooms, v.edges(), markers, v.relicCount(), v.pedestals(), arrival, v.guardianSpawn(), v.gatekeeper(), v.vaultCenter(),
                    v.seal(), v.sealFacing(), v.nextId(), v.floorModel(), v.bossFloor(), stairBack, stairForward, stairForwardPad);
        }
    }

    private static SectorView saved(Request q, Result r) {
        return SectorFill.of(r, q.region).view(0, "ACTIVE", q.region, q.relicCount, -1, true, q.floor.boss());
    }

    private static List<String> audit(SectorView v) { return SavedAudit.audit(v, null, true); }

    @Test
    void aSavedFloorThatIsWholePassesAndHasItsStairs() {
        for (int chain : new int[] {0, 4, 9}) {
            Request q = TestPlans.floor(CAT, 21, Polarity.HOLY, chain);
            SectorView v = saved(q, plan(q));
            assertEquals(List.of(), audit(v), "floor " + (chain + 1));
            assertTrue(v.stairBack() != null && v.stairForward() != null && v.stairForwardPad() != null, "the fill found the stairs");
        }
    }

    @Test
    void aSavedFloorWithoutItsExitMarkersOrCellsIsNamed() {
        Request q = TestPlans.floor(CAT, 21, Polarity.HOLY, 3);
        SectorView good = saved(q, plan(q));
        V noExit = new V(good);
        noExit.markers.removeIf(m -> m.kind() == MarkerKind.STAIR_EXIT);
        assertTrue(says(audit(noExit.build()), "0 stair_exit markers were saved"), listing(audit(noExit.build())));
        V noBack = new V(good);
        noBack.stairBack = null;
        assertTrue(says(audit(noBack.build()), "no stair that leads back"), listing(audit(noBack.build())));
        V noOn = new V(good);
        noOn.stairForward = null;
        assertTrue(says(audit(noOn.build()), "no stair that leads on"), listing(audit(noOn.build())));
        V noPad = new V(good);
        noPad.stairForwardPad = null;
        assertTrue(says(audit(noPad.build()), "no cell for a party that comes back"), listing(audit(noPad.build())));
        V noArrivalMarker = new V(good);
        noArrivalMarker.markers.removeIf(m -> m.kind() == MarkerKind.ARRIVAL);
        assertTrue(says(audit(noArrivalMarker.build()), "no arrival marker was saved"), listing(audit(noArrivalMarker.build())));
    }

    @Test
    void anExitInTheWrongStairwellOrTwoStairsInOneCellAreNamed() {
        Request q = TestPlans.floor(CAT, 21, Polarity.ABYSSAL, 5);
        SectorView good = saved(q, plan(q));
        V swapped = new V(good);
        swapped.stairBack = good.stairForward();
        swapped.stairForward = good.stairBack();
        List<String> bad = audit(swapped.build());
        assertTrue(says(bad, "is not inside the arrival stairwell"), listing(bad));
        assertTrue(says(bad, "is not inside the forward stairwell"), listing(bad));
        V same = new V(good);
        same.stairForward = good.stairBack();
        assertTrue(says(audit(same.build()), "both stairs end in the same cell"), listing(audit(same.build())));
        V pad = new V(good);
        pad.stairForwardPad = good.stairBack();
        assertTrue(says(audit(pad.build()), "the cell for a returning party"), listing(audit(pad.build())));
        V away = new V(good);
        away.arrival = good.stairForward();
        assertTrue(says(audit(away.build()), "is not inside the arrival stairwell"), listing(audit(away.build())));
    }

    @Test
    void aThirdStairwellOrAMissingOneIsNamed() {
        Request q = TestPlans.floor(CAT, 21, Polarity.HOLY, 6);
        SectorView good = saved(q, plan(q));
        V fewer = new V(good);
        for (int i = 0; i < fewer.rooms.size(); i++) {
            if (fewer.rooms.get(i).kind() == RoomKind.STAIRWELL && !fewer.rooms.get(i).box().contains(good.arrival())) {
                fewer.rooms.remove(i);
                break;
            }
        }
        assertTrue(says(audit(fewer.build()), "expected exactly two stairwells (arrival and forward) but the sector has 1"), listing(audit(fewer.build())));
        V more = new V(good);
        SectorFill.RoomOut any = more.rooms.stream().filter(x -> x.kind() == RoomKind.STAIRWELL).findFirst().orElseThrow();
        more.rooms.add(new SectorFill.RoomOut(900, any.template(), RoomKind.STAIRWELL, any.box()));
        assertTrue(says(audit(more.build()), "but the sector has 3"), listing(audit(more.build())));
    }

    @Test
    void anOrdinaryFloorWithABossRoomAndABossFloorWithoutOneAreNamed() {
        Request ord = TestPlans.floor(CAT, 21, Polarity.HOLY, 1);
        SectorView good = saved(ord, plan(ord));
        V arena = new V(good);
        SectorFill.RoomOut any = arena.rooms.stream().filter(x -> x.kind() != RoomKind.STAIRWELL).findFirst().orElseThrow();
        arena.rooms.add(new SectorFill.RoomOut(901, any.template(), RoomKind.ARENA, any.box()));
        assertTrue(says(audit(arena.build()), "an ordinary floor has a boss chain"), listing(audit(arena.build())));

        Request bossQ = TestPlans.floor(CAT, 21, Polarity.HOLY, 9);
        SectorView boss = saved(bossQ, plan(bossQ));
        V noOnward = new V(boss);
        noOnward.rooms.removeIf(x -> x.kind() == RoomKind.ONWARD);
        assertFalse(audit(noOnward.build()).isEmpty(), "a boss floor without its Way Onward does not pass");
        assertTrue(says(audit(noOnward.build()), "the sector has no Way Onward corridor"), listing(audit(noOnward.build())));
        V asOrdinary = new V(boss);
        SectorView wrong = new SectorView(boss.id(), boss.state(), boss.region(), asOrdinary.rooms, boss.edges(), asOrdinary.markers, boss.relicCount(), boss.pedestals(), boss.arrival(),
                boss.guardianSpawn(), boss.gatekeeper(), boss.vaultCenter(), boss.seal(), boss.sealFacing(), boss.nextId(), true, false, boss.stairBack(), boss.stairForward(), boss.stairForwardPad());
        assertTrue(says(audit(wrong), "an ordinary floor has a boss chain"), "a boss floor saved as an ordinary one: " + listing(audit(wrong)));
    }
}
