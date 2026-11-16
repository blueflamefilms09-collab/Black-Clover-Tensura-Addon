package com.newuniverse.nusmp.bossdrops;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: a first-kill-only row ({@link BossDropTable.Row#firstKillOnly()}) must remember WHO already killed WHICH boss, across restarts. Saved with the world in the Overworld's data storage as
 * {@code nusmp_bossdrops}: per player UUID, the set of boss ids that player has killed (so the first-kill bonus is per player, never a static "current player"). Size is at most players x 23 short strings;
 * read and written only on a boss death (no per-tick work). Separate from the Charter lock on purpose: that ledger is the world-wide grimoire claim and is never touched here.
 */
public final class BossDropLedger extends SavedData {
    public static final String NAME = "nusmp_bossdrops";
    private final Map<UUID, Set<String>> killed = new HashMap<>();

    public static BossDropLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(BossDropLedger::new, BossDropLedger::load, null), NAME);
    }

    /** True for the call that records this player's first kill of the boss, false when it was recorded before. */
    public boolean firstKill(UUID player, String boss) {
        if (!killed.computeIfAbsent(player, k -> new HashSet<>()).add(boss)) return false;
        setDirty();
        return true;
    }

    public boolean has(UUID player, String boss) {
        Set<String> s = killed.get(player);
        return s != null && s.contains(boss);
    }

    static BossDropLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        BossDropLedger l = new BossDropLedger();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            UUID id;
            try { id = UUID.fromString(key); } catch (IllegalArgumentException bad) { continue; }
            Set<String> set = new HashSet<>();
            for (Tag t : players.getList(key, Tag.TAG_STRING)) set.add(t.getAsString());
            l.killed.put(id, set);
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> e : killed.entrySet()) {
            ListTag list = new ListTag();
            for (String s : e.getValue()) list.add(StringTag.valueOf(s));
            players.put(e.getKey().toString(), list);
        }
        tag.put("players", players);
        return tag;
    }
}
