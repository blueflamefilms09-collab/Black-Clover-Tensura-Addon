package com.newuniverse.nusmp.labyrinth.mega.rift;

import java.util.function.IntPredicate;

/**
 * Why: every number and decision of the rifts that does not need a world, in one pure class so it is unit-tested without the game: how far a shortcut jumps (3 to 5 floors, rolled from the
 * rift's seed so the same rift always leads to the same place), how a jump is held short of a decennial boss (a shortcut must never skip the boss gate that the Mana Core opens), how
 * open a rift looks (it grows, holds, then gutters before it closes), where the instanced pocket of a slot stands, which variant a pocket is, the order of its puzzle and how a puzzle
 * answers a press. The game classes ask this class and carry out the answer.
 */
public final class RiftRules {
    private RiftRules() {}

    /** A shortcut jumps 3 to 5 floors (owner's brief), unless the config narrows it. */
    public static final int JUMP_MIN = 3, JUMP_MAX = 5;
    /** Ticks a rift takes to open, and the last ticks in which it gutters before it closes. */
    public static final int GROW_TICKS = 30, GUTTER_TICKS = 80;

    /** A splitmix64 step: a well mixed value from a seed and a salt. */
    public static long mix(long seed, long salt) {
        long z = seed + 0x9E3779B97F4A7C15L * (salt + 1);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The jump of a shortcut rift: {@code min .. max} floors, the same for the same seed. */
    public static int jumpFor(long seed, int min, int max) {
        int lo = Math.max(1, Math.min(min, max)), hi = Math.max(lo, max);
        return lo + (int) Math.floorMod(mix(seed, 1), (long) (hi - lo + 1));
    }

    /**
     * The chain index a shortcut from {@code current} leads to: {@code current + jump}, held at the first boss floor on the way (the boss floor itself may be the destination; nothing past it may be), and
     * never lower than {@code current + 1}. {@code boss} says whether a chain index is a boss floor. Returns {@code current} when the player already stands on a boss floor (no shortcut there).
     */
    public static int shortcutTarget(int current, int jump, IntPredicate boss) {
        if (boss.test(current)) return current;
        int want = current + Math.max(1, jump);
        for (int i = current + 1; i <= want; i++) if (boss.test(i)) return i;
        return want;
    }

    /** True when the roll {@code r} (0 .. 1) succeeds at {@code chance}; a chance of 0 or less never does, 1 or more always does. */
    public static boolean rolls(double chance, double r) {
        return chance > 0 && (chance >= 1 || r < chance);
    }

    /** 0 .. 1: how open a rift is {@code age} ticks after it opened with {@code left} ticks to live: grows over {@link #GROW_TICKS}, flickers in the last {@link #GUTTER_TICKS}. */
    public static float openness(int age, int left) {
        float grow = Math.min(1f, Math.max(0f, age / (float) GROW_TICKS));
        if (left >= GUTTER_TICKS) return grow;
        float k = Math.max(0f, left) / GUTTER_TICKS;
        float flicker = 0.75f + 0.25f * (float) Math.sin(left * 1.7);
        return grow * k * flicker;
    }

    // ---------------------------------------------------------------- pockets
    /** Pockets stand in the Mega-Dungeon's dimension, far outside the grid of the sectors: slot {@code n} is a 192 x 192 cell on a line. */
    public static final int POCKET_X0 = 4_000_000, POCKET_Z = 4_000_000, POCKET_Y = 96, POCKET_STRIDE = 192, MAX_SLOTS = 64;

    public static int pocketX(int slot) { return POCKET_X0 + slot * POCKET_STRIDE; }

    /** The slot whose cell holds the x coordinate, or -1 when it is not in the pocket field. */
    public static int slotAt(double x, double z) {
        if (z < POCKET_Z - POCKET_STRIDE / 2.0 || z >= POCKET_Z + POCKET_STRIDE / 2.0) return -1;
        double rel = x - (POCKET_X0 - POCKET_STRIDE / 2.0);
        if (rel < 0) return -1;
        int slot = (int) (rel / POCKET_STRIDE);
        return slot < MAX_SLOTS ? slot : -1;
    }

    /** The two pocket variants: a secret boss with a vault behind it, and a puzzle vault. */
    public enum Variant {
        BOSS("boss"), PUZZLE("puzzle");

        private final String id;

        Variant(String id) { this.id = id; }

        public String id() { return id; }

        public static Variant byId(String id) { return "puzzle".equals(id) ? PUZZLE : BOSS; }
    }

    public static Variant variantFor(long seed, double bossShare) {
        double r = (mix(seed, 2) >>> 11) / (double) (1L << 53);
        return r < bossShare ? Variant.BOSS : Variant.PUZZLE;
    }

    /** The order in which the four coloured plates of a puzzle must be pressed: a permutation of 0 .. 3 from the seed. */
    public static int[] puzzleOrder(long seed) {
        int[] a = {0, 1, 2, 3};
        for (int i = 3; i > 0; i--) {
            int j = (int) Math.floorMod(mix(seed, 10 + i), (long) (i + 1));
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
        return a;
    }

    public enum Press { PROGRESS, SOLVED, WRONG }

    /** The state of one puzzle: how many plates of the order have been pressed right so far. */
    public static final class Puzzle {
        private final int[] order;
        private int done;

        public Puzzle(int[] order) { this(order, 0); }

        /** A puzzle that has already had {@code done} right presses (restored from a save). */
        public Puzzle(int[] order, int done) {
            this.order = order.clone();
            this.done = Math.max(0, Math.min(done, order.length));
        }

        public int done() { return done; }

        public boolean solved() { return done >= order.length; }

        /** A press of plate {@code plate}: the right next plate goes on, the last right one solves it, anything else resets it. */
        public Press press(int plate) {
            if (solved()) return Press.SOLVED;
            if (plate == order[done]) {
                done++;
                return done >= order.length ? Press.SOLVED : Press.PROGRESS;
            }
            done = 0;
            return Press.WRONG;
        }
    }

    /** Ticks left before a pocket collapses, as the whole seconds a warning names: 60, 30, 10, 5, 3, 2, 1 once each, else 0. */
    public static int warnSeconds(long ticksLeft, long previousTicksLeft) {
        int[] marks = {60, 30, 10, 5, 3, 2, 1};
        for (int m : marks) {
            long at = m * 20L;
            if (previousTicksLeft > at && ticksLeft <= at) return m;
        }
        return 0;
    }
}
