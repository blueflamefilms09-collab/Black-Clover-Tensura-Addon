package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.skill.NUSkills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the pages of a merged book from its parents' books: the curated union ({@link SpellUnion}), each page wrapping the PARENT'S OWN
 * cast code (called with the parent's own book, so every spell keeps working exactly as it does for its owner) inside a
 * {@link MergeContext}, with the merged cost. The parents' books are only read, never changed. Cached per slot and definition version.
 */
public final class MergedPages {
    private MergedPages() {}

    private record Built(int version, List<BookPage> pages, GrimoireBook[] parents, boolean complete) {}

    private static final Map<Integer, Built> CACHE = new ConcurrentHashMap<>();
    public static final String UNBOUND_ID = "merged_unbound";

    private static BookPage placeholder() {
        return new BookPage(UNBOUND_ID, "Unbound", "Unbound", 0, 0, 0, (b, i, p, m) -> false);
    }

    /** The family pages of the merged book in this slot (a single inert page while the slot has no magic or a parent's book is not there). */
    public static List<BookPage> pages(int slot) { return built(slot).pages(); }

    /**
     * The page-list version of a slot's book: changes when the definition changes AND when the pages become complete (a parent's book was
     * not registered yet when the list was first asked for), so GrimoireBook.pages() rebuilds its cached list.
     */
    public static int version(int slot) { return MergedRegistry.version(slot) * 31 + (built(slot).complete() ? 1 : 0); }

    /** The parents' books of the magic in this slot (entries may be null while registries are not ready). */
    public static GrimoireBook parentBook(int slot, int parent) {
        GrimoireBook[] bs = built(slot).parents();
        return parent >= 0 && parent < bs.length ? bs[parent] : null;
    }

    private static Built built(int slot) {
        int v = MergedRegistry.version(slot);
        Built b = CACHE.get(slot);
        if (b != null && b.version() == v) return b;
        MergedDef def = MergedRegistry.bySlot(slot);
        Built made = build(def, v);
        if (made.complete()) CACHE.put(slot, made);                               // an incomplete build (a parent's book missing) is not remembered
        return made;
    }

    private static Built build(MergedDef def, int version) {
        if (def == null) return new Built(version, List.of(placeholder()), new GrimoireBook[0], true);
        int n = def.size();
        GrimoireBook[] books = new GrimoireBook[n];
        List<List<BookPage>> family = new ArrayList<>();
        List<List<SpellUnion.PageInfo>> infos = new ArrayList<>();
        boolean complete = true;
        for (int i = 0; i < n; i++) {
            MagicType t = MagicType.lookup(def.parents().get(i));
            GrimoireBook book = null;
            try { book = t == null ? null : NUSkills.bookFor(t); } catch (RuntimeException e) { /* registries not ready */ }
            books[i] = book;
            List<BookPage> pages = book == null ? List.of() : List.copyOf(book.pages().subList(0, Math.min(book.familyCount(), book.pages().size())));
            if (book == null) complete = false;
            family.add(pages);
            List<SpellUnion.PageInfo> info = new ArrayList<>();
            for (BookPage p : pages) info.add(new SpellUnion.PageInfo(p.id(), p.costPercent(), p.costFloor(), p.cooldown()));
            infos.add(info);
        }
        List<BookPage> out = new ArrayList<>();
        for (SpellUnion.Pick pick : SpellUnion.union(infos)) {
            BookPage src = null;
            for (BookPage p : family.get(pick.parent())) if (p.id().equals(pick.pageId())) { src = p; break; }
            if (src == null) continue;
            out.add(wrap(def, pick, src, books[pick.parent()]));
        }
        if (out.isEmpty()) out.add(placeholder());
        return new Built(version, List.copyOf(out), books, complete);
    }

    /** One merged page: the parent's page under the merged cost, cast through the parent's own book inside the merge context. */
    private static BookPage wrap(MergedDef def, SpellUnion.Pick pick, BookPage src, GrimoireBook parentBook) {
        BookPage.Cast parentCast = src.cast();
        int parent = pick.parent();
        BookPage.Cast cast = (b, i, p, mode) -> {
            boolean done = MergeContext.cast(def, parent, () -> parentCast.cast(parentBook, i, p, mode));
            if (done) MergeContext.stance(p, def);
            return done;
        };
        String id = def.parents().get(parent).toLowerCase(java.util.Locale.ROOT) + "_" + src.id();
        return new BookPage(id, src.name(), src.incantation(), pick.costPercent(), pick.costFloor(), pick.cooldown(), cast, src.anim(), src.weaponTag());
    }
}
