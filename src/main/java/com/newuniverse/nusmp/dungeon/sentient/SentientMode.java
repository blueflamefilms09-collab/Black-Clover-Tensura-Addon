package com.newuniverse.nusmp.dungeon.sentient;

import com.newuniverse.nusmp.dungeon.DungeonConfig;

/**
 * The one switch of the Sentient Dungeon: the server config {@code dungeon.sentient} (true by default). While it is on, nothing in the
 * Dungeon breeds or hunts for itself: mobs are born from the walls of the rooms players enter, the immune system answers an assault on the
 * Dungeon with a Juggernaut, and the dead turn to ash and leave Magic Stones. While it is off the Dungeon is the closed ecosystem of
 * 1.18 to 1.21 (hunger, breeding, carcasses), exactly as it was.
 */
public final class SentientMode {
    private SentientMode() {}

    /** dungeon.sentient (true by default) and dungeon.enabled. Safe before the config is loaded: it answers the defaults. */
    public static boolean active() {
        return DungeonConfig.get(DungeonConfig.SENTIENT) && DungeonConfig.get(DungeonConfig.ENABLED);
    }
}
