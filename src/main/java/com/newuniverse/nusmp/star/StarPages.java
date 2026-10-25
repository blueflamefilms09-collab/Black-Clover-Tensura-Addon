package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.pages15.Pages15Kit;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Star Magic nodes: the seven pages appended to {@code book_star} AFTER every other page (the Pages15 pages included), so no id, name, mode number, unlock bit or mastery of an older page moves (rule 2). They
 * are handed to {@code Pages15.definedFor} (one line), so they follow the 15-pages project's setting ({@code magic.pagesFifteen}) and its self-test {@code magic.pages15}. The list is the same on client and server whatever
 * {@code star.nodes} says (so the mode numbers always agree); with {@code star.nodes = false} the new pages simply never open ({@link #block}) and cannot be cast.
 *
 * <p>Pages (docs/vfx_remake/star_ingame.md has the canon source of each): Star Seed (starter), Quartile Scutum and Quartile Hasta (mid, heir), Black Neverland (zone, heir), Quintile: Atrum Scutum / Hasta /
 * Flagellum (zone, heir, unverified canon: one source). Conjunction and the Quartile Flagellum are older pages of the book whose casts were replaced in {@code Batch1} by {@link StarCasts#conjunction} and
 * {@link StarCasts#flagellum} (the old cast is their fallback).
 */
public final class StarPages {
    private StarPages() {}

    public static final String BOOK = "book_star";

    private static List<BookPage> pages;

    /** The seven new pages, in order (built once). */
    public static synchronized List<BookPage> pages() {
        if (pages == null) {
            pages = List.of(
                    Pages15Kit.starter("star_seed", "Star Seed", StarCasts.seed()).withCooldown(20).withAnim("up"),
                    Pages15Kit.mid("quartile_scutum", "Quartile Scutum", StarCasts.scutum(Tier.MID, 4, StarRules.PANE_LIFE_TICKS)).withCooldown(300).withAnim("up"),
                    Pages15Kit.mid("quartile_hasta", "Quartile Hasta", StarCasts.hasta(Tier.MID, 4, 1.2)).withCooldown(160).withAnim("thrust"),
                    Pages15Kit.zone("black_neverland", "Black Neverland", StarCasts.blackNeverland()).withCooldown(420).withAnim("up"),
                    Pages15Kit.zone("quintile_atrum_scutum", "Quintile: Atrum Scutum", StarCasts.scutum(Tier.ZONE, 5, StarRules.PANE_LIFE_TICKS_QUINTILE)).withCooldown(360).withAnim("up"),
                    Pages15Kit.zone("quintile_atrum_hasta", "Quintile: Atrum Hasta", StarCasts.hasta(Tier.ZONE, 5, 1.6)).withCooldown(400).withAnim("thrust"),
                    Pages15Kit.zone("quintile_atrum_flagellum", "Quintile: Atrum Flagellum", StarCasts.atrumFlagellum()).withCooldown(380).withAnim("sweep"));
        }
        return pages;
    }

    /**
     * The hook of {@code Pages15.definedFor}: the pages the batch wrote for this book followed by the seven new ones (the same list object for every other book). {@code Pages15.append} then puts them
     * after every other page of the book, honours {@code magic.pagesFifteen} and the 64-page limit, and the in-game self-test {@code magic.pages15} checks them like every page of the project.
     */
    public static List<BookPage> extend(String bookPath, List<BookPage> batch) {
        if (!BOOK.equals(bookPath)) return batch;
        List<BookPage> out = new ArrayList<>(batch);
        Set<String> ids = new java.util.HashSet<>();
        for (BookPage p : batch) ids.add(p.id());
        for (BookPage p : pages()) if (ids.add(p.id())) out.add(p);
        return List.copyOf(out);
    }

    /**
     * The hook of {@code GrimoireBook.pageBlock}: what a new page waits for before mastery may open it (null = nothing). With {@code star.nodes} off none of them opens; the heir pages wait for the heir mark. The
     * old pages (the Flagellum included) are never blocked.
     */
    public static String block(GrimoireBook book, BookPage page, ServerPlayer p) {
        if (book == null || page == null || book.getRegistryName() == null || !BOOK.equals(book.getRegistryName().getPath()) || !StarRules.NEW_PAGES.contains(page.id())) return null;
        if (!StarConfig.nodes()) return "a server where Star nodes are switched on";
        if (StarRules.HEIR_PAGES.contains(page.id()) && StarAccess.needsHeir(page.id(), StarConfig.vfxForAll()) && !StarGame.isHeir(p)) return "a Grinberryall heir (a Spade Star grimoire at full mastery, after the Spade Kingdom Raid)";      // star.vfxForAll (default): only Black Neverland still waits for the heir; false = the 3.7.0 table
        return null;
    }
}
