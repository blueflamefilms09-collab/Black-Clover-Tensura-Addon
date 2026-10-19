package com.newuniverse.nusmp.book;

import java.util.List;

/**
 * Recombination Magic (Henry Legolant), 1.93.0 rebuild: the pure numbers and ids of the book (RecombinationRulesTest). The four old page ids keep their order and mastery bits; four new
 * pages are appended after them.
 */
public final class RecombinationRules {
    private RecombinationRules() {}

    public static final String CORKSCREW = "mana_corkscrew", BULWARK = "bulwark", ROOM_SWAP = "room_swap", BLACK_BULL = "raging_black_bull";
    public static final String HOUSE_ARMOUR = "house_armour", ROOM_REBUILD = "room_rebuild", ULTRA_BULL = "ultra_giant_bull", PLANK_SNARE = "plank_snare";
    public static final List<String> OLD_IDS = List.of(CORKSCREW, BULWARK, ROOM_SWAP, BLACK_BULL);
    public static final List<String> NEW_IDS = List.of(HOUSE_ARMOUR, ROOM_REBUILD, ULTRA_BULL, PLANK_SNARE);

    public static final int BULL_TICKS = 1200, ULTRA_TICKS = 400, ARMOUR_TICKS = 400, SHORT_ARMOUR_TICKS = 100, SNARE_TICKS = 60;
    /** The Black Bull's cooldown for a ram, a cannon volley, and the ultra volley, in ticks. */
    public static final int RAM_COOLDOWN = 50, CANNON_COOLDOWN = 120, ULTRA_CANNON_COOLDOWN = 40;
    /** The bit of the prop's state word that marks the Ultra Giant Bull. */
    public static final int ULTRA_BIT = 1 << 18;

    /** The Bull's scale: 1.1 at 1,000 EP, up to 1.34 (the Ultra form is 1.6 times that). */
    public static float bullScale(double epScale, boolean ultra) {
        float s = 1.1f + 0.03f * (float) Math.min(8.0, Math.max(0, epScale - 1));
        return ultra ? s * 1.6f : s;
    }

    public static int summonTicks(int base, double epScale) { return (int) (base * Math.min(1.5, 1 + 0.04 * Math.max(0, epScale - 1))); }

    /** Cannons that fire in one volley: 2, or 4 for the Ultra Giant Bull. */
    public static int cannonShots(boolean ultra, int modifier) { return (ultra ? 4 : 2) + (modifier >= 3 ? 1 : 0); }

    public static float ramDamage(boolean ultra) { return ultra ? 20f : 14f; }

    public static float cannonDamage(boolean ultra) { return ultra ? 9f : 6f; }

    /** The share of every blow the house takes off the wearer of the House Armour: 25 % at 1,000 EP, at most 38 %. */
    public static float armourCut(double epScale) { return (float) Math.max(0.25, Math.min(0.38, 0.25 + 0.016 * (epScale - 1))); }

    /** Hit points Room Rebuild gives each ally: 6 at 1,000 EP and 1.5 more per EP step. */
    public static float roomHeal(double epScale) { return 6f + 1.5f * (float) Math.min(8.0, Math.max(0, epScale - 1)); }

    /** How long the snare holds a foe, in ticks, after the control law (a boss is held for a quarter). */
    public static int snareTicks(boolean boss, int lawTicks) { return boss ? Math.max(8, lawTicks / 4) : lawTicks; }
}
