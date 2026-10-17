package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: Kahono and Kiato are the two exclusive casters of the mod's Song and Butoh books ({@code docs/exclusive/kh.md}, {@code kt.md}), and the series has
 * them as the brother and sister of the Seabed Temple who dream of the surface, not as Magic Knights (the owner's brief put them in a Magic Knight squad;
 * the titles here follow the series). They stand in the Sanctuary because it is the first place with open sky for them, and they arrive only after Vetto
 * has fallen, the beast that broke their temple, so a player who beats him meets the two he hurt. Their hints point at their own pages (singing, dancing)
 * and never touch the casters themselves.
 */
public final class SeabedTempleCast {
    private SeabedTempleCast() {}

    /** Kahono: a water mage whose Song Magic heals; bright, talkative, wants to be an idol of the surface. */
    public static final ArcCharacter KAHONO = ArcCharacter.npc("kahono", "Kahono").title("Priestess of the Seabed Temple")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xC890D8, 0xE8D8F0).magic("song").stats(0, 0, 0.25, 0)
            .after(KnightsGates.VETTO).build();

    /** Kiato: Kahono's older brother, a dancer-warrior of Butoh Magic; serious, devout, protective. */
    public static final ArcCharacter KIATO = ArcCharacter.npc("kiato", "Kiato").title("Dancer of the Seabed Temple")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0x78D0E8, 0xB8E6FF).magic("butoh").stats(0, 0, 0.25, 0)
            .after(KnightsGates.VETTO).build();
}
