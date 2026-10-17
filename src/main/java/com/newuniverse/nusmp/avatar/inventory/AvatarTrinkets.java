package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the owner asked for Curios slots on the Avatar. The Curios API is not a compile dependency of this mod and cannot be attached to a mob without data and
 * behaviour nobody can check here, so the Avatar gets "a Curios-like extra container of N slots" (the brief allows exactly that): eight trinket slots for rings,
 * charms, belts and the like. They are not a second store. They are a VIEW of the pack that the relic system already keeps for any named mob (the list of stacks
 * under {@link AvatarRelicBridge#PACK} in the mob's persistent data), because that is where {@code Relics.scan} looks for a mob's worn relics, where the sneak-and-use
 * "give a relic to a mob" of 1.50.0 puts them, and what {@code MobRelics} drops when the mob dies. One store means a relic can never be copied or lost between two
 * lists, the old give-and-take-back still works on an Avatar, and a relic placed here by the menu really works. The pack keeps no empty places, so the view
 * compacts: after a reload the stacks sit in the first slots in the order they were stored. The view re-reads the pack when somebody else changed it
 * ({@link #refresh}); every change made through the view is written at once and ends in {@link AvatarRelicBridge#refresh}.
 */
final class AvatarTrinkets {
    private final AvatarEntity avatar;
    private final NonNullList<ItemStack> slots = NonNullList.withSize(InventoryLayout.TRINKETS, ItemStack.EMPTY);
    private int seen = Integer.MIN_VALUE;

    AvatarTrinkets(AvatarEntity avatar) { this.avatar = avatar; }

    /** The hash of the stored pack, to notice somebody else's change. */
    private int hashOfPack() {
        Tag t = avatar.getPersistentData().get(AvatarRelicBridge.PACK);
        return t == null ? 0 : t.hashCode();
    }

    /** Re-reads the pack when it is not what this view last wrote or read (a sneak-give, a take-back, a death drop). Cheap enough to call every tick. */
    void refresh() {
        if (avatar.level().isClientSide) return;
        int h = hashOfPack();
        if (h == seen) return;
        for (int i = 0; i < slots.size(); i++) slots.set(i, ItemStack.EMPTY);
        CompoundTag data = avatar.getPersistentData();
        if (data.contains(AvatarRelicBridge.PACK, Tag.TAG_LIST)) {
            ListTag list = data.getList(AvatarRelicBridge.PACK, Tag.TAG_COMPOUND);
            int k = 0;
            for (int i = 0; i < list.size() && k < slots.size(); i++) {
                Optional<ItemStack> s = ItemStack.parse(avatar.registryAccess(), list.getCompound(i));
                if (s.isPresent() && !s.get().isEmpty()) slots.set(k++, s.get());
            }
        }
        seen = h;
    }

    ItemStack get(int i) { return i >= 0 && i < slots.size() ? slots.get(i) : ItemStack.EMPTY; }

    /** Takes up to {@code count} out of slot {@code i} (the bond to a relic is cut). */
    ItemStack remove(int i, int count) {
        refresh();
        if (i < 0 || i >= slots.size() || count <= 0) return ItemStack.EMPTY;
        ItemStack cur = slots.get(i);
        if (cur.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = cur.split(count);
        if (cur.isEmpty()) slots.set(i, ItemStack.EMPTY);
        AvatarRelicBridge.onLeave(avatar, out);
        write();
        return out;
    }

    /** Takes the whole stack out of slot {@code i}. */
    ItemStack take(int i) {
        refresh();
        if (i < 0 || i >= slots.size()) return ItemStack.EMPTY;
        ItemStack cur = slots.get(i);
        slots.set(i, ItemStack.EMPTY);
        AvatarRelicBridge.onLeave(avatar, cur);
        if (!cur.isEmpty()) write();
        return cur;
    }

    /** Puts a stack into slot {@code i}, replacing what is there (the caller has taken that out first). A relic that needs attunement is attuned to the Avatar. */
    void set(int i, ItemStack stack) {
        refresh();
        if (i < 0 || i >= slots.size()) return;
        ItemStack s = stack == null ? ItemStack.EMPTY : stack;
        ItemStack old = slots.get(i);
        if (old != s) AvatarRelicBridge.onLeave(avatar, old);
        slots.set(i, s);
        AvatarRelicBridge.onEnter(avatar, s);
        write();
    }

    /** Every stack in the view, for the death drop and the "is it empty" check (not copies). */
    List<ItemStack> all() {
        refresh();
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : slots) if (!s.isEmpty()) out.add(s);
        return out;
    }

    /** Writes the view back as the relic system's pack (no empty places), fixes the marker and the cache. */
    private void write() {
        if (avatar.level().isClientSide) return;
        ListTag list = new ListTag();
        for (ItemStack s : slots) if (!s.isEmpty()) list.add(s.save(avatar.registryAccess()));
        CompoundTag data = avatar.getPersistentData();
        if (list.isEmpty()) data.remove(AvatarRelicBridge.PACK); else data.put(AvatarRelicBridge.PACK, list);
        seen = hashOfPack();
        AvatarRelicBridge.refresh(avatar);
    }
}
