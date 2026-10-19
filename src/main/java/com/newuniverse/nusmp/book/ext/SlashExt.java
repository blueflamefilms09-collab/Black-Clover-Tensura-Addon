package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.SlashWorks;

import java.util.List;

/**
 * 1.93.0 Slash Magic rebuild: the pages APPENDED to the Slash book (never reordered or removed; the book's own four pages come first): Death Scythe: Lunatic Slash
 * (the wiki's second spell) and Adaptive Edge (the wiki's adaptation to space, healing and gravity magic).
 */
public final class SlashExt {
    private SlashExt() {}

    public static List<BookPage> pages() { return SlashWorks.pages(); }
}
