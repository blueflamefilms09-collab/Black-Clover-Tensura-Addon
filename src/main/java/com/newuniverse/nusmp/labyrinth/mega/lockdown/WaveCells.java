package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Why: a locked room's wave has to stand INSIDE the room, on its floor, away from the walls (a monster in a wall layer is in the doorway the barrier is about to fill)
 * and not on top of the party. The room is a box in the saved floor plan, the template's own spawn markers are the places the designer chose, and the rest of the
 * floor is a grid; this picks the cells: markers inside the room first (kept inset from the walls), then a grid over the inside of the floor, all at least {@code minDist}
 * from every player, shuffled by the given random so two waves of the same room do not stand in the same places. The first {@code want} are the cells, the rest the
 * reserve the spawner falls back on when a cell turns out to be unusable in the world. Pure: integers and doubles only.
 */
public final class WaveCells {
    private WaveCells() {}

    /** A cell (the feet row). */
    public record Cell(int x, int y, int z) {}

    /** The grid spacing in blocks. */
    public static final int GRID = 4;
    /** The least distance from the box's edge for a cell: the two wall layers and one block of floor. */
    public static final int WALL_INSET = 3;

    /**
     * @param spots   spawn markers as {x, y, z}
     * @param players eye or feet positions as {x, y, z}
     */
    public static List<Cell> pick(int minX, int minZ, int maxX, int maxZ, int standY, List<int[]> spots, List<double[]> players, double minDist, Random rnd) {
        return pick(minX, minZ, maxX, maxZ, standY, spots, players, minDist, rnd, List.of());
    }

    /** The blocks in front of a doorway that a wave keeps clear when it can: the apron of the 7 x 7 clearance (3 blocks behind the two wall layers) and one more. */
    public static final int DOOR_CLEAR = 4;

    /**
     * As {@link #pick(int, int, int, int, int, List, List, double, Random)}, for a room whose doorways are 7 x 7 (the sector was cut): {@code doorways} are the {x, z} of the barrier cells.
     * A cell within {@link #DOOR_CLEAR} blocks of a doorway stands in the apron in front of a barrier, where the party arrives and a wave would be on top of the way in and the way out,
     * so those cells go to the END of the list (the reserve), in the same order among themselves; the rest is exactly what the plain {@code pick} gives. With no doorways it IS the plain pick.
     */
    public static List<Cell> pick(int minX, int minZ, int maxX, int maxZ, int standY, List<int[]> spots, List<double[]> players, double minDist, Random rnd, List<int[]> doorways) {
        List<Cell> marked = new ArrayList<>(), grid = new ArrayList<>();
        for (int[] s : spots) {
            if (inside(minX, minZ, maxX, maxZ, s[0], s[2]) && farFromAll(players, s[0] + 0.5, s[2] + 0.5, minDist)) marked.add(new Cell(s[0], s[1], s[2]));
        }
        int x0 = minX + WALL_INSET, x1 = maxX - WALL_INSET, z0 = minZ + WALL_INSET, z1 = maxZ - WALL_INSET;
        if (x1 >= x0 && z1 >= z0) {
            int offX = (x1 - x0) % GRID / 2, offZ = (z1 - z0) % GRID / 2;
            for (int x = x0 + offX; x <= x1; x += GRID) {
                for (int z = z0 + offZ; z <= z1; z += GRID) {
                    if (farFromAll(players, x + 0.5, z + 0.5, minDist)) grid.add(new Cell(x, standY, z));
                }
            }
        }
        Collections.shuffle(marked, rnd);
        Collections.shuffle(grid, rnd);
        List<Cell> out = new ArrayList<>(marked.size() + grid.size());
        if (doorways.isEmpty()) {
            out.addAll(marked);
            out.addAll(grid);
            return out;
        }
        List<Cell> near = new ArrayList<>();
        for (List<Cell> group : List.of(marked, grid)) {
            for (Cell c : group) (nearDoorway(doorways, c.x(), c.z()) ? near : out).add(c);
        }
        out.addAll(near);
        return out;
    }

    /** Is the cell within {@link #DOOR_CLEAR} blocks (either axis) of a doorway cell? */
    public static boolean nearDoorway(List<int[]> doorways, int x, int z) {
        for (int[] d : doorways) if (Math.abs(d[0] - x) <= DOOR_CLEAR && Math.abs(d[1] - z) <= DOOR_CLEAR) return true;
        return false;
    }

    private static boolean inside(int minX, int minZ, int maxX, int maxZ, int x, int z) {
        return x >= minX + WALL_INSET && x <= maxX - WALL_INSET && z >= minZ + WALL_INSET && z <= maxZ - WALL_INSET;
    }

    private static boolean farFromAll(List<double[]> players, double x, double z, double minDist) {
        for (double[] p : players) if (Math.hypot(p[0] - x, p[2] - z) < minDist) return false;
        return true;
    }
}
