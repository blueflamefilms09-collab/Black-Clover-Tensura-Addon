package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the dungeon is infinite only if sector after sector keeps fitting. This grows whole simulated dungeons through the same planner, tunnel builder and
 * assembler the game uses and checks the invariants that hold ACROSS sectors, which no single-sector test can see: no piece of any sector (rooms, door caps,
 * tunnel segments) overlaps a piece of any other, every region keeps the gap from every other, every tunnel starts exactly on the cell in front of the old
 * seal, and the dungeon keeps growing for many sectors (a dead end is allowed but must be rare, and never costs a sector).
 */
class WorldGrowthTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    @Test
    void noPieceOfAnySectorTouchesAPieceOfAnother() {
        int grown = 0;
        for (long seed = 0; seed < 6; seed++) {
            SimWorld w = new SimWorld(CAT, 1000 + seed, new Recipe(), 450, 40, 24);
            boolean all = w.grow(9);
            if (all) grown++;
            else assertTrue(w.stopped.startsWith("dead end"), "a world may only stop at a dead end: " + w.stopped);
            List<Placed> world = new ArrayList<>();
            List<Integer> owner = new ArrayList<>();
            for (SimWorld.Entry e : w.sectors) {
                assertTrue(e.solved().ok(), "sector " + e.id() + " was built");
                for (Placed p : e.result().pieces) {
                    for (int i = 0; i < world.size(); i++) {
                        Placed q = world.get(i);
                        if (!q.box.overlaps(p.box)) continue;
                        // the one allowed meeting: a door cap of the old sector that enters only the two outer columns of the keep-clear strip, where the
                        // 2-thick wall of the tunnel overwrites it completely
                        boolean allowed = p.link && owner.get(i) == e.id() - 1 && q.shape.isCap() && !q.box.overlaps(MegaAssembler.inner(p.box, e.plan().forward()));
                        assertTrue(allowed, "seed " + seed + " sector " + e.id() + ": " + p + " overlaps " + q + " of sector " + owner.get(i));
                    }
                    world.add(p);
                    owner.add(e.id());
                }
            }
            assertTrue(world.size() > 100);
        }
        assertTrue(grown >= 5, "almost every world grows nine sectors: " + grown + " of 6");
    }

    @Test
    void regionsKeepTheGapAndTunnelsStartOnTheOldSeal() {
        for (long seed = 0; seed < 5; seed++) {
            SimWorld w = new SimWorld(CAT, 2000 + seed, new Recipe(), 450, 40, 24);
            w.grow(8);
            List<SimWorld.Entry> s = w.sectors;
            for (int i = 0; i < s.size(); i++) {
                for (int j = i + 1; j < s.size(); j++) {
                    Box a = s.get(i).plan().region(), b = s.get(j).plan().region();
                    int dx = Math.max(0, Math.max(a.minX() - b.maxX(), b.minX() - a.maxX()) - 1), dz = Math.max(0, Math.max(a.minZ() - b.maxZ(), b.minZ() - a.maxZ()) - 1);
                    assertTrue(Math.max(dx, dz) >= MegaGrid.REGION_GAP, "sectors " + i + " and " + j + " are " + Math.max(dx, dz) + " apart");
                }
            }
            for (int j = 1; j < s.size(); j++) {
                Result.Expansion prev = s.get(j - 1).result().expansion;
                Result r = s.get(j).result();
                Placed first = r.pieces.get(0);
                assertTrue(first.link);
                assertEquals(prev.seal().step(prev.facing()), first.socketCell(first.ownSocket), "tunnel " + j + " starts in front of the seal of sector " + (j - 1));
                assertEquals(prev.facing(), s.get(j).plan().forward(), "and the new sector grows the way the seal points");
                Placed land = r.landingPiece();
                assertEquals(s.get(j).plan().entry(), land.socketCell(land.ownSocket));
            }
        }
    }

    @Test
    void theDungeonKeepsGrowingForManySectors() {
        int full = 0, worlds = 10;
        for (long seed = 0; seed < worlds; seed++) {
            SimWorld w = new SimWorld(CAT, 3000 + seed, new Recipe(), 450, 40, 24);
            if (w.grow(20)) full++;
            else assertTrue(w.stopped.startsWith("dead end"), w.stopped);
            assertTrue(w.sectors.size() >= 6, "even an unlucky world gets past six sectors: " + w.sectors.size());
        }
        assertTrue(full >= 8, "most worlds grow twenty sectors: " + full + " of " + worlds);
    }

    @Test
    void everySectorOfAGrownWorldPassesTheFullAudit() {
        SimWorld w = new SimWorld(CAT, 4242, new Recipe(), 450, 40, 24);
        w.grow(7);
        for (SimWorld.Entry e : w.sectors) {
            Request q = e.prepared().request().withSeed(e.result().seed, e.solved().fallback());
            assertEquals(List.of(), SectorAudit.audit(e.result(), q, !e.solved().fallback()), "sector " + e.id());
        }
    }
}
