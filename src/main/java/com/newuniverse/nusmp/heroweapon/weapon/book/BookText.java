package com.newuniverse.nusmp.heroweapon.weapon.book;

import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.n;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.pct;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.sec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the names and descriptions of the Book's techniques, pages and passives are read in two places that must agree, the skill objects (the tooltip and the
 * Weapon Book read them) and the language file ({@code hero.skill.<id>.name}, {@code .desc}, {@code hero.passive.book.<id>} and {@code .desc}), so they are
 * written once, here, with no game imports, and the numbers in the sentences are the constants the skills use. The names of the pages (which are not skills and
 * so have no language key of their own; the action bar names them as they are read) live here too. A test holds the words to the house rules and a small
 * program writes the language fragment from this table.
 */
public final class BookText {
    private BookText() {}

    /** {id, name, description} of every skill, in the order the weapon lists them. */
    private static final String[][] SKILLS = {
            {BookNumbers.READ_COMPOSITION, "Read Composition",
                    "Reads the next page of the offence chapter, in order: a Bird of Flame (" + n(BookNumbers.BIRD_RAW) + " damage along a line of " + n(BookNumbers.BIRD_RANGE)
                            + " blocks), a Needle Rain (" + BookNumbers.NEEDLE_PULSES + " volleys of " + n(BookNumbers.NEEDLE_RAW) + " on a circle of " + n(BookNumbers.NEEDLE_RADIUS)
                            + " blocks) and, with the Feather page, a beam of " + n(BookNumbers.FEATHER_RAW) + " damage. A page that finds no target is not read."},
            {BookNumbers.READ_VERSE, "Read Verse",
                    "Reads the next page of the support chapter, in order: a Paper Wall (absorption for you and friends within " + n(BookNumbers.WALL_RADIUS) + " blocks), a Healing Verse ("
                            + BookNumbers.VERSE_PULSES + " pulses, together at most " + pct(BookNumbers.VERSE_CAP) + " percent of a friend's health) and, with the Binding page, a Constriction."},
            {BookNumbers.TURN_PAGE, "Turn the Page",
                    "Turns past the next page of the chapter you read last without reading it, so another page comes up. A skipped page is still unread, so the volume is not complete until it is read."},
            {BookNumbers.PAPER_BURST, "Paper Splits Rock",
                    "The page you aim at lights up (a ring shows where) and " + BookNumbers.BURST_DELAY + " ticks later explodes: foes within " + n(BookNumbers.BURST_RADIUS)
                            + " blocks take " + n(BookNumbers.BURST_RAW) + " damage and are pushed away. Counter: leave the ring."},
            {BookNumbers.LIFE_FORCE_BURST, "Life Force Burst",
                    "Pages wrap the foe on your crosshair; if it stays within " + n(BookNumbers.LIFE_LEASH) + " blocks of where it was wrapped for " + sec(BookNumbers.LIFE_DELAY)
                            + " seconds, the trapped life force bursts for " + n(BookNumbers.LIFE_RAW) + " damage. Counter: run before the burst."},
            {BookNumbers.CONSTRICTION, "Constriction",
                    "Pages close into a cage round the foe on your crosshair (within " + n(BookNumbers.CAGE_RANGE) + " blocks): it is rooted for up to " + sec(BookNumbers.CAGE_ROOT)
                            + " seconds, then needles fall through the False Ceiling for " + n(BookNumbers.CAGE_CEILING_RAW) + " damage. It is hard control and shares the 12 second control rest."},
            {BookNumbers.LIBRARIA, "Libraria",
                    "Ultimate. Needs a complete volume (every page of both chapters read). The room turns white and in " + BookNumbers.LIB_VOLLEYS + " volleys birds of fire, lightning, boulders and beams fall on every foe within "
                            + n(BookNumbers.LIB_RADIUS) + " blocks for " + n(BookNumbers.LIB_RAW) + " damage each (" + n(BookNumbers.LIB_RAW * BookNumbers.LIB_VOLLEYS) + " in all). Counter: leave the white room."},
    };

    /** {id, name, description} of every passive a form of the Book may list. */
    private static final String[][] PASSIVES = {
            {BookNumbers.P_CHAPTERS, "Two Chapters",
                    "The book holds two chapters read in order: use reads the offence chapter, sneak and use the support chapter. Reading every page of both completes the volume and opens Libraria."},
            {BookNumbers.P_DECK_FEATHERS, "Feather Page",
                    "Adds the Feather Volley to the offence chapter: a beam of " + n(BookNumbers.FEATHER_RANGE) + " blocks for " + n(BookNumbers.FEATHER_RAW) + " damage that mends you a little."},
            {BookNumbers.P_DECK_CAGE, "Binding Page", "Adds the Constriction to the support chapter."},
            {BookNumbers.P_REVISED, "Revised Edition", "Every page and burst is " + pct(BookNumbers.REVISED - 1.0) + " percent stronger, within the limits of its kind."},
            {BookNumbers.P_GRAND, "Grand Library",
                    "Libraria falls in " + BookNumbers.LIB_VOLLEYS_GRAND + " volleys, and the volume needs all but one page."},
    };

    /** {id, name} of every page of the chapters. */
    private static final String[][] PAGES = {
            {BookNumbers.BIRD, "Bird of Flame"}, {BookNumbers.NEEDLES, "Needle Rain"}, {BookNumbers.FEATHERS, "Feather Volley"},
            {BookNumbers.WALL, "Paper Wall"}, {BookNumbers.VERSE, "Healing Verse"}, {BookNumbers.CAGE, "Constriction"},
    };

    private static final Map<String, String[]> SKILL_MAP = table(SKILLS), PASSIVE_MAP = table(PASSIVES);
    private static final Map<String, String> PAGE_MAP = pages();

    public static List<String> skillIds() { return List.copyOf(SKILL_MAP.keySet()); }

    public static List<String> passiveIds() { return List.copyOf(PASSIVE_MAP.keySet()); }

    public static List<String> pageIds() { return List.copyOf(PAGE_MAP.keySet()); }

    public static String skillName(String id) { return SKILL_MAP.get(id)[0]; }

    public static String skillDesc(String id) { return SKILL_MAP.get(id)[1]; }

    public static String passiveName(String id) { return PASSIVE_MAP.get(id)[0]; }

    public static String passiveDesc(String id) { return PASSIVE_MAP.get(id)[1]; }

    /** The name of a page, or the id itself for an unknown page. */
    public static String pageName(String id) { return id == null ? "nothing" : PAGE_MAP.getOrDefault(id, id); }

    private static Map<String, String[]> table(String[][] rows) {
        Map<String, String[]> m = new LinkedHashMap<>();
        for (String[] r : rows) m.put(r[0], new String[] {r[1], r[2]});
        return m;
    }

    private static Map<String, String> pages() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String[] r : PAGES) m.put(r[0], r[1]);
        return m;
    }

    /** The language entries of the Book: skill and passive names and descriptions, keyed as the lang file wants them. */
    public static Map<String, String> lang() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String[] r : SKILLS) {
            out.put("hero.skill." + r[0] + ".name", r[1]);
            out.put("hero.skill." + r[0] + ".desc", r[2]);
        }
        for (String[] r : PASSIVES) {
            out.put("hero.passive.book." + r[0], r[1]);
            out.put("hero.passive.book." + r[0] + ".desc", r[2]);
        }
        return out;
    }
}
