package com.newuniverse.nusmp.labyrinth.loot.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Why: the window of a backpack in the hand needs a {@link net.minecraft.world.Container} that is backed by the STACK, not by a block: it loads the stack's container component
 * when it is made and writes the whole thing back every time a slot changes and when the window closes, so nothing is ever only in memory. It holds the stack OBJECT, not a copy,
 * so if the player moves the backpack to another slot while the window is open (a swap with the off hand, a shift-click in another screen) the writes still land on the right
 * stack. The write is a plain component set, so the stack's tier and name are untouched. A backpack cannot be put into it. Server side (the client's copy is a plain container the
 * menu fills from the server's slot sync).
 */
public final class HeldBackpack extends SimpleContainer {
    private final ItemStack backpack;
    private boolean ready;

    public HeldBackpack(ItemStack backpack, BackpackTier tier) {
        super(tier.slots());
        this.backpack = backpack;
        ItemContainerContents c = backpack.get(DataComponents.CONTAINER);
        if (c != null) {
            NonNullList<ItemStack> list = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
            c.copyInto(list);
            for (int i = 0; i < list.size(); i++) getItems().set(i, list.get(i));
        }
        ready = true;
    }

    /** The stack this container is the inside of. */
    public ItemStack stack() { return backpack; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem().canFitInsideContainerItems(); }

    @Override
    public void setChanged() {
        super.setChanged();
        save();
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        save();
    }

    /** Writes the slots into the stack's container component. */
    void save() {
        if (!ready || backpack.isEmpty()) return;
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
    }
}
