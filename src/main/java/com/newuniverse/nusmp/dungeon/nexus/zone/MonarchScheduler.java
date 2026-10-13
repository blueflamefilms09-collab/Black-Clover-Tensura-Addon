package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * Chooses the Monarch's next move: a small utility scheduler in the manner of the other bosses' (a score per move from the situation,
 * a weighted pick). It is pure and takes its dice as a number in [0, 1), so the same situation and roll always give the same move. Rules it
 * keeps: nothing before the pause that follows the last move, nothing still cooling down, never the same move twice running, no soldiers when
 * the cap is nearly reached, and a move that sets up another (the field of Authority) makes the slam and the line likelier for a while.
 */
public final class MonarchScheduler {
    /** What the scheduler needs to know. */
    public record Situation(int phase, int minions, int foes, double nearest) {}

    private final long[] readyAt = new long[MonarchMove.values().length];
    private long nextAt;
    private MonarchMove last;
    private long authorityAt = Long.MIN_VALUE / 2;

    /** The ticks after Authority during which the slam and the line are favoured. */
    static final int COMBO_WINDOW = 120;

    /** Forget everything: a fresh fight. The first move comes `delay` ticks from `now`. */
    public void reset(long now, int delay) {
        java.util.Arrays.fill(readyAt, 0L);
        nextAt = now + delay;
        last = null;
        authorityAt = Long.MIN_VALUE / 2;
    }

    public long nextAt() { return nextAt; }

    public MonarchMove last() { return last; }

    /** How much each move is wanted in this situation; 0 means it may not be chosen now. */
    public double weight(MonarchMove m, long now, Situation s) {
        if (m == last || now < readyAt[m.ordinal()]) return 0.0;
        int phase = MonarchRules.clampPhase(s.phase());
        double near = s.nearest();
        boolean combo = now - authorityAt <= COMBO_WINDOW;
        return switch (m) {
            case SLAM -> 10.0 * (near <= 9.0 ? 2.0 : near > 22.0 ? 0.3 : 1.0) * (combo ? 2.0 : 1.0);
            case RAIN -> 10.0 * (1.0 + 0.25 * Math.min(Math.max(s.foes() - 1, 0), 4)) * (near > 14.0 ? 1.2 : 1.0);
            case BEAM -> 9.0 * (near >= 14.0 ? 1.6 : near < 5.0 ? 0.6 : 1.0) * (combo ? 2.0 : 1.0);
            case SUMMON -> {
                int n = MonarchRules.summonCount(phase, s.minions());
                yield n < 2 && s.minions() > 0 || n <= 0 ? 0.0 : 7.0 * (s.minions() == 0 ? 2.5 : 1.0) * (phase >= 2 ? 1.2 : 1.0);
            }
            case AUTHORITY -> 5.0 * (phase >= 2 ? 1.5 : 0.6);
        };
    }

    /** The move to start now, or null if it is not time yet or nothing is wanted. `roll` is in [0, 1). */
    public MonarchMove pick(long now, Situation s, double roll) {
        if (now < nextAt || s.foes() <= 0) return null;
        MonarchMove[] all = MonarchMove.values();
        double total = 0;
        double[] w = new double[all.length];
        for (int i = 0; i < all.length; i++) {
            w[i] = weight(all[i], now, s);
            total += w[i];
        }
        if (total <= 0) return null;
        double at = Math.max(0.0, Math.min(roll, 0.999999999)) * total, acc = 0;
        for (int i = 0; i < all.length; i++) {
            acc += w[i];
            if (w[i] > 0 && at < acc) return all[i];
        }
        for (int i = all.length - 1; i >= 0; i--) if (w[i] > 0) return all[i];
        return null;
    }

    /** Records that a move began now: its cooldown, the pause before the next, and the combo window. */
    public void started(MonarchMove m, long now, int phase) {
        readyAt[m.ordinal()] = now + MonarchRules.cooldown(m, phase);
        nextAt = now + MonarchRules.freeze(m, phase) + MonarchRules.gap(phase);
        last = m;
        if (m == MonarchMove.AUTHORITY) authorityAt = now;
    }

    /** Puts the next choice off to `tick` at the earliest (a phase change, a recovery). */
    public void holdUntil(long tick) { nextAt = Math.max(nextAt, tick); }
}
