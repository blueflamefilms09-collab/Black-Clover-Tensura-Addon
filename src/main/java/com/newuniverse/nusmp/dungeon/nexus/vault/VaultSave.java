package com.newuniverse.nusmp.dungeon.nexus.vault;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Dungeon's hidden rooms remember (saved with the world, in the Overworld's data storage as "nusmp_vaults"): for each cell of
 * the vault, shrine and core grids that rolled a room, how the search for its site went (which attempt it is on, or that there is none),
 * where the site is, how far the building got (so a restart carries on) and whether its puzzle was solved; and the chunks whose Warp Traps
 * have been laid. Cells that rolled nothing have no record.
 */
public final class VaultSave extends SavedData {
    public static final String NAME = "nusmp_vaults";

    /** The search is still going (attempt N of the cell), there is no site, a site is planned, being built, built, or fully populated. */
    public static final int SCANNING = 0, NONE = 1, PLANNED = 2, BUILDING = 3, BUILT = 4, DONE = 5;

    /** One cell's room. Fields are public for the director; change them only before calling {@link #dirty()}. */
    public static final class Entry {
        public final int grid, cellX, cellZ;
        public final VaultKind kind;
        public int state = SCANNING, attempt, op;
        public long cell;
        public VaultSite site;
        public boolean solved;

        Entry(int grid, int cellX, int cellZ, VaultKind kind) {
            this.grid = grid;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.kind = kind;
        }

        /** Is the room there to be walked into (built, at least)? */
        public boolean standing() { return state >= BUILT && site != null; }
    }

    private final Map<Long, Entry> entries = new HashMap<>();
    private final LongOpenHashSet trapChunks = new LongOpenHashSet();

    public static VaultSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(VaultSave::new, VaultSave::load, null), NAME);
    }

    /** The key of a cell of a grid. */
    public static long key(int grid, int cellX, int cellZ) { return ((long) grid << 60) | ((long) (cellX & 0x3FFFFFFF) << 30) | (cellZ & 0x3FFFFFFFL); }

    public Entry lookup(int grid, int cellX, int cellZ) { return entries.get(key(grid, cellX, cellZ)); }

    /** The record of a cell, made if it is not there yet. */
    public Entry entry(int grid, int cellX, int cellZ, VaultKind kind) {
        return entries.computeIfAbsent(key(grid, cellX, cellZ), k -> {
            setDirty();
            return new Entry(grid, cellX, cellZ, kind);
        });
    }

    public Collection<Entry> all() { return entries.values(); }

    /** Marks the data as changed. */
    public void dirty() { setDirty(); }

    // ---------------------------------------------------------------- sites in space
    /** The records of every grid's cells round this column (the cell it is in and the eight beside it) that have a site. */
    public List<Entry> sitesNear(int x, int z) {
        List<Entry> out = new ArrayList<>();
        for (int grid = 0; grid < VaultPlan.CELL.length; grid++) {
            int size = VaultPlan.CELL[grid], cx = Math.floorDiv(x, size), cz = Math.floorDiv(z, size);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Entry e = entries.get(key(grid, cx + dx, cz + dz));
                    if (e != null && e.site != null) out.add(e);
                }
            }
        }
        return out;
    }

    /** Is this block in a room or its shell (with a margin), whatever its state? Rooms that are only planned count: nothing else goes there. */
    public boolean insideAnySite(int x, int y, int z, int margin) {
        for (Entry e : sitesNear(x, z)) if (e.site.contains(x, y, z, margin)) return true;
        return false;
    }

    /** Is this block in the air of a standing room (for keeping players and traps out of hidden rooms)? */
    public boolean insideAnyRoom(int x, int y, int z, int margin) {
        for (Entry e : sitesNear(x, z)) if (e.standing() && e.site.insideInterior(x, y, z, margin)) return true;
        return false;
    }

    /** Does a candidate site overlap one already recorded (shells and a margin of two blocks)? */
    public boolean overlaps(VaultSite s) {
        for (Entry e : sitesNear(s.centreX(), s.centreZ())) if (e.site.intersects(s, 2)) return true;
        return false;
    }

    // ---------------------------------------------------------------- warp traps
    public boolean trapsLaid(long chunkPos) { return trapChunks.contains(chunkPos); }

    public void markTrapsLaid(long chunkPos) {
        if (trapChunks.add(chunkPos)) setDirty();
    }

    public int trapChunkCount() { return trapChunks.size(); }

    // ---------------------------------------------------------------- saving
    private static VaultSave load(CompoundTag tag, HolderLookup.Provider provider) {
        VaultSave s = new VaultSave();
        ListTag list = tag.getList("Rooms", Tag.TAG_COMPOUND);
        VaultKind[] kinds = VaultKind.values();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int kind = t.getInt("Kind");
            if (kind < 0 || kind >= kinds.length) continue;
            Entry e = new Entry(t.getInt("G"), t.getInt("X"), t.getInt("Z"), kinds[kind]);
            e.state = t.getInt("State");
            e.attempt = t.getInt("Attempt");
            e.op = t.getInt("Op");
            e.cell = t.getLong("Cell");
            e.solved = t.getBoolean("Solved");
            if (t.contains("Site", Tag.TAG_COMPOUND)) {
                CompoundTag st = t.getCompound("Site");
                try {
                    e.site = new VaultSite(kinds[st.getInt("Kind")], st.getInt("IX"), st.getInt("IY"), st.getInt("IZ"), st.getInt("W"), st.getInt("H"), st.getInt("D"),
                            st.getInt("F"), st.getBoolean("Hidden"), st.getInt("Var"));
                } catch (RuntimeException damaged) {
                    e.site = null;
                    if (e.state >= PLANNED) e.state = SCANNING;                      // a damaged site is searched for again
                }
            } else if (e.state >= PLANNED) {
                e.state = SCANNING;
            }
            s.entries.put(key(e.grid, e.cellX, e.cellZ), e);
        }
        for (long c : tag.getLongArray("Traps")) s.trapChunks.add(c);
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putInt("G", e.grid);
            t.putInt("X", e.cellX);
            t.putInt("Z", e.cellZ);
            t.putInt("Kind", e.kind.ordinal());
            t.putInt("State", e.state);
            t.putInt("Attempt", e.attempt);
            t.putInt("Op", e.op);
            t.putLong("Cell", e.cell);
            t.putBoolean("Solved", e.solved);
            if (e.site != null) {
                CompoundTag st = new CompoundTag();
                st.putInt("Kind", e.site.kind().ordinal());
                st.putInt("IX", e.site.ix());
                st.putInt("IY", e.site.iy());
                st.putInt("IZ", e.site.iz());
                st.putInt("W", e.site.width());
                st.putInt("H", e.site.height());
                st.putInt("D", e.site.depth());
                st.putInt("F", e.site.facing());
                st.putBoolean("Hidden", e.site.hidden());
                st.putInt("Var", e.site.variant());
                t.put("Site", st);
            }
            list.add(t);
        }
        tag.put("Rooms", list);
        tag.putLongArray("Traps", trapChunks.toLongArray());
        return tag;
    }
}
