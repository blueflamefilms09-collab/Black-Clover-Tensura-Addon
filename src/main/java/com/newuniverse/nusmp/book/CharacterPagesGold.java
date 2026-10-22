package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Golden Dawn / Crimson Lion / Silver Eagle / Wizard King side of the anime cast (builder key "gold", docs/magic/character_spells_gold.md) used spells the shared element books never had
 * (Mereoleona's Purgatory, Leopold's Deep Spiral Flame, Mimosa's Flower Princess Utopia, William's Great Tree Mistilteinn, Klaus's Spiral Lance, Langris's Archangel's Crash, Karna's Lunatic Crisis,
 * Kaiser's Area Helix). They are APPENDED to the end of each book's family list (rule 2: no page id, name or mode number moves, so every save, unlock bit and mastery stays) and each belongs to one
 * character: the id carries the character's name and the data file marks it exclusive, so another grimoire never opens it, with the character-spell-set switch off too. The casts are in
 * {@link CharacterArtsGold}. This class is the same pattern as {@link CharacterPages} (the pilots) in its own file so that the three content builders never edit the same lines.
 *
 * <p>The tiers (cost, cooldown) follow the usual page tiers; the rank gates come from those tiers as for every other page.
 */
public final class CharacterPagesGold {
    private CharacterPagesGold() {}

    /** Book registry path -> the page ids this class appended, in order. */
    private static final Map<String, List<String>> ADDED = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        List<String> ids = new ArrayList<>();
        for (BookPage p : pages) ids.add(p.id());
        ADDED.put(book, List.copyOf(ids));
        return pages;
    }

    // ---------------------------------------------------------------- Mereoleona Vermillion (fists) and Leopold Vermillion (spirals): Flame Magic
    private static final List<BookPage> FIRE = register("book_fire", List.of(
            BookPage.mid("mereoleona_brachium_barrage", "Calidus Brachium Barrage", CharacterArtsGold::brachiumBarrage).withCooldown(300).withAnim("thrust"),
            BookPage.zone("mereoleona_purgatory", "Calidus Brachium Purgatory", CharacterArtsGold::purgatory).withCooldown(1200).withAnim("slam"),
            BookPage.signature("mereoleona_purgatory_abyss", "Calidus Brachium Purgatory: Abyss", CharacterArtsGold::purgatoryAbyss).withCooldown(1500).withAnim("signature"),
            BookPage.signature("mereoleona_hellfire_incarnate", "Hellfire Incarnate", CharacterArtsGold::hellfireIncarnate).withCooldown(2400).withAnim("signature"),
            BookPage.zone("leopold_deep_spiral_flame", "Deep Spiral Flame", CharacterArtsGold::deepSpiralFlame).withCooldown(500).withAnim("slam"),
            BookPage.signature("leopold_crimson_eruption", "True Flame Magic: Crimson Eruption", CharacterArtsGold::crimsonEruption).withCooldown(1000).withAnim("signature")));

    // ---------------------------------------------------------------- Mimosa Vermillion: Plant Magic
    private static final List<BookPage> PLANT = register("book_plant", List.of(
            BookPage.mid("mimosa_magic_flower_cannon", "Magic Flower Cannon", CharacterArtsGold::flowerCannon).withCooldown(260).withAnim("thrust"),
            BookPage.mid("mimosa_dream_healing_cradle", "Dream-Healing Flower Cradle", CharacterArtsGold::healingCradle).withCooldown(400).withAnim("up"),
            BookPage.mid("mimosa_princess_healing_robe", "Princess-Healing Flower Robe", CharacterArtsGold::healingRobe).withCooldown(600).withAnim("up"),
            BookPage.signature("mimosa_flower_princess_utopia", "Flower Princess Utopia", CharacterArtsGold::princessUtopia).withCooldown(1800).withAnim("signature")));

    // ---------------------------------------------------------------- William Vangeance: World Tree Magic
    private static final List<BookPage> WORLD_TREE = register("book_world_tree", List.of(
            BookPage.mid("william_magic_warding_ash", "Magic Warding Ash", CharacterArtsGold::wardingAsh).withCooldown(500).withAnim("slam"),
            BookPage.zone("william_great_tree_mistilteinn", "Great Tree Mistilteinn", CharacterArtsGold::greatTreeMistilteinn).withCooldown(900).withAnim("slam"),
            BookPage.mid("william_mistilteinn_blade", "Mistilteinn Blade", CharacterArtsGold::mistilteinnBlade).withCooldown(200).withAnim("sweep")));

    // ---------------------------------------------------------------- Klaus Lunettes: Steel Magic
    private static final List<BookPage> STEEL = register("book_steel", List.of(
            BookPage.mid("klaus_piercing_lance", "Blazing Piercing Lance", CharacterArtsGold::piercingLance).withCooldown(200).withAnim("thrust"),
            BookPage.mid("klaus_castle_wall", "Steel Castle's Armored Wall", CharacterArtsGold::castleWall).withCooldown(500).withAnim("slam"),
            BookPage.zone("klaus_spiral_lance", "Violent Spiral Lance", CharacterArtsGold::spiralLance).withCooldown(400).withAnim("thrust"),
            BookPage.signature("klaus_thunder_arrow", "Thunder Arrow", CharacterArtsGold::thunderArrow).withCooldown(900).withAnim("signature")));

    // ---------------------------------------------------------------- Langris Vaude: Spatial Magic
    private static final List<BookPage> SPATIAL = register("book_spatial", List.of(
            BookPage.mid("langris_archangel_crash", "Archangel's Crash", CharacterArtsGold::archangelCrash).withCooldown(240).withAnim("sweep"),
            BookPage.signature("langris_archangel_annihilation", "Archangel's Annihilation", CharacterArtsGold::archangelAnnihilation).withCooldown(1000).withAnim("signature"),
            BookPage.mid("langris_spatial_auto_guard", "Spatial Auto-Guard", CharacterArtsGold::spatialAutoGuard).withCooldown(600).withAnim("up")));

    // ---------------------------------------------------------------- Karna Freese: Moonlight Magic (drawn in the Light book: Moonlight has no book of its own)
    private static final List<BookPage> LIGHT = register("book_light", List.of(
            BookPage.mid("karna_crescent_cut", "Crescent Cut", CharacterArtsGold::crescentCut).withCooldown(200).withAnim("sweep"),
            BookPage.zone("karna_bewitching_moonlight", "Bewitching Moonlight", CharacterArtsGold::bewitchingMoonlight).withCooldown(700).withAnim("out"),
            BookPage.mid("karna_full_moon_shell", "Full Moon Shell", CharacterArtsGold::fullMoonShell).withCooldown(500).withAnim("up"),
            BookPage.zone("karna_crescent_cut_full_moon", "Crescent Cut Full Moon", CharacterArtsGold::crescentCutFullMoon).withCooldown(700).withAnim("slam"),
            BookPage.signature("karna_lunatic_crisis", "Lunatic Crisis", CharacterArtsGold::lunaticCrisis).withCooldown(1800).withAnim("signature")));

    // ---------------------------------------------------------------- Kaiser Granvorka: Vortex Magic (drawn in the Storm book, the book his canon grimoire is bound to)
    private static final List<BookPage> STORM = register("book_storm", List.of(
            BookPage.signature("kaiser_area_helix", "Vortex Magic: Area Helix", CharacterArtsGold::areaHelix).withCooldown(1200).withAnim("signature")));

    public static List<BookPage> fire(List<BookPage> base) { return append(base, FIRE); }
    public static List<BookPage> plant(List<BookPage> base) { return append(base, PLANT); }
    public static List<BookPage> worldTree(List<BookPage> base) { return append(base, WORLD_TREE); }
    public static List<BookPage> steel(List<BookPage> base) { return append(base, STEEL); }
    public static List<BookPage> spatial(List<BookPage> base) { return append(base, SPATIAL); }
    public static List<BookPage> light(List<BookPage> base) { return append(base, LIGHT); }
    public static List<BookPage> storm(List<BookPage> base) { return append(base, STORM); }

    /** The base list followed by the appended pages; the result is a new immutable list. */
    public static List<BookPage> append(List<BookPage> base, List<BookPage> extra) {
        List<BookPage> out = new ArrayList<>(base);
        out.addAll(extra);
        return List.copyOf(out);
    }

    /** Was this page appended by this class (a page that belongs to one character from the day it exists)? */
    public static boolean isAdded(String bookPath, String pageId) {
        List<String> l = ADDED.get(bookPath);
        return l != null && l.contains(pageId);
    }

    /** The ids appended to a book (for the self-test and docs). */
    public static List<String> addedTo(String bookPath) { return ADDED.getOrDefault(bookPath, List.of()); }

    /** The books this class appends to (for the self-test and docs). */
    public static java.util.Set<String> books() { return ADDED.keySet(); }
}
