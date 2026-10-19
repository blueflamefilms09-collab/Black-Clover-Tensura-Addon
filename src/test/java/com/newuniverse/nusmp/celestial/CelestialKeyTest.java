package com.newuniverse.nusmp.celestial;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The table of keys: the twelve Gold Zodiac keys are exactly the twelve of the brief, unique and in order; the order is the save format; every spirit has numbers that make sense. */
class CelestialKeyTest {
    private static final List<String> GOLD = List.of("aquarius", "taurus", "leo", "virgo", "sagittarius", "gemini", "cancer", "scorpio", "capricorn", "pisces", "aries", "libra");

    @Test
    void twelveGoldKeysAreUniqueAndInTheOrderOfTheBrief() {
        List<CelestialKey> gold = CelestialKey.gold();
        assertEquals(12, gold.size());
        for (int i = 0; i < 12; i++) {
            assertEquals(GOLD.get(i), gold.get(i).id, "gold key " + i);
            assertEquals(i, gold.get(i).ordinal(), "the gold keys are the first twelve ordinals (the save format)");
            assertTrue(gold.get(i).isGold());
        }
        assertEquals(12, new HashSet<>(GOLD).size());
    }

    @Test
    void theSilverKeysAndTheGateKeyFollowTheGold() {
        assertEquals(List.of("horologium", "crux", "lyra", "nikora"), CelestialKey.silver().stream().map(k -> k.id).toList());
        assertEquals(16, CelestialKey.SPIRIT_KING_GATE.ordinal());
        assertEquals(17, CelestialKey.values().length, "append-only: a new key goes at the end");
        assertFalse(CelestialKey.SPIRIT_KING_GATE.summons());
        assertEquals("spirit_king_gate_key", CelestialKey.SPIRIT_KING_GATE.itemId());
    }

    @Test
    void idsItemIdsAndBitsAreUnique() {
        Set<String> ids = new HashSet<>(), items = new HashSet<>(), names = new HashSet<>();
        int mask = 0;
        for (CelestialKey k : CelestialKey.values()) {
            assertTrue(ids.add(k.id), k.id);
            assertTrue(items.add(k.itemId()), k.itemId());
            assertTrue(names.add(k.spirit), k.spirit);
            assertTrue((mask & k.bit()) == 0, "bit of " + k.id);
            mask |= k.bit();
            assertTrue(k.id.matches("[a-z][a-z_]*"), k.id);
            assertSame(k, CelestialKey.byId(k.id));
            assertSame(k, CelestialKey.byId(k.itemId()));
            assertSame(k, CelestialKey.byOrdinal(k.ordinal()));
        }
        assertNull(CelestialKey.byId("nope"));
        assertNull(CelestialKey.byOrdinal(99));
        assertEquals(0xFFF, CelestialKey.GOLD_MASK);
    }

    @Test
    void everySpiritHasSensibleNumbersAndText() {
        for (CelestialKey k : CelestialKey.values()) {
            assertFalse(k.display.isBlank() || k.spirit.isBlank() || k.ability.isBlank() || k.abilityText.isBlank() || k.note.isBlank(), k.id);
            assertTrue(k.rgb > 0 && k.rgb <= 0xFFFFFF, k.id);
            if (!k.summons()) continue;
            assertTrue(k.costPermille > 0 && k.costPermille <= 150, k.id + " cost");
            assertTrue(k.cooldown >= 200 && k.cooldown <= 1200, k.id + " cooldown");
            assertTrue(k.stay >= 60 && k.stay <= 240, k.id + " stay");
            assertTrue(k.reach >= 4 && k.reach <= 30, k.id + " reach");
            assertTrue(k.power >= 0f && k.power <= 20f, k.id + " power");
            assertTrue(k.bossFactor >= 0f && k.bossFactor <= 1f, k.id + " boss factor");
        }
    }

    @Test
    void onlyTheGoldKeysHaveAStarDress() {
        for (CelestialKey k : CelestialKey.values()) {
            assertEquals(k.isGold(), k.hasStarDress(), k.id);
            if (k.hasStarDress()) assertTrue(k.dressName.endsWith("Form"), k.id);
        }
    }

    @Test
    void goldSpiritsAreStrongerAndCostMoreThanTheSilverOnes() {
        double goldPower = CelestialKey.gold().stream().mapToDouble(k -> k.power).max().orElse(0), silverPower = CelestialKey.silver().stream().mapToDouble(k -> k.power).max().orElse(0);
        assertTrue(goldPower > silverPower);
        double goldCost = CelestialKey.gold().stream().mapToInt(k -> k.costPermille).min().orElse(0), silverCost = CelestialKey.silver().stream().mapToInt(k -> k.costPermille).max().orElse(0);
        assertTrue(goldCost > silverCost, "the cheapest gold spirit costs more than the dearest silver one");
    }
}
