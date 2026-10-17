package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

/**
 * Why: the unit test of the Hub's walkability and the in-game self-test {@code macro.model} must ask the SAME question (from the road outside each door to the
 * cell in front of each gate), or one of them could pass while the other is wrong. The start cells, the target cells and the search boxes live here, derived
 * from {@link HubLayout}, and both read them. The start is on the road outside the door, the target is the standing cell next to the gate plane.
 * Pure: no Minecraft import.
 */
public final class WalkRoutes {
    private WalkRoutes() {}

    /**
     * Where the feet are at the start of each route (x, y, z): on the road, outside the building. The Spire's ground gallery reaches 4.5 blocks beyond the nominal wall
     * ({@code SPIRE_DOOR_X} is the nominal wall), so its start is 8 blocks before {@code SPIRE_DOOR_X}; the Chasm's rim door is the rim itself, so its start is 3 blocks before it.
     */
    public static final int[] SPIRE_START = {HubLayout.SPIRE_DOOR_X - 8, HubLayout.SURFACE_Y, HubLayout.SPIRE_DOOR_Z};
    public static final int[] CHASM_START = {HubLayout.CHASM_DOOR_X + 3, HubLayout.SURFACE_Y, HubLayout.CHASM_DOOR_Z};
    /** Where the feet are at the end: the standing cell beside the gate plane (the plane is 4 high and its bottom row is the standing level). */
    public static final int[] HOLY_TARGET = {HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y - 2, HubLayout.HOLY_GATE_Z + 1};
    public static final int[] ABYSS_TARGET = {HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y - 2, HubLayout.ABYSS_GATE_Z + 1};

    /** The search box of the Spire route: the tower plus the road in front of its door, from the foundation to above the crown. */
    public static Walkability.Box spireBox() {
        return new Walkability.Box(HubLayout.SPIRE_X - 46, HubLayout.SPIRE_X + 40, HubLayout.SPIRE_Z - 40, HubLayout.SPIRE_Z + 40, HubLayout.SURFACE_Y - 2, HubLayout.HOLY_GATE_Y + 6);
    }

    /** The search box of the Chasm route: the pit, its rim and the road in front of the rim door, from below the floor to above the rim. */
    public static Walkability.Box chasmBox() {
        return new Walkability.Box(HubLayout.CHASM_X - 52, HubLayout.CHASM_X + 52, HubLayout.CHASM_Z - 52, HubLayout.CHASM_Z + 52, HubLayout.ABYSS_GATE_Y - 6, HubLayout.SURFACE_Y + 4);
    }
}
