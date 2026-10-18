package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GravityWorksPages;

import java.util.List;

/**
 * 1.93.0 Gravity Magic rebuild: the pages APPENDED to the Gravity book (never reordered or removed; the book's own five pages come first): Heavy Infighting:
 * Gladiator, Evil God's Pressure Craft, Lightened Step and Weighted Blows (the wiki's spell list and its description of lowering and raising gravity).
 */
public final class GravityExt {
    private GravityExt() {}

    public static List<BookPage> pages() { return GravityWorksPages.pages(); }
}
