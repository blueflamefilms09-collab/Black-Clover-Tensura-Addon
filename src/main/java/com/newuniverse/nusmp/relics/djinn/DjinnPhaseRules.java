package com.newuniverse.nusmp.relics.djinn;

/**
 * The four phases of a Djinn as numbers (pure, so DjinnPhaseRulesTest can check every table). Magi's progression:
 * <ol>
 *   <li><b>Lesser Summon</b> (1): a small manifestation for a few seconds at a light price. A household member can reach no further.</li>
 *   <li><b>Djinn Weapon / Half Equip</b> (2): the bearer's blows take the Djinn's magic. Toggled; Magoi drains while it is on.</li>
 *   <li><b>Full Djinn Equip, Maso</b> (3): the transformation. Flight, big boosts, an EP spike. At most 90 s, then a backlash and a lockout.</li>
 *   <li><b>Extreme Magic</b> (4): only inside a Full Equip. A telegraphed spell that takes 60% of the Magoi and is not a phase you stay in.</li>
 * </ol>
 * Magoi is the Tensura magicule pool; every cost is a fraction of its maximum, so a small pool and a huge one feel alike. Times are ticks.
 */
public final class DjinnPhaseRules {
    private DjinnPhaseRules() {}

    public static final int OFF = 0, LESSER = 1, HALF = 2, FULL = 3, EXTREME = 4;

    // ---------------------------------------------------------------- time
    public static final int LESSER_TICKS = 400;            // 20 s of Lesser Summon
    public static final int LESSER_COOLDOWN = 300;         // 15 s before the next one
    public static final int FULL_MAX_TICKS = 1800;         // 90 s of Maso at the most
    public static final int LOCKOUT_MIN = 1200;            // an early, clean exit locks Maso for 1 min
    public static final int LOCKOUT_MAX = 6000;            // the full 90 s, or burning out, locks it for 5 min
    public static final int BACKLASH_FREE = 600;           // under 30 s in Maso a voluntary exit has no backlash
    public static final int EXTREME_CHARGE = 60;           // 3 s of telegraph before the strike
    public static final int EXTREME_COOLDOWN = 12000;      // 10 min
    public static final int KEY_SPACING = 4;               // a key press is answered at most every 4 ticks

    // ---------------------------------------------------------------- trust and assimilation
    public static final int START_TRUST = 20, FULL_TRUST = 40, EXTREME_TRUST = 70;
    public static final float HOUSEHOLD_CAP = 50f;         // a household member is refused past 50% (the Magi guide)
    public static final float OWNER_CAP = 100f;            // the bearer's Djinn will not go beyond Lesser/Half at 100%
    public static final float OWNER_STRAIN = 90f;          // from here on every drain is 1.25x ("the Djinn wants more")
    public static final float MARK_AT = 50f;               // from here the bearer wears the Djinn's mark (a faint rune glow, +5% affinity)
    public static final int HOUSEHOLD_MAX = 2;             // an owner lends the Djinn to two allies at the most

    // ---------------------------------------------------------------- Magoi, as fractions of the maximum
    /** What entering a phase costs, once. */
    public static double entryFraction(int phase) {
        return switch (phase) {
            case LESSER -> 0.03;
            case HALF -> 0.05;
            case FULL -> 0.20;
            case EXTREME -> 0.60;
            default -> 0.0;
        };
    }

    /** What staying in a phase costs per second (a phase's upkeep replaces the one below, it does not add to it). */
    public static double upkeepFraction(int phase) {
        return switch (phase) {
            case LESSER -> 0.003;
            case HALF -> 0.006;
            case FULL -> 0.007;
            default -> 0.0;
        };
    }

    /** How full the Magoi bar must be to enter a phase at all (it must hold the entry cost and a margin). */
    public static double requiredFraction(int phase) {
        return switch (phase) {
            case LESSER -> 0.10;
            case HALF -> 0.15;
            case FULL -> 0.35;
            case EXTREME -> 0.60;
            default -> 0.0;
        };
    }

    /** The share of the pool a pool must never be drained below by upkeep (below it the phase ends: the Djinn cannot be fed). */
    public static final double FLOOR = 0.02;

    // ---------------------------------------------------------------- the exclusive-bond price and strain
    /** The drain factor from assimilation strain: 1.25 from 90%. */
    public static double strainMultiplier(float assimilation) { return assimilation >= OWNER_STRAIN ? 1.25 : 1.0; }

    /** Everything that scales a Magoi drain, in one place: the multi-bond price and the strain. */
    public static double drainFactor(int heldKinds, boolean multiBond, float assimilation) {
        return BondRules.drainMultiplier(heldKinds, multiBond) * strainMultiplier(assimilation);
    }

    // ---------------------------------------------------------------- requests
    public enum Verdict { OK, NOT_BOUND, NO_VESSEL, ORDER, NOT_IN_FULL, LOCKED_OUT, COOLDOWN, NEED_TRUST, NEED_MAGOI, ASSIMILATED, HOUSEHOLD_LIMIT }

    /**
     * Everything a request for a new phase depends on. {@code magoi} is the bar as a fraction of its maximum; times are game ticks;
     * {@code creative} players pay and wait for nothing (their bindings still need a Djinn).
     */
    public record Snapshot(int phase, boolean bound, boolean household, boolean vesselOn, int trust, float assimilation, double magoi,
                           long now, long lockoutUntil, long lesserReadyAt, long extremeReadyAt, boolean creative) {}

    /** Can the player go from their phase to {@code target} (0 = off, 4 = Extreme Magic)? Stepping down is always allowed. */
    public static Verdict request(Snapshot s, int target) {
        if (target <= OFF || target <= s.phase()) return Verdict.OK;       // stepping down, or staying, is always allowed
        if (!s.bound() && !s.household()) return Verdict.NOT_BOUND;
        if (s.household()) {
            if (target > LESSER) return Verdict.HOUSEHOLD_LIMIT;
            if (s.assimilation() >= HOUSEHOLD_CAP) return Verdict.ASSIMILATED;
        } else if (!s.vesselOn()) {
            return Verdict.NO_VESSEL;
        }
        if (target == EXTREME && s.phase() != FULL) return Verdict.NOT_IN_FULL;
        if (target > s.phase() + 1) return Verdict.ORDER;
        if (target >= HALF && !s.household() && s.assimilation() >= OWNER_CAP) return Verdict.ASSIMILATED;
        if (s.creative()) return Verdict.OK;
        if (target == LESSER && s.now() < s.lesserReadyAt()) return Verdict.COOLDOWN;
        if (target == FULL && s.now() < s.lockoutUntil()) return Verdict.LOCKED_OUT;
        if (target == EXTREME && s.now() < s.extremeReadyAt()) return Verdict.COOLDOWN;
        if (target == FULL && s.trust() < FULL_TRUST) return Verdict.NEED_TRUST;
        if (target == EXTREME && s.trust() < EXTREME_TRUST) return Verdict.NEED_TRUST;
        if (s.magoi() < requiredFraction(target)) return Verdict.NEED_MAGOI;
        return Verdict.OK;
    }

    // ---------------------------------------------------------------- how a Full Equip ends
    public enum EndReason { VOLUNTARY, TIMEOUT, DEPLETED, VESSEL_LOST, LOGOUT, DEATH, ADMIN }

    private static double usedFraction(int usedTicks) { return Math.max(0.0, Math.min(1.0, usedTicks / (double) FULL_MAX_TICKS)); }

    /** 0..1: how hard the backlash is (0 = none). */
    public static double severity(EndReason why, int usedTicks) {
        double f = usedFraction(usedTicks);
        return switch (why) {
            case TIMEOUT -> 1.0;
            case DEPLETED -> Math.max(0.6, f);
            case VOLUNTARY -> usedTicks < BACKLASH_FREE ? 0.0 : Math.max(0.1, Math.min(0.8, 0.8 * f));
            default -> 0.0;
        };
    }

    /** Ticks Maso stays locked after it ends. */
    public static int lockoutTicks(EndReason why, int usedTicks) {
        double f = switch (why) {
            case TIMEOUT -> 1.0;
            case DEPLETED -> Math.max(0.6, usedFraction(usedTicks));
            case VOLUNTARY, VESSEL_LOST, LOGOUT -> usedFraction(usedTicks);
            default -> -1.0;
        };
        if (f < 0) return 0;
        return (int) Math.round(LOCKOUT_MIN + (LOCKOUT_MAX - LOCKOUT_MIN) * f);
    }

    /** The trust a Full Equip's end earns (a clean exit) or costs (a burn-out). */
    public static int trustDelta(EndReason why, int usedTicks) {
        return switch (why) {
            case VOLUNTARY -> usedTicks >= BACKLASH_FREE ? 2 : 0;
            case TIMEOUT -> -5;
            case DEPLETED -> -8;
            default -> 0;
        };
    }

    /**
     * The cost of overusing the Djinn, as Tensura effects and a temporary attribute modifier, never a new bar: Slowness, Weakness, Nausea
     * (and Blindness when severe), plus a loss of maximum health ({@code healthLoss} hit points) that heals back linearly over
     * {@code strainTicks} (Magi's shortened lifespan, made to fade). The {@code hungerTicks} and {@code foodLoss} numbers stay in the record (they are tested
     * data) but the engine ignores them since the 1.99.0 hunger ban.
     */
    public record Backlash(int slowTicks, int slowAmp, int weakTicks, int weakAmp, int nauseaTicks, int hungerTicks, int foodLoss,
                           int blindTicks, int healthLoss, int strainTicks) {
        public boolean none() { return slowTicks == 0 && weakTicks == 0 && nauseaTicks == 0 && hungerTicks == 0 && foodLoss == 0 && healthLoss == 0; }
    }

    public static Backlash backlash(double severity) {
        if (severity <= 0.0) return new Backlash(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        double s = Math.min(1.0, severity);
        int loss = s >= 0.66 ? (int) Math.round(4 * s) : s >= 0.33 ? 2 : 0;
        return new Backlash((int) Math.round(100 + 300 * s), s >= 0.66 ? 2 : s >= 0.33 ? 1 : 0,
                (int) Math.round(200 + 1000 * s), s >= 0.66 ? 1 : 0,
                s >= 0.5 ? (int) Math.round(100 + 200 * s) : 0,
                (int) Math.round(200 + 400 * s), (int) Math.round(8 * s),
                s >= 0.9 ? 60 : 0, loss, loss > 0 ? (int) Math.round(3600 * s) : 0);
    }

    /** The maximum health a strain still takes with {@code left} of {@code total} ticks to go (linear, so it heals back as it fades). */
    public static double strainAmount(int healthLoss, long left, long total) {
        if (healthLoss <= 0 || total <= 0 || left <= 0) return 0.0;
        return -healthLoss * Math.min(1.0, left / (double) total);
    }

    /** Vessel in hand or on the body: the natural magicule regeneration this adds (a Heritage-style +25%). */
    public static final double VESSEL_REGEN = 0.25;

    // ---------------------------------------------------------------- Draw Rukh (a channelled recharge)
    public static final int RUKH_MAX_TICKS = 600;           // a channel lasts 30 s at the most
    public static final double RUKH_PER_SECOND = 0.008;     // share of max magicules per second at normal mana density
    public static final double RUKH_MAX_DENSITY = 3.0;      // the local mana density (ManaLaw.density; a Grand Magic Zone is 1.8+) is clamped here

    /** What one second of channelling gives, as a share of the maximum magicules. */
    public static double rukhGain(double density) { return RUKH_PER_SECOND * Math.max(0.25, Math.min(RUKH_MAX_DENSITY, density)); }

    // ---------------------------------------------------------------- assimilation
    /** Assimilation gained per second of use. A household member's Lesser Summon bites hard: ten summons reach the cap. */
    public static float assimilationPerSecond(int phase, boolean household) {
        if (household) return phase >= LESSER ? 0.25f : 0f;
        return switch (phase) {
            case LESSER -> 0.01f;
            case HALF -> 0.03f;
            case FULL -> 0.06f;
            default -> 0f;
        };
    }

    /** Assimilation lost per second while resting (phase 0). */
    public static float assimilationDecayPerSecond(boolean household) { return household ? 0.005f : 0.004f; }

    /** Extra assimilation one Extreme Magic costs. */
    public static final float EXTREME_ASSIMILATION = 2.0f;

    // ---------------------------------------------------------------- trust
    /** Seconds of use between two points of trust (Half or Full). */
    public static final int TRUST_EVERY_TICKS = 600;

    // ---------------------------------------------------------------- Full Equip numbers (read by the engine)
    public static final double MASO_SPEED = 0.30;           // +30% movement speed (multiplied total)
    public static final double MASO_ATTACK = 5.0;           // +5 attack damage
    public static final double MASO_ARMOR = 6.0, MASO_TOUGHNESS = 3.0, MASO_KNOCKBACK = 0.4, MASO_HEALTH = 8.0;
    public static final double MASO_EP = 0.25;              // +25% max magicule and aura: Tensura's EP follows both
    public static final double MASO_ELEMENT = 0.30;         // +30% of the Djinn's own element boost
    public static final float MASO_FLY_SPEED = 0.075f;      // vanilla creative flight is 0.05
    public static final int MASO_LANDING_TICKS = 200;       // Slow Falling when it ends, so the fall is never the backlash
}
