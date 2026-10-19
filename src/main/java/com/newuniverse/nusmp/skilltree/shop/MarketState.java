package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the shelf is a pure function of the world seed, the restock period and a restock counter (see {@link StockBoard}), so the world has to remember only the part that is
 * NOT computable: which restock it is on and how many copies of each slot have been bought since. That is a world-wide fact (one witch, one shelf, the same for everybody), so it
 * is a {@link SavedData} in the overworld's data storage, the NeoForge/Minecraft home of per-world state, not a per-player attachment. Storing only counts (not prices or whole
 * stacks) keeps the file a few dozen bytes, can never disagree with the data file about what a book costs, and an old world has nothing to migrate: with no file the shelf
 * simply starts at period 0 with nothing bought. Server thread only.
 */
public final class MarketState extends SavedData {
    /** The file name under the overworld's {@code data} folder. */
    public static final String NAME = "nusmp_black_market";

    private long period = -1;
    private int epoch;
    private final Map<String, Integer> bought = new LinkedHashMap<>();

    /** The world's market (created on first use). */
    public static MarketState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MarketState::new, MarketState::load, null), NAME);
    }

    private static MarketState load(CompoundTag tag, HolderLookup.Provider provider) {
        MarketState s = new MarketState();
        s.period = tag.contains("Period", Tag.TAG_LONG) ? tag.getLong("Period") : -1;
        s.epoch = Math.max(0, tag.getInt("Epoch"));
        if (tag.contains("Bought", Tag.TAG_COMPOUND)) {
            CompoundTag b = tag.getCompound("Bought");
            for (String k : b.getAllKeys()) if (b.getInt(k) > 0) s.bought.put(k, b.getInt(k));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("Period", period);
        tag.putInt("Epoch", epoch);
        CompoundTag b = new CompoundTag();
        for (Map.Entry<String, Integer> e : bought.entrySet()) b.putInt(e.getKey(), e.getValue());
        tag.put("Bought", b);
        return tag;
    }

    /** The restock period the purchases below belong to (-1 before the first visit). */
    public long period() { return period; }

    /** The operator's restock counter (see {@code /multiverse skillbook shop restock}). */
    public int epoch() { return epoch; }

    /** Moves to the restock period {@code now}; a new period forgets the purchases of the old one. Returns true when the shelf was restocked. */
    public boolean syncPeriod(long now) {
        if (now == period) return false;
        period = now;
        bought.clear();
        setDirty();
        return true;
    }

    /** Copies of the slot bought since the last restock. */
    public int bought(String slotId) {
        return bought.getOrDefault(slotId, 0);
    }

    public void addBought(String slotId, int n) {
        if (n <= 0) return;
        bought.merge(slotId, n, Integer::sum);
        setDirty();
    }

    /** The operator's restock: a new shelf at once (new prices, new counts, nothing bought). */
    public void restock() {
        epoch++;
        bought.clear();
        setDirty();
    }

    /** A copy of the purchases, for the status command and the self-test. */
    public Map<String, Integer> boughtView() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(bought));
    }

    /** Replaces the purchases (the self-test puts the world back as it found it). */
    public void replaceBought(Map<String, Integer> to) {
        bought.clear();
        bought.putAll(to);
        setDirty();
    }
}
