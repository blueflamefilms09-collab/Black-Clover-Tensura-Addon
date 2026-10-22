package com.newuniverse.nusmp.client.aura;

/**
 * Spirit Dive remake: the one question the dive painters ask of the cast animation system: is this player casting right now (a page's chant or release clip is playing)? Used to make the flames of
 * Fuegoleon's arm leap and the dive flare during a cast; the answer comes from the clips the server already sends (client.CastAnimClient), so it costs no packet. Kept in its own class so the
 * painters stay free of the cast animation's game classes (the render preview compiles them against a stub of this one).
 */
public final class DiveCast {
    private DiveCast() {}

    public static boolean casting(int entityId) { return com.newuniverse.nusmp.client.CastAnimClient.oneShotActive(entityId); }
}
