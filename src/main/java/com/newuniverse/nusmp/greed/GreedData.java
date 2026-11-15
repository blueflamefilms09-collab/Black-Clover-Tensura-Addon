package com.newuniverse.nusmp.greed;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/**
 * Why: there is ONE Black Sword Greed per world, so there is ONE saved record of it, in the Overworld's data storage ({@code nusmp_greed.dat}): whether it has entered the world, the token of the one valid stack, the
 * wielder, the tier the sword has reached (it survives a change of wielder), the form it is in and the last time its wielder was seen (the 14 day release). The item is only a body of it. Server thread only.
 */
public final class GreedData extends SavedData {
    public static final String NAME = "nusmp_greed";

    private static volatile MinecraftServer server;

    private boolean spawned;
    private long token;
    private UUID owner;
    private long lastActiveMs;
    private int tier, form;

    public GreedData() {}

    public static void bind(MinecraftServer s) { server = s; }
    public static boolean bound() { return server != null; }
    public static MinecraftServer server() { return server; }
    public static GreedData get() { return get(server); }

    public static GreedData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(GreedData::new, GreedData::load, null), NAME);
    }

    public boolean spawned() { return spawned; }
    public long token() { return token; }
    public UUID owner() { return owner; }
    public int tier() { return tier; }
    public int form() { return form; }
    public long lastActiveMs() { return lastActiveMs; }

    public boolean isOwner(UUID id) { return owner != null && owner.equals(id); }

    /** A stack is the valid one when the sword exists and its token is the world's. */
    public boolean valid(long stackToken) { return spawned && token != 0L && token == stackToken; }

    /** The sword enters the world (or is made whole again): a fresh token, so every older stack dissolves. Returns the new token. */
    public long mint(java.util.Random rnd) {
        long t;
        do { t = rnd.nextLong(); } while (t == 0L);
        spawned = true;
        token = t;
        setDirty();
        return t;
    }

    public void bindOwner(UUID id, long nowMs) {
        owner = id;
        lastActiveMs = nowMs;
        setDirty();
    }

    /** The wielder lost it (killed by a player, starved, 14 days away): nobody holds it, the tier stays with the sword. */
    public void clearOwner() {
        owner = null;
        setDirty();
    }

    /** The sword is gone from the world (the inactivity release): it may enter again. */
    public void vanish() {
        spawned = false;
        token = 0L;
        owner = null;
        setDirty();
    }

    public void touch(long nowMs) { lastActiveMs = nowMs; setDirty(); }

    public void setTier(int t) {
        int c = Math.max(0, Math.min(GreedRules.MAX_TIER, t));
        if (c != tier) { tier = c; setDirty(); }
    }

    public void setForm(int f) {
        int c = Math.max(0, Math.min(GreedRules.FORMS - 1, f));
        if (c != form) { form = c; setDirty(); }
    }

    static GreedData load(CompoundTag tag, HolderLookup.Provider provider) {
        GreedData d = new GreedData();
        d.spawned = tag.getBoolean("spawned");
        d.token = tag.getLong("token");
        if (tag.hasUUID("owner")) d.owner = tag.getUUID("owner");
        d.lastActiveMs = tag.getLong("lastActive");
        d.tier = Math.max(0, Math.min(GreedRules.MAX_TIER, tag.getInt("tier")));
        d.form = Math.max(0, Math.min(GreedRules.FORMS - 1, tag.getInt("form")));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("spawned", spawned);
        tag.putLong("token", token);
        if (owner != null) tag.putUUID("owner", owner);
        tag.putLong("lastActive", lastActiveMs);
        tag.putInt("tier", tier);
        tag.putInt("form", form);
        return tag;
    }
}
