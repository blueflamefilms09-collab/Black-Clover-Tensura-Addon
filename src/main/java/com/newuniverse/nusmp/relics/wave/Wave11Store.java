package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Wave 11: small numbers a blade keeps in its own stack (the sub-compound "nusmp_w11"): the drum of Bianca, the daily roll, the heated blade. */
final class Wave11Store {
    private Wave11Store() {}

    static final String ROOT = "nusmp_w11";

    static boolean has(ItemStack stack, String key) {
        CustomData d = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getCompound(ROOT).contains(key);
    }

    static long get(ItemStack stack, String key) {
        CustomData d = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0L : d.copyTag().getCompound(ROOT).getLong(key);
    }

    static void put(ItemStack stack, String key, long value) {
        if (stack == null || stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
            CompoundTag w = t.getCompound(ROOT);
            w.putLong(key, value);
            t.put(ROOT, w);
        });
    }

    /** The stack an ability runs on: the one the engine hands over, else the player's main hand. */
    static ItemStack stack(ActCtx c) {
        ItemStack s = c.stack();
        return s == null || s.isEmpty() ? c.player().getMainHandItem() : s;
    }
}
