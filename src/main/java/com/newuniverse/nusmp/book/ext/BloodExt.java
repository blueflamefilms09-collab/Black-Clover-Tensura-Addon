package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * 0.53 Blood Magic upgrade: STUB (replaced by the attribute's own implementation). The pages returned here are APPENDED to the
 * existing BLOOD book (never reorder or remove the book's own pages).
 */
public final class BloodExt {
    private BloodExt() {}

    public static List<BookPage> pages() { return List.of(); }
}
