package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.ritual.SoulForgeRitual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Why: the Soul Forge screen needs a server-side anchor, exactly as the classic creation screen has {@link AvatarBuilderMenu}: a slotless menu whose
 * existence is the server's proof that "this player really is inside a ritual with the screen open". The net handlers act only for a player whose open
 * container is one of these AND whose ritual carries this menu's token, so a client cannot roll an aptitude or finalize without the forge having opened
 * it. It also gives the ritual its cancel path for free: closing the screen (Esc, a death, a walk away, a disconnect) makes the game call
 * {@link #removed}, which tells the ritual to end and give the player their movement back. The menu holds no items; nothing can be put into it.
 */
public class SoulForgeMenu extends AbstractContainerMenu {
    private final long ritualToken;

    /** The client half: it knows no ritual (the server owns it). */
    public SoulForgeMenu(int id) { this(id, 0L); }

    public SoulForgeMenu(int id, long ritualToken) {
        super(AvatarMenus.SOUL_FORGE.get(), id);
        this.ritualToken = ritualToken;
    }

    /** The token of the ritual that opened this menu (0 on the client). */
    public long ritualToken() { return ritualToken; }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    /** True while the ritual that opened this menu is still running; false closes the screen by itself (the game asks every tick). */
    @Override
    public boolean stillValid(Player player) {
        if (!player.isAlive()) return false;
        return !(player instanceof ServerPlayer sp) || SoulForgeRitual.menuStillValid(sp, this);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) SoulForgeRitual.onMenuRemoved(sp, this);
    }
}
