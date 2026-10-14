package com.newuniverse.nusmp.relics.djinn.dark;

import java.util.List;

/**
 * The Dark Djinn as numbers (pure, so DarkRulesTest can check every one). In Magi a Djinn whose bond is fed with black rukh turns dark: the
 * power is greater and the bearer's nature is eaten by it faster. Becoming dark is a choice with a price, never an accident; it can be undone.
 *
 * <p>Every effect here is a multiplier or an offset on a number the Djinn system already has ({@code DjinnPhaseRules}, {@code Djinn.Trait},
 * {@code Djinn.Extreme}); nothing is a new pool or meter. What each stat runs on stays what it was: Magoi on Tensura's magicules, trust on the
 * ability-mastery-gain modifier, assimilation on the ability-learning-gain modifier. The caps of the balance law (30% of a player's health a hit,
 * 45% for an Extreme Magic, the 6-point rider on a player) are never touched here.
 *
 * <p>The first parameter of the "if dark" functions is the plain value, the last the dark flag, so the engine reads {@code f(plain, dark)}.
 */
public final class DarkRules {
    private DarkRules() {}

    // ---------------------------------------------------------------- becoming dark and coming back
    /** Trust the Djinn needs before it will drink the black rukh. */
    public static final int DRINK_TRUST = 50;
    /** Assimilation (percent) from which a Full Equip may darken by itself when a shard is carried. */
    public static final float FALL_ASSIMILATION = 90f;
    /** The chance per minute of that unforced fall (each second rolls the equivalent share of it). */
    public static final double FALL_PER_MINUTE = 0.10;

    /** Why a drink of black rukh is refused. */
    public enum Verdict { OK, NOT_SNEAKING, NOT_OWNER, HOUSEHOLD_LENT, HOUSEHOLD_MEMBERS, ALREADY_DARK, NO_VESSEL, NEED_TRUST, PHASE_ON, COLLAPSE, NO_SHARD }

    /** Everything the drink depends on. {@code vesselInHand}: the other hand holds the vessel bound to the player's own Djinn. */
    public record Drink(boolean sneaking, boolean owns, boolean lent, int householdMembers, boolean dark, boolean vesselInHand, int trust, int phase,
                        boolean collapse, boolean hasShard, boolean creative) {}

    /** May the owner drink now? The order is the order the player is told in. */
    public static Verdict drink(Drink d) {
        if (!d.sneaking()) return Verdict.NOT_SNEAKING;
        if (d.lent()) return Verdict.HOUSEHOLD_LENT;                        // a household member never gets a dark Djinn
        if (!d.owns()) return Verdict.NOT_OWNER;
        if (d.dark()) return Verdict.ALREADY_DARK;
        if (d.householdMembers() > 0) return Verdict.HOUSEHOLD_MEMBERS;     // a dark Djinn cannot be lent: call the household back first
        if (!d.vesselInHand()) return Verdict.NO_VESSEL;
        if (d.collapse() && !d.creative()) return Verdict.COLLAPSE;
        if (d.phase() != 0) return Verdict.PHASE_ON;
        if (!d.creative() && d.trust() < DRINK_TRUST) return Verdict.NEED_TRUST;
        if (!d.hasShard()) return Verdict.NO_SHARD;
        return Verdict.OK;
    }

    /**
     * Does a non-dark owner in a Full Equip fall to the black rukh this second? Only with a shard in the pack, from 90% assimilation, never in a
     * collapse, never twice. {@code roll} is uniform [0,1).
     */
    public static boolean falls(double roll, boolean owns, boolean lent, boolean dark, int phase, float assimilation, boolean hasShard, boolean collapse) {
        if (!owns || lent || dark || collapse || !hasShard) return false;
        if (phase < 3 || assimilation < FALL_ASSIMILATION) return false;
        return roll < fallPerSecond();
    }

    /** The per-second share of {@link #FALL_PER_MINUTE}: 1 - (1 - p)^(1/60), so sixty seconds add up to exactly that chance. */
    public static double fallPerSecond() { return 1.0 - Math.pow(1.0 - FALL_PER_MINUTE, 1.0 / 60.0); }

    // ---------------------------------------------------------------- Half and Full Equip
    /** The Djinn's rider (extra elemental damage on a blow) grows by a third and a half: +35% of the rider, its cap 6 -> 9. */
    public static final double RIDER_FRAC_MULT = 1.35, RIDER_CAP_MULT = 1.5;

    public static double riderFrac(double plain, boolean dark) { return dark ? plain * RIDER_FRAC_MULT : plain; }

    public static double riderCap(double plain, boolean dark) { return dark ? plain * RIDER_CAP_MULT : plain; }

    /** Maso (Full Equip) numbers: +10% more speed (0.30 -> 0.40), +2 more attack (5 -> 7), the Djinn's element boost +50% instead of +30%. */
    public static final double MASO_SPEED_PLUS = 0.10, MASO_ATTACK_PLUS = 2.0, MASO_ELEMENT = 0.50;

    public static double masoSpeed(double plain, boolean dark) { return dark ? plain + MASO_SPEED_PLUS : plain; }

    public static double masoAttack(double plain, boolean dark) { return dark ? plain + MASO_ATTACK_PLUS : plain; }

    public static double masoElement(double plain, boolean dark) { return dark ? MASO_ELEMENT : plain; }

    /** The flat element bonus of a Strength (attack) or Life (health) Djinn grows by the same ratio as the boost: x5/3. */
    public static double masoElementFlat(double plain, double plainBoost, boolean dark) { return dark ? plain * MASO_ELEMENT / plainBoost : plain; }

    // ---------------------------------------------------------------- Extreme Magic
    /** Raw damage x1.25 (still through the balance law and the 45% player cap); the Magoi price x0.8. */
    public static final double EXTREME_DAMAGE = 1.25, EXTREME_PRICE = 0.8;

    public static double extremeDamage(double plain, boolean dark) { return dark ? plain * EXTREME_DAMAGE : plain; }

    public static double extremePrice(double plain, boolean dark) { return dark ? plain * EXTREME_PRICE : plain; }

    /** A dark Djinn's trust cannot rise above this, so its Extreme Magic opens at it (the plain gate of 70 is out of reach). */
    public static final int TRUST_CAP = 60;

    // ---------------------------------------------------------------- the price
    /** Magoi upkeep x1.3; assimilation rises x2 (and so does the extra one an Extreme Magic costs). */
    public static final double UPKEEP = 1.3, ASSIMILATION = 2.0;

    public static double upkeep(double plain, boolean dark) { return dark ? plain * UPKEEP : plain; }

    public static float assimilationRate(float plain, boolean dark) { return dark ? (float) (plain * ASSIMILATION) : plain; }

    /**
     * Trust after a change: a dark Djinn's trust may fall freely and may rise only up to {@link #TRUST_CAP} (a trust above it, kept from before the
     * drink, is neither raised nor taken away).
     */
    public static int trustAfter(int old, int wanted, boolean dark) {
        if (!dark || wanted <= old) return wanted;
        return Math.min(wanted, Math.max(old, TRUST_CAP));
    }

    /** The trust the Full and Extreme gates see: a dark Djinn with {@link #TRUST_CAP} trust passes the Extreme Magic's gate of 70. */
    public static int gateTrust(int trust, int extremeGate, boolean dark) { return dark && trust >= TRUST_CAP ? Math.max(trust, extremeGate) : trust; }

    /** The trust the player is told the Extreme Magic needs. */
    public static int extremeTrustNeeded(int plainGate, boolean dark) { return dark ? Math.min(plainGate, TRUST_CAP) : plainGate; }

    /** Backlash severity of a Maso that ended: +0.15 when it ended by the bearer's will, by timeout or by burning out (never for death, logout, an admin or a lost vessel). */
    public static final double BACKLASH_PLUS = 0.15;

    public static double backlash(double severity, boolean dark, boolean deliberateEnd) {
        if (!dark || !deliberateEnd) return severity;
        return Math.min(1.0, Math.max(0.0, severity) + BACKLASH_PLUS);
    }

    /** The share of the Djinn's element the bearer shrugs off is halved. */
    public static final double CALM = 0.5;

    public static double calm(double plain, boolean dark) { return dark ? plain * CALM : plain; }

    // ---------------------------------------------------------------- the unease of a dark Full Equip
    public static final double AURA_RADIUS = 6.0;
    public static final int FEAR_TICKS = 60, UNEASE_TICKS = 40, MAX_FRIGHTENED = 16, MAX_UNEASY = 8;

    // ---------------------------------------------------------------- Tensura skills lent while dark
    /** Darkness-themed Tensura skills (all in the owner's Tensura id reference), lent on top of the Djinn's own set. */
    public static final String DARKNESS_RESISTANCE = "tensura:darkness_attack_resistance", SHADOW_MOTION = "tensura:shadow_motion", MORTAL_FEAR = "tensura:mortal_fear";

    /** The extra skills of a phase (1-3): none in a Lesser Summon, resistance from Half Equip, the shadow and the fear of death in Full Equip. */
    public static List<String> skills(int phase) {
        if (phase >= 3) return List.of(DARKNESS_RESISTANCE, SHADOW_MOTION, MORTAL_FEAR);
        if (phase >= 2) return List.of(DARKNESS_RESISTANCE);
        return List.of();
    }

    // ---------------------------------------------------------------- where the shards come from
    /** A floor boss falling (the Blood-Red Commander, the Devil Avatar) or Lucius dying: a roll for one shard. A vault chest: a smaller one. */
    public static final double BOSS_CHANCE = 0.15, VAULT_CHANCE = 0.02;

    /** The Nexus zone names whose boss may drop a shard: the Blood-Red Commander's Monarch Vault and the Devil Avatar's Underworld Gate. */
    public static boolean isShardZone(String zoneName) { return "MONARCH_VAULT".equals(zoneName) || "UNDERWORLD_GATE".equals(zoneName); }

    /** The boss of a shard zone, for the message. */
    public static String bossName(String zoneName) { return "UNDERWORLD_GATE".equals(zoneName) ? "the Devil Avatar" : "the Blood-Red Commander"; }

    /** Does this roll (uniform [0,1)) win a shard against the chance? A chance at or below 0 never does, at or above 1 always. */
    public static boolean wins(double roll, double chance) { return chance > 0.0 && roll < chance; }
}
