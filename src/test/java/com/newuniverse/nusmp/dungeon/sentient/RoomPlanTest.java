package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The owner's numbers: up to 8 mobs a room on the Upper Catacombs, none in the Monarch's Vault, tunnels and nooks are not rooms. */
class RoomPlanTest {
    private static final long SEED = 123456789L;

    @Test
    void theUpperCatacombsBirthFiveToEightAndNeverMore() {
        int populated = 0;
        for (int i = 0; i < 4000; i++) {
            RoomPlan.Birth b = RoomPlan.decide(FloorKind.CATACOMBS, RoomKey.of(i, i * 7 - 300, i % 4), SEED, 64);
            if (!b.populated()) {
                assertEquals(0, b.count());
                continue;
            }
            populated++;
            assertTrue(b.count() >= 5 && b.count() <= 8, "count " + b.count());
        }
        assertTrue(populated > 0);
    }

    @Test
    void noRoomIsEverAboveTheHardCapWhateverTheSetting() {
        for (FloorKind kind : FloorKind.values()) {
            for (int i = 0; i < 1500; i++) {
                RoomPlan.Birth b = RoomPlan.decide(kind, RoomKey.of(i * 3, -i, 2), SEED, 5000, 3.0);
                assertTrue(b.count() <= 8, kind + " " + b.count() + ": the design's cap is 8 mobs a room");
            }
        }
    }

    @Test
    void aPopulatedRoomBirthsAtLeastOneMobWhateverTheSetting() {
        int populated = 0;
        for (int i = 0; i < 2000; i++) {
            RoomPlan.Birth b = RoomPlan.decide(FloorKind.UNDERWORLD, RoomKey.of(i, 3, 1), SEED, 100, 0.0);
            if (!b.populated()) continue;
            populated++;
            assertEquals(1, b.count());
        }
        assertTrue(populated > 500);
    }

    @Test
    void theMonarchsVaultAndTheLegacyZonesNeverBirthAnything() {
        for (int i = 0; i < 3000; i++) {
            long k = RoomKey.of(i, -i, i % 5);
            assertFalse(RoomPlan.decide(FloorKind.MONARCH, k, SEED, 500).populated());
            assertFalse(RoomPlan.decide(FloorKind.LEGACY, k, SEED, 500, 3.0).populated());
        }
    }

    @Test
    void theSameRoomAlwaysRollsTheSame() {
        for (int i = 0; i < 500; i++) {
            long k = RoomKey.of(i, i * 5, 1);
            for (FloorKind kind : FloorKind.values()) {
                assertEquals(RoomPlan.decide(kind, k, SEED, 80), RoomPlan.decide(kind, k, SEED, 80));
                assertEquals(RoomPlan.decide(kind, k, SEED, 80, 1.5), RoomPlan.decide(kind, k, SEED, 80, 1.5));
            }
        }
    }

    @Test
    void tunnelsAndNooksAreNotRooms() {
        assertEquals(10, RoomPlan.MIN_SPOTS, "the design's number: fewer than 10 standable spots is not a room");
        for (int spots = 0; spots <= 9; spots++) {
            for (int i = 0; i < 400; i++) assertFalse(RoomPlan.decide(FloorKind.CATACOMBS, RoomKey.of(i, i, 0), SEED, spots).populated(), "spots " + spots);
        }
        int populated = 0;
        for (int i = 0; i < 400; i++) if (RoomPlan.decide(FloorKind.CATACOMBS, RoomKey.of(i, i, 0), SEED, 10).populated()) populated++;
        assertTrue(populated > 200, "a place with ten standable spots is a room: " + populated + " of 400 are populated");
    }

    @Test
    void theCatacombsCountsAreSpreadEvenlyOverFiveToEight() {
        int[] seen = new int[9];
        int populated = 0;
        for (int i = 0; i < 40000; i++) {
            RoomPlan.Birth b = RoomPlan.decide(FloorKind.CATACOMBS, RoomKey.of(i % 500 - 250, i / 500 - 40, i % 7), SEED, 200);
            if (!b.populated()) continue;
            seen[b.count()]++;
            populated++;
        }
        for (int c = 5; c <= 8; c++) assertEquals(0.25, seen[c] / (double) populated, 0.03, "count " + c);
        for (int c = 0; c <= 4; c++) assertEquals(0, seen[c], "count " + c);
        int[] water = new int[9], devils = new int[9];
        int w = 0, u = 0;
        for (int i = 0; i < 40000; i++) {
            long k = RoomKey.of(i % 500 - 250, i / 500 - 40, i % 7);
            RoomPlan.Birth a = RoomPlan.decide(FloorKind.WATERWAYS, k, SEED, 200), b = RoomPlan.decide(FloorKind.UNDERWORLD, k, SEED, 200);
            if (a.populated()) {
                water[a.count()]++;
                w++;
            }
            if (b.populated()) {
                devils[b.count()]++;
                u++;
            }
        }
        for (int c = 4; c <= 6; c++) assertEquals(1.0 / 3.0, water[c] / (double) w, 0.03, "waterways " + c);
        for (int c = 3; c <= 5; c++) assertEquals(1.0 / 3.0, devils[c] / (double) u, 0.03, "underworld " + c);
    }

    @Test
    void theShareOfPopulatedRoomsFollowsTheFloor() {
        int n = 20000;
        for (FloorKind kind : new FloorKind[]{FloorKind.CATACOMBS, FloorKind.WATERWAYS, FloorKind.UNDERWORLD}) {
            int populated = 0;
            for (int i = 0; i < n; i++) if (RoomPlan.decide(kind, RoomKey.of(i % 400 - 200, i / 400 - 25, i % 3), SEED, 100).populated()) populated++;
            double share = 100.0 * populated / n;
            assertEquals(RoomPlan.percent(kind), share, 3.0, kind + " populated " + share + "%");
        }
        assertEquals(70, RoomPlan.percent(FloorKind.CATACOMBS));
        assertEquals(60, RoomPlan.percent(FloorKind.WATERWAYS));
        assertEquals(60, RoomPlan.percent(FloorKind.UNDERWORLD));
        assertEquals(0, RoomPlan.percent(FloorKind.MONARCH));
        assertEquals(0, RoomPlan.percent(FloorKind.LEGACY));
    }

    @Test
    void everyFloorKeepsToItsRange() {
        for (int i = 0; i < 6000; i++) {
            long k = RoomKey.of(i - 3000, i * 11, i % 6);
            RoomPlan.Birth w = RoomPlan.decide(FloorKind.WATERWAYS, k, SEED, 300);
            if (w.populated()) assertTrue(w.count() >= 4 && w.count() <= 6, "waterways " + w.count());
            RoomPlan.Birth u = RoomPlan.decide(FloorKind.UNDERWORLD, k, SEED, 300);
            if (u.populated()) assertTrue(u.count() >= 3 && u.count() <= 5, "underworld " + u.count());
        }
    }

    @Test
    void aRoomIsNeverGivenMoreThanAThirdOfItsSpots() {
        for (int spots = RoomPlan.MIN_SPOTS; spots < 40; spots++) {
            for (int i = 0; i < 300; i++) {
                RoomPlan.Birth b = RoomPlan.decide(FloorKind.CATACOMBS, RoomKey.of(i, spots, 0), SEED, spots, 3.0);
                if (b.populated()) assertTrue(b.count() >= 1 && b.count() <= spots / 3, "spots " + spots + " count " + b.count());
            }
        }
    }

    @Test
    void theRoomPopulationSettingScalesTheCount() {
        double low = 0, high = 0;
        int n = 0;
        for (int i = 0; i < 3000; i++) {
            long k = RoomKey.of(i, -i * 3, 1);
            RoomPlan.Birth a = RoomPlan.decide(FloorKind.CATACOMBS, k, SEED, 200, 0.25), b = RoomPlan.decide(FloorKind.CATACOMBS, k, SEED, 200, 1.0);
            assertEquals(a.populated(), b.populated(), "the setting never changes which rooms are populated");
            if (!a.populated()) continue;
            assertTrue(a.count() >= 1 && a.count() <= 2, "quarter: " + a.count());
            low += a.count();
            high += b.count();
            n++;
        }
        assertTrue(n > 100 && low < high);
    }

    @Test
    void theWorldSeedChangesTheRolls() {
        int differ = 0;
        for (int i = 0; i < 500; i++) {
            long k = RoomKey.of(i, i, 0);
            if (!RoomPlan.decide(FloorKind.CATACOMBS, k, 1L, 100).equals(RoomPlan.decide(FloorKind.CATACOMBS, k, 2L, 100))) differ++;
        }
        assertTrue(differ > 100, "differ " + differ);
    }
}
