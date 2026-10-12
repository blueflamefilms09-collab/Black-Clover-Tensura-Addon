package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The Golden City against the tiers and the save record. */
class CityGlueTest {
    @Test
    void aCityCellIsExactlyWhereTheTierSaysGoldenCity() {
        int cities = 0;
        for (int cx = -30; cx <= 30; cx++) {
            for (int cz = -30; cz <= 30; cz++) {
                boolean city = DungeonTier.isCityCell(cx, cz);
                if (city) cities++;
                for (int[] probe : new int[][]{{0, 0}, {127, 127}, {64, 3}}) {
                    DungeonTier tier = DungeonTier.at(cx * 128 + probe[0], -50, cz * 128 + probe[1]);
                    assertEquals(city, tier == DungeonTier.GOLDEN_CITY);
                }
            }
        }
        assertTrue(cities > 300 && cities < 1000, "about 18% of 3721 cells: " + cities);
    }

    @Test
    void aStandingPlayerOnTheFloorOfACityIsInTheGoldenCityTier() {
        int found = 0;
        for (int cx = -8; cx <= 8 && found < 12; cx++) {
            for (int cz = -8; cz <= 8 && found < 12; cz++) {
                if (!DungeonTier.isCityCell(cx, cz)) continue;
                found++;
                CityPlan p = CityPlan.of(cx, cz);
                assertEquals(DungeonTier.GOLDEN_CITY, DungeonTier.at(p.centreX, p.floorY + 1, p.centreZ));
                assertEquals(DungeonTier.GOLDEN_CITY, DungeonTier.at(p.minX(), p.floorY + 1, p.centreZ));
                assertEquals(DungeonTier.GOLDEN_CITY, DungeonTier.at(p.maxX(), p.floorY + 1, p.centreZ));
            }
        }
        assertEquals(12, found);
    }

    @Test
    void theNearestCityOfTheMapIsACityCell() {
        int[] cell = CityPlan.nearest(5000, -3000, 10, DungeonTier::isCityCell);
        assertNotNull(cell);
        assertTrue(DungeonTier.isCityCell(cell[0], cell[1]));
    }

    @Test
    void cellKeysRoundTripAndClaimsAreOncePerPlayerPerCity() {
        for (int[] c : new int[][]{{0, 0}, {-1, -1}, {5, -9}, {-300, 77}, {123456, -654321}}) {
            long key = CitySave.key(c[0], c[1]);
            assertEquals(c[0], CitySave.cellX(key));
            assertEquals(c[1], CitySave.cellZ(key));
        }
        CitySave save = new CitySave();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        long city = CitySave.key(2, 3), other = CitySave.key(2, 4);
        assertTrue(save.claim(city, a));
        assertFalse(save.claim(city, a), "once");
        assertTrue(save.claim(city, b));
        assertTrue(save.claim(other, a), "another city, another claim");
        assertEquals(2, save.claims(city));
        assertFalse(save.isDone(city));
        save.progress(city, 12, 345L);
        assertEquals(12, save.lookup(city).op);
        save.finish(city);
        assertTrue(save.isDone(city));
        assertTrue(save.forget(city));
        assertFalse(save.isDone(city));
        assertEquals(0, save.claims(city));
        assertTrue(save.claim(city, a), "a reset gives the gift again");
        assertFalse(save.forget(CitySave.key(99, 99)));
    }

    @Test
    void theDoorsOfTheWaysInAreRememberedPerCityAndForgottenWithIt() {
        CitySave save = new CitySave();
        long city = CitySave.key(4, 4), other = CitySave.key(4, 5);
        assertEquals(0, save.ways(city).length, "nothing opened yet");
        save.setWays(city, new long[]{11L, 22L});
        assertEquals(2, save.ways(city).length);
        assertEquals(22L, save.ways(city)[1]);
        assertEquals(0, save.ways(other).length, "another city has its own");
        save.finish(city);
        assertEquals(2, save.ways(city).length, "finishing keeps them");
        assertTrue(save.forget(city));
        assertEquals(0, save.ways(city).length, "a reset forgets them with the city");
    }
}
