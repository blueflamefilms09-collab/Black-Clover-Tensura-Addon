package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: "the air of the Holy floors is dense with Light Rukh; magic that matches the flow is massively multiplied; opposing magic is violently
 * rejected" (the owner's brief) is a set of small decisions, and each one is a number that will be argued about in play: which place has which
 * flow, what a match is worth, what a rejection costs, how often it may cost, whether it can kill. They live here, in one pure class with no Minecraft
 * import, so that every combination is pinned by JUnit and the game-side glue ({@code FlowEvents}, {@code Rejection}) only gathers facts and applies
 * the answer. The flow of a place comes from its road: the Holy road is LIGHT (config {@code holyFlow}), the Abyssal road is NEUTRAL by default
 * (the brief defines only the Holy side; {@code abyssalFlow = "dark"} mirrors the rule), Floor 0 and everywhere else have none. A cast that matches
 * the flow is multiplied (a match is worth {@code holyMultiplier}, 2.5 by default, kept inside 1..10); a NEUTRAL cast or a place with no flow
 * changes nothing; a cast of the opposing alignment is REJECTED. The rejection takes a share of the caster's maximum health (8 percent, never less
 * than 2 HP), at most once per cooldown, and by default never kills: "violently rejects" is a price, not an execution, so the drain stops at 1 HP
 * unless the owner turns {@code nonLethal} off. All of it clamps its inputs and never throws, because it runs inside damage events.
 */
public final class FlowRules {
    private FlowRules() {}

    /** The defaults the config copies (one place, so the tests and the config cannot drift apart). */
    public static final double DEFAULT_MULTIPLIER = 2.5, MIN_MULTIPLIER = 1.0, MAX_MULTIPLIER = 10.0;
    public static final double DEFAULT_REJECT_PERCENT = 8.0, DEFAULT_REJECT_MIN_HEALTH = 2.0;
    public static final int DEFAULT_REJECT_COOLDOWN_TICKS = 20, DEFAULT_CAST_WINDOW_TICKS = 30;
    /** "Never rejected yet": far enough below any game time that a subtraction cannot overflow. */
    public static final long NEVER = Long.MIN_VALUE / 4;

    /** What a cast does in a place: nothing, a boost (it matches the flow), or a rejection (it opposes the flow). */
    public enum Outcome { NONE, BOOST, REJECT }

    /** Why: the verdict on one hit in one piece: what happened, the damage to use, and whether the hit is cancelled outright. */
    public record Decision(Outcome outcome, float amount, boolean cancel) {}

    // ---------------------------------------------------------------- the flow of a place
    /** Is this text a flow the config may hold ({@code light}, {@code dark}, {@code none} or {@code neutral})? */
    public static boolean validFlow(String s) { return Alignment.fromId(s) != null; }

    /** The flow the text names, or {@code fallback} for null and anything else (a bad config value reads as its default). */
    public static Alignment parseFlow(String s, Alignment fallback) {
        Alignment a = Alignment.fromId(s);
        return a == null ? fallback : a;
    }

    /** The flow of a road: the Holy road's, the Abyssal road's, or NEUTRAL for Floor 0 and everywhere outside the dungeon (a null polarity). */
    public static Alignment flowOf(Polarity road, Alignment holyFlow, Alignment abyssalFlow) {
        if (road == Polarity.HOLY) return holyFlow == null ? Alignment.NEUTRAL : holyFlow;
        if (road == Polarity.ABYSSAL) return abyssalFlow == null ? Alignment.NEUTRAL : abyssalFlow;
        return Alignment.NEUTRAL;
    }

    // ---------------------------------------------------------------- a cast against a flow
    /** BOOST when a LIGHT or DARK cast meets the same flow, REJECT when it meets the opposite one, NONE when either side is NEUTRAL (or null). */
    public static Outcome outcome(Alignment flow, Alignment cast) {
        if (flow == null || cast == null || flow == Alignment.NEUTRAL || cast == Alignment.NEUTRAL) return Outcome.NONE;
        return flow == cast ? Outcome.BOOST : Outcome.REJECT;
    }

    /** A configured multiplier kept inside 1..10 (a NaN reads as the default): a boost can never be a penalty and never an absurdity. */
    public static double clampMultiplier(double m) {
        if (Double.isNaN(m)) return DEFAULT_MULTIPLIER;
        return Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, m));
    }

    /** What a cast's damage is multiplied by: the configured match value for a BOOST, exactly 1 for everything else (a rejection is a penalty, not a divisor). */
    public static double multiplier(Alignment flow, Alignment cast, double matchMultiplier) {
        return outcome(flow, cast) == Outcome.BOOST ? clampMultiplier(matchMultiplier) : 1.0;
    }

    /**
     * The verdict on one hit of a player whose last cast (within the cast window, or null for none) had the alignment {@code recentCast}, in a place
     * with the flow {@code flow}. A match multiplies the damage; a rejection leaves it alone, or cancels the hit when {@code cancelRejected}; anything
     * else is untouched. A NaN amount is returned as it came.
     */
    public static Decision decide(Alignment flow, Alignment recentCast, float amount, double matchMultiplier, boolean cancelRejected) {
        if (Float.isNaN(amount)) return new Decision(Outcome.NONE, amount, false);
        Outcome o = outcome(flow, recentCast);
        if (o == Outcome.BOOST) return new Decision(o, (float) (amount * multiplier(flow, recentCast, matchMultiplier)), false);
        if (o == Outcome.REJECT && cancelRejected) return new Decision(o, 0f, true);
        return new Decision(o, amount, false);
    }

    // ---------------------------------------------------------------- keeping a boosted hit inside the mod's own limits
    /**
     * The damage of a boosted hit on a boss that has a damage-per-second budget. The boss already drew the ORIGINAL hit from its budget (its
     * {@code hurt} runs before the damage event), so only the extra the boost adds may be drawn now: {@code allow} maps the extra asked for to the extra
     * the budget still has (a budget that is empty grants 0, so the hit stays what it was). The boost can therefore never push a boss past its cap.
     */
    public static float chargeExtra(float original, float boosted, java.util.function.DoubleUnaryOperator allow) {
        if (Float.isNaN(original) || Float.isNaN(boosted) || !(boosted > original) || allow == null) return boosted;
        double extra = boosted - original;
        double granted = Math.max(0.0, Math.min(extra, allow.applyAsDouble(extra)));
        return (float) (original + granted);
    }

    /**
     * A boosted hit kept under a ceiling (the share of a player's health that one hit may take, for a hit on a player): never below the original hit
     * (the boost is limited, not the damage that was already there), never above the boost.
     */
    public static float capBoost(float original, float boosted, float ceiling) {
        if (Float.isNaN(original) || Float.isNaN(boosted) || Float.isNaN(ceiling) || !(boosted > original)) return boosted;
        return Math.max(original, Math.min(boosted, ceiling));
    }

    // ---------------------------------------------------------------- the rejection
    /**
     * The health one rejected cast takes: {@code percent} of the maximum health, never less than {@code minDrain}; and, when {@code nonLethal},
     * never more than leaves the caster 1 HP (a caster at 1 HP or less loses nothing). A caster with no maximum health, or an unreadable number, loses nothing.
     */
    public static double rejectionDrain(double maxHealth, double health, double percent, double minDrain, boolean nonLethal) {
        if (Double.isNaN(maxHealth) || Double.isNaN(health) || maxHealth <= 0.0) return 0.0;
        double pct = Double.isNaN(percent) ? 0.0 : Math.max(0.0, percent);
        double floor = Double.isNaN(minDrain) ? 0.0 : Math.max(0.0, minDrain);
        double drain = Math.max(floor, maxHealth * pct / 100.0);
        if (nonLethal) drain = Math.min(drain, Math.max(0.0, health - 1.0));
        return drain;
    }

    /**
     * Has a cooldown run out? True when {@code gapTicks} ticks or more passed since {@code lastTick} ({@link #NEVER} for "not yet"), when the gap is
     * zero or negative, or when the clock moved backwards (a stale stamp must never lock a player out). Used for the rejection's cooldown and for
     * the pauses between two on-screen effects.
     */
    public static boolean due(long now, long lastTick, int gapTicks) {
        if (gapTicks <= 0) return true;
        long age = now - lastTick;
        return age < 0 || age >= gapTicks;
    }
}
