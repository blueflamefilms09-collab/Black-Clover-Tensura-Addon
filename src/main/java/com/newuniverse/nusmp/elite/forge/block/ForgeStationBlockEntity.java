package com.newuniverse.nusmp.elite.forge.block;

import com.newuniverse.nusmp.elite.forge.ForgeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: the block entity of a forge station remembers what the station itself has done, not the player (that is the attachment): how many items have been forged at
 * it, the best quality it has produced, and how many times it was upgraded. They are shown by {@code /multiverse elite forge info} when looking at a station and
 * they are carried over when the station is upgraded to the next tier (the old block entity is replaced when the block changes, so {@code StationUpgrade} copies
 * these three numbers across). The station holds no items, so breaking it can never lose one. Tolerant of missing keys.
 */
public class ForgeStationBlockEntity extends BlockEntity {
    private int forged;
    private int best;
    private int upgrades;

    public ForgeStationBlockEntity(BlockPos pos, BlockState state) { super(ForgeRegistry.STATION_BE.get(), pos, state); }

    public int forged() { return forged; }

    public int best() { return best; }

    public int upgrades() { return upgrades; }

    public void recordForge(int qualityLevel) {
        forged++;
        best = Math.max(best, qualityLevel);
        setChanged();
    }

    /** Sets the three numbers (used when a station is upgraded and the block entity is replaced). */
    public void restore(int forged, int best, int upgrades) {
        this.forged = Math.max(0, forged);
        this.best = Math.max(0, Math.min(6, best));
        this.upgrades = Math.max(0, upgrades);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("forged", forged);
        tag.putInt("best", best);
        tag.putInt("upgrades", upgrades);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        forged = tag.contains("forged") ? Math.max(0, tag.getInt("forged")) : 0;
        best = tag.contains("best") ? Math.max(0, Math.min(6, tag.getInt("best"))) : 0;
        upgrades = tag.contains("upgrades") ? Math.max(0, tag.getInt("upgrades")) : 0;
    }
}
