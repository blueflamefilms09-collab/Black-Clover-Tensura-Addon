package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the basic grimoire (bound to nobody) should be built on the anime but differ from the characters' spells (docs/magic/basic_grimoires.md). The audit hid a few signature spells from it
 * ({@code charset.BasicSignatures}) and found some books thin, so the generic spells of ordinary mages and minor named characters of the anime are APPENDED here, after every other page of the
 * book (rule 2: no page id, name or mode number moves, so every save, unlock bit and mastery stays). None of them belongs to a character: they have no owner, no lock and no set lists them, so
 * every grimoire of the book can open them by the usual mastery order, and a character's grimoire shows them only when its set lists them (it never does for these). The casts are in {@link BasicArts}.
 *
 * <p>The hook is one place, {@code GrimoireBook.familyAll()}, which asks {@link #append} last of all (after the character pages), so a book class needs no edit. The family page budget
 * is 32 pages per grimoire (the unlock mask has 64 bits, {@link UnlockMask}); {@code charset.BasicGrimoiresTest} counts every book's family pages from the sources and fails above 32. The tiers (cost, cooldown) follow the usual page tiers.
 */
public final class BasicPages {
    private BasicPages() {}

    /** Book registry path -> the page ids this class appended, in order. */
    private static final Map<String, List<String>> ADDED = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        List<String> ids = new ArrayList<>();
        for (BookPage p : pages) ids.add(p.id());
        ADDED.put(book, List.copyOf(ids));
        return pages;
    }

    // ---------------------------------------------------------------- Flame Magic (the Fire book): minor Crimson Lion / Spade fire mages
    private static final List<BookPage> FIRE = register("book_fire", List.of(
            BookPage.mid("shining_flame_cross", "Shining Flame Cross", BasicArts::shiningFlameCross).withCooldown(200).withAnim("slam"),
            BookPage.mid("combo_eruption", "Combo Eruption", BasicArts::comboEruption).withCooldown(260).withAnim("sweep")));

    // ---------------------------------------------------------------- Water Magic: Rhya, Selena Silva, Solid Silva
    private static final List<BookPage> WATER = register("book_water", List.of(
            BookPage.mid("aqua_curtain", "Aqua Curtain", BasicArts::aquaCurtain).withCooldown(400).withAnim("up"),
            BookPage.mid("cascade_destroyer", "Cascade Destroyer", BasicArts::cascadeDestroyer).withCooldown(260).withAnim("thrust"),
            BookPage.mid("holy_water_assassination_bullet", "Holy Water Assassination Bullet", BasicArts::holyWaterBullet).withCooldown(300).withAnim("thrust")));

    // ---------------------------------------------------------------- Wind Magic: Fanzell Kruger, Jozou Hanegatsuji, George
    private static final List<BookPage> WIND = register("book_wind", List.of(
            BookPage.mid("slicing_wind_emperor_wintry_wind", "Slicing Wind Emperor: Wintry Wind", BasicArts::wintryWind).withCooldown(240).withAnim("sweep"),
            BookPage.mid("slicing_wind_emperor_gale", "Slicing Wind Emperor: Gale", BasicArts::galeDash).withCooldown(220).withAnim("thrust"),
            BookPage.mid("slicing_wind_emperor_mountain_retreat", "Slicing Wind Emperor: Mountain Retreat", BasicArts::mountainRetreat).withCooldown(260).withAnim("out"),
            BookPage.mid("slicing_wind_emperor_whirlwind", "Slicing Wind Emperor: Whirlwind", BasicArts::whirlwind).withCooldown(400).withAnim("sweep"),
            BookPage.mid("killing_kakara_blade", "Killing Kakara Blade", BasicArts::killingKakaraBlade).withCooldown(220).withAnim("sweep"),
            BookPage.mid("piercing_tornado_needle", "Piercing Tornado Needle", BasicArts::piercingTornadoNeedle).withCooldown(240).withAnim("thrust")));

    // ---------------------------------------------------------------- Dark Magic: Ichika Yami
    private static final List<BookPage> DARK = register("book_dark", List.of(
            BookPage.mid("black_star", "Black Star", BasicArts::blackStar).withCooldown(260).withAnim("up")));

    // ---------------------------------------------------------------- Ice Magic: Heath Graice
    private static final List<BookPage> ICE = register("book_ice", List.of(
            BookPage.mid("heavenly_ice_fang", "Heavenly Ice Fang", BasicArts::heavenlyIceFang).withCooldown(240).withAnim("slam"),
            BookPage.mid("icicle_bridge", "Icicle Bridge", BasicArts::icicleBridge).withCooldown(260).withAnim("thrust"),
            BookPage.mid("dissolve", "Dissolve", BasicArts::dissolve).withCooldown(320).withAnim("up"),
            BookPage.mid("crystal_ice_shield", "Crystal Ice Shield", BasicArts::crystalIceShield).withCooldown(500).withAnim("up")));

    // ---------------------------------------------------------------- Beast Magic: Vetto
    private static final List<BookPage> BEAST = register("book_beast", List.of(
            BookPage.mid("bear_claw", "Bear Claw", BasicArts::bearClaw).withCooldown(200).withAnim("thrust"),
            BookPage.mid("cheetah_charge", "Cheetah Charge", BasicArts::cheetahCharge).withCooldown(260).withAnim("thrust"),
            BookPage.mid("rhinoceros_armor", "Rhinoceros Armor", BasicArts::rhinocerosArmor).withCooldown(500).withAnim("up")));

    private static List<BookPage> pagesOf(String bookPath) {
        return switch (bookPath) {
            case "book_fire" -> FIRE;
            case "book_water" -> WATER;
            case "book_wind" -> WIND;
            case "book_dark" -> DARK;
            case "book_ice" -> ICE;
            case "book_beast" -> BEAST;
            default -> List.of();
        };
    }

    /** The book's family list followed by the pages this class appends to it. A book with nothing added gets its list back unchanged (the same object). */
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

    /** Was this page appended by this class? */
    public static boolean isAdded(String bookPath, String pageId) {
        List<String> l = ADDED.get(bookPath);
        return l != null && l.contains(pageId);
    }

    /** The ids appended to a book, in order (for the self-test and docs). */
    public static List<String> addedTo(String bookPath) { return ADDED.getOrDefault(bookPath, List.of()); }

    /** Every book this class adds pages to. */
    public static List<String> books() { return List.copyOf(ADDED.keySet()); }
}
