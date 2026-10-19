package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the search for a place is a ladder (the ring, a wider ring, every room, a far place in view) over a plain layout, and each rung is a promise: the first is
 * exactly the old behaviour, the next ones find a place where the old one found none, none of them ever puts a monster in a safe room or nearer than the hard
 * minimum, and the sight rule is eased only on the last rung and only far away. These tests build the layouts that broke the old search (a floor of corridors, a
 * party spread over two places, a floor built at another height) and pin each promise.
 */
class SpawnSearchTest {
    private static final int Y0 = 56;

    private static SpawnLayout.RoomBox room(int id, RoomKind kind, int x, int z, int w, int d) { return room(id, kind, x, z, w, d, Y0); }

    private static SpawnLayout.RoomBox room(int id, RoomKind kind, int x, int z, int w, int d, int minY) {
        return new SpawnLayout.RoomBox(id, kind, x, minY, z, x + w - 1, minY + 47, z + d - 1);
    }

    private static List<SpawnPicker.Eye> eyeIn(SpawnLayout.RoomBox r) {
        return List.of(new SpawnPicker.Eye((r.minX() + r.maxX()) / 2.0 + 0.5, r.standY() + 1.62, (r.minZ() + r.maxZ()) / 2.0 + 0.5));
    }

    /** A row of rooms along x, each 27 wide, kinds as given, joined in a chain. */
    private static SpawnLayout row(RoomKind... kinds) {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < kinds.length; i++) {
            rooms.add(room(i, kinds[i], i * 27, 0, 27, 27));
            if (i > 0) edges.add(new int[] {i - 1, i});
        }
        return new SpawnLayout(rooms, List.of(), edges);
    }

    private static final SpawnPicker.LineOfSight NOTHING_SEEN = (ex, ey, ez, cx, cy, cz) -> false;
    private static final SpawnPicker.LineOfSight EVERYTHING_SEEN = (ex, ey, ez, cx, cy, cz) -> true;

    @Test
    void aFloorOfCorridorsGivesTheOldSearchNothingAndTheLadderAPlace() {
        SpawnLayout layout = row(RoomKind.LANDING, RoomKind.CORRIDOR, RoomKind.JUNCTION, RoomKind.CORRIDOR, RoomKind.CORRIDOR, RoomKind.JUNCTION, RoomKind.CORRIDOR);
        List<SpawnPicker.Eye> eyes = eyeIn(layout.room(3));
        SpawnSearch.Found ringOnly = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, NOTHING_SEEN, 48, false, 4, new Random(1));
        assertFalse(ringOnly.ok(), "no marker and no ordinary room: the 1.54.0 search has nothing to draw from");
        SpawnSearch.Found ladder = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, NOTHING_SEEN, 48, true, 4, new Random(1));
        assertTrue(ladder.ok(), ladder.trace().toString());
        assertEquals(2, ladder.tier().index(), "the rung that reads every room, corridors included");
        assertEquals(CellFinder.Mode.DENSE, ladder.tier().mode());
        assertEquals(3, ladder.trace().size(), "one line per rung tried");
    }

    @Test
    void theFirstRungIsExactlyTheOldBehaviourAndStopsTheLadder() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        for (int i = 0; i < 8; i++) rooms.add(room(i, i == 0 ? RoomKind.LANDING : RoomKind.HALL, i * 27, 0, 27, 27));
        List<SpawnLayout.Spot> spots = new ArrayList<>();
        for (int i = 2; i < 8; i++) spots.add(new SpawnLayout.Spot(i * 27 + 13, 65, 13, 1));
        SpawnLayout layout = new SpawnLayout(rooms, spots, List.of());
        SpawnSearch.Found f = SpawnSearch.find(layout, eyeIn(layout.room(1)), 24.0, 48.0, 2, NOTHING_SEEN, 48, true, 3, new Random(3));
        assertTrue(f.ok());
        assertEquals(0, f.tier().index());
        assertEquals(1, f.trace().size(), "the ladder stopped at the first rung");
        for (SpawnPicker.Cell c : f.cells()) assertTrue(SpawnPicker.nearest(c, eyeIn(layout.room(1))) >= 24.0);
    }

    @Test
    void theCandidatesAreTakenRoundEveryMemberNotRoundTheCentreOfTheParty() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        rooms.add(room(0, RoomKind.HALL, 0, 0, 27, 27));
        rooms.add(room(1, RoomKind.HALL, 400, 0, 27, 27));
        rooms.add(room(2, RoomKind.HALL, 430, 0, 27, 27));              // 30 blocks from the second member, 230 from the centre
        SpawnLayout layout = new SpawnLayout(rooms, List.of(new SpawnLayout.Spot(443, 65, 13, 1)), List.of());
        List<SpawnPicker.Eye> party = List.of(new SpawnPicker.Eye(13.5, 66.6, 13.5), new SpawnPicker.Eye(413.5, 66.6, 13.5));
        List<SpawnPicker.Cell> cells = CellFinder.candidates(layout, party, 64.0, CellFinder.Mode.LEGACY, null, new Random(1));
        boolean nearSecond = false;
        for (SpawnPicker.Cell c : cells) if (c.x() >= 430) nearSecond = true;
        assertTrue(nearSecond, "a hall beside the second member is a candidate");
        SpawnSearch.Found f = SpawnSearch.find(layout, party, 24.0, 48.0, 1, NOTHING_SEEN, 48, false, 4, new Random(1));
        assertTrue(f.ok());
        assertTrue(f.cells().get(0).x() >= 430, "and it is the one found");
    }

    @Test
    void aSafeRoomIsNeverACandidateOnAnyRung() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        rooms.add(room(0, RoomKind.HALL, 0, 0, 27, 27));
        rooms.add(room(1, RoomKind.SAFE, 30, 0, 27, 27));
        rooms.add(room(2, RoomKind.STAIRWELL, 60, 0, 27, 27));
        rooms.add(room(3, RoomKind.ARENA, 90, 0, 27, 27));
        rooms.add(room(4, RoomKind.VAULT, 120, 0, 27, 27));
        List<SpawnLayout.Spot> spots = new ArrayList<>();
        for (int r = 1; r <= 4; r++) spots.add(new SpawnLayout.Spot(r * 30 + 13, 65, 13, 5));      // markers in the safe and boss rooms
        SpawnLayout layout = new SpawnLayout(rooms, spots, List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3}, new int[] {3, 4}));
        for (int tier = 0; tier < 4; tier++) {
            for (long seed = 0; seed < 10; seed++) {
                SpawnSearch.Found f = SpawnSearch.find(layout, eyeIn(layout.room(0)), 24.0, 48.0, 4, NOTHING_SEEN, 48, tier > 0, 4, new Random(seed));
                List<SpawnPicker.Cell> all = new ArrayList<>(f.cells());
                all.addAll(f.reserve());
                for (SpawnPicker.Cell c : all) {
                    assertFalse(Sanctuary.is(layout.room(c.roomId()).kind()), "cell in " + layout.room(c.roomId()).kind());
                    assertFalse(c.safe());
                }
            }
        }
        for (SpawnPicker.Cell c : CellFinder.candidates(layout, eyeIn(layout.room(0)), 200.0, CellFinder.Mode.DENSE, null, new Random(1))) {
            SpawnLayout.RoomBox r = layout.room(c.roomId());
            assertTrue(!Sanctuary.is(r.kind()) || c.safe(), "every cell of a safe room that is made is marked safe");
        }
    }

    @Test
    void noRungEverPlacesAnythingNearerThanTheHardMinimum() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        for (int i = 0; i < 6; i++) rooms.add(room(i, RoomKind.HALL, i * 27, 0, 27, 27));
        SpawnLayout layout = new SpawnLayout(rooms, List.of(), List.of());
        List<SpawnPicker.Eye> eyes = eyeIn(layout.room(0));
        for (double min : new double[] {1.0, 4.0, 10.0, 24.0}) {
            for (SpawnSearch.Tier t : SpawnSearch.tiers(min, min + 12.0, true, 4)) assertTrue(t.min() >= SpawnSearch.HARD_MIN, "rung " + t.name() + " of a ring starting at " + min + ": " + t.min());
            SpawnSearch.Found f = SpawnSearch.find(layout, eyes, min, min + 12.0, 3, NOTHING_SEEN, 48, true, 4, new Random(4));
            for (SpawnPicker.Cell c : f.cells()) assertTrue(SpawnPicker.nearest(c, eyes) >= SpawnSearch.HARD_MIN);
        }
    }

    @Test
    void theSightRuleIsEasedOnlyOnTheLastRungAndOnlyFarAway() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        for (int i = 0; i < 10; i++) rooms.add(room(i, RoomKind.HALL, i * 27, 0, 27, 27));
        SpawnLayout layout = new SpawnLayout(rooms, List.of(), List.of());
        List<SpawnPicker.Eye> eyes = eyeIn(layout.room(0));
        SpawnSearch.Found strict = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, EVERYTHING_SEEN, 48, true, 4, new Random(2));
        assertFalse(strict.ok(), "a party that sees everything is given no place while the rule is whole");
        SpawnSearch.Found eased = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, EVERYTHING_SEEN, 48, true, 3, new Random(2));
        assertTrue(eased.ok(), eased.trace().toString());
        assertEquals(3, eased.tier().index());
        assertTrue(eased.tier().relaxSightFrom() >= 36.0, "three quarters of the ring's far edge: " + eased.tier().relaxSightFrom());
        for (SpawnPicker.Cell c : eased.cells()) assertTrue(SpawnPicker.nearest(c, eyes) >= eased.tier().relaxSightFrom() - 1e-9);
        // and the same party with a line of sight that sees only what is near: the eased rung takes the far cells only
        SpawnPicker.LineOfSight nearOnly = (ex, ey, ez, cx, cy, cz) -> Math.hypot(ex - cx, ez - cz) < 30.0;
        SpawnSearch.Found f = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, nearOnly, 48, true, 4, new Random(2));
        assertTrue(f.ok());
        assertEquals(0, f.tier().index(), "an ordinary party finds its place on the first rung");
    }

    @Test
    void theRungsWidenInOrderAndTheLadderCanBeSwitchedOff() {
        List<SpawnSearch.Tier> t = SpawnSearch.tiers(24.0, 48.0, true, 3);
        assertEquals(4, t.size());
        for (int i = 0; i < t.size(); i++) assertEquals(i, t.get(i).index());
        for (int i = 1; i < t.size(); i++) {
            assertTrue(t.get(i).min() <= t.get(i - 1).min());
            assertTrue(t.get(i).max() >= t.get(i - 1).max());
        }
        assertEquals(CellFinder.Mode.LEGACY, t.get(0).mode());
        assertEquals(CellFinder.Mode.DENSE, t.get(3).mode());
        assertEquals(0.0, t.get(0).relaxSightFrom(), 0.0);
        assertEquals(0.0, t.get(2).relaxSightFrom(), 0.0);
        assertTrue(t.get(3).relaxSightFrom() > 0.0);
        assertEquals(1, SpawnSearch.tiers(24.0, 48.0, false, 3).size());
        for (SpawnSearch.Tier x : SpawnSearch.tiers(24.0, 48.0, true, 5)) assertEquals(0.0, x.relaxSightFrom(), 0.0, "5 never eases the rule");
        // a narrow ring of a wall ambush widens too, and stays a ring
        List<SpawnSearch.Tier> narrow = SpawnSearch.tiers(10.0, 22.0, true, 3);
        assertEquals(10.0, narrow.get(0).min(), 0.0);
        assertEquals(22.0, narrow.get(0).max(), 0.0);
        assertTrue(narrow.get(3).max() >= 44.0);
    }

    @Test
    void roomsNobodyCanWalkToCountLittleAndNearRoomsCountFully() {
        assertEquals(1.0, CellFinder.hopFactor(0), 0.0);
        assertEquals(1.0, CellFinder.hopFactor(2), 0.0);
        assertTrue(CellFinder.hopFactor(4) < 1.0 && CellFinder.hopFactor(4) > CellFinder.hopFactor(9));
        assertTrue(CellFinder.hopFactor(null) < CellFinder.hopFactor(9), "a sealed-off room is worth less than the farthest reachable one");
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        rooms.add(room(0, RoomKind.HALL, 0, 0, 27, 27));
        rooms.add(room(1, RoomKind.HALL, 30, 0, 27, 27));              // joined to room 0
        rooms.add(room(2, RoomKind.HALL, 60, 0, 27, 27));              // not joined to anything
        SpawnLayout layout = new SpawnLayout(rooms, List.of(new SpawnLayout.Spot(43, 65, 13, 1), new SpawnLayout.Spot(73, 65, 13, 1)), List.of(new int[] {0, 1}));
        Map<Integer, Integer> hops = layout.hopsFrom(List.of(0));
        assertEquals(Map.of(0, 0, 1, 1), hops);
        List<SpawnPicker.Cell> cells = CellFinder.candidates(layout, eyeIn(layout.room(0)), 100.0, CellFinder.Mode.LEGACY, hops, new Random(1));
        double joined = 0, sealed = 0;
        for (SpawnPicker.Cell c : cells) {
            if (c.roomId() == 1) joined = Math.max(joined, c.weight());
            if (c.roomId() == 2) sealed = Math.max(sealed, c.weight());
        }
        assertTrue(joined > sealed * 3, "the joined room's marker outweighs the sealed one: " + joined + " against " + sealed);
    }

    @Test
    void theDenseRungIsBoundedAndAddsTheCellsInsideEveryDoorway() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            rooms.add(room(i, i % 3 == 0 ? RoomKind.HALL : RoomKind.CORRIDOR, (i % 9) * 27, (i / 9) * 27, 27, 27));
            if (i % 9 > 0) edges.add(new int[] {i - 1, i});
        }
        SpawnLayout big = new SpawnLayout(rooms, List.of(), edges);
        List<SpawnPicker.Cell> cells = CellFinder.candidates(big, List.of(new SpawnPicker.Eye(120.0, 66.0, 120.0)), 400.0, CellFinder.Mode.DENSE, null, new Random(1));
        assertTrue(cells.size() <= CellFinder.DENSE_CAP, "bounded: " + cells.size());
        assertTrue(cells.size() > 100);
        // a pair of touching rooms: a cell just inside each, on the line of the contact
        SpawnLayout pair = new SpawnLayout(List.of(room(0, RoomKind.HALL, 0, 0, 27, 27), room(1, RoomKind.HALL, 27, 0, 27, 27)), List.of(), List.of(new int[] {0, 1}));
        boolean inA = false, inB = false;
        for (SpawnPicker.Cell c : CellFinder.candidates(pair, List.of(new SpawnPicker.Eye(13.0, 66.0, 13.0)), 100.0, CellFinder.Mode.DENSE, null, new Random(1))) {
            if (c.roomId() == 0 && c.x() == 23 && c.z() == 13) inA = true;          // the wall is the last two layers (25, 26): 23 is the second cell inside
            if (c.roomId() == 1 && c.x() == 30 && c.z() == 13) inB = true;
        }
        assertTrue(inA && inB, "doorway cells on both sides of the wall between the rooms");
    }

    @Test
    void aFloorBuiltAtAnotherHeightIsLookedAtWhereItsOwnFloorIs() {
        int high = 300;                                                  // a floor stacked far above the standard grid
        SpawnLayout.RoomBox r = room(0, RoomKind.HALL, 0, 0, 27, 27, high);
        assertEquals(high + 9, r.standY(), "the floor row of the template is 8 above the bottom of the box, the feet one above that");
        assertEquals(65, room(1, RoomKind.HALL, 0, 0, 27, 27, 56).standY(), "on the standard grid that is the familiar row 65");
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        for (int i = 0; i < 6; i++) rooms.add(room(i, RoomKind.HALL, i * 27, 0, 27, 27, high));
        SpawnLayout layout = new SpawnLayout(rooms, List.of(), List.of());
        SpawnSearch.Found f = SpawnSearch.find(layout, eyeIn(layout.room(0)), 24.0, 48.0, 3, NOTHING_SEEN, 48, true, 4, new Random(5));
        assertTrue(f.ok());
        for (SpawnPicker.Cell c : f.cells()) assertEquals(high + 9, c.y());
    }

    @Test
    void theSameSeedGivesTheSameCellsAndNobodyToAppearForGivesNothing() {
        SpawnLayout layout = row(RoomKind.LANDING, RoomKind.HALL, RoomKind.HALL, RoomKind.HALL, RoomKind.HALL, RoomKind.HALL);
        List<SpawnPicker.Eye> eyes = eyeIn(layout.room(1));
        assertEquals(SpawnSearch.find(layout, eyes, 24.0, 48.0, 3, NOTHING_SEEN, 48, true, 3, new Random(8)).cells(), SpawnSearch.find(layout, eyes, 24.0, 48.0, 3, NOTHING_SEEN, 48, true, 3, new Random(8)).cells());
        assertFalse(SpawnSearch.find(layout, List.of(), 24.0, 48.0, 3, NOTHING_SEEN, 48, true, 3, new Random(8)).ok());
        assertFalse(SpawnSearch.find(layout, eyes, 24.0, 48.0, 0, NOTHING_SEEN, 48, true, 3, new Random(8)).ok());
        assertFalse(SpawnSearch.find(new SpawnLayout(List.of(), List.of(), List.of()), eyes, 24.0, 48.0, 2, NOTHING_SEEN, 48, true, 3, new Random(8)).ok(), "an empty floor plan gives nothing and does not throw");
    }

    @Test
    void theReserveIsKeptApartFromTheCellsAndAllOfThemObeyTheRung() {
        List<SpawnLayout.RoomBox> rooms = new ArrayList<>();
        for (int i = 0; i < 12; i++) rooms.add(room(i, RoomKind.HALL, i * 27, 0, 27, 27));
        List<SpawnLayout.Spot> spots = new ArrayList<>();
        for (int i = 0; i < 12; i++) for (int k = 0; k < 4; k++) spots.add(new SpawnLayout.Spot(i * 27 + 4 + k * 5, 65, 6 + k * 4, 1));
        SpawnLayout layout = new SpawnLayout(rooms, spots, List.of());
        List<SpawnPicker.Eye> eyes = eyeIn(layout.room(0));
        SpawnSearch.Found f = SpawnSearch.find(layout, eyes, 24.0, 48.0, 2, NOTHING_SEEN, 48, true, 4, new Random(6));
        assertEquals(2, f.cells().size());
        assertTrue(f.reserve().size() >= 1 && f.reserve().size() <= SpawnSearch.RESERVE);
        List<SpawnPicker.Cell> all = new ArrayList<>(f.cells());
        all.addAll(f.reserve());
        for (int i = 0; i < all.size(); i++) {
            assertTrue(SpawnPicker.nearest(all.get(i), eyes) >= f.tier().min() && SpawnPicker.nearest(all.get(i), eyes) <= f.tier().max());
            for (int j = i + 1; j < all.size(); j++) assertNotEquals(all.get(i), all.get(j));
        }
    }
}
