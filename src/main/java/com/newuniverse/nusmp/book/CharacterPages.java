package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the pilot characters (docs/magic/character_spells.md) used spells the shared element books never had (Yuno's Sylph's Breath, Noelle's Point-Blank Sea Dragon's Roar, Fuegoleon's Salamander's
 * Breath, Yami's Black Crescent Moon). They are APPENDED to the end of the book's family list (rule 2: no page id, name or mode number moves, so every save, unlock bit and mastery stays), and each
 * belongs to one character: it is exclusive to that character's data file, so another grimoire never opens it, with the character-spell-set switch off too. The casts are in {@link CharacterArts}.
 *
 * <p>The tiers (cost, cooldown) follow the usual page tiers; the rank gates come from those tiers as for every other page.
 */
public final class CharacterPages {
    private CharacterPages() {}

    /** Book registry path -> the page ids this class appended, in order. */
    private static final Map<String, List<String>> ADDED = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        List<String> ids = new ArrayList<>();
        for (BookPage p : pages) ids.add(p.id());
        ADDED.put(book, List.copyOf(ids));
        return pages;
    }

    // ---------------------------------------------------------------- Yuno Grinberryall (Wind Magic + Wind Spirit Magic)
    private static final List<BookPage> WIND = register("book_wind", List.of(
            BookPage.mid("tornado_fang", "Tornado Fang", CharacterArts::tornadoFang).withCooldown(200),
            BookPage.zone("sylphs_breath", "Sylph's Breath", CharacterArts::sylphsBreath).withCooldown(500),
            BookPage.zone("spirits_hushed_dance", "Spirit's Hushed Dance", CharacterArts::hushedDance).withCooldown(700),
            BookPage.zone("liable_tempest", "Liable Tempest", CharacterArts::liableTempest).withCooldown(700),
            BookPage.mid("gale_white_bow", "Gale White Bow", CharacterArts::galeWhiteBow).withCooldown(240),
            BookPage.mid("swift_white_hawk", "Swift White Hawk", CharacterArts::swiftWhiteHawk).withCooldown(260),
            BookPage.zone("heavenly_wind_ark", "Heavenly Wind Ark", CharacterArts::heavenlyWindArk).withCooldown(900),
            BookPage.mid("wind_blades_shower", "Wind Blades Shower", CharacterArts::windBladesShower).withCooldown(300),
            BookPage.signature("spirit_of_boreas", "Spirit of Boreas", CharacterArts::spiritOfBoreas).withCooldown(900)));

    // ---------------------------------------------------------------- Noelle Silva (Water Magic)
    private static final List<BookPage> WATER = register("book_water", List.of(
            BookPage.signature("point_blank_roar", "Point-Blank Sea Dragon's Roar", CharacterArts::pointBlankRoar).withCooldown(700)));

    // ---------------------------------------------------------------- Fuegoleon Vermillion (Flame Magic + Salamander)
    private static final List<BookPage> FIRE = register("book_fire", List.of(
            BookPage.mid("leo_parma", "Leo Parma", CharacterArts::leoParma).withCooldown(300),
            BookPage.zone("salamanders_breath", "Salamander's Breath", CharacterArts::salamandersBreath).withCooldown(500)));

    // ---------------------------------------------------------------- Yami Sukehiro (Dark Magic)
    private static final List<BookPage> DARK = register("book_dark", List.of(
            BookPage.mid("deep_black_blade", "Dark Cloaked Deep Black Blade", CharacterArts::deepBlackBlade).withCooldown(1000),
            BookPage.signature("black_crescent_moon", "Black Crescent Moon", CharacterArts::blackCrescentMoon).withCooldown(700)));

    public static List<BookPage> wind(List<BookPage> base) { return append(base, WIND); }
    public static List<BookPage> water(List<BookPage> base) { return append(base, WATER); }
    public static List<BookPage> fire(List<BookPage> base) { return append(base, FIRE); }
    public static List<BookPage> dark(List<BookPage> base) { return append(base, DARK); }

    /** The base list followed by the appended pages; the result is a new immutable list. */
    public static List<BookPage> append(List<BookPage> base, List<BookPage> extra) {
        List<BookPage> out = new ArrayList<>(base);
        out.addAll(extra);
        return List.copyOf(out);
    }

    /** Was this page appended by this class (a page that belongs to one character from the day it exists)? */
    public static boolean isAdded(String bookPath, String pageId) {
        List<String> l = ADDED.get(bookPath);
        return (l != null && l.contains(pageId)) || CharacterPagesRest.isAdded(bookPath, pageId);
    }

    /** The ids appended to a book (for the self-test and docs). */
    public static List<String> addedTo(String bookPath) { return ADDED.getOrDefault(bookPath, List.of()); }
}
