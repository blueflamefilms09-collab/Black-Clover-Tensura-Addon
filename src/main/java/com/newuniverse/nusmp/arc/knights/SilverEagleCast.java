package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: the Silver Eagle is the squad of the Silva family, a house of royal blood whose pride is part of the character: Nozel, the cold captain who cares more
 * than he says, and Solid, the second son, who is learning to stop sneering. They arrive together (both after the second champion of the first saga) because
 * one without the other loses the joke of the family; the Black Bulls' Noelle, their sister, belongs to the other NPC cast.
 */
public final class SilverEagleCast {
    private SilverEagleCast() {}

    /** Nozel Silva: captain of the Silver Eagle; Mercury Magic; curt, proud, guarded. */
    public static final ArcCharacter NOZEL_SILVA = ArcCharacter.npc("nozel_silva", "Nozel Silva").title("Captain of the Silver Eagle")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xC8CCD8, 0x8890A8).magic("mercury").stats(0, 0, 0.25, 0)
            .after(KnightsGates.HEATH).build();

    /** Solid Silva: second son of the Silva family, Water Magic, a knight of the Silver Eagle; condescending, softening. */
    public static final ArcCharacter SOLID_SILVA = ArcCharacter.npc("solid_silva", "Solid Silva").title("the Silver Eagle's Second Son")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0x6AA0E0, 0xC8D8F0).magic("water").stats(0, 0, 0.25, 0)
            .after(KnightsGates.HEATH).build();
}
