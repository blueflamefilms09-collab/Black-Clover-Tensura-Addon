package com.newuniverse.nusmp.arc.knights;

/**
 * Why: the Sanctuary Hub should fill up as the story moves, not show sixteen strangers to a player who has not yet fought anything, so the more senior
 * Magic Knights only come to the plaza once the delvers have shown themselves. The Hub places an NPC when the prerequisites of its record are beaten
 * (see {@code ArcCharacter.prerequisites}), and every prerequisite of this cast is one of the five opening champions of the first saga. The ids are
 * spelled ONCE here so a typo cannot make a character wait for somebody who does not exist. They are the ids the briefs of the Saga 1 builders fix
 * ({@code revchi_salik}, {@code heath_grice}, {@code lotus_whomalt}, {@code mars}, {@code vetto}); the story rules treat an id that is not in the
 * roster as already satisfied, so if one of them is renamed the character simply stands in the Hub from the start instead of never appearing.
 *
 * <p>Who waits for whom (the court, the juniors Yuno, Mimosa, Klaus and Leopold wait for nobody): the first captains after the first champion, the
 * Silver Eagle and the Green Mantis after the second, the Coral Peacock and the Golden Dawn's captain after the third, the strongest Vermillion after
 * the fourth, and the two survivors of the Seabed Temple after the fifth, the beast that broke their temple.
 */
public final class KnightsGates {
    private KnightsGates() {}

    /** The first champion of the first saga: the Crimson Lion's captain and the Blue Rose's captain arrive after it. */
    public static final String REVCHI = "revchi_salik";
    /** The second: the Silver Eagle (Nozel, Solid) and the Green Mantis (Jack) arrive. */
    public static final String HEATH = "heath_grice";
    /** The third: the Coral Peacock (Dorothy) and the Golden Dawn's captain (William) arrive. */
    public static final String LOTUS = "lotus_whomalt";
    /** The fourth: Mereoleona arrives. */
    public static final String MARS = "mars";
    /** The fifth, the beast that broke the Seabed Temple: Kahono and Kiato arrive. */
    public static final String VETTO = "vetto";
}
