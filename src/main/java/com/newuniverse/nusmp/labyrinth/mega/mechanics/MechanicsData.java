package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: "state in SavedData only where a mechanic must survive a restart (otherwise rebuilt from the seed)". Exactly two things must survive: which mechanics were already beaten
 * (so a solved puzzle does not re-arm when the party comes back after a restart), and the journals of the blocks a RUNNING mechanic has changed (so a crash or a power cut in the
 * middle of a flood, a collapsed floor or a locked room cannot leave the dungeon altered: at the next start {@link #recover} puts every leftover block back before any player
 * is let near it). Everything else about a mechanic is derived from the sector's seed. Stored in the Overworld's data storage like {@code MegaData}, so it is always loadable.
 * The journals are mirrored from the live instances when the data is written, so the instance never has to remember to save.
 */
public final class MechanicsData extends SavedData {
    public static final String NAME = "nusmp_mechanics";
    private static final int MAX_SOLVED = 20000;

    private final Set<String> solved = new LinkedHashSet<>();
    /** Journals found on disk at load: restored once, then forgotten. */
    private final Map<String, ListTag> leftover = new HashMap<>();
    /** Live instances, whose journals are written on save. */
    private final Map<String, Instance> live = new HashMap<>();

    public static MechanicsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MechanicsData::new, MechanicsData::load, null), NAME);
    }

    public boolean isSolved(String key) { return solved.contains(key); }

    public void markSolved(String key) {
        solved.add(key);
        if (solved.size() > MAX_SOLVED) {
            Iterator<String> it = solved.iterator();
            it.next();
            it.remove();
        }
        setDirty();
    }

    public int solvedCount() { return solved.size(); }

    public void clearSolved() {
        solved.clear();
        setDirty();
    }

    void track(Instance in) {
        live.put(in.key, in);
        setDirty();
    }

    void forget(Instance in) {
        if (live.remove(in.key) != null) setDirty();
    }

    /** True when some live instance has blocks changed (the Director marks the data dirty so the next save mirrors them). */
    boolean anyJournal() {
        for (Instance in : live.values()) for (Journal j : in.journals()) if (!j.isEmpty()) return true;
        return false;
    }

    public int liveCount() { return live.size(); }

    public int leftoverCount() { return leftover.size(); }

    /** Puts back every block a previous run left changed. Returns the number of journals restored. */
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
        ListTag s = new ListTag();
        for (String k : solved) s.add(StringTag.valueOf(k));
        tag.put("solved", s);
        CompoundTag j = new CompoundTag();
        for (Map.Entry<String, ListTag> e : leftover.entrySet()) j.put(e.getKey(), e.getValue().copy());
        for (Map.Entry<String, Instance> e : live.entrySet()) {
            ListTag l = new ListTag();
            for (Journal x : e.getValue().journals()) if (!x.isEmpty()) l.add(x.save());
            if (!l.isEmpty()) j.put(e.getKey(), l);
        }
        tag.put("journals", j);
        return tag;
    }

    public static MechanicsData load(CompoundTag tag, HolderLookup.Provider registries) {
        MechanicsData d = new MechanicsData();
        ListTag s = tag.getList("solved", Tag.TAG_STRING);
        for (int i = 0; i < s.size(); i++) d.solved.add(s.getString(i));
        CompoundTag j = tag.getCompound("journals");
        for (String k : j.getAllKeys()) d.leftover.put(k, j.getList(k, Tag.TAG_COMPOUND));
        return d;
    }

    /** The keys of the journals left on disk (for the verify command). */
    public List<String> leftoverKeys() { return new ArrayList<>(leftover.keySet()); }
}
