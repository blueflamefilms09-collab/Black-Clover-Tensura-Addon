package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * Wave 6: Neket's Practical Mage Robe, "Lost and Found": in a fight, an item that leaves the wearer's hands without the wearer sneaking (a tossed
 * weapon, a knocked-loose tool) is handed straight back to the inventory instead of landing on the floor. Kept in its own class and its own listener
 * so the one event type it depends on (ItemTossEvent) can be dropped on its own if a build ever disagrees.
 */
public final class Wave6LostFound {
    private Wave6LostFound() {}

    /** A fight is "on" for this many ticks after the wearer hit or was hit. */
    private static final int COMBAT_TICKS = 100;

    public static void register() { NeoForge.EVENT_BUS.addListener(Wave6LostFound::onToss); }

    private static void onToss(ItemTossEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p.isShiftKeyDown() || !RelicFlags.has(p, Wave6Tune.F_NEKET)) return;
        boolean fighting = p.tickCount - p.getLastHurtByMobTimestamp() < COMBAT_TICKS || p.tickCount - p.getLastHurtMobTimestamp() < COMBAT_TICKS;
        if (!fighting) return;
        // cancelling alone would delete the stack (it already left the inventory), so hand it back first and cancel only when it all fit
        net.minecraft.world.item.ItemStack stack = e.getEntity().getItem();
        if (stack.isEmpty()) return;
        p.getInventory().add(stack);
        if (stack.isEmpty()) e.setCanceled(true);
    }
}
