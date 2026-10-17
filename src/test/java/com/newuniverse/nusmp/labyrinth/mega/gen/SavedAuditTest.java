package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@code /multiverse mega audit} judges the saved sector, and the CI smoke test trusts its answer, so each promise it checks is tested the same way: a
 * view made from real assembler output passes, and breaking exactly one thing in a copy of that view (a room moved, a marker dropped, a doorway cut) makes
 * exactly that sentence appear. The views are made through {@link SectorFill}, the same path the generator uses to fill the saved sector, so the test also
 * pins the landmarks it copies (the arrival cell, the guardian spawn, the pedestals one cell below their markers).
 */
class SavedAuditTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    private static Request req;
    private static Result plan;
    private static SectorView good;

    private static void ensure() {
        if (good != null) return;
        for (long s = 0; s < 30 && good == null; s++) {
            Request q = TestPlans.first(CAT, s);
            SectorSolver.Solved sv = SectorSolver.solve(q, 24);
            if (sv.ok() && !sv.fallback()) {
                req = q.withSeed(sv.result().seed, false);
                plan = sv.result();
                good = SectorFill.of(plan, req.region).view(0, "ACTIVE", req.region, req.relicCount, -1);
            }
        }
        assertNotNull(good, "a good plan to start from");
    }

    private static boolean has(List<String> a, String part) {
        for (String s : a) if (s.contains(part)) return true;
        return false;
    }

    private static List<String> run(SectorView v) { return SavedAudit.audit(v, Set.of(0, 1), true); }

    private static SectorView rooms(SectorView v, List<SectorFill.RoomOut> rooms) {
        return new SectorView(v.id(), v.state(), v.region(), rooms, v.edges(), v.markers(), v.relicCount(), v.pedestals(), v.arrival(), v.guardianSpawn(), v.gatekeeper(),
                v.vaultCenter(), v.seal(), v.sealFacing(), v.nextId());
    }

    private static SectorView edges(SectorView v, List<int[]> edges) {
        return new SectorView(v.id(), v.state(), v.region(), v.rooms(), edges, v.markers(), v.relicCount(), v.pedestals(), v.arrival(), v.guardianSpawn(), v.gatekeeper(),
                v.vaultCenter(), v.seal(), v.sealFacing(), v.nextId());
    }

    private static SectorView markers(SectorView v, List<SectorView.MarkerView> markers) {
        return new SectorView(v.id(), v.state(), v.region(), v.rooms(), v.edges(), markers, v.relicCount(), v.pedestals(), v.arrival(), v.guardianSpawn(), v.gatekeeper(),
                v.vaultCenter(), v.seal(), v.sealFacing(), v.nextId());
    }

    private static SectorView landmarks(SectorView v, Vec arrival, Vec guardian, Vec gate, Vec vault, Vec seal, Facing sealFacing, List<Vec> pedestals) {
        return new SectorView(v.id(), v.state(), v.region(), v.rooms(), v.edges(), v.markers(), v.relicCount(), pedestals, arrival, guardian, gate, vault, seal, sealFacing, v.nextId());
    }

    private static SectorView withState(SectorView v, String state, int relics, int next) {
        return new SectorView(v.id(), state, v.region(), v.rooms(), v.edges(), v.markers(), relics, v.pedestals(), v.arrival(), v.guardianSpawn(), v.gatekeeper(),
                v.vaultCenter(), v.seal(), v.sealFacing(), next);
    }

    private static Box shifted(Box b, int dx, int dz) { return new Box(b.minX() + dx, b.minY(), b.minZ() + dz, b.maxX() + dx, b.maxY(), b.maxZ() + dz); }

    private static SectorFill.RoomOut first(SectorView v, RoomKind k) {
        for (SectorFill.RoomOut r : v.rooms()) if (r.kind() == k) return r;
        assertTrue(false, "no " + k + " room in the plan");
        return null;
    }

    private static List<SectorFill.RoomOut> replace(SectorView v, SectorFill.RoomOut old, SectorFill.RoomOut now) {
        List<SectorFill.RoomOut> out = new ArrayList<>();
        for (SectorFill.RoomOut r : v.rooms()) out.add(r == old ? now : r);
        return out;
    }

    @Test
    void realPlansPassTheSavedAudit() {
        int checked = 0;
        for (long s = 0; s < 40; s++) {
            Request q = TestPlans.first(CAT, s);
            SectorSolver.Solved sv = SectorSolver.solve(q, 24);
            if (!sv.ok()) continue;
            SectorView v = SectorFill.of(sv.result(), q.region).view(0, "ACTIVE", q.region, q.relicCount, -1);
            assertEquals(List.of(), SavedAudit.audit(v, Set.of(0), true), "seed " + s + (sv.fallback() ? " (fallback)" : ""));
            checked++;
        }
        assertTrue(checked >= 30, "most seeds make a sector, got " + checked);
    }

    @Test
    void theFillCopiesTheLandmarksOfTheBossChain() {
        ensure();
        SectorFill f = SectorFill.of(plan, req.region);
        assertNotNull(f.arrival);
        assertNotNull(f.guardianSpawn);
        assertNotNull(f.gatekeeper);
        assertNotNull(f.vaultCenter);
        assertEquals(req.relicCount, f.pedestals.size());
        // the pedestal block is one cell below its marker
        int found = 0;
        for (SectorFill.MarkerOut m : f.markers) {
            if (m.spec().kind() == MarkerKind.PEDESTAL) {
                assertTrue(f.pedestals.contains(m.cell().add(0, -1, 0)), "pedestal below " + m.cell());
                found++;
            }
        }
        assertEquals(f.pedestals.size(), found);
        assertEquals(plan.expansion.seal(), f.expansion.seal());
        assertEquals(plan.count(RoomKind.CAP) + f.rooms.size(), plan.pieces.size());
    }

    @Test
    void roomIdsAndEdgesOfTheFillMatchTheResult() {
        ensure();
        SectorFill f = SectorFill.of(plan, req.region);
        int[] ids = plan.roomIds();
        int expected = 0;
        for (int id : ids) if (id >= 0) expected++;
        assertEquals(expected, f.rooms.size());
        for (int i = 0; i < f.rooms.size(); i++) assertEquals(i, f.rooms.get(i).id(), "ids run 0.. in placement order");
        assertEquals(plan.edges().size(), f.edges.size());
    }

    @Test
    void aSectorThatIsStillGeneratingOrFailedIsReportedAsThat() {
        ensure();
        assertEquals(List.of("the sector is still being generated"), run(withState(good, "GENERATING", good.relicCount(), -1)));
        assertEquals(List.of("the sector failed to generate"), run(withState(good, "FAILED", good.relicCount(), -1)));
    }

    @Test
    void aSectorWithoutRoomsFails() {
        ensure();
        assertEquals(List.of("the sector has no rooms"), run(rooms(good, List.of())));
    }

    @Test
    void aRoomIdUsedTwiceFails() {
        ensure();
        SectorFill.RoomOut a = good.rooms().get(0), b = good.rooms().get(1);
        List<SectorFill.RoomOut> rs = replace(good, b, new SectorFill.RoomOut(a.id(), b.template(), b.kind(), b.box()));
        assertTrue(has(run(rooms(good, rs)), "room id " + a.id() + " is used twice"));
    }

    @Test
    void aRoomOutsideTheRegionFails() {
        ensure();
        SectorFill.RoomOut arena = first(good, RoomKind.ARENA);
        Box far = shifted(arena.box(), 2000, 0);
        List<String> a = run(rooms(good, replace(good, arena, new SectorFill.RoomOut(arena.id(), arena.template(), arena.kind(), far))));
        assertTrue(has(a, "room " + arena.id() + " (" + arena.template() + ") leaves the region"), a.toString());
    }

    @Test
    void theTunnelIntoTheLandingMayLieOutsideTheRegion() {
        ensure();
        SectorFill.RoomOut landing = first(good, RoomKind.LANDING);
        List<SectorFill.RoomOut> rs = new ArrayList<>(good.rooms());
        rs.add(new SectorFill.RoomOut(9999, "nusmp:mega/link/straight", RoomKind.LINK, shifted(landing.box(), -3000, 0)));
        List<int[]> es = new ArrayList<>(good.edges());
        es.add(new int[] {9999, landing.id()});
        assertEquals(List.of(), run(edges(rooms(good, rs), es)));
    }

    @Test
    void twoRoomsOnTopOfEachOtherFail() {
        ensure();
        SectorFill.RoomOut a = good.rooms().get(0), b = good.rooms().get(1);
        List<String> out = run(rooms(good, replace(good, b, new SectorFill.RoomOut(b.id(), b.template(), b.kind(), a.box()))));
        assertTrue(has(out, "rooms " + a.id() + " and " + b.id() + " overlap"), out.toString());
    }

    @Test
    void aMissingOrDoubledBossChainRoomFails() {
        ensure();
        SectorFill.RoomOut arena = first(good, RoomKind.ARENA);
        List<SectorFill.RoomOut> without = new ArrayList<>(good.rooms());
        without.remove(arena);
        assertTrue(has(run(rooms(good, without)), "expected exactly one arena room but the sector has 0"));

        SectorFill.RoomOut vault = first(good, RoomKind.VAULT);
        List<SectorFill.RoomOut> twice = new ArrayList<>(good.rooms());
        twice.add(new SectorFill.RoomOut(5000, vault.template(), RoomKind.VAULT, shifted(vault.box(), 900, 900)));
        List<String> a = run(rooms(good, twice));
        assertTrue(has(a, "expected exactly one vault room but the sector has 2"), a.toString());
    }

    @Test
    void aSectorWithoutAWayOnwardCorridorFails() {
        ensure();
        List<SectorFill.RoomOut> rs = new ArrayList<>(good.rooms());
        rs.removeIf(r -> r.kind() == RoomKind.ONWARD);
        List<String> a = run(rooms(good, rs));
        assertTrue(has(a, "the sector has no Way Onward corridor"), a.toString());
    }

    @Test
    void missingBossChainMarkersFailOnlyWhenTheRecipeRequiresThem() {
        ensure();
        List<SectorView.MarkerView> ms = new ArrayList<>(good.markers());
        ms.removeIf(m -> m.kind() == MarkerKind.GATEKEEPER || m.kind() == MarkerKind.GUARDIAN_SPAWN);
        SectorView v = markers(good, ms);
        List<String> strict = SavedAudit.audit(v, Set.of(0), true);
        assertTrue(has(strict, "no gatekeeper marker was saved"), strict.toString());
        assertTrue(has(strict, "no guardian_spawn marker was saved"), strict.toString());
        List<String> relaxed = SavedAudit.audit(landmarks(v, v.arrival(), null, null, v.vaultCenter(), v.seal(), v.sealFacing(), v.pedestals()), Set.of(0), false);
        assertEquals(List.of(), relaxed);
    }

    @Test
    void twoGuardianSpawnsFail() {
        ensure();
        List<SectorView.MarkerView> ms = new ArrayList<>(good.markers());
        ms.add(new SectorView.MarkerView(MarkerKind.GUARDIAN_SPAWN, good.guardianSpawn()));
        List<String> a = run(markers(good, ms));
        assertTrue(has(a, "2 guardian_spawn markers were saved"), a.toString());
    }

    @Test
    void landmarksOutsideTheirRoomsFail() {
        ensure();
        Vec far = new Vec(good.region().minX() - 500, 65, good.region().minZ() - 500);
        List<String> a = run(landmarks(good, far, far, far, far, far, good.sealFacing(), List.of(far)));
        assertTrue(has(a, "the arrival cell " + far + " is not inside the Landing"), a.toString());
        assertTrue(has(a, "the guardian spawn " + far + " is not inside the arena"), a.toString());
        assertTrue(has(a, "the gatekeeper cell " + far + " is not at the arena or antechamber"), a.toString());
        assertTrue(has(a, "the vault centre " + far + " is not inside the vault"), a.toString());
        assertTrue(has(a, "the Way Onward seal " + far + " is not inside an onward corridor"), a.toString());
    }

    @Test
    void thePedestalsMustMatchTheRelicCountAndSitInTheVault() {
        ensure();
        List<String> a = run(withState(good, "ACTIVE", good.relicCount() == 3 ? 2 : good.relicCount() + 1, -1));
        assertTrue(has(a, "relics but " + good.pedestals().size() + " pedestals were saved"), a.toString());
        Vec far = new Vec(good.region().minX() - 500, 65, good.region().minZ() - 500);
        List<Vec> peds = new ArrayList<>(good.pedestals());
        peds.set(0, far);
        List<String> b = run(landmarks(good, good.arrival(), good.guardianSpawn(), good.gatekeeper(), good.vaultCenter(), good.seal(), good.sealFacing(), peds));
        assertTrue(has(b, "1 pedestals are not inside the vault"), b.toString());
    }

    @Test
    void aSectorWithoutASealOrFacingFails() {
        ensure();
        List<String> a = run(landmarks(good, good.arrival(), good.guardianSpawn(), good.gatekeeper(), good.vaultCenter(), null, null, good.pedestals()));
        assertTrue(has(a, "the sector has no Way Onward seal"), a.toString());
    }

    @Test
    void roomsThatCannotBeWalkedToFail() {
        ensure();
        List<String> a = run(edges(good, List.of()));
        assertTrue(has(a, "rooms cannot be reached from the Landing"), a.toString());
        List<int[]> bad = new ArrayList<>(good.edges());
        bad.add(new int[] {0, 77777});
        List<String> b = run(edges(good, bad));
        assertTrue(has(b, "a doorway joins rooms 0 and 77777 but one of them does not exist"), b.toString());
    }

    @Test
    void aNextSectorThatDoesNotExistFails() {
        ensure();
        assertTrue(has(SavedAudit.audit(withState(good, "ACTIVE", good.relicCount(), 9), Set.of(0, 1), true), "leads to sector 9 which does not exist"));
        assertEquals(List.of(), SavedAudit.audit(withState(good, "ACTIVE", good.relicCount(), 1), Set.of(0, 1), true));
        assertEquals(List.of(), SavedAudit.audit(withState(good, "ACTIVE", good.relicCount(), 9), null, true));
    }

    @Test
    void sentencesNeverContainASemicolonBecauseTheCommandJoinsThemWithOne() {
        ensure();
        Vec far = new Vec(good.region().minX() - 500, 65, good.region().minZ() - 500);
        List<SectorFill.RoomOut> rs = new ArrayList<>(good.rooms());
        rs.add(rs.get(0));
        List<String> all = new ArrayList<>(run(landmarks(edges(rooms(good, rs), List.of()), far, far, far, far, null, null, List.of(far))));
        all.addAll(run(withState(good, "GENERATING", 1, 4)));
        assertFalse(all.isEmpty());
        for (String s : all) assertFalse(s.contains(";"), s);
    }
}
