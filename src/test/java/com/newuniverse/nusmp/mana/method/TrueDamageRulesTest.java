package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.TrueDamageRules.Kind;
import com.newuniverse.nusmp.mana.method.TrueDamageRules.Route;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** How True Magic hurts: the physical route, the Anti-Magic answer, the damage type of each element, the resistance that is no longer undone. */
class TrueDamageRulesTest {
    @Test
    void normalRunesAlwaysTakeTheOldMagicRoute() {
        for (boolean phys : new boolean[]{false, true})
            for (boolean anti : new boolean[]{false, true})
                for (boolean user : new boolean[]{false, true})
                    assertEquals(Route.MAGIC_UNDONE, TrueDamageRules.route(false, phys, anti, user), "a normal rune is the grimoire's magic damage");
    }

    @Test
    void trueMagicIsPhysicalWhenTheSettingIsOn() {
        assertEquals(Route.PHYSICAL, TrueDamageRules.route(true, true, true, false));
        assertEquals(Route.PHYSICAL, TrueDamageRules.route(true, true, false, false));
        assertEquals(Route.PHYSICAL, TrueDamageRules.route(true, true, false, true), "with antiMagicStillCancels off, True Magic passes Anti-Magic");
    }

    @Test
    void antiMagicStillCancelsTrueMagicByDefault() {
        assertEquals(Route.MAGIC_UNDONE, TrueDamageRules.route(true, true, true, true), "an Anti-Magic user is hit by the magic route, which Black Form cancels");
    }

    @Test
    void withThePhysicalSettingOffTrueMagicIsTheOldSpell() {
        for (boolean anti : new boolean[]{false, true})
            for (boolean user : new boolean[]{false, true})
                assertEquals(Route.MAGIC_UNDONE, TrueDamageRules.route(true, false, anti, user));
    }

    @Test
    void everyElementHasAVanillaDamageType() {
        assertEquals(Kind.FIRE, TrueDamageRules.kindOf(RuneElement.FIRE));
        assertEquals(Kind.WIND, TrueDamageRules.kindOf(RuneElement.WIND));
        assertEquals(Kind.STONE, TrueDamageRules.kindOf(RuneElement.EARTH));
        assertEquals(Kind.LIGHTNING, TrueDamageRules.kindOf(RuneElement.LIGHTNING));
        assertEquals(Kind.KINETIC, TrueDamageRules.kindOf(RuneElement.WATER));
        assertEquals(Kind.KINETIC, TrueDamageRules.kindOf(RuneElement.PLANT));
        assertEquals(Kind.KINETIC, TrueDamageRules.kindOf(null));
        for (RuneElement e : RuneElement.values()) {
            String path = TrueDamageRules.kindOf(e).path;
            assertTrue(path.matches("[a-z_]+"), "a plain vanilla path: " + path);
            assertFalse(path.contains("magic") || path.contains("tensura"), "never a magic type: " + path);
        }
    }

    @Test
    void thePhysicalRouteHasNoResistanceToUndoAndTheOldOneKeepsItsCaps() {
        assertEquals(1.0, TrueDamageRules.resistanceFactor(Route.PHYSICAL, 0.7, false));
        assertEquals(1.0, TrueDamageRules.resistanceFactor(Route.PHYSICAL, 0.7, true));
        assertEquals(RuneRules.bypassFor(0.5, false), TrueDamageRules.resistanceFactor(Route.MAGIC_UNDONE, 0.5, false), 1e-12);
        assertEquals(RuneRules.bypassFor(0.9, true), TrueDamageRules.resistanceFactor(Route.MAGIC_UNDONE, 0.9, true), 1e-12);
        assertTrue(TrueDamageRules.resistanceFactor(Route.MAGIC_UNDONE, 5.0, false) <= RuneRules.BYPASS_MAX + 1e-12, "still capped");
        assertTrue(TrueDamageRules.resistanceFactor(Route.MAGIC_UNDONE, 5.0, true) <= RuneRules.BOSS_BYPASS_MAX + 1e-12, "a boss keeps its resistance");
    }
}
