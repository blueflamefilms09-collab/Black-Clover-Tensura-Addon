package com.newuniverse.nusmp.relics.wave;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * Wave 18: the small per-player state the charms keep (the coin's fortune, the dial's setting, the mirror's open window), in the player's persistent
 * data under one key so it survives relog and death. Every value is a plain counter or an end time, never a stat.
 */
final class Wave18Data {
    private Wave18Data() {}

    private static final String ROOT = "nusmp_w18";

    /** The wave's compound in the player's persisted data; created when {@code create}, else null when there is none. */
    static CompoundTag of(Player p, boolean create) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            persisted.put(ROOT, new CompoundTag());
        }
        return persisted.getCompound(ROOT);
    }

    static long getLong(Player p, String key) {
        CompoundTag t = of(p, false);
        return t == null ? 0L : t.getLong(key);
    }

    static int getInt(Player p, String key) {
        CompoundTag t = of(p, false);
        return t == null ? 0 : t.getInt(key);
    }

    static void putLong(Player p, String key, long v) { of(p, true).putLong(key, v); }

    static void putInt(Player p, String key, int v) { of(p, true).putInt(key, v); }

    static void remove(Player p, String key) {
        CompoundTag t = of(p, false);
        if (t != null) t.remove(key);
    }

    // the keys
    static final String FORTUNE = "fortune", FORTUNE_UNTIL = "fortune_until", DIAL_TYPE = "dial_type", DIAL_UNTIL = "dial_until", MIRROR_UNTIL = "mirror_until";
}
