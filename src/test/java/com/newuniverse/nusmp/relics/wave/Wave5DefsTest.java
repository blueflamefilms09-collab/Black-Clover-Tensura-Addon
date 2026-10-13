package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 5: the seven neck relics are defined as the lead fixed them (ids, grades, slot, vessel) and every ability is priced. */
class Wave5DefsTest {
    private static Map<String, RelicDef> defs() {
        List<RelicDef> all = new ArrayList<>();
        Wave5.defs(all::add);
        Map<String, RelicDef> m = new LinkedHashMap<>();
        for (RelicDef d : all) m.put(d.id(), d);
        assertEquals(all.size(), m.size(), "no id twice");
        return m;
    }

    @Test
    void theSevenCardsAreHereWithTheirFixedIdsGradesAndSlots() {
        Map<String, RelicDef> m = defs();
        assertEquals(7, m.size());
        Object[][] want = {
                {"amulet_of_the_warmage", Grade.RARE, false},
                {"hemlocks_amulet", Grade.LEGEND, true},
                {"amulet_of_safeguarding", Grade.UNIQUE, false},
                {"amulet_of_the_huntress", Grade.RARE, false},
                {"the_amulet_of_lost_souls", Grade.LEGEND, true},
                {"lovelorn_locket", Grade.SPECIAL, false},
                {"amulet_of_dragon_rage", Grade.LEGEND, true}};
        for (Object[] w : want) {
            RelicDef d = m.get((String) w[0]);
            assertNotNull(d, (String) w[0]);
            assertEquals(w[1], d.grade(), d.id());
            assertEquals(RelicSlot.NECK, d.slot(), d.id());
            assertEquals(w[2], d.isVessel(), d.id() + " vessel");
            assertTrue(d.needsAttunement(), d.id() + " needs attunement");
            assertFalse(d.hasModel3d(), "an amulet has no hand mesh");
            assertFalse(d.desc().isBlank(), d.id() + " has a description");
            assertTrue(d.desc().length() <= 330, d.id() + " description stays short: " + d.desc().length());
            assertTrue(d.hasPalette(), d.id() + " has a palette");
            if (d.isVessel()) assertTrue(d.grade().djinnCapable(), d.id() + " is a legal vessel");
        }
    }

    @Test
    void everyAbilityHasACostAndAKeyThatIsUniqueInsideItsRelic() {
        Map<String, Integer> expectedActs = Map.of("amulet_of_the_warmage", 4, "hemlocks_amulet", 2, "amulet_of_safeguarding", 1, "the_amulet_of_lost_souls", 4,
                "lovelorn_locket", 1, "amulet_of_dragon_rage", 3, "amulet_of_the_huntress", 0);
        for (RelicDef d : defs().values()) {
            assertEquals(expectedActs.get(d.id()), d.acts().size(), d.id() + " acts");
            Set<String> keys = new HashSet<>();
            for (Act a : d.acts()) {
                assertTrue(keys.add(a.key()), d.id() + " repeats the act key " + a.key());
                assertTrue(a.cooldownTicks() > 0 || a.epPercentCost() > 0 || a.usesPerDay() > 0, d.id() + "/" + a.key() + " must cost something");
                assertTrue(a.epPercentCost() <= 10.0, d.id() + "/" + a.key() + " EP cost stays moderate");
                assertTrue(a.manaPercent() <= 10, d.id() + "/" + a.key() + " mana cost stays moderate");
            }
        }
    }

    @Test
    void theDragonFormIsPaidInExistenceAndOncePerDay() {
        RelicDef d = defs().get("amulet_of_dragon_rage");
        Act word = d.actByKey("command_word");
        assertNotNull(word);
        assertEquals(24000, word.cooldownTicks(), "once a day, like the card's 24 hours");
        assertTrue(word.epPercentCost() > 0, "the legend's form costs existence points");
        assertNotNull(d.actByKey("frost_breath"));
        assertNotNull(d.actByKey("back_to_flesh"));
    }

    @Test
    void theSpellAmuletsHaveOneAbilityPerSpellTierAndTheWarmageOnePerCantrip() {
        RelicDef souls = defs().get("the_amulet_of_lost_souls");
        for (int i = 1; i <= 3; i++) assertNotNull(souls.actByKey("soul_spell_" + i));
        assertNotNull(souls.actByKey("commune"));
        RelicDef mage = defs().get("amulet_of_the_warmage");
        for (String k : new String[]{"fire_bolt", "ray_of_frost", "shocking_grasp", "acid_splash"}) assertNotNull(mage.actByKey(k), k);
    }
}
