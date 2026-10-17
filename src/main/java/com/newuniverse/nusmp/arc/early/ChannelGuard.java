package com.newuniverse.nusmp.arc.early;

/**
 * Why: the ultimates of the early bosses are rituals with a long wind-up, and the lesson they teach the party is the same for four of the five: a big cast
 * can be broken by a burst of damage while it is being wound up. The framework's per-second damage budget makes "kill it before it lands" impossible, so
 * the break is a share of the boss's health dealt during the wind-up (counted before the budget, which is what the party tried to deal). This pure class
 * is that counter: armed when the wind-up of a guarded move begins, fed every blow, and it says exactly once that the threshold was crossed. A boss class
 * decides what a broken cast means (the cast is dropped, the boss is left open).
 */
public final class ChannelGuard {
    /** The default share of maximum health that breaks a cast. */
    public static final double DEFAULT_SHARE = 0.12;

    private boolean armed, broken;
    private double threshold, dealt;

    /** Starts watching a new cast: the damage so far is forgotten. */
    public void arm(double threshold) {
        this.threshold = Math.max(1, threshold);
        this.dealt = 0;
        this.armed = true;
        this.broken = false;
    }

    /** Stops watching (the cast ended or was dropped). */
    public void disarm() {
        armed = false;
        broken = false;
        dealt = 0;
    }

    public boolean armed() { return armed; }

    /** True exactly once: the blow that carries the total over the threshold. Blows before arming, after the break, or of no size do nothing. */
    public boolean hit(double amount) {
        if (!armed || broken || amount <= 0) return false;
        dealt += amount;
        if (dealt < threshold) return false;
        broken = true;
        return true;
    }

    public boolean broken() { return broken; }

    /** How close the cast is to breaking, 0 to 1. */
    public double progress() { return armed ? Math.min(1, dealt / threshold) : 0; }

    /** The threshold for a boss of this maximum health: {@code share} of it, at least 1. */
    public static double threshold(double maxHealth, double share) { return Math.max(1, maxHealth * share); }
}
