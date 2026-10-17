package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Why: "give the Avatar a fully functional equipment inventory (mainhand, offhand, armor, Curios slots)" and "it picks up items off the ground" need one place that
 * holds all of an Avatar's things and answers for them: this is that container. It is a window of {@link InventoryLayout#SIZE} indices onto three stores: the
 * entity's own six equipment slots (so combat, armour values, the renderer and the vanilla save all see the same stacks), the eight trinket slots (a view of the
 * relic pack, see {@link AvatarTrinkets}) and eighteen pockets for loot, kept in the Avatar's own NBT under {@code Pockets}. It is the container of the server-side
 * inventory menu, the target of the looting goal, and the source of ammunition for the bow; its equipment changes keep the relic marker and bonds right
 * ({@link AvatarRelicBridge}). Server side; the client menu uses a plain container that the menu packets fill.
 */
public final class AvatarInventory implements Container {
    private final AvatarEntity avatar;
    private final NonNullList<ItemStack> pockets = NonNullList.withSize(InventoryLayout.POCKETS, ItemStack.EMPTY);
    private final AvatarTrinkets trinkets;

    public AvatarInventory(AvatarEntity avatar) {
        this.avatar = avatar;
        this.trinkets = new AvatarTrinkets(avatar);
    }

    // ---------------------------------------------------------------- the index map
    /** The equipment slot of an index 0..5, or null for a trinket or a pocket. */
    public static EquipmentSlot slotOf(int index) {
        return switch (index) {
            case InventoryLayout.MAIN -> EquipmentSlot.MAINHAND;
            case InventoryLayout.OFF -> EquipmentSlot.OFFHAND;
            case InventoryLayout.HEAD -> EquipmentSlot.HEAD;
            case InventoryLayout.CHEST -> EquipmentSlot.CHEST;
            case InventoryLayout.LEGS -> EquipmentSlot.LEGS;
            case InventoryLayout.FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /** The index of an armour slot, or -1. */
    public static int indexOf(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> InventoryLayout.MAIN;
            case OFFHAND -> InventoryLayout.OFF;
            case HEAD -> InventoryLayout.HEAD;
            case CHEST -> InventoryLayout.CHEST;
            case LEGS -> InventoryLayout.LEGS;
            case FEET -> InventoryLayout.FEET;
            default -> -1;
        };
    }

    public AvatarEntity avatar() { return avatar; }

    // ---------------------------------------------------------------- Container
    @Override public int getContainerSize() { return InventoryLayout.SIZE; }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < InventoryLayout.SIZE; i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        EquipmentSlot s = slotOf(index);
        if (s != null) return avatar.getItemBySlot(s);
        if (InventoryLayout.isTrinket(index)) return trinkets.get(index - InventoryLayout.TRINKET_START);
        if (InventoryLayout.isPocket(index)) return pockets.get(index - InventoryLayout.POCKET_START);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (count <= 0) return ItemStack.EMPTY;
        EquipmentSlot s = slotOf(index);
        if (s != null) {
            ItemStack cur = avatar.getItemBySlot(s);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = cur.split(count);
            if (cur.isEmpty()) avatar.setItemSlot(s, ItemStack.EMPTY);
            AvatarRelicBridge.onLeave(avatar, out);
            setChanged();
            return out;
        }
        if (InventoryLayout.isTrinket(index)) return trinkets.remove(index - InventoryLayout.TRINKET_START, count);
        if (InventoryLayout.isPocket(index)) return ContainerHelper.removeItem(pockets, index - InventoryLayout.POCKET_START, count);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        EquipmentSlot s = slotOf(index);
        if (s != null) {
            ItemStack cur = avatar.getItemBySlot(s);
            avatar.setItemSlot(s, ItemStack.EMPTY);
            AvatarRelicBridge.onLeave(avatar, cur);
            return cur;
        }
        if (InventoryLayout.isTrinket(index)) return trinkets.take(index - InventoryLayout.TRINKET_START);
        if (InventoryLayout.isPocket(index)) return ContainerHelper.takeItem(pockets, index - InventoryLayout.POCKET_START);
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack s = stack == null ? ItemStack.EMPTY : stack;
        EquipmentSlot slot = slotOf(index);
        if (slot != null) {
            ItemStack old = avatar.getItemBySlot(slot);
            if (old != s) AvatarRelicBridge.onLeave(avatar, old);
            avatar.setItemSlot(slot, s);
            AvatarRelicBridge.onEnter(avatar, s);
            setChanged();
            return;
        }
        if (InventoryLayout.isTrinket(index)) { trinkets.set(index - InventoryLayout.TRINKET_START, s); return; }
        if (InventoryLayout.isPocket(index)) {
            if (s.getCount() > getMaxStackSize()) s.setCount(getMaxStackSize());
            pockets.set(index - InventoryLayout.POCKET_START, s);
        }
    }

    /** Called after an equipment change: the relic marker and cache follow what the Avatar wears. */
    @Override
    public void setChanged() { AvatarRelicBridge.refresh(avatar); }

    @Override
    public boolean stillValid(Player player) { return avatar.isAlive() && player.distanceToSqr(avatar) <= 64.0; }

    @Override
    public void clearContent() {
        for (int i = 0; i < InventoryLayout.SIZE; i++) removeItemNoUpdate(i);
        setChanged();
    }

    /** The trinket view re-reads the relic pack if somebody else changed it; the menu calls this every tick while it is open. */
    public void refresh() { trinkets.refresh(); }

    // ---------------------------------------------------------------- pockets (the looting goal's door)
    /** How many pockets are empty. */
    public int freePockets() {
        int n = 0;
        for (ItemStack s : pockets) if (s.isEmpty()) n++;
        return n;
    }

    /** Is there room for (all or part of) this stack: an empty pocket, or a stack of the same item with space? */
    public boolean hasRoomFor(ItemStack stack) {
        if (stack.isEmpty()) return true;
        for (ItemStack s : pockets) {
            if (s.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) return true;
        }
        return false;
    }

    /** Puts the stack into the pockets (topping up equal stacks first). Returns what did not fit; EMPTY when everything did. The argument is not changed. */
    public ItemStack addToPockets(ItemStack stack) {
        ItemStack rest = stack.copy();
        if (rest.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < pockets.size() && !rest.isEmpty(); i++) {
            ItemStack s = pockets.get(i);
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, rest) || s.getCount() >= s.getMaxStackSize()) continue;
            int move = Math.min(rest.getCount(), s.getMaxStackSize() - s.getCount());
            s.grow(move);
            rest.shrink(move);
        }
        for (int i = 0; i < pockets.size() && !rest.isEmpty(); i++) {
            if (!pockets.get(i).isEmpty()) continue;
            int move = Math.min(rest.getCount(), rest.getMaxStackSize());
            pockets.set(i, rest.copyWithCount(move));
            rest.shrink(move);
        }
        return rest;
    }

    /** Counts the items in the pockets that match. */
    public int countInPockets(Predicate<ItemStack> match) {
        int n = 0;
        for (ItemStack s : pockets) if (!s.isEmpty() && match.test(s)) n += s.getCount();
        return n;
    }

    /** The pocket stacks (live, not copies) as a read-only list, for the hand-over and the drop. */
    public List<ItemStack> pocketStacks() { return java.util.Collections.unmodifiableList(pockets); }

    /** Takes one item that matches out of the pockets (or null when none is there); the pocket shrinks. */
    public ItemStack takeOneFromPockets(Predicate<ItemStack> match) {
        for (int i = 0; i < pockets.size(); i++) {
            ItemStack s = pockets.get(i);
            if (s.isEmpty() || !match.test(s)) continue;
            ItemStack one = s.copyWithCount(1);
            s.shrink(1);
            if (s.isEmpty()) pockets.set(i, ItemStack.EMPTY);
            return one;
        }
        return null;
    }

    /** Takes the stack out of one pocket (the whole stack); EMPTY when it is empty. */
    public ItemStack takePocket(int pocket) {
        if (pocket < 0 || pocket >= pockets.size()) return ItemStack.EMPTY;
        return ContainerHelper.takeItem(pockets, pocket);
    }

    // ---------------------------------------------------------------- everything the Avatar carries
    /** Every non-empty stack the Avatar carries (equipment, trinkets, pockets): live stacks, for the death drop. */
    public List<ItemStack> everything() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < InventoryLayout.SIZE; i++) {
            ItemStack s = getItem(i);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    // ---------------------------------------------------------------- save and load (the pockets only: the entity saves its hands and armour itself, the pack is persistent data)
    public ListTag savePockets(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < pockets.size(); i++) {
            ItemStack s = pockets.get(i);
            if (s.isEmpty()) continue;
            CompoundTag c = new CompoundTag();
            c.putByte("Slot", (byte) i);
            c.put("Item", s.save(provider));
            list.add(c);
        }
        return list;
    }

    public void loadPockets(ListTag list, HolderLookup.Provider provider) {
        for (int i = 0; i < pockets.size(); i++) pockets.set(i, ItemStack.EMPTY);
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int slot = c.getByte("Slot") & 255;
            if (slot >= pockets.size() || !c.contains("Item", Tag.TAG_COMPOUND)) continue;
            Optional<ItemStack> s = ItemStack.parse(provider, c.getCompound("Item"));
            s.filter(x -> !x.isEmpty()).ifPresent(x -> pockets.set(slot, x));
        }
    }
}
