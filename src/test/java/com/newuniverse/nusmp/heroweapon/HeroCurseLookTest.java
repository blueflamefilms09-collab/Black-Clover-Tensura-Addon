package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newuniverse.nusmp.client.relics.item.WeaponClasses;
import org.junit.jupiter.api.Test;

/**
 * Why: the cursed look of a Holy hero weapon is drawn from the mesh {@code <id>_curse}, and the renderer finds that mesh's size class and model scale through
 * the weapon's base id. If {@code WeaponClasses.baseId} did not strip {@code _curse}, the cursed mesh would be sized by a keyword guess instead of by the class
 * of its weapon. The test pins the mapping for the four Holy Weapons and that no older id changed.
 */
class HeroCurseLookTest {
    @Test
    void theCursedMeshTakesItsBaseWeaponsClass() {
        for (HeroKind k : HeroKind.values()) {
            if (!k.isHoly()) continue;
            assertEquals(k.itemId(), WeaponClasses.baseId(k.itemId() + "_curse"), k + " cursed mesh");
            assertEquals(k.itemId(), WeaponClasses.baseId(k.itemId()), k + " plain mesh");
        }
    }

    @Test
    void olderIdsAreUnchanged() {
        assertEquals("bianca", WeaponClasses.baseId("bianca_pull2"));
        assertEquals("marids_valor", WeaponClasses.baseId("marids_valor_liquid"));
        assertEquals("returning_hand_axe", WeaponClasses.baseId("returning_hand_axe"));
        assertEquals("curse_of_rot", WeaponClasses.baseId("curse_of_rot"), "a curse in the middle of an id is not a state");
    }
}
