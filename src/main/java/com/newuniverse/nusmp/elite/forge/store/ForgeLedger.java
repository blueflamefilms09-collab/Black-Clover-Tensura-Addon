package com.newuniverse.nusmp.elite.forge.store;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: {@code EliteHooks.forgeCraftedCount} and {@code forgeBestQuality} are asked by UUID, "so offline data works too", but the real progress lives in an
 * attachment on the player entity which only exists while that player is loaded. So the totals are mirrored into this small world-saved ledger (UUID, items
 * forged, best quality) every time they change and when a player logs in; the hooks read the ledger. It holds two numbers per player and nothing else, tolerant of
 * missing keys, stored with the overworld like the other server-wide books of this mod.
 */
public final class ForgeLedger extends SavedData {
    private static final String NAME = "nusmp_elite_forge";

    private final Map<UUID, int[]> rows = new HashMap<>();

    public static ForgeLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ForgeLedger::new, ForgeLedger::load, null), NAME);
    }

    private static ForgeLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        ForgeLedger l = new ForgeLedger();
        CompoundTag players = tag.contains("players", 10) ? tag.getCompound("players") : new CompoundTag();
        for (String k : players.getAllKeys()) {
            try {
                UUID id = UUID.fromString(k);
                CompoundTag row = players.getCompound(k);
                l.rows.put(id, new int[]{Math.max(0, row.getInt("crafted")), Math.max(0, Math.min(6, row.getInt("best")))});
            } catch (IllegalArgumentException badKey) {
                // a hand-edited key: skip it
            }
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, int[]> e : rows.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putInt("crafted", e.getValue()[0]);
            row.putInt("best", e.getValue()[1]);
            players.put(e.getKey().toString(), row);
        }
        tag.put("players", players);
        return tag;
    }

    /** Records a player's totals; marks the book dirty only when they changed. */
    public void put(UUID id, int crafted, int best) {
        int[] cur = rows.get(id);
        if (cur != null && cur[0] == crafted && cur[1] == best) return;
        rows.put(id, new int[]{crafted, best});
        setDirty();
    }

    public int crafted(UUID id) {
        int[] r = rows.get(id);
        return r == null ? 0 : r[0];
    }

    public int best(UUID id) {
        int[] r = rows.get(id);
        return r == null ? 0 : r[1];
    }
}
