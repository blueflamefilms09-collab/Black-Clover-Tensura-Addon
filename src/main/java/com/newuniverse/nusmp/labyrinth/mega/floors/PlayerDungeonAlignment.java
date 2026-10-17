package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.Objects;

/**
 * Why: this is the "capability" {@code PlayerDungeonAlignment} of the owner's brief: what the Directional Lock remembers about one player. In NeoForge 1.21
 * a capability is a way to ask a block, an item or an entity for behaviour ({@code IItemHandler} and friends), while data that belongs to a player and has
 * to be saved, copied when they die and sent to their client is a DATA ATTACHMENT (see {@code AlignmentAttachments}); this class is the value that
 * attachment holds. It is deliberately a plain object with no Minecraft import: the rules that change it ({@link AlignmentRules}) are pure and
 * unit-tested, and the codec that saves it only needs the five primitives below ({@link #of} and the getters), so a save can be pinned by a test that never
 * starts the game.
 *
 * <p>The state is exactly the brief's: the road the player is on ({@link #chosen()}, null until they step onto a floor), whether it is closed behind them
 * ({@link #locked()}), when ({@link #lockedAt()}, a game time), and the deepest floor ever reached on each road ({@link #deepestHoly()},
 * {@link #deepestAbyssal()}, as a distance from Floor 0, so always 0 or more). The deepest is kept per road, not as one number, because the two roads are
 * different places: someone locked to the Holy road who was once forced onto the Abyssal one (an operator's teleport) has been to depth 3 down there and to
 * depth 12 up here, and "have they already been below floor 3" has to be answerable for each. Setters are package-private on purpose: only the rules class
 * and the admin command change a state, so no other module can leave it half-edited (the invariants are: a lock always has a road, a distance is never
 * negative, and a lock time exists only while locked).
 */
public final class PlayerDungeonAlignment {
    private Polarity chosen;
    private boolean locked;
    private int deepestHoly, deepestAbyssal;
    private long lockedAt;

    public PlayerDungeonAlignment() {}

    /**
     * Rebuilds a state from its five saved values, repairing anything a damaged or hand-edited save could hold: an unknown or empty road is none, a
     * negative distance is 0, a lock without a road is no lock, and a lock time without a lock is 0. This is what the codec calls, so an old or odd save
     * always loads into a valid state.
     */
    public static PlayerDungeonAlignment of(String chosenId, boolean locked, int deepestHoly, int deepestAbyssal, long lockedAt) {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        s.chosen = Polarity.fromId(chosenId);
        s.locked = locked && s.chosen != null;
        s.deepestHoly = Math.max(0, deepestHoly);
        s.deepestAbyssal = Math.max(0, deepestAbyssal);
        s.lockedAt = s.locked ? Math.max(0L, lockedAt) : 0L;
        return s;
    }

    /** The road the player is on (the last one they stepped onto while it was still open to them, or the one an operator chose), or null. */
    public Polarity chosen() { return chosen; }

    /** The id of {@link #chosen()} ({@code holy}, {@code abyssal}) or the empty string: the form the codec and the sync packet carry. */
    public String chosenId() { return chosen == null ? "" : chosen.id(); }

    /** Is the other road closed to this player? */
    public boolean locked() { return locked; }

    /** The game time at which the lock closed, or 0 when it is not locked. */
    public long lockedAt() { return lockedAt; }

    public int deepestHoly() { return deepestHoly; }

    public int deepestAbyssal() { return deepestAbyssal; }

    /** The deepest distance from Floor 0 ever reached on a road (0 for a road never entered and for null). */
    public int deepest(Polarity road) { return road == Polarity.HOLY ? deepestHoly : road == Polarity.ABYSSAL ? deepestAbyssal : 0; }

    /** Is this the state of a player who never touched the dungeon? Such a state is not saved at all, so a world full of players who never went stays small. */
    public boolean isDefault() { return chosen == null && !locked && deepestHoly == 0 && deepestAbyssal == 0 && lockedAt == 0L; }

    public PlayerDungeonAlignment copy() { return of(chosenId(), locked, deepestHoly, deepestAbyssal, lockedAt); }

    // ---- changes: only AlignmentRules and the admin command (same package) may make them
    void choose(Polarity road) { chosen = road; if (road == null) unlock(); }

    void lock(long at) {
        if (chosen == null) return;
        locked = true;
        lockedAt = Math.max(0L, at);
    }

    void unlock() {
        locked = false;
        lockedAt = 0L;
    }

    /** Raises the deepest distance of a road; true when it grew. */
    boolean raiseDeepest(Polarity road, int distance) {
        if (road == Polarity.HOLY && distance > deepestHoly) { deepestHoly = distance; return true; }
        if (road == Polarity.ABYSSAL && distance > deepestAbyssal) { deepestAbyssal = distance; return true; }
        return false;
    }

    void clear() {
        chosen = null;
        unlock();
        deepestHoly = 0;
        deepestAbyssal = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerDungeonAlignment a)) return false;
        return chosen == a.chosen && locked == a.locked && deepestHoly == a.deepestHoly && deepestAbyssal == a.deepestAbyssal && lockedAt == a.lockedAt;
    }

    @Override
    public int hashCode() { return Objects.hash(chosen, locked, deepestHoly, deepestAbyssal, lockedAt); }

    @Override
    public String toString() {
        return "road=" + (chosen == null ? "none" : chosen.id()) + (locked ? " LOCKED since " + lockedAt : " open") + ", deepest holy " + deepestHoly + " / abyssal " + deepestAbyssal;
    }
}
