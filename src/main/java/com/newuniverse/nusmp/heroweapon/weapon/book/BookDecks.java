package com.newuniverse.nusmp.heroweapon.weapon.book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Why: which pages a Book holds depends on its form, and the forms file names a form's character only through its passives ({@code deck_feathers},
 * {@code deck_cage}), so the translation from passives to the two chapters lives in one small pure function that is tested: the offence chapter always opens
 * with the Bird of Flame and the Needle Rain and gains the Feather Volley with {@code deck_feathers}; the support chapter always opens with the Paper Wall and
 * the Healing Verse and gains the Constriction with {@code deck_cage}. The order of the pages never changes, so a wielder's plan survives a change of form.
 */
public final class BookDecks {
    private BookDecks() {}

    /** The offence chapter's pages, in reading order, for a form with these passives. */
    public static List<String> offense(Collection<String> passives) {
        List<String> l = new ArrayList<>(List.of(BookNumbers.BIRD, BookNumbers.NEEDLES));
        if (passives.contains(BookNumbers.P_DECK_FEATHERS)) l.add(BookNumbers.FEATHERS);
        return List.copyOf(l);
    }

    /** The support chapter's pages, in reading order, for a form with these passives. */
    public static List<String> support(Collection<String> passives) {
        List<String> l = new ArrayList<>(List.of(BookNumbers.WALL, BookNumbers.VERSE));
        if (passives.contains(BookNumbers.P_DECK_CAGE)) l.add(BookNumbers.CAGE);
        return List.copyOf(l);
    }

    /** True for a page of the offence chapter. */
    public static boolean isOffense(String page) {
        return BookNumbers.BIRD.equals(page) || BookNumbers.NEEDLES.equals(page) || BookNumbers.FEATHERS.equals(page);
    }
}
