package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Wave 1: a {@link Wave1Rules.Bag} kept in a stack's custom data (the sub-compound "nusmp_w1"), so a relic carries its own state. */
final class Wave1Store {
    private Wave1Store() {}

    static final String ROOT = "nusmp_w1";

    static Wave1Rules.Bag bag(ItemStack stack) {
        return new Wave1Rules.Bag() {
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

    /** The stack an ability runs on: the one the engine hands over, else the player's main hand. */
    static ItemStack stack(ActCtx c) {
        ItemStack s = c.stack();
        return s == null || s.isEmpty() ? c.player().getMainHandItem() : s;
    }
}
