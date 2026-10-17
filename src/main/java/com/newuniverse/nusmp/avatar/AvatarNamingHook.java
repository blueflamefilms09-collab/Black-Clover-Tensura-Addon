package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.evolution.AvatarEvolution;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingInterceptor;

/**
 * Why: the door through which the Avatar system joins Tensura's events, called once at common setup. A player can also name the Avatar with Tensura's own naming, and
 * the owner wants that to count (see {@link TensuraNamingInterceptor}, which holds the logic since the Avatar rebuild); Tensura's Harvest Festival is what an Avatar
 * evolves with (see {@link AvatarEvolution}). Each registration is defensive on its own: if an event is not what it was written against it logs once and the rest still
 * works.
 */
final class AvatarNamingHook {
    private AvatarNamingHook() {}

    static void register() {
        TensuraNamingInterceptor.register();
        AvatarEvolution.registerTensuraHooks();
    }
}
