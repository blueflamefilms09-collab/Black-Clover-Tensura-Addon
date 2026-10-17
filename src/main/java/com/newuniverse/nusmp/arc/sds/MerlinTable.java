package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;

import java.util.List;

/**
 * Why: Merlin's numbers in one pure place (the same shape as the Zogratis tables), so the scheduler can be played against them in a unit test and the
 * boss class only adds the telegraph and the code. The design the numbers serve is in {@code docs/arc/merlin.md}: a caster who never runs dry (every move is
 * {@link MoveRow#noMana() free}: that is her Infinity, "endless magic uptime"), whose three answers to the player's own tricks are all telegraphed and all
 * have a counter: Absolute Cancel erases the next spell that reaches her hands (so bait it with a cheap one, or hit her from the side, or use a blade),
 * Perfect Cube reflects what is thrown at it but only a few times (so break it with melee or anti-magic, or spend three small shots), and her Infinity
 * fields last until she is staggered (so burst her). The ultimate, Chrono Coffin, is the longest wind-up of any move she has, and a burst breaks it.
 * The order of the rows is the index the client reads move names by: only ever append.
 */
public final class MerlinTable {
    private MerlinTable() {}

    public static final String ARCANE_VOLLEY = "arcane_volley", ALDAN_STRIKE = "aldan_strike", TRANSFER = "transfer", ABSOLUTE_CANCEL = "absolute_cancel",
            PERFECT_CUBE = "perfect_cube", INFINITY_FIELD = "infinity_field", STARFALL = "starfall", MAGIC_SEAL = "magic_seal",
            POWER_AMPLIFY = "power_amplify", MASS_TRANSFER = "mass_transfer", SPELL_LIBRARY = "spell_library", CHRONO_COFFIN = "chrono_coffin";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float BOLT = 5.5f, ALDAN = 13f, SNAP = 5f, STAR = 9f, MASS_SNAP = 6f, COFFIN_COLLAPSE = 20f;
    /** The tick of an Infinity field: base damage after 0 seconds inside, growth per second inside, and the most it reaches. */
    public static final float FIELD_TICK = 2.5f, FIELD_GROWTH = 0.6f, FIELD_TICK_MAX = 7f;

    public static final double ALDAN_RADIUS = 3.4, FIELD_RADIUS = 5.5, SEAL_RADIUS = 2.2, STAR_RADIUS = 2.3, MASS_RADIUS = 2.2, COFFIN_HALF = 5.0;
    public static final int CANCEL_TICKS = 44, CANCEL_TICKS_GATES = 64, CUBE_TICKS = 80, CUBE_TICKS_GATES = 110, CUBE_CHARGES = 3, CUBE_CHARGES_GATES = 4,
            CANCEL_CHARGES = 1, CANCEL_CHARGES_GATES = 2, FIELD_CAP = 2, FIELD_CAP_GATES = 3, AMP_TICKS = 160, COFFIN_FREEZE = 60, COFFIN_COLLAPSE_DELAY = 40;
    /** The burst that staggers her (a share of her maximum health inside the span, then the cool-down), and how long the stagger lasts. */
    public static final float STAGGER_FRACTION = 0.07f, FOCUS_FRACTION = 0.09f;
    public static final int STAGGER_SPAN = 60, STAGGER_COOLDOWN = 400, STAGGER_TICKS = 40;
    /** Chrono Coffin: ticks of the extra weakness after a hit while she is staggered. */
    public static final float STAGGERED_TAKES = 1.15f;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(ARCANE_VOLLEY, "Arcane Volley", 20, 18, 100, 6, 34, 5, 8).noMana(),
            MoveRow.of(ALDAN_STRIKE, "Aldan Strike", 20, 14, 80, 0, 7, 5, 10).noMana(),
            MoveRow.of(TRANSFER, "Transfer", 20, 16, 150, 0, 9, 3, 6).noMana(),
            MoveRow.of(ABSOLUTE_CANCEL, "Absolute Cancel", 20, 14, 300, 5, 40, 4, 14).noMana(),
            MoveRow.of(PERFECT_CUBE, "Perfect Cube", 40, 20, 360, 0, 48, 3, 12).noMana(),
            MoveRow.of(INFINITY_FIELD, "Infinity: Enchant", 20, 24, 260, 4, 34, 4, 10).crowdMove().noMana(),
            MoveRow.of(STARFALL, "Starfall", 40, 24, 230, 4, 36, 4, 10).crowdMove().noMana(),
            MoveRow.of(MAGIC_SEAL, "Magic Seal", 40, 20, 320, 5, 34, 2, 8).noMana(),
            MoveRow.of(POWER_AMPLIFY, "Power Amplify", 60, 16, 560, 0, 48, 1, 12).noMana(),
            MoveRow.of(MASS_TRANSFER, "Mass Transfer", 60, 26, 380, 0, 48, 2, 14).crowdMove().noMana(),
            MoveRow.of(SPELL_LIBRARY, "Spell Library", 20, 16, 260, 6, 30, 2, 8).noMana(),
            MoveRow.of(CHRONO_COFFIN, "Chrono Coffin", 60, 64, 1300, 0, 40, 5, 60).ult().noMana());

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) {
        for (MoveRow r : ROWS) if (r.id().equals(id)) return r;
        return null;
    }

    /** Volley size: two more than the framework's volley slots (3, 3, 4, 5, 6 across the power steps), two more at the Gates. */
    public static int boltCount(int castSlots, boolean gates) { return 2 + Math.max(1, castSlots) + (gates ? 2 : 0); }

    /** Falling stars of one Starfall: three plus the volley slots (4 to 7). */
    public static int starCount(int castSlots) { return 3 + Math.max(1, castSlots); }

    /** Seconds spent inside an Infinity field to damage of one tick. */
    public static float fieldTick(int secondsInside) { return Math.min(FIELD_TICK_MAX, FIELD_TICK + FIELD_GROWTH * Math.max(0, secondsInside)); }

    /** Half the side of the Chrono Coffin's cube: 5 blocks, half a block more for every power step above the opening (7 at the Gates). */
    public static double coffinHalf(int stage) { return COFFIN_HALF + 0.5 * Math.max(0, Math.min(4, stage)); }
}
