package com.newuniverse.nusmp.arc.arena;

import java.util.Map;

/**
 * Why: every character has its own arena in ONE dimension, so each needs a place of its own that never moves and never touches another: a grid of sites,
 * {@link #SPACING} blocks apart (the widest arena is 81 blocks across, so two are never closer than 175 blocks, more than the usual view distance of ten chunks
 * leaves loaded round a player), sixteen to a row. A character is given a SLOT the first time its arena is needed and keeps it for ever (the slot is saved
 * with the world), so a roster that grows or is re-ordered later cannot move an arena that players have already built in. This pure class does the arithmetic
 * (slot to centre, position to slot, the next free slot); the saved map lives in {@code ArcArenaSave}.
 */
public final class ArenaGrid {
    private ArenaGrid() {}

    /** Blocks between the centres of two neighbouring sites. */
    public static final int SPACING = 256;
    /** Sites in a row; the grid grows south after sixteen. */
    public static final int PER_ROW = 16;
    /** The most sites (sixteen rows of sixteen): far more characters than the arcs will ever have. */
    public static final int MAX_SLOTS = PER_ROW * 16;

    /** World x of the centre of a slot. */
    public static int centreX(int slot) { return (slot % PER_ROW) * SPACING; }

    /** World z of the centre of a slot. */
    public static int centreZ(int slot) { return (slot / PER_ROW) * SPACING; }

    /** The slot whose site holds this position (within half a spacing of its centre in both axes), or -1 for a place no site owns. */
    public static int slotAt(double x, double z) {
        int col = (int) Math.round(x / SPACING), row = (int) Math.round(z / SPACING);
        if (col < 0 || col >= PER_ROW || row < 0 || row >= MAX_SLOTS / PER_ROW) return -1;
        int slot = row * PER_ROW + col;
        double half = SPACING / 2.0;
        return Math.abs(x - centreX(slot)) <= half && Math.abs(z - centreZ(slot)) <= half ? slot : -1;
    }

    /** The lowest slot no character holds, or -1 when the grid is full. */
    public static int nextFree(Map<String, Integer> taken) {
        boolean[] used = new boolean[MAX_SLOTS];
        for (int s : taken.values()) if (s >= 0 && s < MAX_SLOTS) used[s] = true;
        for (int s = 0; s < MAX_SLOTS; s++) if (!used[s]) return s;
        return -1;
    }
}
