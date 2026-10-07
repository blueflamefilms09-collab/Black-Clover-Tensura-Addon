package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BeastSummonPages;
import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * 0.59 Beast Magic upgrade: the pages appended after the BEAST book's own (never reorder or remove the book's pages).
 * spirit_beast_summon (zone cost): a spectral beast-fire beast that follows you and fights for 20 to 30 s.
 * primal_beast_form (signature cost): the spectral beast form on your body for 30 s (the older beast_form page stays as it was).
 */
public final class BeastExt {
    private BeastExt() {}

    public static List<BookPage> pages() { return BeastSummonPages.pages(); }
}
