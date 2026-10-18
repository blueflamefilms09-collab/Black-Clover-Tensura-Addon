package com.newuniverse.nusmp.elite.races.rule;

/**
 * Why: an evolution rule must be testable without a Minecraft server, so a rule never reads a player. It reads this small view of one, and the real implementation
 * ({@code runtime.PlayerRaceCtx}) is built from a ServerPlayer while the tests use a fake. Every method answers a safe default (0, false) for what it does not know.
 */
public interface RaceCtx {
    /** The player's maximum EP (Tensura's existence points). */
    double ep();

    /** Kills of a category: {@code any}, {@code hostile}, {@code boss} or {@code player}. */
    int kills(String key);

    /** Mastery of a skill as a fraction 0..1 (0 when the player does not have it). */
    double mastery(String skillId);

    /** How many of an item the player carries. */
    int itemCount(String itemId);

    /** Whether the player has unlocked a record (read through {@code EliteHooks.recordUnlocked}; false while the records system is absent). */
    boolean record(String recordId);

    /** Takes {@code count} of an item from the player; true when all of it was taken. The default takes nothing. */
    default boolean consume(String itemId, int count) { return false; }

    /** The server's multiplier on every EP gate (config {@code epGateScale}). */
    default double epScale() { return 1.0; }
}
