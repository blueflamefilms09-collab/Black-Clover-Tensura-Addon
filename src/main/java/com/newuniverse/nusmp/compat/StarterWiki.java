package com.newuniverse.nusmp.compat;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Why (4.78.0): the owner wants the Multiverse Wiki book in the hands of every new player. At login a player who has not been stamped yet gets one book (into the inventory, dropped at the feet if it is full) and the
 * stamp; the stamp lives in the player's persistent data, so it survives death and relogs and the book is never given twice. Server side, one NBT flag check per login, no ticking, no packets. Server setting
 * {@code starter.starterWiki} (default true) turns it off.
 */
public final class StarterWiki {
    private StarterWiki() {}

    /** The stamp: set once the book was handed out. */
    static final String GIVEN = "anime_multiverse_starter_wiki";

    /** Pure rule: hand out the book only when the setting is on and the player has no stamp yet. */
    static boolean shouldGive(boolean settingOn, CompoundTag persistent) { return settingOn && !persistent.getBoolean(GIVEN); }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean on;
        try { on = NUConfig.STARTER_WIKI.get(); } catch (RuntimeException notLoaded) { on = true; }
        CompoundTag data = p.getPersistentData();
        if (!shouldGive(on, data)) return;
        data.putBoolean(GIVEN, true);
        ItemStack book = new ItemStack(CompatItems.WIKI.get());
        if (!p.getInventory().add(book)) p.drop(book, false);
    }
}
