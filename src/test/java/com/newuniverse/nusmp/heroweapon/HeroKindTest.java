package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the catalogue is the key everything else is built on (the item id, the forms file, the logic class, the model, the lang key), so its shape is pinned:
 * 21 weapons, unique ids, the family counts the owner's text gives (4 Holy, 7 Seven Star, 10 Vassal), the canon sins of the Holy Weapons, the naming
 * convention of the logic classes and the existence of their source files.
 */
class HeroKindTest {
    @Test
    void twentyOneWeaponsWithUniqueIdsAndNames() {
        assertEquals(21, HeroKind.values().length);
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (HeroKind k : HeroKind.values()) {
            assertTrue(ids.add(k.itemId()), "duplicate item id " + k.itemId());
            assertTrue(names.add(k.display()), "duplicate name " + k.display());
            assertTrue(k.itemId().matches("[a-z0-9_]+"), "bad item id " + k.itemId());
        }
    }

    @Test
    void theFamiliesHaveTheOwnersCounts() {
        int holy = 0, star = 0, vassal = 0;
        for (HeroKind k : HeroKind.values()) {
            switch (k.heroClass()) {
                case HOLY -> holy++;
                case SEVEN_STAR -> star++;
                case VASSAL -> vassal++;
            }
        }
        assertEquals(4, holy);
        assertEquals(7, star);
        assertEquals(10, vassal);
        assertTrue(HeroClass.HOLY.rejectsOthers() && HeroClass.HOLY.repelsExperience());
        assertTrue(!HeroClass.SEVEN_STAR.rejectsOthers() && !HeroClass.VASSAL.repelsExperience());
    }

    @Test
    void theHolyWeaponsCarryTheirCanonSins() {
        assertEquals(java.util.List.of(Sin.WRATH), HeroKind.SHIELD.sins());
        assertEquals(java.util.List.of(Sin.GREED, Sin.GLUTTONY), HeroKind.SWORD.sins());
        assertEquals(java.util.List.of(Sin.LUST), HeroKind.SPEAR.sins());
        assertEquals(java.util.List.of(Sin.PRIDE), HeroKind.BOW.sins());
        assertEquals("Naofumi Iwatani", HeroKind.SHIELD.holder());
        assertEquals("L'Arc Berg", HeroKind.SCYTHE.holder());
    }

    @Test
    void logicClassNamesFollowTheConventionAndTheSourcesExist() throws Exception {
        assertEquals("com.newuniverse.nusmp.heroweapon.weapon.sewingset.SewingSetLogic", HeroKind.SEWING_SET.logicClassName());
        assertEquals("com.newuniverse.nusmp.heroweapon.weapon.shield.ShieldLogic", HeroKind.SHIELD.logicClassName());
        for (HeroKind k : HeroKind.values()) {
            Path src = Path.of("src/main/java", k.logicClassName().replace('.', '/') + ".java");
            assertTrue(Files.exists(src), "missing " + src);
        }
    }

    @Test
    void parseFindsAWeaponByItemIdOrName() {
        assertEquals(HeroKind.SHIELD, HeroKind.parse("shield"));
        assertEquals(HeroKind.SHIELD, HeroKind.parse("legendary_shield"));
        assertEquals(HeroKind.SHIELD, HeroKind.parse("nusmp:legendary_shield"));
        assertEquals(HeroKind.SEWING_SET, HeroKind.parse("Sewing_Set"));
        assertEquals(HeroKind.SEWING_SET, HeroKind.parse("vassal_sewing_set"));
        assertNull(HeroKind.parse("boat"));
        assertNull(HeroKind.parse(null));
        assertNotNull(HeroKind.byItemId("vassal_fan"));
        assertEquals(HeroTrigger.KEY_2, HeroTrigger.bySlot("key2"));
    }
}
