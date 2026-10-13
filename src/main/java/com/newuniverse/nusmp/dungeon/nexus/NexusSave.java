package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.LegacySeal;
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

/**
 * What the Variable Core remembers (saved with the world, in the Overworld's data storage as "nusmp_nexus"): for each core cell, the
 * zone it was built as, how far its building got (stage, op and cell, so a restart carries on), whether its seal was broken, whether
 * its central relic was taken, a zone an admin forced before it was built, and the ground it covers. Cells nothing ever happened in
 * have no record.
 */
public final class NexusSave extends SavedData {
    public static final String NAME = "nusmp_nexus";

    /** One core cell. Fields are public for the framework and the zone agents; change them only through the methods that mark the data dirty. */
    public static final class Entry {
        public NexusState state = NexusState.UNBUILT;
        /** The zone a started build fixed for good; null before that. */
        public NexusZone zone;
        /** The zone an admin forced for a cell nothing was built in; null if none. */
        public NexusZone override;
        public int stage, op;
        public long cell;
        public boolean relicTaken, sealBroken;
        /** The ground and height of the Nexus, recorded when the build starts (null before). */
        public Footprint fp;
        public int floorY, topY;
    }

    private final Map<Long, Entry> cells = new HashMap<>();
    /** Was the Golden City's old seal flag already set when this data was first made (a world from before the Nexus)? */
    private boolean legacyWorld;
    private boolean fresh = true;

    public static NexusSave get(MinecraftServer server) {
        NexusSave s = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(NexusSave::new, NexusSave::load, null), NAME);
        if (s.fresh) {
            s.fresh = false;
            s.legacyWorld = LegacySeal.broken(server);
            s.setDirty();
        }
        return s;
    }

    /** The key of a core cell. */
    public static long key(int cellX, int cellZ) { return NexusCells.key(cellX, cellZ); }

    // ---------------------------------------------------------------- reading
    /** The record of a cell, or null if nothing was ever recorded there. */
    public Entry lookup(long key) { return cells.get(key); }

    /** The record of a cell, made if it is not there yet. */
    public Entry entry(long key) { return cells.computeIfAbsent(key, k -> new Entry()); }

    public NexusState state(long key) {
        Entry e = cells.get(key);
        return e == null ? NexusState.UNBUILT : e.state;
    }

    /** The zone of a cell: the one its build fixed, else the one an admin forced, else the stable hash. */
    public NexusZone zoneOf(int cellX, int cellZ) {
        Entry e = cells.get(key(cellX, cellZ));
        return ZoneChoice.pick(cellX, cellZ, e == null ? null : e.zone, e == null ? null : e.override);
    }

    public boolean sealBroken(long key) {
        Entry e = cells.get(key);
        return e != null && e.sealBroken;
    }

    public boolean centreRelicTaken(long key) {
        Entry e = cells.get(key);
        return e != null && e.relicTaken;
    }

    /** How many cells have had their seal broken. */
    public int sealedCount() {
        int n = 0;
        for (Entry e : cells.values()) if (e.sealBroken) n++;
        return n;
    }

    /** Was the Golden City's old seal flag already set when this data was first made? */
    public boolean legacyWorld() { return legacyWorld; }

    /** The keys of every recorded cell. */
    public List<Long> keys() { return new ArrayList<>(cells.keySet()); }

    // ---------------------------------------------------------------- writing
    /** Forces the zone of a cell nothing was built in. False if a build was already started there. */
    public boolean forceZone(long key, NexusZone zone) {
        Entry e = entry(key);
        if (!ZoneChoice.canForce(e.state)) return false;
        e.override = zone;
        setDirty();
        return true;
    }

    /** Records that a build of `plan` starts: fixes the zone, remembers the ground. Does nothing to a cell that is already further on. */
    public Entry begin(long key, NexusZone zone, ZonePlan plan) {
        Entry e = entry(key);
        if (e.zone == null) e.zone = zone;
        e.fp = plan.footprint;
        e.floorY = plan.floorY;
        e.topY = plan.topY;
        if (e.state == NexusState.UNBUILT) e.state = NexusState.PLAN;
        setDirty();
        return e;
    }

    /** Remembers how far the building got. */
    public void progress(long key, int stage, int op, long cell) {
        Entry e = entry(key);
        e.stage = stage;
        e.op = op;
        e.cell = cell;
        if (e.state == NexusState.PLAN || e.state == NexusState.UNBUILT) e.state = NexusState.BUILDING;
        setDirty();
    }

    /** The Nexus stands: the cell is DONE and the progress is forgotten. */
    public void finish(long key) {
        Entry e = entry(key);
        e.state = NexusState.DONE;
        e.stage = 0;
        e.op = 0;
        e.cell = 0;
        setDirty();
    }

    /** The Nexus was robbed and brought down. */
    public void markCollapsed(long key) {
        Entry e = entry(key);
        e.state = NexusState.COLLAPSED;
        setDirty();
    }

    public void markCentreRelicTaken(long key) {
        Entry e = entry(key);
        if (e.relicTaken) return;
        e.relicTaken = true;
        setDirty();
    }

    public void breakSeal(long key) {
        Entry e = entry(key);
        if (e.sealBroken) return;
        e.sealBroken = true;
        setDirty();
    }

    /** Forgets a cell: it counts as unbuilt again, its seal is whole and its relic back. A zone an admin forced stays forced. False if there was nothing to forget. */
    public boolean forget(long key) {
        Entry e = cells.get(key);
        if (e == null) return false;
        boolean had = e.state.started() || e.sealBroken || e.relicTaken;
        if (e.override != null) {
            Entry kept = new Entry();
            kept.override = e.override;
            cells.put(key, kept);
        } else {
            cells.remove(key);
        }
        setDirty();
        return had;
    }

    // ---------------------------------------------------------------- saving
    private static NexusZone zoneNamed(String name) {
        if (name == null || name.isEmpty()) return null;
        try { return NexusZone.valueOf(name); } catch (IllegalArgumentException unknown) { return null; }
    }

    static NexusSave load(CompoundTag tag, HolderLookup.Provider provider) {
        NexusSave s = new NexusSave();
        s.fresh = false;
        s.legacyWorld = tag.getBoolean("LegacyWorld");
        ListTag list = tag.getList("Cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry e = new Entry();
            e.state = NexusState.of(t.getInt("State"));
            e.zone = zoneNamed(t.getString("Zone"));
            e.override = zoneNamed(t.getString("Override"));
            e.stage = t.getInt("Stage");
            e.op = t.getInt("Op");
            e.cell = t.getLong("Cell");
            e.relicTaken = t.getBoolean("Relic");
            e.sealBroken = t.getBoolean("Seal");
            int[] fp = t.getIntArray("Fp");
            if (fp.length == 7 && fp[3] >= 1 && fp[4] >= 1 && fp[0] >= 0 && fp[0] < Footprint.Shape.values().length) {
                e.fp = new Footprint(Footprint.Shape.values()[fp[0]], fp[1], fp[2], fp[3], fp[4]);
                e.floorY = fp[5];
                e.topY = fp[6];
            }
            s.cells.put(t.getLong("K"), e);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("LegacyWorld", legacyWorld);
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> me : cells.entrySet()) {
            Entry e = me.getValue();
            CompoundTag t = new CompoundTag();
            t.putLong("K", me.getKey());
            t.putInt("State", e.state.id);
            if (e.zone != null) t.putString("Zone", e.zone.name());
            if (e.override != null) t.putString("Override", e.override.name());
            t.putInt("Stage", e.stage);
            t.putInt("Op", e.op);
            t.putLong("Cell", e.cell);
            t.putBoolean("Relic", e.relicTaken);
            t.putBoolean("Seal", e.sealBroken);
            if (e.fp != null) t.putIntArray("Fp", new int[]{e.fp.shape().ordinal(), e.fp.centreX(), e.fp.centreZ(), e.fp.halfX(), e.fp.halfZ(), e.floorY, e.topY});
            list.add(t);
        }
        tag.put("Cells", list);
        return tag;
    }
}
