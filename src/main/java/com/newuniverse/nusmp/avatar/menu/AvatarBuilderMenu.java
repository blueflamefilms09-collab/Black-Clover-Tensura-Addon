package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.net.RaceEntryFactory;
import com.newuniverse.nusmp.avatar.net.S2C_RacesPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the creation screen needs a server-side anchor. This menu has no slots and no logic; it exists so the server can tell "this player really has
 * the creation screen open" (opened from the Genesis Core) before it honours a roll or a create request, and so the screen closes by itself when the
 * player walks away or dies. {@link #open} also sends the filtered race list.
 */
public class AvatarBuilderMenu extends AbstractContainerMenu {
    public AvatarBuilderMenu(int id) { super(AvatarMenus.BUILDER.get(), id); }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) { return player.isAlive(); }

    /** Opens the creation screen for a player (the Genesis Core item calls this) and sends the race list. Does nothing when the Avatar system is off. */
    public static void open(ServerPlayer player) {
        if (!AvatarConfig.enabled()) {
            player.displayClientMessage(Component.translatable("nusmp.avatar.gui.disabled"), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AvatarBuilderMenu(id), Component.translatable("nusmp.avatar.gui.title")));
        PacketDistributor.sendToPlayer(player, new S2C_RacesPayload(RaceEntryFactory.build(player)));
    }
}
