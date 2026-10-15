package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Why: a timer must live in saved data (the wave's fairness rule), and the two sets of keys must not collide with another wave's.
 * <p>
 * Wave 30: the small state the wave keeps, in two places. Per player, in the persistent data under one key (it survives relog and death): wish
 * clocks, the Soul Stone's souls, the stars of the Deck, pending boons. Per stack, in the custom data of the relic (the deck's seed and cards drawn,
 * the grafting of Vecna's relics, the Key's known doors). Every value is a plain counter, an end time or an id: never a stat.
 */
final class Wave30Data {
    private Wave30Data() {}

    private static final String ROOT = "nusmp_w30";

    // ---------------------------------------------------------------- the player's compound
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

    static String getString(Player p, String key) {
        CompoundTag t = of(p, false);
        return t == null ? "" : t.getString(key);
    }

    static void putLong(Player p, String key, long v) { of(p, true).putLong(key, v); }

    static void putInt(Player p, String key, int v) { of(p, true).putInt(key, v); }

    static void putString(Player p, String key, String v) { of(p, true).putString(key, v); }

    static void remove(Player p, String key) {
        CompoundTag t = of(p, false);
        if (t != null) t.remove(key);
    }

    // player keys
    static final String SURPRISE_READY = "surprise_ready", WISH_LAST = "wish_last_ms", WISH_PICK = "wish_pick", GREAT_PICK = "great_pick",
            GREAT_DAY = "great_day", SNAP_DAY = "snap_day", FORESIGHT_UNTIL = "foresight_until", STARS = "stars", COMET_UNTIL = "comet_until",
            HUNGER_UNTIL = "hunger_until", SOULS = "souls", SOUL_DAY = "soul_day", SOUL_NAMES = "soul_names", WARD_UNTIL = "ward_until",
            LAST_HIT = "last_hit", LAST_HIT_AT = "last_hit_at", MARK_DIM = "mark_dim", MARK_X = "mark_x", MARK_Y = "mark_y", MARK_Z = "mark_z",
            TITLE = "title", DECK_DAY = "deck_day", DECK_USED = "deck_used";

    // ---------------------------------------------------------------- the stack's compound (custom data of the relic)
    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    static void edit(ItemStack s, java.util.function.Consumer<CompoundTag> change) { CustomData.update(DataComponents.CUSTOM_DATA, s, change); }

    static long stackLong(ItemStack s, String key) { return tag(s).getLong(key); }

    static int stackInt(ItemStack s, String key) { return tag(s).getInt(key); }

    static String stackString(ItemStack s, String key) { return tag(s).getString(key); }

    static void putStackLong(ItemStack s, String key, long v) { edit(s, t -> t.putLong(key, v)); }

    static void putStackInt(ItemStack s, String key, int v) { edit(s, t -> t.putInt(key, v)); }

    static void putStackString(ItemStack s, String key, String v) { edit(s, t -> t.putString(key, v)); }

    static void removeStack(ItemStack s, String key) { edit(s, t -> t.remove(key)); }

    // stack keys
    static final String GRAFT = "w30_graft", DECK_SEED = "w30_deck_seed", DECK_DRAWN = "w30_deck_drawn";
}
