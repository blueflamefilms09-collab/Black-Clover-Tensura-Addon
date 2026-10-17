package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: the Vermillion family fills the Crimson Lion, and three of them are friends in the Sanctuary: Fuegoleon (the captain, calm and just), Mereoleona (the
 * strongest, loud and merciless about training) and Leopold (the youngest, who declares everybody his rival). They are three very different voices on the
 * same fire, which is why the records are together: the Flame Magic hints each of them gives (the spirit bond, Mana Zone, Mana Strike, warming up) should
 * not repeat. Mimosa, their sister, stands with the Golden Dawn ({@link GoldenDawnCast}) because that is the squad she chose.
 *
 * <p>Leopold has no gate (a beginner's cheerleader); the captain comes after the first champion, the lioness after the fourth, see {@link KnightsGates}.
 */
public final class CrimsonLionCast {
    private CrimsonLionCast() {}

    /** Fuegoleon Vermillion: captain of the Crimson Lion; Flame Magic with the fire spirit Salamander; measured, protective, honourable. */
    public static final ArcCharacter FUEGOLEON_VERMILLION = ArcCharacter.npc("fuegoleon_vermillion", "Fuegoleon Vermillion").title("Captain of the Crimson Lion")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xE0502C, 0xE8B850).magic("flame").stats(0, 0, 0.25, 0)
            .after(KnightsGates.REVCHI).build();

    /** Mereoleona Vermillion: the strongest Vermillion; Flame Magic and Mana Zone; blunt, fearless, delighted by a fight. */
    public static final ArcCharacter MEREOLEONA_VERMILLION = ArcCharacter.npc("mereoleona_vermillion", "Mereoleona Vermillion").title("the Crimson Lioness")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xE8602A, 0xE8B850).magic("flame").stats(0, 0, 0.25, 0)
            .after(KnightsGates.MARS).build();

    /** Leopold Vermillion: the youngest of the Crimson Lion; Flame Magic; loud, sincere, always picking a rival. */
    public static final ArcCharacter LEOPOLD_VERMILLION = ArcCharacter.npc("leopold_vermillion", "Leopold Vermillion").title("the Crimson Lion's Youngest Flame")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xF08040, 0xD8A848).magic("flame").stats(0, 0, 0.25, 0).build();
}
