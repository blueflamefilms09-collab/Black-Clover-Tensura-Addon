package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.mechanics.Journal;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a puzzle changes the floor of a room (tiles, pillars) while it is armed, and the dungeon must come back exactly as it was even if the server dies in the middle (a power cut with a
 * lights-out grid on the floor would otherwise leave coloured tiles in a hall for ever). The journals of the running puzzles are mirrored here whenever the data is written, and at the
 * next start {@link #recover} puts every block back before any player is let near. Nothing else is saved: whether a puzzle was solved is remembered in the mechanics data
 * ({@code MechanicsData.markSolved}, key {@code puzzle:<sector>:<room>:<type>}), everything else is rebuilt from the sector's seed.
 */
public final class PuzzleData extends SavedData {
    public static final String NAME = "nusmp_puzzles";

    private final Map<String, ListTag> leftover = new HashMap<>();
    private final Map<String, PuzzleInstance> live = new HashMap<>();

    public static PuzzleData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(PuzzleData::new, PuzzleData::load, null), NAME);
    }

    void track(PuzzleInstance in) {
        live.put(in.key, in);
        setDirty();
    }

    void forget(PuzzleInstance in) {
        if (live.remove(in.key) != null) setDirty();
    }

    boolean anyJournal() {
        for (PuzzleInstance in : live.values()) if (!in.journal().isEmpty()) return true;
        return false;
    }

    public int liveCount() { return live.size(); }

    public int leftoverCount() { return leftover.size(); }

    /** Puts back every block a previous run left changed; returns the number of journals restored. */
    public int recover(ServerLevel level) {
        int n = 0;
        for (ListTag l : new ArrayList<>(leftover.values())) {
            for (int i = 0; i < l.size(); i++) {
                Journal.load(level, l.getCompound(i)).restore();
                n++;
            }
        }
        if (!leftover.isEmpty()) {
            leftover.clear();
            setDirty();
        }
        return n;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag j = new CompoundTag();
        for (Map.Entry<String, ListTag> e : leftover.entrySet()) j.put(e.getKey(), e.getValue().copy());
        for (Map.Entry<String, PuzzleInstance> e : live.entrySet()) {
            ListTag l = new ListTag();
            if (!e.getValue().journal().isEmpty()) l.add(e.getValue().journal().save());
            if (!l.isEmpty()) j.put(e.getKey(), l);
        }
        tag.put("journals", j);
        return tag;
    }

    public static PuzzleData load(CompoundTag tag, HolderLookup.Provider registries) {
        PuzzleData d = new PuzzleData();
        CompoundTag j = tag.getCompound("journals");
        for (String k : j.getAllKeys()) d.leftover.put(k, j.getList(k, Tag.TAG_COMPOUND));
        return d;
    }

    /** The keys of the journals left on disk (for the verify command). */
    public List<String> leftoverKeys() { return new ArrayList<>(leftover.keySet()); }
}
