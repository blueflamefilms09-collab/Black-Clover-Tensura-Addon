package com.newuniverse.nusmp.saiyan;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

/**
 * Why: the hand-written save format of {@link SaiyanState} (3.63.0), clamped on read so a damaged or edited save can not give a value outside the rules. It needs only the NBT classes, so it is unit tested
 * without a game ({@link SaiyanStore} holds the attachment and the codec that use it).
 */
public final class SaiyanTag {
    private SaiyanTag() {}

    public static CompoundTag toTag(SaiyanState s) {
        CompoundTag t = new CompoundTag();
        t.putInt("v", 1);
        t.putBoolean("saiyan", s.saiyan);
        t.putBoolean("awakened", s.awakened);
        t.putDouble("stamina", s.stamina);
        t.putDouble("bp", s.battlePower);
        t.putLong("bpDay", s.bpDay);
        t.putDouble("bpToday", s.bpToday);
        ListTag recent = new ListTag();
        for (Map.Entry<String, long[]> e : s.recent.entrySet()) {
            CompoundTag r = new CompoundTag();
            r.putString("t", e.getKey());
            r.putLong("n", e.getValue()[0]);
            r.putLong("at", e.getValue()[1]);
            recent.add(r);
        }
        t.put("recent", recent);
        CompoundTag cd = new CompoundTag();
        for (Map.Entry<String, Long> e : s.readyAt.entrySet()) cd.putLong(e.getKey(), e.getValue());
        t.put("cd", cd);
        return t;
    }

    public static SaiyanState fromTag(CompoundTag t) {
        SaiyanState s = new SaiyanState();
        s.saiyan = t.getBoolean("saiyan");
        s.awakened = t.getBoolean("awakened");
        s.stamina = t.contains("stamina") ? SaiyanRules.clamp(t.getDouble("stamina"), -1.0, 1.0e9) : -1.0;
        s.battlePower = t.contains("bp") ? SaiyanRules.clampBp(t.getDouble("bp")) : SaiyanRules.START_BP;
        s.bpDay = t.contains("bpDay") ? t.getLong("bpDay") : -1;
        s.bpToday = SaiyanRules.clamp(t.getDouble("bpToday"), 0.0, SaiyanRules.BP_HARD_CAP);
        ListTag recent = t.getList("recent", Tag.TAG_COMPOUND);
        for (int i = 0; i < recent.size() && s.recent.size() < SaiyanState.MAX_RECENT; i++) {
            CompoundTag r = recent.getCompound(i);
            String type = r.getString("t");
            if (type.isEmpty() || type.length() > 80) continue;
            s.recent.put(type, new long[] {Math.max(0, Math.min(10_000, r.getLong("n"))), Math.max(0, r.getLong("at"))});
        }
        CompoundTag cd = t.getCompound("cd");
        for (String k : cd.getAllKeys()) if (s.readyAt.size() < SaiyanState.MAX_COOLDOWNS) s.readyAt.put(k, Math.max(0, cd.getLong(k)));
        return s;
    }
}
