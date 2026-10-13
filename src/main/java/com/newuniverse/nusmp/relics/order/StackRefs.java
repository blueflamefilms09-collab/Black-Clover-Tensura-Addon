package com.newuniverse.nusmp.relics.order;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The places a stack can live ({@link StackRef}) and the two things the Order does to one: stamp it and take it away. */
final class StackRefs {
    private StackRefs() {}

    static StackRef container(Container c, int i) {
        return new StackRef() {
            @Override public ItemStack get() { return i < c.getContainerSize() ? c.getItem(i) : ItemStack.EMPTY; }

            @Override public void set(ItemStack s) { if (i < c.getContainerSize()) c.setItem(i, s); }

            @Override public void changed() { c.setChanged(); }
        };
    }

    static StackRef slot(Slot slot) {
        return new StackRef() {
            @Override public ItemStack get() { return slot.getItem(); }

            @Override public void set(ItemStack s) { slot.set(s); }

            @Override public void changed() { slot.setChanged(); }
        };
    }

    /** The stack on the mouse cursor of an open menu. */
    static StackRef carried(AbstractContainerMenu menu) {
        return new StackRef() {
            @Override public ItemStack get() { return menu.getCarried(); }

            @Override public void set(ItemStack s) { menu.setCarried(s); }
        };
    }

    /** A stack handed over as an object (a Curios slot): it can be edited in place and emptied, not replaced. */
    static StackRef raw(ItemStack stack) {
        return new StackRef() {
            @Override public ItemStack get() { return stack; }

            @Override public void set(ItemStack s) { if (s == null || s.isEmpty()) stack.setCount(0); }
        };
    }

    static StackRef equipment(LivingEntity e, EquipmentSlot slot) {
        return new StackRef() {
            @Override public ItemStack get() { return e.getItemBySlot(slot); }

            @Override public void set(ItemStack s) { e.setItemSlot(slot, s == null ? ItemStack.EMPTY : s); }
        };
    }

    static StackRef itemEntity(ItemEntity e) {
        return new StackRef() {
            @Override public ItemStack get() { return e.getItem(); }

            @Override
            public void set(ItemStack s) {
                if (s == null || s.isEmpty()) e.discard();
                else e.setItem(s);
            }
        };
    }

    /** A place inside the stack at 'parent': slot 'index' of its container component, or position 'index' of its bundle. */
    static StackRef nested(StackRef parent, int index, boolean bundle) {
        return new StackRef() {
            @Override
            public ItemStack get() {
                ItemStack p = parent.get();
                List<ItemStack> l = bundle ? OrderNest.bundle(p) : OrderNest.container(p);
                return index >= 0 && index < l.size() ? l.get(index) : ItemStack.EMPTY;
            }

            @Override
            public void set(ItemStack s) {
                ItemStack p = parent.get();
                if (p.isEmpty()) return;
                boolean done = bundle ? OrderNest.replaceInBundle(p, index, s) : OrderNest.replaceInContainer(p, index, s);
                if (done) parent.changed();
            }

            @Override public boolean nested() { return true; }

            @Override public void changed() { parent.changed(); }
        };
    }

    /** The unique relics inside the stack at 'parent' as places, last position first (taking one out of a bundle moves the ones after it). */
    static List<StackRef> insideOf(StackRef parent) {
        ItemStack p = parent.get();
        List<StackRef> out = new ArrayList<>();
        if (p.isEmpty()) return out;
        List<ItemStack> c = OrderNest.container(p);
        for (int i = c.size() - 1; i >= 0; i--) if (OrderRelics.uniqueId(c.get(i)) != null) out.add(nested(parent, i, false));
        List<ItemStack> b = OrderNest.bundle(p);
        for (int i = b.size() - 1; i >= 0; i--) if (OrderRelics.uniqueId(b.get(i)) != null) out.add(nested(parent, i, true));
        return out;
    }

    /** Writes a serial onto the stack at this place. True if the stack there carries it afterwards. */
    static boolean stamp(StackRef ref, UUID serial) {
        ItemStack s = ref.get();
        if (s.isEmpty() || serial == null) return false;
        if (ref.nested()) {
            ItemStack copy = s.copy();
            RelicSerial.set(copy, serial);
            ref.set(copy);
        } else {
            RelicSerial.set(s, serial);
            ref.changed();
        }
        return serial.equals(RelicSerial.get(ref.get()));
    }

    /** Takes the stack away. True if the place is empty afterwards. */
    static boolean clear(StackRef ref) {
        if (ref.get().isEmpty()) return true;
        ref.set(ItemStack.EMPTY);
        ref.changed();
        return ref.get().isEmpty();
    }
}
