package com.newuniverse.nusmp.entity.zogratis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Picks a boss's next move (pure, deterministic for a seed). A move is allowed when
 * <ul>
 *   <li>the boss's power reaches its minimum, the target is inside its range band, its cooldown has run out and the mana pays for it;</li>
 *   <li>it is not an ultimate that came too soon: at most one ultimate per gap (the first only after {@link #FIRST_ULTIMATE_DELAY}),
 *       and never right after another ultimate;</li>
 *   <li>it is not the same ordinary move a third time in a row.</li>
 * </ul>
 * Among the allowed moves one is drawn by weight: ultimates grow likelier as health falls, crowd moves against two or more foes,
 * moves aimed at the current distance, and a repeat is discouraged. {@link #pick} returns the index or -1 (none: the boss closes in
 * or melees); the boss calls {@link #commit} when it starts the move.
 */
public final class MoveScheduler {
    public static final int FIRST_ULTIMATE_DELAY = 400, REPEAT_LIMIT = 2;

    /** The fight as the scheduler sees it. 'mana' is infinity for an unbound pool. */
    public record Situation(double distance, float healthFraction, int power, int foes, long now, double mana, int ultimateGap) {}

    private final Random rng;
    private final Map<String, Long> readyAt = new HashMap<>();
    private String last = "";
    private int repeats;
    private boolean lastWasUltimate;
    private long nextUltimateAt = FIRST_ULTIMATE_DELAY;

    public MoveScheduler(long seed) { rng = new Random(seed); }

    /** A fresh fight starting at 'now': cooldowns and history cleared. */
    public void reset(long now) {
        readyAt.clear();
        last = "";
        repeats = 0;
        lastWasUltimate = false;
        nextUltimateAt = now + FIRST_ULTIMATE_DELAY;
    }

    public long cooldownLeft(String id, long now) { return Math.max(0, readyAt.getOrDefault(id, 0L) - now); }

    public String last() { return last; }

    /** Puts a move on cooldown until 'until' (a boss that wants a move to wait). */
    public void holdUntil(String id, long until) { readyAt.merge(id, until, Math::max); }

    /** Is this move allowed in this situation? */
    public boolean eligible(MoveSpec m, Situation s) {
        if (s.power() < m.minPower()) return false;
        if (s.distance() < m.minRange() || s.distance() > m.maxRange()) return false;
        if (s.now() < readyAt.getOrDefault(m.id(), 0L)) return false;
        if (s.mana() < m.manaCost()) return false;
        if (m.ultimate()) return s.now() >= nextUltimateAt && !lastWasUltimate;
        return !(m.id().equals(last) && repeats >= REPEAT_LIMIT);
    }

    /** The chance weight of an allowed move. */
    double weightOf(MoveSpec m, Situation s) {
        double w = Math.max(1, m.weight());
        if (m.ultimate()) w *= 1.0 + 2.0 * (1.0 - clamp01(s.healthFraction()));
        if (m.crowd()) w *= s.foes() >= 2 ? 1.8 : 0.5;
        if (!m.ultimate() && m.id().equals(last)) w *= 0.35;
        double span = m.maxRange() - m.minRange();
        if (span > 0 && span < 1e6) {
            double off = Math.abs(s.distance() - (m.minRange() + m.maxRange()) / 2) / (span / 2);
            w *= 1.25 - 0.5 * clamp01(off);
        }
        return w;
    }

    /** The index of the next move in 'moves', or -1 when none is allowed. */
    public int pick(List<? extends MoveSpec> moves, Situation s) {
        int n = moves.size();
        double[] w = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            MoveSpec m = moves.get(i);
            if (!eligible(m, s)) continue;
            w[i] = weightOf(m, s);
            total += w[i];
        }
        if (total <= 0) return -1;
        double roll = rng.nextDouble() * total;
        int chosen = -1;
        for (int i = 0; i < n; i++) {
            if (w[i] <= 0) continue;
            chosen = i;
            roll -= w[i];
            if (roll < 0) break;
        }
        return chosen;
    }

    /** Like {@link #pick}, but returns the move itself (empty when none is allowed). */
    public <M extends MoveSpec> Optional<M> choose(List<M> moves, Situation s) {
        int i = pick(moves, s);
        return i < 0 ? Optional.empty() : Optional.of(moves.get(i));
    }

    /** The boss started 'm': its cooldown runs, the history and the ultimate pacing move on. */
    public void commit(MoveSpec m, Situation s, int cooldownTicks) {
        readyAt.put(m.id(), s.now() + Math.max(0, cooldownTicks));
        if (m.id().equals(last)) repeats++;
        else { last = m.id(); repeats = 1; }
        lastWasUltimate = m.ultimate();
        if (m.ultimate()) nextUltimateAt = s.now() + Math.max(s.ultimateGap(), cooldownTicks);
    }

    private static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
}
