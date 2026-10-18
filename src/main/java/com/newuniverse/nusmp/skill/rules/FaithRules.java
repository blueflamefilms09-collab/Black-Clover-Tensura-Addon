package com.newuniverse.nusmp.skill.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Why: Faith (skill bestowal) and its Ultimate Goddess of Faith move a stored number (the Faith points) around, and a wrong clamp or a
 * negative balance would corrupt saves, so every rule that touches Faith is here and unit-tested. The points live in the skill tag under
 * the key "Faith" and carry over unchanged when Faith evolves into Goddess of Faith.
 */
public final class FaithRules {
    private FaithRules() {}

    /** The tag key of the stored points (unchanged since the first release: old worlds keep their Faith). */
    public static final String FAITH_KEY = "Faith";
    public static final int MAX_FAITH = 1_000_000;
    public static final int BLESSING_COST = 2, MIRACLE_COST = 5;

    public static int clamp(long faith) { return (int) Math.max(0L, Math.min(MAX_FAITH, faith)); }

    /** Faith one prayer yields. */
    public static int prayGain(Tier tier) { return tier.pick(1, 2, 3); }

    /** Faith one bestowal costs (the Ultimate halves it, never below 1). */
    public static int bestowCost(int configCost, Tier tier) {
        if (configCost <= 0) return 0;
        return tier == Tier.ULTIMATE ? Math.max(1, configCost / 2) : configCost;
    }

    public static int bestowCooldown(int configCooldown, Tier tier) {
        return tier == Tier.ULTIMATE ? configCooldown / 2 : configCooldown;
    }

    public static boolean canSpend(int faith, int cost) { return cost <= 0 || faith >= cost; }

    /** New balance after spending; never below 0. */
    public static int spend(int faith, int cost) { return clamp((long) faith - Math.max(0, cost)); }

    public static int add(int faith, int gain) { return clamp((long) faith + Math.max(0, gain)); }

    /** The configured skill ids the target does not own yet: each valid id once, in config order. */
    public static List<String> giftPool(List<String> configured, Predicate<String> targetHas, Predicate<String> exists) {
        Set<String> seen = new LinkedHashSet<>();
        if (configured != null) for (String id : configured) {
            if (id == null || id.isBlank()) continue;
            String s = id.trim();
            if (!exists.test(s) || targetHas.test(s)) continue;
            seen.add(s);
        }
        return new ArrayList<>(seen);
    }

    /** Index of the gift from a random roll (any int, even negative). */
    public static int pickIndex(int poolSize, int roll) { return poolSize <= 0 ? -1 : Math.floorMod(roll, poolSize); }

    /** Radius of the Ultimate's Blessing and Miracle (blocks). */
    public static double blessingRadius(Tier tier) { return tier.pick(0.0, 0.0, 16.0); }

    public static int blessingTicks() { return 1200; }
    public static int blessingCooldown() { return 1800; }
    public static int miracleCooldown() { return 6000; }
}
