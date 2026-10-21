package com.newuniverse.nusmp.labyrinth.loot.backpack;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Why: one menu serves both ways of opening a backpack, a set-down block (its block entity is the container) and a stack in the hand ({@link HeldBackpack} is the container), and
 * for every tier: the number of container rows is the tier's, the slot positions are {@link BackpackLayout}'s, and the player's inventory sits under them like a chest's. The
 * rules that keep it safe: a backpack cannot be put inside a backpack (the slots refuse anything that cannot fit inside a container item); when the window belongs to a stack in
 * the hand, the slot holding that very stack cannot be picked up (it is the window's own backing, and moving it would leave the window writing into nothing); the window closes by
 * itself when the block entity is gone or too far, or the stack is no longer in the player's inventory; shift-click moves a stack between the backpack and the inventory the way a
 * chest does. The client half is built from the packet with an empty container of the right size, which the server then fills by the ordinary slot sync. {@code heldSlot} is -1 for
 * a block, else the player-inventory index of the backpack stack (the selected hotbar slot, or 40 for the off hand).
 */
public class BackpackMenu extends AbstractContainerMenu {
    private final Container container;
    private final BackpackTier tier;
    private final int heldSlot;

    public BackpackMenu(int id, Inventory inv, Container container, BackpackTier tier, int heldSlot) {
        super(BackpackRegistry.MENU.get(), id);
        this.container = container;
        this.tier = tier;
        this.heldSlot = heldSlot;
        checkContainerSize(container, tier.slots());
        container.startOpen(inv.player);
        for (int i = 0; i < tier.slots(); i++) addSlot(new BackpackSlot(container, i, BackpackLayout.slotX(i), BackpackLayout.slotY(i)));
        int invY = BackpackLayout.inventoryY(tier.rows());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(playerSlot(inv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(playerSlot(inv, col, 8 + col * 18, BackpackLayout.hotbarY(tier.rows())));
    }

    /** The client half: an empty container of the tier's size, filled by the server. */
    public static BackpackMenu client(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        BackpackTier tier = BackpackTier.of(buf.readVarInt());
        int held = buf.readVarInt();
        return new BackpackMenu(id, inv, new SimpleContainer(tier.slots()), tier, held);
    }

    private Slot playerSlot(Inventory inv, int index, int x, int y) {
        if (heldSlot >= 0 && index == heldSlot) {
            return new Slot(inv, index, x, y) {
                @Override public boolean mayPickup(Player player) { return false; }

                @Override public boolean mayPlace(ItemStack stack) { return false; }
            };
        }
        return new Slot(inv, index, x, y);
    }

    public BackpackTier tier() { return tier; }

    public int rows() { return tier.rows(); }

    /** The slot count of the backpack part (the rest are the player's). */
    public int backpackSlots() { return tier.slots(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return moved;
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        int size = tier.slots();
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!stack.getItem().canFitInsideContainerItems() || !moveItemStackTo(stack, 0, size, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        if (heldSlot >= 0) {
            if (player.level().isClientSide) return true;
            if (!(container instanceof HeldBackpack held)) return true;
            ItemStack backing = held.stack();
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i) == backing) return true;
            return false;
        }
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** A slot of the backpack: refuses what cannot go inside a container item (a backpack, a shulker box). */
    public static final class BackpackSlot extends Slot {
        public BackpackSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(ItemStack stack) { return stack.getItem().canFitInsideContainerItems(); }
    }
}
