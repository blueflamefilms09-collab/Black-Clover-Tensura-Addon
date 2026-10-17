package com.newuniverse.nusmp.labyrinth.mega.flow;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: a damage event does not say what caused it, and the Rukh flow rewards or punishes a CAST, so something has to remember what each player
 * cast last and when. That is all this is: per player, the alignment of the last cast and the game tick it happened on, plus the two stamps that
 * keep the flow from nagging (the tick of the last rejection, for its cooldown, and of the last on-screen effect and notice, so a zone that hits ten
 * times in a second does not send ten particle bursts). A hit counts as "from that cast" when it lands within the cast window of it (30 ticks by
 * default); the window is the pragmatic bridge between a spell that fires and the damage it deals a moment later, and its limits are written in
 * the doc (a spell that keeps hurting for six seconds is boosted only for the first one and a half). There is one more case: a spell that hurts
 * INSTANTLY, inside the very call that casts it, deals its damage before the cast is reported as finished. So a cast has two steps: {@link #beginCast}
 * (just before the spell runs: it counts for the rest of THAT tick only) and {@link #recordCast} (after the spell succeeded: it counts for the whole window).
 * A page that fails never reports success, so it never opens a window; the most it can do is colour the damage of its own tick, which is none, because a
 * failed page deals none. The tracker is pure (a {@link UUID} is not a Minecraft type), so JUnit pins the window, the two steps, the cooldown and the clock
 * edge cases. It is memory only: forgotten when a player logs out
 * and cleared when the server stops, because a one-second memory is worth nothing to save. Server thread only; the maps are concurrent just
 * so a stray call from another thread cannot corrupt them.
 */
public final class FlowTracker {
    private static final FlowTracker SHARED = new FlowTracker();

    /** Why: the alignment and the tick of one cast together, so a reader never sees one without the other. */
    public record Cast(Alignment alignment, long tick) {}

    private final Map<UUID, Cast> casts = new ConcurrentHashMap<>();
    private final Map<UUID, Cast> casting = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rejected = new ConcurrentHashMap<>();
    private final Map<UUID, Long> effect = new ConcurrentHashMap<>();
    private final Map<UUID, Long> notice = new ConcurrentHashMap<>();

    /** The tracker the game uses (cleared when the server stops). Tests make their own with {@code new FlowTracker()}. */
    public static FlowTracker shared() { return SHARED; }

    public FlowTracker() {}

    // ---------------------------------------------------------------- casts
    /**
     * Step one: the player's spell of this alignment is about to run. Until the end of tick {@code now} the damage this player deals counts as coming from
     * it; nothing is remembered beyond that tick unless {@link #recordCast} follows.
     */
    public void beginCast(UUID player, Alignment alignment, long now) {
        if (player == null || alignment == null) return;
        casting.put(player, new Cast(alignment, now));
    }

    /**
     * Step two: the player's spell of this alignment SUCCEEDED. Remembers it for the cast window (a NEUTRAL cast is remembered too: it ends the window of
     * an older LIGHT one) and closes the step-one record.
     */
    public void recordCast(UUID player, Alignment alignment, long now) {
        if (player == null || alignment == null) return;
        casts.put(player, new Cast(alignment, now));
        casting.remove(player);
    }

    /** The last cast of this player, or null. */
    public Cast lastCast(UUID player) { return player == null ? null : casts.get(player); }

    /**
     * The alignment of the cast the player's damage counts as coming from right now: the spell that is running this very tick (step one), else the last
     * finished cast if it was at most {@code windowTicks} ago (0 ticks ago counts), else null. A cast stamped in the future (the clock was set back) is not
     * recent: null.
     */
    public Alignment recentCast(UUID player, long now, int windowTicks) {
        if (player == null) return null;
        Cast running = casting.get(player);
        if (running != null && running.tick() == now) return running.alignment();
        Cast c = lastCast(player);
        if (c == null) return null;
        long age = now - c.tick();
        return age >= 0 && age <= Math.max(0, windowTicks) ? c.alignment() : null;
    }

    // ---------------------------------------------------------------- the stamps
    /** True, and the rejection is stamped, when this player may be rejected now (none for {@code cooldownTicks} ticks); false when the cooldown still runs. */
    public boolean tryReject(UUID player, long now, int cooldownTicks) {
        if (player == null) return false;
        Long last = rejected.get(player);
        if (!FlowRules.due(now, last == null ? FlowRules.NEVER : last, cooldownTicks)) return false;
        rejected.put(player, now);
        return true;
    }

    /** True, and stamped, when a particle effect for this player is due ({@code gapTicks} since the last one). */
    public boolean effectDue(UUID player, long now, int gapTicks) { return due(effect, player, now, gapTicks); }

    /** True, and stamped, when an action-bar notice for this player is due ({@code gapTicks} since the last one). */
    public boolean noticeDue(UUID player, long now, int gapTicks) { return due(notice, player, now, gapTicks); }

    private static boolean due(Map<UUID, Long> stamps, UUID player, long now, int gapTicks) {
        if (player == null) return false;
        Long last = stamps.get(player);
        if (!FlowRules.due(now, last == null ? FlowRules.NEVER : last, gapTicks)) return false;
        stamps.put(player, now);
        return true;
    }

    // ---------------------------------------------------------------- lifetime
    /** Forgets one player (logout). */
    public void forget(UUID player) {
        if (player == null) return;
        casts.remove(player);
        casting.remove(player);
        rejected.remove(player);
        effect.remove(player);
        notice.remove(player);
    }

    /** Forgets everyone (the server stopped). */
    public void clear() {
        casts.clear();
        casting.clear();
        rejected.clear();
        effect.clear();
        notice.clear();
    }

    /** How many players have a remembered cast (for tests and the status command). */
    public int size() { return casts.size(); }
}
