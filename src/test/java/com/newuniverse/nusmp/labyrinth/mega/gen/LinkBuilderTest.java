package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the tunnel between two sectors is placed without any search, so if its geometry is wrong nothing will catch it until a player walks it: a segment one
 * block off its neighbour is a wall across the corridor. The tests build runs of 1 to 20 segments in all four directions and check every join door to door
 * (each segment starts on the cell in front of the previous one's far door, faces back along the run, advances exactly one module), that the run ends on
 * the cell where the Landing's door goes, that every eighth segment is a waystation, that a veto on a waystation falls back to a plain segment, and that
 * missing or unusable templates give a clear failure instead of a half-built run.
 */
class LinkBuilderTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    @Test
    void runsOfEveryLengthJoinDoorToDoorInEveryDirection() {
        for (Facing f : Facing.values()) {
            for (int m = 1; m <= 20; m++) {
                Vec seal = new Vec(-53, 65, 118);
                LinkBuilder.Tunnel t = LinkBuilder.build(CAT, seal, f, m, 99, box -> true);
                assertTrue(t.ok(), t.failure());
                assertEquals(m, t.pieces().size());
                Vec expectFront = seal.step(f).step(f, 9 * m);
                assertEquals(expectFront, t.landingFront(), "the run ends where the Landing's door goes, " + f + " x" + m);
                for (int i = 0; i < m; i++) {
                    Placed p = t.pieces().get(i);
                    assertEquals(i, p.index);
                    assertTrue(p.link);
                    assertEquals(-1, p.depth);
                    assertEquals(RoomKind.LINK, p.shape.kind);
                    assertEquals(i - 1, p.parent);
                    Vec inFront = seal.step(f).step(f, 9 * i);
                    assertEquals(inFront, p.socketCell(p.ownSocket), "segment " + i + " starts on the cell in front of the previous one");
                    assertEquals(f.opposite(), p.socketFacing(p.ownSocket), "and its near door looks back along the run");
                    int out = i == m - 1 ? t.lastOutSocket() : t.pieces().get(i + 1).parentSocket;
                    assertEquals(f, p.socketFacing(out), "its far door looks forward");
                    assertEquals(inFront.step(f, 8), p.socketCell(out), "one module (8 cells between the two doors)");
                    assertEquals(inFront.along(f.cw()), p.socketCell(out).along(f.cw()), "on the same line");
                    assertTrue(p.isUsed(p.ownSocket));
                    if (i < m - 1) assertTrue(p.isUsed(out), "the far door of a middle segment leads to the next");
                    else assertFalse(p.isUsed(out), "the last far door waits for the Landing");
                    boolean station = p.shape.id.contains("waystation");
                    assertEquals((i + 1) % LinkBuilder.STATION_EVERY == 0, station, "segment " + i + (station ? " is a waystation" : " is plain"));
                }
            }
        }
    }

    @Test
    void theSegmentsDoNotOverlapEachOther() {
        LinkBuilder.Tunnel t = LinkBuilder.build(CAT, new Vec(0, 65, 0), Facing.SOUTH, 20, 5, box -> true);
        BoxIndex idx = new BoxIndex();
        for (Placed p : t.pieces()) {
            assertFalse(idx.overlapsAny(p.box), p + " overlaps an earlier segment");
            idx.add(p.box);
        }
    }

    @Test
    void aVetoedWaystationBecomesAPlainSegment() {
        LinkBuilder.Tunnel t = LinkBuilder.build(CAT, new Vec(0, 65, 0), Facing.EAST, 16, 5, box -> box.sizeZ() <= 9 && box.sizeX() <= 9);
        assertTrue(t.ok(), t.failure());
        for (Placed p : t.pieces()) assertFalse(p.shape.id.contains("waystation"), "no waystation fits, so none is used: " + p);
        assertEquals(new Vec(0, 65, 0).step(Facing.EAST).step(Facing.EAST, 144), t.landingFront());
    }

    @Test
    void theRunIsTheSameEveryTimeForTheSameSeed() {
        LinkBuilder.Tunnel a = LinkBuilder.build(CAT, new Vec(7, 65, 7), Facing.NORTH, 17, 31, box -> true);
        LinkBuilder.Tunnel b = LinkBuilder.build(CAT, new Vec(7, 65, 7), Facing.NORTH, 17, 31, box -> true);
        for (int i = 0; i < 17; i++) {
            assertEquals(a.pieces().get(i).shape.id, b.pieces().get(i).shape.id);
            assertEquals(a.pieces().get(i).origin, b.pieces().get(i).origin);
            assertEquals(a.pieces().get(i).rot, b.pieces().get(i).rot);
        }
    }

    @Test
    void zeroSegmentsIsAnEmptyRunThatEndsInFrontOfTheSeal() {
        LinkBuilder.Tunnel t = LinkBuilder.build(CAT, new Vec(5, 65, 5), Facing.WEST, 0, 1, box -> true);
        assertTrue(t.ok());
        assertTrue(t.pieces().isEmpty());
        assertEquals(new Vec(4, 65, 5), t.landingFront());
    }

    @Test
    void aMissingPoolOrUnusableTemplatesGiveAClearFailure() {
        SyntheticCatalog none = SyntheticCatalog.standard();
        none.pools().remove(MegaGrid.P_LINK);
        LinkBuilder.Tunnel t = LinkBuilder.build(none, new Vec(0, 65, 0), Facing.EAST, 3, 1, box -> true);
        assertFalse(t.ok());
        assertTrue(t.failure().contains(MegaGrid.P_LINK), t.failure());
        assertTrue(t.pieces().isEmpty());

        SyntheticCatalog bad = SyntheticCatalog.standard();
        bad.pool(MegaGrid.P_LINK, 0, MegaGrid.P_CAP, "nusmp:mega/trial/archway_3x3", 1, "nusmp:mega/corridor/straight_1x3", 1);   // opposite doors, but three modules apart
        t = LinkBuilder.build(bad, new Vec(0, 65, 0), Facing.EAST, 3, 1, box -> true);
        assertFalse(t.ok());
        assertTrue(t.failure().contains("two opposite doors"), t.failure());
    }

    @Test
    void onlyWaystationsStillMakeARun() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        c.pool(MegaGrid.P_LINK, 0, MegaGrid.P_CAP, "nusmp:mega/link/waystation_a", 1);
        LinkBuilder.Tunnel t = LinkBuilder.build(c, new Vec(0, 65, 0), Facing.SOUTH, 5, 1, box -> true);
        assertTrue(t.ok(), t.failure());
        List<String> ids = new ArrayList<>();
        for (Placed p : t.pieces()) ids.add(p.shape.id);
        assertEquals(5, ids.size());
        for (String id : ids) assertTrue(id.contains("waystation"));
    }
}
