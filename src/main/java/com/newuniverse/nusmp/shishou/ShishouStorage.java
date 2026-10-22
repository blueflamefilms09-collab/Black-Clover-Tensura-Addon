package com.newuniverse.nusmp.shishou;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Why: Stomach (Ibukuro), the internal storage of the sword. It is ONE container per world, held by the saved data ({@link ShishouData}), not by the item stack: the contents survive the wielder's death, a
 * lost item and a recovery, and a copy of the stack cannot carry a second copy of the items (no duplication by copying). {@link #accepts} applies the pure rules ({@link ShishouStorageRules}: no living
 * thing, no other container, no bundle, not the sword itself); only the first {@link ShishouConfig#storageRows()} rows are usable (the rest stay safe and come back if the setting is raised). Used through
 * vanilla's chest menu (no custom screen) and through the item-handler capability of the stack (hoppers, other mods). Server thread only.
 */
public final class ShishouStorage extends SimpleContainer {
    private Runnable onChange = () -> {};

    public ShishouStorage() { super(ShishouRules.STORAGE_SLOTS); }

    /** The saved data that must be marked dirty when a slot changes. */
    public void setListener(Runnable r) { onChange = r == null ? () -> {} : r; }

    /** The pure rule applied to a stack. */
    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean living = stack.getItem() instanceof SpawnEggItem || stack.getItem() instanceof MobBucketItem || stack.has(DataComponents.ENTITY_DATA) || stack.has(DataComponents.BUCKET_ENTITY_DATA);
        return ShishouStorageRules.accepts(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), false, stack.has(DataComponents.CONTAINER), stack.has(DataComponents.BUNDLE_CONTENTS), living);
    }

    /** How many slots are usable now. */
    public static int usable() { return ShishouConfig.storageRows() * 9; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < usable() && accepts(stack); }

    @Override
    public boolean stillValid(Player player) { return ShishouService.mayUseStorage(player); }

    @Override
    public void setChanged() {
        super.setChanged();
        onChange.run();
    }

    /**
     * Puts as much of the stack as fits into the usable slots (merging with equal stacks first), and returns what is left. Never inserts what {@link #accepts} refuses. The caller's stack is not changed; a
     * copy is returned.
     */
    public ItemStack insert(ItemStack in) {
        if (!accepts(in)) return in;
        ItemStack rest = in.copy();
        int n = usable();
        for (int i = 0; i < n && !rest.isEmpty(); i++) {
            ItemStack s = getItem(i);
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, rest)) continue;
            int room = Math.min(getMaxStackSize(), s.getMaxStackSize()) - s.getCount();
            if (room <= 0) continue;
            int move = Math.min(room, rest.getCount());
            s.grow(move);
            rest.shrink(move);
        }
        for (int i = 0; i < n && !rest.isEmpty(); i++) {
            if (!getItem(i).isEmpty()) continue;
            int move = Math.min(rest.getCount(), Math.min(getMaxStackSize(), rest.getMaxStackSize()));
            setItem(i, rest.split(move));
        }
        setChanged();
        return rest;
    }

    public int usedSlots() {
        int c = 0;
        for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) c++;
        return c;
    }

    // ---------------------------------------------------------------- saving
    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) list.set(i, getItem(i));
        ContainerHelper.saveAllItems(tag, list, provider);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, provider);
        for (int i = 0; i < getContainerSize(); i++) super.setItem(i, list.get(i));
    }
}
