package com.newuniverse.nusmp.labyrinth.mega.cyclic;

/**
 * Why: the cyclic layer reasons about rooms on the 9-block module grid and about which side of a room a doorway is on. It needs the four compass
 * directions and nothing else, in the SAME clockwise order as the generator's {@code Facing} (north, east, south, west; north is -z, east is +x), so the
 * adapter converts with an ordinal and the two never disagree about which way a hallway turns. Kept as its own enum (instead of importing the generator's)
 * so this package stays free of every other package of the mod and can be read, tested and reused on its own.
 */
public enum Dir {
    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

    private static final Dir[] VALUES = values();

    public final int dx, dz;

    Dir(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public Dir opposite() { return VALUES[(ordinal() + 2) & 3]; }

    public Dir cw() { return VALUES[(ordinal() + 1) & 3]; }

    public Dir ccw() { return VALUES[(ordinal() + 3) & 3]; }

    public static Dir byIndex(int i) { return VALUES[i & 3]; }

    public static Dir[] all() { return VALUES.clone(); }
}
