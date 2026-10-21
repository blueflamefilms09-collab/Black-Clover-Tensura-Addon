package com.newuniverse.nusmp.labyrinth.loot;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which armour piece a chest or a kill hands out, how good it is and that the roll of its lines is randomised and reproducible. */
class LuckArmourTest {
    private static final LootRoster ROSTER = LootRoster.parse(new InputStreamReader(LuckArmourTest.class.getResourceAsStream(LootRoster.RESOURCE), StandardCharsets.UTF_8));

    @Test
    void aShallowFloorNeverHoldsASetThatStartsDeeper() {
        for (int band = 0; band < LootBand.BANDS; band++) {
            for (String id : LuckArmour.candidates(ROSTER, band)) {
                LootRoster.ArmourDef a = ROSTER.armourPiece(id);
                assertNotNull(a, id);
                assertTrue(ROSTER.armourSets().get(a.set()).minBand() <= band || band == 0, id + " is offered at band " + band);
            }
        }
        assertFalse(LuckArmour.candidates(ROSTER, 0).isEmpty(), "the first band always has something");
        assertTrue(LuckArmour.candidates(ROSTER, 4).size() >= LuckArmour.candidates(ROSTER, 0).size(), "a deeper floor offers at least as many pieces");
    }

    @Test
    void thePickIsReproducibleAndDifferentChestsGiveDifferentSeeds() {
        LuckArmour.Pick a = LuckArmour.pick(ROSTER, 120, 5.0, false, new Random(77)::nextDouble);
        LuckArmour.Pick b = LuckArmour.pick(ROSTER, 120, 5.0, false, new Random(77)::nextDouble);
        assertEquals(a, b, "the same dice gives the same piece");
        Random rnd = new Random(1);
        Set<Long> seeds = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            LuckArmour.Pick p = LuckArmour.pick(ROSTER, 120, 5.0, false, rnd::nextDouble);
            assertNotNull(p);
            seeds.add(p.seed());
            ids.add(p.id());
        }
        assertTrue(seeds.size() > 190, "the seeds differ: " + seeds.size());
        assertTrue(ids.size() > 1, "more than one piece is handed out");
    }

    @Test
    void luckAndBossesRaiseTheRarity() {
        double[] mean = new double[3];
        int n = 6000;
        for (int k = 0; k < 3; k++) {
            Random rnd = new Random(100 + k);
            double luck = k == 0 ? 0.0 : 20.0;
            boolean boss = k == 2;
            for (int i = 0; i < n; i++) mean[k] += LuckArmour.pick(ROSTER, 40, luck, boss, rnd::nextDouble).rarity().ordinal();
            mean[k] /= n;
        }
        assertTrue(mean[1] > mean[0], "Luck raises the rarity: " + mean[0] + " vs " + mean[1]);
        assertTrue(mean[2] >= mean[1], "a boss counts a band deeper: " + mean[1] + " vs " + mean[2]);
    }

    @Test
    void theRollOfAPickedPieceIsRandomisedAndReproducibleFromItsSeed() {
        Random rnd = new Random(2);
        Set<String> distinct = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            LuckArmour.Pick p = LuckArmour.pick(ROSTER, 200, 10.0, false, rnd::nextDouble);
            LootRoster.ArmourDef def = ROSTER.armourPiece(p.id());
            GearRoll r1 = GearRollMath.roll(p.seed(), 200, p.rarity(), GearKind.ARMOUR, StatKind.ARMOR, 0.0);
            GearRoll r2 = GearRollMath.roll(p.seed(), 200, p.rarity(), GearKind.ARMOUR, StatKind.ARMOR, 0.0);
            assertEquals(r1, r2, "the same seed, the same roll: " + def.id());
            assertTrue(r1.lines().size() >= 2, "an armour piece rolls at least two lines");
            assertEquals(StatKind.ARMOR, r1.lines().get(0).kind(), "armour is the first line of a piece of armour");
            distinct.add(r1.lines().toString());
        }
        assertTrue(distinct.size() > 60, "the rolls differ from piece to piece: " + distinct.size());
        List<String> none = LuckArmour.candidates(LootRoster.parse(new java.io.StringReader("{}")), 3);
        assertTrue(none.isEmpty(), "no roster, no armour");
    }
}
