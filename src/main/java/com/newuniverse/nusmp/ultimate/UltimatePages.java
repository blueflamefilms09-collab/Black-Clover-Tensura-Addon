package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.pages15.Pages15Rules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the grimoire side of Ultimate Magic. {@code GrimoireBook.familyAll()} wraps the page list of every book in {@link #append}, OUTSIDE the "15 spells for every grimoire" wrapper, so the Ultimate is the LAST
 * family page of its book: no existing page id, name, mode number, unlock bit or mastery moves (rule 2; same recipe as docs/magic/pages15_recipe.md). A book that has no Ultimate, a server that switched the feature
 * off ({@code ultimate.enabled = false}) and a book that already holds the id get their list back unchanged (the same object). The page is an ordinary family page: it opens through the mastery order at the end of
 * the bar, and {@link #block} adds the late-game gate (full mastery, Senior 1st Class, 1,000,000 EP) that {@code MasteryPages.tryUnlock} asks through {@code GrimoireBook.pageBlock}, so it stays sealed until the gate
 * is open; the cast checks the same gate again.
 *
 * <p>The page costs nothing in the grimoire's own flow (cost 0, no cooldown there): the real price, the 100 percent of the mana, the wind-up and the long cooldown are decided by {@link UltimateManager}.
 */
public final class UltimatePages {
    private UltimatePages() {}

    /** Is the feature on (server setting {@code ultimate.enabled}, default true)? */
    public static boolean enabled() { return UltimateConfig.enabled(); }

    /** The page of one Ultimate. Cost 0 and cooldown 0 in the grimoire's own flow: {@link UltimateManager} charges the whole mana at the release and sets the long cooldown itself. */
    public static BookPage pageFor(UltimateDef def) {
        return new BookPage(def.id(), def.name(), def.name(), 0, 0, 0, (book, inst, caster, mode) -> UltimateManager.begin(book, inst, caster, mode, def), com.newuniverse.nusmp.anim.CastAnim.MANA_ZONE);      // the release clip of this page is the looping charge pose of the wind-up
    }

    /** The book's family list followed by its Ultimate. The same object when there is nothing to add. */
    public static List<BookPage> append(String bookPath, List<BookPage> base) {
        if (bookPath == null || base == null || !enabled()) return base;
        UltimateDef def = UltimateRegistry.forBook(bookPath);
        if (def == null || base.size() >= Pages15Rules.MAX_FAMILY) return base;
        for (BookPage p : base) if (p.id().equals(def.id())) return base;
        List<BookPage> out = new ArrayList<>(base.size() + 1);
        out.addAll(base);
        out.add(pageFor(def));
        return List.copyOf(out);
    }

    /** How many pages this class appends to a book right now (0 or 1); the "15 spells" self-test subtracts it to find its own pages. */
    public static int extraCount(String bookPath) { return enabled() && UltimateRegistry.forBook(bookPath) != null ? 1 : 0; }

    /** The ids this class appends to books, for the docs and the tests. */
    public static Set<String> ids() {
        Set<String> out = new HashSet<>();
        for (UltimateDef d : UltimateRegistry.all()) out.add(d.id());
        return out;
    }

    /**
     * What the Ultimate page of this book still waits for, or null when it is open (or the page is not an Ultimate). {@code GrimoireBook.pageBlock} returns it to {@code MasteryPages.tryUnlock}, which keeps the
     * page sealed and tells the player once what is missing.
     */
    public static String block(GrimoireBook book, BookPage page, ServerPlayer p, ManasSkillInstance inst) {
        if (page == null || !UltimateRegistry.isUltimatePage(page.id())) return null;
        UltimateGate.Verdict v = UltimateManager.verdict(p, book, inst);
        return v.open() ? null : v.summary();
    }
}
