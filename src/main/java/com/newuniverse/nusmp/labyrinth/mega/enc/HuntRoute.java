package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.List;

/**
 * Why: the Dungeon Master used to send a hunting monster straight at the nearest player: one call to the navigation with the player's position, once a second.
 * That works in an open field and fails in a maze, which is what this dungeon is: the pathfinder looks only as far as the monster's follow range (16 to 35 blocks)
 * and, when it cannot reach the goal, walks to the explored point nearest to it, which in a floor of walls and doorways is a dead end against the wall nearest the
 * player. The monsters appeared 24 to 48 blocks away, walked to that wall and stood there, so an encounter that was spawned correctly never arrived, and
 * the floor filled up with idle monsters. The dungeon already knows its own shape (the rooms and the doorways between them), so a hunter follows it: while it is
 * not in the player's room (or the next one) it walks to the next room of the shortest way, and only in the last stretch straight at the player. Pure.
 */
public final class HuntRoute {
    private HuntRoute() {}

    /** Within this many blocks of the next room's waypoint the monster is "there" and heads for the one after. */
    public static final double ARRIVED = 3.5;

    /**
     * The waypoint a hunter standing in {@code mobRoom} at (mx, mz) should walk to in order to reach a player in {@code preyRoom}, or null when it should walk straight
     * at the player (the same room, an unknown room, no route).
     */
    public static PatrolGraph.Node next(PatrolGraph g, int mobRoom, int preyRoom, double mx, double mz) {
        if (g == null || mobRoom == preyRoom || !g.has(mobRoom) || !g.has(preyRoom)) return null;
        List<Integer> route = g.route(mobRoom, preyRoom);
        if (route.size() < 2) return null;                         // no way along the doorways: the straight walk is all there is
        PatrolGraph.Node next = g.node(route.get(1));
        if (route.size() > 2 && next != null && Math.hypot(next.x() - mx, next.z() - mz) < ARRIVED) next = g.node(route.get(2));
        return next;
    }
}
