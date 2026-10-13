package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

/** Where the walls of a room birth: spread out, a fair way from the player, and on the side the player is not looking at. */
class SpotPickTest {
    /** Seeds that are far apart in effect (a plain Random of consecutive seeds starts with nearly the same numbers). */
    private static Dice dice(long seed) { return new SplittableRandom(seed)::nextDouble; }

    private static List<SpotPick.Spot> room() {
        List<SpotPick.Spot> s = new ArrayList<>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) s.add(new SpotPick.Spot(x, 64, z));
        return s;
    }

    private static double dist(SpotPick.Spot a, double px, double py, double pz) {
        double dx = a.x() + 0.5 - px, dy = a.y() - py, dz = a.z() + 0.5 - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Test
    void spotsKeepTheirDistanceFromThePlayerAndFromEachOther() {
        for (long seed = 0; seed < 40; seed++) {
            List<SpotPick.Spot> out = SpotPick.pick(room(), 8.5, 64, 8.5, 0, 1, 8, 7, 24, 3, dice(seed));
            for (SpotPick.Spot s : out) {
                double d = dist(s, 8.5, 64, 8.5);
                assertTrue(d >= 7 && d <= 24, "distance " + d);
            }
            for (int i = 0; i < out.size(); i++) for (int j = i + 1; j < out.size(); j++) {
                SpotPick.Spot a = out.get(i), b = out.get(j);
                double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
                assertTrue(Math.sqrt(dx * dx + dy * dy + dz * dz) >= 3.0 - 1e-9, "spacing between " + a + " and " + b);
            }
        }
    }

    @Test
    void neverMoreThanAskedFor() {
        for (int count = 0; count <= 8; count++) assertTrue(SpotPick.pick(room(), 8.5, 64, 8.5, 1, 0, count, 5, 24, 3, dice(count)).size() <= count);
        assertTrue(SpotPick.pick(room(), 8.5, 64, 8.5, 1, 0, 0, 5, 24, 3, dice(1)).isEmpty());
        assertTrue(SpotPick.pick(room(), 8.5, 64, 8.5, 1, 0, -2, 5, 24, 3, dice(1)).isEmpty());
        assertTrue(SpotPick.pick(new ArrayList<>(), 8.5, 64, 8.5, 1, 0, 5, 5, 24, 3, dice(1)).isEmpty());
    }

    @Test
    void fewerSpotsThanAskedGivesFewer() {
        List<SpotPick.Spot> few = List.of(new SpotPick.Spot(0, 64, 0), new SpotPick.Spot(1, 64, 0), new SpotPick.Spot(15, 64, 15));
        List<SpotPick.Spot> out = SpotPick.pick(few, 8.5, 64, 8.5, 0, 1, 6, 7, 24, 3, dice(3));
        assertEquals(2, out.size(), "(0,0) and (1,0) are one spot apart: only one of them can be used");
    }

    @Test
    void spotsOutsideTheDistanceBandAreNeverUsed() {
        List<SpotPick.Spot> near = List.of(new SpotPick.Spot(8, 64, 8), new SpotPick.Spot(9, 64, 9), new SpotPick.Spot(10, 64, 8));
        assertTrue(SpotPick.pick(near, 8.5, 64, 8.5, 0, 1, 5, 7, 24, 3, dice(3)).isEmpty());
        List<SpotPick.Spot> far = List.of(new SpotPick.Spot(100, 64, 100));
        assertTrue(SpotPick.pick(far, 8.5, 64, 8.5, 0, 1, 5, 7, 24, 3, dice(3)).isEmpty());
        List<SpotPick.Spot> otherFloor = List.of(new SpotPick.Spot(8, 100, 20));
        assertTrue(SpotPick.pick(otherFloor, 8.5, 64, 8.5, 0, 1, 5, 7, 24, 3, dice(3)).isEmpty(), "the distance counts the height too");
    }

    @Test
    void theBirthsPreferTheSpotsBehindThePlayerButNotOnlyThose() {
        // the player stands in the middle of the room, looking along +z: behind is z < 8.5
        int behind = 0, ahead = 0;
        for (long seed = 0; seed < 600; seed++) {
            for (SpotPick.Spot s : SpotPick.pick(room(), 8.5, 64, 8.5, 0, 1, 3, 4, 24, 3, dice(seed))) {
                if (s.z() + 0.5 < 8.5) behind++; else ahead++;
            }
        }
        double share = behind / (double) (behind + ahead);
        assertTrue(share > 0.65 && share < 0.97, "the share of births behind the player: " + share);
        assertTrue(ahead > 0, "the ones ahead are rarer, not impossible");
    }

    @Test
    void theDrawWeighsSpotsByWhereTheyLie() {
        assertEquals(1.15, SpotPick.weight(-1.0), 1e-9);
        assertEquals(0.4, SpotPick.weight(0.0), 1e-9);
        assertEquals(0.15, SpotPick.weight(1.0), 1e-9);
        double last = Double.MAX_VALUE;
        for (double dot = -1.0; dot <= 1.0; dot += 0.1) {
            double w = SpotPick.weight(dot);
            assertTrue(w < last + 1e-12 && w > 0.0, "the weight only falls as the spot moves ahead of the player: " + dot);
            last = w;
        }
        assertEquals(SpotPick.weight(-5.0), SpotPick.weight(-1.0), 1e-9, "the look direction is clamped");
    }

    @Test
    void spotsAheadAreUsedWhenNothingIsBehind() {
        List<SpotPick.Spot> allAhead = new ArrayList<>();
        for (int x = 0; x < 16; x++) for (int z = 12; z < 16; z++) allAhead.add(new SpotPick.Spot(x, 64, z));
        List<SpotPick.Spot> out = SpotPick.pick(allAhead, 8.5, 64, 4.5, 0, 1, 3, 7, 24, 3, dice(5));
        assertEquals(3, out.size(), "the preference never refuses a spot, it only orders them");
    }

    @Test
    void withNoLookDirectionThereIsNoPreference() {
        int behind = 0, ahead = 0;
        for (long seed = 0; seed < 300; seed++) {
            for (SpotPick.Spot s : SpotPick.pick(room(), 8.5, 64, 8.5, 0, 0, 3, 4, 24, 3, dice(seed))) {
                if (s.z() + 0.5 < 8.5) behind++; else ahead++;
            }
        }
        assertEquals(1.0, behind / (double) ahead, 0.35);
    }

    @Test
    void sameSeedSamePick() {
        assertEquals(SpotPick.pick(room(), 3.5, 64, 12.5, 1, 1, 6, 7, 24, 3, dice(77)), SpotPick.pick(room(), 3.5, 64, 12.5, 1, 1, 6, 7, 24, 3, dice(77)));
    }
}
