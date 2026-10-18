package com.newuniverse.nusmp.skilltree.rules;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Why: the owner's mechanic (a book opens a route, Skill Points are spent on its nodes, only the active class casts) has to give the same answer to the server's purchase handler, the admin
 * commands, the self-tests and the client's "can I cast this?" question, and a rule that is wrong lets a modified client buy a node it cannot afford. So every rule is written ONCE, here,
 * as static functions over the pure records ({@link SkillRoute}, {@link SkillNode}) and the small {@link TreeState} surface, with no Minecraft or NeoForge type: it is unit-tested without a game
 * and {@code SkillTreeApi} only adds what needs a server (the attachment, the skills, the packets, the hooks). The functions never throw on bad input (a null route or a node that does not
 * exist is a refusal), because they are called with whatever a packet carried. A purchase changes the state only when every check passed, so a refused purchase is always a no-op.
 */
public final class TreeRules {
    private TreeRules() {}

    /** Why: the screen, the packet handler and the tests all need to know not just "no" but WHICH rule said no; this is the answer to "may this node level be bought?" (the same words as {@code SkillTreeApi.UpgradeResult}, which maps them one to one). */
    public enum Verdict { OK, NO_ROUTE, UNKNOWN_NODE, MAXED, PARENTS, NO_SP }

    /** Why: a refused class change must say whether it was the cooldown, the lock or a no-op, because the player is told differently; the answer to "may the player switch to this class?". */
    public enum ClassVerdict { OK, SAME, NOT_UNLOCKED, COOLDOWN }

    /** Why: the screen draws a node differently in each of these states and must not re-derive them from the numbers (two copies of a rule drift); how a node looks to the player, in the order the screen cares about: the first two are "cannot click", {@link #AVAILABLE} glows. */
    public enum NodeStatus { ROUTE_LOCKED, PARENTS_LOCKED, TOO_EXPENSIVE, AVAILABLE, OWNED, MAXED }

    /** Why: the caller needs the new level (to teach a skill at level 1) and the price paid (for the log and the hook) without asking the state again; the result of {@link #buy}: the verdict, the level the node has afterwards, and the SP that was spent (0 when refused). */
    public record Purchase(Verdict verdict, int level, int price) {
        public boolean ok() { return verdict == Verdict.OK; }
    }

    /** Why: a respec reports what it did so the command and the player can be told the real numbers; the result of {@link #respec}: how many saved nodes were cleared, how many SP came back, and the ids of the nodes the route knew that were cleared. */
    public record Refund(int cleared, int sp, List<String> nodeIds) {}

    // ------------------------------------------------------------------ buying
    /** Every parent of the node is at the level it asks for. */
    public static boolean parentsMet(SkillRoute route, SkillNode node, TreeState s) {
        for (SkillNode.Parent p : node.parents()) if (s.level(route.id(), p.node()) < p.level()) return false;
        return true;
    }

    /** The SP price of the next level of a node that is at {@code level}, or -1 when it is at its maximum. */
    public static int priceOfNext(SkillNode node, int level) {
        return level >= node.maxLevel() ? -1 : node.costOf(level + 1);
    }

    /**
     * May the next level of this node be bought? Checked in the order the owner lists them: the route is unlocked, the node exists, it is not maxed, every parent is at its level, and the
     * SP covers the price of the next level.
     */
    public static Verdict check(SkillRoute route, String nodeId, TreeState s) {
        if (route == null || s == null || !s.hasRoute(route.id())) return Verdict.NO_ROUTE;
        SkillNode node = nodeId == null ? null : route.node(nodeId);
        if (node == null) return Verdict.UNKNOWN_NODE;
        int level = s.level(route.id(), node.id());
        if (level >= node.maxLevel()) return Verdict.MAXED;
        if (!parentsMet(route, node, s)) return Verdict.PARENTS;
        if (s.sp() < node.costOf(level + 1)) return Verdict.NO_SP;
        return Verdict.OK;
    }

    /** The yes/no form of {@link #check}: may the next level of this node be bought right now? */
    public static boolean canBuy(SkillRoute route, String nodeId, TreeState s) { return check(route, nodeId, s) == Verdict.OK; }

    /** Buys one level: checks everything, then spends the price, adds it to the SP spent and raises the level. A refusal leaves the state exactly as it was. */
    public static Purchase buy(SkillRoute route, String nodeId, TreeState s) {
        Verdict v = check(route, nodeId, s);
        if (v != Verdict.OK) return new Purchase(v, route != null && nodeId != null && s != null ? s.level(route.id(), nodeId) : 0, 0);
        SkillNode node = route.node(nodeId);
        int level = s.level(route.id(), node.id());
        int price = node.costOf(level + 1);
        s.setSp(s.sp() - price);
        s.setSpSpent((int) Math.min(Integer.MAX_VALUE, (long) s.spSpent() + price));
        s.setLevel(route.id(), node.id(), level + 1);
        return new Purchase(Verdict.OK, level + 1, price);
    }

    /** What the screen shows for a node: see {@link NodeStatus}. */
    public static NodeStatus status(SkillRoute route, SkillNode node, TreeState s) {
        if (!s.hasRoute(route.id())) return NodeStatus.ROUTE_LOCKED;
        int level = s.level(route.id(), node.id());
        if (level >= node.maxLevel()) return NodeStatus.MAXED;
        if (level >= 1) return NodeStatus.OWNED;
        if (!parentsMet(route, node, s)) return NodeStatus.PARENTS_LOCKED;
        return s.sp() >= node.costOf(1) ? NodeStatus.AVAILABLE : NodeStatus.TOO_EXPENSIVE;
    }

    // ------------------------------------------------------------------ SP
    /**
     * {@code current + delta}, never below 0 and, for a gain, never above {@code max}. A gain never LOWERS a total that is already above {@code max} (the owner may have lowered the cap
     * after a player earned more): it just adds nothing. A loss is only bounded below.
     */
    public static int addSp(int current, int delta, int max) {
        long v = (long) current + delta;
        if (delta >= 0) return (int) Math.max(current, Math.min(Math.max(0, max), v));
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, v));
    }

    // ------------------------------------------------------------------ respec
    /**
     * Clears every node of a route and returns the SP of the levels that were owned: the {@link SkillNode#totalCost} of each node at its level, but never more than the SP the player really
     * spent ({@code spSpent}), so a datapack that made nodes dearer after the purchase (or an admin who set levels by hand) can never mint SP out of a respec. {@code route} may be null for a
     * route the registry no longer knows: its saved nodes are still cleared, and nothing is refunded for them (their price is unknown).
     */
    public static Refund respec(SkillRoute route, String routeId, TreeState s) {
        int sum = 0;
        List<String> ids = new ArrayList<>();
        if (route != null) {
            for (SkillNode n : route.nodes()) {
                int level = s.level(routeId, n.id());
                if (level <= 0) continue;
                sum = (int) Math.min(Integer.MAX_VALUE, (long) sum + n.totalCost(level));
                ids.add(n.id());
            }
        }
        int cleared = s.clearRoute(routeId);
        int refund = Math.min(sum, s.spSpent());
        s.setSp((int) Math.min(Integer.MAX_VALUE, (long) s.sp() + refund));
        s.setSpSpent(s.spSpent() - refund);
        return new Refund(cleared, refund, ids);
    }

    // ------------------------------------------------------------------ class
    /**
     * May the player make the class of {@code target} their active one? {@code now} is the game time, {@code cooldownTicks} the wait between two changes. The first class (none active yet) and a
     * {@code bypass} (creative players, commands) are free; choosing the class already active is {@link ClassVerdict#SAME}.
     */
    public static ClassVerdict classSwitch(TreeState s, SkillRoute target, long now, long cooldownTicks, boolean bypass) {
        if (target == null || !s.hasRoute(target.id())) return ClassVerdict.NOT_UNLOCKED;
        if (target.classId().equals(s.activeClass())) return ClassVerdict.SAME;
        if (s.activeClass().isEmpty() || bypass) return ClassVerdict.OK;
        return cooldownRemaining(s.classChangedAt(), now, cooldownTicks) > 0 ? ClassVerdict.COOLDOWN : ClassVerdict.OK;
    }

    /**
     * Ticks left before the class may change again. 0 when the class was never changed by the player ({@code changedAt <= 0}), when there is no cooldown, when it has run out, or when the game
     * clock reads EARLIER than the stored change (the world time was set back: the stored stamp cannot be trusted, so it never locks the player for the whole gap).
     */
    public static long cooldownRemaining(long changedAt, long now, long cooldownTicks) {
        if (changedAt <= 0 || cooldownTicks <= 0 || now < changedAt) return 0;
        return Math.max(0, cooldownTicks - (now - changedAt));
    }

    /** The class id to set when the player owns routes but has no active class (a hand-edited save, an admin who removed a route), or null when nothing needs doing. */
    public static String healClass(TreeState s, Function<String, SkillRoute> routeById) {
        if (!s.activeClass().isEmpty()) return null;
        for (String id : s.routes()) {
            SkillRoute r = routeById.apply(id);
            if (r != null) return r.classId();
        }
        return null;
    }

    // ------------------------------------------------------------------ the cast gate
    /**
     * The ability hook of the owner: the route is unlocked AND the node is bought (level 1 or more) AND, when {@code requireActiveClass}, the route's class is the player's active class.
     * Creative players pass the class check (they still need the route and the node). {@code route} is the registry's record of {@code routeId}, or null when the registry does not know it
     * (the class cannot be checked, so it is refused whenever the class matters).
     */
    public static boolean canCast(SkillRoute route, TreeState s, String routeId, String nodeId, boolean requireActiveClass, boolean creative) {
        if (s == null || routeId == null || nodeId == null || !s.hasRoute(routeId)) return false;
        if (s.level(routeId, nodeId) < 1) return false;
        if (!requireActiveClass || creative) return true;
        return route != null && route.classId().equals(s.activeClass());
    }
}
