package com.newuniverse.nusmp.client.skilltree;

import net.minecraft.world.entity.player.Player;

/**
 * Why: the owner wants an operator or a creative player to be able to open and test EVERY registered line, without that being something an ordinary player can stumble on. The switch lives on the client
 * (it only changes what the library lists and which line the screen will ask the server to show), is offered only to a player who the server would also accept ({@code hasPermissions(2)} or creative:
 * the server checks the same thing on every inspect request, so a client that forces the flag on without being allowed gets lines listed from their cards but never a tree), and is remembered for the
 * session only: it is reset when the player leaves the world and whenever the permission is lost. Inspecting is read-only by construction: the tree of a line the player does not own is shown with the
 * sealed rules (no node can be bought), because buying is decided by ownership and the server alone.
 */
final class InspectMode {
    private InspectMode() {}

    private static boolean on;

    /** Whether this player may use the inspect mode at all (an operator or a creative player). */
    static boolean available(Player player) { return player != null && (player.hasPermissions(2) || player.isCreative()); }

    /** True while the mode is on AND the player is still allowed to use it. */
    static boolean active(Player player) {
        if (on && !available(player)) on = false;
        return on;
    }

    static void toggle() { on = !on; }

    static void reset() { on = false; }
}
