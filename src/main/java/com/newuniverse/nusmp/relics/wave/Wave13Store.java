package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Wave 13: a {@link Wave13Rules.Bag} (and a few strings) kept in a stack's custom data, in the sub-compound "nusmp_w13", so a relic carries its own state. */
final class Wave13Store {
    private Wave13Store() {}

    static final String ROOT = "nusmp_w13";

    static Wave13Rules.Bag bag(ItemStack stack) {
        return new Wave13Rules.Bag() {
            @Override
            public long get(String key) {
                CustomData d = stack.get(DataComponents.CUSTOM_DATA);
                if (d == null) return 0L;
                CompoundTag t = d.copyTag();
                return t.contains(ROOT) ? t.getCompound(ROOT).getLong(key) : 0L;
            }

            @Override
            public void put(String key, long value) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
                    CompoundTag w = t.getCompound(ROOT);
                    w.putLong(key, value);
                    t.put(ROOT, w);
                });
            }
        };
    }

    static String getStr(ItemStack stack, String key) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return "";
        CompoundTag t = d.copyTag();
        return t.contains(ROOT) ? t.getCompound(ROOT).getString(key) : "";
    }

    static void putStr(ItemStack stack, String key, String value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
            CompoundTag w = t.getCompound(ROOT);
            w.putString(key, value == null ? "" : value);
            t.put(ROOT, w);
        });
    }

    /** The stack an ability runs on: the one the engine hands over, else the player's main hand. */
    static ItemStack stack(ActCtx c) {
        ItemStack s = c.stack();
        return s == null || s.isEmpty() ? c.player().getMainHandItem() : s;
    }
}
