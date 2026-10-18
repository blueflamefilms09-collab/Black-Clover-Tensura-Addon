package com.newuniverse.nusmp.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.net.TreeNet;
import com.newuniverse.nusmp.skilltree.rules.TreeRules;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Why: the ONE door every other system uses to read or change a player's skill tree (the items that unlock routes, the screen's packets, the abilities' cast gate, the shop, the loot, the
 * commands). Putting the rules behind one class is what makes them impossible to skip: a book cannot add a route without the announcement and the sync, an ability cannot be cast without the
 * check, SP cannot be spent twice. The arithmetic is not here: it is in the pure {@link TreeRules}, tested without a game; this class adds only what needs a server (the player's saved tree, the
 * route registry, the ManasCore skills through {@code SkillGrants}, the sync queue, the quest hooks, the unlock listeners). Every write method is server-thread only, re-checks everything it is
 * given (the arguments may come straight from a packet or a command), changes nothing when it refuses, and never throws: an unexpected failure is logged and answered as a refusal. The reads work
 * on both sides and only look at the attachment (on the client, the copy the sync packet filled); the cast gate also needs the route's class from the registry, which is the server's set
 * on a server and the synced copy on a client. The methods after the contract (the listeners, the admin writes, the respec, the class switch with a reason) are additions that the commands and the
 * packet handlers need; the signatures of the contract are unchanged.
 */
public final class SkillTreeApi {
    private SkillTreeApi() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final List<RouteUnlockListener> LISTENERS = new CopyOnWriteArrayList<>();

    /** The result of unlocking a route. */
    public enum UnlockResult { OK, ALREADY, UNKNOWN_ROUTE, DISABLED }

    /** The result of buying a node level. */
    public enum UpgradeResult { OK, NO_ROUTE, UNKNOWN_NODE, MAXED, PARENTS, NO_SP, DISABLED }

    /** Why: the command and the chat line after a respec report real numbers, not a guess; what a respec did: how many saved node entries were cleared and how many Skill Points came back. */
    public record RespecResult(int cleared, int sp) {}

    // ------------------------------------------------------------------ reads (both sides)
    /** The player's tree; on the client, the local player's synced copy. */
    public static IPlayerSkillTree of(Player player) { return SkillTreeAttachments.get(player); }

    public static boolean hasRoute(Player player, String routeId) { return of(player).hasRoute(routeId); }

    public static int nodeLevel(Player player, String routeId, String nodeId) { return of(player).nodeLevel(routeId, nodeId); }

    /** True when the route is unlocked and the node is at level 1 or more: the node is "active in the capability map". */
    public static boolean isNodeActive(Player player, String routeId, String nodeId) { return hasRoute(player, routeId) && nodeLevel(player, routeId, nodeId) >= 1; }

    public static int sp(Player player) { return of(player).sp(); }

    // ------------------------------------------------------------------ the config the other systems ask about
    /** The master switch: false means nothing can be unlocked, bought or cast (a system that spends something on the player's behalf, such as an SP Book, should check it first). */
    public static boolean isEnabled() { return TreeConfig.enabled(); }

    /** What a Skill Book of a route the player already has does: true = it gives the route's duplicate Skill Points, false = it is refused. */
    public static boolean duplicateBookGivesSp() { return TreeConfig.duplicateBookGivesSp(); }

    /** Seconds between two class changes. */
    public static int classSwitchSeconds() { return TreeConfig.classSwitchSeconds(); }

    /** The most Skill Points a player can hold from gains. */
    public static int maxSp() { return TreeConfig.maxSp(); }

    /** Ticks left before this player may change class again (0 when they may). Works on the client too, from the synced tree. */
    public static long classCooldownTicks(Player player) {
        IPlayerSkillTree t = of(player);
        return TreeRules.cooldownRemaining(t.classChangedAt(), player.level().getGameTime(), TreeConfig.classSwitchSeconds() * 20L);
    }

    // ------------------------------------------------------------------ the cast gate
    /**
     * The ability hook: may this player cast the ability of this node right now? Requires the engine on, the route unlocked AND the node bought, and (config {@code requireActiveClass}) the
     * route's class to be the player's active class (creative players pass the class check). Works on both sides: the skill menu asks it on the client, the cast asks it on the server.
     */
    public static boolean canCast(Player player, String routeId, String nodeId) {
        try {
            if (player == null || !TreeConfig.enabled()) return false;
            return TreeRules.canCast(RouteRegistry.byId(routeId), new PlayerTreeState(of(player)), routeId, nodeId, TreeConfig.requireActiveClass(), player.isCreative());
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ 1.82.0: the switches of the line passives
    /**
     * Whether this node's own switch is on (true when it was never turned off). This is only the switch: whether the passive RUNS also needs the line to be equipped, which
     * {@code skilltree.line.LineGate} decides in one place.
     */
    public static boolean isPassiveEnabled(Player player, String routeId, String nodeId) { return of(player).isPassiveEnabled(routeId, nodeId); }

    /**
     * Turns the switch of a passive or attribute node on or off. Server only; refuses (false, nothing changed) when the engine is off, the route is not owned, the node does not exist, has no
     * switch (it is an art or a plain node) or is not bought. The client sends this as {@code TogglePassivePayload} and never decides.
     */
    public static boolean setPassiveEnabled(ServerPlayer player, String routeId, String nodeId, boolean enabled) {
        try {
            if (player == null || !TreeConfig.enabled()) return false;
            SkillRoute route = RouteRegistry.byId(routeId);
            SkillNode node = route == null ? null : route.node(nodeId);
            IPlayerSkillTree t = of(player);
            if (node == null || !node.hasToggle() || !t.hasRoute(routeId) || t.nodeLevel(routeId, nodeId) < 1) return false;
            t.setPassiveEnabled(routeId, nodeId, enabled);
            resync(player);
            return true;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: setPassiveEnabled failed for {} {}", routeId, nodeId, e);
            return false;
        }
    }

    // ------------------------------------------------------------------ listeners
    /** Registers a listener that is told whenever a route is unlocked (the unlock effects of the items system). Call once at start; listeners live as long as the game. */
    public static void addUnlockListener(RouteUnlockListener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    // ------------------------------------------------------------------ writes (server only)
    /**
     * Adds Skill Points (a negative amount takes them), never below 0, and never above {@code maxSp} for a gain; then queues the sync. {@code why} is a short key for the log. Does nothing
     * while the engine is off. A real gain fires the quest hook {@code tree.sp} with the amount.
     */
    public static void addSp(ServerPlayer player, int amount, String why) {
        try {
            if (player == null || amount == 0 || !TreeConfig.enabled()) return;
            IPlayerSkillTree t = of(player);
            int before = t.sp();
            int after = TreeRules.addSp(before, amount, TreeConfig.maxSp());
            if (after == before) return;
            t.setSp(after);
            resync(player);
            LOG.debug("[nusmp] skill tree: {} SP for {} ({}): {} -> {}", amount, player.getGameProfile().getName(), why, before, after);
            if (after > before) NusmpEvents.fire(player, "tree.sp", String.valueOf(after - before));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: addSp failed for {}", player == null ? "?" : player.getGameProfile().getName(), e);
        }
    }

    /**
     * Unlocks a route for good: adds it, sets the active class when none is set, teaches the skills of nodes already bought (none, for a new route), queues the sync, fires the quest hook
     * {@code tree.route} and tells the unlock listeners ({@code announce}: whether this unlock should be celebrated with sound, effects and a banner).
     */
    public static UnlockResult unlockRoute(ServerPlayer player, String routeId, boolean announce) {
        try {
            if (player == null || !TreeConfig.enabled()) return UnlockResult.DISABLED;
            SkillRoute route = RouteRegistry.byId(routeId);
            if (route == null) return UnlockResult.UNKNOWN_ROUTE;
            IPlayerSkillTree t = of(player);
            if (!t.addRoute(route.id())) return UnlockResult.ALREADY;
            if (t.activeClass().isEmpty()) t.setActiveClass(route.classId());
            SkillGrants.learnBought(player, route);
            resync(player);
            NusmpEvents.fire(player, "tree.route", route.id());
            for (RouteUnlockListener l : LISTENERS) {
                try {
                    l.onUnlocked(player, route, announce);
                } catch (RuntimeException e) {
                    LOG.warn("[nusmp] skill tree: an unlock listener failed for {}", route.id(), e);
                }
            }
            return UnlockResult.OK;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: unlockRoute failed for {}", routeId, e);
            return UnlockResult.DISABLED;
        }
    }

    /**
     * Buys one level of a node: every check of {@link TreeRules#buy}, then the node's skill is taught when it reaches level 1, the sync is queued and the quest hooks {@code tree.node}
     * (argument {@code <route>#<node>}, once per level bought) and {@code tree.node.level} (argument {@code <route>#<node>@<level>}) fire. A refusal changes nothing.
     */
    public static UpgradeResult upgradeNode(ServerPlayer player, String routeId, String nodeId) {
        try {
            if (player == null || !TreeConfig.enabled()) return UpgradeResult.DISABLED;
            SkillRoute route = RouteRegistry.byId(routeId);
            if (route == null) return UpgradeResult.NO_ROUTE;
            TreeRules.Purchase buy = TreeRules.buy(route, nodeId, new PlayerTreeState(of(player)));
            if (!buy.ok()) {
                return switch (buy.verdict()) {
                    case NO_ROUTE -> UpgradeResult.NO_ROUTE;
                    case UNKNOWN_NODE -> UpgradeResult.UNKNOWN_NODE;
                    case MAXED -> UpgradeResult.MAXED;
                    case PARENTS -> UpgradeResult.PARENTS;
                    case NO_SP -> UpgradeResult.NO_SP;
                    case OK -> UpgradeResult.OK;
                };
            }
            SkillNode node = route.node(nodeId);
            if (buy.level() == 1 && node.grantsSkill()) SkillGrants.learn(player, node.skill(), true);
            if (buy.level() == 1 && node.hasToggle()) of(player).setPassiveEnabled(route.id(), node.id(), true);       // 1.82.0: a bought passive starts ON and appears in the Passives panel
            resync(player);
            String key = SkillRoute.key(route.id(), node.id());
            NusmpEvents.fire(player, "tree.node", key);
            NusmpEvents.fire(player, "tree.node.level", key + "@" + buy.level());
            return UpgradeResult.OK;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: upgradeNode failed for {} {}", routeId, nodeId, e);
            return UpgradeResult.DISABLED;
        }
    }

    /** Makes a class (the {@code classId} of an unlocked route) the active one; false when refused (not unlocked, cooldown). Choosing the class that is already active is true. */
    public static boolean setActiveClass(ServerPlayer player, String classId) {
        TreeRules.ClassVerdict v = switchClass(player, classId, false);
        return v == TreeRules.ClassVerdict.OK || v == TreeRules.ClassVerdict.SAME;
    }

    /**
     * The class change with its reason. {@code force} (a command) skips the cooldown and does not start one; creative players skip it too. On success the class is set, the change time is
     * stored (not for a forced change), the sync is queued and the quest hook {@code tree.class} fires with the class id. The engine being off answers {@code NOT_UNLOCKED}.
     */
    public static TreeRules.ClassVerdict switchClass(ServerPlayer player, String classId, boolean force) {
        try {
            if (player == null || !TreeConfig.enabled()) return TreeRules.ClassVerdict.NOT_UNLOCKED;
            SkillRoute target = RouteRegistry.byClass(classId);
            IPlayerSkillTree t = of(player);
            long now = player.level().getGameTime();
            TreeRules.ClassVerdict v = TreeRules.classSwitch(new PlayerTreeState(t), target, now, TreeConfig.classSwitchSeconds() * 20L, force || player.isCreative());
            if (v != TreeRules.ClassVerdict.OK) return v;
            t.setActiveClass(target.classId());
            if (!force) t.setClassChangedAt(now);
            resync(player);
            NusmpEvents.fire(player, "tree.class", target.classId());
            return v;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: switchClass failed for {}", classId, e);
            return TreeRules.ClassVerdict.NOT_UNLOCKED;
        }
    }

    /** Queues a sync of the player's whole tree to their client; any number of calls in one tick send one packet at the end of the tick. */
    public static void resync(ServerPlayer player) { TreeNet.markDirty(player); }

    /** Makes the ManasCore skills of the player match the tree: learn the skill of every bought node, forget the tree skills whose node is not bought. Called on login and respawn. */
    public static void reconcileSkills(ServerPlayer player) {
        try {
            if (player != null && TreeConfig.enabled()) SkillGrants.reconcile(player);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: reconcileSkills failed", e);
        }
    }

    // ------------------------------------------------------------------ the administration (commands) and the respec
    /**
     * Resets the nodes of one route (or of every unlocked route when {@code routeIdOrNull} is null), gives the Skill Points back (never more than were really spent) and forgets the skills of
     * the cleared nodes. The routes stay unlocked. Returns what was done.
     */
    public static RespecResult respec(ServerPlayer player, String routeIdOrNull) {
        try {
            IPlayerSkillTree t = of(player);
            PlayerTreeState s = new PlayerTreeState(t);
            List<String> ids = routeIdOrNull == null ? new ArrayList<>(t.unlockedRoutes()) : List.of(routeIdOrNull);
            int cleared = 0, sp = 0;
            for (String id : ids) {
                if (!t.hasRoute(id)) continue;
                TreeRules.Refund f = TreeRules.respec(RouteRegistry.byId(id), id, s);
                cleared += f.cleared();
                sp += f.sp();
            }
            if (cleared > 0) SkillGrants.reconcile(player);
            resync(player);
            return new RespecResult(cleared, sp);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: respec failed", e);
            return new RespecResult(0, 0);
        }
    }

    /**
     * Takes a route away from a player: resets its nodes (the Skill Points come back), forgets their skills and removes the route; when it was the active class, the next owned route's class
     * (or none) takes its place. False when the player did not have it. Quest hooks do not fire.
     */
    public static boolean lockRoute(ServerPlayer player, String routeId) {
        try {
            IPlayerSkillTree t = of(player);
            if (!t.hasRoute(routeId)) return false;
            TreeRules.respec(RouteRegistry.byId(routeId), routeId, new PlayerTreeState(t));
            t.removeRoute(routeId);
            SkillRoute route = RouteRegistry.byId(routeId);
            if (route != null && route.classId().equals(t.activeClass())) {
                t.setActiveClass("");
                String next = TreeRules.healClass(new PlayerTreeState(t), RouteRegistry::byId);
                if (next != null) t.setActiveClass(next);
            }
            SkillGrants.reconcile(player);
            resync(player);
            return true;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: lockRoute failed for {}", routeId, e);
            return false;
        }
    }

    /**
     * Sets a node to an exact level without paying (an administrator's tool): the level is clamped to the node's maximum, the route must be unlocked and the node must exist, the parents are
     * NOT checked, the Skill Points are not touched, and the node's skill is taught at level 1 or forgotten at 0. False when refused.
     */
    public static boolean setNodeLevel(ServerPlayer player, String routeId, String nodeId, int level) {
        try {
            SkillRoute route = RouteRegistry.byId(routeId);
            SkillNode node = route == null ? null : route.node(nodeId);
            IPlayerSkillTree t = of(player);
            if (node == null || !t.hasRoute(routeId)) return false;
            int lv = Math.max(0, Math.min(node.maxLevel(), level));
            t.setNodeLevel(routeId, nodeId, lv);
            if (lv >= 1 && node.hasToggle() && !t.passiveToggles().containsKey(SkillRoute.key(routeId, nodeId))) t.setPassiveEnabled(routeId, nodeId, true);     // 1.82.0
            if (lv >= 1 && node.grantsSkill()) SkillGrants.learn(player, node.skill(), false);
            else if (lv == 0) SkillGrants.reconcile(player);
            resync(player);
            return true;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: setNodeLevel failed for {} {}", routeId, nodeId, e);
            return false;
        }
    }

    /** Login and respawn housekeeping: gives a player who owns routes but has no class the class of the first route the registry knows (a hand-edited save), then reconciles the skills and queues a sync. */
    static void onJoin(ServerPlayer player) {
        try {
            IPlayerSkillTree t = of(player);
            String heal = TreeRules.healClass(new PlayerTreeState(t), RouteRegistry::byId);
            if (heal != null) t.setActiveClass(heal);
            reconcileSkills(player);
            resync(player);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill tree: the join housekeeping failed", e);
        }
    }
}
