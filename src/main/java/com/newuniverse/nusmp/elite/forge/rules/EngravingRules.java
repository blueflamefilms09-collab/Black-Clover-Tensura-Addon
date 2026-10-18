package com.newuniverse.nusmp.elite.forge.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the pure half of the Engraving Handler. Tensura's engravings are enchantments; how MANY a forged item can carry is capped by its quality (Common 0,
 * Uncommon 1, Rare 2, Epic 3, Legendary 4, Mythical 5; +1 for a matching Weaponsmith/Armorsmith/Toolsmith, never more than 6), at what LEVEL they are applied
 * depends on the quality (1, +1 from Epic, +1 more at Mythical, +1 for a Runesmith), and WHICH ones are chosen comes from the specialization's pool in an order
 * fixed by a seed (so a plan is reproducible and testable). The forge itself fills half the slots (rounded up) when an item is made; the rest the player fills
 * at the station for a price. The Minecraft half ({@code EngravingHandler}) turns a plan into enchantments, skipping ids Tensura does not have. No Minecraft classes.
 */
public final class EngravingRules {
    private EngravingRules() {}

    /** The hard cap on engravings from the forge on one item. */
    public static final int MAX_SLOTS = 6;

    /** One engraving to apply: the Tensura enchantment id (without namespace) and the wished level (capped by the enchantment's own maximum at run time). */
    public record Pick(String id, int level) {}

    /** Slots a quality gives before specialization. */
    public static int slots(Quality q) { return q == null ? 0 : q.level() - 1; }

    /** Slots for a quality, a specialization (nullable) and the category of the item. */
    public static int totalSlots(Quality q, Specialization s, Category c) {
        int n = slots(q);
        if (n > 0 && s != null) n += s.slotBonus(c);
        return Math.min(MAX_SLOTS, n);
    }

    /** How many engravings the forge applies by itself when the item is made. */
    public static int initialCount(int totalSlots) { return (Math.max(0, totalSlots) + 1) / 2; }

    /** The level the forge engraves at. */
    public static int levelFor(Quality q, Specialization s) {
        int lv = 1 + (q != null && q.level() >= Quality.EPIC.level() ? 1 : 0) + (q == Quality.MYTHICAL ? 1 : 0) + (s != null ? s.levelBonus() : 0);
        return Math.max(1, lv);
    }

    /** The engravings a smith may use. */
    public static List<String> pool(Specialization s) { return s == null ? Specialization.BASIC_POOL : s.pool(); }

    /**
     * The pool in a seeded order (a Fisher-Yates shuffle with {@link ForgeRng}), with the ids in {@code exclude} (already on the item) left out. The caller applies
     * the first ones that Tensura has and the item can take; the order is the same for the same seed, so a plan can be reproduced and tested.
     */
    public static List<String> order(Specialization s, long seed, List<String> exclude) {
        List<String> ids = new ArrayList<>(pool(s));
        if (exclude != null) ids.removeAll(exclude);
        for (int i = ids.size() - 1; i > 0; i--) {
            int j = (int) (ForgeRng.unit(seed, 90, i) * (i + 1));
            String t = ids.get(i);
            ids.set(i, ids.get(j));
            ids.set(j, t);
        }
        return ids;
    }

    /** A plan of up to {@code want} engravings, never more than the free slots ({@code totalSlots - applied}). */
    public static List<Pick> plan(Quality q, Specialization s, Category c, int applied, int want, long seed, List<String> exclude) {
        int free = Math.max(0, totalSlots(q, s, c) - Math.max(0, applied));
        int n = Math.min(Math.max(0, want), free);
        int level = levelFor(q, s);
        List<Pick> out = new ArrayList<>();
        for (String id : order(s, seed, exclude)) {
            if (out.size() >= n) break;
            out.add(new Pick(id, level));
        }
        return out;
    }
}
