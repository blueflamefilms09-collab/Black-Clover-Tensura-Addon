package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * 0.53 Gravity Magic upgrade: STUB (replaced by the attribute's own implementation). The pages returned here are APPENDED to the
 * existing GRAVITY book (never reorder or remove the book's own pages).
 */
public final class GravityExt {
    private GravityExt() {}

    public static List<BookPage> pages() { return List.of(); }
}
