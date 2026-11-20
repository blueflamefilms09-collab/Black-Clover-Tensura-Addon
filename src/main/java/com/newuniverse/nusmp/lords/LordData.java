package com.newuniverse.nusmp.lords;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/**
 * Why: each of the four Mortal Sin weapons exists ONCE per world (like the Black Sword Greed), so there is ONE saved record per weapon, in the Overworld's data storage ({@code nusmp_lords.dat}): whether it has entered
 * the world, the token of the one valid stack, the wielder, the form it was last in (a respawn returns it in that form) and the last time its wielder was seen (the 14 day release). The item is only a body of it.
 * Server thread only.
 */
public final class LordData extends SavedData {
    public static final String NAME = "nusmp_lords";
    private static final int N = LordRules.LORDS;

    private static volatile MinecraftServer server;

    private final boolean[] spawned = new boolean[N];
    private final long[] token = new long[N], lastActiveMs = new long[N];
    private final UUID[] owner = new UUID[N];
    private final int[] form = new int[N];

    public LordData() {}

    public static void bind(MinecraftServer s) { server = s; }
    public static boolean bound() { return server != null; }
    public static MinecraftServer server() { return server; }
    public static LordData get() { return get(server); }

    public static LordData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LordData::new, LordData::load, null), NAME);
    }

    private static int i(LordRules.Sin s) { return s.ordinal(); }

    public boolean spawned(LordRules.Sin s) { return spawned[i(s)]; }
    public long token(LordRules.Sin s) { return token[i(s)]; }
    public UUID owner(LordRules.Sin s) { return owner[i(s)]; }
    public int form(LordRules.Sin s) { return form[i(s)]; }
    public long lastActiveMs(LordRules.Sin s) { return lastActiveMs[i(s)]; }
    public boolean isOwner(LordRules.Sin s, UUID id) { return owner[i(s)] != null && owner[i(s)].equals(id); }

    /** A stack is the valid one when the weapon exists and its token is the world's. */
    public boolean valid(LordRules.Sin s, long stackToken) { return spawned[i(s)] && token[i(s)] != 0L && token[i(s)] == stackToken; }

    /** The weapon enters the world (or is made whole again): a fresh token, so every older stack dissolves. Returns the new token. */
    public long mint(LordRules.Sin s, java.util.Random rnd) {
        long t;
        do { t = rnd.nextLong(); } while (t == 0L);
        spawned[i(s)] = true;
        token[i(s)] = t;
        setDirty();
        return t;
    }

    public void bindOwner(LordRules.Sin s, UUID id, long nowMs) {
        owner[i(s)] = id;
        lastActiveMs[i(s)] = nowMs;
        setDirty();
    }

    /** The wielder lost it: nobody holds it. */
    public void clearOwner(LordRules.Sin s) {
        owner[i(s)] = null;
        setDirty();
    }

    /** The weapon is gone from the world (the inactivity release or an operator's reset): it may enter again. */
    public void vanish(LordRules.Sin s) {
        spawned[i(s)] = false;
        token[i(s)] = 0L;
        owner[i(s)] = null;
        setDirty();
    }

    public void touch(LordRules.Sin s, long nowMs) { lastActiveMs[i(s)] = nowMs; setDirty(); }

    public void setForm(LordRules.Sin s, int f) {
        int c = Math.max(0, Math.min(s.forms() - 1, f));
        if (c != form[i(s)]) { form[i(s)] = c; setDirty(); }
    }

    static LordData load(CompoundTag tag, HolderLookup.Provider provider) {
        LordData d = new LordData();
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            CompoundTag t = tag.getCompound(s.id);
            int k = i(s);
            d.spawned[k] = t.getBoolean("spawned");
            d.token[k] = t.getLong("token");
            if (t.hasUUID("owner")) d.owner[k] = t.getUUID("owner");
            d.lastActiveMs[k] = t.getLong("lastActive");
            d.form[k] = Math.max(0, Math.min(s.forms() - 1, t.getInt("form")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            int k = i(s);
            CompoundTag t = new CompoundTag();
            t.putBoolean("spawned", spawned[k]);
            t.putLong("token", token[k]);
            if (owner[k] != null) t.putUUID("owner", owner[k]);
            t.putLong("lastActive", lastActiveMs[k]);
            t.putInt("form", form[k]);
            tag.put(s.id, t);
        }
        return tag;
    }
}
