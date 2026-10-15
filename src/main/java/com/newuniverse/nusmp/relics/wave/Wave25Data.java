package com.newuniverse.nusmp.relics.wave;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * Why: the small per-player state of the wave 25 relics (the Game's next thought, the Mimic's lost finger, the beast form and its allowance, the
 * Forgotten Ring's vanishing, the Death Knight's days) lives in the player's persisted data under one key, so it survives relog and death. Every
 * value is a plain counter or an end time, never a stat.
 */
final class Wave25Data {
    private Wave25Data() {}

    private static final String ROOT = "nusmp_w25";

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

    static boolean has(Player p, String key) {
        CompoundTag t = of(p, false);
        return t != null && t.contains(key);
    }

    static long getLong(Player p, String key) {
        CompoundTag t = of(p, false);
        return t == null ? 0L : t.getLong(key);
    }

    static int getInt(Player p, String key) {
        CompoundTag t = of(p, false);
        return t == null ? 0 : t.getInt(key);
    }

    static boolean getBool(Player p, String key) {
        CompoundTag t = of(p, false);
        return t != null && t.getBoolean(key);
    }

    static void putLong(Player p, String key, long v) { of(p, true).putLong(key, v); }

    static void putInt(Player p, String key, int v) { of(p, true).putInt(key, v); }

    static void putBool(Player p, String key, boolean v) { of(p, true).putBoolean(key, v); }

    static void remove(Player p, String key) {
        CompoundTag t = of(p, false);
        if (t != null) t.remove(key);
    }

    // the keys
    static final String GAME_NEXT = "game_next", FINGER_UNTIL = "finger_until", FORM = "form", FORM_ON = "form_on", FORM_LEFT = "form_left", FORM_DAY = "form_day",
            VANISHED = "vanished", ENSNARE_UNTIL = "ensnare_until", SPOON_DAY = "spoon_day", STEADY_UNTIL = "steady_until",
            DK_LAST = "dk_last_day", DK_DAYS = "dk_days", DK_CON = "dk_con", DK_STAGE = "dk_stage", MASK_UNTIL = "mask_until", MASK_TYPE = "mask_type",
            MASK_NAME = "mask_name", BUCKET_DAY = "bucket_day", THOUGHT_AT = "thought_at", MIMIC_MSG = "mimic_msg";
}
