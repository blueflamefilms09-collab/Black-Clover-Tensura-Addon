package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Corpse Mask keeps up to five faces of the dead on the mask itself (item custom data), so they stay with the item when it is dropped or
 * handed on; this class is the only place that reads and writes them.
 */
final class Wave25Mask {
    private Wave25Mask() {}

    private static final String FACES = "w25_faces", LAST = "w25_last", SEL = "w25_sel";

    private static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    static List<String> faces(ItemStack s) {
        List<String> out = new ArrayList<>();
        ListTag l = tag(s).getList(FACES, Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    static long lastStored(ItemStack s) {
        CompoundTag t = tag(s);
        return t.contains(LAST) ? t.getLong(LAST) : Long.MIN_VALUE / 2;
    }

    static int selected(ItemStack s) {
        int n = faces(s).size();
        return n == 0 ? 0 : Math.floorMod(tag(s).getInt(SEL), n);
    }

    /** Stores a face seen dying (see {@link Wave25Pure#storeFace}); true if the mask changed. */
    static boolean store(ItemStack s, String face, long now) {
        List<String> before = faces(s);
        List<String> after = Wave25Pure.storeFace(before, face, now, lastStored(s), Wave25Tune.MASK_FACES, Wave25Tune.MASK_REPLACE_GAP);
        if (after.equals(before)) return false;
        boolean replaced = before.size() >= Wave25Tune.MASK_FACES && !before.contains(face);
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> {
            ListTag l = new ListTag();
            for (String f : after) l.add(StringTag.valueOf(f));
            t.put(FACES, l);
            if (replaced || before.size() < after.size()) t.putLong(LAST, now);
            t.putInt(SEL, after.size() - 1);                            // the newest face is the one at hand
        });
        return true;
    }

    static void select(ItemStack s, int index) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt(SEL, Math.max(0, index)));
    }
}
