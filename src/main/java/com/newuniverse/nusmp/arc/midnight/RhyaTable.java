package com.newuniverse.nusmp.arc.midnight;

import java.util.List;

/**
 * Why: Rhya is the Third Eye's Disloyal, an elf in a human body whose magic is copying: he touches another mage's grimoire and the spells in it become his.
 * The story shows him wearing the face of a captain, throwing Yami's dark slash, a water javelin and Patolli's swords as if they were his own, and vanishing
 * to strike from behind. The fight is a mirror held up to the party: he learns the magic that hits him and answers in its colours, and he knows that magic
 * so well that it hurts him less; the answer is to change what you throw. This table holds the numbers (pure, unit tested), so {@code RhyaBoss} only has to
 * turn each row into a telegraph and a strike.
 * <p>
 * Rhya is the member of the Third Eye that no other builder covers (Vetto belongs to the first saga's builder, Fana is her own boss), which is why the
 * "Third Eye" of the brief is built as Rhya plus the bond that makes the three of them fight as one ({@link EyeBondRules}).
 */
public final class RhyaTable {
    private RhyaTable() {}

    public static final String COPY_TOUCH = "copy_touch", BOLTS = "imitation_bolts", LANCE = "imitation_lance", AVIDYA = "avidya_slash",
            JAVELIN = "aqua_javelin", SCOUT = "invisible_scout", NOVA = "imitation_nova", SEAL = "seal_chains", SALAMANDER_COPY = "salamander_copy",
            LIGHT_COPY = "light_copy", SPELLBOOK = "stolen_spellbook";

    /** Base damage of the moves that are not shapes of the Tower tables ({@code TowerMath}: bolts 6.5, lance 11, nova 13), before the boss's damage law. */
    public static final float TOUCH = 7f, AVIDYA_HIT = 12f, JAVELIN_HIT = 10f, SCOUT_HIT = 13f, SEAL_HIT = 6f, BREATH_HIT = 9f, LIGHT_HIT = 5.5f, BOOK_NOVA = 9f;

    /** The moves in the order the boss lists them (the client reads names by index: new moves are only ever appended). */
    private static final List<MidnightSpec> SPECS = List.of(
            new MidnightSpec(COPY_TOUCH, "Copy Magic: Touch", 20, 18, 260, 0, 30, 4, false, false, 8),
            new MidnightSpec(BOLTS, "Imitation: Bolts", 20, 18, 90, 5, 36, 6, false, false, 8),
            new MidnightSpec(LANCE, "Imitation: Lance", 20, 22, 160, 6, 36, 4, false, false, 10),
            new MidnightSpec(AVIDYA, "Dark Cloaked Avidya Slash", 20, 22, 150, 5, 26, 4, false, false, 10),
            new MidnightSpec(JAVELIN, "Aqua Javelin", 20, 20, 130, 8, 38, 4, false, false, 8),
            new MidnightSpec(SCOUT, "Invisible Scout", 20, 24, 210, 5, 40, 4, false, false, 10),
            new MidnightSpec(NOVA, "Imitation: Nova", 40, 28, 250, 0, 34, 4, false, true, 12),
            new MidnightSpec(SEAL, "Seal Magic: Chains", 40, 28, 260, 0, 30, 3, false, false, 10),
            new MidnightSpec(SALAMANDER_COPY, "Copied: Salamander's Breath", 60, 24, 220, 2, 16, 3, false, false, 8),
            new MidnightSpec(LIGHT_COPY, "Copied: Light Sword of Judgment", 60, 22, 200, 5, 34, 3, false, false, 8),
            new MidnightSpec(SPELLBOOK, "Stolen Spellbook", 60, 50, 1250, 0, 40, 5, true, true, 60));

    public static List<MidnightSpec> specs() { return SPECS; }

    public static MidnightSpec byId(String id) {
        for (MidnightSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
