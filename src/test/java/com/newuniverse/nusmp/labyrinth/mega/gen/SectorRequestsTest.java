package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@link SectorRequests} is the one place that turns "sector n behind this seal" into the assembler's request, used by the game and by the simulator
 * alike, so what it builds is checked directly: sector 0 starts at the fixed cell with no tunnel, a later sector starts with a tunnel whose last segment
 * the Landing hangs from, the vault pool follows the relic count, the neighbours become exclusions grown by the reach of a door cap, and a Landing door that
 * is not behind the old seal (a corrupt saved sector) is refused with a message instead of being built somewhere wrong.
 */
class SectorRequestsTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    @Test
    void sectorZeroStartsAtTheFixedCellWithNoTunnel() {
        SectorPlanner.Plan p = SectorPlanner.first(5);
        SectorRequests.Prepared prep = SectorRequests.prepare(new SectorRequests.Inputs(CAT, new Recipe(), 450, 40, p.seed(), p.entry(), p.forward(), p.region(),
                null, null, null, List.of(), 2));
        assertTrue(prep.ok());
        Request q = prep.request();
        assertEquals(p.entry(), q.start.frontCell());
        assertEquals(Facing.EAST, q.start.growth());
        assertEquals(MegaGrid.P_LANDING, q.start.pool());
        assertEquals(MegaGrid.DOOR, q.start.socketName());
        assertEquals(-1, q.start.parent());
        assertTrue(q.pre.isEmpty());
        assertNull(prep.tunnelObstacle());
        assertEquals("nusmp:mega/vault_2", q.forced.get(MegaGrid.VAULT), "the vault pool follows the relic count");
        assertEquals(2, q.relicCount);
        assertTrue(q.exclusions.isEmpty());
        assertEquals(p.region(), q.region);
        assertEquals(3, q.onward.variants().size());
    }

    @Test
    void aLaterSectorStartsWithATunnelAndTheLandingHangsFromItsLastSegment() {
        SimWorld w = new SimWorld(CAT, 11, new Recipe(), 450, 40, 24);
        assertTrue(w.first());
        Result.Expansion ex = w.sectors.get(0).result().expansion;
        SectorPlanner.Next n = SectorPlanner.planNext(ex.seal(), ex.facing(), w.sectors.get(0).plan().region(), List.of(), 40);
        assertNotNull(n);
        SectorRequests.Inputs in = new SectorRequests.Inputs(CAT, new Recipe(), 450, 40, 99, n.entry(), n.forward(), n.region(), ex.seal(), ex.facing(),
                w.sectors.get(0).plan().region(), List.of(SectorPlanner.Obstacle.region(w.sectors.get(0).plan().region())), 1);
        SectorRequests.Prepared prep = SectorRequests.prepare(in);
        assertTrue(prep.ok(), prep.failure());
        Request q = prep.request();
        assertEquals(n.linkModules(), q.pre.size());
        assertEquals(q.pre.size() - 1, q.start.parent());
        assertEquals(prep.tunnel().lastOutSocket(), q.start.parentSocket());
        assertEquals(n.entry(), q.start.frontCell());
        assertEquals(ex.facing(), q.start.growth());
        assertEquals(SectorPlanner.tunnelBox(ex.seal(), ex.facing(), n.linkModules()), prep.tunnelObstacle().box());
        assertEquals(MegaGrid.CAP_MARGIN, prep.tunnelObstacle().clearance());
        assertFalse(prep.tunnelObstacle().region());
        assertTrue(q.exclusions.isEmpty() || q.exclusions.stream().noneMatch(b -> b.overlaps(n.region())), "the neighbour at the planned gap is not inside the new region");
        Result r = MegaAssembler.assemble(q);
        assertTrue(r.assembled(), r.failure);
        assertEquals(List.of(), TestPlans.brute(r, q));
    }

    @Test
    void aLandingDoorThatIsNotBehindTheSealIsRefused() {
        Box prev = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        Vec seal = new Vec(170, 65, 10);
        Vec wrong = new Vec(171, 65, 10);                           // zero segments away
        SectorRequests.Prepared p = SectorRequests.prepare(new SectorRequests.Inputs(CAT, new Recipe(), 450, 40, 1, wrong, Facing.EAST, SectorPlanner.regionFor(wrong, Facing.EAST),
                seal, Facing.EAST, prev, List.of(), 1));
        assertFalse(p.ok());
        assertTrue(p.failure().contains("not behind the old seal"), p.failure());
        Vec off = new Vec(171 + 18, 65, 12);                        // two segments away but two cells off the line
        p = SectorRequests.prepare(new SectorRequests.Inputs(CAT, new Recipe(), 450, 40, 1, off, Facing.EAST, SectorPlanner.regionFor(off, Facing.EAST),
                seal, Facing.EAST, prev, List.of(), 1));
        assertFalse(p.ok());
        assertTrue(p.failure().contains("tunnel ends at"), p.failure());
    }

    @Test
    void aMissingLinkPoolIsReportedAsATunnelFailure() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        c.pools().remove(MegaGrid.P_LINK);
        Box prev = SectorPlanner.regionFor(new Vec(0, 65, 0), Facing.EAST);
        Vec seal = new Vec(170, 65, 10);
        SectorPlanner.Next n = SectorPlanner.planNext(seal, Facing.EAST, prev, List.of(), 40);
        SectorRequests.Prepared p = SectorRequests.prepare(new SectorRequests.Inputs(c, new Recipe(), 450, 40, 1, n.entry(), n.forward(), n.region(), seal, Facing.EAST, prev, List.of(), 1));
        assertFalse(p.ok());
        assertTrue(p.failure().startsWith("tunnel:"), p.failure());
        assertEquals(RegionTheme.GRAVITY, RegionTheme.pick(1, 0, null));
    }
}
