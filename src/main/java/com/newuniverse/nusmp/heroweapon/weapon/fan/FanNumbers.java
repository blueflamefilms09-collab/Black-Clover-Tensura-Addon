package com.newuniverse.nusmp.heroweapon.weapon.fan;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every number of the Vassal Fan (damage, ranges, rests, costs, durations) is stated once, here, with no game imports, so the skills read them from one
 * place and one test ({@code FanNumbersTest}) can hold the whole weapon to the owner's balance budget (docs/heroweapon/design.md section 5, {@link Budget}):
 * a later edit that makes a gust too strong or a veil too cheap fails the test instead of reaching a player. Damage is RAW (before the level factor of 1.0 to
 * 2.2 and before the balance law). The weapon's play is graceful wind: a wide cutting sweep, a crescent, a push, a pull, a gliding dash, a veil, a buff for
 * friends, one armour-cracking thrust and, once the fan has gathered magic residue (the series' Ambient Mana Absorption), the red petal tornado.
 */
public final class FanNumbers {
    private FanNumbers() {}

    // ---- skill ids
    public static final String FLOWER_WIND = "fan.flower_wind", MOON_BREAK = "fan.moon_break", REPEL_GUST = "fan.repel_gust", GALE_PULL = "fan.gale_pull",
            WIND_DANCE = "fan.wind_dance", MIST_VEIL = "fan.mist_veil", SNOW_PLUM = "fan.snow_plum", TORTOISE_SHELL_CRACKER = "fan.tortoise_shell_cracker",
            SNOW_MOON_FLOWER = "fan.reverse_snow_moon_flower";

    // ---- passive ids (the forms file lists them, the logic checks them)
    public static final String P_FEATHERFALL = "featherfall", P_RESIDUE = "residue", P_DRAGON_SURGE = "dragon_surge", P_SEALING_TIDE = "sealing_tide",
            P_TIGER_POUNCE = "tiger_pounce", P_KEYSTONE_WARD = "keystone_ward";

    // ---- Flower Wind: the wide cutting sweep
    public static final double FLOWER_RAW = 5.0, FLOWER_RANGE = 5.5, FLOWER_HALF_ANGLE = 55.0, FLOWER_SHOVE = 0.5;
    public static final int FLOWER_TARGETS = 8;
    /** Dragon Surge (Demon Dragon Fan, full residue): the sweep cuts this much harder and reaches this much farther. */
    public static final double SURGE_RAW = 2.0, SURGE_RANGE = 1.5;

    // ---- Moon Break: the crescent flung along the aim
    public static final double MOON_RAW = 8.0, MOON_RANGE = 14.0, MOON_WIDTH = 0.9;
    public static final int MOON_TARGETS = 6;

    // ---- Repelling Gust: the push (and the arrows turned back)
    public static final double GUST_RADIUS = 5.0, GUST_SHOVE = 1.0;
    public static final int GUST_TARGETS = 12, GUST_PROJECTILES = 8;

    // ---- Gale Pull: the pull toward a point in front
    public static final double PULL_RANGE = 9.0, PULL_HALF_ANGLE = 50.0, PULL_POINT_AHEAD = 2.5, PULL_GAIN = 0.35, PULL_MAX_SPEED = 0.8;
    public static final int PULL_TARGETS = 8, PULL_SECOND_AFTER = 4;
    /** Wave Sealing Sword: the gust and the pull reach this much farther. */
    public static final double TIDE_EXTRA = 1.5;

    // ---- Wind Dance: the gliding dash
    public static final double DANCE_SPEED = 0.95, DANCE_LENGTH = 6.0, DANCE_WIDTH = 1.2, DANCE_RAW = 4.0, DANCE_SHOVE = 0.4, DANCE_LIFT = 0.2, DANCE_UP = 0.3, DANCE_DOWN = 0.2;
    public static final int DANCE_TARGETS = 4, DANCE_SLOW_FALL_TICKS = 80;
    /** White Tiger Fan (Tiger Pounce): the dash is this much faster. */
    public static final double POUNCE_FACTOR = 1.25;

    // ---- Mist Veil
    public static final int VEIL_TICKS = 80;
    public static final double VEIL_RADIUS = 20.0, VEIL_PROJECTILE_FACTOR = 0.6;

    // ---- Snow Plum: the blessing for friends
    public static final double PLUM_RADIUS = 8.0, PLUM_HEAL_FLAT = 2.0, PLUM_HEAL_SHARE = 0.04, PLUM_HEAL_CAP = 0.25;
    public static final int PLUM_TICKS = 160, PLUM_TARGETS = 12;

    // ---- Tortoise Shell Cracker: the thrust that ignores armour
    public static final double TORTOISE_BASE = 10.0, TORTOISE_PER_ARMOR = 0.5, TORTOISE_ARMOR_BONUS_CAP = 8.0, TORTOISE_RANGE = 9.0, TORTOISE_SHOVE = 0.6;
    public static final int TORTOISE_DELAY = 10;

    // ---- Reverse Snow Moon Flower: the red petal tornado
    public static final double SNOW_MOON_RADIUS = 5.0, SNOW_MOON_AIM = 12.0, SNOW_MOON_BASE = 3.5, SNOW_MOON_PER_RESIDUE = 0.25, SNOW_MOON_DRAW = 0.2;
    public static final int SNOW_MOON_DELAY = 20, SNOW_MOON_PULSES = 6, SNOW_MOON_INTERVAL = 10, SNOW_MOON_TARGETS = 12;

    // ---- Magic residue (the series' Ambient Mana Absorption)
    public static final int RESIDUE_MAX = 6, SWINGS_PER_RESIDUE = 4, SWEEP_RESIDUE_FOES = 2;

    // ---- featherfall: the share of a fall that gets through, and the glide
    public static final float FALL_FACTOR = 0.4f;
    /**
     * Holding sneak while falling at least this fast (blocks per tick), or after a fall of at least {@link #GLIDE_MIN_DISTANCE} blocks, lets the fan glide you down:
     * slow falling, renewed every five ticks for as long as you keep holding. (A server only half knows how fast a player moves, so the distance fallen, which it
     * does track, starts the glide as well.)
     */
    public static final double GLIDE_MIN_FALL = 0.25, GLIDE_MIN_DISTANCE = 3.0;
    public static final int GLIDE_TICKS = 12;

    // ---- costs (share of the maximum magicules) and rests (ticks, before the level scaling)
    public static final int REST_FLOWER = 60, REST_MOON = 80, REST_GUST = 120, REST_PULL = 160, REST_DANCE = 100, REST_VEIL = 200, REST_PLUM = 200, REST_TORTOISE = 140,
            REST_SNOW_MOON = 700;
    public static final double COST_FLOWER = 0.010, COST_MOON = 0.015, COST_GUST = 0.015, COST_PULL = 0.015, COST_DANCE = 0.015, COST_VEIL = 0.015, COST_PLUM = 0.020,
            COST_TORTOISE = 0.025, COST_SNOW_MOON = 0.060;

    // ---- the pure rules the skills call
    /** Raw damage of Flower Wind; {@code surge} when Dragon Surge is on. */
    public static double flowerRaw(boolean surge) { return FLOWER_RAW + (surge ? SURGE_RAW : 0.0); }

    public static double flowerRange(boolean surge) { return FLOWER_RANGE + (surge ? SURGE_RANGE : 0.0); }

    /** Raw damage of the Tortoise Shell Cracker against a foe with this much armour: 10 and half a point per armour point, at most 8 more. */
    public static double tortoiseRaw(double armor) {
        return TORTOISE_BASE + Math.min(TORTOISE_ARMOR_BONUS_CAP, Math.max(0.0, armor) * TORTOISE_PER_ARMOR);
    }

    /** Raw damage of ONE pulse of the petal tornado with this much residue spent. */
    public static double snowMoonPulse(int residue) {
        return SNOW_MOON_BASE + SNOW_MOON_PER_RESIDUE * Math.max(0, Math.min(RESIDUE_MAX, residue));
    }

    /** What one Snow Plum heals a friend of this maximum health: 2 and 4 percent, never more than a quarter of it. */
    public static double plumHeal(double maxHealth) {
        return Math.max(0.0, Math.min(PLUM_HEAL_FLAT + PLUM_HEAL_SHARE * maxHealth, PLUM_HEAL_CAP * maxHealth));
    }

    /**
     * True when the wielder should be gliding now: sneaking in the air, not carried some other way, and either already gliding (so the glide holds once it has
     * slowed the fall) or falling fast enough or having fallen far enough to start one.
     *
     * @param onGround      standing on something
     * @param sneaking      holding sneak
     * @param carried       flying with creative flight or an elytra, swimming, climbing or riding: nothing for a glide to do
     * @param fallSpeed     blocks per tick downward (negative when rising)
     * @param fallDistance  blocks fallen so far
     * @param alreadyGliding the glide was renewed within the last {@link #GLIDE_TICKS} ticks
     */
    public static boolean glides(boolean onGround, boolean sneaking, boolean carried, double fallSpeed, double fallDistance, boolean alreadyGliding) {
        if (!sneaking || onGround || carried) return false;
        return alreadyGliding || fallSpeed >= GLIDE_MIN_FALL || fallDistance >= GLIDE_MIN_DISTANCE;
    }

    // ---- the table the budget test reads
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        add(new Spec(FLOWER_WIND, Kind.ORDINARY, REST_FLOWER, COST_FLOWER, flowerRaw(true), 1, FLOWER_TARGETS, 0));
        add(new Spec(MOON_BREAK, Kind.ORDINARY, REST_MOON, COST_MOON, MOON_RAW, 1, MOON_TARGETS, 0));
        add(new Spec(REPEL_GUST, Kind.UTILITY, REST_GUST, COST_GUST, 0.0, 0, GUST_TARGETS, 0));
        add(new Spec(GALE_PULL, Kind.UTILITY, REST_PULL, COST_PULL, 0.0, 0, PULL_TARGETS, PULL_SECOND_AFTER));
        add(new Spec(WIND_DANCE, Kind.UTILITY, REST_DANCE, COST_DANCE, DANCE_RAW, 1, DANCE_TARGETS, DANCE_SLOW_FALL_TICKS));
        add(new Spec(MIST_VEIL, Kind.UTILITY, REST_VEIL, COST_VEIL, 0.0, 0, 0, VEIL_TICKS));
        add(new Spec(SNOW_PLUM, Kind.UTILITY, REST_PLUM, COST_PLUM, 0.0, 0, PLUM_TARGETS, PLUM_TICKS));
        add(new Spec(TORTOISE_SHELL_CRACKER, Kind.HEAVY, REST_TORTOISE, COST_TORTOISE, tortoiseRaw(1000.0), 1, 1, TORTOISE_DELAY));
        add(new Spec(SNOW_MOON_FLOWER, Kind.ULTIMATE, REST_SNOW_MOON, COST_SNOW_MOON, snowMoonPulse(RESIDUE_MAX), SNOW_MOON_PULSES, SNOW_MOON_TARGETS,
                SNOW_MOON_DELAY + SNOW_MOON_PULSES * SNOW_MOON_INTERVAL));
    }

    private static void add(Spec s) { SPECS.put(s.id(), s); }

    /** Every technique of the Fan, in the order the weapon lists them. */
    public static List<Spec> table() { return List.copyOf(SPECS.values()); }

    /** The row of a skill id; throws for an id the Fan does not have (a typo is a bug, not a default). */
    public static Spec spec(String id) {
        Spec s = SPECS.get(id);
        if (s == null) throw new IllegalArgumentException("the Fan has no skill " + id);
        return s;
    }
}
