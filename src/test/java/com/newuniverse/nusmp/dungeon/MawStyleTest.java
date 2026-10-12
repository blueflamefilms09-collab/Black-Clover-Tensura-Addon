package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawShapes.Kind;
import com.newuniverse.nusmp.dungeon.MawShapes.Style;
import com.newuniverse.nusmp.dungeon.MawStyle.Family;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Which manifestation a place and a biome give, and how worn it is: stable, spread by the family's odds, never the hole. */
class MawStyleTest {
    @Test
    void biomesAreReadByTheWordsOfTheirNames() {
        assertEquals(Family.PLAIN, MawStyle.family("plains"));
        assertEquals(Family.PLAIN, MawStyle.family("sunflower_plains"));
        assertEquals(Family.PLAIN, MawStyle.family("river"));
        assertEquals(Family.PLAIN, MawStyle.family("deep_ocean"));
        assertEquals(Family.PLAIN, MawStyle.family("the_end"));
        assertEquals(Family.PLAIN, MawStyle.family(""));
        assertEquals(Family.PLAIN, MawStyle.family(null));
        assertEquals(Family.FOREST, MawStyle.family("dark_forest"));
        assertEquals(Family.FOREST, MawStyle.family("old_growth_pine_taiga"));
        assertEquals(Family.FOREST, MawStyle.family("cherry_grove"), "a cherry grove is not the snowy one");
        assertEquals(Family.WET, MawStyle.family("swamp"));
        assertEquals(Family.WET, MawStyle.family("mangrove_swamp"));
        assertEquals(Family.WET, MawStyle.family("bamboo_jungle"));
        assertEquals(Family.COLD, MawStyle.family("snowy_plains"));
        assertEquals(Family.COLD, MawStyle.family("snowy_taiga"));
        assertEquals(Family.COLD, MawStyle.family("frozen_peaks"));
        assertEquals(Family.COLD, MawStyle.family("ice_spikes"));
        assertEquals(Family.COLD, MawStyle.family("grove"));
        assertEquals(Family.ARID, MawStyle.family("desert"));
        assertEquals(Family.ARID, MawStyle.family("wooded_badlands"));
        assertEquals(Family.ARID, MawStyle.family("windswept_savanna"));
        assertEquals(Family.HIGH, MawStyle.family("windswept_hills"));
        assertEquals(Family.HIGH, MawStyle.family("jagged_peaks"));
        assertEquals(Family.HIGH, MawStyle.family("stony_shore"));
        assertEquals(Family.FOREST, MawStyle.family("DARK_FOREST"), "case does not matter");
        assertEquals(Family.PLAIN, MawStyle.family("modded:thing"), "an unknown name is plain");
    }

    @Test
    void everyFamilyHasOddsThatAddUpAndWearThatFits() {
        for (Family f : Family.values()) {
            int[] w = MawStyle.weights(f);
            assertEquals(3, w.length);
            assertEquals(100, w[0] + w[1] + w[2], f.name());
            for (int p : w) assertTrue(p > 0, "every shape stays possible everywhere: " + f);
            Style s = MawStyle.styleOf(f);
            assertTrue(s.mossy() >= 0 && s.cracked() >= 0 && s.mossy() + s.cracked() <= 100, f.name());
        }
        assertTrue(MawStyle.styleOf(Family.WET).mossy() > MawStyle.styleOf(Family.ARID).mossy(), "swamps are mossier than deserts");
        assertTrue(MawStyle.styleOf(Family.ARID).cracked() > MawStyle.styleOf(Family.WET).cracked(), "deserts crack more than swamps");
    }

    @Test
    void theSeedIsStableAndTellsPlacesAndBiomesApart() {
        assertEquals(MawStyle.seed(100, -250, "plains"), MawStyle.seed(100, -250, "plains"));
        assertNotEquals(MawStyle.seed(100, -250, "plains"), MawStyle.seed(101, -250, "plains"));
        assertNotEquals(MawStyle.seed(100, -250, "plains"), MawStyle.seed(100, -251, "plains"));
        assertNotEquals(MawStyle.seed(100, -250, "plains"), MawStyle.seed(100, -250, "desert"));
        assertNotEquals(MawStyle.seed(1, 2, "x"), MawStyle.seed(2, 1, "x"), "x and z are not interchangeable");
        assertEquals(MawStyle.seed(7, 9, null), MawStyle.seed(7, 9, null), "no biome is fine");
        assertEquals(MawStyle.choose(MawStyle.seed(5, 5, "plains"), Family.PLAIN), MawStyle.choose(MawStyle.seed(5, 5, "plains"), Family.PLAIN));
    }

    @Test
    void theHashFollowsTheOddsOfTheFamilyAndNeverPicksTheHole() {
        for (Family f : Family.values()) {
            Map<Kind, Integer> n = new EnumMap<>(Kind.class);
            int total = 0;
            for (int x = -150; x < 150; x++) {
                for (int z = -60; z < 60; z++) {
                    Kind k = MawStyle.choose(MawStyle.seed(x * 7, z * 5, f.name().toLowerCase(Locale.ROOT)), f);
                    assertNotEquals(Kind.HOLE, k);
                    n.merge(k, 1, Integer::sum);
                    total++;
                }
            }
            int[] w = MawStyle.weights(f);
            assertEquals(w[0] / 100.0, n.getOrDefault(Kind.TOWER, 0) / (double) total, 0.025, f + " towers");
            assertEquals(w[1] / 100.0, n.getOrDefault(Kind.RUINS, 0) / (double) total, 0.025, f + " ruins");
            assertEquals(w[2] / 100.0, n.getOrDefault(Kind.GATE, 0) / (double) total, 0.025, f + " gates");
        }
    }

    @Test
    void theBiomeChangesTheOutcomeForTheSameSpot() {
        int differ = 0;
        for (int i = 0; i < 400; i++) {
            Kind a = MawStyle.choose(MawStyle.seed(i * 13, i * 7, "plains"), Family.PLAIN), b = MawStyle.choose(MawStyle.seed(i * 13, i * 7, "desert"), Family.ARID);
            if (a != b) differ++;
        }
        assertTrue(differ > 100, "a desert and a plain are not the same coin: " + differ);
    }

    @Test
    void theRotationIsAQuarterTurnAndAllFourHappen() {
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 400; i++) {
            int r = MawStyle.rotation(MawStyle.seed(i, -i * 3, "plains"));
            assertTrue(r >= 0 && r <= 3);
            seen[r] = true;
        }
        for (boolean s : seen) assertTrue(s);
    }
}
