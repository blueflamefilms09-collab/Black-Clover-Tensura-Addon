package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;

import java.util.ArrayList;
import java.util.List;

/** 0.53: appends an upgrade's pages after a book's own (the book's pages keep their order and mode numbers). */
public final class Ext {
    private Ext() {}

    public static List<BookPage> join(List<BookPage> base, List<BookPage> extra) {
        if (extra.isEmpty()) return base;
        List<BookPage> all = new ArrayList<>(base);
        all.addAll(extra);
        return List.copyOf(all);
    }
}
