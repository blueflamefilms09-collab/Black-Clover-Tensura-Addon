package com.newuniverse.nusmp.labyrinth.mega.lock;

import java.util.Collection;
import java.util.Set;

/**
 * Why: the owner's second request is "remove the random being locked in a room or add a way to get out". The dungeon seals players in on purpose (a trial chamber bars its
 * doorways until the wave is dead, 214 of the thousand mechanics bar the doorways or cage the party until a puzzle is solved, a boss mechanic can cage the arena), and
 * every one of those must have a guaranteed way out. This is the rule they all share, written once as a pure function of the facts of a lock: how long it has been closed,
 * how many living players are inside and for how long it has been empty, how many hostile creatures of the fight are alive and for how long none has been, whether a boss
 * is alive, and whether the players present can beat it at all. The answer is one of three: HOLD (stay closed), SHOW_RUNE (the escape rune must be visible) or RELEASE (open
 * now), with the reason, which is also what the players are told. The rules, in the order they are asked:
 * <ol>
 * <li>Nobody inside (all dead, gone, or logged out) for {@code absentSeconds}: RELEASE. A lock without a prisoner is a trap for the next party.</li>
 * <li>A boss arena stays closed while the boss lives and a player is inside, but the escape rune is visible from the first moment; the boss dead: RELEASE; the fight longer
 * than {@code bossMaxSeconds}: RELEASE.</li>
 * <li>Any other lock: longer than {@code maxSealSeconds}: RELEASE. One the players present cannot beat (no counter, or a plate puzzle for more players than are inside) is
 * released after {@code unsolvableSeconds}. One with no hostile alive for {@code rescueMinutes}: RELEASE (the automatic rescue).</li>
 * <li>The rune: visible at once after {@code idleRuneSeconds} when no hostile is alive (the room is idle) or the lock cannot be beaten, and after {@code runeAfterSeconds}
 * whatever is going on.</li>
 * </ol>
 * All of it is arithmetic on numbers, so {@code EscapeRulesTest} pins every branch, and the game classes ({@link LockBook}, the trial chamber) only gather the facts.
 */
public final class EscapeRules {
    private EscapeRules() {}

    /** What kind of lock it is: a trial chamber's barriers, a mechanic's bars or cage, or a boss arena's. */
    public enum Kind { TRIAL, SEAL, BOSS }

    /** What to do now. */
    public enum Action { HOLD, SHOW_RUNE, RELEASE }

    /** Why (shown to the players and written to the log). */
    public enum Why { NONE, ABSENT, BOSS_FIGHT, BOSS_DEAD, MAX_TIME, UNSOLVABLE, RESCUE, IDLE, TIMEOUT }

    /** The decision. */
    public record Decision(Action action, Why why) {
        public boolean release() { return action == Action.RELEASE; }

        public boolean rune() { return action != Action.HOLD; }
    }

    /**
     * The timings, in seconds (minutes for the rescue). {@link #clamped} keeps each in a sensible range so a bad config value can neither disable the guarantee nor open
     * a lock the moment it closes.
     */
    public record Config(int absentSeconds, int runeAfterSeconds, int idleRuneSeconds, int rescueMinutes, int maxSealSeconds, int bossMaxSeconds, int unsolvableSeconds) {
        public static final Config DEFAULT = new Config(10, 60, 15, 5, 600, 900, 30);

        public Config clamped() {
            return new Config(clamp(absentSeconds, 3, 600), clamp(runeAfterSeconds, 5, 3600), clamp(idleRuneSeconds, 1, 3600), clamp(rescueMinutes, 1, 120),
                    clamp(maxSealSeconds, 30, 7200), clamp(bossMaxSeconds, 60, 7200), clamp(unsolvableSeconds, 5, 3600));
        }
    }

    /**
     * The facts of one lock. {@code emptyTicks} counts how long no living player has been inside, {@code quietTicks} how long no hostile of the fight has been alive.
     */
    public record Facts(Kind kind, long sealedTicks, int playersInside, long emptyTicks, int hostiles, long quietTicks, boolean bossAlive, boolean solvable) {}

    public static Decision decide(Facts f, Config config) {
        Config c = config.clamped();
        long sealed = f.sealedTicks();
        if (f.playersInside() <= 0 && f.emptyTicks() >= c.absentSeconds() * 20L) return new Decision(Action.RELEASE, Why.ABSENT);
        if (f.kind() == Kind.BOSS) {
            if (!f.bossAlive()) return new Decision(Action.RELEASE, Why.BOSS_DEAD);
            if (sealed >= c.bossMaxSeconds() * 20L) return new Decision(Action.RELEASE, Why.MAX_TIME);
            return new Decision(Action.SHOW_RUNE, Why.BOSS_FIGHT);
        }
        if (sealed >= c.maxSealSeconds() * 20L) return new Decision(Action.RELEASE, Why.MAX_TIME);
        if (!f.solvable() && sealed >= c.unsolvableSeconds() * 20L) return new Decision(Action.RELEASE, Why.UNSOLVABLE);
        boolean idle = f.hostiles() <= 0;
        if (idle && f.quietTicks() >= c.rescueMinutes() * 60L * 20L) return new Decision(Action.RELEASE, Why.RESCUE);
        if (idle && sealed >= c.idleRuneSeconds() * 20L) return new Decision(Action.SHOW_RUNE, Why.IDLE);
        if (!f.solvable() && sealed >= c.idleRuneSeconds() * 20L) return new Decision(Action.SHOW_RUNE, Why.UNSOLVABLE);
        if (sealed >= c.runeAfterSeconds() * 20L) return new Decision(Action.SHOW_RUNE, Why.TIMEOUT);
        return new Decision(Action.HOLD, Why.NONE);
    }

    // ================================================================ what a mechanic can be beaten by
    /** The primitives of a mechanic that bar the doorways or cage the party: a mechanic that uses one is a lock. */
    public static final Set<String> SEALING_OPS = Set.of("seal_doors", "cage");

    /** Does a mechanic with these effect primitives lock players in? */
    public static boolean seals(Collection<String> ops) {
        if (ops == null) return false;
        for (String op : ops) if (SEALING_OPS.contains(op)) return true;
        return false;
    }

    /**
     * Can the players present beat a mechanic with this counter? {@code none} cannot be beaten by definition ("survive it or leave"), a plate puzzle that needs
     * {@code need} different players cannot be beaten by fewer, everything else can (it may be hard, but it can be done).
     */
    public static boolean solvable(String counterKind, int need, int playersInside) {
        if (counterKind == null || counterKind.isEmpty() || counterKind.equals("none")) return false;
        if (counterKind.equals("split_plates")) return playersInside >= Math.max(1, need);
        return true;
    }

    // ================================================================ the rune
    /** What the rune does when it is used. */
    public enum RuneUse {
        /** Open the lock where it stands. */
        RELEASE,
        /** Open the lock AND send the players inside back to the entrance, with the boss healed (a boss arena). */
        EXIT_AND_RESET
    }

    public static RuneUse runeFor(Kind kind) { return kind == Kind.BOSS ? RuneUse.EXIT_AND_RESET : RuneUse.RELEASE; }

    /** Seconds until the rune is visible for a lock in this state (0 = already), or -1 when it never will be by the timers alone. For the status command. */
    public static long secondsUntilRune(Facts f, Config config) {
        Config c = config.clamped();
        if (f.kind() == Kind.BOSS) return 0;
        long need = c.runeAfterSeconds();
        if (f.hostiles() <= 0 || !f.solvable()) need = Math.min(need, c.idleRuneSeconds());
        return Math.max(0, need - f.sealedTicks() / 20L);
    }

    /** Seconds until the lock opens by itself at the latest (the earliest of the timers that apply), for the status command. */
    public static long secondsUntilRelease(Facts f, Config config) {
        Config c = config.clamped();
        long cap = (f.kind() == Kind.BOSS ? c.bossMaxSeconds() : c.maxSealSeconds()) - f.sealedTicks() / 20L;
        if (f.kind() != Kind.BOSS) {
            if (!f.solvable()) cap = Math.min(cap, c.unsolvableSeconds() - f.sealedTicks() / 20L);
            if (f.hostiles() <= 0) cap = Math.min(cap, c.rescueMinutes() * 60L - f.quietTicks() / 20L);
        }
        if (f.playersInside() <= 0) cap = Math.min(cap, c.absentSeconds() - f.emptyTicks() / 20L);
        return Math.max(0, cap);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
