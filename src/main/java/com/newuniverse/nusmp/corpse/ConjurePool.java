package com.newuniverse.nusmp.corpse;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: until 1.46.0 Soul Corpse Reanimation could only raise creatures that had died near the caster in the last minute, so a mage with the
 * magic and no recent dead summoned nothing. With nothing to raise, the earth now gives up a body of its own, and WHICH body should fit the
 * place (husks in the desert, strays in the snow, drowned in the water, skeletons in the dark and the Nether). This pure class holds that
 * choice (the setting, the weighted pool, how many bodies the caster's mastery allows) so it can be unit tested without the game. It names
 * only vanilla undead; the optional Tensura undead are added by the caller only when they exist in the registry.
 */
public final class ConjurePool {
    private ConjurePool() {}

    public enum Setting { SURFACE, DRY, COLD, WET, UNDERGROUND, NETHER, END }

    /** One candidate body: a registry id and its weight in the draw. */
    public record Entry(String id, int weight) {}

    /** Optional Tensura undead (only used when the id exists as an entity type); weak ones, low weight. */
    public static final List<String> TENSURA_UNDEAD = List.of("tensura:ghoul", "tensura:wight");

    /**
     * The setting of a caster's position. Dimension first (Nether, End), then water, then the dark, then the climate.
     * @param dimension the dimension id, e.g. "minecraft:the_nether"
     */
    public static Setting setting(String dimension, boolean inWater, boolean underground, boolean cold, boolean dry) {
        if ("minecraft:the_nether".equals(dimension)) return Setting.NETHER;
        if ("minecraft:the_end".equals(dimension)) return Setting.END;
        if (inWater) return Setting.WET;
        if (underground) return Setting.UNDERGROUND;
        if (cold) return Setting.COLD;
        if (dry) return Setting.DRY;
        return Setting.SURFACE;
    }

    /** The weighted pool of vanilla undead for a setting. Never empty. */
    public static List<Entry> pool(Setting s) {
        return switch (s) {
            case SURFACE -> List.of(new Entry("minecraft:zombie", 6), new Entry("minecraft:skeleton", 4));
            case DRY -> List.of(new Entry("minecraft:husk", 6), new Entry("minecraft:skeleton", 3), new Entry("minecraft:zombie", 1));
            case COLD -> List.of(new Entry("minecraft:stray", 6), new Entry("minecraft:zombie", 3), new Entry("minecraft:skeleton", 1));
            case WET -> List.of(new Entry("minecraft:drowned", 8), new Entry("minecraft:zombie", 2));
            case UNDERGROUND -> List.of(new Entry("minecraft:skeleton", 5), new Entry("minecraft:zombie", 4), new Entry("minecraft:stray", 1));
            case NETHER -> List.of(new Entry("minecraft:zombified_piglin", 5), new Entry("minecraft:skeleton", 4), new Entry("minecraft:wither_skeleton", 1));
            case END -> List.of(new Entry("minecraft:skeleton", 5), new Entry("minecraft:zombie", 5));
        };
    }

    /** The pool with the Tensura undead that exist added at weight 1 each (they stay rare). {@code available} says whether an id is registered. */
    public static List<Entry> poolWithModded(Setting s, java.util.function.Predicate<String> available, boolean includeModded) {
        List<Entry> out = new ArrayList<>(pool(s));
        if (includeModded && s != Setting.WET) for (String id : TENSURA_UNDEAD) if (available.test(id)) out.add(new Entry(id, 1));
        return out;
    }

    /** A weighted pick: {@code roll} in [0,1). Out-of-range rolls are clamped; an empty list gives null. */
    public static String pick(List<Entry> pool, double roll) {
        if (pool == null || pool.isEmpty()) return null;
        int total = 0;
        for (Entry e : pool) total += Math.max(0, e.weight());
        if (total <= 0) return pool.get(0).id();
        double r = Math.max(0, Math.min(0.999999999, roll)) * total;
        for (Entry e : pool) {
            r -= Math.max(0, e.weight());
            if (r < 0) return e.id();
        }
        return pool.get(pool.size() - 1).id();
    }

    /**
     * How many bodies one casting conjures: 1 for a beginner, 2 from a third of the mastery bar, 3 from two thirds; never above {@code cap}
     * (the corpse.raiseMax setting), never below 1.
     */
    public static int count(double masteryFraction, int cap) {
        int n = 1 + (masteryFraction >= 1.0 / 3.0 ? 1 : 0) + (masteryFraction >= 2.0 / 3.0 ? 1 : 0);
        return Math.max(1, Math.min(n, Math.max(1, cap)));
    }
}
