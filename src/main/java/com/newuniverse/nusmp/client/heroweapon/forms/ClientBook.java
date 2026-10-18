package com.newuniverse.nusmp.client.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.forms.FormsNet;

/**
 * Why: the Weapon Book is a view of what the server last said about the player's bond, and the screen can be opened, closed and rebuilt (a window resize) while
 * the facts stay the same. This holds the latest state packet in one place, on the client thread only, and forgets it when the player leaves the world.
 */
public final class ClientBook {
    private ClientBook() {}

    private static FormsNet.State state;

    /** The last state the server sent, or null before the first. */
    public static FormsNet.State state() { return state; }

    static void set(FormsNet.State s) { state = s; }

    static void clear() { state = null; }
}
