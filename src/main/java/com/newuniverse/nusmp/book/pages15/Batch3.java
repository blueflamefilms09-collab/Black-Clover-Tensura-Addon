package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch 3 of the "15 spells for every grimoire" project: Wind, Air, Sound, Mind and Soul.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_dream}, {@code book_illusion}, {@code book_air}, {@code book_smoke}, {@code book_vortex}, {@code book_wing}, {@code book_wind}, {@code book_sound}, {@code book_memory}, {@code book_permeation}, {@code book_soul}, {@code book_soul_corpse}, {@code book_song}.
 *
 * <p>THE BUILDER OF THIS BATCH EDITS ONLY THIS FILE (plus tools/lang/pages15_batch3.json, src/test/resources/armour_policy/pages15_batch3_new_pages.json and docs/magic/pages15_batch3.md). How to write a
 * page: docs/magic/pages15_recipe.md. Shape of an entry, one {@code register} per book (the page counter script and the tests read this exact shape):
 * <pre>
 *     private static final List&lt;BookPage&gt; EXPLOSION = register(BOOK_NAME_HERE, List.of(
 *             Pages15Kit.mid(PAGE_ID_HERE, PAGE_NAME_HERE, Pages15Kit.nova(Tier.MID, 4, false, VfxShape.FLAME_EXPLOSION, Pages15Kit.ignite(3))).withCooldown(200)));
 * </pre>
 * (the real call takes string literals; they are not written out here because the armour-policy test scans the sources for page ids). Pages are appended AFTER every old page of the book, in the order
 * written here; never reorder or rename one once released (page ids are save keys).
 */
public final class Batch3 {
    private Batch3() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- the books of this batch go here, one register(...) each

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
