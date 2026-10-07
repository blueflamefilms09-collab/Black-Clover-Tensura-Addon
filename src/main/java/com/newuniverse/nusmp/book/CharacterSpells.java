package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.grimoire.CanonBook;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Character spells (0.34): pages that only open in a named character's grimoire - the squad captains and the anime cast, after the
 * wiki. The character is the canon book bound to the player ({@code Canon} on the grimoire: the creative tab, the altar, or
 * {@code /multiverse grimoire canon <player> <book>}). A locked page that is already open stays open; the lock only decides who
 * mastery can open it for.
 */
public final class CharacterSpells {
    private CharacterSpells() {}

    private static final Map<String, Set<CanonBook>> LOCKS = new LinkedHashMap<>();

    private static void lock(String book, String page, CanonBook first, CanonBook... more) {
        LOCKS.put(book + ":" + page, EnumSet.of(first, more));
    }

    static {
        // Black Bull
        lock("book_dark", "dimension_slash_equinox", CanonBook.YAMI);
        lock("book_shadow", "unite_canis", CanonBook.NACHT);
        lock("book_shadow", "unite_gallus", CanonBook.NACHT);
        lock("book_shadow", "unite_canis_felis", CanonBook.NACHT);
        lock("book_lightning", "thunder_fiend", CanonBook.LUCK);
        lock("book_lightning", "black_lightning_battle_fiend", CanonBook.LUCK);
        lock("book_poison", "curse_workers_neighbor", CanonBook.GORDON);
        lock("book_recombination", "raging_black_bull", CanonBook.HENRY);
        lock("book_ash", "revelation_of_the_cowardly", CanonBook.ZORA);
        // Crimson Lion
        lock("book_fire", "leo_rugiens", CanonBook.FUEGOLEON, CanonBook.LEOPOLD);
        lock("book_fire", "calidos_brachium", CanonBook.MEREOLEONA);
        // Golden Dawn
        lock("book_wind", "spirit_of_zephyr", CanonBook.YUNO);
        lock("book_plant", "yggdrasil", CanonBook.WILLIAM);
        // the other captains
        lock("book_plant", "briar_prison", CanonBook.CHARLOTTE);
        lock("book_sword", "ripper_cut", CanonBook.JACK);
        lock("book_creation", "painted_menagerie", CanonBook.RILL);
        lock("book_painting", "painted_menagerie", CanonBook.RILL);      // 0.41: Painting Magic proper
        lock("book_painting", "master_of_valhalla", CanonBook.RILL);
        lock("book_dream", "glamour_world", CanonBook.DOROTHY);
        lock("book_storm", "vortex_shield", CanonBook.KAISER);
        lock("book_mercury", "mercury_rain", CanonBook.NOZEL);
        // 0.56: Secre Swallowtail's Seal Magic
        lock("book_sealing", "branching_array", CanonBook.SECRE);
        lock("book_sealing", "eternal_prison", CanonBook.SECRE);
        lock("book_sealing", "wound_sealing", CanonBook.SECRE);
        lock("book_sealing", "orbital_bind", CanonBook.SECRE);
    }

    /** Who may open this page, or null if anyone may. */
    public static Set<CanonBook> owners(GrimoireBook book, BookPage page) {
        if (book == null || page == null || book.getRegistryName() == null) return null;
        return LOCKS.get(book.getRegistryName().getPath() + ":" + page.id());
    }

    /** The canon character of the player's bound grimoire, or null. */
    public static CanonBook characterOf(ServerPlayer p) {
        return CanonBook.byId(GrimoireItem.data(GrimoireSlot.get(p)).getString("Canon"));
    }

    /** Null if the player may open this page, else who it waits for ("the grimoire of Yami Sukehiro"). */
    public static String block(GrimoireBook book, BookPage page, ServerPlayer p) {
        Set<CanonBook> owners = owners(book, page);
        if (owners == null || owners.contains(characterOf(p))) return null;
        StringBuilder sb = new StringBuilder("the grimoire of ");
        int k = 0;
        for (CanonBook b : owners) sb.append(k++ == 0 ? "" : " or ").append(b.owner);
        return sb.toString();
    }

    /** All locks, for the README / command listing. */
    public static Map<String, Set<CanonBook>> all() { return java.util.Collections.unmodifiableMap(LOCKS); }
}
