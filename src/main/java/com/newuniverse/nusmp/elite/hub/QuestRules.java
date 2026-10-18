package com.newuniverse.nusmp.elite.hub;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Why: which quests a board shows and whether an event counts for a quest are pure questions, and getting them wrong is what makes a quest system feel broken (a kill that does not count, the
 * same board for ever, a tier-5 hunt offered to a newcomer). So they are answered here with no game type: {@link #matches} compares a {@link KillFact} with a quest's filters; {@link #dailyBoard}
 * picks the day's dailies from the pool by sorting them on a hash of (day, id), so the board is the same for every player with the same unlocked floors on a given day (a party sees one board) and changes every day, and a heavier quest sorts earlier on
 * average; {@link #progressionOffers} lists the next steps of the chain (not done, not accepted, requirements done, floor
 * unlocked), in file order. The day is the world's day count (the overworld's day time divided by 24000), so "a new day" is the world's own and not the wall clock's.
 */
public final class QuestRules {
    private QuestRules() {}

    /** Ticks in a day of the world's clock. */
    public static final long TICKS_PER_DAY = 24_000L;

    /** The world's day number for a day time in ticks (negative counts as day 0). */
    public static long dayOf(long dayTime) { return Math.max(0L, dayTime) / TICKS_PER_DAY; }

    /** Does a kill count for a kill quest? */
    public static boolean matches(QuestDef q, KillFact f) {
        if (q.kind() != ObjectiveKind.KILL || f == null) return false;
        if (q.tier() != 0 && q.tier() != f.tier()) return false;
        if (!q.road().isEmpty() && !q.road().equals(f.road())) return false;
        if (!q.faction().isEmpty() && !q.faction().equals(f.faction())) return false;
        return switch (q.mob()) {
            case "foundry" -> f.foundry();
            case "boss" -> f.boss();
            default -> true;
        };
    }

    /** Does a cleared floor count for a clear-floors quest (tier and road filters)? */
    public static boolean matchesFloor(QuestDef q, int floor) {
        if (q.kind() != ObjectiveKind.CLEAR_FLOORS || floor == 0) return false;
        if (q.tier() != 0 && q.tier() != CoinLoot.tierOf(floor)) return false;
        return q.road().isEmpty() || q.road().equals(floor > 0 ? "holy" : "abyssal");
    }

    /** Does entering this floor finish a reach-floor quest: same road, as deep or deeper? */
    public static boolean reaches(QuestDef q, int floor) {
        if (q.kind() != ObjectiveKind.REACH_FLOOR || q.floor() == 0 || floor == 0) return false;
        return (q.floor() > 0) == (floor > 0) && Math.abs(floor) >= Math.abs(q.floor());
    }

    /** A stable pseudo-random number for (day, id): the board's sort key. */
    static long mix(long day, String id) {
        long h = day * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < id.length(); i++) h = (h ^ id.charAt(i)) * 0x100000001B3L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return h;
    }

    /** The daily quests on the board for a day: up to {@code count} of the pool whose {@code minFloor} the player has reached and whose weight is above 0. */
    public static List<QuestDef> dailyBoard(long day, List<QuestDef> pool, int count, int maxFloorReached) {
        List<QuestDef> eligible = new ArrayList<>();
        for (QuestDef q : pool) if (q.daily() && q.weight() > 0 && q.minFloor() <= maxFloorReached) eligible.add(q);
        // a heavier quest sorts earlier on average: the key is the hash divided by the weight
        eligible.sort(Comparator.comparingDouble((QuestDef q) -> (mix(day, q.id()) >>> 11) / (double) (1L << 53) / q.weight()).thenComparing(QuestDef::id));
        return eligible.size() <= count ? eligible : new ArrayList<>(eligible.subList(0, Math.max(0, count)));
    }

    /** The next steps of the progression chain a player can accept now, in file order, at most {@code limit}. */
    public static List<QuestDef> progressionOffers(List<QuestDef> pool, Set<String> completed, Set<String> active, int maxFloorReached, int limit) {
        List<QuestDef> out = new ArrayList<>();
        for (QuestDef q : pool) {
            if (q.type() != QuestType.PROGRESSION || completed.contains(q.id()) || active.contains(q.id()) || q.minFloor() > maxFloorReached) continue;
            if (!completed.containsAll(q.requires())) continue;
            out.add(q);
            if (out.size() >= limit) break;
        }
        return out;
    }

    /** The reward after the server's scale, in bronze (never negative, capped). */
    public static long reward(QuestDef q, double scale) {
        double v = q.reward() * Math.max(0.0, scale);
        return v >= CoinMath.CAP ? CoinMath.CAP : Math.round(v);
    }
}
