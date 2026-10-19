package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The thirty-six weapon rows (the twelve of 1.34.0, buffed in 1.93.0, and the twenty-four added then) and the numbers derived from them. Four bounds were
 * widened on purpose by the buff: melee damage (14 to 15), act power (13 to 15), act reach (32 to 35) and the shortest cooldown (60 to 50 ticks).
 */
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
            assertTrue(s.damage() >= 5 && s.damage() <= 15, d.name() + " damage");
            assertTrue(s.speed() >= 0.8 && s.speed() <= 2.6, d.name() + " speed");
            assertTrue(s.reach() >= 0 && s.reach() <= 1.5, d.name() + " reach");
            for (int a = 0; a < 2; a++) {
                DjinnWeaponRules.Act act = s.act(a);
                assertTrue(keys.add(act.key()), "unique act key " + act.key());
                assertTrue(act.cost() > 0 && act.cost() <= 0.06, d.name() + " cost " + act.key());
                assertTrue(act.cooldown() >= 50 && act.cooldown() <= 300, d.name() + " cooldown " + act.key());
                assertTrue(act.power() >= 5 && act.power() <= 15, d.name() + " power " + act.key());
                assertTrue(act.range() >= 4 && act.range() <= 35, d.name() + " range " + act.key());
            }
            assertTrue(s.two().cost() >= s.one().cost() * 0.9, d.name() + ": the second act is no cheaper than the first");
            assertTrue(s.two().cooldown() >= s.one().cooldown(), d.name() + ": the second act waits at least as long");
        }
        assertEquals(72, keys.size(), "two acts for each of the thirty-six");
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
        assertEquals(10.5, half, 1e-9);
        assertEquals(10.5 * 1.35, full, 1e-9);
        assertEquals(10.5 * 1.20, dark, 1e-9);
        assertEquals(10.5 * 1.35 * 1.20, DjinnWeaponRules.power(s, 0, 3, true), 1e-9);
        assertEquals(0.03, DjinnWeaponRules.cost(s, 0, false), 1e-12);
        assertEquals(0.03 * 1.25, DjinnWeaponRules.cost(s, 0, true), 1e-12);
        assertEquals(108, DjinnWeaponRules.cooldown(s, 0, 2));
        assertEquals(86, DjinnWeaponRules.cooldown(s, 0, 3));
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

    /** The twenty-four acts of the original twelve weapons: hand-written in DjinnWeaponActs, never in ActLibrary. */
    private static final Set<String> ORIGINAL_ACTS = Set.of("thunder_throw", "judgement_step", "sun_cleave", "ember_wall", "tide_spear", "frost_surge", "cyclone_sweep", "updraft_leap",
            "bloom_heal", "thorn_snare", "titan_slam", "rock_breaker", "resonance_wave", "dissonance", "ember_rush", "brand", "dawn_arrow", "radiant_burst", "chain_lightning",
            "storm_pillars", "crescent_wind", "heavenly_dash", "abyssal_reap", "drowning_pull");

    @Test
    void seventyTwoWeaponIdsAndSeventyTwoActs() {
        int ids = 0;
        for (Djinn d : Djinn.values()) {
            assertNotNull(DjinnWeaponRules.spec(d), d.name());
            ids += 2;
        }
        assertEquals(72, ids, "thirty-six Djinn, a light and a dark weapon each");
        int i = 0;
        for (Djinn d : Djinn.values()) {
            for (int a = 0; a < 2; a++) {
                String key = DjinnWeaponRules.spec(d).act(a).key();
                boolean original = i < 12;
                assertEquals(original, ORIGINAL_ACTS.contains(key), key + ": the first twelve weapons keep their hand-written acts, the others are ActLibrary's");
                assertEquals(!original, ActLibrary.has(key), key);
            }
            i++;
        }
        assertEquals(48, ActLibrary.keys().size());
    }

    @Test
    void everyGenericActIsUsedOnceAndSane() {
        Set<String> used = new HashSet<>();
        for (Djinn d : Djinn.values())
            for (int a = 0; a < 2; a++) {
                String key = DjinnWeaponRules.spec(d).act(a).key();
                ActLibrary.Gen g = ActLibrary.gen(key);
                if (g == null) continue;
                assertTrue(used.add(key), key);
                assertNotNull(g.kind(), key);
                assertTrue(g.mult() >= 0.4 && g.mult() <= 1.2, key + " mult");
                assertTrue(g.push() >= 0 && g.push() <= 1.5 && g.lift() >= 0 && g.lift() <= 1.0, key + " shove");
                assertTrue(g.fire() >= 0 && g.fire() <= 200, key + " fire");
                assertTrue(g.leech() >= 0 && g.leech() <= 0.3, key + " leech");
                for (ActLibrary.Eff e : new ActLibrary.Eff[]{g.eff1(), g.eff2(), g.self()}) {
                    assertNotNull(e, key);
                    assertTrue(e.ticks() >= 0 && e.ticks() <= 200 && e.amp() >= 0 && e.amp() <= 4, key + " effect " + e);
                    if (!e.none() && !Set.of("silence", "freeze", "cleanse", "unveil").contains(e.id())) assertTrue(e.id().matches("(minecraft|tensura):[a-z_]+"), e.id());
                }
                switch (g.kind()) {
                    case BEAM -> assertTrue(g.count() >= 1 && g.count() <= 3 && g.arc() >= 0.5 && g.arc() <= 1.5, key);
                    case CONE -> assertTrue(g.arc() >= 30 && g.arc() <= 90, key);
                    case WALL -> assertTrue(g.arc() >= 2 && g.arc() <= 6, key);
                    case CHAIN -> assertTrue(g.count() >= 2 && g.count() <= 5, key);
                    case PILLARS -> assertTrue(g.count() >= 1 && g.count() <= 3, key + ": three marks, two casts in the air stay inside the bearer's six");
                    case HEAL -> assertTrue(g.heal() > 0 && g.heal() <= 1.0, key);
                    case SHIELD -> assertTrue(g.heal() > 0 && g.heal() <= 8.0, key + ": Absorption points, at most four hearts");
                    default -> { }
                }
            }
        assertEquals(ActLibrary.keys(), used, "every ActLibrary act belongs to a weapon row");
    }

    @Test
    void theTwelveAreBuffedAndNeverWeaker() {
        // damage, then (power, cooldown, range) of act one and act two, as they were in 1.34.0
        Object[][] old = {
                {Djinn.BAAL, 9.0, 9.0, 120, 20.0, 11.0, 260, 8.0}, {Djinn.AMON, 12.0, 11.0, 110, 6.0, 8.0, 280, 7.0}, {Djinn.VINEA, 9.0, 8.0, 100, 11.0, 9.0, 260, 7.0},
                {Djinn.FOCALOR, 8.0, 8.0, 110, 4.5, 6.0, 220, 5.0}, {Djinn.ZAGAN, 6.0, 6.0, 180, 9.0, 7.0, 240, 12.0}, {Djinn.ZEPAR, 13.0, 12.0, 160, 5.5, 12.0, 240, 4.5},
                {Djinn.FURFUR, 7.0, 7.0, 120, 9.0, 6.0, 260, 6.5}, {Djinn.MARCHOSIAS, 7.5, 8.0, 120, 8.0, 5.0, 200, 14.0}, {Djinn.SITRI, 6.0, 10.0, 60, 32.0, 8.0, 260, 6.5},
                {Djinn.BIFRON, 9.0, 8.0, 120, 12.0, 10.0, 260, 14.0}, {Djinn.PAIMON, 9.0, 8.0, 110, 10.0, 9.0, 220, 9.0}, {Djinn.VASSAGO, 10.0, 9.0, 140, 5.5, 6.0, 260, 9.0}};
        for (Object[] o : old) {
            DjinnWeaponRules.Spec s = DjinnWeaponRules.spec((Djinn) o[0]);
            assertTrue(s.damage() > (Double) o[1], o[0] + " melee damage grew");
            assertTrue(s.one().power() > (Double) o[2] && s.one().cooldown() < (Integer) o[3] && s.one().range() > (Double) o[4], o[0] + " act one is better");
            assertTrue(s.two().power() > (Double) o[5] && s.two().cooldown() < (Integer) o[6] && s.two().range() > (Double) o[7], o[0] + " act two is better");
            assertTrue(s.one().power() <= (Double) o[2] * 1.2 && s.two().power() <= (Double) o[5] * 1.2, o[0] + ": by no more than a fifth");
        }
    }

    @Test
    void theWeaponsOfTheNewDjinnAreDistinct() {
        Set<String> names = new HashSet<>();
        for (int i = 12; i < 36; i++) {
            Djinn d = Djinn.values()[i];
            DjinnWeaponRules.Spec s = DjinnWeaponRules.spec(d);
            assertFalse(s.kind().isBlank(), d.name());
            assertTrue(names.add(s.one().key() + "|" + s.two().key()), d.name());
            assertTrue(s.one().key().matches("[a-z_]+") && s.two().key().matches("[a-z_]+"), d.name());
            assertNotEquals(s.one().key(), s.two().key(), d.name());
        }
    }
}
