package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteEvolution;
import com.newuniverse.nusmp.skilltree.unique.MasteryLevels;

import java.util.List;
import java.util.function.Predicate;

/**
 * Why (3.26.0, the Evolution skills): the rules of "what counts, how far, and when does it open the Ultimate" are small and must be right for every player of a public server (a counter that overflows, goes
 * negative, counts a pet or a friend, or lets the Ultimate through half-ready is a bug that a whole community would find in a day), so they are written ONCE here, as pure functions with no Minecraft type, and
 * unit tested without a game. The game-facing class {@link Evolution} only reads the facts off an event (is the victim hostile, what is its maximum health, what kind of damage was it) and asks these rules.
 * The rules: a counter is a whole number from 0 to the requirement's count (never above it, never below zero, overflow safe); damage is counted in whole points with the fraction carried over, so a hundred
 * hits of 0.4 are forty points and not zero; only HOSTILE creatures count for kills and damage dealt (killing a cow never evolves a skill); an elite is a boss tag or at least 100 maximum health; a near-lethal
 * blow is one that leaves the player at 2 health or less, alive, and took at least a heart; the skill has EVOLVED when the counter has reached the count; and the Ultimate Evolution art is open only when the skill
 * has evolved AND is mastered (or when the whole feature is switched off in the server settings, which restores the old behaviour of a plain finisher).
 */
public final class EvolutionRules {
    private EvolutionRules() {}

    /** The counters of the catalogue ({@code catalog.EVOLUTION}): what a route's {@code evolution.requirement} may be. A test compares this list with the catalogue. */
    public static final List<String> REQUIREMENTS = List.of("kills", "elite_kills", "boss_kills", "damage_dealt", "damage_taken", "lethal_survived", "arts_cast", "ally_buffs");
    /** A counter never goes above this, whatever a route asks (the catalogue's {@code EVOLUTION_MAX_COUNT}). */
    public static final int MAX_COUNT = RouteEvolution.MAX_COUNT;
    /** A creature with at least this much maximum health is an elite. */
    public static final double ELITE_HEALTH = 100.0;
    /** A blow counts as "survived a lethal blow" when the player is left with at most this much health. */
    public static final float LETHAL_HEALTH = 2.0f;
    /** ... and the blow took at least this much (a heart), so a sliver of damage at one health point is not a lethal blow. */
    public static final float LETHAL_MIN_DAMAGE = 2.0f;
    /** At most one near-lethal blow counts every 5 seconds. */
    public static final int LETHAL_REST_TICKS = 100;
    /** The key of the skill's own data tag where the server mirrors the player's progress so the client can show it (the skill instance is already synced to the owner). */
    public static final String TAG_PROGRESS = "nusmp_evo", TAG_COUNT = "nusmp_evo_n";
    /** ... and which art (index among the skill's arts, -1 none) is the Ultimate, because the client only holds a node-less card of the line and cannot tell. */
    public static final String TAG_ULTIMATE = "nusmp_evo_i";
    /** The progress mirror is sent at most this often (ticks) while it changes: a counter that moves every hit must not become a packet every hit. */
    public static final int MIRROR_TICKS = 40;

    // ---------------------------------------------------------------- the counter
    /** The counter bounded to 0 up to the requirement's count (and never above {@link #MAX_COUNT}). */
    public static int clamp(long value, int count) {
        long cap = Math.max(0, Math.min(count, MAX_COUNT));
        return (int) Math.max(0L, Math.min(value, cap));
    }

    /** The counter after adding {@code delta} (a negative delta is ignored: progress never goes down), bounded by {@link #clamp}. Overflow safe. */
    public static int add(int current, long delta, int count) {
        long base = Math.max(0, current);
        long d = Math.max(0L, delta);
        return clamp(base + Math.min(d, (long) MAX_COUNT + 1), count);
    }

    /** True when the counter has reached the requirement's count (a count below 1 is never reached: a broken route cannot evolve for free). */
    public static boolean evolved(int progress, int count) { return count >= 1 && progress >= count; }

    /** How many whole points came out of {@code carry + amount}, and what is left over (damage is counted in whole points with the fraction carried). */
    public record Whole(int whole, double rest) {}

    /** Adds {@code amount} (ignored when negative, NaN or infinite) to the carried fraction and splits it into whole points and the new fraction. */
    public static Whole takeWhole(double carry, double amount) {
        double a = Double.isFinite(amount) && amount > 0 ? amount : 0.0;
        double c = Double.isFinite(carry) && carry > 0 ? carry : 0.0;
        double total = c + a;
        double whole = Math.floor(total);
        if (whole > MAX_COUNT) return new Whole(MAX_COUNT, 0.0);
        return new Whole((int) whole, total - whole);
    }

    // ---------------------------------------------------------------- what counts
    /** An elite: a creature with a boss tag, or at least {@link #ELITE_HEALTH} maximum health. */
    public static boolean isElite(boolean bossTag, double maxHealth) { return bossTag || maxHealth >= ELITE_HEALTH; }

    /** A foe that counts for kills and damage dealt: a hostile creature or an elite, never a pet, a villager, an animal or a player. */
    public static boolean countsAsFoe(boolean hostile, boolean elite) { return hostile || elite; }

    /** Does a kill (already known to be of a foe, never a player, a pet or an ally) count for this requirement: {@code kills} for any foe, {@code elite_kills} for an elite, {@code boss_kills} for a boss. */
    public static boolean killCounts(String requirement, boolean countsAsFoe, boolean elite, boolean boss) {
        if (requirement == null) return false;
        return switch (requirement) {
            case "kills" -> countsAsFoe;
            case "elite_kills" -> elite;
            case "boss_kills" -> boss;
            default -> false;
        };
    }

    /** Does damage of this kind count for a {@code damage_taken} requirement of kind {@code want} ({@code any} or empty: every kind). {@code isKind} answers for one catalogue word. */
    public static boolean damageKindMatches(String want, Predicate<String> isKind) {
        if (want == null || want.isEmpty() || want.equals("any")) return true;
        return isKind != null && isKind.test(want);
    }

    /** A near-lethal blow survived: the player is alive, was left with at most {@link #LETHAL_HEALTH} and the blow took at least {@link #LETHAL_MIN_DAMAGE}. The 5 second rest is the caller's. */
    public static boolean lethalSurvived(float healthAfter, float damage, boolean alive) {
        return alive && damage >= LETHAL_MIN_DAMAGE && healthAfter <= LETHAL_HEALTH;
    }

    // ---------------------------------------------------------------- the Ultimate gate
    /** Why a cast of the Ultimate Evolution art is refused, or {@code OPEN}. */
    public enum Gate { OPEN, NOT_EVOLVED, NOT_MASTERED }

    /**
     * The Ultimate Evolution art is open when it is not an Ultimate at all, when the feature is off, when the line has no evolution block, or when the skill has evolved AND is mastered
     * ({@link MasteryLevels#mastered}); otherwise the reason it is locked ({@code NOT_EVOLVED} first: it is the thing the player has to go and do).
     */
    public static Gate gate(boolean featureOn, boolean ultimate, boolean hasEvolution, int progress, int count, double masteryFraction) {
        if (!ultimate || !featureOn || !hasEvolution) return Gate.OPEN;
        if (!evolved(progress, count)) return Gate.NOT_EVOLVED;
        return MasteryLevels.mastered(masteryFraction) ? Gate.OPEN : Gate.NOT_MASTERED;
    }

    // ---------------------------------------------------------------- the card
    /** What the skill card shows beside the Ultimate art: still counting, evolved but not yet mastered, or ready. */
    public enum Card { PROGRESS, NEEDS_MASTERY, READY }

    public static Card card(int progress, int count, boolean mastered) {
        if (!evolved(progress, count)) return Card.PROGRESS;
        return mastered ? Card.READY : Card.NEEDS_MASTERY;
    }

    /** {@code "12 / 50"}: the progress as the card writes it (the progress is shown bounded by the count). */
    public static String progressText(int progress, int count) { return Math.max(0, Math.min(progress, Math.max(0, count))) + " / " + Math.max(0, count); }

    /** Should the mirror of the progress be sent now: it differs from what was sent, and the last send is old enough (or the skill just evolved, which is always sent at once). */
    public static boolean shouldMirror(int mirrored, int actual, int count, long lastMirrorTick, long now) {
        if (mirrored == actual) return false;
        if (evolved(actual, count) && !evolved(mirrored, count)) return true;
        return now - lastMirrorTick >= MIRROR_TICKS || now < lastMirrorTick;
    }
}
