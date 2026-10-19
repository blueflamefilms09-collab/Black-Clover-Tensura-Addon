package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.PermeationWorks;

import java.util.List;

/**
 * 1.93.0 Permeation Magic rebuild: the pages APPENDED after the five generic wiki pages of the Permeation book (dispatched by {@link ExclusiveExt}): Phase Step,
 * Phase Through, Permeating Strike and Ghost Zone. The list only ever grows at the end.
 */
public final class PermeationExt {
    private PermeationExt() {}

    public static List<BookPage> pages() { return PermeationWorks.pages(); }
}
