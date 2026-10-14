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

/** Wave 12: the ten weapons are defined as the lead fixed them (ids, grades, slot), are real 3D weapons with engravings, and every ability is priced. */
class Wave12DefsTest {
    private static Map<String, RelicDef> defs() {
        List<RelicDef> all = new ArrayList<>();
        Wave12.defs(all::add);
        Map<String, RelicDef> m = new LinkedHashMap<>();
        for (RelicDef d : all) m.put(d.id(), d);
        assertEquals(all.size(), m.size(), "no id twice");
        return m;
    }

    @Test
    void theTenCardsAreHereWithTheirFixedIdsAndGrades() {
        Map<String, RelicDef> m = defs();
        assertEquals(10, m.size());
        Object[][] want = {
                {"giant_jawblade", Grade.RARE, false}, {"the_lumberjack", Grade.RARE, false}, {"white_whip_of_welting", Grade.RARE, false},
                {"whirling_blades", Grade.RARE, false}, {"warmages_longsword", Grade.UNIQUE, true}, {"sword_crystal", Grade.RARE, false},
                {"the_stabshroom", Grade.RARE, false}, {"twin_hook_swords", Grade.NORMAL, false}, {"bottle_blade", Grade.RARE, false},
                {"disintegration_dagger", Grade.RARE, false}};
        for (Object[] w : want) {
            RelicDef d = m.get((String) w[0]);
            assertNotNull(d, (String) w[0]);
            assertEquals(w[1], d.grade(), d.id());
            assertEquals(RelicSlot.MAIN_HAND, d.slot(), d.id());
            assertEquals(w[2], d.needsAttunement(), d.id() + " attunement");
            assertFalse(d.isVessel(), d.id() + " is no vessel");
            assertTrue(d.hasModel3d(), d.id() + " is a real 3D weapon");
            assertFalse(d.engravings().isEmpty(), d.id() + " has Tensura engravings");
            assertTrue(d.hasPalette(), d.id() + " has a palette");
            assertFalse(d.desc().isBlank(), d.id() + " has a description");
            assertTrue(d.desc().length() <= 330, d.id() + " description stays short: " + d.desc().length());
        }
    }

    @Test
    void everyAbilityHasACostAndAKeyThatIsUniqueInsideItsRelic() {
        Map<String, Integer> expectedActs = Map.of("giant_jawblade", 1, "the_lumberjack", 1, "white_whip_of_welting", 2, "whirling_blades", 1,
                "warmages_longsword", 2, "sword_crystal", 2, "the_stabshroom", 0, "twin_hook_swords", 1, "bottle_blade", 2, "disintegration_dagger", 0);
        for (RelicDef d : defs().values()) {
            assertEquals(expectedActs.get(d.id()), d.acts().size(), d.id() + " acts");
            Set<String> keys = new HashSet<>();
            for (Act a : d.acts()) {
                assertTrue(keys.add(a.key()), d.id() + " repeats the act key " + a.key());
                assertTrue(a.cooldownTicks() > 0, d.id() + "/" + a.key() + " must have a cooldown");
            }
        }
    }

    @Test
    void theSecondAbilityOfAnAttunedOrPairedWeaponNeedsASneak() {
        for (String id : new String[]{"white_whip_of_welting", "warmages_longsword", "sword_crystal", "bottle_blade"}) {
            RelicDef d = defs().get(id);
            assertFalse(d.acts().get(0).sneakRequired(), id + " first ability is a plain right-click");
            assertTrue(d.acts().get(1).sneakRequired(), id + " second ability is sneak + use");
        }
    }
}
