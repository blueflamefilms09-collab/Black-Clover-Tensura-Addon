package com.newuniverse.nusmp.heroweapon.weapon.katana;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every number of the Vassal Katana (damage, lines, windows, tempo, rests, costs) is stated once, here, with no game imports, so the skills read them from
 * one place and one test ({@code KatanaNumbersTest}) can hold the whole weapon to the owner's balance budget (docs/heroweapon/design.md section 5,
 * {@code Budget}). Damage is RAW (before the level factor of 1.0 to 2.2 and before the balance law). The weapon is the duelist's: long thin cuts, the draw cut
 * of iaido (a breath, then a dash through the line), a parry that answers a blow, a step that dodges one, three thrusts, a flurry on one foe, a phantom second
 * blade, a cut that drains magicules, and the dance of sakura, which grows with the TEMPO the wielder has built by chaining different techniques.
 */
public final class KatanaNumbers {
    private KatanaNumbers() {}

    // ---- skill ids
    public static final String CRESCENT_MOON = "katana.crescent_moon", INSTANT_BLADE = "katana.instant_blade", COUNTER_STANCE = "katana.counter_stance",
            RELEASE_EVADE = "katana.release_evade", TRIPLE_THRUST = "katana.triple_thrust", STARDUST_BLADE = "katana.stardust_blade", DOUBLE_SWORD = "katana.double_sword",
            SOUL_SLICE = "katana.soul_slice", SAKURA_DANCE = "katana.sakura_dance";

    // ---- passive ids
    public static final String P_IAI_TEMPO = "iai_tempo", P_KEEN_OPENING = "keen_opening", P_RIPOSTE = "riposte";

    // ---- tempo (the flow of a duel)
    /** The longest the chain survives between two katana techniques, and the highest level it reaches. */
    public static final int TEMPO_WINDOW = 80, TEMPO_MAX = 3;
    /** Raw damage each level of tempo adds to a cut or a draw, and to each thrust of the triple thrust. */
    public static final double TEMPO_CUT = 0.5, TEMPO_THRUST = 0.25;

    // ---- Crescent Moon Sword
    public static final double CRESCENT_RAW = 6.5, CRESCENT_RANGE = 9.0, CRESCENT_WIDTH = 0.9;
    public static final int CRESCENT_TARGETS = 5;

    // ---- Instant Blade: Mist (the draw cut)
    public static final double INSTANT_RAW = 9.0, INSTANT_DASH = 7.0, INSTANT_MARGIN = 0.8, INSTANT_WIDTH = 1.0, INSTANT_LIFT = 0.18;
    public static final int INSTANT_DELAY = 5, INSTANT_TARGETS = 6;

    // ---- Counter Stance (the parry)
    public static final int PARRY_TICKS = 14;
    /** The share of the wielder's maximum health one parry can stop; the rest of a bigger blow gets through. */
    public static final double PARRY_NEGATE_SHARE = 0.6;
    public static final double COUNTER_MIN = 4.0, COUNTER_MAX = 12.0, COUNTER_PER_DAMAGE = 0.5, COUNTER_REACH = 8.0, COUNTER_SHOVE = 0.6;

    // ---- Release Evade (the dodge)
    public static final int EVADE_TICKS = 8;
    public static final double EVADE_SPEED = 0.9, EVADE_LIFT = 0.15;

    // ---- Triple Thrust
    public static final double THRUST_RAW = 5.0, THRUST_LENGTH = 4.8, THRUST_WIDTH = 0.8, THRUST_STEP = 0.25;
    public static final int THRUSTS = 3, THRUST_INTERVAL = 4, THRUST_DELAY = 6, THRUST_TARGETS = 5;

    // ---- Stardust Blade
    public static final double STARDUST_RAW = 2.4, STARDUST_RANGE = 8.0;
    public static final int STARDUST_CUTS = 5, STARDUST_INTERVAL = 3;

    // ---- Double Sword
    public static final double ECHO_RAW = 2.5;
    public static final int DOUBLE_TICKS = 120, ECHO_MAX = 4, ECHO_GAP = 6;

    // ---- Soul Slice
    public static final double SLICE_RAW = 8.0, SLICE_RANGE = 4.5, SLICE_DRAIN = 0.04, SLICE_TAKE_BACK = 0.5, SLICE_GAIN_CAP = 0.015;

    // ---- Kagura Dance of Sakura (the ultimate)
    public static final double SAKURA_BASE = 18.0, SAKURA_PER_TEMPO = 4.0, SAKURA_RADIUS = 4.8, SAKURA_CONE_RANGE = 7.0, SAKURA_CONE_HALF = 40.0, SAKURA_LINE = 9.0,
            SAKURA_LINE_WIDTH = 1.4;
    public static final int SAKURA_FORMATIONS = 3, SAKURA_GAP = 10, SAKURA_TARGETS = 12;

    // ---- passives
    /** Keen Opening: idle this long, and the next full swing strikes again for this much. */
    public static final int OPENING_IDLE = 80;
    public static final double OPENING_RAW = 3.0;
    /** Riposte: after a parry, the next full swing within this long strikes again for this much. */
    public static final int RIPOSTE_TICKS = 60;
    public static final double RIPOSTE_RAW = 3.0;

    // ---- costs (share of the maximum magicules) and rests (ticks, before the level scaling)
    public static final int REST_CRESCENT = 60, REST_INSTANT = 100, REST_COUNTER = 120, REST_EVADE = 100, REST_THRUST = 140, REST_STARDUST = 120, REST_DOUBLE = 200,
            REST_SLICE = 140, REST_SAKURA = 700;
    public static final double COST_CRESCENT = 0.010, COST_INSTANT = 0.020, COST_COUNTER = 0.015, COST_EVADE = 0.010, COST_THRUST = 0.025, COST_STARDUST = 0.020,
            COST_DOUBLE = 0.020, COST_SLICE = 0.020, COST_SAKURA = 0.060;

    // ---- the pure rules the skills call
    /** Raw damage of a crescent cut at this tempo. */
    public static double crescentRaw(int tempo) { return CRESCENT_RAW + TEMPO_CUT * clampTempo(tempo); }

    /** Raw damage of the draw cut at this tempo. */
    public static double instantRaw(int tempo) { return INSTANT_RAW + TEMPO_CUT * clampTempo(tempo); }

    /** Raw damage of ONE thrust of the triple thrust at this tempo. */
    public static double thrustRaw(int tempo) { return THRUST_RAW + TEMPO_THRUST * clampTempo(tempo); }

    /** Raw damage of the whole dance of sakura at this tempo (a third of it in each formation). */
    public static double sakuraTotal(int tempo) { return SAKURA_BASE + SAKURA_PER_TEMPO * clampTempo(tempo); }

    /** Raw damage of ONE formation of the dance at this tempo. */
    public static double sakuraFormation(int tempo) { return sakuraTotal(tempo) / SAKURA_FORMATIONS; }

    private static int clampTempo(int t) { return Math.max(0, Math.min(TEMPO_MAX, t)); }

    // ---- the table the budget test reads
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        add(new Spec(CRESCENT_MOON, Kind.ORDINARY, REST_CRESCENT, COST_CRESCENT, crescentRaw(TEMPO_MAX), 1, CRESCENT_TARGETS, 0));
        add(new Spec(INSTANT_BLADE, Kind.ORDINARY, REST_INSTANT, COST_INSTANT, instantRaw(TEMPO_MAX), 1, INSTANT_TARGETS, INSTANT_DELAY));
        add(new Spec(COUNTER_STANCE, Kind.UTILITY, REST_COUNTER, COST_COUNTER, COUNTER_MAX, 1, 1, PARRY_TICKS));
        add(new Spec(RELEASE_EVADE, Kind.UTILITY, REST_EVADE, COST_EVADE, 0.0, 0, 0, EVADE_TICKS));
        add(new Spec(TRIPLE_THRUST, Kind.HEAVY, REST_THRUST, COST_THRUST, thrustRaw(TEMPO_MAX), THRUSTS, THRUST_TARGETS, THRUST_DELAY + THRUSTS * THRUST_INTERVAL));
        add(new Spec(STARDUST_BLADE, Kind.ORDINARY, REST_STARDUST, COST_STARDUST, STARDUST_RAW, STARDUST_CUTS, 1, STARDUST_CUTS * STARDUST_INTERVAL));
        add(new Spec(DOUBLE_SWORD, Kind.UTILITY, REST_DOUBLE, COST_DOUBLE, ECHO_RAW, ECHO_MAX, 1, DOUBLE_TICKS));
        add(new Spec(SOUL_SLICE, Kind.ORDINARY, REST_SLICE, COST_SLICE, SLICE_RAW, 1, 1, 0));
        add(new Spec(SAKURA_DANCE, Kind.ULTIMATE, REST_SAKURA, COST_SAKURA, sakuraFormation(TEMPO_MAX), SAKURA_FORMATIONS, SAKURA_TARGETS, SAKURA_FORMATIONS * SAKURA_GAP));
    }

    private static void add(Spec s) { SPECS.put(s.id(), s); }

    /** Every technique of the Katana, in the order the weapon lists them. */
    public static List<Spec> table() { return List.copyOf(SPECS.values()); }

    /** The row of a skill id; throws for an id the Katana does not have (a typo is a bug, not a default). */
    public static Spec spec(String id) {
        Spec s = SPECS.get(id);
        if (s == null) throw new IllegalArgumentException("the Katana has no skill " + id);
        return s;
    }
}
