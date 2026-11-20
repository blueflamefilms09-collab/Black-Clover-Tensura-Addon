package com.newuniverse.nusmp.arc.realm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the boss realm (docs/boss_realm.md) carries the party of a boss that is NOT a dungeon boss into the story-arena dimension to fight there, and brings it home again. Every decision in it is
 * a small question with a plain answer (who is a realm boss, which arena, who goes along, may the door open, how long may a site stand empty, when does a rest end in a ride home), so they
 * are all answered here, with no Minecraft types, and unit tested ({@code BossRealmRulesTest}); {@link BossRealm} only asks and carries the answers out.
 */
public final class BossRealmRules {
    private BossRealmRules() {}

    /** The arena ids of the realm bosses that have a place of their own ({@code arc/arena/SevenSeasIsleShape} and {@code TournamentGroundsShape}); every other realm boss uses its saga's arena. */
    public static final String SEVEN_SEAS = "seven_seas_isle", TOURNAMENT = "tournament_grounds";

    /**
     * Does this boss fight in the realm? A boss that stands on no dungeon floor ({@code floorMax} 0), that is not one of the seven older bosses (they have their own halls, the Final Hour's
     * courtyard and the Charybdis floor) and that names no arena of its own. Today this is exactly {@code goku_boss} and {@code sinbad_boss}; a new boss added without floors joins by itself.
     */
    public static boolean isRealmBoss(boolean boss, int floorMax, boolean olderBoss, boolean namesOwnArena) {
        return boss && floorMax <= 0 && !olderBoss && !namesOwnArena;
    }

    /** The arena a realm boss has of its own, or null (its saga's arena is used). */
    public static String shapeId(String characterId) {
        if (characterId == null) return null;
        return switch (characterId) {
            case "sinbad_boss" -> SEVEN_SEAS;
            case "goku_boss" -> TOURNAMENT;
            default -> null;
        };
    }

    // ---------------------------------------------------------------- who goes along
    /**
     * The companions of the one who opens the door: the indexes (into the parallel arrays) of the eligible players within {@code radius} (squared distances are given), nearest first, at most
     * {@code partyCap - 1} of them (the one who opens the door is the first of the party; a party never exceeds 16, the arena's arrival limit). Pure: the caller builds the arrays.
     */
    public static int[] companions(double[] dist2, boolean[] eligible, double radius, int partyCap) {
        int room = Math.max(0, Math.min(16, partyCap) - 1);
        List<Integer> in = new ArrayList<>();
        double r2 = radius * radius;
        for (int i = 0; i < dist2.length && i < eligible.length; i++) if (eligible[i] && dist2[i] <= r2) in.add(i);
        in.sort(Comparator.comparingDouble(i -> dist2[i]));
        return in.stream().limit(room).mapToInt(Integer::intValue).toArray();
    }

    // ---------------------------------------------------------------- the door
    public enum Verdict { OPEN, DISABLED, BUSY, SLOW_DOWN }

    /**
     * May the door open now? {@code activeSites}: realm sites with a player or a fight; {@code thisSiteActive}: the boss's own site is one of them (joining it does not need a free site);
     * {@code cooling}: this player used a door a moment ago. A site that rests after a fight is NOT refused: a player who died there may come back for the things that lie where they fell, and the
     * realm carries everyone in a resting site home after {@code returnSeconds} anyway.
     */
    public static Verdict verdict(boolean enabled, int activeSites, int maxSites, boolean thisSiteActive, boolean cooling) {
        if (!enabled) return Verdict.DISABLED;
        if (cooling) return Verdict.SLOW_DOWN;
        if (!thisSiteActive && activeSites >= Math.max(1, maxSites)) return Verdict.BUSY;
        return Verdict.OPEN;
    }

    // ---------------------------------------------------------------- one site's clock
    /**
     * The only state the realm keeps per site (a slot of the grid, at most 256, never per player): since when nobody is there, since when the fight runs, since when the site rests with people
     * in it. All in server ticks, pure, forgotten when the site is quiet and on server stop. The people themselves are never stored here.
     */
    public static final class Clock {
        private long emptySince = -1, fightSince = -1, restSince = -1;

        /** The number of fighters the arena referee should believe: the real count, or one while the site has been empty for less than {@code graceTicks} (a dead or disconnected party may come back). */
        public int present(int players, long now, long graceTicks) {
            if (players > 0) {
                emptySince = -1;
                return players;
            }
            if (emptySince < 0) emptySince = now;
            return now - emptySince < Math.max(0L, graceTicks) ? 1 : 0;
        }

        /** Tells the clock whether a fight is running (so it can time it). */
        public void fight(boolean fighting, long now) {
            if (!fighting) fightSince = -1;
            else if (fightSince < 0) fightSince = now;
        }

        /** Has the fight run longer than the limit? A limit of 0 means no limit. */
        public boolean timedOut(long now, long limitTicks) { return fightSince >= 0 && limitTicks > 0 && now - fightSince >= limitTicks; }

        /** Is it time to carry the people of a resting site home? False while it does not rest or nobody is in it. */
        public boolean returnDue(boolean resting, int players, long now, long afterTicks) {
            if (!resting || players <= 0) {
                restSince = -1;
                return false;
            }
            if (restSince < 0) restSince = now;
            return now - restSince >= Math.max(0L, afterTicks);
        }

        /** Nothing running and nobody there: the clock can be dropped. */
        public boolean idle(int players) { return players <= 0 && fightSince < 0; }
    }

    /** A setting in seconds or minutes as ticks (never negative, never overflowing). */
    public static long ticks(double units, int perUnit) { return Math.max(0L, Math.round(Math.min(1.0e6, Math.max(0.0, units)) * perUnit)); }
}
