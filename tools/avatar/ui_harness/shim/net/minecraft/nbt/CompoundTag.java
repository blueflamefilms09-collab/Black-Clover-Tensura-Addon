package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

/** Why: harness shim: only what AppearanceData uses. */
public class CompoundTag {
    private final Map<String, Object> m = new HashMap<>();
    public void putInt(String k, int v) { m.put(k, v); }
    public void putFloat(String k, float v) { m.put(k, v); }
    public int getInt(String k) { Object o = m.get(k); return o instanceof Integer i ? i : 0; }
    public float getFloat(String k) { Object o = m.get(k); return o instanceof Float f ? f : 0f; }
    public boolean contains(String k) { return m.containsKey(k); }
    public boolean isEmpty() { return m.isEmpty(); }
}
