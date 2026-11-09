package com.newuniverse.nusmp.heroweapon.rules;

/**
 * Why: the pure rule of giving up your own hero weapon (3.72.0, {@code /multiverse hero abandon}). Giving it up deletes its level, forms and mastery for good, so it takes two steps: the
 * command asks, and the same player confirms within {@link #CONFIRM_TICKS} (30 seconds). A confirmation with no question, a late one, or one from the future (a clock that went back) is refused.
 */
public final class HeroAbandonRules {
    private HeroAbandonRules() {}

    /** Ticks a question stays open. */
    public static final int CONFIRM_TICKS = 600;

    /** True when the player asked at {@code askedAt} (game time, null = never) and {@code now} is still inside the window. */
    public static boolean confirmed(Long askedAt, long now) {
        return askedAt != null && now >= askedAt && now - askedAt <= CONFIRM_TICKS;
    }
}
