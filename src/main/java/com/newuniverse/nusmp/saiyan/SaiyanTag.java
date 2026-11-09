package com.newuniverse.nusmp.saiyan;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
        t.putInt("v", 2);
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
        // 3.65.0: Stage 2 and the Goku path (a v1 save has none of these keys and reads as the defaults)
        CompoundTag uses = new CompoundTag();
        for (Map.Entry<String, Integer> e : s.uses.entrySet()) uses.putInt(e.getKey(), e.getValue());
        t.put("uses", uses);
        t.putInt("zenkai", s.zenkai);
        t.putLong("zenkaiAt", s.zenkaiAt);
        t.putLong("zenkaiDay", s.zenkaiDay);
        t.putInt("zenkaiToday", s.zenkaiToday);
        t.putString("ultimate", s.ultimate);
        t.putInt("hardKills", s.hardMartialKills);
        t.putInt("trialKills", s.trialKills);
        t.put("training", doubles(s.training));
        t.put("trainToday", doubles(s.trainToday));
        t.putLong("trainDay", s.trainDay);
        t.put("mastery", doubles(s.formMastery));
        t.put("unlocked", strings(s.unlocked));
        t.put("flags", strings(s.flags));
        t.putString("selected", s.selectedForm);
        t.putDouble("strain", s.strain);
        t.putDouble("formToday", s.formSecondsToday);
        t.putLong("formDay", s.formDay);
        return t;
    }

    private static CompoundTag doubles(Map<String, Double> m) {
        CompoundTag c = new CompoundTag();
        for (Map.Entry<String, Double> e : m.entrySet()) c.putDouble(e.getKey(), e.getValue());
        return c;
    }

    private static ListTag strings(Iterable<String> it) {
        ListTag l = new ListTag();
        for (String x : it) l.add(StringTag.valueOf(x));
        return l;
    }

    /** A save key kept only if it is short and not empty (a damaged save can not grow a map with junk). */
    private static boolean key(String k) { return k != null && !k.isEmpty() && k.length() <= 60; }

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
        CompoundTag uses = t.getCompound("uses");
        for (String k : uses.getAllKeys()) if (key(k) && s.uses.size() < SaiyanState.MAX_USES) s.uses.put(k, Math.max(0, Math.min(1_000_000, uses.getInt(k))));
        s.zenkai = Math.max(0, Math.min(100_000, t.getInt("zenkai")));
        s.zenkaiAt = t.contains("zenkaiAt") ? t.getLong("zenkaiAt") : -1;
        s.zenkaiDay = t.contains("zenkaiDay") ? t.getLong("zenkaiDay") : -1;
        s.zenkaiToday = Math.max(0, Math.min(1000, t.getInt("zenkaiToday")));
        String ult = t.getString("ultimate");
        s.ultimate = SaiyanUltimates.byId(ult) != null ? ult : "";
        s.hardMartialKills = Math.max(0, Math.min(1_000_000, t.getInt("hardKills")));
        s.trialKills = Math.max(0, Math.min(1_000_000, t.getInt("trialKills")));
        readDoubles(t.getCompound("training"), s.training, 1.0e9);
        readDoubles(t.getCompound("trainToday"), s.trainToday, 1.0e6);
        s.trainDay = t.contains("trainDay") ? t.getLong("trainDay") : -1;
        readDoubles(t.getCompound("mastery"), s.formMastery, 1.0);
        for (Tag x : t.getList("unlocked", Tag.TAG_STRING)) {
            String id = x.getAsString();
            if (SaiyanForms.byId(id) != null && s.unlocked.size() < SaiyanState.MAX_FORMS) s.unlocked.add(id);
        }
        for (Tag x : t.getList("flags", Tag.TAG_STRING)) {
            String f = x.getAsString();
            if (key(f) && s.flags.size() < SaiyanState.MAX_FLAGS) s.flags.add(f);
        }
        String sel = t.getString("selected");
        s.selectedForm = SaiyanForms.byId(sel) != null ? sel : "";
        s.strain = SaiyanRules.clamp(t.getDouble("strain"), 0.0, 100.0);
        s.formSecondsToday = SaiyanRules.clamp(t.getDouble("formToday"), 0.0, 1.0e6);
        s.formDay = t.contains("formDay") ? t.getLong("formDay") : -1;
        return s;
    }

    private static void readDoubles(CompoundTag c, Map<String, Double> into, double hi) {
        for (String k : c.getAllKeys()) if (key(k) && into.size() < SaiyanState.MAX_FORMS) into.put(k, SaiyanRules.clamp(c.getDouble(k), 0.0, hi));
    }
}
