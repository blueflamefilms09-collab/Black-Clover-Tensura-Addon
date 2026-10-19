package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.requip.Armour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's rule is that every weapon gets Tensura engravings that fit its lore (CLAUDE.md rule 6). Every weapon an armour of Requip: The Knight puts in the hand must be listed in
 * {@link WeaponEngravings} with real engravings (levels 1 to 3), and the weapons of the armours that fight against magic must carry the anti-magic engravings.
 */
class RequipEngravingsTest {
    @Test
    void everyRequipWeaponIsEngraved() {
        for (Armour a : Armour.values()) {
            if (a.weapon() == null) continue;
            List<WeaponEngravings.Engraving> list = WeaponEngravings.ENGRAVINGS.get(a.weapon());
            assertTrue(list != null && !list.isEmpty(), a.weapon() + " has no engravings");
            for (WeaponEngravings.Engraving e : list) assertTrue(e.level() >= 1 && e.level() <= 3 && !e.id().isBlank(), a.weapon() + " " + e);
        }
    }

    @Test
    void nakagamisEdgePartsMagic() {
        List<WeaponEngravings.Engraving> list = WeaponEngravings.ENGRAVINGS.get("nakagami_edge");
        assertTrue(list.stream().anyMatch(e -> e.id().equals("barrier_piercing")));
        assertTrue(list.stream().anyMatch(e -> e.id().equals("magic_interference")));
        assertFalse(list.stream().anyMatch(e -> e.id().equals("holy_weapon")));
    }
}
