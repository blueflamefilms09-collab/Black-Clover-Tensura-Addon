package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Wave 9: a few counters kept in a stack's custom data (the sub-compound "nusmp_w9"), so a weapon carries its own history (the Null Blade's kills). */
final class Wave9Store {
    private Wave9Store() {}

    static final String ROOT = "nusmp_w9";

    static int get(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty()) return 0;
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return 0;
        CompoundTag t = d.copyTag();
        return t.contains(ROOT) ? t.getCompound(ROOT).getInt(key) : 0;
    }

    static void put(ItemStack stack, String key, int value) {
        if (stack == null || stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
            CompoundTag w = t.getCompound(ROOT);
            w.putInt(key, value);
            t.put(ROOT, w);
        });
    }
}
