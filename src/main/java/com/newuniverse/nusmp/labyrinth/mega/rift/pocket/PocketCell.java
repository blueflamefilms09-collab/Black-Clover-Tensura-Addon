package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Why: where the cell of a pocket slot is in the Mega-Dungeon's dimension and how a plan's local coordinates become world ones, in one small class so the builder, the runtime, the events and
 * the self-test cannot disagree. The cells are the old pocket field's ({@link RiftRules#pocketX}): slot {@code n} is a 192 x 192 square centred on {@code (POCKET_X0 + n * 192, POCKET_Z)}, millions of
 * blocks from any floor of the dungeon; an exclusive tomb of 168 blocks across fits in one with room for its walls; a plan's local x and z are offsets from the centre, y is absolute.
 */
public final class PocketCell {
    private PocketCell() {}

    /** The half width of what the cell is allowed to hold (a tomb is at most 168 + 24 across; the cell is 192). */
    public static final int HALF = 96;
    /** The lowest and highest y a tomb touches (the void under the inverted rooms' rivers, the ceiling of the tallest room, with a margin). */
    public static final int Y_MIN = PocketPlan.Y0 - 8, Y_MAX = PocketPlan.Y0 + 26;

    public static int centreX(int slot) { return RiftRules.pocketX(slot); }

    public static int centreZ() { return RiftRules.POCKET_Z; }

    /** The block of a plan's local point. */
    public static BlockPos world(int slot, int lx, int y, int lz) { return new BlockPos(centreX(slot) + lx, y, centreZ() + lz); }

    /** The local x of a world x in this slot's cell. */
    public static double localX(int slot, double x) { return x - centreX(slot); }

    public static double localZ(double z) { return z - centreZ(); }

    /** The whole box the cell's tomb may touch: what a clear sweeps and what an entity sweep covers. */
    public static AABB bounds(int slot) { return new AABB(centreX(slot) - HALF, Y_MIN, centreZ() - HALF, centreX(slot) + HALF, Y_MAX, centreZ() + HALF); }

    /** The slot a world point lies in, or -1 (the same rule as every pocket: {@link RiftRules#slotAt}). */
    public static int slotAt(double x, double z) { return RiftRules.slotAt(x, z); }

    /** The chunk coordinates (min x, min z, max x, max z) a cell covers. */
    public static int[] chunks(int slot) {
        int x0 = centreX(slot) - HALF, x1 = centreX(slot) + HALF - 1, z0 = centreZ() - HALF, z1 = centreZ() + HALF - 1;
        return new int[]{x0 >> 4, z0 >> 4, x1 >> 4, z1 >> 4};
    }
}
