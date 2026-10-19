package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a monster the Dungeon Master put into a maze that cannot reach the party stands where it is until the game despawns it, and while it stands there it
 * counts against {@code maxLiveMobsPerParty}: eight idle zombies behind a wall were enough to stop every later encounter ("crowded") for as long as the vanilla
 * despawn took, minutes. The remedy needs one fact the game does not keep: how long a monster has been staying put. This remembers, for each monster, where it
 * was last seen to have moved to and since when. A monster counts as moved when it is more than {@link #MOVED} blocks from that anchor. Pure.
 */
public final class StuckTracker {
    /** A monster must stray this far from its anchor to count as moving (blocks); idle shuffling does not. */
    public static final double MOVED = 2.5;

    private record Anchor(double x, double y, double z, long since) {}

    private final Map<UUID, Anchor> anchors = new HashMap<>();

    /** Records where a monster is at game time {@code now} and returns for how many ticks it has stayed within {@link #MOVED} blocks of one place (0 the first time). */
    public long update(UUID id, double x, double y, double z, long now) {
        Anchor a = anchors.get(id);
        if (a == null || dist(a, x, y, z) > MOVED) {
            anchors.put(id, new Anchor(x, y, z, now));
            return 0L;
        }
        return Math.max(0L, now - a.since());
    }

    /** The idle time without changing anything (0 when the monster is not known). */
    public long idle(UUID id, double x, double y, double z, long now) {
        Anchor a = anchors.get(id);
        if (a == null || dist(a, x, y, z) > MOVED) return 0L;
        return Math.max(0L, now - a.since());
    }

    public void forget(UUID id) { anchors.remove(id); }

    public void clear() { anchors.clear(); }

    public int size() { return anchors.size(); }

    private static double dist(Anchor a, double x, double y, double z) {
        double dx = a.x() - x, dy = a.y() - y, dz = a.z() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
