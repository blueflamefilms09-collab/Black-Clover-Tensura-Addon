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

    // ---------------------------------------------------------------- 1.94.0: the fixes (every decision here is pure so a unit test can pin it)
    /** How long (ticks) a shortcut whose floor is still being built waits before it settles for the farthest floor that already exists. */
    public static final int FORM_PATIENCE_TICKS = 20 * 45;
    /** After this many failures inside one rift's tick the rift closes itself (the error is logged once per failure; the server never crashes on it). */
    public static final int FAIL_LIMIT = 3;
    /** Ticks between two ambient hints (a faint chime and a rising wisp of Rukh) a rift gives to the delvers near it. */
    public static final int BEACON_EVERY = 40;
    /** Blocks within which a delver hears the chime and within which the opening of a rift is announced to them. */
    public static final int HINT_RADIUS = 48;
    /** The half width of the tear around its block's centre for a player's feet (the block, 0.45 each side, and the half width of the player), and its height above the block's floor. */
    public static final double TEAR_HALF = 1.25, TEAR_HEIGHT = 2.7, PLAYER_HEIGHT = 1.8;
    /** How far from the middle of a rift a player who comes back out of a pocket is put down (it must be outside the tear, or the tear would take them in again). */
    public static final double EXIT_RING = 2.2;

    /** Is a player standing at (px, py, pz) in the tear of the rift whose block is (bx, by, bz)? The same box the block entity always used (the block, 0.45 wider each side, 1.7 taller), against the player's box. */
    public static boolean inTear(int bx, int by, int bz, double px, double py, double pz) {
        return Math.abs(px - (bx + 0.5)) <= TEAR_HALF && Math.abs(pz - (bz + 0.5)) <= TEAR_HALF && py + PLAYER_HEIGHT > by && py < by + TEAR_HEIGHT;
    }

    /** What a shortcut whose destination may not be built yet does: go through (READY), keep waiting (WAIT), or settle for a nearer floor that is built (SETTLE). */
    public enum Forming { READY, WAIT, SETTLE }

    public static Forming forming(boolean targetReady, int waitedTicks, int nearerReady) {
        if (targetReady) return Forming.READY;
        if (waitedTicks >= FORM_PATIENCE_TICKS && nearerReady >= 0) return Forming.SETTLE;
        return Forming.WAIT;
    }

    /** The farthest chain index in (from, upTo] for which {@code exists} holds, or -1 when no floor between the rift's floor and its target is built. */
    public static int settleTarget(int from, int upTo, IntPredicate exists) {
        for (int c = upTo; c > from; c--) if (exists.test(c)) return c;
        return -1;
    }

    /** Can a shortcut opened on chain index {@code from} ever lead anywhere? Yes when a floor beyond it exists, or when the dungeon may still grow. */
    public static boolean canLeadAnywhere(int from, int tail, boolean expansionOn) { return tail > from || expansionOn; }

    /** The lowest free pocket slot of {@code max}, or -1 when every one is taken. */
    public static int firstFreeSlot(java.util.Collection<Integer> used, int max) {
        for (int i = 0; i < max; i++) if (!used.contains(i)) return i;
        return -1;
    }

    /** Why a roll for a new rift near a player did not even start (OK = it may). Order is the order the checks were always made in. */
    public enum RollGate { OK, OFF, SPECTATOR, AIRBORNE, IN_POCKET, NO_SECTOR, NOT_A_FLOOR, NOT_BUILT, SAFE_ROOM, CROWDED }

    public static RollGate rollGate(boolean enabled, boolean spectator, boolean onGround, boolean inPocket, boolean hasSector, boolean isFloor, boolean built, boolean safeRoom, int near, int maxNear) {
        if (!enabled) return RollGate.OFF;
        if (spectator) return RollGate.SPECTATOR;
        if (!onGround) return RollGate.AIRBORNE;
        if (inPocket) return RollGate.IN_POCKET;
        if (!hasSector) return RollGate.NO_SECTOR;
        if (!isFloor) return RollGate.NOT_A_FLOOR;
        if (!built) return RollGate.NOT_BUILT;
        if (safeRoom) return RollGate.SAFE_ROOM;
        if (near >= maxNear) return RollGate.CROWDED;
        return RollGate.OK;
    }

    /**
     * The places a player is put down when they come back out of a pocket through the rift they went in by: eight points on a ring of {@code radius} blocks round the middle of that rift, the
     * one nearest to where they stood (so they come out on the side they came from) first. Putting them back exactly where they stood put them INSIDE the tear, which took them in again.
     */
    public static double[][] exitRing(int riftX, int riftZ, double px, double pz, double radius, float yawDegrees) {
        double cx = riftX + 0.5, cz = riftZ + 0.5, dx = px - cx, dz = pz - cz;
        double want;
        if (Math.hypot(dx, dz) < 1e-3) {
            double a = Math.toRadians(yawDegrees);
            want = Math.atan2(-Math.cos(a), Math.sin(a));                 // behind the way they faced
        } else {
            want = Math.atan2(dz, dx);
        }
        double[][] out = new double[8][];
        int[] order = {0, 1, -1, 2, -2, 3, -3, 4};
        for (int i = 0; i < 8; i++) {
            double a = want + order[i] * Math.PI / 4.0;
            out[i] = new double[]{cx + Math.cos(a) * radius, cz + Math.sin(a) * radius};
        }
        return out;
    }

    /** Which plate (0..n-1) a right click on block (x, y, z) means: the button on the plate, or the coloured block under it; -1 when it is neither. Plates are given as {x, y, z} of their button. */
    public static int plateHit(int[][] buttons, int x, int y, int z) {
        for (int i = 0; i < buttons.length; i++) if (buttons[i][0] == x && buttons[i][2] == z && (buttons[i][1] == y || buttons[i][1] - 1 == y)) return i;
        return -1;
    }

    /** Does an empty pocket collapse now? Never in its first 90 seconds (the party may still be coming), and 20 seconds after it was cleared. */
    public static boolean collapseEmpty(boolean empty, boolean cleared, long sinceCreated, long sinceCleared) {
        return empty && (sinceCreated > 20L * 90L || (cleared && sinceCleared > 20L * 20L));
    }

    /** The eight-point compass name of a direction, 0 = south, clockwise through west (Minecraft's own axes: +z is south, +x is east). */
    public static String compass(double dx, double dz) {
        String[] names = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        int i = (int) Math.round(((Math.toDegrees(Math.atan2(-dx, dz)) % 360 + 360) % 360) / 45.0) % 8;
        return names[i];
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
