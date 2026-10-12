package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
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

/**
 * The collapses that are under way, saved with the world (in the Overworld's data storage as "nusmp_collapse"): where each is centred, the
 * ground it covers, when it began (the Dungeon's game time), the exits and the Root Gates at them, how far each of its jobs has got and who
 * was inside, so a restart carries on where it stopped. The layout of one record is {@link CollapseCodec}; a record that cannot be read
 * is dropped with a log line and the others load.
 */
public final class CollapseSave extends SavedData {
    public static final String NAME = "nusmp_collapse";

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private final Map<Long, CollapseState> states = new LinkedHashMap<>();
    private long nextId = 1L;

    public static CollapseSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CollapseSave::new, CollapseSave::load, null), NAME);
    }

    /** A number no collapse of this world has had. */
    public long takeId() {
        long id = nextId++;
        setDirty();
        return id;
    }

    public void add(CollapseState s) {
        states.put(s.id, s);
        setDirty();
    }

    public void remove(long id) {
        if (states.remove(id) != null) setDirty();
    }

    /** The collapses that are under way, oldest first. */
    public List<CollapseState> all() { return new ArrayList<>(states.values()); }

    static CollapseSave load(CompoundTag tag, HolderLookup.Provider provider) {
        CollapseSave s = new CollapseSave();
        s.nextId = Math.max(1L, tag.getLong("Next"));
        ListTag list = tag.getList("Collapses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            CollapseState c = CollapseCodec.decode(t.getIntArray("I"), t.getLongArray("L"));
            if (c == null) {
                LOG.warn("[nusmp] A saved Treasury Collapse (record {}) is damaged and was dropped", i);
                continue;
            }
            s.nextId = Math.max(s.nextId, c.id + 1);
            s.states.put(c.id, c);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("Next", nextId);
        ListTag list = new ListTag();
        for (CollapseState c : states.values()) {
            CompoundTag t = new CompoundTag();
            t.putIntArray("I", CollapseCodec.ints(c));
            t.putLongArray("L", CollapseCodec.longs(c));
            list.add(t);
        }
        tag.put("Collapses", list);
        return tag;
    }
}
