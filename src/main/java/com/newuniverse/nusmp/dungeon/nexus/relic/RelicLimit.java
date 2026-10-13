package com.newuniverse.nusmp.dungeon.nexus.relic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The strict limit of the Dungeon Relic slots (the owner's design, section 3): a player never has more than {@link #CAP} relics working at
 * once. The two Curios slots {@code dungeon_relic} (data/nusmp/curios/slots) already hold no more; this is the same rule in code, for a pack whose
 * datapack was overridden and for the off-hand, which is the way a relic is worn without Curios. Pure: {@link Worn} hands it what it found.
 */
public final class RelicLimit {
    private RelicLimit() {}

    /** Exactly two, like the two slots. */
    public static final int CAP = 2;

    /**
     * May this relic work from here? From a Curios slot (the Dungeon Relic slots, or any slot a pack lets it into), any relic. From the off-hand: the
     * Gambler's Page, which has always been cast from there, and any relic when there is no Curios to wear it in (there the hand is the one place).
     */
    public static boolean fits(Relic relic, boolean offHand, boolean curiosAvailable) {
        if (!offHand) return true;
        return relic == Relic.GAMBLERS_GRIMOIRE_PAGE || !curiosAvailable;
    }

    /**
     * Of the candidates, in the order they were found (the Curios slots in their own order, then the off-hand), the first {@link #CAP} the test
     * accepts. The rest do not count: a third relic does nothing until one of the others is taken off.
     */
    public static <T> List<T> counted(List<T> found, Predicate<? super T> eligible) {
        List<T> out = new ArrayList<>(CAP);
        for (T candidate : found) {
            if (out.size() >= CAP) break;
            if (eligible.test(candidate)) out.add(candidate);
        }
        return out;
    }
}
