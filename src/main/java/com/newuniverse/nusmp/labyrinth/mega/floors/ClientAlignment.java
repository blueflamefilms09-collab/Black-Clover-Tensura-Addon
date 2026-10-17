package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: the client needs to know which road its player is bound to (a future gate hint, a tooltip on the Hub's signposts, a line in the System window), and
 * the only truth lives on the server, so the {@code nusmp:alignment_sync} packet fills this holder. It is plain Java with statics and no client-only import
 * (the same shape as the discovery system's cache), so a common class on a dedicated server can touch it without loading a client class, and a unit test
 * can pin it. One immutable {@link Snapshot} is swapped in whole: the network thread writes and the render thread reads, and a reader must never see a
 * half-updated mix (the new road with the old lock). Until the first packet of a session arrives ({@link #synced()} false: a server without the module, or
 * a screen opened too early) every question answers "free": the old, unlocked behaviour is what you get whenever the system is not running.
 */
public final class ClientAlignment {
    private ClientAlignment() {}

    /**
     * Why: one consistent picture of the player's alignment as the last packet described it; immutable so it can be published with a single volatile write.
     */
    public record Snapshot(Polarity chosen, boolean locked, int deepestHoly, int deepestAbyssal, boolean synced) {
        public static final Snapshot EMPTY = new Snapshot(null, false, 0, 0, false);
    }

    private static volatile Snapshot current = Snapshot.EMPTY;

    public static Snapshot snapshot() { return current; }

    /** Network handler (client thread): replaces the picture. An unknown road id reads as none and a negative distance as 0, like a damaged save does. */
    public static void receive(String chosenId, boolean locked, int deepestHoly, int deepestAbyssal) {
        Polarity road = Polarity.fromId(chosenId);
        current = new Snapshot(road, locked && road != null, Math.max(0, deepestHoly), Math.max(0, deepestAbyssal), true);
    }

    /** Logout, or a different server: back to "not synced". */
    public static void clear() { current = Snapshot.EMPTY; }

    public static boolean synced() { return current.synced(); }

    /** The road the player is on, or null. */
    public static Polarity chosen() { return current.chosen(); }

    public static boolean locked() { return current.locked(); }

    /** The deepest distance from Floor 0 the player reached on a road (0 when unknown). */
    public static int deepest(Polarity road) {
        Snapshot s = current;
        return road == Polarity.HOLY ? s.deepestHoly() : road == Polarity.ABYSSAL ? s.deepestAbyssal() : 0;
    }

    /** Is the road closed to this player? The same question as {@code AlignmentRules.mayEnter}, answered from the last packet (free before the first one). */
    public static boolean mayEnter(Polarity road) {
        Snapshot s = current;
        return AlignmentRules.mayEnter(s.chosen(), s.locked(), road, true);
    }
}
