package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MarkerSpec;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the saved sector is what every other builder's system trusts, and {@link SectorFill} decides what goes into it. These tests build tiny plans BY HAND
 * (a Landing, an arena, a door cap, with markers at chosen cells and pieces turned by chosen quarter turns) so the expected world cells can be worked out on
 * paper: where the arrival cell lands for a turned Landing, which of two guardian markers wins, that pedestals are one cell below their markers, that door
 * caps are never rooms but do keep their markers, and what the arrival falls back to when a Landing forgot its marker.
 */
class SectorFillTest {
    private static ShapeMarker marker(String metadata, int x, int y, int z) { return new ShapeMarker(MarkerSpec.parse(metadata), new Vec(x, y, z)); }

    private static Shape shape(String id, int sx, int sz, RoomKind kind, List<ShapeMarker> markers) { return new Shape(id, sx, MegaGrid.HEIGHT, sz, kind, List.of(), markers, 0); }

    private static Result result(Box region, List<Placed> pieces, Result.Expansion expansion) {
        return new Result(region, pieces, List.of(), expansion, null, List.of(), 7L, false, pieces.isEmpty() ? -1 : 0, Map.of());
    }

    private static final Box REGION = new Box(0, 56, 0, 170, 103, 170);

    @Test
    void theArrivalCellIsTheLandingsMarkerTurnedWithThePiece() {
        Shape landing = shape("nusmp:mega/landing/hall", 9, 18, RoomKind.LANDING, List.of(marker("nusmp:arrival", 2, 9, 3)));
        for (int rot = 0; rot < 4; rot++) {
            Placed p = new Placed(0, landing, "p", rot, new Vec(100, 56, 200), 0, -1, -1, -1, false);
            SectorFill f = SectorFill.of(result(REGION, List.of(p), null), REGION);
            Vec expected = new Vec(100, 56, 200).add(Rot.cell(rot, new Vec(2, 9, 3)));
            assertEquals(expected, f.arrival, "rotation " + rot);
        }
    }

    @Test
    void aLandingWithoutAnArrivalMarkerFallsBackToTheMiddleOneAboveTheFloor() {
        Shape landing = shape("nusmp:mega/landing/hall", 9, 9, RoomKind.LANDING, List.of());
        Placed p = new Placed(0, landing, "p", 0, new Vec(90, 56, 90), 0, -1, -1, -1, false);
        SectorFill f = SectorFill.of(result(REGION, List.of(p), null), REGION);
        assertEquals(new Vec(94, MegaGrid.FLOOR_Y + 1, 94), f.arrival);
    }

    @Test
    void theFirstMarkerOfAKindWinsInPieceOrderThenMarkerOrder() {
        Shape landing = shape("nusmp:mega/landing/hall", 9, 9, RoomKind.LANDING, List.of(marker("nusmp:arrival", 4, 9, 4)));
        Shape arena = shape("nusmp:mega/arena/pit", 18, 18, RoomKind.ARENA,
                List.of(marker("nusmp:guardian_spawn", 5, 9, 5), marker("nusmp:guardian_spawn", 6, 9, 6), marker("nusmp:gatekeeper", 1, 10, 8)));
        Shape vault = shape("nusmp:mega/vault/one", 9, 9, RoomKind.VAULT, List.of(marker("nusmp:vault_center", 4, 9, 4), marker("nusmp:guardian_spawn", 2, 9, 2)));
        Placed a = new Placed(0, landing, "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        Placed b = new Placed(1, arena, "p", 0, new Vec(20, 56, 0), 1, 0, 0, 0, false);
        Placed c = new Placed(2, vault, "p", 0, new Vec(50, 56, 0), 2, 1, 0, 0, false);
        SectorFill f = SectorFill.of(result(REGION, List.of(a, b, c), null), REGION);
        assertEquals(new Vec(25, 9 + 56, 5), f.guardianSpawn, "the arena's first marker, not its second and not the vault's");
        assertEquals(new Vec(21, 10 + 56, 8), f.gatekeeper);
        assertEquals(new Vec(54, 9 + 56, 4), f.vaultCenter);
        assertEquals(6, f.markers.size(), "every marker of every piece is kept, in piece order");
    }

    @Test
    void pedestalsAreOneCellBelowTheirMarkersAndKeepTheirOrder() {
        Shape vault = shape("nusmp:mega/vault/three", 9, 9, RoomKind.VAULT,
                List.of(marker("nusmp:pedestal;slot=0", 2, 10, 4), marker("nusmp:pedestal;slot=1", 4, 10, 4), marker("nusmp:pedestal;slot=2", 6, 10, 4)));
        Placed p = new Placed(0, vault, "p", 2, new Vec(30, 56, 30), 0, -1, -1, -1, false);
        SectorFill f = SectorFill.of(result(REGION, List.of(p), null), REGION);
        assertEquals(3, f.pedestals.size());
        for (int i = 0; i < 3; i++) {
            Vec m = new Vec(30, 56, 30).add(Rot.cell(2, new Vec(2 + 2 * i, 10, 4)));
            assertEquals(m.add(0, -1, 0), f.pedestals.get(i), "pedestal " + i);
        }
    }

    @Test
    void doorCapsAreNeverRoomsButTheirMarkersAreKept() {
        Shape landing = shape("nusmp:mega/landing/hall", 9, 9, RoomKind.LANDING, List.of(marker("nusmp:arrival", 4, 9, 4)));
        Shape cap = new Shape("nusmp:mega/cap/niche", 3, 6, 4, RoomKind.CAP, List.of(), List.of(marker("nusmp:furniture;as=table", 1, 1, 2)), 0);
        Placed a = new Placed(0, landing, "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        Placed b = new Placed(1, cap, "p", 0, new Vec(20, 56, 0), 1, 0, 0, 0, false);
        Placed c = new Placed(2, landing, "p", 0, new Vec(60, 56, 0), 1, 0, 0, 0, false);
        SectorFill f = SectorFill.of(result(REGION, List.of(a, b, c), null), REGION);
        assertEquals(2, f.rooms.size(), "two rooms and one cap");
        assertEquals(0, f.rooms.get(0).id());
        assertEquals(1, f.rooms.get(1).id(), "the room after a cap has the next id, not the piece index");
        assertEquals(new Vec(60, 56, 0), new Vec(f.rooms.get(1).box().minX(), f.rooms.get(1).box().minY(), f.rooms.get(1).box().minZ()));
        boolean furniture = false;
        for (SectorFill.MarkerOut m : f.markers) furniture |= m.spec().kind() == MarkerKind.FURNITURE;
        assertTrue(furniture, "the cap's marker is kept");
    }

    @Test
    void theBoundsAreTheUnionOfAllPiecesAndTheRegionForAnEmptyPlan() {
        Shape landing = shape("nusmp:mega/landing/hall", 9, 9, RoomKind.LANDING, List.of());
        Placed a = new Placed(0, landing, "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        Placed b = new Placed(1, landing, "p", 0, new Vec(50, 56, 20), 1, 0, 0, 0, false);
        SectorFill f = SectorFill.of(result(REGION, List.of(a, b), null), REGION);
        assertEquals(new Box(0, 56, 0, 58, 103, 28), f.bounds);
        SectorFill empty = SectorFill.of(result(REGION, List.of(), null), REGION);
        assertEquals(REGION, empty.bounds);
        assertNull(empty.arrival);
    }

    @Test
    void theViewCarriesTheSealTheNextIdAndTheState() {
        Shape landing = shape("nusmp:mega/landing/hall", 9, 9, RoomKind.LANDING, List.of(marker("nusmp:arrival", 4, 9, 4)));
        Placed a = new Placed(0, landing, "p", 0, new Vec(0, 56, 0), 0, -1, -1, -1, false);
        Result.Expansion e = new Result.Expansion(0, new Vec(4, 65, 0), Facing.NORTH, 3, 4, 2, new Box(0, 56, -40, 8, 103, -1), 1);
        SectorFill f = SectorFill.of(result(REGION, List.of(a), e), REGION);
        SectorView v = f.view(5, "UNSEALED", REGION, 2, 6);
        assertEquals(5, v.id());
        assertEquals("UNSEALED", v.state());
        assertEquals(2, v.relicCount());
        assertEquals(6, v.nextId());
        assertEquals(new Vec(4, 65, 0), v.seal());
        assertEquals(Facing.NORTH, v.sealFacing());
        assertEquals(1, v.markers().size());
        assertEquals(MarkerKind.ARRIVAL, v.markers().get(0).kind());
    }
}
