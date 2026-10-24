package com.newuniverse.nusmp.companion.bond;

import com.newuniverse.nusmp.companion.CompanionRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Why: what a companion remembers about her side of the bond, in one value saved with her: the stance (GUARD / AGGRESSIVE / PASSIVE), the nine-slot pouch of what the player gave her to carry, and the switch that
 * lets her use it on the player. It is saved as one compound named {@code bond} inside the entity's own save data, so it never collides with her other fields. Rapport is NOT here: it belongs to the player (it
 * survives her dying) and is kept in their persistent data ({@link CompanionData}). Not thread safe: server thread only.
 */
public final class PersonState {
    public static final String KEY = "bond";

    public Stance stance = Stance.GUARD;
    /** May she use something from the pouch on the player when their health is low? (The player can switch it off.) */
    public boolean useIt = true;
    public final NonNullList<ItemStack> pouch = NonNullList.withSize(CompanionRules.POUCH_SLOTS, ItemStack.EMPTY);
    /** Game time of her last use of the pouch (not saved: a reload simply allows one at once). */
    public long lastUse = Long.MIN_VALUE / 2;

    public void save(CompoundTag entityTag, HolderLookup.Provider access) {
        CompoundTag t = new CompoundTag();
        t.putString("stance", stance.name());
        t.putBoolean("useIt", useIt);
        ContainerHelper.saveAllItems(t, pouch, access);
        entityTag.put(KEY, t);
    }

    public void load(CompoundTag entityTag, HolderLookup.Provider access) {
        CompoundTag t = entityTag.getCompound(KEY);
        stance = Stance.byName(t.getString("stance"));
        useIt = !t.contains("useIt") || t.getBoolean("useIt");
        for (int i = 0; i < pouch.size(); i++) pouch.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(t, pouch, access);
    }

    /** Is every slot empty? */
    public boolean isEmpty() {
        for (ItemStack s : pouch) if (!s.isEmpty()) return false;
        return true;
    }

    /** How many slots hold something. */
    public int filled() {
        int n = 0;
        for (ItemStack s : pouch) if (!s.isEmpty()) n++;
        return n;
    }

    /**
     * Moves as much of {@code from} as fits into the pouch (onto matching stacks first, then into empty slots); {@code from} is reduced by what moved. Returns how many items moved.
     */
    public int put(ItemStack from) {
        if (from.isEmpty()) return 0;
        int moved = 0;
        for (int i = 0; i < pouch.size() && !from.isEmpty(); i++) {
            ItemStack s = pouch.get(i);
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, from)) continue;
            int room = s.getMaxStackSize() - s.getCount();
            int n = Math.min(room, from.getCount());
            if (n <= 0) continue;
            s.grow(n);
            from.shrink(n);
            moved += n;
        }
        for (int i = 0; i < pouch.size() && !from.isEmpty(); i++) {
            if (!pouch.get(i).isEmpty()) continue;
            int n = Math.min(from.getMaxStackSize(), from.getCount());
            pouch.set(i, from.copyWithCount(n));
            from.shrink(n);
            moved += n;
        }
        return moved;
    }

    /** Takes the stack out of a slot (the slot becomes empty). Empty when the slot is empty or out of range. */
    public ItemStack take(int slot) {
        if (slot < 0 || slot >= pouch.size()) return ItemStack.EMPTY;
        ItemStack s = pouch.get(slot);
        pouch.set(slot, ItemStack.EMPTY);
        return s;
    }
}
