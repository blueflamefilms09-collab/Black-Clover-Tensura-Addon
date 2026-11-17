package com.newuniverse.nusmp.unique;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the world's memory of its 1-of-1s lives in the Overworld's data storage as {@code nusmp_unique.dat}: per unique id the state, last holder, last place, the clocks, the epoch and the release times (the pure
 * {@link UniqueData}), plus two small tables of the Holy Weapons rule (when a bonded player was last seen, and which player's bond must dissolve if they come back after their weapon was released). Size: one record per unique
 * ever seen (at most a few hundred) and one entry per Holy Weapon holder. Server thread only.
 */
public final class UniqueSaved extends SavedData {
    public static final String NAME = "nusmp_unique";

    private final UniqueData data = new UniqueData();
    /** Holy Weapon holders: when they were last seen online (real ms). */
    final Map<UUID, Long> heroSeen = new HashMap<>();
    /** Players whose Holy Weapon was returned to the world while they were away: the weapon's item id. */
    final Map<UUID, String> heroDissolve = new HashMap<>();

    public static UniqueSaved get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(UniqueSaved::new, UniqueSaved::load, null), NAME);
    }

    public UniqueData data() { return data; }

    static UniqueSaved load(CompoundTag tag, HolderLookup.Provider provider) {
        UniqueSaved s = new UniqueSaved();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            String id = t.getString("Id");
            if (!UniqueRules.validId(id)) continue;
            UniqueRecord r = new UniqueRecord(id);
            r.state = UniqueState.byOrdinal(t.getInt("State"));
            if (t.hasUUID("Holder")) r.holder = t.getUUID("Holder");
            r.dim = t.getString("Dim");
            r.x = t.getDouble("X");
            r.y = t.getDouble("Y");
            r.z = t.getDouble("Z");
            r.block = t.contains("Block") ? t.getLong("Block") : UniqueRecord.NO_BLOCK;
            r.seenTick = t.getLong("SeenTick");
            r.seenMs = t.getLong("SeenMs");
            r.savedMs = r.seenMs;
            r.epoch = t.getInt("Epoch");
            r.releases = t.getInt("Releases");
            r.lastReleaseMs = t.getLong("LastReleaseMs");
            r.lastReason = t.getString("Reason");
            long[] st = t.getLongArray("Stamps");
            r.stampCount = Math.min(st.length, UniqueRecord.STAMPS);
            System.arraycopy(st, 0, r.stamps, 0, r.stampCount);
            // a missing clock, a dropped entity and an open grace do not survive a restart: the next scan sees the copy again, or starts the clock afresh
            s.data.put(r);
        }
        ListTag seen = tag.getList("HeroSeen", Tag.TAG_COMPOUND);
        for (int i = 0; i < seen.size(); i++) {
            CompoundTag t = seen.getCompound(i);
            if (t.hasUUID("Id")) s.heroSeen.put(t.getUUID("Id"), t.getLong("At"));
        }
        ListTag dis = tag.getList("HeroDissolve", Tag.TAG_COMPOUND);
        for (int i = 0; i < dis.size(); i++) {
            CompoundTag t = dis.getCompound(i);
            if (t.hasUUID("Id") && !t.getString("Weapon").isEmpty()) s.heroDissolve.put(t.getUUID("Id"), t.getString("Weapon"));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (UniqueRecord r : data.all()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", r.id);
            t.putInt("State", r.state.ordinal());
            if (r.holder != null) t.putUUID("Holder", r.holder);
            t.putString("Dim", r.dim);
            t.putDouble("X", r.x);
            t.putDouble("Y", r.y);
            t.putDouble("Z", r.z);
            if (r.block != UniqueRecord.NO_BLOCK) t.putLong("Block", r.block);
            t.putLong("SeenTick", r.seenTick);
            t.putLong("SeenMs", r.seenMs);
            t.putInt("Epoch", r.epoch);
            t.putInt("Releases", r.releases);
            t.putLong("LastReleaseMs", r.lastReleaseMs);
            t.putString("Reason", r.lastReason);
            long[] st = new long[r.stampCount];
            System.arraycopy(r.stamps, 0, st, 0, r.stampCount);
            t.putLongArray("Stamps", st);
            list.add(t);
        }
        tag.put("Records", list);
        ListTag seen = new ListTag();
        for (Map.Entry<UUID, Long> e : heroSeen.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.getKey());
            t.putLong("At", e.getValue());
            seen.add(t);
        }
        tag.put("HeroSeen", seen);
        ListTag dis = new ListTag();
        for (Map.Entry<UUID, String> e : heroDissolve.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.getKey());
            t.putString("Weapon", e.getValue());
            dis.add(t);
        }
        tag.put("HeroDissolve", dis);
        return tag;
    }
}
