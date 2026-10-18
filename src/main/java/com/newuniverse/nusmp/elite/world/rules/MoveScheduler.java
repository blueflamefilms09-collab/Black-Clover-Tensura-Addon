package com.newuniverse.nusmp.elite.world.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Why: both calamity bosses fight with a list of moves (a telegraphed wind-up, then the blow, then a recovery). Which move comes next is a pure decision: only moves that are unlocked in the
 * current phase, in range, and off cooldown are candidates, the move just used is skipped when anything else is possible, and the choice is weighted. It answers null when nothing can be used,
 * and the boss then simply walks to its target. Seeded in tests, so a fight is reproducible.
 */
public final class MoveScheduler {
    private MoveScheduler() {}

    /** One move. {@code minPhase} is the first phase it appears in, ranges in blocks, times in ticks. {@code once} moves (phase openers) are not chosen here. */
    public record Move(String id, int minPhase, int windup, int recover, int cooldown, double minRange, double maxRange, int weight) {
        public Move {
            weight = Math.max(1, weight);
            windup = Math.max(1, windup);
        }
    }

    /** The first usable move chosen by weight, or null. {@code readyAt} maps move id to the game time it is next allowed. */
    public static Move choose(List<Move> moves, int phase, double distance, Map<String, Long> readyAt, long now, String last, Random rng) {
        List<Move> pool = candidates(moves, phase, distance, readyAt, now);
        if (pool.isEmpty()) return null;
        if (pool.size() > 1 && last != null) {
            List<Move> without = new ArrayList<>();
            for (Move m : pool) if (!m.id().equals(last)) without.add(m);
            if (!without.isEmpty()) pool = without;
        }
        int total = 0;
        for (Move m : pool) total += m.weight();
        int roll = rng.nextInt(total), acc = 0;
        for (Move m : pool) {
            acc += m.weight();
            if (roll < acc) return m;
        }
        return pool.get(pool.size() - 1);
    }

    public static List<Move> candidates(List<Move> moves, int phase, double distance, Map<String, Long> readyAt, long now) {
        List<Move> pool = new ArrayList<>();
        for (Move m : moves) {
            if (m.minPhase() > phase) continue;
            if (distance < m.minRange() || distance > m.maxRange()) continue;
            if (now < readyAt.getOrDefault(m.id(), 0L)) continue;
            pool.add(m);
        }
        return pool;
    }

    /** The cooldown of a move scaled by the phase's tuning (never under 10 ticks). */
    public static int scaledCooldown(Move m, CalamityPhases.Tuning t) { return Math.max(10, (int) Math.round(m.cooldown() * t.cooldownMult())); }
}
