package com.newuniverse.nusmp.dragonballs;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the words of the ritual are taught ball by ball (docs/dragon_ball_words.md), so the world has to remember, per player UUID, which balls of each set that player has already found. One small
 * int mask per set (one bit per star). World saved data {@code nusmp_dragonball_words}. Read or written only when a ball is found or the player asks; never per tick. Bounded: at most
 * {@link #MAX_PLAYERS} rows (the oldest row is dropped to make room, so a damaged file or a huge server can not grow it).
 */
final class DbLessons extends SavedData {
    static final int MAX_PLAYERS = 4096;

    private final Map<UUID, int[]> rows = new LinkedHashMap<>();

    static DbLessons get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DbLessons::new, DbLessons::load, null), "nusmp_dragonball_words");
    }

    private static DbLessons load(CompoundTag t, HolderLookup.Provider p) {
        DbLessons l = new DbLessons();
        ListTag list = t.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && l.rows.size() < MAX_PLAYERS; i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) continue;
            int[] masks = new int[BallSet.values().length];
            boolean any = false;
            for (BallSet s : BallSet.values()) {
                masks[s.ordinal()] = c.getInt(s.id()) & 0xFF;
                any |= masks[s.ordinal()] != 0;
            }
            if (any) l.rows.put(c.getUUID("id"), masks);
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider p) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, int[]> e : rows.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            for (BallSet s : BallSet.values()) c.putInt(s.id(), e.getValue()[s.ordinal()]);
            list.add(c);
        }
        t.put("players", list);
        return t;
    }

    /** The mask of the balls of {@code set} this player has found (0 = none). */
    int mask(UUID id, BallSet set) {
        int[] r = id == null ? null : rows.get(id);
        return r == null ? 0 : r[set.ordinal()];
    }

    /** The player finds this ball. Returns how many balls of the set they have found now, or 0 when they had already found this one (nothing written). */
    int learn(UUID id, BallSet set, int star) {
        if (id == null || star < 1 || star > 8) return 0;
        int[] r = rows.get(id);
        if (r == null) {
            if (rows.size() >= MAX_PLAYERS) {
                Iterator<UUID> it = rows.keySet().iterator();
                it.next();
                it.remove();
            }
            r = new int[BallSet.values().length];
            rows.put(id, r);
        }
        if (PhraseLessons.has(r[set.ordinal()], star)) return 0;
        r[set.ordinal()] = PhraseLessons.with(r[set.ordinal()], star);
        setDirty();
        return PhraseLessons.count(r[set.ordinal()]);
    }

    /** An operator wipes what a player has learned (all sets). */
    boolean forget(UUID id) {
        boolean had = id != null && rows.remove(id) != null;
        if (had) setDirty();
        return had;
    }

    int playerRows() { return rows.size(); }
}
