package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the character spell sets of the squad captains, the Spade Kingdom, the Heart Kingdom, Diamond and the Seabed Temple (docs/magic/character_spells_rest.md) use a few canon spells the shared
 * element books never had (Charlotte's Blue Rose Paradise, Rill's Four-Headed Lindworm, Vanica's Red Beast, Gadjah's Tenjiolia ...). They are APPENDED after the end of the book's family list
 * (rule 2: no page id, name or mode number moves, so every save, unlock bit and mastery stays) and each belongs to one character: it is exclusive in that character's data file, so another
 * grimoire never opens it, with the character-spell-set switch off too. The casts are in {@link CharacterArtsRest}.
 *
 * <p>The hook is one place, {@code GrimoireBook.familyAll()}, which asks {@link #append} for every book (a book with nothing added is returned unchanged), so a book class needs no edit and a
 * book that other builders also extend ({@link CharacterPages}) simply gets these pages after theirs.
 */
public final class CharacterPagesRest {
    private CharacterPagesRest() {}

    /** Book registry path -> the page ids this class appended, in order. */
    private static final Map<String, List<String>> ADDED = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        List<String> ids = new ArrayList<>();
        for (BookPage p : pages) ids.add(p.id());
        ADDED.put(book, List.copyOf(ids));
        return pages;
    }

    // ---------------------------------------------------------------- Charlotte Roselei (Briar Magic: the True Briar spells)
    private static final List<BookPage> BRIAR = register("book_briar", List.of(
            BookPage.zone("blue_rose_paradise", "Briar Magic: Blue Rose Paradise", CharacterArtsRest::blueRoseParadise).withCooldown(600).withAnim("slam"),
            BookPage.mid("scarlet_braided_spear", "Briar Magic: Scarlet-Braided Beautiful Battle Spear", CharacterArtsRest::scarletBraidedSpear).withCooldown(260).withAnim("thrust")));

    // ---------------------------------------------------------------- Rill Boismortier (Painting Magic)
    private static final List<BookPage> PAINTING = register("book_painting", List.of(
            BookPage.mid("souterrain_groundswell", "Painting Magic: Souterrain Groundswell", CharacterArtsRest::souterrainGroundswell).withCooldown(240).withAnim("sweep"),
            BookPage.mid("sirenes_game", "Painting Magic: Sirene's Game", CharacterArtsRest::sirenesGame).withCooldown(240).withAnim("sweep"),
            BookPage.zone("four_headed_lindworm", "Painting Magic: Four-Headed Lindworm", CharacterArtsRest::fourHeadedLindworm).withCooldown(500).withAnim("sweep"),
            BookPage.zone("vouivres_shout", "Painting Magic: Phantom Dragon Vouivre's Shout", CharacterArtsRest::vouivresShout).withCooldown(500).withAnim("sweep"),
            BookPage.zone("souterrain_giant", "Painting Magic: Souterrain Giant", CharacterArtsRest::souterrainGiant).withCooldown(600).withAnim("sweep"),
            BookPage.signature("twilight_of_valhalla", "Painting Magic: Twilight of Valhalla", CharacterArtsRest::twilightOfValhalla).withCooldown(1500).withAnim("sweep")));

    // ---------------------------------------------------------------- Vanica Zogratis (Blood Magic)
    private static final List<BookPage> BLOOD = register("book_blood", List.of(
            BookPage.mid("red_beast", "Blood Magic: Red Beast", CharacterArtsRest::redBeast).withCooldown(500),
            BookPage.zone("herd_of_red_beasts", "Blood Magic: Herd of Red Beasts", CharacterArtsRest::herdOfRedBeasts).withCooldown(700)));

    // ---------------------------------------------------------------- Gadjah (Lightning Magic, the Heart Kingdom's True Lightning)
    private static final List<BookPage> LIGHTNING = register("book_lightning", List.of(
            BookPage.mid("tenjiolia", "Lightning Magic: Tenjiolia", CharacterArtsRest::tenjiolia).withCooldown(200).withAnim("thrust"),
            BookPage.signature("apocalypra_astrauza", "Lightning Ultimate Magic: Apocalypra Astrauza", CharacterArtsRest::apocalypraAstrauza).withCooldown(1800).withAnim("thrust")));

    // ---------------------------------------------------------------- Mars (Mineral Magic)
    private static final List<BookPage> MINERAL = register("book_mineral", List.of(
            BookPage.signature("laevateinn", "Mineral Creation Magic: Laevateinn", CharacterArtsRest::laevateinn).withCooldown(900).withAnim("slam")));

    // ---------------------------------------------------------------- Kahono (Song Magic)
    private static final List<BookPage> SONG = register("book_song", List.of(
            BookPage.mid("heal_lullaby", "Song Magic: Heal Lullaby", CharacterArtsRest::healLullaby).withCooldown(400).withAnim("up"),
            BookPage.mid("mother_lullaby", "Song Magic: Mother Lullaby", CharacterArtsRest::motherLullaby).withCooldown(500).withAnim("up")));

    private static List<BookPage> pagesOf(String bookPath) {
        return switch (bookPath) {
            case "book_briar" -> BRIAR;
            case "book_painting" -> PAINTING;
            case "book_blood" -> BLOOD;
            case "book_lightning" -> LIGHTNING;
            case "book_mineral" -> MINERAL;
            case "book_song" -> SONG;
            default -> List.of();
        };
    }

    /** The book's own family list followed by the pages this class appends to it. A book with nothing added gets its list back unchanged (the same object). */
    public static List<BookPage> append(String bookPath, List<BookPage> base) {
        List<BookPage> extra = pagesOf(bookPath == null ? "" : bookPath);
        if (extra.isEmpty()) return base;
        List<BookPage> out = new ArrayList<>(base);
        for (BookPage p : extra) {
            boolean there = false;
            for (BookPage q : base) if (q.id().equals(p.id())) { there = true; break; }      // never twice (a book that already has the id keeps its own)
            if (!there) out.add(p);
        }
        return List.copyOf(out);
    }

    /** Was this page appended by this class (a page that belongs to one character from the day it exists)? */
    public static boolean isAdded(String bookPath, String pageId) {
        List<String> l = ADDED.get(bookPath);
        return l != null && l.contains(pageId);
    }

    /** The ids appended to a book (for the self-test and docs). */
    public static List<String> addedTo(String bookPath) { return ADDED.getOrDefault(bookPath, List.of()); }

    /** Every book this class adds pages to. */
    public static List<String> books() { return List.copyOf(ADDED.keySet()); }
}
