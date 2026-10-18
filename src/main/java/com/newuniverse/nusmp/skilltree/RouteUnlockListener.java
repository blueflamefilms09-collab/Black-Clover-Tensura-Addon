package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a route can be unlocked by a book, a command, the shop or the loot, and the owner wants every one of them to feel like an anime power-up (a circle, a flash, a title), but the engine
 * must not know how the effect is drawn: effects belong to the items system, the rules to the core. So {@code SkillTreeApi.unlockRoute} does the rules and then tells every registered
 * listener that a route was unlocked ({@code announce} says whether this unlock should be celebrated: a quiet unlock by a command passes false). The items system registers one listener at start
 * and plays the effects; a different mod could register another. A listener that throws never stops the unlock or the other listeners (the API catches and logs it).
 */
@FunctionalInterface
public interface RouteUnlockListener {
    /** Called on the server thread, after the route was added to the player's tree and the sync was queued. */
    void onUnlocked(ServerPlayer player, SkillRoute route, boolean announce);
}
