package com.newuniverse.nusmp.labyrinth.loot.backpack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: the set-down backpack is a container block, and building it on the vanilla randomizable container gives it, for free and correctly, everything a chest has that matters here:
 * the lock and custom name, hoppers and comparators reading it, a loot table (so a structure can place a backpack that is full of loot), and the save format. Three things are
 * the backpack's own. THE SIZE FOLLOWS THE TIER, which is the block state's {@code tier} property, so the state is the single source of truth and a world edit that changes the
 * state resizes the pack ({@link #resize}); an upgrade grows the list and keeps every item, and a list can never shrink (the pure {@link BackpackTier#upgraded}). THE CONTENTS RIDE
 * THE ITEM: {@code collectImplicitComponents} writes the container component and the tier component and {@code applyImplicitComponents} reads them, which is exactly how the loot
 * table's {@code copy_components} and the pick-up code carry a pack with everything in it. THE PLACED FLAG remembers that a PLAYER set this one down, which is the only kind the
 * sneak pick-up will take (a backpack a structure generated is not a prop to walk off with). A backpack cannot hold a backpack. The block entity has no ticker.
 */
public class BackpackBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items;
    private boolean placedByPlayer;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(BackpackRegistry.BLOCK_ENTITY.get(), pos, state);
        this.items = NonNullList.withSize(tierOf(state).slots(), ItemStack.EMPTY);
    }

    private static BackpackTier tierOf(BlockState state) {
        return state.getBlock() instanceof BackpackBlock ? BackpackTier.of(state.getValue(BackpackBlock.TIER)) : BackpackTier.LEATHER;
    }

    /** The tier of this backpack: its block state's. */
    public BackpackTier tier() { return tierOf(getBlockState()); }

    public boolean placedByPlayer() { return placedByPlayer; }

    public void setPlacedByPlayer(boolean placed) {
        if (this.placedByPlayer != placed) {
            this.placedByPlayer = placed;
            setChanged();
        }
    }

    /** Grows (or, never, shrinks) the item list to the size of a tier, keeping every item. Returns true when it changed. */
    public boolean resize(BackpackTier tier) {
        if (tier.slots() <= items.size()) return false;
        NonNullList<ItemStack> bigger = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) bigger.set(i, items.get(i));
        items = bigger;
        setChanged();
        return true;
    }

    // ---------------------------------------------------------------- the container
    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        NonNullList<ItemStack> fixed = NonNullList.withSize(Math.max(list.size(), tier().slots()), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) fixed.set(i, list.get(i));
        items = fixed;
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.nusmp.backpack"); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new BackpackMenu(id, inv, this, BackpackTier.fitting(items.size()), -1); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem().canFitInsideContainerItems(); }

    // ---------------------------------------------------------------- saving
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        BackpackTier tier = tierOf(getBlockState());
        int saved = tag.contains("Slots") ? tag.getInt("Slots") : 0;
        items = NonNullList.withSize(Math.max(tier.slots(), Math.min(BackpackTier.MAX_SLOTS, saved)), ItemStack.EMPTY);
        placedByPlayer = tag.getBoolean("PlacedByPlayer");
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Slots", items.size());
        tag.putBoolean("PlacedByPlayer", placedByPlayer);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, registries);
    }

    // ---------------------------------------------------------------- the item form
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(BackpackRegistry.TIER.get(), tier().ordinal());
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }
}
