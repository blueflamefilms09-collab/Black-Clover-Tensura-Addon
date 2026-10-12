package com.newuniverse.nusmp.dungeon;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the Living Dungeon remembers server-wide (saved with the world, in the Overworld's data storage as "nusmp_dungeon"):
 * the ambient magicule density of the Overworld's regions (what opens a Maw), the Maws that are open, the blocks mined in the Dungeon
 * that will grow back, and whether the Golden City's seal has been broken.
 */
public final class DungeonSave extends SavedData {
    /** A block mined in the Dungeon and the time it grows back. */
    public record Repair(long pos, CompoundTag state, long due) {}

    /** An open Maw: where it is (dimension and position), when it closes by itself, and the roots wrapped round it. */
    public record Maw(String dim, long pos, long closeAt, long[] roots) {}

    /** density, last update (game time) */
    final Map<Long, double[]> ambient = new HashMap<>();
    final List<Repair> repairs = new ArrayList<>();
    final List<Maw> maws = new ArrayList<>();
    long lastMaw = -1_000_000L;
    boolean cityBreached;

    public static DungeonSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DungeonSave::new, DungeonSave::load, null), "nusmp_dungeon");
    }

    private static DungeonSave load(CompoundTag tag, HolderLookup.Provider provider) {
        DungeonSave s = new DungeonSave();
        ListTag am = tag.getList("Ambient", Tag.TAG_COMPOUND);
        for (int i = 0; i < am.size(); i++) {
            CompoundTag t = am.getCompound(i);
            s.ambient.put(t.getLong("K"), new double[]{t.getDouble("D"), t.getDouble("T")});
        }
        ListTag rp = tag.getList("Repairs", Tag.TAG_COMPOUND);
        for (int i = 0; i < rp.size(); i++) {
            CompoundTag t = rp.getCompound(i);
            s.repairs.add(new Repair(t.getLong("P"), t.getCompound("S"), t.getLong("Due")));
        }
        ListTag mw = tag.getList("Maws", Tag.TAG_COMPOUND);
        for (int i = 0; i < mw.size(); i++) {
            CompoundTag t = mw.getCompound(i);
            s.maws.add(new Maw(t.getString("Dim"), t.getLong("P"), t.getLong("Close"), t.getLongArray("Roots")));
        }
        s.lastMaw = tag.contains("LastMaw") ? tag.getLong("LastMaw") : s.lastMaw;
        s.cityBreached = tag.getBoolean("City");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag am = new ListTag();
        for (var e : ambient.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("K", e.getKey());
            t.putDouble("D", e.getValue()[0]);
            t.putDouble("T", e.getValue()[1]);
            am.add(t);
        }
        tag.put("Ambient", am);
        ListTag rp = new ListTag();
        for (Repair r : repairs) {
            CompoundTag t = new CompoundTag();
            t.putLong("P", r.pos());
            t.put("S", r.state());
            t.putLong("Due", r.due());
            rp.add(t);
        }
        tag.put("Repairs", rp);
        ListTag mw = new ListTag();
        for (Maw m : maws) {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", m.dim());
            t.putLong("P", m.pos());
            t.putLong("Close", m.closeAt());
            t.putLongArray("Roots", m.roots());
            mw.add(t);
        }
        tag.put("Maws", mw);
        tag.putLong("LastMaw", lastMaw);
        tag.putBoolean("City", cityBreached);
        return tag;
    }

    /** A saved block state, read back (null if its block no longer exists). */
    static BlockState readState(HolderLookup.Provider provider, CompoundTag tag) {
        try { return NbtUtils.readBlockState(provider.lookupOrThrow(Registries.BLOCK), tag); } catch (RuntimeException gone) { return null; }
    }
}
