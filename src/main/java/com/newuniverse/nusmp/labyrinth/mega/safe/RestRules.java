package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;

/**
 * Why: the brief says players "cannot regenerate easily; they must secure and defend Safe Zones" and may take D&amp;D short and long rests only
 * by physically using the marked seats, hearths and beds of a Safe Zone. Every number and every refusal of that rule lives here, in one pure
 * class (no Minecraft types, so JUnit pins the balance without a server): how long each rest takes, what it gives, which anchor starts which
 * rest, why a rest is refused before it starts (not in a Safe Zone, a hostile within sixteen blocks, no short rests left, the long-rest
 * cool-down) and what breaks one in progress (damage, walking away, a monster arriving, leaving the zone). The event glue
 * ({@link RestEvents}) only gathers plain values from the world, asks this class for the verdict and applies it, so the balance can be changed
 * and tested in one place. The defaults are also the config defaults of {@link SafeConfig} (one source of truth), and the long rest's ambush
 * chance grows with the sector's threat so that sleeping deep in the dungeon is a bet, not a free heal.
 */
public final class RestRules {
    private RestRules() {}

    // ---------------------------------------------------------------- the numbers (also the SafeConfig defaults)
    public static final int DEFAULT_SHORT_SECONDS = 10;
    public static final int DEFAULT_LONG_SECONDS = 30;
    /** Short rests available between two long rests (D&amp;D: a party gets a few breathers per day). */
    public static final int DEFAULT_SHORT_USES = 3;
    /** Share of maximum health and of maximum mana a short rest gives back. */
    public static final double DEFAULT_SHORT_HEALTH = 0.4;
    public static final double DEFAULT_SHORT_MANA = 0.4;
    /** Game ticks that must pass between two long rests (5 minutes). */
    public static final int DEFAULT_LONG_COOLDOWN_TICKS = 6000;
    /** A hostile mob this close forbids a rest and breaks one in progress. */
    public static final double DEFAULT_HOSTILE_RADIUS = 16.0;
    /** Ambush chance of a long rest per level of sector threat, and the ceiling of that chance. */
    public static final double DEFAULT_AMBUSH_PER_THREAT = 0.05;
    public static final double DEFAULT_AMBUSH_CAP = 0.5;

    /** How far (blocks) the resting player may stray from where they sat down before the rest is lost. */
    public static final double MOVE_LIMIT = 1.5;
    /** How far (blocks) the player may be from the seat, hearth or bed they click to start a rest (the reach is 4.5; resting asks for 3.5). */
    public static final double START_REACH = 3.5;
    /** The channel is checked this often (ticks): twice a second is plenty for a ten-second wait and costs nothing. */
    public static final int CHECK_INTERVAL = 10;
    /** A long rest that rolled an ambush has it arrive between this share and the next of the way through the rest. */
    public static final double AMBUSH_EARLIEST = 0.25, AMBUSH_LATEST = 0.75;

    // ---------------------------------------------------------------- which anchor starts which rest
    /** The rest a marker starts: seats and hearths give a short rest, beds a long one; every other kind starts nothing (null). */
    public static RestKind kindOf(MarkerKind kind) {
        if (kind == null) return null;
        return switch (kind) {
            case SEAT, HEARTH -> RestKind.SHORT;
            case BED -> RestKind.LONG;
            default -> null;
        };
    }

    /** Is this marker kind one a player can start a rest on? */
    public static boolean isAnchor(MarkerKind kind) { return kindOf(kind) != null; }

    // ---------------------------------------------------------------- starting a rest
    /** Why a rest cannot start; {@link #NONE} means it can. The order is the order the checks run in (the first failure wins). */
    public enum Refusal { NONE, DISABLED, NOT_SURVIVAL, BUSY, NOT_SAFE_ZONE, TOO_FAR, HOSTILE_NEAR, NO_SHORT_RESTS, LONG_COOLDOWN }

    /**
     * Everything the start check needs, as plain values. {@code distanceToAnchor} is player-to-block in blocks; {@code nearestHostile} is the
     * distance of the closest hostile mob ({@link Double#POSITIVE_INFINITY} when none); {@code lastLongRest} is the game tick of the last
     * long rest or a negative number for "never".
     */
    public record Start(boolean enabled, boolean survival, boolean resting, boolean inSafeZone, double distanceToAnchor, double nearestHostile,
                        int shortRestsLeft, long now, long lastLongRest) {}

    /** Can a rest of this kind start now? {@link Refusal#NONE} when yes, else the first reason it cannot. */
    public static Refusal checkStart(RestKind kind, Start s, double hostileRadius, int longCooldownTicks) {
        if (!s.enabled()) return Refusal.DISABLED;
        if (!s.survival()) return Refusal.NOT_SURVIVAL;
        if (s.resting()) return Refusal.BUSY;
        if (!s.inSafeZone()) return Refusal.NOT_SAFE_ZONE;
        if (s.distanceToAnchor() > START_REACH) return Refusal.TOO_FAR;
        if (s.nearestHostile() <= hostileRadius) return Refusal.HOSTILE_NEAR;
        if (kind == RestKind.SHORT && s.shortRestsLeft() <= 0) return Refusal.NO_SHORT_RESTS;
        if (kind == RestKind.LONG && longCooldownRemaining(s.now(), s.lastLongRest(), longCooldownTicks) > 0) return Refusal.LONG_COOLDOWN;
        return Refusal.NONE;
    }

    /** Ticks until the next long rest is allowed (0 = now). A negative {@code lastLongRest} means "never rested", so no wait. */
    public static long longCooldownRemaining(long now, long lastLongRest, int cooldownTicks) {
        if (lastLongRest < 0 || cooldownTicks <= 0) return 0L;
        return Math.max(0L, lastLongRest + cooldownTicks - now);
    }

    // ---------------------------------------------------------------- a rest in progress
    /** What broke a rest in progress; {@link #NONE} means it carries on. The order is the order the checks run in. */
    public enum Interrupt { NONE, STATE, DAMAGED, MOVED, LEFT_ZONE, HOSTILE }

    /**
     * One look at a resting player: {@code valid} is false when the player died, left the dimension or changed game mode; {@code moved} is the
     * distance from where they sat down; {@code damaged} is true when they took damage since the rest began.
     */
    public record Sample(boolean valid, boolean inSafeZone, double moved, boolean damaged, double nearestHostile) {}

    public static Interrupt checkChannel(Sample s, double hostileRadius) {
        if (!s.valid()) return Interrupt.STATE;
        if (s.damaged()) return Interrupt.DAMAGED;
        if (s.moved() > MOVE_LIMIT) return Interrupt.MOVED;
        if (!s.inSafeZone()) return Interrupt.LEFT_ZONE;
        if (s.nearestHostile() <= hostileRadius) return Interrupt.HOSTILE;
        return Interrupt.NONE;
    }

    // ---------------------------------------------------------------- time and progress
    /** Length of a rest in ticks from the configured seconds (never below one second, so a bad config cannot make a rest instant). */
    public static int ticksFor(RestKind kind, int shortSeconds, int longSeconds) {
        int seconds = kind == RestKind.LONG ? longSeconds : shortSeconds;
        return Math.max(1, seconds) * 20;
    }

    /** Progress in whole percent (0 to 100) after {@code elapsed} of {@code total} ticks. */
    public static int progressPercent(long elapsed, int total) {
        if (total <= 0) return 100;
        long p = elapsed * 100L / total;
        return (int) Math.max(0L, Math.min(100L, p));
    }

    public static boolean finished(long elapsed, int total) { return elapsed >= total; }

    // ---------------------------------------------------------------- what a rest gives
    /** Share of maximum health a rest restores: everything for a long rest, the configured share for a short one (clamped to 0..1). */
    public static double healthShare(RestKind kind, double shortShare) { return kind == RestKind.LONG ? 1.0 : clamp01(shortShare); }

    /** Share of maximum mana (and aura) a rest restores, same shape as {@link #healthShare}. */
    public static double manaShare(RestKind kind, double shortShare) { return kind == RestKind.LONG ? 1.0 : clamp01(shortShare); }

    /** Health to give back now: the share of the maximum, never more than the missing health, never negative. */
    public static float healAmount(float current, float max, double share) {
        float missing = Math.max(0f, max - current);
        return Math.min(missing, (float) Math.max(0.0, max * share));
    }

    /** Short rests left given the configured number of uses and how many were spent since the last long rest. */
    public static int shortRestsLeft(int uses, int used) { return Math.max(0, uses - Math.max(0, used)); }

    // ---------------------------------------------------------------- the ambush
    /** Chance (0..cap) that a long rest in a sector of this threat brings an ambush: {@code perThreat} per level of threat, capped. */
    public static double ambushChance(int threat, double perThreat, double cap) {
        double c = Math.max(0.0, perThreat) * Math.max(0, threat);
        return Math.max(0.0, Math.min(Math.max(0.0, cap), c));
    }

    /** The roll: {@code roll} is uniform in [0,1). */
    public static boolean rollAmbush(double chance, double roll) { return roll < chance; }

    /** Ticks after the start at which the ambush arrives: somewhere between a quarter and three quarters of the rest ({@code roll} uniform in [0,1)). */
    public static long ambushDelay(int totalTicks, double roll) {
        double r = Math.max(0.0, Math.min(0.999999, roll));
        return Math.round(totalTicks * (AMBUSH_EARLIEST + (AMBUSH_LATEST - AMBUSH_EARLIEST) * r));
    }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }
}
