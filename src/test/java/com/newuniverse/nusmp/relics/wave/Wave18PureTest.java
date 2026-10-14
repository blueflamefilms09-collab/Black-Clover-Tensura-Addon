package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The maths of the wave 18 bags and charms: saves, fits, gilding, composting, feeding, the divide, the coin, the dial, the flood, the powders (docs/doc_wave18.md). */
class Wave18PureTest {
    @Test
    void sevenBagsAreKnownByIdAndNoBagGoesInsideAnother() {
        assertEquals(7, Wave18Pure.Bag.values().length);
        for (Wave18Pure.Bag b : Wave18Pure.Bag.values()) {
            assertSame(b, Wave18Pure.Bag.byId(b.id()));
            assertTrue(Wave18Pure.Bag.isBag(b.id()));
            assertEquals(b.rows() * 9, b.slots());
        }
        assertNull(Wave18Pure.Bag.byId("coin_of_fortune"));
        assertNull(Wave18Pure.Bag.byId(null));
        assertTrue(Wave18Pure.Bag.isBag("bag_of_kittens"), "the kittens' bag is a bag too");
        assertFalse(Wave18Pure.Bag.isBag("dragon_satchel"));
        assertEquals(1, Wave18Pure.Bag.GOLDING.rows(), "a pound of coins, a single row");
        assertEquals(Wave18Pure.Hazard.GULP, Wave18Pure.Bag.DEVOURING.hazard());
        assertEquals(Wave18Pure.Hazard.NONE, Wave18Pure.Bag.HOLDING.hazard());
    }

    @Test
    void theSaveIsFiftyFiveToFortyBracingHalvesItWithAFloor() {
        assertEquals(0.55, Wave18Pure.failChance(0.0), 1e-9);
        assertEquals(0.275, Wave18Pure.failChance(1.0), 1e-9);
        assertEquals(0.55, Wave18Pure.failChance(-3.0), 1e-9, "negative bracing counts as none");
        assertEquals(0.275, Wave18Pure.failChance(9.0), 1e-9, "more than full bracing counts as full");
        assertTrue(Wave18Pure.failsSave(0.10, 0.0));
        assertFalse(Wave18Pure.failsSave(0.60, 0.0));
        assertFalse(Wave18Pure.failsSave(0.40, 1.0));
    }

    @Test
    void aFoeFitsTheMouthOnlyIfSmallAndSoft() {
        assertTrue(Wave18Pure.fitsBag(0.6, 1.95, 20), "a zombie");
        assertFalse(Wave18Pure.fitsBag(1.9, 1.9, 40), "a ravager is too wide");
        assertFalse(Wave18Pure.fitsBag(0.6, 3.0, 20), "an enderman is too tall");
        assertFalse(Wave18Pure.fitsBag(0.6, 1.9, 300), "a boss is too tough");
    }

    @Test
    void theGulpIsFortyPercentWithinBounds() {
        assertEquals(8f, Wave18Pure.gulp(10), 1e-6);
        assertEquals(8f, Wave18Pure.gulp(20), 1e-6);
        assertEquals(12f, Wave18Pure.gulp(30), 1e-6);
        assertEquals(60f, Wave18Pure.gulp(1000), 1e-6);
        assertTrue(Wave18Pure.pulledIn(0.2, 0.0));
        assertFalse(Wave18Pure.pulledIn(0.6, 0.0));
        assertFalse(Wave18Pure.pulledIn(0.2, 0.9), "a foe that braces hard is rarely pulled in");
    }

    @Test
    void goldingGildsNuggetsAndCopperOnly() {
        assertEquals("minecraft:gold_nugget", Wave18Pure.gildedOf("minecraft:iron_nugget"));
        assertEquals("minecraft:gold_nugget", Wave18Pure.gildedOf("minecraft:copper_ingot"));
        assertNull(Wave18Pure.gildedOf("minecraft:diamond"));
        assertNull(Wave18Pure.gildedOf(null));
        assertEquals(1, Wave18Pure.gildedCount("minecraft:iron_nugget"));
        assertEquals(3, Wave18Pure.gildedCount("minecraft:copper_ingot"));
        assertTrue(Wave18Pure.coinLike("gold_coin"));
        assertTrue(Wave18Pure.coinLike("tin_ingot"));
        assertFalse(Wave18Pure.coinLike("apple"));
        assertFalse(Wave18Pure.coinLike(null));
    }

    @Test
    void moldingMakesOneBoneMealPerFiveLevels() {
        assertEquals(0, Wave18Pure.boneMealFor(4.9));
        assertEquals(1, Wave18Pure.boneMealFor(5.0));
        assertEquals(3, Wave18Pure.boneMealFor(17.0));
        assertEquals(0, Wave18Pure.boneMealFor(-2));
    }

    @Test
    void feedingIsCappedPerDay() {
        assertEquals(1.0, Wave18Pure.feedPercent(4, 0), 1e-9);
        assertEquals(0.5, Wave18Pure.feedPercent(10, 28), 1e-9, "only two items of the day's thirty are left");
        assertEquals(0.0, Wave18Pure.feedPercent(10, 30), 1e-9);
        assertEquals(0.0, Wave18Pure.feedPercent(-5, 0), 1e-9);
        assertEquals(2, Wave18Pure.feedCounted(10, 28));
        assertEquals(7.5, Wave18Pure.feedPercent(500, 0), 1e-9, "a day's feeding never passes 7.5% of the pool");
        assertTrue(Wave18Pure.dayPassed(3, 4));
        assertFalse(Wave18Pure.dayPassed(4, 4));
        assertFalse(Wave18Pure.dayPassed(5, 4));
    }

    @Test
    void thePartnerTakesHalfButNeverTheirLastHealth() {
        assertEquals(4f, Wave18Pure.partnerShare(8f, 20f), 1e-6);
        assertEquals(2f, Wave18Pure.partnerShare(8f, 3f), 1e-6, "three health: two can be taken");
        assertEquals(0f, Wave18Pure.partnerShare(8f, 1f), 1e-6);
        assertEquals(0f, Wave18Pure.partnerShare(8f, 0.5f), 1e-6);
        assertEquals(0f, Wave18Pure.partnerShare(-4f, 20f), 1e-6);
    }

    @Test
    void theCoinIsEvenForGoldAndBendsTheNextBlow() {
        int gold = 0;
        for (int d = 1; d <= 20; d++) if (Wave18Pure.goldFace(d)) gold++;
        assertEquals(10, gold, "ten even faces, ten odd");
        assertEquals(1.5, Wave18Pure.fortuneOut(1), 1e-9);
        assertEquals(0.6, Wave18Pure.fortuneOut(-1), 1e-9);
        assertEquals(1.0, Wave18Pure.fortuneOut(0), 1e-9);
        assertEquals(0.7, Wave18Pure.fortuneIn(1), 1e-9);
        assertEquals(1.3, Wave18Pure.fortuneIn(-1), 1e-9);
    }

    @Test
    void theDialShowsFireFrostLightningAndStartsOver() {
        assertEquals(0, Wave18Pure.dialType(0));
        assertEquals(1, Wave18Pure.dialType(1));
        assertEquals(2, Wave18Pure.dialType(2));
        assertEquals(0, Wave18Pure.dialType(3));
        assertEquals(2, Wave18Pure.dialType(-1));
        assertEquals("minecraft:is_fire", Wave18Pure.dialDamageTag(0));
        assertEquals("minecraft:is_freezing", Wave18Pure.dialDamageTag(1));
        assertEquals("minecraft:is_lightning", Wave18Pure.dialDamageTag(2));
        assertEquals("frost", Wave18Pure.dialName(4));
    }

    @Test
    void kittensStartAtAThousandAndTheFloodGrowsToASizeSetByThem() {
        assertEquals(1000, Wave18Pure.kittensIn(null));
        assertEquals(0, Wave18Pure.kittensIn(-4));
        assertEquals(1000, Wave18Pure.kittensIn(5000));
        assertEquals(37, Wave18Pure.kittensIn(37));
        assertEquals(4.0, Wave18Pure.floodRadius(1000, 0), 1e-9);
        assertEquals(4.8, Wave18Pure.floodRadius(1000, 1), 1e-9);
        assertEquals(9.0, Wave18Pure.floodRadius(1000, 50), 1e-9);
        assertTrue(Wave18Pure.floodRadius(10, 50) < 5.0, "a nearly empty bag floods a small circle");
        double prev = 0;
        for (int i = 0; i < 12; i++) {
            double r = Wave18Pure.floodRadius(1000, i);
            assertTrue(r >= prev);
            prev = r;
        }
    }

    @Test
    void theFloodLastsThreeD6TurnsWithinTheGamesBounds() {
        assertEquals(3, Wave18Pure.floodPulses(n -> 0), "three ones");
        assertEquals(12, Wave18Pure.floodPulses(n -> n - 1), "three sixes (18) are held to twelve");
        assertEquals(9, Wave18Pure.floodPulses(n -> 2), "three threes");
    }

    @Test
    void powdersMapFromItemsAndDiceAreThreeFours() {
        assertEquals(1, Wave18Pure.powderOf("minecraft:charcoal"));
        assertEquals(2, Wave18Pure.powderOf("minecraft:red_sand"));
        assertEquals(3, Wave18Pure.powderOf("minecraft:short_grass"));
        assertEquals(4, Wave18Pure.powderOf("minecraft:snowball"));
        assertEquals(0, Wave18Pure.powderOf("minecraft:stone"));
        assertEquals(0, Wave18Pure.powderOf(null));
        assertEquals("minecraft:on_fire", Wave18Pure.powderDamageType(1));
        assertEquals("minecraft:lightning_bolt", Wave18Pure.powderDamageType(2));
        assertEquals("minecraft:freeze", Wave18Pure.powderDamageType(4));
        assertEquals("ash", Wave18Pure.powderName(1));
        assertEquals(3, Wave18Pure.dice(3, 4, n -> 0));
        assertEquals(12, Wave18Pure.dice(3, 4, n -> n - 1));
        assertEquals(12, Wave18Pure.dice(3, 4, n -> 99), "a roller that lies is clamped to the die");
    }

    @Test
    void theConeIsInFrontAndShort() {
        assertTrue(Wave18Pure.inCone(0, 3, 0, 1, 5, 0.82), "straight ahead");
        assertTrue(Wave18Pure.inCone(1, 3, 0, 1, 5, 0.82), "a little to the side");
        assertFalse(Wave18Pure.inCone(3, 1, 0, 1, 5, 0.82), "off to the side");
        assertFalse(Wave18Pure.inCone(0, -3, 0, 1, 5, 0.82), "behind");
        assertFalse(Wave18Pure.inCone(0, 6, 0, 1, 5, 0.82), "out of range");
        assertTrue(Wave18Pure.inCone(0, 0, 0, 1, 5, 0.82), "on top of the thrower");
        assertFalse(Wave18Pure.inCone(0, 3, 0, 0, 5, 0.82), "no direction, no cone");
    }
}
