package com.newuniverse.nusmp.skilltree.state;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Set;

/**
 * Why: the owner asked for an {@code IPlayerSkillTree} "capability" holding four things: the routes a player has unlocked, their Skill Points, the levels of the nodes they bought, and
 * their active Fairy Tail class. In NeoForge 21.1 the thing that stores such per-player data is a DATA ATTACHMENT (a Forge-style capability is for exposing behaviour such as an inventory
 * or an energy store from a block or item, and is not saved or copied on death by itself), so this interface is the contract of that data and {@link PlayerSkillTree} is its implementation;
 * everything else (the rules, the packets, the screen, the abilities, the shop) talks to the interface, never to a map. A separate record of the systems' own state lives in
 * {@link #extra}, so a system such as the shop can save its numbers without changing this interface. Deliberately NOTHING in here knows about the Black Clover grimoire: the two systems
 * share no field, which is how the isolation rule of the owner is enforced in the data itself.
 */
public interface IPlayerSkillTree {
    /** Ids of the routes the player has unlocked (a book was consumed). Unmodifiable view. */
    Set<String> unlockedRoutes();

    boolean hasRoute(String routeId);

    /** True when the route was newly added. Does not grant skills: use {@code SkillTreeApi.unlockRoute}. */
    boolean addRoute(String routeId);

    boolean removeRoute(String routeId);

    /** Skill Points available to spend. */
    int sp();

    void setSp(int sp);

    /** Adds (or with a negative amount removes) SP; never below 0. */
    void addSp(int amount);

    /** Total SP spent on nodes so far (for refunds and statistics). */
    int spSpent();

    void setSpSpent(int spent);

    /** The level of a node (0 when not bought), by route and node id. */
    int nodeLevel(String routeId, String nodeId);

    void setNodeLevel(String routeId, String nodeId, int level);

    /** Every bought node: key {@code <route>#<node>} to level (levels above 0 only). Unmodifiable view. */
    Map<String, Integer> nodeLevels();

    /** Forgets every node of a route (a respec); returns the number of nodes cleared. */
    int clearRoute(String routeId);

    /** The id of the class the player is now ({@code fire_dragon_slayer}), or an empty string. */
    String activeClass();

    void setActiveClass(String classId);

    /** Game time of the last class change (for the switch cooldown). */
    long classChangedAt();

    void setClassChangedAt(long gameTime);

    /** A live sub-compound for a system's own saved state ({@code market} for the shop, ...); created empty on first use. */
    CompoundTag extra(String system);

    boolean hasExtra(String system);

    CompoundTag toTag();

    /** True when nothing is stored (the attachment then writes nothing to the save). */
    boolean isEmpty();
}
