package com.newuniverse.nusmp.beastwill;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Why: the "no duplicate rewards" half of acquisition (3.45.0). Every core carries a serial and every direct offer has a key; the first time one is absorbed the ledger remembers it, so a duplicated stack
 * (a dupe exploit, a creative copy of an used core) can not be absorbed a second time. The ledger lives in the Overworld's data storage ({@code nusmp_beastwill_ledger.dat}), is bounded
 * ({@link #CAPACITY}; the oldest entry is forgotten first, so the worst case is a very old copy being accepted once) and is touched only when a core is absorbed, never per tick. Server thread only.
 */
public final class BeastWillLedger extends SavedData {
    public static final String NAME = "nusmp_beastwill_ledger";
    public static final int CAPACITY = 4096;

    private final Set<String> used = new LinkedHashSet<>();

    public BeastWillLedger() {}

    public static BeastWillLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(BeastWillLedger::new, BeastWillLedger::load, null), NAME);
    }

    public boolean has(String key) { return key != null && used.contains(key); }

    /** Remembers the key; false (nothing changed) when it was already used or is not a key. */
    public boolean consume(String key) {
        if (key == null || key.isEmpty() || key.length() > 120) return false;
        if (!used.add(key)) return false;
        Iterator<String> it = used.iterator();
        while (used.size() > CAPACITY && it.hasNext()) {
            it.next();
            it.remove();
        }
        setDirty();
        return true;
    }

    public int size() { return used.size(); }

    static BeastWillLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        BeastWillLedger l = new BeastWillLedger();
        ListTag list = tag.getList("used", Tag.TAG_STRING);
        for (int i = 0; i < list.size() && l.used.size() < CAPACITY; i++) {
            String s = list.getString(i);
            if (!s.isEmpty() && s.length() <= 120) l.used.add(s);
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (String s : used) list.add(StringTag.valueOf(s));
        tag.put("used", list);
        return tag;
    }
}
