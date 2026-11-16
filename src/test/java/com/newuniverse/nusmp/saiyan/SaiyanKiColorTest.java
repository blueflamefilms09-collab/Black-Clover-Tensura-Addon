package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Why: the colour the anime Ki effects are drawn in (SaiyanForms.kiColor) is a rule of the look: base ki in Base, the form's colour for generic ki, the signature techniques keep their own except the red Kamehameha. */
class SaiyanKiColorTest {
    @Test
    void baseKeepsTheTechniquesOwnColour() {
        assertEquals(SaiyanForms.KI_BASE, SaiyanForms.kiColor(null, SaiyanForms.KI_BASE, true));
        assertEquals(SaiyanForms.KI_KAME, SaiyanForms.kiColor(null, SaiyanForms.KI_KAME, false));
    }

    @Test
    void genericKiTakesTheFormsColour() {
        for (SaiyanForms.Form f : SaiyanForms.ALL) assertEquals(0xFF000000 | (f.rgb() & 0xFFFFFF), SaiyanForms.kiColor(f, SaiyanForms.KI_BASE, true), f.id());
    }

    @Test
    void signatureTechniquesKeepTheirColourButTheKamehamehaTurnsRedInARedForm() {
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            assertEquals(SaiyanForms.KI_GOLD, SaiyanForms.kiColor(f, SaiyanForms.KI_GOLD, false), "the Dragon Fist stays gold in " + f.id());
            assertEquals(SaiyanForms.KI_SPIRIT, SaiyanForms.kiColor(f, SaiyanForms.KI_SPIRIT, false), "the Spirit Bomb stays blue-white in " + f.id());
            int kame = SaiyanForms.kiColor(f, SaiyanForms.KI_KAME, false);
            assertEquals(f.aura() == SaiyanForms.Aura.RED ? 0xFF000000 | (f.rgb() & 0xFFFFFF) : SaiyanForms.KI_KAME, kame, f.id());
        }
        assertEquals(SaiyanForms.SUPER_SAIYAN_4.rgb(), SaiyanForms.kiColor(SaiyanForms.SUPER_SAIYAN_4, SaiyanForms.KI_KAME, false), "the crimson 10x Kamehameha");
        assertEquals(SaiyanForms.KI_KAME, SaiyanForms.kiColor(SaiyanForms.SUPER_SAIYAN_BLUE, SaiyanForms.KI_KAME, false));
    }
}
