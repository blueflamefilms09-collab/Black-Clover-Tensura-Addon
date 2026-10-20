package com.newuniverse.nusmp.requip;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the modes of the Equip Magic are appended after Dismiss and a saved mode index must never move; the table, the gates and the passives are pinned here. */
class RequipMagicRulesTest {
    @Test
    void theOldModesKeepTheirIndicesAndTheNewOnesComeAfterDismiss() {
        assertEquals(15, RequipMagicRules.BASE_MODES);
        assertEquals(RequipRules.MODES, RequipMagicRules.BASE_MODES);
        assertEquals(14, RequipRules.DISMISS_MODE);
        assertEquals(15, RequipMagicRules.Mode.ARMOURY.modeIndex());
        assertEquals(RequipMagicRules.BASE_MODES + RequipMagicRules.Mode.values().length, RequipMagicRules.MODES);
        assertNull(RequipMagicRules.byModeIndex(0));
        assertNull(RequipMagicRules.byModeIndex(RequipRules.DISMISS_MODE));
        assertNull(RequipMagicRules.byModeIndex(RequipMagicRules.MODES));
        assertNull(RequipMagicRules.byModeIndex(-1));
        for (RequipMagicRules.Mode m : RequipMagicRules.Mode.values()) assertEquals(m, RequipMagicRules.byModeIndex(m.modeIndex()));
    }

    /** Append only: this is the order the first release shipped. A reorder moves a saved mode index. */
    @Test
    void theOrderIsPinned() {
        String[] order = {"armoury", "loadouts", "blade", "barrage", "strike", "partial", "circle_sword", "blumenblatt", "titanias_rampage", "dance_of_swords", "sea_of_swords", "moonlight_cross", "dimension_cut"};
        RequipMagicRules.Mode[] modes = RequipMagicRules.Mode.values();
        assertTrue(modes.length >= order.length);
        for (int i = 0; i < order.length; i++) assertEquals(order[i], modes[i].id());
    }

    @Test
    void idsAreUniqueAndDoNotClashWithTheArmours() {
        Set<String> ids = new HashSet<>();
        for (Armour a : Armour.values()) ids.add(a.id());
        ids.add("dismiss");
        for (RequipMagicRules.Mode m : RequipMagicRules.Mode.values()) {
            assertTrue(ids.add(m.id()), "duplicate id " + m.id());
            assertFalse(m.english().isEmpty());
            assertEquals(m, RequipMagicRules.byId(m.id()));
        }
        assertNull(RequipMagicRules.byId("nothing"));
    }

    @Test
    void theGatesAreFairAndOpenUpWithMastery() {
        for (RequipMagicRules.Mode m : RequipMagicRules.Mode.values()) {
            assertTrue(m.unlock() >= 0 && m.unlock() <= 0.8, m.id());
            assertTrue(m.minEp() >= 0 && m.minEp() <= 300_000, m.id());
            assertTrue(m.cost() >= 0 && m.cost() <= 15, m.id());
            assertTrue(m.cooldown() >= 0 && m.cooldown() <= 800, m.id());
            // an operator is never locked; nobody is open before mastery and EP both reach the gate
            assertTrue(RequipMagicRules.unlocked(m, 0, 0, true));
            if (m.unlock() > 0) assertFalse(RequipMagicRules.unlocked(m, m.unlock() - 0.01, 10_000_000, false), m.id());
            if (m.minEp() > 0) assertFalse(RequipMagicRules.unlocked(m, 1.0, m.minEp() - 1, false), m.id());
            assertTrue(RequipMagicRules.unlocked(m, m.unlock(), m.minEp(), false), m.id());
        }
        // the Armoury is open from the first moment; the heavy finishers are not
        assertTrue(RequipMagicRules.unlocked(RequipMagicRules.Mode.ARMOURY, 0.0, 25_000, false));
        assertFalse(RequipMagicRules.unlocked(RequipMagicRules.Mode.SEA_OF_SWORDS, 0.3, 25_000, false));
        // the later the finisher, the more it costs and the longer it rests (the named ones are the dearer half)
        assertTrue(RequipMagicRules.Mode.SEA_OF_SWORDS.cost() > RequipMagicRules.Mode.CIRCLE_SWORD.cost());
        assertTrue(RequipMagicRules.Mode.SEA_OF_SWORDS.cooldown() > RequipMagicRules.Mode.CIRCLE_SWORD.cooldown());
    }

    @Test
    void theFinishersAreTheNamedTechniquesAndTheCut() {
        var finishers = RequipMagicRules.finishers();
        assertEquals(7, finishers.size());
        for (RequipMagicRules.Mode m : finishers) {
            assertTrue(m.isFinisher());
            assertTrue(m.range() > 0, m.id());
            if (m != RequipMagicRules.Mode.DIMENSION_CUT) assertTrue(m.raw() > 0, m.id());
            assertTrue(m.cooldown() >= 100, m.id() + " needs its own rest");
            assertTrue(m.cost() > 0, m.id());
        }
        assertFalse(RequipMagicRules.Mode.BARRAGE.isFinisher());
        assertFalse(RequipMagicRules.Mode.PARTIAL.isFinisher());
        // the names that are the techniques' names in Fairy Tail are marked; the descriptive and the owner's names are not
        assertTrue(RequipMagicRules.Mode.CIRCLE_SWORD.canon());
        assertTrue(RequipMagicRules.Mode.BLUMENBLATT.canon());
        assertFalse(RequipMagicRules.Mode.SEA_OF_SWORDS.canon());
        assertFalse(RequipMagicRules.Mode.DIMENSION_CUT.canon());
        assertNotNull(RequipMagicRules.Mode.MOONLIGHT_CROSS.english());
    }

    @Test
    void theReflexNeedsMasteryAndAPause() {
        int pause = RequipMagicRules.REFLEX_SECONDS * 20;
        assertEquals(400, pause);
        assertFalse(RequipMagicRules.reflexReady(true, 0.39, 10_000, 0, pause));
        assertTrue(RequipMagicRules.reflexReady(true, 0.40, 10_000, 0, pause));
        assertFalse(RequipMagicRules.reflexReady(false, 1.0, 10_000, 0, pause));                     // the config switch
        assertFalse(RequipMagicRules.reflexReady(true, 1.0, 10_399, 10_000, pause));                 // once per 20 s
        assertTrue(RequipMagicRules.reflexReady(true, 1.0, 10_400, 10_000, pause));
        assertTrue(RequipMagicRules.reflexReady(true, 1.0, 5, 5, 0));                                // a pause of 0 means every hit
    }

    @Test
    void aWeaponIsAnythingThatHitsLikeOne() {
        assertTrue(RequipMagicRules.isWeapon(7.0));
        assertTrue(RequipMagicRules.isWeapon(RequipMagicRules.WEAPON_MIN_DAMAGE));
        assertFalse(RequipMagicRules.isWeapon(1.0));                          // a bare hand, a block, a stick
        assertEquals(14.0, RequipMagicRules.strikeRange(0.0), 1e-9);
        assertEquals(24.0, RequipMagicRules.strikeRange(1.0), 1e-9);
        assertEquals(24.0, RequipMagicRules.strikeRange(7.0), 1e-9);
    }
}
