package com.newuniverse.nusmp.labyrinth.mega.weekly;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Why: pays the first-clear prize ({@link FirstClearRules}) to every player standing in a sector when its Guardian falls, once per player and sector, remembered in the player's own saved
 * data (so it follows the player, not the server). Worst case per Guardian death: one pass over the players of the dungeon dimension, at most one item stack and one experience award each, no
 * entity, no packet beyond the vanilla item and level ones, nothing per tick.
 */
public final class FirstClearListener implements SectorListener {
    static final String KEY = "anime_multiverse_first_clear";

    @Override
    public void onGuardianDefeated(ServerLevel level, Sector sector) {
        if (!WeeklyConfig.get(WeeklyConfig.FIRST_CLEAR) || sector == null) return;
        for (ServerPlayer p : level.players()) {
            try {
                Sector at = MegaDungeon.sectorAt(level, p.blockPosition());
                if (at == null || at.id != sector.id || !p.isAlive()) continue;
                pay(p, sector.id, Math.max(1, sector.lastFloor()));
            } catch (RuntimeException ex) {
                // a prize is a bonus: one player's fault must not stop the others or the Guardian's own death
            }
        }
    }

    /** Pays the prize to one player if the sector is new to their record; true when paid. */
    public static boolean pay(ServerPlayer p, int sectorId, int floor) {
        CompoundTag data = p.getPersistentData();
        int[] next = FirstClearRules.claim(data.contains(KEY) ? data.getIntArray(KEY) : null, sectorId);
        if (next == null) return false;
        data.putIntArray(KEY, next);
        p.giveExperienceLevels(FirstClearRules.levels(floor));
        ResourceLocation id = ResourceLocation.tryParse(WeeklyConfig.get(WeeklyConfig.FIRST_CLEAR_ITEM));
        Item item = id == null ? Items.DIAMOND : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.DIAMOND);
        ItemStack stack = new ItemStack(item, FirstClearRules.items(floor));
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        p.displayClientMessage(Component.literal("First clear of this Guardian: +" + FirstClearRules.levels(floor) + " levels and " + FirstClearRules.items(floor) + " " + stack.getHoverName().getString() + "."), true);
        return true;
    }
}
