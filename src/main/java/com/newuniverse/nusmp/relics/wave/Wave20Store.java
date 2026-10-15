package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Why: wave 20's relics keep small counters (the chosen element, the day's allowances) on the stack itself, in the sub-compound "nusmp_w20" of its custom
 * data, and per-player timers in the player's persistent data; this class is the one place that reads and writes the first, behind
 * {@link Wave20Rules.Bag}.
 */
final class Wave20Store {
    private Wave20Store() {}

    static final String ROOT = "nusmp_w20";

    static Wave20Rules.Bag bag(ItemStack stack) {
        return new Wave20Rules.Bag() {
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

    /** A {@link Wave20Rules.Bag} over a player's persistent data (timers and daily allowances that belong to the wielder, not to one stack). */
    static Wave20Rules.Bag playerBag(CompoundTag data) {
        return new Wave20Rules.Bag() {
            @Override
            public long get(String key) { return data.getLong("nusmp_w20_" + key); }

            @Override
            public void put(String key, long value) { data.putLong("nusmp_w20_" + key, value); }
        };
    }

    /** The stack an ability runs on: the one the engine hands over, else the player's main hand. */
    static ItemStack stack(ActCtx c) {
        ItemStack s = c.stack();
        return s == null || s.isEmpty() ? c.player().getMainHandItem() : s;
    }
}
