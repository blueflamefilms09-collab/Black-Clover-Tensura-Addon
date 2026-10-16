package com.newuniverse.nusmp.avatar;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: an Avatar lives in a chunk, and a chunk that is not loaded cannot tick, so a betrayed Avatar hunting a creator in another dimension, or a loyal one
 * that has to follow, must be findable without the entity being loaded. This world-wide SavedData (the owner's "Global World Saved Data") records every
 * Avatar's creator, name, state, last dimension and last position, and counts a creator's Avatars for the per-player limit; the entity updates its row every
 * few seconds and removes it on death or discard. {@link com.newuniverse.nusmp.avatar.betrayal.BetrayalDimensionHandler} reads it to put a short chunk
 * ticket on the last known position so the entity wakes up and acts.
 */
public final class AvatarManager extends SavedData {
    private static final String NAME = "nusmp_avatars";

    /** One row. {@code state} is an {@link com.newuniverse.nusmp.avatar.outcome.Outcome} name; {@code dim} is a dimension id. */
    public static final class Row {
        public UUID id, creator;
        public String name = "", dim = "minecraft:overworld", state = "PENDING";
        public double x, y, z, tax;
        public long lastWake;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putUUID("Creator", creator);
            t.putString("Name", name);
            t.putString("Dim", dim);
            t.putString("State", state);
            t.putDouble("X", x); t.putDouble("Y", y); t.putDouble("Z", z);
            t.putLong("Wake", lastWake);
            t.putDouble("Tax", tax);
            return t;
        }

        static Row load(CompoundTag t) {
            Row r = new Row();
            r.id = t.getUUID("Id");
            r.creator = t.getUUID("Creator");
            r.name = t.getString("Name");
            r.dim = t.getString("Dim");
            r.state = t.getString("State");
            r.x = t.getDouble("X"); r.y = t.getDouble("Y"); r.z = t.getDouble("Z");
            r.lastWake = t.getLong("Wake");
            r.tax = t.getDouble("Tax");
            return r;
        }
    }

    private final Map<UUID, Row> rows = new LinkedHashMap<>();

    public static AvatarManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AvatarManager::new, AvatarManager::load, null), NAME);
    }

    private static AvatarManager load(CompoundTag tag, HolderLookup.Provider provider) {
        AvatarManager m = new AvatarManager();
        ListTag list = tag.getList("Rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                Row r = Row.load(list.getCompound(i));
                m.rows.put(r.id, r);
            } catch (RuntimeException skip) { /* a damaged row is dropped, the rest still load */ }
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Row r : rows.values()) list.add(r.save());
        tag.put("Rows", list);
        return tag;
    }

    // ---------------------------------------------------------------- access
    public Row row(UUID id) { return rows.get(id); }

    public List<Row> all() { return new ArrayList<>(rows.values()); }

    public List<Row> of(UUID creator) {
        List<Row> out = new ArrayList<>();
        for (Row r : rows.values()) if (r.creator.equals(creator)) out.add(r);
        return out;
    }

    /** The sum of the permanent magicule tax of the creator's living Avatars, as a fraction of their maximum (at most 0.9). */
    public double totalTax(UUID creator) {
        double t = 0;
        for (Row r : of(creator)) t += r.tax;
        return Math.min(0.9, t);
    }

    public int count(UUID creator) { return of(creator).size(); }

    public List<String> names(UUID creator) {
        List<String> out = new ArrayList<>();
        for (Row r : of(creator)) if (!r.name.isEmpty()) out.add(r.name);
        return out;
    }

    /** Writes the Avatar's current facts into its row (creating it) and marks the data dirty only when something changed. */
    public void update(AvatarEntity a) {
        if (a.creatorUuid() == null) return;
        Row r = rows.computeIfAbsent(a.getUUID(), k -> new Row());
        boolean changed = r.id == null;
        r.id = a.getUUID();
        r.creator = a.creatorUuid();
        String dim = a.level().dimension().location().toString(), state = a.outcomeState().name(), name = a.avatarName();
        changed |= r.tax != a.permanentTax() || !dim.equals(r.dim) || !state.equals(r.state) || !name.equals(r.name) || Math.abs(r.x - a.getX()) > 8 || Math.abs(r.z - a.getZ()) > 8 || Math.abs(r.y - a.getY()) > 8;
        r.dim = dim; r.state = state; r.name = name; r.tax = a.permanentTax();
        if (changed) { r.x = a.getX(); r.y = a.getY(); r.z = a.getZ(); setDirty(); }
    }

    public void remove(UUID id) { if (rows.remove(id) != null) setDirty(); }

    public void touchWake(UUID id, long gameTime) {
        Row r = rows.get(id);
        if (r != null) { r.lastWake = gameTime; setDirty(); }
    }
}
