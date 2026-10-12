package com.newuniverse.nusmp.entity.zogratis;

import java.util.Random;

/**
 * Grimoire Tower Access (pure): Lucius can cast any spell the grimoire towers of the Clover Kingdom have ever recorded. A mob cannot
 * hold a grimoire, so each page of each magic is cast as the spell of its <i>tier</i>: the page's cost class decides the shape (a
 * starter page is a bolt, a middle page a lance, a zone page a nova or a lingering field, a signature page a rain), the magic decides
 * the colours and the rider. This class holds the tier mapping, the counts and damage per shape, and the deck that makes the towers
 * answer with every magic before any repeats.
 */
public final class TowerMath {
    private TowerMath() {}

    public enum Shape {
        /** Homing bolts, a few at once. */
        BOLT,
        /** A beam along a marked line. */
        LANCE,
        /** A ring that closes on a marked spot. */
        NOVA,
        /** A lingering field that pulses. */
        FIELD,
        /** Bolts falling on marked spots. */
        RAIN
    }

    /** The shape of a page by its cost in percent of the caster's magicule (the grimoire's starter / mid / zone / signature tiers). */
    public static Shape shapeFor(double costPercent, int salt) {
        if (costPercent >= 30) return Shape.RAIN;
        if (costPercent >= 18) return Math.floorMod(salt, 2) == 0 ? Shape.NOVA : Shape.FIELD;
        if (costPercent >= 10) return Shape.LANCE;
        return Shape.BOLT;
    }

    /** How many bolts, beams, rings, fields or falling bolts one cast makes at a power's volley size (1..4). */
    public static int count(Shape shape, int castSlots) {
        int s = Math.max(1, Math.min(4, castSlots));
        return switch (shape) {
            case BOLT -> 2 + s;                 // 3 .. 6
            case LANCE -> 1 + (s + 1) / 2;      // 2 .. 3
            case NOVA -> 1 + s / 3;             // 1 .. 2
            case FIELD -> 1;
            case RAIN -> 3 + s;                 // 4 .. 7
        };
    }

    /** Base damage of one hit of a shape (before the boss's damage law). */
    public static float baseDamage(Shape shape) {
        return switch (shape) {
            case BOLT -> 6.5f;
            case LANCE -> 11f;
            case NOVA -> 13f;
            case FIELD -> 5f;
            case RAIN -> 9f;
        };
    }

    /**
     * Ticks of telegraph a marked shape gives after its marks appear (rings on the ground, beams drawn). A mark is drawn where the
     * foe stands, so the ticks must be enough to walk out of it: radius / ticks stays under {@link #WALK_BLOCKS_PER_TICK}.
     */
    public static int markTicks(Shape shape) {
        return switch (shape) {
            case BOLT -> 0;
            case LANCE -> 14;
            case NOVA -> 24;
            case FIELD -> 22;
            case RAIN -> 16;
        };
    }

    /** Radius of the area a shape marks. */
    public static double radius(Shape shape) {
        return switch (shape) {
            case NOVA -> 5.0;
            case FIELD -> 4.5;
            case RAIN -> 2.2;
            default -> 1.0;
        };
    }

    /** A player's walking speed, blocks a tick (4.3 blocks a second): every mark can be left at a walk. */
    public static final double WALK_BLOCKS_PER_TICK = 0.216;

    /** What a clone's weak copy of a spell does: the same shape, a third of the damage. */
    public static float cloneDamage(Shape shape) { return (float) (baseDamage(shape) * SoulMath.CLONE_DAMAGE); }

    /** "The towers answer: Flame Magic - Flame Bullet". */
    public static String announcement(String magic, String page) { return "The towers answer: " + magic + (page == null || page.isEmpty() ? "" : " - " + page); }

    // ---------------------------------------------------------------- the deck
    /**
     * A shuffled deck over 'size' choices: every index comes up once before any comes up again, and the first card of a new deck is
     * never the last card of the old one. Deterministic for a seed.
     */
    public static final class Deck {
        private final int size;
        private final Random rng;
        private int[] order;
        private int at, last = -1;

        public Deck(int size, long seed) {
            this.size = Math.max(1, size);
            this.rng = new Random(seed);
            reshuffle();
        }

        private void reshuffle() {
            order = new int[size];
            for (int i = 0; i < size; i++) order[i] = i;
            for (int i = size - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int t = order[i];
                order[i] = order[j];
                order[j] = t;
            }
            if (size > 1 && order[0] == last) {
                int j = 1 + rng.nextInt(size - 1);
                int t = order[0];
                order[0] = order[j];
                order[j] = t;
            }
            at = 0;
        }

        /** The next index. */
        public int next() {
            if (at >= order.length) reshuffle();
            last = order[at++];
            return last;
        }

        public int size() { return size; }
    }
}
