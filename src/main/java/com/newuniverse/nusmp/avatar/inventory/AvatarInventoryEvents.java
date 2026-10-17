package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Why: the creator opens the Avatar's inventory by sneaking and right-clicking it with an empty hand. The relic system (1.50.0) already uses that same gesture to
 * take a relic back from a named mob, and it would answer first, so this listener runs ahead of it (high priority), opens the window instead and ends the
 * interaction. Nothing is lost: the trinket slots of the window are that same relic pack, so a relic is taken back from there. A plain right-click (no sneak) still
 * opens the action menu, in {@code AvatarEntity.mobInteract}. Only the Avatar's creator is affected.
 */
public final class AvatarInventoryEvents {
    private AvatarInventoryEvents() {}

    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        try {
            if (e.getLevel().isClientSide || e.getHand() != InteractionHand.MAIN_HAND) return;
            if (!(e.getTarget() instanceof AvatarEntity avatar) || !(e.getEntity() instanceof ServerPlayer player)) return;
            if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || !avatar.isCreator(player)) return;
            AvatarInventoryMenu.open(player, avatar);
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
        } catch (RuntimeException soft) { /* an interaction must never throw: the vanilla path takes over */ }
    }
}
