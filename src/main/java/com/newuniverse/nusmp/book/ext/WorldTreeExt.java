package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * 0.53 World Tree Magic audit: STUB (replaced by the attribute's own implementation). The pages returned here are APPENDED to the
 * existing WORLD_TREE book (never reorder or remove the book's own pages).
 */
public final class WorldTreeExt {
    private WorldTreeExt() {}

    public static List<BookPage> pages() { return List.of(); }
}
