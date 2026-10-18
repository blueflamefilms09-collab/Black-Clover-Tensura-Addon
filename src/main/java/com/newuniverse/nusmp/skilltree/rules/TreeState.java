package com.newuniverse.nusmp.skilltree.rules;

import java.util.Collection;

/**
 * Why: the rules of the Skill Tree (what a purchase costs, who may buy, what a respec returns, when a class may change, who may cast) must be provable without starting a game, but the player's
 * real tree, {@code IPlayerSkillTree}, is stored as NBT and so cannot be created outside Minecraft. This tiny interface is the whole surface the rules touch: the routes a player owns, the node levels,
 * the Skill Points and the active class. The real tree is adapted to it by {@code PlayerTreeState}; the unit tests and the in-game self-test use {@link SimpleTreeState}. Keeping the rules on this
 * interface (and not on NBT or on the attachment) is what lets one set of rules serve the server's purchase, the command line, the self-tests and the client's "can I cast this" question, with
 * the same answer everywhere. No Minecraft types, on purpose.
 */
public interface TreeState {
    /** Ids of the routes the player owns, in the order they were unlocked. */
    Collection<String> routes();

    boolean hasRoute(String routeId);

    /** The level of a node (0 when not bought). */
    int level(String routeId, String nodeId);

    /** Sets a node level; a level of 0 or less forgets the node. */
    void setLevel(String routeId, String nodeId, int level);

    /** Forgets every node of a route, including nodes the registry no longer knows; returns how many were cleared. */
    int clearRoute(String routeId);

    /** Skill Points available to spend. */
    int sp();

    void setSp(int sp);

    /** Skill Points spent on nodes so far (bounds what a respec may give back). */
    int spSpent();

    void setSpSpent(int spent);

    /** The class id the player is now, or an empty string. */
    String activeClass();

    /** Game time of the last class change, or 0 when the class was never changed by the player. */
    long classChangedAt();
}
