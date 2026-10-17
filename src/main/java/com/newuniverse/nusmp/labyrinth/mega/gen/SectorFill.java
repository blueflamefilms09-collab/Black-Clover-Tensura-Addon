package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MarkerSpec;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a finished plan has to become the saved {@code Sector}: its rooms, its doorways, its markers and the handful of landmarks of the boss chain that the
 * other systems read (where the player arrives, where the Guardian appears, where the Gatekeeper stands, the centre of the vault, the pedestals, the rubble
 * seal and the way it points). Deciding WHICH marker is "the" Guardian spawn, or what the arrival cell is when a Landing forgot its marker, is the part
 * of that job that can go wrong silently, and the saved sector is what every other builder's system trusts. So that decision is made here, on plain
 * numbers, where a test can feed it every plan the simulator makes and look at the answer, and {@code SectorGenerator.fill} only copies the answer into the
 * game's classes. The same object also becomes the {@link SectorView} that {@code SavedAudit} checks, so what the audit looks at is exactly what is saved.
 * Since 1.58.0 it also answers where the stairs of a floor are: the exit of the arrival stairwell, the exit of the forward stairwell and the cell where a party
 * stands when it comes back down (or up) to this floor.
 */
public final class SectorFill {
    /** A room of the sector (a placed piece that is not a door cap): its id is its position among the rooms, as the saved {@code Room} has it. */
    public record RoomOut(int id, String template, RoomKind kind, Box box) {}

    /** A template marker moved into the world: the parsed spec, the world cell and the quarter turns of the piece it came from (the game turns the {@code facing} argument). */
    public record MarkerOut(MarkerSpec spec, Vec cell, int rot) {}

    public final List<RoomOut> rooms;
    public final List<int[]> edges;
    public final List<MarkerOut> markers;
    /** Where a player stands on arrival (the Landing's ARRIVAL marker; else the middle of the Landing one cell above the floor). */
    public final Vec arrival;
    /** The first GUARDIAN_SPAWN / GATEKEEPER / VAULT_CENTER marker cell of the plan, or null. */
    public final Vec guardianSpawn, gatekeeper, vaultCenter;
    /** The pedestal blocks: one cell below every PEDESTAL marker. */
    public final List<Vec> pedestals;
    /**
     * 1.58.0, the floor model: the STAIR_EXIT cell of the arrival stairwell (it leads back), the STAIR_EXIT cell of the forward stairwell (it leads on) and the
     * ARRIVAL cell of the forward stairwell (where a party stands when it comes back from the next floor). Null for the Landing of the old model.
     */
    public final Vec stairBack, stairForward, stairForwardPad;
    /** The end of the Way Onward, or null. */
    public final Result.Expansion expansion;
    /** The union of the pieces (door caps included); the region when the plan is empty. */
    public final Box bounds;

    private SectorFill(List<RoomOut> rooms, List<int[]> edges, List<MarkerOut> markers, Vec arrival, Vec guardianSpawn, Vec gatekeeper, Vec vaultCenter,
                       List<Vec> pedestals, Result.Expansion expansion, Box bounds, Vec stairBack, Vec stairForward, Vec stairForwardPad) {
        this.stairBack = stairBack;
        this.stairForward = stairForward;
        this.stairForwardPad = stairForwardPad;
        this.rooms = List.copyOf(rooms);
        this.edges = List.copyOf(edges);
        this.markers = List.copyOf(markers);
        this.arrival = arrival;
        this.guardianSpawn = guardianSpawn;
        this.gatekeeper = gatekeeper;
        this.vaultCenter = vaultCenter;
        this.pedestals = List.copyOf(pedestals);
        this.expansion = expansion;
        this.bounds = bounds;
    }

    /** The content of the saved sector for an assembled plan. {@code region} is only the fallback for the bounds of a plan with no pieces. */
    public static SectorFill of(Result r, Box region) {
        int[] ids = r.roomIds();
        List<RoomOut> rooms = new ArrayList<>();
        List<MarkerOut> markers = new ArrayList<>();
        Vec guardian = null, gate = null, vault = null;
        List<Vec> pedestals = new ArrayList<>();
        for (Placed p : r.pieces) {
            if (ids[p.index] >= 0) rooms.add(new RoomOut(ids[p.index], p.shape.id, p.shape.kind, p.box));
            for (ShapeMarker m : p.shape.markers) {
                Vec w = p.world(m.cell());
                markers.add(new MarkerOut(m.spec(), w, p.rot));
                switch (m.spec().kind()) {
                    case GUARDIAN_SPAWN -> { if (guardian == null) guardian = w; }
                    case GATEKEEPER -> { if (gate == null) gate = w; }
                    case VAULT_CENTER -> { if (vault == null) vault = w; }
                    case PEDESTAL -> pedestals.add(w.add(0, -1, 0));
                    default -> { }
                }
            }
        }
        Vec arrival = null;
        Placed landing = r.landingPiece();
        if (landing != null) {
            for (ShapeMarker m : landing.shape.markers) {
                if (m.spec().kind() == MarkerKind.ARRIVAL) {
                    arrival = landing.world(m.cell());
                    break;
                }
            }
            if (arrival == null) arrival = new Vec((int) landing.box.centerX(), MegaGrid.FLOOR_Y + 1, (int) landing.box.centerZ());
        }
        // the stairs of the floor model: the first room is the arrival stairwell (its exit leads back); the other stairwell is the forward one
        Vec back = null, forward = null, forwardPad = null;
        for (Placed p : r.pieces) {
            if (p.shape.kind != RoomKind.STAIRWELL) continue;
            boolean start = p.index == r.landing;
            for (ShapeMarker m : p.shape.markers) {
                Vec w = p.world(m.cell());
                if (m.spec().kind() == MarkerKind.STAIR_EXIT) {
                    if (start && back == null) back = w;
                    else if (!start && forward == null) forward = w;
                } else if (m.spec().kind() == MarkerKind.ARRIVAL && !start && forwardPad == null) {
                    forwardPad = w;
                }
            }
        }
        Box all = r.bounds();
        return new SectorFill(rooms, r.edges(), markers, arrival, guardian, gate, vault, pedestals, r.expansion, all == null ? region : all, back, forward, forwardPad);
    }

    /** The checkable face of this content for the sector with the given id, state name, region and relic count (a sector of the old model). */
    public SectorView view(int id, String state, Box region, int relicCount, int nextId) { return view(id, state, region, relicCount, nextId, false, true); }

    /** The checkable face of this content; {@code floorModel} and {@code boss} say which kind of sector it is (an ordinary floor has no boss chain and no seal). */
    public SectorView view(int id, String state, Box region, int relicCount, int nextId, boolean floorModel, boolean boss) {
        List<SectorView.MarkerView> mv = new ArrayList<>();
        for (MarkerOut m : markers) mv.add(new SectorView.MarkerView(m.spec().kind(), m.cell()));
        return new SectorView(id, state, region, rooms, edges, mv, relicCount, pedestals, arrival, guardianSpawn, gatekeeper, vaultCenter,
                expansion == null ? null : expansion.seal(), expansion == null ? null : expansion.facing(), nextId, floorModel, boss, stairBack, stairForward, stairForwardPad);
    }
}
