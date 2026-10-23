package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.charset.CharacterSetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the owner wants every grimoire to have 15 spells ("15 spells for every grimoire", docs/magic/page_counts.md, docs/magic/pages15_recipe.md). The new pages are APPENDED after every other page of the
 * book (rule 2: no page id, name or mode number moves, so every save, unlock bit and mastery stays), by the outermost wrapper of {@code GrimoireBook.familyAll()}. Eight builders each own a disjoint set of books
 * ({@link Pages15Plan}) and write the pages of their books in ONE class each ({@link Batch1} ... {@link Batch8}); this class only asks the batch that owns the book.
 *
 * <p>Server setting {@code magic.pagesFifteen} (file nusmp-character-spells-server.toml, default true): false returns every list unchanged, i.e. the books as they were before this project. A batch that throws
 * is logged once and ignored (its books keep their old pages); a page whose id the book already has is skipped; a book never grows past {@link Pages15Rules#MAX_FAMILY} family pages. The result is built once per
 * book and cached by {@code GrimoireBook}; the cost while nobody casts is zero.
 */
public final class Pages15 {
    private Pages15() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp/pages15");
    private static final Set<Integer> BROKEN = new HashSet<>();

    /** Is the project switched on (server setting {@code magic.pagesFifteen}, default true)? */
    public static boolean enabled() { return CharacterSetConfig.pagesFifteen(); }

    /** What the owning batch defines for this book (every page, switch or not): empty for a book nobody wrote pages for. Never null. */
    public static List<BookPage> definedFor(String bookPath) {
        int batch = Pages15Plan.batchOf(bookPath);
        if (batch == 0) return List.of();
        try {
            List<BookPage> l = switch (batch) {
                case 1 -> Batch1.pages(bookPath);
                case 2 -> Batch2.pages(bookPath);
                case 3 -> Batch3.pages(bookPath);
                case 4 -> Batch4.pages(bookPath);
                case 5 -> Batch5.pages(bookPath);
                case 6 -> Batch6.pages(bookPath);
                case 7 -> Batch7.pages(bookPath);
                case 8 -> Batch8.pages(bookPath);
                default -> List.of();
            };
            return l == null ? List.of() : l;
        } catch (RuntimeException | LinkageError e) {
            if (BROKEN.add(batch)) LOG.error("[nusmp] pages15 batch {} failed for {}; its books keep their old pages", batch, bookPath, e);
            return List.of();
        }
    }

    /** The book's family list followed by the new pages. A book with nothing to add (or the setting off) gets its list back unchanged (the same object). */
    public static List<BookPage> append(String bookPath, List<BookPage> base) {
        if (bookPath == null || !enabled()) return base;
        List<BookPage> extra = definedFor(bookPath);
        if (extra.isEmpty()) return base;
        List<BookPage> out = new ArrayList<>(base);
        Set<String> ids = new HashSet<>();
        for (BookPage p : base) ids.add(p.id());
        for (BookPage p : extra) {
            if (out.size() >= Pages15Rules.MAX_FAMILY) break;
            if (ids.add(p.id())) out.add(p);      // never twice (a book that already has the id keeps its own)
        }
        return out.size() == base.size() ? base : List.copyOf(out);
    }

    /** The ids this class appends to a book, in order (empty when the setting is off). For the self-test and the docs. */
    public static List<String> addedTo(String bookPath) {
        List<String> out = new ArrayList<>();
        if (!enabled()) return out;
        for (BookPage p : definedFor(bookPath)) out.add(p.id());
        return out;
    }

    /** Was this page appended by this class? */
    public static boolean isAdded(String bookPath, String pageId) { return addedTo(bookPath).contains(pageId); }
}
