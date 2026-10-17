package com.newuniverse.nusmp.avatar.yandere;

/**
 * Why: the Yandere rules that are plain arithmetic (how much hit the Avatar takes for its creator, how much health a frenzy cast burns, how long
 * a person may stay near the creator before the Avatar acts) are kept pure so the "it can never kill itself" and "it always warns first" promises
 * are unit-tested instead of trusted.
 */
public final class YandereMath {
    private YandereMath() {}

    /** Ticks of warning before the Avatar body-blocks someone who came close to its creator. */
    public static final int WARN_TICKS = 20;
    /** Ticks someone may stay close (blocked, but unharmed) before the Avatar attacks even if they did nothing hostile. */
    public static final int STAY_TICKS = 60;

    public enum Phase { NONE, WARN, BLOCK, ATTACK }

    /**
     * What the Avatar does about an intruder who has been within the jealous radius for {@code ticks}.
     * First a warning (no contact), then a body-block; only a hostile act (the intruder targets or hit the creator) or staying three seconds
     * leads to an attack, and never earlier than after the warning.
     */
    public static Phase phase(int ticks, boolean hostileActing) {
        if (ticks <= 0) return Phase.NONE;
        if (ticks < WARN_TICKS) return Phase.WARN;
        if (hostileActing || ticks >= STAY_TICKS) return Phase.ATTACK;
        return Phase.BLOCK;
    }

    /** How much of an incoming hit the Avatar takes on itself: {@code share} of it, but it always keeps 1 health, so intercepting never kills it. */
    public static float interceptedPart(float amount, double share, float avatarHealth) {
        if (amount <= 0 || share <= 0) return 0;
        float want = (float) (amount * Math.min(1.0, share));
        return Math.max(0f, Math.min(want, avatarHealth - 1f));
    }

    /** Health a frenzy cast burns: the configured burn, but never more than leaves the Avatar at 1 health. */
    public static float burn(float health, float burn) {
        return Math.max(0f, Math.min(burn, health - 1f));
    }

    /**
     * The brief's frenzy cost is the Avatar's own MAXIMUM health ("drains the Avatar's own Max Health to machine-gun skills"): each frenzy cast takes {@code perCast}
     * off the maximum, as a temporary modifier that the end of the frenzy removes. Returns how much more to take: the configured amount, but never so much that the
     * effective maximum falls below 1 (so the Avatar can never kill itself with its own spells). {@code maxWithoutDrain} is the maximum health without the drain,
     * {@code alreadyDrained} what the modifier holds now.
     */
    public static double drainStep(double maxWithoutDrain, double alreadyDrained, double perCast) {
        if (perCast <= 0.0) return 0.0;
        return Math.max(0.0, Math.min(perCast, maxWithoutDrain - Math.max(0.0, alreadyDrained) - 1.0));
    }

    /** Raw damage allowed on a player when lethal Yandere hits are off: never more than leaves them at 1 health (counting absorption). */
    public static float nonLethal(float amount, float playerHealth, float absorption) {
        return Math.max(0f, Math.min(amount, playerHealth + absorption - 1f));
    }
}
