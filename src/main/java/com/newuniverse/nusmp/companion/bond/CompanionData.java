package com.newuniverse.nusmp.companion.bond;

import com.newuniverse.nusmp.companion.CompanionRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.regex.Pattern;

/**
 * Why: what the PLAYER has with each companion (rapport, the day of the last high five, how many loved gifts today) is kept on the player, in their persistent data, keyed by the companion's bond key: it then
 * survives the companion dying, being replaced, or the player changing servers of the same world, and there is no static "current player" anywhere. It lives in the part of the persistent data the game copies on
 * death ({@code Player.PERSISTED_NBT_TAG}, the same place the mod's other per-player records use). Nothing is kept in memory, so there is nothing to clean up on logout. A key that is not a plain lower-case name is refused.
 */
public final class CompanionData {
    private CompanionData() {}

    public static final String ROOT = "nusmp_companion";
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]{1,48}");

    public static boolean validKey(String key) { return key != null && KEY.matcher(key).matches(); }

    /** The compound of one companion on one player (created if missing), or null for an invalid key. Writing to it writes to the player. */
    public static CompoundTag entry(Player p, String key) {
        if (p == null || !validKey(key)) return null;
        CompoundTag persistent = p.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, 10)) persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT, 10)) persisted.put(ROOT, new CompoundTag());
        CompoundTag root = persisted.getCompound(ROOT);
        if (!root.contains(key, 10)) root.put(key, new CompoundTag());
        return root.getCompound(key);
    }

    public static int rapport(Player p, String key) {
        CompoundTag e = entry(p, key);
        return e == null ? 0 : CompanionRules.clampRapport(e.getInt("rapport"));
    }

    /** Adds to the rapport (which may be negative); returns the new value. */
    public static int addRapport(Player p, String key, int n) {
        CompoundTag e = entry(p, key);
        if (e == null) return 0;
        int now = CompanionRules.addRapport(CompanionRules.clampRapport(e.getInt("rapport")), n);
        e.putInt("rapport", now);
        return now;
    }

    /** The day of the last high five with this companion, or -1 if never. */
    public static long lastFiveDay(Player p, String key) {
        CompoundTag e = entry(p, key);
        return e == null || !e.contains("fiveDay") ? -1L : e.getLong("fiveDay");
    }

    public static void setFiveDay(Player p, String key, long day) {
        CompoundTag e = entry(p, key);
        if (e != null) e.putLong("fiveDay", day);
    }

    /** How many loved gifts this companion took from the player on {@code today}. */
    public static int lovedToday(Player p, String key, long today) {
        CompoundTag e = entry(p, key);
        return e == null ? 0 : CompanionRules.lovedToday(e.contains("lovedDay") ? e.getLong("lovedDay") : -1L, e.getInt("lovedN"), today);
    }

    public static void setLovedToday(Player p, String key, long today, int count) {
        CompoundTag e = entry(p, key);
        if (e == null) return;
        e.putLong("lovedDay", today);
        e.putInt("lovedN", Math.max(0, count));
    }
}
