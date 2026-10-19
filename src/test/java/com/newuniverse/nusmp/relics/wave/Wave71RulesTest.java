package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the passive of every weapon of wave 71 is a {@link Wave71Rules.Rule}; its arithmetic and the table of the 35 rules are pure and checked here. */
class Wave71RulesTest {
    private static Wave71Rules.Facts none() {
        return new Wave71Rules.Facts(false, false, false, false, false, false, false, false, false, false, false, false, false, false);
    }

    private static Wave71Rules.Facts only(Wave71Rules.Cond c) {
        boolean t = true, f = false;
        return switch (c) {
            case ALWAYS -> none();
            case LOW_HP_FOE -> new Wave71Rules.Facts(t, f, f, f, f, f, f, f, f, f, f, f, f, f);
            case UNDEAD_FIEND -> new Wave71Rules.Facts(f, t, f, f, f, f, f, f, f, f, f, f, f, f);
            case WYRM -> new Wave71Rules.Facts(f, f, t, f, f, f, f, f, f, f, f, f, f, f);
            case FLYING_FOE -> new Wave71Rules.Facts(f, f, f, t, f, f, f, f, f, f, f, f, f, f);
            case WET_FOE -> new Wave71Rules.Facts(f, f, f, f, t, f, f, f, f, f, f, f, f, f);
            case FIRE_FOE -> new Wave71Rules.Facts(f, f, f, f, f, t, f, f, f, f, f, f, f, f);
            case DAYLIGHT -> new Wave71Rules.Facts(f, f, f, f, f, f, t, f, f, f, f, f, f, f);
            case NIGHT -> new Wave71Rules.Facts(f, f, f, f, f, f, f, t, f, f, f, f, f, f);
            case FULL_HP_SELF -> new Wave71Rules.Facts(f, f, f, f, f, f, f, f, t, f, f, f, f, f);
            case LOW_HP_SELF -> new Wave71Rules.Facts(f, f, f, f, f, f, f, f, f, t, f, f, f, f);
            case BOSS -> new Wave71Rules.Facts(f, f, f, f, f, f, f, f, f, f, t, f, f, f);
            case SNEAKING -> new Wave71Rules.Facts(f, f, f, f, f, f, f, f, f, f, f, t, f, f);
            case FALLING -> new Wave71Rules.Facts(f, f, f, f, f, f, f, f, f, f, f, f, t, f);
            case BIG_FOE -> new Wave71Rules.Facts(f, f, f, f, f, f, f, f, f, f, f, f, f, t);
        };
    }

    @Test
    void everyConditionHoldsExactlyWhenItsFactIsSet() {
        for (Wave71Rules.Cond c : Wave71Rules.Cond.values()) {
            assertTrue(Wave71Rules.applies(c, only(c)), c + " holds on its own fact");
            if (c != Wave71Rules.Cond.ALWAYS) assertFalse(Wave71Rules.applies(c, none()), c + " does not hold on nothing");
        }
    }

    @Test
    void theBonusIsAddedOnlyWhereTheConditionHoldsAndTheChanceRollIsUnderTheChance() {
        Wave71Rules.Rule r = Wave71Rules.Rule.of(Wave71Rules.Cond.WYRM, 1.0);
        assertEquals(2.0, Wave71Rules.factor(r, only(Wave71Rules.Cond.WYRM), 0.5), 1e-9);
        assertEquals(1.0, Wave71Rules.factor(r, none(), 0.0), 1e-9);
        Wave71Rules.Rule lucky = Wave71Rules.Rule.of(Wave71Rules.Cond.ALWAYS, 0.5).chance(0.15);
        assertEquals(1.5, Wave71Rules.factor(lucky, none(), 0.10), 1e-9);
        assertEquals(1.0, Wave71Rules.factor(lucky, none(), 0.20), 1e-9);
        assertEquals(1.0, Wave71Rules.factor(null, none(), 0.0), 1e-9);
        assertTrue(Wave71Rules.lands(lucky, none(), 0.10));
        assertFalse(Wave71Rules.lands(lucky, none(), 0.90));
        assertFalse(Wave71Rules.lands(r, none(), 0.0), "a status that needs a dragon does not land on a goat");
    }

    @Test
    void lifeStolenIsAShareOfTheBlowAndIsCapped() {
        Wave71Rules.Rule r = Wave71Rules.Rule.of(Wave71Rules.Cond.ALWAYS, 0.2).steal(0.08);
        assertEquals(8.0f, Wave71Rules.steal(r, 100f), 1e-4);
        assertEquals(Wave71Rules.MAX_STEAL, Wave71Rules.steal(r, 100000f), 1e-4);
        assertEquals(0f, Wave71Rules.steal(Wave71Rules.Rule.of(Wave71Rules.Cond.ALWAYS, 0.1), 500f), 1e-9);
        assertEquals(0f, Wave71Rules.steal(r, -5f), 1e-9);
    }

    @Test
    void creatureKindsAreReadFromTheEntityPath() {
        for (String p : new String[]{"ender_dragon", "wyvern", "sky_drake", "wyrm_king", "serpent", "hydra", "naga_guard"}) assertTrue(Wave71Rules.isWyrm(p), p);
        for (String p : new String[]{"cow", "zombie", "pig"}) assertFalse(Wave71Rules.isWyrm(p), p);
        for (String p : new String[]{"greater_demon", "devil_x", "imp", "fire_imp", "wraith", "lich"}) assertTrue(Wave71Rules.isFiend(p), p);
        assertFalse(Wave71Rules.isFiend("chimpanzee"));
        assertTrue(Wave71Rules.isRangedId("silver_eagles_galewing_bow"));
        assertTrue(Wave71Rules.isRangedId("dawnpiercer_crossbow"));
        assertFalse(Wave71Rules.isRangedId("vorpal_sword"));
    }

    @Test
    void theTableHasOneSaneRuleForEveryWeaponAndNoTwoWeaponsShareOne() {
        Map<String, Wave71Rules.Rule> rules = Wave71Passives.RULES;
        assertEquals(35, rules.size());
        List<String> bad = new ArrayList<>();
        java.util.Set<Wave71Rules.Rule> seen = new java.util.HashSet<>();
        for (Map.Entry<String, Wave71Rules.Rule> e : rules.entrySet()) {
            Wave71Rules.Rule r = e.getValue();
            if (r.bonus() < 0.0 || r.bonus() > 1.5) bad.add(e.getKey() + " bonus " + r.bonus());
            if (r.chance() <= 0.0 || r.chance() > 1.0) bad.add(e.getKey() + " chance " + r.chance());
            if (r.lifesteal() < 0.0 || r.lifesteal() > 0.1) bad.add(e.getKey() + " lifesteal " + r.lifesteal());
            if (!r.effect().isEmpty() && (!r.effect().startsWith("minecraft:") || r.secs() <= 0 || r.secs() > 30 || r.amp() < 0 || r.amp() > 4)) bad.add(e.getKey() + " effect " + r.effect());
            if (r.ignite() < 0 || r.ignite() > 10) bad.add(e.getKey() + " ignite " + r.ignite());
            if (r.bonus() == 0.0 && r.effect().isEmpty() && r.lifesteal() == 0.0 && r.ignite() == 0) bad.add(e.getKey() + " does nothing");
            if (!seen.add(r)) bad.add(e.getKey() + " has the same rule as another weapon");
        }
        assertEquals(List.of(), bad);
    }
}
