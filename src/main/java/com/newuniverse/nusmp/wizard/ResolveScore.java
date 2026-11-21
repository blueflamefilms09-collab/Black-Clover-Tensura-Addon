package com.newuniverse.nusmp.wizard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * The resolve the questionnaire measured, kept on the player (persistent data, so it is per player and survives death; nothing clears it) for the later rebirth feature to read:
 * {@code nusmp_resolve} is the total of the Resolve answers and {@code nusmp_resolve_max} the best total that sheet allowed, so a reader can turn it into a fraction. Server side only.
 */
public final class ResolveScore {
    private ResolveScore() {}

    public static final String KEY = "nusmp_resolve", KEY_MAX = "nusmp_resolve_max";

    private static CompoundTag root(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return pd.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void set(Player p, int resolve, int max) {
        CompoundTag r = root(p);
        r.putInt(KEY, resolve);
        r.putInt(KEY_MAX, max);
    }

    /** The stored resolve, or -1 if the player never answered. */
    public static int get(Player p) { CompoundTag r = root(p); return r.contains(KEY, 3) ? r.getInt(KEY) : -1; }

    public static int max(Player p) { CompoundTag r = root(p); return r.contains(KEY_MAX, 3) ? r.getInt(KEY_MAX) : -1; }
}
