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
 * Where wave 5 keeps its numbers. An item's own record (charges, souls, energy, the Safeguarding record) lives in the stack's custom-data
 * component under {@link #KEY}, so it travels with the amulet; a wearer's short timers (the veil, a shroud, the dragon form) live in the
 * player's persisted data under the same key. Both are small compounds and are read or written only when a relic is used or ticks.
 */
final class Wave5Data {
    private Wave5Data() {}

    static final String KEY = "nusmp_w5";

    // ---------------------------------------------------------------- the wearer
    /** The wearer's own compound (created on first use). It is the live tag: write into it directly. */
    static CompoundTag player(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(KEY, 10)) persisted.put(KEY, new CompoundTag());
        return persisted.getCompound(KEY);
    }

    /** Is there any wave 5 state on this player at all (no allocation: used by the cheap tick path)? */
    static boolean hasPlayerData(Player p) {
        CompoundTag root = p.getPersistentData();
        return root.contains(Player.PERSISTED_NBT_TAG, 10) && root.getCompound(Player.PERSISTED_NBT_TAG).contains(KEY, 10);
    }

    // ---------------------------------------------------------------- the item
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

    /**
     * The stack of a relic (by id): the one the engine handed to the ability when it is that relic, else the one the wearer has equipped (read from
     * the engine's short-lived snapshot, which holds the real stacks, so a write reaches the item). EMPTY when there is none.
     */
    static ItemStack live(ServerPlayer p, String relicId, ItemStack handed) {
        if (isRelic(handed, relicId)) return handed;
        for (Relics.Worn w : Relics.wornList(p)) {
            if (w.def().id().equals(relicId) && !w.stack().isEmpty()) return w.stack();
        }
        return ItemStack.EMPTY;
    }

    static ItemStack live(ServerPlayer p, String relicId) { return live(p, relicId, ItemStack.EMPTY); }

    private static boolean isRelic(ItemStack s, String relicId) {
        if (s == null || s.isEmpty()) return false;
        Optional<RelicDef> d = Relics.of(s);
        return d.isPresent() && relicId.equals(d.get().id());
    }
}
