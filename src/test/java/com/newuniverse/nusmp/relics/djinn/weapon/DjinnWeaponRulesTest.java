package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The twelve weapon rows and the numbers derived from them. */
class DjinnWeaponRulesTest {
    /** The ids of Tensura's engravings (scratchpad/tensura_ids/clean.json, the enchantments); a typo here would be skipped silently in game. */
    private static final Set<String> TENSURA_ENGRAVINGS = Set.of("barrier_piercing", "breathing_support", "crushing", "dead_end_rainbow", "elemental_boost", "elemental_resistance",
            "energy_protection", "energy_steal", "enervation", "growth", "holy_coat", "holy_weapon", "intangibility", "lethargy", "magic_capacity", "magic_interference",
            "magic_protection", "magic_weapon", "magicule_absorption", "restoration", "ruination", "sealing", "severance", "severance_protection", "slotting", "soul_eater",
            "spiritual_protection", "stagnation", "sturdy", "swift", "transcendence", "tsukumogami", "vigor", "vitality");

    @Test
    void everyDjinnHasARowWithSaneNumbers() {
        Set<String> keys = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            DjinnWeaponRules.Spec s = DjinnWeaponRules.spec(d);
            assertNotNull(s, d.name());
            assertSame(d, s.djinn());
            assertTrue(s.damage() >= 5 && s.damage() <= 14, d.name() + " damage");
            assertTrue(s.speed() >= 0.8 && s.speed() <= 2.6, d.name() + " speed");
            assertTrue(s.reach() >= 0 && s.reach() <= 1.5, d.name() + " reach");
            for (int a = 0; a < 2; a++) {
                DjinnWeaponRules.Act act = s.act(a);
                assertTrue(keys.add(act.key()), "unique act key " + act.key());
                assertTrue(act.cost() > 0 && act.cost() <= 0.06, d.name() + " cost " + act.key());
                assertTrue(act.cooldown() >= 60 && act.cooldown() <= 300, d.name() + " cooldown " + act.key());
                assertTrue(act.power() >= 5 && act.power() <= 13, d.name() + " power " + act.key());
                assertTrue(act.range() >= 4 && act.range() <= 32, d.name() + " range " + act.key());
            }
            assertTrue(s.two().cost() >= s.one().cost() * 0.9, d.name() + ": the second act is no cheaper than the first");
            assertTrue(s.two().cooldown() >= s.one().cooldown(), d.name() + ": the second act waits at least as long");
        }
        assertEquals(24, keys.size());
    }

    @Test
    void theHalfEquipOpensTheFirstActAndTheFullEquipBoth() {
        assertFalse(DjinnWeaponRules.open(0, 1));
        assertTrue(DjinnWeaponRules.open(0, 2));
        assertFalse(DjinnWeaponRules.open(1, 2));
        assertTrue(DjinnWeaponRules.open(1, 3));
        assertTrue(DjinnWeaponRules.open(1, 4));
    }

    @Test
    void fullAndDarkMakeItStrongerAndDarkDearer() {
        DjinnWeaponRules.Spec s = DjinnWeaponRules.spec(Djinn.BAAL);
        double half = DjinnWeaponRules.power(s, 0, 2, false), full = DjinnWeaponRules.power(s, 0, 3, false), dark = DjinnWeaponRules.power(s, 0, 2, true);
        assertEquals(9.0, half, 1e-9);
        assertEquals(9.0 * 1.35, full, 1e-9);
        assertEquals(9.0 * 1.20, dark, 1e-9);
        assertEquals(9.0 * 1.35 * 1.20, DjinnWeaponRules.power(s, 0, 3, true), 1e-9);
        assertEquals(0.03, DjinnWeaponRules.cost(s, 0, false), 1e-12);
        assertEquals(0.03 * 1.25, DjinnWeaponRules.cost(s, 0, true), 1e-12);
        assertEquals(120, DjinnWeaponRules.cooldown(s, 0, 2));
        assertEquals(96, DjinnWeaponRules.cooldown(s, 0, 3));
    }

    @Test
    void theCooldownTableNeverGoesBelowATwentieth() {
        for (Djinn d : Djinn.values())
            for (int a = 0; a < 2; a++)
                for (int phase = 2; phase <= 4; phase++) {
                    int c = DjinnWeaponRules.cooldown(DjinnWeaponRules.spec(d), a, phase);
                    assertTrue(c >= 20 && c <= ActClock.MAX_COOLDOWN, d + " " + a + " " + phase);
                }
    }

    @Test
    void aFullEquipActNeverCostsMoreThanTheDjinnsOwnEntry() {
        for (Djinn d : Djinn.values())
            for (int a = 0; a < 2; a++)
                assertTrue(DjinnWeaponRules.cost(DjinnWeaponRules.spec(d), a, true) < 0.2, d + ": an act is cheaper than the Full Equip's entry price");
    }

    @Test
    void idsRoundTrip() {
        for (Djinn d : Djinn.values()) {
            String light = DjinnWeaponRules.itemPath(d, false), dark = DjinnWeaponRules.itemPath(d, true);
            assertEquals("djinn_weapon_" + d.id(), light);
            assertEquals(light + "_dark", dark);
            assertSame(d, DjinnWeaponRules.djinnOfPath(light));
            assertSame(d, DjinnWeaponRules.djinnOfPath(dark));
            assertFalse(DjinnWeaponRules.isDarkPath(light));
            assertTrue(DjinnWeaponRules.isDarkPath(dark));
        }
        assertNull(DjinnWeaponRules.djinnOfPath("djinn_weapon_nobody"));
        assertNull(DjinnWeaponRules.djinnOfPath("djinn_seal_baal"));
        assertNull(DjinnWeaponRules.djinnOfPath(null));
        assertFalse(DjinnWeaponRules.isDarkPath("djinn_weapon_nobody_dark"));
    }

    @Test
    void everyEngravingIsATensuraOneAndEveryWeaponHasSome() {
        for (Djinn d : Djinn.values())
            for (boolean dark : new boolean[]{false, true}) {
                var list = DjinnWeaponRules.engravings(d, dark);
                assertTrue(list.size() >= 3, d + " has its engravings");
                Set<String> seen = new HashSet<>();
                for (DjinnWeaponRules.Engraving e : list) {
                    assertTrue(TENSURA_ENGRAVINGS.contains(e.id()), e.id());
                    assertTrue(e.level() >= 1 && e.level() <= 3);
                    assertTrue(seen.add(e.id()), "no engraving twice: " + e.id());
                }
                assertEquals(dark, seen.contains("ruination"));
            }
    }
}
