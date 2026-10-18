package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Why: the ONE door every other system uses to read or change a player's skill tree (the items that unlock routes, the screen's packets, the abilities' cast gate, the shop, the loot, the
 * commands). Putting the rules behind one class is what makes them impossible to skip: a book cannot add a route without the announcement and the sync, an ability cannot be cast without the
 * check, SP cannot be spent twice. The reads at the top work as they stand (they only look at the attachment); the core builder replaces the whole class with the real writes (validation against
 * the route registry, prerequisites, SP, granting and forgetting the ManasCore skills, sync packets, quest hooks) and keeps every signature. The stub's writes refuse, so a system built
 * against it compiles and fails safe.
 */
public final class SkillTreeApi {
    private SkillTreeApi() {}

    /** The result of unlocking a route. */
    public enum UnlockResult { OK, ALREADY, UNKNOWN_ROUTE, DISABLED }

    /** The result of buying a node level. */
    public enum UpgradeResult { OK, NO_ROUTE, UNKNOWN_NODE, MAXED, PARENTS, NO_SP, DISABLED }

    // ------------------------------------------------------------------ reads (both sides)
    /** The player's tree; on the client, the local player's synced copy. */
    public static IPlayerSkillTree of(Player player) { return SkillTreeAttachments.get(player); }

    public static boolean hasRoute(Player player, String routeId) { return of(player).hasRoute(routeId); }

    public static int nodeLevel(Player player, String routeId, String nodeId) { return of(player).nodeLevel(routeId, nodeId); }

    /** True when the route is unlocked and the node is at level 1 or more: the node is "active in the capability map". */
    public static boolean isNodeActive(Player player, String routeId, String nodeId) { return hasRoute(player, routeId) && nodeLevel(player, routeId, nodeId) >= 1; }

    public static int sp(Player player) { return of(player).sp(); }

    // ------------------------------------------------------------------ the cast gate (server)
    /**
     * The ability hook: may this player cast the ability of this node right now? Requires the route unlocked AND the node bought, and (config {@code requireActiveClass}) the route's class to be
     * the player's active class. The stub checks the first two only.
     */
    public static boolean canCast(Player player, String routeId, String nodeId) { return isNodeActive(player, routeId, nodeId); }

    // ------------------------------------------------------------------ writes (server only; the stub refuses)
    /** Adds SP (negative removes), clamped at 0, then syncs. {@code why} is a short key for the log. */
    public static void addSp(ServerPlayer player, int amount, String why) {}

    /** Unlocks a route for good: adds it, sets the active class when none is set, learns the granted skills of nodes already at level 1 (none), syncs, fires the hook. {@code announce}: sound, effects, banner. */
    public static UnlockResult unlockRoute(ServerPlayer player, String routeId, boolean announce) { return UnlockResult.DISABLED; }

    /** Buys one level of a node (the server re-checks everything the client could have lied about). */
    public static UpgradeResult upgradeNode(ServerPlayer player, String routeId, String nodeId) { return UpgradeResult.DISABLED; }

    /** Makes a class (the {@code classId} of an unlocked route) the active one; false when refused (not unlocked, cooldown). */
    public static boolean setActiveClass(ServerPlayer player, String classId) { return false; }

    /** Sends the player's tree to their client. */
    public static void resync(ServerPlayer player) {}

    /** Makes the ManasCore skills of the player match the tree: learn the skill of every bought node, forget those of nodes not bought. Called on login and respawn. */
    public static void reconcileSkills(ServerPlayer player) {}
}
