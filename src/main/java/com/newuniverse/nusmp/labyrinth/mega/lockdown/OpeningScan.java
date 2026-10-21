package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: a room can only be locked where it has a WALL. The rooms of the Mega-Dungeon are boxes with two wall layers, the doorways are the gaps in them, and the
 * lockdown fills a gap with a barrier; but not every room is a box (a cavern has no wall at all, a ruined hall may have lost half of one), and a "doorway" scan of
 * a cavern would find a hundred cells and seal a hole the size of a hillside, or worse, a hole with a way round. So the passable cells found in the room's inner
 * wall layer are grouped into openings (cells that touch edge to edge, in the wall's plane) and judged: there must be at least one opening, none may be bigger than a
 * doorway of the clearance rule could be, there may not be more of them than a room has sides to put them in, and the total is bounded. A room that fails is an
 * "open plan" room: it does not lock (the ambush director still watches it). The geometry is plain integers, so the judgement is a unit test. Pure.
 */
public final class OpeningScan {
    private OpeningScan() {}

    /** A passable block in the wall layer. */
    public record Cell(int x, int y, int z) {}

    /** The limits of a judgement. The defaults hold four doors of 7 by 7 (the clearance rule) with room to spare. */
    public record Limits(int maxTotal, int maxPerOpening, int maxOpenings) {
        public static final Limits DEFAULT = new Limits(320, 96, 12);
    }

    /** The judgement: whether to lock, why not otherwise, and the numbers. */
    public record Result(boolean ok, String why, int cells, int openings, int biggest) {}

    /**
     * Groups the cells into openings: two cells are one opening when they share a face (6-neighbourhood, so a doorway stays one piece and an arch with a gap
     * above its keystone is still one).
     */
    public static List<List<Cell>> openings(Iterable<Cell> cells) {
        Set<Cell> left = new HashSet<>();
        for (Cell c : cells) left.add(c);
        List<List<Cell>> out = new ArrayList<>();
        int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!left.isEmpty()) {
            Cell seed = left.iterator().next();
            List<Cell> group = new ArrayList<>();
            ArrayDeque<Cell> queue = new ArrayDeque<>();
            queue.add(seed);
            left.remove(seed);
            while (!queue.isEmpty()) {
                Cell c = queue.poll();
                group.add(c);
                for (int[] n : d) {
                    Cell next = new Cell(c.x() + n[0], c.y() + n[1], c.z() + n[2]);
                    if (left.remove(next)) queue.add(next);
                }
            }
            out.add(group);
        }
        out.sort((a, b) -> {
            Cell x = a.stream().min(OpeningScan::order).orElse(new Cell(0, 0, 0)), y = b.stream().min(OpeningScan::order).orElse(new Cell(0, 0, 0));
            return order(x, y);
        });
        return out;
    }

    private static int order(Cell a, Cell b) {
        if (a.x() != b.x()) return Integer.compare(a.x(), b.x());
        if (a.z() != b.z()) return Integer.compare(a.z(), b.z());
        return Integer.compare(a.y(), b.y());
    }

    /** Judges a set of passable wall cells. */
    public static Result judge(Iterable<Cell> cells, Limits limits) {
        List<List<Cell>> groups = openings(cells);
        int total = 0, biggest = 0;
        for (List<Cell> g : groups) {
            total += g.size();
            biggest = Math.max(biggest, g.size());
        }
        if (groups.isEmpty()) return new Result(false, "no opening in the wall: there is nothing to close", 0, 0, 0);
        if (groups.size() > limits.maxOpenings()) return new Result(false, groups.size() + " openings: an open-plan room", total, groups.size(), biggest);
        if (biggest > limits.maxPerOpening()) return new Result(false, "an opening of " + biggest + " cells: not a doorway", total, groups.size(), biggest);
        if (total > limits.maxTotal()) return new Result(false, total + " cells to close: an open-plan room", total, groups.size(), biggest);
        return new Result(true, "ok", total, groups.size(), biggest);
    }
}
