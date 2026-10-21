package com.newuniverse.nusmp.audio.music;

/**
 * Why: the themed music is not one track per floor, it is a LAYER that follows what is happening to the player: walking through a floor (EXPLORE), standing in a safe room
 * (SAFE), in the Hub (HUB), inside a rift pocket (RIFT), fighting (BATTLE) or facing a boss (BOSS). The server decides the layer from facts only it knows (who is hunting the
 * player, which room this is) and sends the number; the client decides which file that means and how to cross-fade to it. The priority is what the client's director uses to
 * decide how eagerly to switch: going UP (a fight starts) is quick, going DOWN (the fight is over) waits, so the music never flaps between two layers on a hit-and-run.
 * The wire code is the ordinal, so entries are only ever appended.
 */
public enum MusicLayer {
    /** No themed music: outside the dungeon, or the server has nothing to say. */
    OFF(0),
    /** Walking through a floor: the theme's slow ambient loop. */
    EXPLORE(1),
    /** A safe room or rest anchor. */
    SAFE(2),
    /** The Sanctuary Hub. */
    HUB(2),
    /** Inside a rift pocket. */
    RIFT(2),
    /** Something is hunting the player: the theme's fast loop with drums. */
    BATTLE(3),
    /** A boss is close: the boss loop of the road. */
    BOSS(4);

    private final int priority;

    MusicLayer(int priority) { this.priority = priority; }

    /** Higher wins: a boss over a fight over the calm layers. */
    public int priority() { return priority; }

    public int code() { return ordinal(); }

    /** The layer with this wire code; anything unknown is {@link #OFF}. */
    public static MusicLayer fromCode(int code) {
        MusicLayer[] all = values();
        return code >= 0 && code < all.length ? all[code] : OFF;
    }
}
