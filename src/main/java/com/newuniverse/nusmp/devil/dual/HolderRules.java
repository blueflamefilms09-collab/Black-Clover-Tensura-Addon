package com.newuniverse.nusmp.devil.dual;

import java.util.UUID;

/**
 * Why: One of One. Each devil of the roster can have only one contract holder in the world; this is the pure rule (no Minecraft class, unit tested) that decides who may take a devil that already has one.
 * The server keeps one small {@link Slot} per devil in world saved data ({@code DevilHolders}); asking is a lookup, nothing is scanned.
 * <p>A devil that is not held is FREE. The holder asking again is SELF. Anybody else may take it only by one of the override conditions (all server decided, all switchable):
 * <ul>
 * <li>DEATH LAPSE: the holder died and has not let the devil in again since, for {@code graceTicks} (a bond that is not kept lapses);</li>
 * <li>ABSENT LAPSE: the holder has not been seen for {@code absentMs} (real time; an abandoned bond lapses);</li>
 * <li>USURPED: the holder lost a bound duel ({@code DevilOwnership}); the devil is then RESERVED for the victor for {@code reserveTicks}, and nobody else may take it meanwhile.</li>
 * </ul>
 * An operator can always release or hand over a devil with {@code /devil holder}.
 */
public final class HolderRules {
    private HolderRules() {}

    /** What the server remembers about one devil. {@code holder} is null while the devil is only reserved. {@code diedAt} and {@code seenMs} are 0 when unknown (the holder is alive and online). */
    public record Slot(UUID holder, long diedAt, long seenMs, UUID reservedFor, long reservedUntil) {}

    /** The settings the verdict reads. {@code graceTicks} 0 and {@code absentMs} 0 each turn that lapse off. */
    public record Params(boolean enabled, long graceTicks, long absentMs) {}

    public enum Verdict {
        FREE(true), SELF(true), LAPSED_DEATH(true), LAPSED_ABSENT(true), RESERVED_FOR_YOU(true), TAKEN(false), RESERVED_OTHER(false);

        public final boolean mayTake;

        Verdict(boolean mayTake) { this.mayTake = mayTake; }
    }

    /** Who may take this devil? {@code nowTicks} is game time, {@code nowMs} real time. */
    public static Verdict check(Slot s, UUID me, long nowTicks, long nowMs, Params p) {
        if (!p.enabled() || s == null || me == null) return Verdict.FREE;
        if (s.reservedFor() != null && nowTicks < s.reservedUntil()) return me.equals(s.reservedFor()) ? Verdict.RESERVED_FOR_YOU : Verdict.RESERVED_OTHER;
        if (s.holder() == null) return Verdict.FREE;
        if (s.holder().equals(me)) return Verdict.SELF;
        if (p.graceTicks() > 0 && s.diedAt() > 0 && nowTicks - s.diedAt() >= p.graceTicks()) return Verdict.LAPSED_DEATH;
        if (p.absentMs() > 0 && s.seenMs() > 0 && nowMs - s.seenMs() >= p.absentMs()) return Verdict.LAPSED_ABSENT;
        return Verdict.TAKEN;
    }

    /** The line the caller reads when the devil can not be taken. */
    public static String refusal(Verdict v) {
        return switch (v) {
            case TAKEN -> "This devil already has a contract holder. A devil keeps one: only a lapsed bond or a lost duel frees it.";
            case RESERVED_OTHER -> "This devil was won in a duel and is reserved for the victor for now.";
            default -> "";
        };
    }

    // ---------------------------------------------------------------- the bound duel
    /** A duel may start: both online, in one dimension, within {@code range}, the challenger is not the holder, no duel is open for this holder, and the table is not full. */
    public static boolean duelMayStart(boolean enabled, boolean sameLevel, double distance, double range, boolean sameMan, boolean holderBusy, int open, int cap) {
        return enabled && sameLevel && !sameMan && !holderBusy && open < cap && distance >= 0 && distance <= range;
    }

    /** Is the kill inside the duel: the victim is the duel's holder, the killer its challenger, and the window is still open. */
    public static boolean duelWon(UUID holder, UUID challenger, UUID victim, UUID killer, long until, long now) {
        return holder != null && challenger != null && holder.equals(victim) && challenger.equals(killer) && now <= until;
    }

    // ---------------------------------------------------------------- the settings, clamped
    public static long graceTicks(int hours) { return Math.max(0, Math.min(720, hours)) * 1000L; }

    public static long absentMillis(int days) { return Math.max(0, Math.min(365, days)) * 86_400_000L; }

    public static long reserveTicks(int hours) { return Math.max(1, Math.min(240, hours)) * 1000L; }

    public static long duelTicks(int seconds) { return Math.max(30, Math.min(1800, seconds)) * 20L; }
}
