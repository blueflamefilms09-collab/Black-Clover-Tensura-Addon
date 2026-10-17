package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: the Shadow Core is a limited tool (three extractions, then it is spent), and a quest reward may recharge a held one by a charge instead of handing out a
 * second core. The few numbers around that (never negative, never above the maximum, spent means gone) are what a duplicated-item bug would hide in, so they
 * are written once, here, and pinned by a test instead of being inline arithmetic in the item class. Pure.
 */
public final class CoreCharges {
    private CoreCharges() {}

    /** The charges of a new core. */
    public static final int START = 3;

    /** The most a core can hold. */
    public static final int MAX = 3;

    /** A stored value brought into the valid range 0..{@link #MAX}. */
    public static int clamp(int charges) { return Math.max(0, Math.min(MAX, charges)); }

    /** One extraction spent. */
    public static int spend(int charges) { return Math.max(0, clamp(charges) - 1); }

    /** Is the core used up? */
    public static boolean spent(int charges) { return clamp(charges) <= 0; }

    /** Can a quest reward put a charge into this core? */
    public static boolean canRecharge(int charges) { return clamp(charges) < MAX; }

    /** One charge more, up to the maximum. */
    public static int recharge(int charges) { return Math.min(MAX, clamp(charges) + 1); }
}
