package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: where a form's number lands (the item or the player, added or as a share of the base) is the part of the loadout that is easiest to get subtly wrong and
 * cannot be seen from outside, so the plan is tested before any attribute is touched.
 */
class FormStatsTest {
    private static Map<String, Double> stats(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }

    private static HeroForm form(String id, Map<String, Double> st, Map<String, Double> bonus) {
        return new HeroForm(HeroKind.SHIELD, id, id, "", 1, List.of("base"), List.of(), 100, st, bonus, Map.of(), List.of(), "base", false, null, 0);
    }

    @Test
    void attackKeysGoOnTheStackAndTheRestOnThePlayer() {
        HeroForm f = form("a1", stats("attack_damage", 3, "attack_speed", 0.4, "armor", 2, "max_health", 4, "movement_speed", 0.04, "reach", 1.5, "luck", 1), Map.of());
        List<FormStats.Mod> mods = FormStats.formMods(f);
        assertEquals(7, mods.size());
        for (FormStats.Mod m : mods) {
            boolean attack = m.key().equals("attack_damage") || m.key().equals("attack_speed");
            assertEquals(attack ? FormStats.Target.STACK : FormStats.Target.PLAYER, m.target(), m.key());
            assertEquals(m.key().equals("movement_speed"), m.multiplied(), m.key() + " is a share of the base only for movement speed");
        }
        assertEquals("attack_damage", mods.get(0).key(), "in STAT_KEYS order");
        assertEquals(3.0, mods.get(0).amount(), 1e-9);
    }

    @Test
    void zerosAreLeftOut() {
        assertTrue(FormStats.formMods(form("a1", stats("armor", 0, "luck", 1e-12), Map.of())).isEmpty());
        assertTrue(FormStats.formMods(HeroForm.base(HeroKind.SHIELD)).isEmpty());
    }

    @Test
    void bonusesAddUpOverFinishedFormsOnly() {
        List<HeroForm> forms = List.of(form("a1", Map.of(), stats("armor", 0.5, "max_health", 1)), form("a2", Map.of(), stats("armor", 0.25, "luck", 0.5)),
                form("a3", Map.of(), stats("armor", 2)));
        Map<String, Double> sum = FormStats.bonusTotals(forms, Set.of("a1", "a2"));
        assertEquals(0.75, sum.get("armor"), 1e-9);
        assertEquals(1.0, sum.get("max_health"), 1e-9);
        assertEquals(0.5, sum.get("luck"), 1e-9);
        assertFalse(sum.containsKey("attack_damage"), "keys with nothing are absent");
        assertTrue(FormStats.bonusTotals(forms, Set.of()).isEmpty());
        assertEquals(2.0, FormStats.bonusTotals(forms, Set.of("a3")).get("armor"), 1e-9);
        assertTrue(FormStats.bonusTotals(forms, Set.of("ghost")).isEmpty(), "a finished form that no longer exists adds nothing");
    }

    @Test
    void bonusModsAreAllOnThePlayerAndMovementSpeedIsAShare() {
        List<HeroForm> forms = List.of(form("a1", Map.of(), stats("attack_damage", 0.5, "movement_speed", 0.02)));
        List<FormStats.Mod> mods = FormStats.bonusMods(forms, Set.of("a1"));
        assertEquals(2, mods.size());
        for (FormStats.Mod m : mods) assertEquals(FormStats.Target.PLAYER, m.target(), "even the attack bonus: it is permanent, not tied to the hand");
        assertTrue(mods.get(1).multiplied());
        assertFalse(mods.get(0).multiplied());
    }

    @Test
    void thePathsAreStable() {
        assertEquals("hero_form_armor", FormStats.path(FormStats.FORM_PREFIX, "armor"));
        assertEquals("hero_bonus_max_health", FormStats.path(FormStats.BONUS_PREFIX, "max_health"));
        assertTrue(FormStats.onStack("attack_speed") && !FormStats.onStack("armor") && FormStats.multiplied("movement_speed") && !FormStats.multiplied("reach"));
    }
}
