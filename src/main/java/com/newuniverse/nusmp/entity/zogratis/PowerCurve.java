package com.newuniverse.nusmp.entity.zogratis;

/**
 * The Percentage Power System of the Zogratis bosses (pure, no Minecraft types). A boss draws out its devil's power in
 * percentages: it opens the fight at 20%, reaches 40% under 85% health, 60% under 65%, 80% under 45% and stays at 80% (the cap)
 * until the Gates of the Tree of Qliphoth open under 25% health: then 100%, the transformation.
 * <ul>
 *   <li>Power never goes down: {@link #next} is a pure function of (health fraction, previous power).</li>
 *   <li>{@link #rage} is the Dark Triad bond: when a sibling falls the others gain one 10-point step, never past the 80% cap
 *       (100% is only ever reached by opening the Gates).</li>
 *   <li>{@link #tuning} turns any power value (the steps and the rage values between them) into the numbers a fight uses.</li>
 *   <li>{@link #clampDamage}: one burst can cross at most one step, so no step is ever skipped by a big hit.</li>
 * </ul>
 */
public final class PowerCurve {
    private PowerCurve() {}

    public static final int OPENING = 20, CAP = 80, FULL = 100, RAGE_STEP = 10, INFINITE_MANA_FROM = 60;
    /** The invulnerable cutscene when the Gates open. */
    public static final int TRANSFORM_TICKS = 60;
    /** Health fraction under which each step arrives, and the power it brings. */
    private static final float[] GATE_HEALTH = {0.85f, 0.65f, 0.45f, 0.25f};
    private static final int[] GATE_POWER = {40, 60, 80, 100};

    /** The power a health fraction asks for (20 at 85% and above, then 40, 60, 80, and 100 under 25%). */
    public static int target(float healthFraction) {
        for (int k = GATE_HEALTH.length - 1; k >= 0; k--) if (healthFraction < GATE_HEALTH[k]) return GATE_POWER[k];
        return OPENING;
    }

    /** The new power: never below the previous one. */
    public static int next(float healthFraction, int previousPower) {
        return Math.max(Math.max(0, Math.min(FULL, previousPower)), target(healthFraction));
    }

    /**
     * The next real step above 'power' (20 -> 40 -> 60 -> 80 -> 100; a rage value like 50 goes to 60); a power already at 100 stays.
     * A boss that is due several steps in one tick (two hits in the same tick) climbs them one at a time, so every step is announced
     * and no step is skipped.
     */
    public static int stepUp(int power) {
        for (int g : GATE_POWER) if (g > power) return g;
        return FULL;
    }

    /** One step of Triad rage: +10, never past the 80% cap (a power already at or above the cap is unchanged). */
    public static int rage(int power) {
        return power >= CAP ? power : Math.min(CAP, power + RAGE_STEP);
    }

    public static boolean gatesOpen(int power) { return power >= FULL; }

    /** 0 below 40%, 1 from 40, 2 from 60, 3 from 80, 4 at 100. */
    public static int stage(int power) {
        return power >= FULL ? 4 : power >= 80 ? 3 : power >= 60 ? 2 : power >= 40 ? 1 : 0;
    }

    /** The health fraction at which the next step arrives, or 0 if power is already 100. */
    public static float nextGateHealth(int power) {
        for (int k = 0; k < GATE_POWER.length; k++) if (GATE_POWER[k] > power) return GATE_HEALTH[k];
        return 0f;
    }

    /**
     * Caps a hit so that it ends at most half a point under the next step's health line: the hit that crosses a line cannot
     * carry on past it, the next one can. Once power is 100 there is no line and the hit is unchanged. A boss that stands in the
     * sliver between that landing point and the line (or exactly on it) is still held at the landing point, so the step
     * ({@link #target} fires strictly under the line) cannot be skipped by rounding.
     */
    public static float clampDamage(float health, float maxHealth, int power, float amount) {
        float floor = nextGateHealth(power) * maxHealth;
        if (floor <= 0f || amount <= 0f || health - amount >= floor) return amount;
        float landing = floor - Math.max(0.5f, maxHealth * 1e-5f);
        if (health <= landing) return amount;                      // already past the line: the power update catches up next tick
        return Math.min(amount, health - landing);
    }

    /** The share of its devil's power a Dark Disciple carries: up to 40%, scaled by the owner's power fraction. */
    public static double discipleShare(int power) {
        return 0.4 * Math.max(0, Math.min(FULL, power)) / 100.0;
    }

    // ---------------------------------------------------------------- tuning
    /**
     * What a power value means for the fight.
     * @param damageMultiplier         on every base damage of the boss
     * @param cooldownMultiplier       on every move cooldown (smaller is faster; it drops sharply at 60%, when the mana is unbound)
     * @param speedMultiplier          on movement speed
     * @param windUpMultiplier         on wind-up ticks (smaller is a shorter tell, never below the boss's minimum)
     * @param regenFractionPerSecond   share of max health healed each second
     * @param discipleCap              most Dark Disciples alive at once
     * @param infiniteMana             from 60%: moves cost nothing
     * @param castSlots                how many projectiles, strikes or simultaneous casts one volley move may launch
     * @param ultimateGapTicks         least ticks between two ultimates
     * @param manaCap                  size of the mana pool (below 60% it is finite)
     * @param manaRegenPerSecond       mana returned each second
     */
    public record Tuning(int power, double damageMultiplier, double cooldownMultiplier, double speedMultiplier, double windUpMultiplier,
                         double regenFractionPerSecond, int discipleCap, boolean infiniteMana, int castSlots, int ultimateGapTicks,
                         double manaCap, double manaRegenPerSecond) {
        public int stage() { return PowerCurve.stage(power); }
        public boolean gatesOpen() { return PowerCurve.gatesOpen(power); }
    }

    // anchors at power 20, 40, 60, 80, 100
    private static final double[] DAMAGE = {0.60, 0.80, 1.00, 1.25, 1.60};
    private static final double[] COOLDOWN = {1.00, 0.92, 0.70, 0.55, 0.42};
    private static final double[] SPEED = {1.00, 1.03, 1.06, 1.10, 1.18};
    private static final double[] WINDUP = {1.00, 1.00, 0.95, 0.85, 0.75};
    private static final double[] REGEN = {0.0005, 0.0010, 0.0015, 0.0022, 0.0030};
    private static final double[] DISCIPLES = {1, 2, 3, 4, 6};
    private static final double[] SLOTS = {1, 1, 2, 3, 4};
    private static final double[] ULT_GAP = {2400, 1800, 1500, 1200, 900};
    private static final double[] MANA_CAP = {100, 140, 200, 260, 320};
    private static final double[] MANA_REGEN = {2.5, 3.5, 9, 12, 16};

    private static double lerp(double[] anchors, int power) {
        double x = (Math.max(OPENING, Math.min(FULL, power)) - OPENING) / 20.0;
        int i = (int) Math.floor(x);
        if (i >= anchors.length - 1) return anchors[anchors.length - 1];
        return anchors[i] + (anchors[i + 1] - anchors[i]) * (x - i);
    }

    /** The tuning of any power value 0..100 (values under 20 use the opening numbers). */
    public static Tuning tuning(int power) {
        int p = Math.max(0, Math.min(FULL, power));
        return new Tuning(p, lerp(DAMAGE, p), lerp(COOLDOWN, p), lerp(SPEED, p), lerp(WINDUP, p), lerp(REGEN, p),
                (int) Math.round(lerp(DISCIPLES, p)), p >= INFINITE_MANA_FROM, (int) Math.round(lerp(SLOTS, p)),
                (int) Math.round(lerp(ULT_GAP, p)), lerp(MANA_CAP, p), lerp(MANA_REGEN, p));
    }
}
