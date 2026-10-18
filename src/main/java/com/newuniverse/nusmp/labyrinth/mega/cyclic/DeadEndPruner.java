package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the loop pass cures a dead end only if one of its open doorways has a partner within reach. Some do not (a closet with one door, a stub at the edge of
 * the region, a branch of corridors that ends at a wall), and the owner's cap ("at most 8 % dead ends") has to hold anyway. The honest cure for those is to
 * not have them: take the dangling room away. This planner decides which rooms go, and only ever takes a room that is
 * <ul>
 * <li>a leaf at that moment (degree 1: it has no other doorway in use, so nothing hangs off it and removing it cuts no path);</li>
 * <li>not a terminal and not required ({@link Roles#PROTECTED}: the entrance, the stairs, the boss chain, the Safe Zones and the rooms the recipe needs);</li>
 * <li>not one that would take the dungeon under {@code minRooms}.</li>
 * </ul>
 * Because a removed leaf is by definition not on any walk between two other rooms, the walk to the stairs, to the boss and to every safe room survives: the
 * connected graph stays connected and every terminal keeps its distance from every other (a property test pins this). Which leaf goes first is the category
 * that is most over its target share ({@link CategoryBalance}), then the one farthest from the entrance, then the lowest id, so the choice is deterministic
 * and tends to improve the balance instead of skewing it. Pure.
 */
public final class DeadEndPruner {
    private DeadEndPruner() {}

    /**
     * The ids to remove, leaf first. Removing them in that order never leaves a room with a missing neighbour that was still needed: each was a leaf when its turn came.
     *
     * @param entrance the entrance room (distances are measured from it); a negative id means room 0
     */
    public static List<Integer> plan(RoomGraph g, LoopConfig cfg, int minRooms, int entrance) {
        int n = g.size();
        List<Integer> out = new ArrayList<>();
        if (!cfg.prune() || n == 0) return out;
        int[] dist = g.distances(entrance < 0 ? 0 : entrance);
        int[] deg = new int[n];
        for (int i = 0; i < n; i++) deg[i] = g.degree(i);
        boolean[] gone = new boolean[n];
        int[] counts = g.categoryCounts();
        int left = n;
        while (left > Math.max(1, minRooms)) {
            int deadCount = 0, pick = -1;
            double pickSurplus = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                if (gone[i] || deg[i] > 1 || g.hasRole(i, cfg.terminalMask())) continue;
                deadCount++;
                if (g.hasRole(i, cfg.protectedMask())) continue;
                double surplus = cfg.balanceWeight() > 0 ? -CategoryBalance.want(g.category(i), counts) : 0.0;
                boolean better;
                if (pick < 0) better = true;
                else if (surplus != pickSurplus) better = surplus > pickSurplus;
                else if (dist[i] != dist[pick]) better = dist[i] > dist[pick];
                else better = false;
                if (better) {
                    pick = i;
                    pickSurplus = surplus;
                }
            }
            if (deadCount <= cfg.deadEndTarget() * left + 1e-9 || pick < 0) break;
            gone[pick] = true;
            out.add(pick);
            counts[g.category(pick).ordinal()]--;
            left--;
            for (int v : g.neighbours(pick)) if (!gone[v]) deg[v]--;
        }
        return out;
    }
}
