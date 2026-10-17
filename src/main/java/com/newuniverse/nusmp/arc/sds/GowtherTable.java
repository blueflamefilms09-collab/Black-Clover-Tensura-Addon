package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;

import java.util.List;

/**
 * Why: Gowther's numbers in one pure place (the same shape as the Zogratis tables) so the scheduler can be played against them in a unit test. The fight they serve
 * is about misdirection and memory, and the owner's rule for it is "no mind control of players": nothing here takes a player's body, aim or controls. His Invasion shows
 * as three things a party can learn to read: Light of Prying Eyes (he reads the KIND of blow you keep dealing and shrugs it off for a few seconds: vary your attacks),
 * Phantom Garden and the Nightmare Teller (many rings, only some real: the real ones stand steady and gold, the illusions flicker and are violet), and Echo of Memory
 * (he strikes where you WERE a second ago: keep moving). Herritt, his twin bows, is the plain ranged pressure, and the Marionette Strings are the one effect that moves
 * a player (a short pull under the balance law, with counters). The order of the rows is the index the client reads move names by: only ever append.
 */
public final class GowtherTable {
    private GowtherTable() {}

    public static final String HERRITT_VOLLEY = "herritt_volley", HERRITT_CROSS = "herritt_cross", PHANTOM_GARDEN = "phantom_garden", ECHO_OF_MEMORY = "echo_of_memory",
            PRYING_EYES = "prying_eyes", NIGHTMARE_TELLER = "nightmare_teller", MARIONETTE = "marionette", ILLUSORY_STEP = "illusory_step",
            REWRITE_LIGHT = "rewrite_light", PUPPET_RUSH = "puppet_rush";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float ARROW = 4.5f, CROSS_ARROW = 7f, GARDEN_BLAST = 13f, ECHO = 7f, NIGHTMARE_BLAST = 13f, RUSH = 8f, STRING_STRIKE = 9f, REWRITE = 5f, STEP_SNAP = 4f;

    public static final double GARDEN_RADIUS = 2.2, ECHO_RADIUS = 1.9, NIGHTMARE_RADIUS = 13, NIGHTMARE_BLAST_RADIUS = 2.2, RUSH_RADIUS = 4.4, STRING_RANGE = 30,
            STRING_STOP = 2.6, REWRITE_WIDTH = 0.9, REWRITE_LENGTH = 20;
    /** The most marks one Garden or one Nightmare wave draws (the packets and the dust of a crowd stay bounded). */
    public static final int MAX_MARKS = 18;
    /** How long the Nightmare dome stands, the ticks between its waves, and how long each wave's marks show before they fall. */
    public static final int NIGHTMARE_TICKS = 140, NIGHTMARE_WAVE = 28, NIGHTMARE_FALL = 20;
    /** The look-back of Light of Prying Eyes and how long a reading lasts. */
    public static final int READ_SPAN = 160, READ_TICKS = 160, READ_TICKS_GATES = 240;
    public static final float READ_CUT = 0.55f, READ_CUT_GATES = 0.65f, READ_MIN = 20f;
    /** The burst that breaks his concentration while the dome stands: a share of his maximum health inside the span. */
    public static final float BREAK_FRACTION = 0.08f;
    public static final int BREAK_SPAN = 60;
    /** Ticks of the ground the echoes remember, and the ages (ticks ago) of the echoes of one Echo of Memory (more at the Gates). */
    public static final int TRAIL_TICKS = 140;
    public static final int[] ECHO_AGES = {20, 40, 60}, ECHO_AGES_GATES = {20, 40, 60, 80, 100};
    /** The most ticks a string pulls (a pull of at most {@code STRINGS_TICKS * StringPull.MAX_SPEED} = 12 blocks). */
    public static final int STRINGS_TICKS = 12;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(HERRITT_VOLLEY, "Herritt: Twin Bows", 20, 18, 90, 5, 34, 5, 8),
            MoveRow.of(HERRITT_CROSS, "Herritt: Cross Lock", 40, 24, 210, 8, 36, 3, 10),
            MoveRow.of(PHANTOM_GARDEN, "Phantom Garden", 20, 28, 280, 4, 34, 4, 10).crowdMove(),
            MoveRow.of(ECHO_OF_MEMORY, "Echo of Memory", 40, 22, 230, 0, 48, 3, 10).crowdMove(),
            MoveRow.of(PRYING_EYES, "Light of Prying Eyes", 20, 18, 420, 0, 48, 2, 12),
            MoveRow.of(NIGHTMARE_TELLER, "Nightmare Teller", 60, 60, 1300, 0, 40, 5, 60).ult(),
            MoveRow.of(MARIONETTE, "Marionette Strings", 40, 20, 300, 5, 30, 2, 10),
            MoveRow.of(ILLUSORY_STEP, "Illusory Step", 20, 12, 140, 0, 48, 3, 6),
            MoveRow.of(REWRITE_LIGHT, "Rewrite Light", 40, 22, 260, 6, 32, 3, 8),
            MoveRow.of(PUPPET_RUSH, "Puppet Rush", 20, 12, 70, 0, 5.5, 5, 10));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) {
        for (MoveRow r : ROWS) if (r.id().equals(id)) return r;
        return null;
    }

    /** Arrows of one Twin Bows volley: two a bow across the power steps (2, 2, 4, 6, 8). */
    public static int arrowCount(int castSlots) { return Math.min(8, 2 * Math.max(1, castSlots)); }

    /** Real rings a Phantom Garden draws on every foe: one on the feet plus the volley slots (2, 2, 3, 4, 5). */
    public static int realPerFoe(int castSlots) { return 1 + Math.max(1, castSlots); }

    /** The illusions it draws for every real ring: two (a crowd of false rings is the misdirection). */
    public static int falsePerReal() { return 2; }

    /** The ages of an Echo of Memory. */
    public static int[] echoAges(boolean gates) { return gates ? ECHO_AGES_GATES : ECHO_AGES; }

    /** How long a reading lasts and how much of the read kind it cuts. */
    public static int readTicks(boolean gates) { return gates ? READ_TICKS_GATES : READ_TICKS; }

    public static float readCut(boolean gates) { return gates ? READ_CUT_GATES : READ_CUT; }

    /** The waves of one Nightmare dome: how many fit before it ends. */
    public static int nightmareWaves() { return NIGHTMARE_TICKS / NIGHTMARE_WAVE; }
}
