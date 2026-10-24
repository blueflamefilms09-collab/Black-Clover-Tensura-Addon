package com.newuniverse.nusmp.mana.method;

/**
 * The numbers and the small maths of the Mana Method, pure and tested (no game classes). Every number that costs the server something has a hard cap here, and the server
 * config can only move them inside the ranges below ({@link #clampPerCaster}, {@link #clampGlobal}).
 *
 * <p>The cast: a rune array forms {@link #RUNE_DISTANCE} blocks in front of the caster's eyes and draws itself for {@link #chargeTicks}; then ONE bolt leaves the rune's centre at
 * {@link #speed} blocks per tick and flies at most {@link #MAX_FLIGHT_TICKS} ticks / {@link #MAX_RANGE} blocks. The modifiers:
 * <ul>
 *   <li><b>Homing</b> (pursue): the bolt picks ONE target when it leaves the rune (one query per cast) and turns toward it by at most {@link #HOMING_TURN} radians per tick for
 *       at most {@link #HOMING_TICKS} ticks. No scan after the launch: the target is looked up by id each tick.</li>
 *   <li><b>Growth</b> (grow): the hit radius grows with the distance flown ({@link #GROWTH_RADIUS_PER_BLOCK} per block), capped at x{@link #GROWTH_RADIUS_MAX}; the damage
 *       grows by {@link #GROWTH_DAMAGE_PER_BLOCK} per block, capped at x{@link #GROWTH_DAMAGE_MAX}.</li>
 *   <li>Both together share the power: x{@link #BOTH_MODS_MULT} damage.</li>
 * </ul>
 */
public final class RuneRules {
    private RuneRules() {}

    // ---------------------------------------------------------------- the cast
    public static final double RUNE_DISTANCE = 2.6, RUNE_RADIUS_MIN = 0.9;
    public static final int CHARGE_TICKS = 18, CHARGE_TICKS_TRUE = 26, CHARGE_TICKS_BOTH = 4;
    public static final double SPEED = 1.25, SPEED_TRUE = 1.1, MAX_RANGE = 36.0;
    public static final int MAX_FLIGHT_TICKS = 40;
    /** How long a finished rune lingers on the clients after it fired (the flare), in ticks. */
    public static final int FLARE_TICKS = 6;

    // ---------------------------------------------------------------- hits
    public static final double BASE_HIT_RADIUS = 0.55, GROWTH_RADIUS_PER_BLOCK = 0.09, GROWTH_RADIUS_MAX = 3.0;
    public static final double GROWTH_DAMAGE_PER_BLOCK = 0.035, GROWTH_DAMAGE_MAX = 1.8, BOTH_MODS_MULT = 0.85;
    public static final double RAW_DAMAGE = 13.0, RAW_DAMAGE_TRUE = 22.0;
    public static final double SPLASH_RADIUS = 2.0, SPLASH_RADIUS_MAX = 3.5, SPLASH_SHARE = 0.5;
    public static final int MAX_TARGETS = 6;

    // ---------------------------------------------------------------- homing
    public static final double HOMING_TURN = 0.22, HOMING_RANGE = 28.0, HOMING_CONE_COS = 0.5;
    public static final int HOMING_TICKS = 36;

    // ---------------------------------------------------------------- caps
    public static final int DEFAULT_PER_CASTER = 2, MAX_PER_CASTER = 4, DEFAULT_GLOBAL = 24, MAX_GLOBAL = 64, MIN_GLOBAL = 4;
    /** The collision queries (entity scans of a bolt's last stretch) all bolts together may make in one tick; a bolt that is denied sweeps a longer stretch next tick. */
    public static final int QUERIES_PER_TICK = 16;
    /** The most trail packets one flight may send (one every {@link #TRAIL_EVERY} ticks) and the particles in each. */
    public static final int TRAIL_EVERY = 3, TRAIL_PARTICLES = 3;

    // ---------------------------------------------------------------- costs of the pages (percent of the pool, floor, cooldown ticks): the element runes are the MID tier of the 15-spells recipe (12 %, floor 300, 160 ticks), True Magic the ZONE tier (20 %, floor 600, 400 ticks: inside its 300..1400 bounds)
    public static final double COST_PERCENT = 12.0, COST_PERCENT_TRUE = 20.0;
    public static final double COST_FLOOR = 300.0, COST_FLOOR_TRUE = 600.0;
    public static final int COOLDOWN = 160, COOLDOWN_TRUE = 400;

    public static int clampPerCaster(int configured) { return configured < 1 ? 1 : Math.min(configured, MAX_PER_CASTER); }

    public static int clampGlobal(int configured) { return configured < MIN_GLOBAL ? MIN_GLOBAL : Math.min(configured, MAX_GLOBAL); }

    public static int chargeTicks(RuneSpec s) {
        return (s.trueMagic() ? CHARGE_TICKS_TRUE : CHARGE_TICKS) + (RuneMods.count(s.mods()) >= 2 ? CHARGE_TICKS_BOTH : 0);
    }

    public static double speed(RuneSpec s) { return s.trueMagic() ? SPEED_TRUE : SPEED; }

    /** Raw damage before the balance law (EP scaling, the per-hit caps on players, the boss caps), without the growth. */
    public static double rawDamage(RuneSpec s) {
        double v = s.trueMagic() ? RAW_DAMAGE_TRUE : RAW_DAMAGE;
        return RuneMods.count(s.mods()) >= 2 ? v * BOTH_MODS_MULT : v;
    }

    // ---------------------------------------------------------------- growth
    /** The radius of the bolt's hit volume after {@code travelled} blocks. Without Growth it never changes. */
    public static double hitRadius(RuneSpec s, double travelled) {
        if (!s.growth()) return BASE_HIT_RADIUS;
        return BASE_HIT_RADIUS * growthScale(travelled);
    }

    /** x1 at the rune, rising {@value #GROWTH_RADIUS_PER_BLOCK} per block, capped at x{@value #GROWTH_RADIUS_MAX}. */
    public static double growthScale(double travelled) {
        double d = Double.isNaN(travelled) ? 0.0 : Math.max(0.0, travelled);
        return Math.min(GROWTH_RADIUS_MAX, 1.0 + GROWTH_RADIUS_PER_BLOCK * d);
    }

    /** The damage factor of the distance flown: 1 without Growth, rising {@value #GROWTH_DAMAGE_PER_BLOCK} per block up to x{@value #GROWTH_DAMAGE_MAX} with it. */
    public static double damageFactor(RuneSpec s, double travelled) {
        if (!s.growth()) return 1.0;
        double d = Double.isNaN(travelled) ? 0.0 : Math.max(0.0, travelled);
        return Math.min(GROWTH_DAMAGE_MAX, 1.0 + GROWTH_DAMAGE_PER_BLOCK * d);
    }

    public static double splashRadius(RuneSpec s, double travelled) {
        return Math.min(SPLASH_RADIUS_MAX, SPLASH_RADIUS * (s.growth() ? Math.sqrt(growthScale(travelled)) : 1.0));
    }

    // ---------------------------------------------------------------- homing
    /** Is a target at this cosine from the launch direction inside the cone the bolt may pursue? */
    public static boolean inHomingCone(double cos) { return cos >= HOMING_CONE_COS; }

    /** Does the bolt still steer at this age (ticks since launch)? */
    public static boolean homingActive(RuneSpec s, int age) { return s.homing() && age < HOMING_TICKS; }

    /**
     * Turns the unit vector {@code dir} toward the unit vector {@code want} by at most {@code maxTurn} radians and writes the result (a unit vector) to {@code out} (which may be
     * {@code dir} itself). Allocation free. Returns the angle between the two before the turn.
     */
    public static double steer(double[] dir, double[] want, double maxTurn, double[] out) {
        double dot = dir[0] * want[0] + dir[1] * want[1] + dir[2] * want[2];
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angle = Math.acos(dot);
        if (angle <= maxTurn || angle < 1e-9) {
            out[0] = want[0];
            out[1] = want[1];
            out[2] = want[2];
            return angle;
        }
        // the component of want perpendicular to dir, normalised; if want is opposite to dir any perpendicular will do
        double px = want[0] - dir[0] * dot, py = want[1] - dir[1] * dot, pz = want[2] - dir[2] * dot;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 1e-9) {                                                             // straight opposite: turn about an arbitrary perpendicular axis
            if (Math.abs(dir[0]) < 0.9) { px = 0; py = -dir[2]; pz = dir[1]; } else { px = dir[2]; py = 0; pz = -dir[0]; }
            pl = Math.sqrt(px * px + py * py + pz * pz);
        }
        px /= pl;
        py /= pl;
        pz /= pl;
        double c = Math.cos(maxTurn), s = Math.sin(maxTurn);
        double ox = dir[0] * c + px * s, oy = dir[1] * c + py * s, oz = dir[2] * c + pz * s;
        double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
        out[0] = ox / ol;
        out[1] = oy / ol;
        out[2] = oz / ol;
        return angle;
    }

    // ---------------------------------------------------------------- True Magic
    /** The most the target's magic resistance can be cancelled: its damage is multiplied by at most this. */
    public static final double BYPASS_MAX = 3.0, RESISTANCE_CAP = 0.8;

    /**
     * True Magic ignores magic resistance. {@code resistance} is Tensura's attribute as 0..1 (a fraction, or a percent above 1: read by {@link #resistanceOf}); the factor undoes
     * a damage cut of that size (1 / (1 - r)), at most x{@value #BYPASS_MAX}. The balance law's per-hit caps still apply on top (a player never loses more than the cap).
     */
    public static double resistanceBypass(double resistance) {
        double r = Double.isNaN(resistance) ? 0.0 : Math.max(0.0, Math.min(RESISTANCE_CAP, resistance));
        return Math.min(BYPASS_MAX, 1.0 / (1.0 - r));
    }

    /** A boss only loses {@link #BOSS_BYPASS_MAX} of its resistance (True Magic does not melt a boss). */
    public static final double BOSS_BYPASS_MAX = 1.5;

    public static double bypassFor(double resistance, boolean boss) {
        double f = resistanceBypass(resistance);
        return boss ? Math.min(f, BOSS_BYPASS_MAX) : f;
    }

    /** Tensura's attribute value as a 0..1 resistance: above 1 it is a percent. */
    public static double resistanceOf(double attributeValue) {
        if (Double.isNaN(attributeValue) || Double.isInfinite(attributeValue)) return 0.0;
        double v = attributeValue > 1.0 ? attributeValue / 100.0 : attributeValue;
        return Math.max(0.0, Math.min(1.0, v));
    }
}
