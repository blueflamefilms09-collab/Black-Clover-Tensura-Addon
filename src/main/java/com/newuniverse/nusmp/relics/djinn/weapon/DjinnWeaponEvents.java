package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The listeners of the Djinn weapons, registered from {@code DjinnEvents.register}: the enforcement tick (the weapon is kept whole in its slot), the
 * guards that stop a temporary weapon from ever reaching the world (a toss, an item entity, a death drop), the Brand mark's extra damage, and the
 * cleanup of the per-player tables on logout and when the server stops.
 */
public final class DjinnWeaponEvents {
    private DjinnWeaponEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(DjinnWeaponEvents::onPlayerTick);
        bus.addListener(EventPriority.HIGH, DjinnWeaponEvents::onToss);
        bus.addListener(DjinnWeaponEvents::onJoin);
        bus.addListener(EventPriority.HIGH, DjinnWeaponEvents::onDrops);
        bus.addListener(DjinnWeaponEvents::onIncoming);
        bus.addListener(DjinnWeaponEvents::onLogout);
        bus.addListener(DjinnWeaponEvents::onServerStopping);
    }

    /** Every fourth tick (offset from the engine's own): the weapon is made whole while an Equip lasts, and pending strikes fall. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount & 3) != 2) return;
        DjinnState s = DjinnState.of(p);
        if (s.phase() >= DjinnPhaseRules.HALF || s.hasLoan()) DjinnWeaponService.apply(p);
        DjinnWeaponActs.tick(p);
    }

    /** A toss of a temporary weapon is cancelled: it was already out of the slot, so it is simply gone, and the next tick makes the weapon again. */
    static void onToss(ItemTossEvent e) {
        if (DjinnTemp.isLoanWeapon(e.getEntity().getItem())) e.setCanceled(true);
    }

    /** No temporary weapon ever becomes an item in the world (a dispenser, a hopper, a container spill, a death drop that got past the guard). */
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof ItemEntity ie && DjinnTemp.isLoanWeapon(ie.getItem())) e.setCanceled(true);
    }

    /** A dying bearer: the Equip has already ended (and handed the displaced item back into the inventory the death drops), so only a stray weapon is left to strip. */
    static void onDrops(LivingDropsEvent e) {
        e.getDrops().removeIf(ie -> DjinnTemp.isLoanWeapon(ie.getItem()));
    }

    /** A branded foe takes more damage from everything. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide()) return;
        double bonus = DjinnWeaponActs.markBonus(t.getUUID(), t.level().getGameTime());
        if (bonus > 0.0) e.setAmount((float) (e.getAmount() * (1.0 + bonus)));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DjinnWeaponActs.forget(p.getUUID());
    }

    static void onServerStopping(ServerStoppingEvent e) { DjinnWeaponActs.clearAll(); }
}
