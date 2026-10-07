package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * 0.53 Kotodama Magic audit: STUB (replaced by the attribute's own implementation). The pages returned here are APPENDED to the
 * existing KOTODAMA book (never reorder or remove the book's own pages).
 */
public final class KotodamaExt {
    private KotodamaExt() {}

    public static List<BookPage> pages() { return List.of(); }
}
