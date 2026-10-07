package com.newuniverse.nusmp.book;

import java.util.List;

/** 0.59 Beast Magic: the two new pages (the casts live in {@link BeastSummons}); a public bridge for book.ext.BeastExt. */
public final class BeastSummonPages {
    private BeastSummonPages() {}

    public static List<BookPage> pages() {
        return List.of(
                BookPage.zone("spirit_beast_summon", "Spirit Beast", BeastSummons::summon),
                BookPage.signature("primal_beast_form", "Primal Beast Form", BeastSummons::form));
    }
}
