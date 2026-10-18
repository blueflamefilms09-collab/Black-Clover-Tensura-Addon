package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.skilltree.rules.TreeState;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;

import java.util.Collection;

/**
 * Why: the rules in {@code TreeRules} are written against the tiny pure {@link TreeState} so they can be tested without NBT, but the real data lives in a player's
 * {@link IPlayerSkillTree}. This adapter is the one-to-one bridge between them (no logic, no copy: every call goes straight to the live tree, so a purchase made through the rules changes the
 * player's saved data), and it is public so any system that wants to ask a rule about a real player (the screen asking {@code TreeRules.status}, the shop asking before a sale) uses the same
 * rules instead of writing its own.
 */
public final class PlayerTreeState implements TreeState {
    private final IPlayerSkillTree tree;

    public PlayerTreeState(IPlayerSkillTree tree) { this.tree = tree; }

    @Override public Collection<String> routes() { return tree.unlockedRoutes(); }

    @Override public boolean hasRoute(String routeId) { return tree.hasRoute(routeId); }

    @Override public int level(String routeId, String nodeId) { return tree.nodeLevel(routeId, nodeId); }

    @Override public void setLevel(String routeId, String nodeId, int level) { tree.setNodeLevel(routeId, nodeId, level); }

    @Override public int clearRoute(String routeId) { return tree.clearRoute(routeId); }

    @Override public int sp() { return tree.sp(); }

    @Override public void setSp(int sp) { tree.setSp(sp); }

    @Override public int spSpent() { return tree.spSpent(); }

    @Override public void setSpSpent(int spent) { tree.setSpSpent(spent); }

    @Override public String activeClass() { return tree.activeClass(); }

    @Override public long classChangedAt() { return tree.classChangedAt(); }
}
