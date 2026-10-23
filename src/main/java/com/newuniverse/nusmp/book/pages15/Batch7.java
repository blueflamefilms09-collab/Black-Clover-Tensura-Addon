package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch 7 of the "15 spells for every grimoire" project: Nature and Beast.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_world_tree}, {@code book_poison}, {@code book_cotton}, {@code book_plant}, {@code book_cherry_blossom}, {@code book_briar}, {@code book_tree}, {@code book_vine}, {@code book_poison_plant}, {@code book_beast}, {@code book_body}, {@code book_fungus}, {@code book_demon_beast}.
 *
 * <p>THE BUILDER OF THIS BATCH EDITS ONLY THIS FILE (plus tools/lang/pages15_batch7.json, src/test/resources/armour_policy/pages15_batch7_new_pages.json and docs/magic/pages15_batch7.md). How to write a
 * page: docs/magic/pages15_recipe.md. Shape of an entry, one {@code register} per book (the page counter script and the tests read this exact shape):
 * <pre>
 *     private static final List&lt;BookPage&gt; EXPLOSION = register(BOOK_NAME_HERE, List.of(
 *             Pages15Kit.mid(PAGE_ID_HERE, PAGE_NAME_HERE, Pages15Kit.nova(Tier.MID, 4, false, VfxShape.FLAME_EXPLOSION, Pages15Kit.ignite(3))).withCooldown(200)));
 * </pre>
 * (the real call takes string literals; they are not written out here because the armour-policy test scans the sources for page ids). Pages are appended AFTER every old page of the book, in the order
 * written here; never reorder or rename one once released (page ids are save keys).
 */
public final class Batch7 {
    private Batch7() {}

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
