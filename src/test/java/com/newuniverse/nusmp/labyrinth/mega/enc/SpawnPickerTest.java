package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: where an encounter appears is half of how it feels; these tests pin the rules of the picker (the distance band, no safe cell, apart from each other,
 * never in sight of any member, a bounded number of ray casts, heavier cells first).
 */
class SpawnPickerTest {
    private static final SpawnPicker.Params P = new SpawnPicker.Params(24, 48, 4, 100, 3);
    private static final List<SpawnPicker.Eye> ONE = List.of(new SpawnPicker.Eye(0.5, 65.6, 0.5));

    private static SpawnPicker.Cell cell(int x, int z) { return new SpawnPicker.Cell(x, 65, z, 1.0, 0, false); }

    private static List<SpawnPicker.Cell> ring(int radius, int n) {
        List<SpawnPicker.Cell> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            out.add(cell((int) Math.round(Math.cos(a) * radius), (int) Math.round(Math.sin(a) * radius)));
        }
        return out;
    }

    @Test
    void onlyCellsInTheDistanceBandAreChosen() {
        List<SpawnPicker.Cell> all = new ArrayList<>();
        all.addAll(ring(10, 12));
        all.addAll(ring(30, 12));
        all.addAll(ring(70, 12));
        for (long seed = 0; seed < 40; seed++) {
            List<SpawnPicker.Cell> got = SpawnPicker.pick(all, ONE, P, null, new Random(seed));
            assertFalse(got.isEmpty());
            for (SpawnPicker.Cell c : got) {
                double d = SpawnPicker.nearest(c, ONE);
                assertTrue(d >= 24 && d <= 48, "distance " + d);
            }
        }
    }

    @Test
    void nobodyInThePartyMayHaveACellCloserThanTheMinimum() {
        List<SpawnPicker.Eye> party = List.of(new SpawnPicker.Eye(0, 65, 0), new SpawnPicker.Eye(40, 65, 0));
        // a cell 30 away from the first member but 10 from the second is too close to someone
        List<SpawnPicker.Cell> all = List.of(cell(-30, 0), cell(50, 0), cell(20, 30));
        for (long seed = 0; seed < 30; seed++)
            for (SpawnPicker.Cell c : SpawnPicker.pick(all, party, P, null, new Random(seed))) {
                assertTrue(SpawnPicker.nearest(c, party) >= 24, c.toString());
                assertFalse(c.x() == 50, "10 blocks from the second member");
            }
    }

    @Test
    void safeCellsAndCellsWithNoWeightAreNeverChosen() {
        List<SpawnPicker.Cell> all = List.of(new SpawnPicker.Cell(30, 65, 0, 1.0, 1, true), new SpawnPicker.Cell(0, 65, 30, 0.0, 2, false),
                new SpawnPicker.Cell(-30, 65, 0, -1.0, 3, false), new SpawnPicker.Cell(0, 65, -30, Double.NaN, 4, false));
        assertTrue(SpawnPicker.pick(all, ONE, P, null, new Random(1)).isEmpty());
    }

    @Test
    void cellsThatTheLineOfSightSeesAreSkipped() {
        List<SpawnPicker.Cell> all = ring(30, 16);
        SpawnPicker.LineOfSight eastIsOpen = (ex, ey, ez, cx, cy, cz) -> cx > 0;
        for (long seed = 0; seed < 30; seed++)
            for (SpawnPicker.Cell c : SpawnPicker.pick(all, ONE, P, eastIsOpen, new Random(seed))) assertTrue(c.x() <= 0, "seen cell " + c);
    }

    @Test
    void aCellAnyMemberCanSeeIsRefused() {
        List<SpawnPicker.Eye> party = List.of(new SpawnPicker.Eye(0, 65, 0), new SpawnPicker.Eye(1, 65, 1));
        List<SpawnPicker.Cell> all = ring(30, 8);
        SpawnPicker.LineOfSight secondSeesAll = (ex, ey, ez, cx, cy, cz) -> ex > 0.5;
        assertTrue(SpawnPicker.pick(all, party, P, secondSeesAll, new Random(1)).isEmpty());
    }

    @Test
    void theSightTestIsCappedSoARollNeverCastsAHundredRays() {
        List<SpawnPicker.Cell> all = ring(30, 40);
        int[] calls = {0};
        SpawnPicker.LineOfSight alwaysSeen = (ex, ey, ez, cx, cy, cz) -> {
            calls[0]++;
            return true;
        };
        List<SpawnPicker.Cell> got = SpawnPicker.pick(all, ONE, new SpawnPicker.Params(24, 48, 4, 7, 3), alwaysSeen, new Random(1));
        assertTrue(got.isEmpty());
        assertEquals(7, calls[0]);
    }

    @Test
    void picksAreKeptApartAndLimitedToWhatWasAsked() {
        List<SpawnPicker.Cell> all = new ArrayList<>();
        for (int x = 28; x < 44; x++) all.add(cell(x, 0));
        for (long seed = 0; seed < 30; seed++) {
            List<SpawnPicker.Cell> got = SpawnPicker.pick(all, ONE, new SpawnPicker.Params(24, 48, 4, 100, 3), null, new Random(seed));
            assertEquals(3, got.size());
            for (int i = 0; i < got.size(); i++)
                for (int j = i + 1; j < got.size(); j++)
                    assertTrue(Math.abs(got.get(i).x() - got.get(j).x()) >= 4, "too close: " + got.get(i) + " " + got.get(j));
        }
        assertEquals(1, SpawnPicker.pick(all, ONE, new SpawnPicker.Params(24, 48, 4, 100, 1), null, new Random(1)).size());
        assertTrue(SpawnPicker.pick(all, ONE, new SpawnPicker.Params(24, 48, 4, 100, 0), null, new Random(1)).isEmpty());
    }

    @Test
    void whenTheCellsAreCrowdedTogetherFewerComeBackButNeverTwoOnTopOfEachOther() {
        List<SpawnPicker.Cell> all = List.of(cell(30, 0), cell(31, 0), cell(30, 1));
        List<SpawnPicker.Cell> got = SpawnPicker.pick(all, ONE, new SpawnPicker.Params(24, 48, 4, 100, 3), null, new Random(2));
        assertEquals(1, got.size());
    }

    @Test
    void heavierCellsAreDrawnFirstMoreOften() {
        List<SpawnPicker.Cell> all = List.of(new SpawnPicker.Cell(30, 65, 0, 9.0, 1, false), new SpawnPicker.Cell(0, 65, 30, 1.0, 2, false));
        int heavyFirst = 0, n = 4000;
        Random r = new Random(12345);
        for (long i = 0; i < n; i++) {
            List<SpawnPicker.Cell> got = SpawnPicker.pick(all, ONE, new SpawnPicker.Params(24, 48, 4, 100, 1), null, r);
            if (got.get(0).roomId() == 1) heavyFirst++;
        }
        assertTrue(heavyFirst > n * 0.86 && heavyFirst < n * 0.94, "heavy cell first " + heavyFirst + " of " + n);
    }

    @Test
    void anEmptyPartyOrNoCandidatesGiveNothing() {
        assertTrue(SpawnPicker.pick(ring(30, 8), List.of(), P, null, new Random(1)).isEmpty());
        assertTrue(SpawnPicker.pick(List.of(), ONE, P, null, new Random(1)).isEmpty());
    }

    @Test
    void theSameSeedGivesTheSamePick() {
        List<SpawnPicker.Cell> all = ring(35, 24);
        List<SpawnPicker.Cell> a = SpawnPicker.pick(all, ONE, P, null, new Random(77));
        List<SpawnPicker.Cell> b = SpawnPicker.pick(all, ONE, P, null, new Random(77));
        assertEquals(a, b);
    }
}
