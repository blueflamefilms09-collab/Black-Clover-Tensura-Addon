package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the weapon offers the sin that is its own and only when its forms really hold a curse form of it (an offer with nothing behind it would take hearts and give
 * nothing). These tests build small form lists and check the order of the offers (canon sins first), that a Vassal weapon offers only what its file carries, which
 * form the Embrace teaches, and that damaged input (null lists, forms without a sin) never throws.
 */
class CurseCandidatesTest {
    private static HeroForm form(HeroKind kind, String id, int tier, boolean curse, Sin sin) {
        return new HeroForm(kind, id, id, "", tier, List.of(), List.of(), 0, Map.of(), Map.of(), Map.of(), List.of(), curse ? "curse" : "base", curse, sin, curse ? 6 : 0);
    }

    @Test
    void aHolyWeaponOffersItsCanonSinsInOrderAndOnlyWithCurseForms() {
        List<HeroForm> sword = new ArrayList<>();
        sword.add(form(HeroKind.SWORD, "base", 0, false, null));
        sword.add(form(HeroKind.SWORD, "gluttony_sword", 3, true, Sin.GLUTTONY));
        sword.add(form(HeroKind.SWORD, "greed_sword", 2, true, Sin.GREED));
        assertEquals(List.of(Sin.GREED, Sin.GLUTTONY), CurseCandidates.of(HeroKind.SWORD, sword), "canon order, not file order");
        List<HeroForm> onlyBase = List.of(form(HeroKind.SHIELD, "base", 0, false, null));
        assertTrue(CurseCandidates.of(HeroKind.SHIELD, onlyBase).isEmpty(), "no curse form, no whisper");
        List<HeroForm> shield = List.of(form(HeroKind.SHIELD, "base", 0, false, null), form(HeroKind.SHIELD, "wrath_shield", 5, true, Sin.WRATH));
        assertEquals(List.of(Sin.WRATH), CurseCandidates.of(HeroKind.SHIELD, shield));
    }

    @Test
    void aVassalWeaponOffersOnlyWhatItsFileCarries() {
        List<HeroForm> mirror = List.of(form(HeroKind.MIRROR, "base", 0, false, null), form(HeroKind.MIRROR, "envy_mirror", 2, true, Sin.ENVY));
        assertEquals(List.of(Sin.ENVY), CurseCandidates.of(HeroKind.MIRROR, mirror));
        List<HeroForm> plain = List.of(form(HeroKind.HAMMER, "base", 0, false, null));
        assertTrue(CurseCandidates.of(HeroKind.HAMMER, plain).isEmpty());
        List<HeroForm> two = List.of(form(HeroKind.CHAIN, "sloth_chain", 2, true, Sin.SLOTH), form(HeroKind.CHAIN, "wrath_chain", 3, true, Sin.WRATH));
        assertEquals(List.of(Sin.SLOTH, Sin.WRATH), CurseCandidates.of(HeroKind.CHAIN, two), "a justified second series follows in file order");
    }

    @Test
    void theEmbraceTeachesTheLowestTierCurseFormOfTheSin() {
        List<HeroForm> forms = List.of(
                form(HeroKind.SHIELD, "base", 0, false, null),
                form(HeroKind.SHIELD, "wrath_ii", 6, true, Sin.WRATH),
                form(HeroKind.SHIELD, "wrath_i", 5, true, Sin.WRATH),
                form(HeroKind.SHIELD, "wrath_i_twin", 5, true, Sin.WRATH),
                form(HeroKind.SHIELD, "pride_x", 1, true, Sin.PRIDE));
        assertEquals("wrath_i", CurseCandidates.firstForm(forms, Sin.WRATH).id(), "lowest tier; the earlier of equals");
        assertEquals("pride_x", CurseCandidates.firstForm(forms, Sin.PRIDE).id());
        assertNull(CurseCandidates.firstForm(forms, Sin.LUST));
        assertTrue(CurseCandidates.hasForm(forms, Sin.WRATH));
        assertFalse(CurseCandidates.hasForm(forms, Sin.SLOTH));
        assertEquals(Sin.WRATH, CurseCandidates.sinOfForm(forms, "wrath_ii"));
        assertNull(CurseCandidates.sinOfForm(forms, "base"), "an ordinary form has no sin");
        assertNull(CurseCandidates.sinOfForm(forms, "nope"));
    }

    @Test
    void damagedInputNeverThrows() {
        assertTrue(CurseCandidates.of(null, List.of()).isEmpty());
        assertTrue(CurseCandidates.of(HeroKind.SHIELD, null).isEmpty());
        assertNull(CurseCandidates.firstForm(null, Sin.WRATH));
        assertNull(CurseCandidates.firstForm(List.of(), null));
        assertNull(CurseCandidates.sinOfForm(null, "x"));
        List<HeroForm> noSin = List.of(form(HeroKind.AXE, "cursed_axe", 2, true, null));
        assertTrue(CurseCandidates.of(HeroKind.AXE, noSin).isEmpty(), "a curse form with no sin is not an offer");
        assertNull(CurseCandidates.firstForm(noSin, Sin.WRATH));
    }
}
