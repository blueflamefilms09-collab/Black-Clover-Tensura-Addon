package com.newuniverse.nusmp.stones;

/**
 * Why: every number of the Infinity Stone slot that can be decided without a world lives here so it can be tested without Minecraft: the EP gate, the scale, the cost and cooldown factors the Power and
 * Time stones lend to every relic act, the away-days rule, the never-lethal recoil, the Foresight dodge and the Soul anchor. Pure; no Minecraft classes. The numbers are design values (docs/infinity_stones_phase_a.md).
 */
public final class StoneRules {
    private StoneRules() {}

    public static final double GATE_EP = 1.0e9;
    public static final double SCALE_CAP = 2.0;
    public static final double MIN_FACTOR = 0.5;
    public static final String SLOT = "infinity_stone";
    /** Nothing may be offered at the Vormir altar except these (a half of the owner's EP offering would drop the bearer under the gate, so it is not offered). */
    public static final String[] OFFERINGS = {"skill", "pet"};

    /** The gate reads BASE maximum EP (never the +20% of an Akashic grade), so a stone cannot lift its bearer over its own gate. */
    public static boolean gateOk(double baseMaxEp, double gate) {
        return gate > 0 && baseMaxEp >= gate;
    }

    /** {@code 1 + log10(EP / gate)}, at least 1 and at most 2: a tenfold gate is already the cap. */
    public static double stoneScale(double baseMaxEp, double gate) {
        if (!(baseMaxEp > 0) || !(gate > 0) || baseMaxEp <= gate) return 1.0;
        return Math.min(SCALE_CAP, 1.0 + Math.log10(baseMaxEp / gate));
    }

    /** The Power Stone: every relic act of its bearer costs 15% less. */
    public static double costFactor(InfinityStone worn) { return worn == InfinityStone.POWER ? 0.85 : 1.0; }

    /** The Power Stone (-20%) and the Time Stone (-25%, cooldowns tick faster): every relic act of the bearer recovers sooner. Never under half. */
    public static double cooldownFactor(InfinityStone worn) {
        double f = worn == InfinityStone.POWER ? 0.80 : worn == InfinityStone.TIME ? 0.75 : 1.0;
        return Math.max(MIN_FACTOR, f);
    }

    /** The recoil of the Power Surge: a fraction of maximum health, never more than leaves the bearer one health point. */
    public static float recoil(float maxHealth, float health, double fraction) {
        float r = (float) (maxHealth * Math.max(0.0, fraction));
        return Math.max(0f, Math.min(r, health - 1f));
    }

    /** The Time Stone's Foresight: a hit of a fifth of maximum health or more, once per cooldown, is turned aside. */
    public static boolean foresightDodges(float damage, float maxHealth, long now, long readyAt) {
        return maxHealth > 0 && damage >= maxHealth * 0.20f && now >= readyAt;
    }

    /** The Soul Stone's anchor: a blow that would kill, once per cooldown. */
    public static boolean soulAnchors(float damage, float healthAndAbsorb, long now, long readyAt) {
        return damage >= healthAndAbsorb && now >= readyAt;
    }

    /** Health the Soul anchor leaves: a fifth of the maximum, at least 1. */
    public static float anchorHealth(float maxHealth) { return Math.max(1f, maxHealth * 0.20f); }

    /** The away rule: a stone whose holder has been offline longer than the limit is declared lost to the Order's ledger (it returns to the world). A limit of 0 or less switches the rule off. */
    public static boolean awayExpired(boolean offline, long sinceSeenMillis, long maxAwayMillis) {
        return offline && maxAwayMillis > 0 && sinceSeenMillis > maxAwayMillis;
    }

    /** Is this a thing that may be offered at the Vormir altar? Only a skill or a named pet of the bearer; never a player, never EP. */
    public static boolean offeringAllowed(String kind) {
        if (kind == null) return false;
        for (String k : OFFERINGS) if (k.equals(kind)) return true;
        return false;
    }

    /** The potency of a slot passive: its base share grows with the stone scale, never past the cap. */
    public static double passive(double base, double scale) { return base * Math.max(1.0, Math.min(SCALE_CAP, scale)); }

    /** The Soulworld heals this fraction of maximum health each second for 15 seconds: never more than 3% a pulse, so a whole cast is at most 45% (the law allows 50%). */
    public static double soulworldPulse(double scale) { return Math.min(0.03, 0.02 * Math.max(1.0, scale)); }

    /** The ticks of Time Lock for a kind of target, within the balance law (1 s boss, 3 s player, 5 s mob). */
    public static int lockTicks(boolean boss, boolean player) { return boss ? 20 : player ? 60 : 100; }
}
