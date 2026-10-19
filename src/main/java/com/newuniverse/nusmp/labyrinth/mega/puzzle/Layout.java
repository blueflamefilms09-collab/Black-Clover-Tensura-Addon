package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: what the world has to build for a puzzle, in the one vocabulary every puzzle shares, so the placement code is written once. A layout is a list of GROUPS of cells; a cell is one
 * block (or one column of blocks) that either is clicked (a PILLAR stands in the air cell above the floor), is a floor tile that can be clicked or stepped on (a PLATE replaces the floor block),
 * only shows something (a DISPLAY) or is a chest (a CHEST). The groups are drawn as rows, grids, rings, scatters or explicit points; the world puts the groups side by side round the room
 * anchor, checks that every cell stands on a free floor and, if the room cannot hold the layout, simply does not arm the puzzle (a puzzle that cannot be placed never blocks a room).
 * Cell numbers run through the groups in order, which is how a rule names a cell. Pure.
 */
public record Layout(List<Group> groups) {
    public enum Role { PILLAR, PLATE, DISPLAY, CHEST }

    public enum Shape { ROW, GRID, RING, SCATTER, POINTS }

    /**
     * One group. {@code cols x rows} is the grid size (a row has rows 1, a ring and a scatter use only {@code count}), {@code height} the column height of every cell, {@code gap} the spacing in
     * blocks (a tile grid 1, pillars 2), {@code palette} the name of the block table in {@link Palettes}; {@code px/pz} are the explicit points of a POINTS group (units of one gap).
     */
    public record Group(String name, Role role, Shape shape, int count, int cols, int rows, int height, int gap, String palette, int[] px, int[] pz) {
        public Group {
            if (px == null) px = new int[0];
            if (pz == null) pz = new int[0];
        }

        public static Group row(String name, Role role, int count, String palette) {
            return new Group(name, role, Shape.ROW, count, count, 1, 1, role == Role.PLATE || role == Role.DISPLAY ? 1 : 2, palette, null, null);
        }

        public static Group grid(String name, Role role, int cols, int rows, String palette) {
            return new Group(name, role, Shape.GRID, cols * rows, cols, rows, 1, role == Role.PILLAR || role == Role.CHEST ? 2 : 1, palette, null, null);
        }

        public static Group ring(String name, Role role, int count, String palette) {
            return new Group(name, role, Shape.RING, count, count, 1, 1, 2, palette, null, null);
        }

        public static Group scatter(String name, Role role, int count, String palette) {
            return new Group(name, role, Shape.SCATTER, count, count, 1, 1, 2, palette, null, null);
        }

        public static Group points(String name, Role role, int[] px, int[] pz, String palette) {
            return new Group(name, role, Shape.POINTS, px.length, 0, 0, 1, 2, palette, px, pz);
        }

        public Group tall(int h) { return new Group(name, role, shape, count, cols, rows, h, gap, palette, px, pz); }

        public Group gap(int g) { return new Group(name, role, shape, count, cols, rows, height, g, palette, px, pz); }
    }

    public static Layout of(Group... g) { return new Layout(List.of(g)); }

    public int total() {
        int n = 0;
        for (Group g : groups) n += g.count();
        return n;
    }

    /** The number of the first cell of group {@code gi}. */
    public int start(int gi) {
        int n = 0;
        for (int i = 0; i < gi; i++) n += groups.get(i).count();
        return n;
    }

    public int groupIndexOf(int cell) {
        int n = 0;
        for (int i = 0; i < groups.size(); i++) {
            n += groups.get(i).count();
            if (cell < n) return i;
        }
        return -1;
    }

    public Group groupOf(int cell) {
        int i = groupIndexOf(cell);
        return i < 0 ? null : groups.get(i);
    }

    /** The cells of group {@code gi} as numbers. */
    public List<Integer> cellsOf(int gi) {
        List<Integer> out = new ArrayList<>();
        int s = start(gi);
        for (int i = 0; i < groups.get(gi).count(); i++) out.add(s + i);
        return out;
    }
}
