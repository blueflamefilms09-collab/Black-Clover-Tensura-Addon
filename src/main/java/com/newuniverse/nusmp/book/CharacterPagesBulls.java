package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Black Bulls (docs/magic/character_spells_bulls.md) used spells in the anime that the shared element books had no page for (Luck's Keraunos, Magna's Grand Slam, Gauche's Double Reflect
 * Refrain ...). They are APPENDED to the end of the book's family list (rule 2: no page id, name or mode number moves, so every save, unlock bit and mastery stays), and each belongs to one
 * character: it is exclusive to that character's data file, so another grimoire never opens it, with the character-spell-set switch off too. The casts are in {@link CharacterArtsBulls}.
 *
 * <p>This class is the Black Bulls' own twin of {@link CharacterPages} (separate so the content builders of the other squads never touch the same lines). The books hook it in with one cached
 * line each: the books that are plain {@code ElementBook}s through {@code ElementBook.familyPages} (by magic name), the others in their own {@code familyPages}.
 */
public final class CharacterPagesBulls {
    private CharacterPagesBulls() {}

    /** Book registry path -> the page ids this class appended, in order. */
    private static final Map<String, List<String>> ADDED = new LinkedHashMap<>();
    /** MagicType name -> book registry path, for the books built from ElementBook. */
    private static final Map<String, String> BOOK_OF_MAGIC = new LinkedHashMap<>();

    private static List<BookPage> register(String book, String magic, List<BookPage> pages) {
        List<String> ids = new ArrayList<>();
        for (BookPage p : pages) ids.add(p.id());
        ADDED.put(book, List.copyOf(ids));
        if (magic != null) BOOK_OF_MAGIC.put(magic, book);
        return pages;
    }

    // ---------------------------------------------------------------- Charmy Pappitson (Cotton Magic)
    private static final List<BookPage> COTTON = register("book_cotton", "COTTON", List.of(
            BookPage.mid("fluffy_sheep_cushion", "Fluffy Sheep Cushion", CharacterArtsBulls::fluffySheepCushion).withCooldown(300)));

    // ---------------------------------------------------------------- Luck Voltia (Lightning Magic)
    private static final List<BookPage> LIGHTNING = register("book_lightning", null, List.of(
            BookPage.mid("thunderclap_crumbling_orb", "Thunderclap Crumbling Orb", CharacterArtsBulls::thunderclapOrb).withCooldown(160),
            BookPage.mid("thunder_gods_gloves", "Thunder God's Gloves", CharacterArtsBulls::thunderGodsGloves).withCooldown(600),
            BookPage.signature("keraunos", "Keraunos", CharacterArtsBulls::keraunos).withCooldown(700).withAnim("out")));

    // ---------------------------------------------------------------- Magna Swing (Flame Magic; the Soul Bat casting of firecast wraps every one of these)
    private static final List<BookPage> FIRE = register("book_fire", null, List.of(
            BookPage.mid("exploding_buckshot", "Exploding Buckshot", CharacterArtsBulls::explodingBuckshot).withCooldown(200),
            BookPage.mid("grand_slam", "Grand Slam", CharacterArtsBulls::grandSlam).withCooldown(260),
            BookPage.signature("soul_chain_deathmatch", "Soul Chain Tag Deathmatch", CharacterArtsBulls::soulChainDeathmatch).withCooldown(1500)));

    // ---------------------------------------------------------------- Gauche Adlai (Mirror Magic)
    private static final List<BookPage> MIRROR = register("book_mirror", null, List.of(
            BookPage.zone("double_reflect_refrain", "Double Reflect Refrain", CharacterArtsBulls::doubleReflectRefrain).withCooldown(400),
            BookPage.zone("mirrors_brigade", "Mirrors Brigade", CharacterArtsBulls::mirrorsBrigade).withCooldown(700)));

    // ---------------------------------------------------------------- Gordon Agrippa (Poison Magic)
    private static final List<BookPage> POISON = register("book_poison", "POISON", List.of(
            BookPage.mid("verbotenes_obst", "Verbotenes Obst", CharacterArtsBulls::verbotenesObst).withCooldown(240),
            BookPage.zone("poison_clouds_abode", "Poison Cloud's Abode", CharacterArtsBulls::poisonCloudsAbode).withCooldown(700)));

    // ---------------------------------------------------------------- Zora Ideale (Ash Magic: Trap Magic)
    private static final List<BookPage> ASH = register("book_ash", "ASH", List.of(
            BookPage.mid("ash_snare_trap", "Ash Snare Trap", CharacterArtsBulls::ashSnareTrap).withCooldown(300),
            BookPage.signature("counter_trap_magic", "Counter Trap Magic", CharacterArtsBulls::counterTrapMagic).withCooldown(900)));

    // ---------------------------------------------------------------- Grey (Transformation / Transmutation)
    private static final List<BookPage> TRANSMUTATION = register("book_transmutation", "TRANSMUTATION", List.of(
            BookPage.mid("transformation_guise", "Transformation: Borrowed Form", CharacterArtsBulls::transformationGuise).withCooldown(800)));

    // ---------------------------------------------------------------- Henry Legolant (Recombination Magic)
    private static final List<BookPage> RECOMBINATION = register("book_recombination", "RECOMBINATION", List.of(
            BookPage.mid("mana_rocket_punch", "Mana Rocket Punch", CharacterArtsBulls::manaRocketPunch).withCooldown(300),
            BookPage.mid("mini_bull", "The Mini Bull", CharacterArtsBulls::miniBull).withCooldown(400)));

    // ---------------------------------------------------------------- Finral Roulacase (Spatial Magic)
    private static final List<BookPage> SPATIAL = register("book_spatial", null, List.of(
            BookPage.mid("fallen_angel_wingbeat", "Fallen Angel Wingbeat", CharacterArtsBulls::fallenAngelWingbeat).withCooldown(260).withAnim("out")));

    // ---------------------------------------------------------------- Nacht Faust (Shadow Magic)
    private static final List<BookPage> SHADOW = register("book_shadow", "SHADOW", List.of(
            BookPage.zone("dark_hunting_ground", "Dark Hunting Ground", CharacterArtsBulls::darkHuntingGround).withCooldown(600),
            BookPage.signature("atonement_monument", "Atonement Monument", CharacterArtsBulls::atonementMonument).withCooldown(1800)));

    // ---------------------------------------------------------------- Secre Swallowtail (Sealing Magic)
    private static final List<BookPage> SEALING = register("book_sealing", null, List.of(
            BookPage.mid("inverse_release", "Seal Magic: Inverse Release", CharacterArtsBulls::inverseRelease).withCooldown(300).withAnim("seal_crush")));

    public static List<BookPage> lightning(List<BookPage> base) { return CharacterPages.append(base, LIGHTNING); }
    public static List<BookPage> fire(List<BookPage> base) { return CharacterPages.append(base, FIRE); }
    public static List<BookPage> mirror(List<BookPage> base) { return CharacterPages.append(base, MIRROR); }
    public static List<BookPage> spatial(List<BookPage> base) { return CharacterPages.append(base, SPATIAL); }
    public static List<BookPage> sealing(List<BookPage> base) { return CharacterPages.append(base, SEALING); }

    /** For an ElementBook (Cotton, Poison, Ash, Transmutation, Recombination, Shadow): the base list followed by this squad's pages for that magic; the same list back for any other magic. */
    public static List<BookPage> forMagic(String magicName, List<BookPage> base) {
        String book = BOOK_OF_MAGIC.get(magicName);
        if (book == null) return base;
        return CharacterPages.append(base, switch (book) {
            case "book_cotton" -> COTTON;
            case "book_poison" -> POISON;
            case "book_ash" -> ASH;
            case "book_transmutation" -> TRANSMUTATION;
            case "book_recombination" -> RECOMBINATION;
            default -> SHADOW;
        });
    }

    /** Was this page appended by this class (a page that belongs to one character from the day it exists)? */
    public static boolean isAdded(String bookPath, String pageId) {
        List<String> l = ADDED.get(bookPath);
        return l != null && l.contains(pageId);
    }

    /** The ids appended to a book (for the self-test and docs). */
    public static List<String> addedTo(String bookPath) { return ADDED.getOrDefault(bookPath, List.of()); }

    /** Every book this class appended to, in order. */
    public static List<String> books() { return List.copyOf(ADDED.keySet()); }
}
