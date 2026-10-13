package com.newuniverse.nusmp.relics.order;

import net.minecraft.world.item.ItemStack;

/**
 * A place where one item stack lives (a container slot, a menu slot, a hand, an item entity, a slot inside a shulker box): the Order reads it, stamps
 * a serial onto it and takes it away through this and nothing else, so the same code serves an inventory, a chest menu, a mob and a bundle.
 */
interface StackRef {
    /** The stack that is there now (EMPTY if none). */
    ItemStack get();

    /** Puts a stack there; EMPTY clears the place. */
    void set(ItemStack stack);

    /** Is this a place inside another stack (its stack cannot be edited in place: it is rebuilt through {@link #set})? */
    default boolean nested() { return false; }

    /** Called after the stack there was edited in place, so the owner of the place saves and syncs it. */
    default void changed() {}
}
