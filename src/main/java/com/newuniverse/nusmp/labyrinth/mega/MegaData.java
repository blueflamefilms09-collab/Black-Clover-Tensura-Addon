package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the WorldSavedData Expansion Tracker of the brief. The Mega-Dungeon is ONE persistent place that grows, so what has been built, in
 * which order, with which theme and in what state has to survive restarts and be reachable from every system without a level in hand. The
 * data lives in the OVERWORLD's data storage (file {@code data/nusmp_mega.dat}) even though the dimension is the Mega-Dungeon: the overworld
 * is always loaded and always present, the storage is the same one the mod's other world-wide records use ({@code nusmp_signatures},
 * {@code nusmp_spirits}), and a lookup from a quest hook or a command never has to wake a dimension. {@link #expansionLevel} counts the
 * sectors that were unsealed (it is what "how far has the dungeon grown" means and what the threat curve reads); {@link #seed} is fixed on
 * first use so the same world always plans the same dungeon. Server thread only (no locking).
 */
public final class MegaData extends SavedData {
    public static final String NAME = "nusmp_mega";

    /** Fixed on first creation: every sector's seed derives from it. */
    public long seed;
    /** Number of sectors that were unsealed (each unsealing generated the next sector). */
    public int expansionLevel;
    private final List<Sector> sectors = new ArrayList<>();

    public MegaData() {}

    public static MegaData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MegaData::new, MegaData::load, null), NAME);
    }

    /** All sectors in id order (live list; callers must {@link #setDirty()} after changing a sector). */
    public List<Sector> sectors() { return sectors; }

    public Sector sector(int id) {
        for (Sector s : sectors) if (s.id == id) return s;
        return null;
    }

    /** 1.55.0: the sectors of one polarity's chain in chain order (a 1.54.0 save has only the Holy chain). */
    public List<Sector> chain(Polarity p) {
        List<Sector> out = new ArrayList<>();
        for (Sector s : sectors) if (s.polarity == p) out.add(s);
        out.sort((a, b) -> Integer.compare(a.chainIndex, b.chainIndex));
        return out;
    }

    /** 1.55.0: the sector of a polarity with this chain index, or null. */
    public Sector chainSector(Polarity p, int chainIndex) {
        for (Sector s : sectors) if (s.polarity == p && s.chainIndex == chainIndex) return s;
        return null;
    }

    /** 1.55.0: the newest sector of a polarity's chain (highest chain index), or null when that road has not been opened. */
    public Sector chainTail(Polarity p) {
        Sector best = null;
        for (Sector s : sectors) if (s.polarity == p && (best == null || s.chainIndex > best.chainIndex)) best = s;
        return best;
    }

    /** The newest sector, or null when none exists yet. */
    public Sector latest() { return sectors.isEmpty() ? null : sectors.get(sectors.size() - 1); }

    /** The sector whose region holds the position, or null. */
    public Sector sectorAt(BlockPos p) {
        for (Sector s : sectors) if (s.state != Sector.State.FAILED && s.containsRegion(p)) return s;
        return null;
    }

    public void add(Sector s) {
        sectors.add(s);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("seed", seed);
        tag.putInt("level", expansionLevel);
        ListTag l = new ListTag();
        for (Sector s : sectors) l.add(s.save());
        tag.put("sectors", l);
        return tag;
    }

    public static MegaData load(CompoundTag tag, HolderLookup.Provider registries) {
        MegaData d = new MegaData();
        d.seed = tag.getLong("seed");
        d.expansionLevel = tag.getInt("level");
        ListTag l = tag.getList("sectors", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) d.sectors.add(Sector.load(l.getCompound(i)));
        return d;
    }
}
