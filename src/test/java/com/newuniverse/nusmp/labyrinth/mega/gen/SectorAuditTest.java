package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the audit is the gate every plan passes before anything is placed, so each of its invariants is tested the way a gate should be: a good plan
 * passes, and breaking exactly ONE promise makes exactly that sentence appear. The plans are real assembler output (synthetic catalog) altered through the
 * request (a bigger relic count, a stricter recipe, a smaller region, an exclusion) or built by hand when the break is geometric (two pieces on top of each
 * other, a room nobody can walk to). The fallback layout's relaxed audit is tested for what it skips and for what it still insists on.
 */
class SectorAuditTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    private static Request req;
    private static Result good;

    private static void ensureGood() {
        if (good != null) return;
        for (long s = 0; s < 20 && good == null; s++) {
            Request q = TestPlans.first(CAT, s);
            SectorSolver.Solved sv = SectorSolver.solve(q, 24);
            if (sv.ok() && !sv.fallback()) {
                req = q.withSeed(sv.result().seed, false);
                good = sv.result();
            }
        }
        assertNotNull(good, "a good plan to start from");
    }

    private static boolean has(List<String> audit, String part) {
        for (String a : audit) if (a.contains(part)) return true;
        return false;
    }

    private static Request with(Recipe rc) {
        Request q = req.withSeed(req.seed, false);
        q.recipe = rc;
        return q;
    }

    @Test
    void aGoodPlanPassesBothAudits() {
        ensureGood();
        assertEquals(List.of(), SectorAudit.audit(good, req, true));
        assertEquals(List.of(), SectorAudit.audit(good, req, false));
    }

    @Test
    void aFailedAssemblyIsReportedAsNoLayout() {
        ensureGood();
        Result failed = Result.failed(req.region, "no boss chain: doors=0", List.of(), 1, false, java.util.Map.of());
        List<String> a = SectorAudit.audit(failed, req, true);
        assertEquals(1, a.size());
        assertTrue(a.get(0).startsWith("no layout: no boss chain"));
    }

    @Test
    void theVaultMustOfferTheSectorsRelicCount() {
        ensureGood();
        Request q = req.withSeed(req.seed, false);
        int peds = 0;
        for (Placed p : good.pieces) if (p.shape.kind == RoomKind.VAULT) peds = p.shape.countMarkers(MarkerKind.PEDESTAL);
        q.relicCount = peds == 3 ? 1 : peds + 1;
        List<String> a = SectorAudit.audit(good, q, true);
        assertTrue(has(a, "the vault has " + peds + " pedestal markers but the sector needs " + q.relicCount), a.toString());
    }

    @Test
    void thePacingChecksFailWhenTheRecipeAsksForMore() {
        ensureGood();
        Recipe rc = new Recipe();
        rc.minRooms = 10000;
        assertTrue(has(SectorAudit.audit(good, with(rc), true), "rooms (the recipe wants 10000)"));
        rc = new Recipe();
        rc.minLoops = 10000;
        assertTrue(has(SectorAudit.audit(good, with(rc), true), "loops (the recipe wants 10000)"));
        rc = new Recipe();
        rc.extraSafe = 50;
        assertTrue(has(SectorAudit.audit(good, with(rc), true), "extra safe room"));
        rc = new Recipe();
        rc.setMin(RoomKind.TRIAL, 99);
        assertTrue(has(SectorAudit.audit(good, with(rc), true), "trial room(s) (the recipe wants 99)"));
        rc = new Recipe();
        rc.setMin(RoomKind.TREASURE, 999);
        assertTrue(has(SectorAudit.audit(good, with(rc), true), "treasure room(s)"));
        rc = new Recipe();
        rc.setMax(RoomKind.HALL, 1);
        assertTrue(has(SectorAudit.audit(good, with(rc), true), "more than 1 hall room(s)"));
        assertTrue(has(SectorAudit.audit(good, with(rc), false), "more than 1 hall room(s)"), "a maximum is structural: the relaxed audit keeps it");
    }

    @Test
    void theRelaxedAuditSkipsPacingButKeepsStructure() {
        ensureGood();
        Recipe rc = new Recipe();
        rc.minRooms = 10000;
        rc.minLoops = 10000;
        rc.extraSafe = 50;
        rc.setMin(RoomKind.TRIAL, 99);
        assertEquals(List.of(), SectorAudit.audit(good, with(rc), false));
        Request q = with(rc);
        q.region = new Box(0, 56, 0, 40, 103, 40);
        assertTrue(has(SectorAudit.audit(good, q, false), "leaves the region"));
    }

    @Test
    void piecesMustStayInsideTheRegionAndOutOfOtherTerritory() {
        ensureGood();
        Request q = req.withSeed(req.seed, false);
        q.region = new Box(0, MegaGrid.ORIGIN_Y, -85, 100, MegaGrid.ORIGIN_Y + 47, 85);
        assertTrue(has(SectorAudit.audit(good, q, true), "leaves the region"));
        q = req.withSeed(req.seed, false);
        Placed victim = good.pieces.get(good.landing + 3);
        q.exclusions = List.of(new Box(victim.box.minX(), victim.box.minY(), victim.box.minZ(), victim.box.minX() + 2, victim.box.maxY(), victim.box.minZ() + 2));
        assertTrue(has(SectorAudit.audit(good, q, true), "inside another sector's territory"));
    }

    private static Shape cell() { return CAT.shape("nusmp:mega/hall/cell_1x1"); }

    private static Result handmade(List<Placed> ps) {
        return new Result(new Box(0, 56, 0, 170, 103, 170), ps, List.of(), null, null, List.of(), 1, false, 0, java.util.Map.of());
    }

    @Test
    void overlappingPiecesAreReported() {
        ensureGood();
        Placed a = new Placed(0, CAT.shape("nusmp:mega/landing/foyer_a"), "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        Placed b = new Placed(1, cell(), "p", 0, new Vec(9, 56, 9), 1, 0, 0, 0, false);
        List<String> audit = SectorAudit.audit(handmade(List.of(a, b)), req, false);
        assertTrue(has(audit, "overlap"), audit.toString());
    }

    @Test
    void aRoomNobodyCanWalkToIsReported() {
        ensureGood();
        Placed a = new Placed(0, CAT.shape("nusmp:mega/landing/foyer_a"), "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        Placed island = new Placed(1, cell(), "p", 0, new Vec(90, 56, 90), 1, -1, -1, -1, false);
        List<String> audit = SectorAudit.audit(handmade(List.of(a, island)), req, false);
        assertTrue(has(audit, "1 room(s) cannot be reached from the Landing"), audit.toString());
    }

    @Test
    void theBossChainMustExistOnce() {
        ensureGood();
        Placed a = new Placed(0, CAT.shape("nusmp:mega/landing/foyer_a"), "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        List<String> audit = SectorAudit.audit(handmade(List.of(a)), req, false);
        assertTrue(has(audit, "expected exactly one arena, found 0"), audit.toString());
        Shape arena = CAT.shape("nusmp:mega/arena/pit_5x5");
        Placed a1 = new Placed(1, arena, "p", 0, new Vec(40, 56, 0), 1, 0, 3, 0, false);
        Placed a2 = new Placed(2, arena, "p", 0, new Vec(100, 56, 0), 1, 0, 3, 0, false);
        audit = SectorAudit.audit(handmade(List.of(a, a1, a2)), req, false);
        assertTrue(has(audit, "expected exactly one arena, found 2"), audit.toString());
        Result oneArena = handmade(List.of(a, a1));
        audit = SectorAudit.audit(oneArena, req, false);
        assertTrue(has(audit, "the arena does not lead to an antechamber"), audit.toString());
    }

    @Test
    void landmarkMarkersAreDemandedUnlessTheRecipeSaysOtherwise() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        Shape arena = c.shape("nusmp:mega/arena/pit_5x5");
        List<ShapeMarker> noSpawn = new ArrayList<>();
        for (ShapeMarker m : arena.markers) if (m.spec().kind() != MarkerKind.GUARDIAN_SPAWN) noSpawn.add(m);
        c.add(new Shape(arena.id, arena.sx, arena.sy, arena.sz, arena.kind, arena.sockets, noSpawn, 0));
        Request q = TestPlans.first(c, 3);
        Result r = MegaAssembler.assemble(q);
        assertTrue(r.assembled(), r.failure);
        assertTrue(has(SectorAudit.audit(r, q, true), "the arena needs exactly one GUARDIAN_SPAWN marker"));
        q.recipe = new Recipe();
        q.recipe.requireMarkers = false;
        assertFalse(has(SectorAudit.audit(r, q, true), "GUARDIAN_SPAWN"), "the demand can be switched off");
    }

    @Test
    void theStripInFrontOfTheSealMustStayClear() {
        ensureGood();
        Result.Expansion e = good.expansion;
        assertNotNull(e);
        Placed other = null;
        for (Placed p : good.pieces) if (p.index != e.piece() && !p.shape.isCap() && !p.link) { other = p; break; }
        Result.Expansion blockedStrip = new Result.Expansion(e.piece(), e.seal(), e.facing(), e.w(), e.h(), e.d(), other.box, e.cost());
        Result tampered = new Result(good.region, good.pieces, good.loops, blockedStrip, null, good.warnings, good.seed, good.fallback, good.landing, good.stats);
        List<String> audit = SectorAudit.audit(tampered, req, true);
        assertTrue(has(audit, "the strip in front of the seal is blocked by"), audit.toString());
        Result noSeal = new Result(good.region, good.pieces, good.loops, null, null, good.warnings, good.seed, good.fallback, good.landing, good.stats);
        assertTrue(has(SectorAudit.audit(noSeal, req, true), "the Way Onward has no readable seal"));
    }

    @Test
    void aDoorwayThatOpensOntoTheVoidFailsTheFullAuditOnly() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        c.pools().remove(MegaGrid.P_CAP);                         // no door caps exist
        Request q = TestPlans.first(c, 3);
        Result r = MegaAssembler.assemble(q);
        assertTrue(r.assembled(), r.failure);
        assertTrue(has(SectorAudit.audit(r, q, true), "doorway(s) open onto the void"));
        assertFalse(has(SectorAudit.audit(r, q, false), "open onto the void"), "the fallback layout may have them");
    }

    @Test
    void sentencesNeverContainASemicolonBecauseTheCommandJoinsThemWithOne() {
        ensureGood();
        Recipe rc = new Recipe();
        rc.minRooms = 10000;
        rc.minLoops = 10000;
        rc.extraSafe = 50;
        Request q = with(rc);
        q.relicCount = 3;
        q.region = new Box(0, 56, 0, 60, 103, 60);
        for (String s : SectorAudit.audit(good, q, true)) assertFalse(s.contains(";"), s);
    }
}
