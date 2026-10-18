package com.newuniverse.nusmp.heroweapon.weapon.book;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every number of the Vassal Book (damage, ranges, rests, costs, durations, the heals) is stated once, here, with no game imports, so the skills and the
 * pages read them from one place and one test ({@code BookNumbersTest}) can hold the whole weapon to the owner's balance budget (docs/heroweapon/design.md
 * section 5, {@code Budget}). Damage is RAW (before the level factor of 1.0 to 2.2 and before the balance law). The weapon is a caster's book read in order:
 * its two chapters hold PAGES (a bird of flame, a rain of needles, a beam of feathers; a wall of paper, a verse that mends, a cage that binds), each page a
 * different bounded effect, read one after the other; reading every page of the volume completes it and opens Libraria, the white room, the ultimate.
 */
public final class BookNumbers {
    private BookNumbers() {}

    // ---- skill ids
    public static final String READ_COMPOSITION = "book.read_composition", READ_VERSE = "book.read_verse", TURN_PAGE = "book.turn_page", PAPER_BURST = "book.paper_burst",
            LIFE_FORCE_BURST = "book.life_force_burst", CONSTRICTION = "book.constriction", LIBRARIA = "book.libraria";

    // ---- page ids
    public static final String BIRD = "bird", NEEDLES = "needles", FEATHERS = "feathers", WALL = "wall", VERSE = "verse", CAGE = "cage";

    // ---- passive ids
    public static final String P_CHAPTERS = "chapters", P_DECK_FEATHERS = "deck_feathers", P_DECK_CAGE = "deck_cage", P_REVISED = "revised_edition", P_GRAND = "grand_library";

    /** Revised Edition: every page and burst is this much stronger, but never past the limit of its kind. */
    public static final double REVISED = 1.25;

    // ---- pages of the offence chapter (read with the use key)
    public static final double BIRD_RAW = 6.0, BIRD_RANGE = 14.0, BIRD_WIDTH = 1.2;
    public static final int BIRD_TARGETS = 4;
    public static final double NEEDLE_RAW = 2.5, NEEDLE_RADIUS = 3.0, NEEDLE_AIM = 14.0;
    public static final int NEEDLE_PULSES = 4, NEEDLE_INTERVAL = 5, NEEDLE_DELAY = 10, NEEDLE_TARGETS = 8;
    public static final double FEATHER_RAW = 9.0, FEATHER_RANGE = 12.0, FEATHER_WIDTH = 1.0, FEATHER_HEAL = 0.02;
    public static final int FEATHER_DELAY = 6, FEATHER_TARGETS = 5;

    // ---- pages of the support chapter (read with sneak and use)
    public static final double WALL_RADIUS = 5.0;
    public static final int WALL_TICKS = 200, WALL_TARGETS = 12;
    public static final double VERSE_RADIUS = 6.0, VERSE_FLAT = 1.6, VERSE_SHARE = 0.02, VERSE_CAP = 0.25;
    public static final int VERSE_PULSES = 3, VERSE_INTERVAL = 30, VERSE_TARGETS = 12;
    public static final double CAGE_RANGE = 14.0, CAGE_CEILING_RAW = 6.0, CAGE_EDGE = 2.4;
    public static final int CAGE_ROOT = 40, CAGE_CEILING_DELAY = 30;

    // ---- Paper Splits Rock
    public static final double BURST_RAW = 9.0, BURST_RADIUS = 3.5, BURST_AIM = 12.0, BURST_SHOVE = 1.0;
    public static final int BURST_DELAY = 8, BURST_TARGETS = 8;

    // ---- Life Force Burst
    public static final double LIFE_RAW = 14.0, LIFE_RANGE = 10.0, LIFE_LEASH = 6.0;
    public static final int LIFE_DELAY = 24;

    // ---- Libraria
    public static final double LIB_RAW = 3.5, LIB_RAW_GRAND = 3.2, LIB_RADIUS = 9.0;
    public static final int LIB_VOLLEYS = 8, LIB_VOLLEYS_GRAND = 10, LIB_INTERVAL = 12, LIB_DELAY = 20, LIB_TARGETS = 12;

    // ---- costs (share of the maximum magicules) and rests (ticks, before the level scaling)
    public static final int REST_COMPOSITION = 60, REST_VERSE = 100, REST_TURN = 100, REST_BURST = 80, REST_LIFE = 140, REST_CONSTRICTION = 160, REST_LIBRARIA = 800;
    public static final double COST_COMPOSITION = 0.015, COST_VERSE = 0.020, COST_TURN = 0.010, COST_BURST = 0.015, COST_LIFE = 0.025, COST_CONSTRICTION = 0.020,
            COST_LIBRARIA = 0.070;

    // ---- the pure rules the pages call
    /** Raw damage of ONE hit of a page of {@code hits} hits: the base, 25 percent more when revised, never so much that the page passes the ordinary limit of 12. */
    public static double pageRaw(double base, int hits, boolean revised) {
        if (!revised) return base;
        return Math.min(base * REVISED, Kind.ORDINARY.maxTotal() / Math.max(1, hits));
    }

    /** The same for a heavy technique (Life Force Burst): never past the heavy limit of 20. */
    public static double heavyRaw(double base, boolean revised) {
        return revised ? Math.min(base * REVISED, Kind.HEAVY.maxTotal()) : base;
    }

    /** What ONE pulse of the Healing Verse heals a friend of this maximum health: the flat part and 2 percent, 25 percent more when revised, and three pulses never pass a quarter of the health. */
    public static double versePulse(double maxHealth, boolean revised) {
        double raw = (VERSE_FLAT + VERSE_SHARE * maxHealth) * (revised ? REVISED : 1.0);
        return Math.max(0.0, Math.min(raw, VERSE_CAP * maxHealth / VERSE_PULSES));
    }

    /** What the Feather Volley mends the wielder who landed it: 2 percent of their health. */
    public static double featherHeal(double maxHealth) { return Math.max(0.0, FEATHER_HEAL * maxHealth); }

    /** How many volleys Libraria falls in, and the raw damage of each. */
    public static int volleys(boolean grand) { return grand ? LIB_VOLLEYS_GRAND : LIB_VOLLEYS; }

    public static double volleyRaw(boolean grand) { return grand ? LIB_RAW_GRAND : LIB_RAW; }

    // ---- the table the budget test reads
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        add(new Spec(READ_COMPOSITION, Kind.ORDINARY, REST_COMPOSITION, COST_COMPOSITION, pageRaw(FEATHER_RAW, 1, true), 1, Math.max(BIRD_TARGETS, Math.max(NEEDLE_TARGETS, FEATHER_TARGETS)),
                NEEDLE_DELAY + NEEDLE_PULSES * NEEDLE_INTERVAL));
        add(new Spec(READ_VERSE, Kind.UTILITY, REST_VERSE, COST_VERSE, pageRaw(CAGE_CEILING_RAW, 1, true), 1, 1, VERSE_PULSES * VERSE_INTERVAL));
        add(new Spec(TURN_PAGE, Kind.UTILITY, REST_TURN, COST_TURN, 0.0, 0, 0, 0));
        add(new Spec(PAPER_BURST, Kind.ORDINARY, REST_BURST, COST_BURST, pageRaw(BURST_RAW, 1, true), 1, BURST_TARGETS, BURST_DELAY));
        add(new Spec(LIFE_FORCE_BURST, Kind.HEAVY, REST_LIFE, COST_LIFE, heavyRaw(LIFE_RAW, true), 1, 1, LIFE_DELAY));
        add(new Spec(CONSTRICTION, Kind.UTILITY, REST_CONSTRICTION, COST_CONSTRICTION, pageRaw(CAGE_CEILING_RAW, 1, true), 1, 1, CAGE_CEILING_DELAY));
        add(new Spec(LIBRARIA, Kind.ULTIMATE, REST_LIBRARIA, COST_LIBRARIA, volleyRaw(true), volleys(true), LIB_TARGETS, LIB_DELAY + volleys(true) * LIB_INTERVAL));
    }

    private static void add(Spec s) { SPECS.put(s.id(), s); }

    /** Every technique of the Book, in the order the weapon lists them. */
    public static List<Spec> table() { return List.copyOf(SPECS.values()); }

    /** The row of a skill id; throws for an id the Book does not have (a typo is a bug, not a default). */
    public static Spec spec(String id) {
        Spec s = SPECS.get(id);
        if (s == null) throw new IllegalArgumentException("the Book has no skill " + id);
        return s;
    }
}
