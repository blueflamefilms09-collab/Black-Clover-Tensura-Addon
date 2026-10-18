package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.function.BiPredicate;

/**
 * Why: the owner's two rules for a Unique Skill Line are "only the EQUIPPED line does anything" and "a passive also needs its switch ON", and a rule that is written in every one of 51 executors is a
 * rule that is eventually forgotten in one of them (and a forgotten gate here means a passive of an unequipped line quietly keeps working). So the rules are written ONCE, in this pure class, and
 * every path into the engine goes through it: the passive dispatcher asks {@link #passiveLevel} before it runs any passive or attribute executor, the cast service asks {@link #activeLevel}
 * before it runs any art or stance. An executor therefore cannot be reached for a node that is not allowed to act, and a test of this class is a test of the whole rule.
 * <p>
 * "Equipped" is the existing single-active-class rule of the Skill Tree ({@code activeClass}): the player must own the route AND the route's class must be the active class. Unlike the classic
 * routes' cast gate (which lets a creative player and the {@code requireActiveClass=false} setting skip the class check) a line has no bypass: a creative player holding a hundred lines would
 * otherwise run a hundred lines' passives at once, which is exactly what "one equipped line" exists to prevent. Pure (the player is seen through {@link TreeState} and one predicate), so it is
 * tested without a game.
 */
public final class LineGate {
    private LineGate() {}

    /** True when the player owns the route and its class is the one they have equipped. */
    public static boolean equipped(TreeState s, SkillRoute route) {
        return s != null && route != null && !route.classId().isEmpty() && s.hasRoute(route.id()) && route.classId().equals(s.activeClass());
    }

    /**
     * The level at which a passive or attribute node counts RIGHT NOW, or 0: the node must be a line node with a switch, bought, of the equipped line, and its switch must be on.
     *
     * @param toggleOn answers "is the switch of (route, node) on"; the real tree's {@code isPassiveEnabled}
     */
    public static int passiveLevel(TreeState s, BiPredicate<String, String> toggleOn, SkillRoute route, SkillNode node) {
        if (route == null || node == null || !node.hasToggle()) return 0;
        if (!equipped(s, route)) return 0;
        int level = s.level(route.id(), node.id());
        if (level < 1) return 0;
        return toggleOn.test(route.id(), node.id()) ? level : 0;
    }

    /** The level at which an art or a stance may be used RIGHT NOW, or 0: an active line node, bought, of the equipped line. */
    public static int activeLevel(TreeState s, SkillRoute route, SkillNode node) {
        if (route == null || node == null || !node.isLineNode() || node.nodeType() != SkillNode.NodeType.ACTIVE) return 0;
        if (!equipped(s, route)) return 0;
        return Math.max(0, s.level(route.id(), node.id()));
    }
}
