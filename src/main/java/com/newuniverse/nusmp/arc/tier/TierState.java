package com.newuniverse.nusmp.arc.tier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the traits of a light boss (soul marks, the Karma mark, the Balance's window, the gaps between two steam fires ...) are small pieces of per-fight memory about the players it
 * fights. Kept in one pure class (no Minecraft types, only UUIDs and game-time ticks) so a unit test can prove the two promises that matter on a public server: nothing is kept for more than
 * {@link TierTrait#MAX_TRACKED} players at once, and everything expires on its own. A boss owns one of these; {@code TierBoss} clears it when the boss leaves the world.
 *
 * <p>Cost: at most {@link TierTrait#MAX_TRACKED} entries per map, five maps, pruned once a second by the boss; every operation is O(entries) at worst (24), no allocation per tick.
 */
public final class TierState {
    /** What the Balance found when its window closed. */
    public record Window(UUID top, float topDamage, float total, int fighters) {
        /** Nothing was dealt in the window. */
        public boolean empty() { return total <= 0f; }

        /** The top dealer's share of the window's damage (0 when empty). */
        public float share() { return total <= 0f ? 0f : topDamage / total; }
    }

    private static final class Mark {
        int count;
        long until;
    }

    private final Map<UUID, Mark> marks = new HashMap<>();
    private final Map<UUID, Long> karma = new HashMap<>();
    private final Map<UUID, Long> steamNext = new HashMap<>();
    private final Map<UUID, Long> lockNext = new HashMap<>();
    private final Map<UUID, Float> window = new HashMap<>();
    private long mirrorUntil, guardUntil, frenzyUntil;
    private long windowEnd;
    private int fusionSeen;

    // ---------------------------------------------------------------- soul marks
    /** Adds a mark to a player (the count is at most {@link TierTrait#MARK_MAX}); returns how many marks they carry now. An expired set starts again from one. */
    public int addMark(UUID who, long now) {
        Mark m = marks.get(who);
        if (m == null || m.until <= now) {
            if (m == null) makeRoom(marks.size(), () -> evictOldestMark());
            m = new Mark();
            marks.put(who, m);
        }
        m.count = Math.min(TierTrait.MARK_MAX, m.count + 1);
        m.until = now + TierTrait.MARK_TICKS;
        return m.count;
    }

    /** Marks a player carries at game tick {@code now}. */
    public int marks(UUID who, long now) {
        Mark m = marks.get(who);
        return m == null || m.until <= now ? 0 : m.count;
    }

    /** A mark that has fired is cleared. */
    public void clearMarks(UUID who) { marks.remove(who); }

    // ---------------------------------------------------------------- karma
    /** The Karma move marked this player; the mark lasts {@link TierTrait#KARMA_TICKS}. */
    public void markKarma(UUID who, long now) {
        if (!karma.containsKey(who)) makeRoom(karma.size(), () -> evictOldest(karma));
        karma.put(who, now + TierTrait.KARMA_TICKS);
    }

    public boolean karma(UUID who, long now) {
        Long until = karma.get(who);
        return until != null && until > now;
    }

    /** Milk and the like clear the mark early. */
    public void clearKarma(UUID who) { karma.remove(who); }

    // ---------------------------------------------------------------- rate limits (steam fires, system lock)
    /** True (and the gap starts) when this attacker may be set alight again. */
    public boolean steamReady(UUID who, long now) { return ready(steamNext, who, now, TierTrait.STEAM_GAP_TICKS); }

    /** True (and the gap starts) when this player may lose another beneficial effect. */
    public boolean lockReady(UUID who, long now) { return ready(lockNext, who, now, TierTrait.LOCK_GAP_TICKS); }

    private boolean ready(Map<UUID, Long> next, UUID who, long now, int gap) {
        Long at = next.get(who);
        if (at != null && at > now) return false;
        if (at == null) makeRoom(next.size(), () -> evictOldest(next));
        next.put(who, now + gap);
        return true;
    }

    // ---------------------------------------------------------------- timed stances
    public void startMirror(long now) { mirrorUntil = Math.max(mirrorUntil, now + TierTrait.MIRROR_TICKS); }

    public boolean mirror(long now) { return now < mirrorUntil; }

    public void startGuard(long now) { guardUntil = Math.max(guardUntil, now + TierTrait.UNTOUCHABLE_TICKS); }

    public boolean guard(long now) { return now < guardUntil; }

    public void startFrenzy(long now) { frenzyUntil = Math.max(frenzyUntil, now + TierTrait.FRENZY_TICKS); }

    public boolean frenzy(long now) { return now < frenzyUntil; }

    // ---------------------------------------------------------------- the Balance
    /**
     * The Balance weighs the damage dealt to him while he raises the scales: the window opens at the first telegraph of the bound move and shuts at its landing (or
     * {@link TierTrait#BALANCE_SLACK_TICKS} after the wind-up if the move was interrupted). Idempotent while a window is still open, so a telegraph that is sent every third tick opens it once.
     */
    public void openWindow(long now, int windUp) {
        if (windowOpen(now)) return;
        window.clear();
        windowEnd = now + windUp + TierTrait.BALANCE_SLACK_TICKS;
    }

    public boolean windowOpen(long now) { return now < windowEnd; }

    /** Damage the boss took from a player counts only while the window is open. */
    public void addWindow(UUID who, float damage, long now) {
        if (!windowOpen(now) || damage <= 0f) return;
        Float old = window.get(who);
        if (old == null && window.size() >= TierTrait.MAX_TRACKED) return;
        window.put(who, (old == null ? 0f : old) + damage);
    }

    /** Closes the window: who dealt the most, how much in all, by how many players. Nothing is left open or counted afterwards. */
    public Window closeWindow() {
        UUID top = null;
        float topDamage = 0f, total = 0f;
        for (Map.Entry<UUID, Float> e : window.entrySet()) {
            total += e.getValue();
            if (e.getValue() > topDamage) { topDamage = e.getValue(); top = e.getKey(); }
        }
        Window w = new Window(top, topDamage, total, window.size());
        window.clear();
        windowEnd = 0;
        return w;
    }

    // ---------------------------------------------------------------- fusion
    /** Fusion steps already announced. */
    public int fusionSeen() { return fusionSeen; }

    public void fusionSeen(int steps) { fusionSeen = Math.max(0, steps); }

    // ---------------------------------------------------------------- upkeep
    /** Drops everything that has run out (called once a second by the boss). */
    public void prune(long now) {
        for (Iterator<Map.Entry<UUID, Mark>> it = marks.entrySet().iterator(); it.hasNext(); ) if (it.next().getValue().until <= now) it.remove();
        karma.values().removeIf(until -> until <= now);
        steamNext.values().removeIf(until -> until <= now);
        lockNext.values().removeIf(until -> until <= now);
    }

    /** Forget a player (logout, death, dimension change). */
    public void forget(UUID who) {
        marks.remove(who);
        karma.remove(who);
        steamNext.remove(who);
        lockNext.remove(who);
        window.remove(who);
    }

    /** Everything is forgotten (the boss left the world or its fight was reset). */
    public void clear() {
        marks.clear();
        karma.clear();
        steamNext.clear();
        lockNext.clear();
        window.clear();
        mirrorUntil = guardUntil = frenzyUntil = 0;
        windowEnd = 0;
        fusionSeen = 0;
    }

    /** Entries held in all maps (for the tests and the cost statement: never more than five times {@link TierTrait#MAX_TRACKED}). */
    public int size() { return marks.size() + karma.size() + steamNext.size() + lockNext.size() + window.size(); }

    // ---------------------------------------------------------------- the cap
    private static void makeRoom(int size, Runnable evict) {
        if (size >= TierTrait.MAX_TRACKED) evict.run();
    }

    private static void evictOldest(Map<UUID, Long> map) {
        UUID oldest = null;
        long least = Long.MAX_VALUE;
        for (Map.Entry<UUID, Long> e : map.entrySet()) if (e.getValue() < least) { least = e.getValue(); oldest = e.getKey(); }
        if (oldest != null) map.remove(oldest);
    }

    private void evictOldestMark() {
        UUID oldest = null;
        long least = Long.MAX_VALUE;
        for (Map.Entry<UUID, Mark> e : marks.entrySet()) if (e.getValue().until < least) { least = e.getValue().until; oldest = e.getKey(); }
        if (oldest != null) marks.remove(oldest);
    }
}
