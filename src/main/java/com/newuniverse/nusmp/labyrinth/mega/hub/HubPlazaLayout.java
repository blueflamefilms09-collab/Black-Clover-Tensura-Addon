package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

/**
 * Why: the plaza of Floor 0 is a Structure Template ({@code data/nusmp/structure/hub/plaza.nbt}) written by {@code tools/labyrinth/gen_mega_hub.py}
 * and stamped by {@link HubPlaza}; the template, the Java that places it, the protection box that surrounds it and the self-tests must all agree on
 * the same numbers: how big the template is, which of its rows is the paving, and therefore where its corner lands in the world so that its home
 * gate stands exactly on {@link HubLayout#HOME_GATE_X} / {@code Y} / {@code Z}. This class is that one place. It is pure (no Minecraft import) and
 * derived from {@link HubLayout}, so a landmark that moves in the layout moves the plaza with it; the Python generator READS this file and
 * {@code HubLayout.java} (it parses the integer constants) instead of keeping a second copy, and {@code HubPlazaLayoutTest} opens the generated
 * template to prove that its size and its gate cells are the ones computed here. The template is 33 x 33 with the paving (the top of the ground)
 * on row {@link #FLOOR_ROW}, so the walking surface is {@code SURFACE_Y} (the same convention as the terrain generator: the grass block is at
 * {@code SURFACE_Y - 1}) and the rows below it only seal the ground.
 */
public final class HubPlazaLayout {
    private HubPlazaLayout() {}

    /** The template's path under {@code data/nusmp/structure/}, without the extension. */
    public static final String TEMPLATE = "hub/plaza";
    /**
     * Size of the template in blocks. X and Z are odd so the plaza has a middle block on {@link HubLayout#PLAZA_X} / {@code Z}. It is 20 high (the paving on
     * row 2, so air is cleared up to 17 blocks over the walking surface) because the terrain generator may have grown a tree on the square before the
     * stamp, and the tallest oak canopy must not be left floating above a trunk the stamp removed.
     */
    public static final int SIZE_X = 33, SIZE_Y = 20, SIZE_Z = 33;
    /** The template row that holds the paving: world y {@code SURFACE_Y - 1}, so players stand on {@code SURFACE_Y}. */
    public static final int FLOOR_ROW = 2;
    /** Bumped whenever the template changes in a way an old world should be told about ({@code /multiverse mega hub status} shows both numbers; nothing re-stamps by itself). */
    public static final int BUILD_VERSION = 1;

    /** The gate plane is 3 wide (centre +-1 on x) and 4 high: from the standing level up to one above the recorded centre. */
    public static final int GATE_HALF_WIDTH = 1, GATE_BELOW = 2, GATE_ABOVE = 1;

    /** World x of the template's west edge (local x 0). */
    public static int originX() { return HubLayout.PLAZA_X - SIZE_X / 2; }

    /** World y of the template's bottom row (local y 0). */
    public static int originY() { return HubLayout.SURFACE_Y - 1 - FLOOR_ROW; }

    /** World z of the template's north edge (local z 0). */
    public static int originZ() { return HubLayout.PLAZA_Z - SIZE_Z / 2; }

    public static int maxX() { return originX() + SIZE_X - 1; }

    public static int maxY() { return originY() + SIZE_Y - 1; }

    public static int maxZ() { return originZ() + SIZE_Z - 1; }

    /** Is the world cell inside the template's box (the box {@link HubProtect} guards)? */
    public static boolean inBox(int x, int y, int z) {
        return x >= originX() && x <= maxX() && y >= originY() && y <= maxY() && z >= originZ() && z <= maxZ();
    }

    /** Template-local x, y, z of the centre of the home gate plane. */
    public static int gateLocalX() { return HubLayout.HOME_GATE_X - originX(); }

    public static int gateLocalY() { return HubLayout.HOME_GATE_Y - originY(); }

    public static int gateLocalZ() { return HubLayout.HOME_GATE_Z - originZ(); }

    /** Lowest world y of the home gate plane: the standing level, so a walking player enters it. */
    public static int gateBottomY() { return HubLayout.HOME_GATE_Y - GATE_BELOW; }

    /** Highest world y of the home gate plane. */
    public static int gateTopY() { return HubLayout.HOME_GATE_Y + GATE_ABOVE; }

    /** Is the world cell part of the home gate plane (3 wide, 4 high, one block deep)? */
    public static boolean isHomeGateCell(int x, int y, int z) {
        return z == HubLayout.HOME_GATE_Z && Math.abs(x - HubLayout.HOME_GATE_X) <= GATE_HALF_WIDTH && y >= gateBottomY() && y <= gateTopY();
    }

    /** The chunk coordinate range the template covers (inclusive): what {@link HubPlaza} loads before it stamps. */
    public static int minChunkX() { return originX() >> 4; }

    public static int maxChunkX() { return maxX() >> 4; }

    public static int minChunkZ() { return originZ() >> 4; }

    public static int maxChunkZ() { return maxZ() >> 4; }
}
