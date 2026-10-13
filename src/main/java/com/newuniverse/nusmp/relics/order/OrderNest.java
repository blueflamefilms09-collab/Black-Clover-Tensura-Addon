package com.newuniverse.nusmp.relics.order;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * The stacks inside a stack, one level deep: a shulker box (and any item that keeps its contents in the container component) and a bundle. Reading is
 * cheap (a component lookup); writing rebuilds the component, because the contents of a component are immutable. This is the only place that touches
 * those two component types.
 */
final class OrderNest {
    private OrderNest() {}

    /** The stacks in a container component, by slot (EMPTY included), or an empty list. */
    static List<ItemStack> container(ItemStack parent) {
        if (parent == null || parent.isEmpty()) return List.of();
        ItemContainerContents c = parent.get(DataComponents.CONTAINER);
        if (c == null) return List.of();
        int n = c.getSlots();
        List<ItemStack> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(c.getStackInSlot(i));
        return out;
    }

    /** The stacks in a bundle, in the bundle's own order, or an empty list. */
    static List<ItemStack> bundle(ItemStack parent) {
        if (parent == null || parent.isEmpty()) return List.of();
        BundleContents b = parent.get(DataComponents.BUNDLE_CONTENTS);
        if (b == null) return List.of();
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : b.items()) out.add(s);
        return out;
    }

    /** How many non-empty stacks are inside (a shulker's and a bundle's contents together). */
    static int count(ItemStack parent) {
        int n = 0;
        for (ItemStack s : container(parent)) if (!s.isEmpty()) n++;
        for (ItemStack s : bundle(parent)) if (!s.isEmpty()) n++;
        return n;
    }

    /** Replaces the stack at a slot of the container component (EMPTY removes it). False if the slot does not exist. */
    static boolean replaceInContainer(ItemStack parent, int index, ItemStack with) {
        ItemContainerContents c = parent.get(DataComponents.CONTAINER);
        if (c == null || index < 0 || index >= c.getSlots()) return false;
        NonNullList<ItemStack> list = NonNullList.withSize(c.getSlots(), ItemStack.EMPTY);
        c.copyInto(list);
        list.set(index, with == null ? ItemStack.EMPTY : with);
        parent.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return true;
    }

    /** Replaces the stack at a position of the bundle (EMPTY removes it). False if the position does not exist. */
    static boolean replaceInBundle(ItemStack parent, int index, ItemStack with) {
        List<ItemStack> list = bundle(parent);
        if (index < 0 || index >= list.size()) return false;
        List<ItemStack> next = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = i == index ? with : list.get(i);
            if (s != null && !s.isEmpty()) next.add(s.copy());
        }
        BundleContents.Mutable m = new BundleContents.Mutable(BundleContents.EMPTY);
        for (int i = next.size() - 1; i >= 0; i--) m.tryInsert(next.get(i));       // a bundle keeps its newest first: put the last one in first
        parent.set(DataComponents.BUNDLE_CONTENTS, m.toImmutable());
        return true;
    }
}
