package com.newuniverse.nusmp.shishou;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: there is ONE Shishou per world, so there is ONE saved record of him, in the Overworld's data storage ({@code nusmp_shishou.dat}): where he is and who wields him ({@link ShishouCap}), his level,
 * experience, mana and skills ({@link ShishouRecord}), the contents of Stomach ({@link ShishouStorage}) and the day of the last natural-appearance roll. It survives the wielder's death, a lost item and
 * a restart; the item, the flying entity and the thrown minion are only bodies of it. This is the saved-data wrapper; the rules are the pure classes. Server thread only.
 */
public final class ShishouData extends SavedData {
    public static final String NAME = "nusmp_shishou";

    private static volatile MinecraftServer server;

    private final ShishouCap cap;
    private final ShishouRecord record;
    private final ShishouStorage storage = new ShishouStorage();
    private long lastRollDay = -1L;

    private ShishouData(ShishouCap cap, ShishouRecord record) {
        this.cap = cap;
        this.record = record;
    }

    public ShishouData() { this(new ShishouCap(), new ShishouRecord()); }

    // ---------------------------------------------------------------- the world's record
    /** Called when the server has started and when it stops (null). */
    public static void bind(MinecraftServer s) { server = s; }

    public static boolean bound() { return server != null; }

    public static MinecraftServer server() { return server; }

    public static ShishouData get() { return get(server); }

    public static ShishouData get(MinecraftServer s) {
        ShishouData d = s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ShishouData::new, ShishouData::load, null), NAME);
        d.storage.setListener(d::setDirty);
        return d;
    }

    public ShishouCap cap() { return cap; }

    public ShishouRecord record() { return record; }

    public ShishouStorage storage() { return storage; }

    public long lastRollDay() { return lastRollDay; }

    public void setLastRollDay(long d) {
        lastRollDay = d;
        setDirty();
    }

    /** Marks the data changed (the caller changed the cap or the record). */
    public void touch() { setDirty(); }

    // ---------------------------------------------------------------- saving
    static ShishouData load(CompoundTag tag, HolderLookup.Provider provider) {
        Map<String, String> cm = new LinkedHashMap<>();
        CompoundTag c = tag.getCompound("cap");
        for (String k : c.getAllKeys()) cm.put(k, c.getString(k));
        Map<String, Long> rm = new LinkedHashMap<>();
        CompoundTag r = tag.getCompound("record");
        for (String k : r.getAllKeys()) if (r.get(k) instanceof net.minecraft.nbt.NumericTag n) rm.put(k, n.getAsLong());
        ShishouData d = new ShishouData(ShishouCap.fromMap(cm), ShishouRecord.fromMap(rm));
        d.lastRollDay = tag.contains("rollDay", Tag.TAG_LONG) ? tag.getLong("rollDay") : -1L;
        d.storage.load(tag.getCompound("storage"), provider);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag c = new CompoundTag();
        for (Map.Entry<String, String> e : cap.toMap().entrySet()) c.put(e.getKey(), StringTag.valueOf(e.getValue()));
        tag.put("cap", c);
        CompoundTag r = new CompoundTag();
        for (Map.Entry<String, Long> e : record.toMap().entrySet()) r.put(e.getKey(), LongTag.valueOf(e.getValue()));
        tag.put("record", r);
        tag.putLong("rollDay", lastRollDay);
        tag.put("storage", storage.save(provider));
        return tag;
    }
}
