package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.rule.RecordData;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the thin, generic bridge between the pure {@link RecordData} (which speaks Maps, Lists, Numbers and Strings) and the NBT the attachment saves. It knows nothing about the records; it only walks a
 * tag tree into plain values and back. Reading is tolerant by construction: an unknown tag type becomes null and is skipped by {@link RecordData#fromRaw}, a number keeps its exactness (whole numbers
 * stay Longs so a unix timestamp is not rounded). The tolerant semantics themselves are unit-tested on the pure side.
 */
public final class RecordDataNbt {
    private RecordDataNbt() {}

    public static RecordData read(CompoundTag tag) { return RecordData.fromRaw(toRaw(tag)); }

    public static CompoundTag write(RecordData data) {
        Tag t = fromRaw(data.toRaw());
        return t instanceof CompoundTag c ? c : new CompoundTag();
    }

    static Object toRaw(Tag t) {
        if (t instanceof CompoundTag c) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String k : c.getAllKeys()) {
                Object v = toRaw(c.get(k));
                if (v != null) m.put(k, v);
            }
            return m;
        }
        if (t instanceof ListTag l) {
            List<Object> out = new ArrayList<>();
            for (Tag x : l) {
                Object v = toRaw(x);
                if (v != null) out.add(v);
            }
            return out;
        }
        if (t instanceof StringTag s) return s.getAsString();
        if (t instanceof LongTag || t instanceof IntTag || t instanceof ByteTag || t instanceof net.minecraft.nbt.ShortTag) return ((NumericTag) t).getAsLong();
        if (t instanceof NumericTag n) return n.getAsDouble();
        return null;
    }

    static Tag fromRaw(Object o) {
        if (o instanceof Map<?, ?> m) {
            CompoundTag c = new CompoundTag();
            for (var e : m.entrySet()) {
                if (!(e.getKey() instanceof String k)) continue;
                Tag v = fromRaw(e.getValue());
                if (v != null) c.put(k, v);
            }
            return c;
        }
        if (o instanceof List<?> l) {
            ListTag out = new ListTag();
            for (Object x : l) {
                Tag v = fromRaw(x);
                if (v != null) out.add(v);
            }
            return out;
        }
        if (o instanceof String s) return StringTag.valueOf(s);
        if (o instanceof Long n) return LongTag.valueOf(n);
        if (o instanceof Integer n) return IntTag.valueOf(n);
        if (o instanceof Double n) return DoubleTag.valueOf(n);
        if (o instanceof Number n) return DoubleTag.valueOf(n.doubleValue());
        if (o instanceof Boolean b) return ByteTag.valueOf(b);
        return null;
    }
}
