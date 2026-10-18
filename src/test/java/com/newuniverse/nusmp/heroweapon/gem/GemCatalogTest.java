package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the nineteen acquisition items are derived from the weapon catalogue, and the models, icons, recipes and lang entries are all found by the ids
 * derived here. If the derivation drifted (a Holy Weapon given a Seal, a repeated id, an id the game would reject) a whole set of files would silently stop
 * matching its item. The canon colours are pinned too, since the icons and the effects must show the jewel colours the series gives.
 */
class GemCatalogTest {
    @Test
    void seventeenSealsOneForEveryWeaponThatIsNotHoly() {
        List<HeroKind> kinds = GemCatalog.sealKinds();
        assertEquals(17, kinds.size());
        assertEquals(HeroKind.values().length - 4, kinds.size());
        for (HeroKind k : kinds) assertFalse(k.isHoly(), k + " is Holy and cannot have a Seal");
        for (HeroKind k : HeroKind.values()) assertEquals(!k.isHoly(), GemCatalog.hasSeal(k));
        assertFalse(GemCatalog.hasSeal(null));
        assertEquals(HeroKind.HAMMER, kinds.get(0), "catalogue order");
        assertEquals(HeroKind.CHAIN, kinds.get(16));
    }

    @Test
    void nineteenValidUniqueItemIdsInCreativeTabOrder() {
        List<String> ids = GemCatalog.allItemIds();
        assertEquals(19, ids.size());
        assertEquals("calling_gem", ids.get(0));
        assertEquals("gem_fragment", ids.get(1));
        assertEquals("vassal_seal_hammer", ids.get(2));
        assertEquals("vassal_seal_sewing_set", GemCatalog.sealId(HeroKind.SEWING_SET));
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            assertTrue(id.matches("[a-z0-9_]+"), "bad registry path " + id);
            assertTrue(seen.add(id), "duplicate id " + id);
            for (HeroKind k : HeroKind.values()) assertFalse(k.itemId().equals(id), "collides with the weapon item " + k.itemId());
        }
    }

    @Test
    void sealNamesDropTheFamilyWord() {
        assertEquals("Scythe", GemCatalog.shortName(HeroKind.SCYTHE));
        assertEquals("Musical Instrument", GemCatalog.shortName(HeroKind.INSTRUMENT));
        assertEquals("Vassal Seal: Sewing Set", GemCatalog.sealName(HeroKind.SEWING_SET));
        assertEquals("Legendary Shield", GemCatalog.shortName(HeroKind.SHIELD), "a name without the family word is kept whole");
    }

    @Test
    void theHolyJewelsAreTheCanonColoursInTheEnumOrder() {
        assertEquals(0x38C86A, GemCatalog.colour(HeroKind.SHIELD), "green");
        assertEquals(0x3F7FE8, GemCatalog.colour(HeroKind.SWORD), "blue");
        assertEquals(0xE23B3B, GemCatalog.colour(HeroKind.SPEAR), "red");
        assertEquals(0xF2C62B, GemCatalog.colour(HeroKind.BOW), "yellow");
        assertEquals(4, GemCatalog.FOUR_COLOURS.length);
        HeroKind[] holy = {HeroKind.SHIELD, HeroKind.SWORD, HeroKind.SPEAR, HeroKind.BOW};
        for (int i = 0; i < 4; i++) assertEquals(GemCatalog.colour(holy[i]), GemCatalog.FOUR_COLOURS[i]);
        Set<Integer> distinct = new HashSet<>();
        for (int c : GemCatalog.FOUR_COLOURS) distinct.add(c);
        assertEquals(4, distinct.size());
    }

    @Test
    void theSealsTakeTheirFamilysTint() {
        for (HeroKind k : HeroKind.values()) {
            switch (k.heroClass()) {
                case SEVEN_STAR -> assertEquals(GemCatalog.SEVEN_STAR_AMBER, GemCatalog.colour(k));
                case VASSAL -> assertEquals(GemCatalog.VASSAL_AQUA, GemCatalog.colour(k));
                case HOLY -> assertTrue(GemCatalog.colour(k) != GemCatalog.SEVEN_STAR_AMBER && GemCatalog.colour(k) != GemCatalog.VASSAL_AQUA);
            }
        }
        assertEquals(0xFF38C86A, GemCatalog.argb(0x38C86A));
        assertEquals(0xFF000000, GemCatalog.argb(0));
    }
}
