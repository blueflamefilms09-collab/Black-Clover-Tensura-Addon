package com.newuniverse.nusmp.dungeon.nexus;

import net.minecraft.core.BlockPos;

/** A read-only look at one core cell's Nexus: which zone, how far along, where it stands. Built by {@link NexusZones}; never null fields. */
public final class NexusInfo {
    public final int cellX, cellZ;
    public final NexusZone zone;
    public final NexusState state;
    public final Footprint footprint;
    public final int floorY, topY;
    public final boolean centreRelicTaken, sealBroken;

    public NexusInfo(int cellX, int cellZ, NexusZone zone, NexusState state, Footprint footprint, int floorY, int topY, boolean centreRelicTaken, boolean sealBroken) {
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.zone = zone;
        this.state = state;
        this.footprint = footprint;
        this.floorY = floorY;
        this.topY = topY;
        this.centreRelicTaken = centreRelicTaken;
        this.sealBroken = sealBroken;
    }

    public long key() { return NexusCells.key(cellX, cellZ); }

    /** The floor block in the middle of the Nexus. */
    public BlockPos centre() { return new BlockPos(footprint.centreX(), floorY, footprint.centreZ()); }

    /** Is this block inside the volume of the Nexus (its footprint grown by `margin`, a little below the floor to a little above the roof)? */
    public boolean contains(int x, int y, int z, int margin) {
        return y >= floorY - ZonePlan.BELOW && y <= topY + ZonePlan.ABOVE && footprint.contains(x, z, margin);
    }

    public boolean contains(BlockPos p, int margin) { return contains(p.getX(), p.getY(), p.getZ(), margin); }
}
