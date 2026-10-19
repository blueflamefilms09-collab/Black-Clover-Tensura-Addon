package com.newuniverse.nusmp.book;

import java.util.List;

/**
 * Transmutation Magic (Grey), 1.93.0 rebuild: the pure numbers and ids of the book (no Minecraft types, so the rules are unit-tested: TransmutationRulesTest). The four old page ids keep their
 * order and their mastery bits; the four new pages are appended after them.
 */
public final class TransmutationRules {
    private TransmutationRules() {}

    public static final String IRON_SPIKES = "iron_spikes", MAGIC_CONVERT = "magic_convert", QUAGMIRE = "quagmire", GRAND = "grand_transmutation";
    public static final String BLADES = "alchemic_blades", FLOWERING = "flowering", SENTINEL = "transmuted_sentinel", ARMOUR = "alchemist_armour";
    /** The pages that existed before the rebuild, in their order. */
    public static final List<String> OLD_IDS = List.of(IRON_SPIKES, MAGIC_CONVERT, QUAGMIRE, GRAND);
    /** The pages the rebuild appended, in their order. */
    public static final List<String> NEW_IDS = List.of(BLADES, FLOWERING, SENTINEL, ARMOUR);

    public static final int ARMOUR_TICKS = 400, SHORT_ARMOUR_TICKS = 100, SENTINEL_TICKS = 600, BLADE_TICKS = 200, FLOWER_RADIUS = 7;
    public static final int MAX_LIVING_SENTINELS = 2;

    /** Spikes in the line of Iron Spikes: 9 at size 1, more for a bigger spell, at least 5 and at most 16. */
    public static int spikeCount(float size) { return Math.max(5, Math.min(16, (int) (9 * size))); }

    /** The share of every blow the Alchemist's Gilded Plate takes: 20 % at 1,000 EP, 1.5 % more per EP step, at most 35 %. */
    public static float armourCut(double epScale) { return (float) Math.max(0.20, Math.min(0.35, 0.20 + 0.015 * (epScale - 1))); }

    /** A summon's life in ticks: longer for a mighty caster, at most 50 % more. */
    public static int summonTicks(int base, double epScale) { return (int) (base * Math.min(1.5, 1 + 0.04 * Math.max(0, epScale - 1))); }

    /** Alchemic blades orbiting the caster: 3, plus one for every two EP modifier steps (0..4), so 3 to 5. */
    public static int bladeCount(int modifier) { return 3 + Math.max(0, Math.min(4, modifier)) / 2; }

    /** Harmful effects Flowering strips from each ally: 2 to 4. */
    public static int flowerCleanses(int modifier) { return 2 + Math.max(0, Math.min(4, modifier)) / 2; }

    /** The scale of the Transmuted Sentinel's model: 0.9 to 1.06. */
    public static float sentinelScale(double epScale) { return 0.9f + 0.02f * (float) Math.min(8.0, Math.max(0, epScale - 1)); }

    /** The damage of one hit of an alchemic blade, before the balance law. */
    public static float bladeHit(float size) { return 5f * Math.max(0.6f, Math.min(1.8f, size)); }
}
