package com.newuniverse.nusmp.relics.wave;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 4: the short-lived server state the wave keeps by id (never an entity or a level): who is weakened by the Coven's gaze, who is
 * charmed by the peacock, who may not be charmed again, whose Truesight is open, and which Forest blessing each wearer has applied.
 * All of it is bounded, expires by itself, and is emptied on logout, death and server stop. Pure Java.
 */
final class Wave4State {
    private Wave4State() {}

    /** Foes under the Weakening Gaze: they take 30 percent more. */
    static final Wave4Timers GAZE = new Wave4Timers(128);
    /** Charmed humanoids: they will not take a player as a target. */
    static final Wave4Timers CHARM = new Wave4Timers(64);
    /** Humanoids that shrugged the charm off: immune for a game day. */
    static final Wave4Timers CHARM_IMMUNE = new Wave4Timers(256);
    /** Players whose Truesight (Amulet of the Sea of Stars) is open. */
    static final Wave4Timers SIGHT = new Wave4Timers(64);
    /** The Forest blessing whose modifiers are currently on each wearer. */
    static final Map<UUID, Wave4Forest> FOREST = new HashMap<>();
    /** Wearers whose gills (Pendant of the Dreamer) are currently on. */
    static final java.util.Set<UUID> GILLS = new java.util.HashSet<>();

    /** Forget one player (logout, death, respawn). */
    static void forget(UUID player) {
        SIGHT.remove(player);
        FOREST.remove(player);
        GILLS.remove(player);
        Wave4Glass.forget(player);
        Wave4Echo.end(player);
    }

    static void clearAll() {
        GAZE.clear();
        CHARM.clear();
        CHARM_IMMUNE.clear();
        SIGHT.clear();
        FOREST.clear();
        GILLS.clear();
        Wave4Glass.clear();
        Wave4Echo.clear();
    }
}
