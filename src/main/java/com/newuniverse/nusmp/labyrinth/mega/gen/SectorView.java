package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;

import java.util.List;

/**
 * Why: the plan audit proves a plan is sound before it is placed, but what every other system trusts is the SAVED sector, and a saved sector can be wrong
 * in ways the plan was not (a marker that did not survive the conversion, a landmark copied from the wrong piece, a sector half-written when the server
 * stopped, one an old version made). {@code /multiverse mega audit} therefore checks the saved data itself, and to keep that check testable without a game it
 * reads this plain-value copy of a sector instead of the game's {@code Sector}: the generator makes one from the saved sector, the tests make one from a
 * plan (through {@link SectorFill#view}) and break it on purpose.
 *
 * @param state       the {@code Sector.State} name
 * @param region      the sector's exclusive territory
 * @param rooms       the saved rooms (door caps are never saved)
 * @param edges       pairs of room ids joined by a doorway
 * @param markers     every saved marker (kind and world cell)
 * @param relicCount  how many relics the Crucible offers
 * @param pedestals   the saved pedestal blocks
 * @param arrival     the Landing's arrival cell, or null
 * @param seal        the jigsaw cell of the Way Onward's rubble seal, or null
 * @param sealFacing  the way the seal points (the direction the next sector attaches), or null
 * @param nextId      the sector generated off this one's Way Onward, or -1
 * @param floorModel  1.58.0: is the sector one floor with stairwells (the floor model)? Then it has no Landing: the arrival stairwell takes its place
 * @param bossFloor   1.58.0: does the floor have the boss chain? An ordinary floor of the floor model has none, and no Way Onward seal
 * @param stairBack   1.58.0: the stair exit of the arrival stairwell (it leads back), or null
 * @param stairForward 1.58.0: the stair exit of the forward stairwell (it leads on), or null
 * @param stairForwardPad 1.58.0: the cell where a party stands when it comes back from the next floor, or null
 */
public record SectorView(int id, String state, Box region, List<SectorFill.RoomOut> rooms, List<int[]> edges, List<MarkerView> markers, int relicCount,
                         List<Vec> pedestals, Vec arrival, Vec guardianSpawn, Vec gatekeeper, Vec vaultCenter, Vec seal, Facing sealFacing, int nextId,
                         boolean floorModel, boolean bossFloor, Vec stairBack, Vec stairForward, Vec stairForwardPad) {
    /** A sector of the old model (no stairwells): the 1.55.0 shape of the view, which the tests of that time build by hand. */
    public SectorView(int id, String state, Box region, List<SectorFill.RoomOut> rooms, List<int[]> edges, List<MarkerView> markers, int relicCount,
                      List<Vec> pedestals, Vec arrival, Vec guardianSpawn, Vec gatekeeper, Vec vaultCenter, Vec seal, Facing sealFacing, int nextId) {
        this(id, state, region, rooms, edges, markers, relicCount, pedestals, arrival, guardianSpawn, gatekeeper, vaultCenter, seal, sealFacing, nextId,
                false, true, null, null, null);
    }

    /** A saved marker as far as the audit cares: what it is and where. */
    public record MarkerView(MarkerKind kind, Vec cell) {}
}
