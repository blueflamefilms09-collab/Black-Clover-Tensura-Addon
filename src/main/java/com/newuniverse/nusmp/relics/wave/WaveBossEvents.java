package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.entity.zogratis.DanteBoss;
import com.newuniverse.nusmp.entity.zogratis.ZenonBoss;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * The game-bus side of the boss relics: the rare drop from Dante and Zenon, and the cleaning up of the twin gates (death, logout, a stopping server).
 * Nothing here runs per tick.
 */
final class WaveBossEvents {
    private WaveBossEvents() {}

    /**
     * Dante may drop his crown and Zenon his ring when a player kills them. The relic is issued through the Order's ledger, so it is dropped only if
     * the world does not have it yet (a live copy anywhere means no second one). One roll per kill, at {@link WaveBoss#dropChance}.
     */
    static void onDrops(LivingDropsEvent e) {
        LivingEntity dead = e.getEntity();
        String id = dead instanceof DanteBoss ? WaveBoss.ID_CROWN : dead instanceof ZenonBoss ? WaveBoss.ID_RING : null;
        if (id == null || !(dead.level() instanceof ServerLevel level)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer)) return;
        double chance;
        try {
            chance = WaveBoss.dropChance.getAsDouble();
        } catch (RuntimeException notReady) {
            chance = WaveBossRules.DROP_DEFAULT;
        }
        if (!WaveBossRules.drops(chance, level.getRandom().nextDouble())) return;
        RelicDef def = Relics.byId(id);
        if (def == null) return;
        ItemStack stack = RelicCatalog.newStack(def);
        if (stack.isEmpty() || !UniqueLedger.issue(level, id, stack)) return;
        e.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.3, dead.getZ(), stack));
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) WaveBossActs.clearGates(p.getUUID());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { WaveBossActs.clearGates(e.getEntity().getUUID()); }

    static void onStopping(ServerStoppingEvent e) { WaveBossActs.clearAllGates(); }
}
