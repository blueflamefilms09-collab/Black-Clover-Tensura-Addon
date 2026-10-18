package com.newuniverse.nusmp.elite.forge.store;

import com.newuniverse.nusmp.elite.forge.rules.ForgeProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the NBT half of "Crafting Progress Storage". {@link ForgeProgress} is pure and speaks plain maps; this adapter turns that map into a CompoundTag and back.
 * Reading is as tolerant as the pure class: every key is checked for its tag type before it is read, so a missing key, a key of another type (an older or newer
 * version, a hand-edited save) or a list with junk in it falls back to the default instead of throwing. Keys: {@code crafted} int, {@code by_tier} and
 * {@code by_quality} int arrays, {@code best}, {@code top_station}, {@code spec_changes} ints, {@code spec} string, {@code known} list of strings.
 */
public final class ProgressTag {
    private ProgressTag() {}

    public static CompoundTag write(ForgeProgress p) {
        CompoundTag t = new CompoundTag();
        Map<String, Object> m = p.toMap();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Integer i) t.putInt(e.getKey(), i);
            else if (v instanceof String s) t.putString(e.getKey(), s);
            else if (v instanceof List<?> l) {
                if (!l.isEmpty() && l.get(0) instanceof Integer) {
                    int[] a = new int[l.size()];
                    for (int i = 0; i < a.length; i++) a[i] = (Integer) l.get(i);
                    t.putIntArray(e.getKey(), a);
                } else {
                    ListTag lt = new ListTag();
                    for (Object o : l) lt.add(StringTag.valueOf(String.valueOf(o)));
                    t.put(e.getKey(), lt);
                }
            }
        }
        return t;
    }

    public static ForgeProgress read(CompoundTag t) {
        Map<String, Object> m = new HashMap<>();
        if (t != null) {
            for (String k : new String[]{"crafted", "best", "top_station", "spec_changes"}) if (t.contains(k, Tag.TAG_ANY_NUMERIC)) m.put(k, t.getInt(k));
            if (t.contains("spec", Tag.TAG_STRING)) m.put("spec", t.getString("spec"));
            for (String k : new String[]{"by_tier", "by_quality"}) {
                if (t.contains(k, Tag.TAG_INT_ARRAY)) {
                    List<Integer> l = new ArrayList<>();
                    for (int v : t.getIntArray(k)) l.add(v);
                    m.put(k, l);
                } else if (t.contains(k, Tag.TAG_LIST)) {
                    List<Integer> l = new ArrayList<>();
                    ListTag lt = t.getList(k, Tag.TAG_INT);
                    for (int i = 0; i < lt.size(); i++) l.add(lt.getInt(i));
                    m.put(k, l);
                }
            }
            if (t.contains("known", Tag.TAG_LIST)) {
                List<String> l = new ArrayList<>();
                ListTag lt = t.getList("known", Tag.TAG_STRING);
                for (int i = 0; i < lt.size(); i++) l.add(lt.getString(i));
                m.put("known", l);
            }
        }
        return ForgeProgress.fromMap(m);
    }
}
