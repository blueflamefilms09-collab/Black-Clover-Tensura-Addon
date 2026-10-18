package com.newuniverse.nusmp.elite.world.calamity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: what the calamity director must remember across restarts, saved with the world (in the overworld's data storage, one file for the whole server): when each calamity last woke (its
 * cooldown), which calamity bosses are alive, and the omens that are counting down (a server stopped during an omen resumes it). Everything is small.
 */
public final class CalamityData extends SavedData {
    private static final String NAME = "nusmp_elite_calamities";

    /** A calamity boss that is alive. */
    public static final class Active {
        public final UUID boss;
        public final String id, dimension;
        public double x, y, z;
        public int misses;

        public Active(UUID boss, String id, String dimension, double x, double y, double z) {
            this.boss = boss;
            this.id = id;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** An announced calamity that has not risen yet. {@code riseAt} is the game time of the rise; {@code lastCount} the last countdown second announced. */
    public static final class Omen {
        public final String id, dimension, reason;
        public final double x, y, z;
        public final long riseAt;
        public final UUID target;
        public int lastCount = Integer.MAX_VALUE, waited;

        public Omen(String id, String dimension, String reason, double x, double y, double z, long riseAt, UUID target) {
            this.id = id;
            this.dimension = dimension;
            this.reason = reason;
            this.x = x;
            this.y = y;
            this.z = z;
            this.riseAt = riseAt;
            this.target = target;
        }
    }

    public final Map<String, Long> lastWoke = new HashMap<>();
    public final List<Active> active = new ArrayList<>();
    public final List<Omen> omens = new ArrayList<>();

    public static CalamityData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CalamityData::new, CalamityData::load, null), NAME);
    }

    public long woke(String id) { return lastWoke.getOrDefault(id, -1_000_000_000L); }

    public Active activeOf(UUID boss) {
        for (Active a : active) if (a.boss.equals(boss)) return a;
        return null;
    }

    public static CalamityData load(CompoundTag tag, HolderLookup.Provider lookup) {
        CalamityData d = new CalamityData();
        CompoundTag w = tag.getCompound("woke");
        for (String k : w.getAllKeys()) d.lastWoke.put(k, w.getLong(k));
        for (Tag t : tag.getList("active", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("boss")) d.active.add(new Active(c.getUUID("boss"), c.getString("id"), c.getString("dim"), c.getDouble("x"), c.getDouble("y"), c.getDouble("z")));
        }
        for (Tag t : tag.getList("omens", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.omens.add(new Omen(c.getString("id"), c.getString("dim"), c.getString("reason"), c.getDouble("x"), c.getDouble("y"), c.getDouble("z"), c.getLong("at"),
                    c.hasUUID("target") ? c.getUUID("target") : null));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        CompoundTag w = new CompoundTag();
        for (Map.Entry<String, Long> e : lastWoke.entrySet()) w.putLong(e.getKey(), e.getValue());
        tag.put("woke", w);
        ListTag a = new ListTag();
        for (Active x : active) {
            CompoundTag c = new CompoundTag();
            c.putUUID("boss", x.boss);
            c.putString("id", x.id);
            c.putString("dim", x.dimension);
            c.putDouble("x", x.x);
            c.putDouble("y", x.y);
            c.putDouble("z", x.z);
            a.add(c);
        }
        tag.put("active", a);
        ListTag o = new ListTag();
        for (Omen x : omens) {
            CompoundTag c = new CompoundTag();
            c.putString("id", x.id);
            c.putString("dim", x.dimension);
            c.putString("reason", x.reason);
            c.putDouble("x", x.x);
            c.putDouble("y", x.y);
            c.putDouble("z", x.z);
            c.putLong("at", x.riseAt);
            if (x.target != null) c.putUUID("target", x.target);
            o.add(c);
        }
        tag.put("omens", o);
        return tag;
    }
}
