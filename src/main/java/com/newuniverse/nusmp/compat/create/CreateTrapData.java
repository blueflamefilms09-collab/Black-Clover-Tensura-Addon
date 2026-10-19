package com.newuniverse.nusmp.compat.create;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the traps that were planned, built or dropped, and the sectors that were already planned (so a sector is planned once, never twice, and a repair that regenerates a
 * sector's pieces does not roll the dice again). Kept in the Overworld's data storage like {@code MegaData}, so it is always loadable; with Create absent nothing ever writes
 * to it and the file is not even created. The traps themselves are blocks in the world; this is only the index the runtime walks.
 */
public final class CreateTrapData extends SavedData {
    public static final String NAME = "nusmp_create_traps";
    private static final int MAX_SITES = 20000;

    private final Map<String, TrapSite> sites = new LinkedHashMap<>();
    private final Set<Integer> plannedSectors = new HashSet<>();

    public static CreateTrapData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CreateTrapData::new, CreateTrapData::load, null), NAME);
    }

    public Map<String, TrapSite> sites() { return sites; }

    public TrapSite site(String id) { return sites.get(id); }

    public boolean planned(int sector) { return plannedSectors.contains(sector); }

    public void markPlanned(int sector) {
        plannedSectors.add(sector);
        setDirty();
    }

    public void unmarkPlanned(int sector) {
        plannedSectors.remove(sector);
        setDirty();
    }

    /** Adds a site (once per id); a full table stops taking new ones. */
    public boolean add(TrapSite s) {
        if (sites.containsKey(s.id) || sites.size() >= MAX_SITES) return false;
        sites.put(s.id, s);
        setDirty();
        return true;
    }

    public List<TrapSite> in(TrapSite.State state) {
        List<TrapSite> out = new ArrayList<>();
        for (TrapSite s : sites.values()) if (s.state == state) out.add(s);
        return out;
    }

    public int count(TrapSite.State state) {
        int n = 0;
        for (TrapSite s : sites.values()) if (s.state == state) n++;
        return n;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag l = new ListTag();
        for (TrapSite s : sites.values()) l.add(s.save());
        tag.put("sites", l);
        tag.putIntArray("planned", plannedSectors.stream().mapToInt(Integer::intValue).toArray());
        return tag;
    }

    public static CreateTrapData load(CompoundTag tag, HolderLookup.Provider registries) {
        CreateTrapData d = new CreateTrapData();
        ListTag l = tag.getList("sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            TrapSite s = TrapSite.load(l.getCompound(i));
            if (s != null) d.sites.put(s.id, s);
        }
        for (int v : tag.getIntArray("planned")) d.plannedSectors.add(v);
        return d;
    }
}
