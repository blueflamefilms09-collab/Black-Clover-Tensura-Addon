package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Where wave 12 keeps its numbers: a weapon's own record (missiles, crystal charges, the bottle's water) lives in the stack's custom data under
 * {@link #KEY}; a wielder's short timers (jaw charges, wet towel, crystal arm, climb) in the player's persisted data under the same key. Both are
 * small compounds read or written only when a weapon is used or ticks. Counters only: no stat or resource of its own.
 */
final class Wave12Data {
    private Wave12Data() {}

    static final String KEY = "nusmp_w12";

    /** The wielder's compound (created on first use); write into it directly. */
    static CompoundTag player(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(KEY, 10)) persisted.put(KEY, new CompoundTag());
        return persisted.getCompound(KEY);
    }

    /** True when the player has any wave 12 state (no allocation: the cheap tick path). */
    static boolean hasPlayerData(Player p) {
        CompoundTag root = p.getPersistentData();
        return root.contains(Player.PERSISTED_NBT_TAG, 10) && root.getCompound(Player.PERSISTED_NBT_TAG).contains(KEY, 10);
    }

    /** A copy of the item's record; changes go back with {@link #save}. */
    static CompoundTag stack(ItemStack s) {
        if (s == null || s.isEmpty()) return new CompoundTag();
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return new CompoundTag();
        CompoundTag all = cd.copyTag();
        return all.contains(KEY, 10) ? all.getCompound(KEY).copy() : new CompoundTag();
    }

    static void save(ItemStack s, CompoundTag record) {
        if (s == null || s.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, s, all -> all.put(KEY, record));
    }

    /** The stack of a relic by id: the one the engine handed to the ability if it is that relic, else the one held in a hand. EMPTY if none. */
    static ItemStack live(ServerPlayer p, String relicId, ItemStack handed) {
        if (isRelic(handed, relicId)) return handed;
        if (isRelic(p.getMainHandItem(), relicId)) return p.getMainHandItem();
        if (isRelic(p.getOffhandItem(), relicId)) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }

    static boolean isRelic(ItemStack s, String relicId) {
        if (s == null || s.isEmpty()) return false;
        Optional<RelicDef> d = Relics.of(s);
        return d.isPresent() && relicId.equals(d.get().id());
    }

    /** The id of the relic in the player's main hand, or "". */
    static String heldId(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() ? "" : Relics.of(s).map(RelicDef::pairBaseId).orElse("");
    }
}
