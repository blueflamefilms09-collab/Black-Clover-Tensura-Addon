package com.newuniverse.nusmp.butoh;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the dance and the deflect dash react to game events (a tick, an incoming hit, a missile, a logout), and the lead wires the whole mod
 * from one place. So every listener lives here and one call, {@link #register}, adds them all to the game event bus; nothing else in the
 * mod needs to know the individual handlers. Evasion runs at HIGH priority so a dodged hit is cancelled before later listeners (armour
 * effects, thorns, on-hit skills) see it.
 */
public final class ButohEvents {
    private ButohEvents() {}

    /** Adds every Butoh dance listener to the game event bus (NeoForge.EVENT_BUS). */
    public static void register(IEventBus bus) {
        bus.addListener(ButohEvents::onPlayerTick);
        bus.addListener(EventPriority.HIGH, ButohEvents::onIncomingDamage);
        bus.addListener(ButohDash::onImpact);
        bus.addListener(ButohEvents::onDeath);
        bus.addListener(ButohEvents::onLogout);
        bus.addListener(ButohEvents::onStartTracking);
        bus.addListener(ButohEvents::onServerStopping);
    }

    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) ButohDance.tick(p);
    }

    /** A dancing player may slip a blow: chance by the current move, capped, 20 points lower against a boss, never against unavoidable damage. */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !ButohDance.dancing(p) || e.isCanceled()) return;
        DamageSource src = e.getSource();
        boolean attacker = src.getEntity() != null || src.getDirectEntity() != null;
        if (!EvasionTable.eligible(src.is(DamageTypeTags.BYPASSES_INVULNERABILITY), src.is(DamageTypes.FELL_OUT_OF_WORLD), attacker)) return;
        DanceMove move = ButohDance.currentMove(p);
        if (move == null) return;
        boolean boss = src.getEntity() instanceof LivingEntity a && BalanceLaw.isBoss(a);
        if (!EvasionTable.dodges(EvasionTable.chance(move, boss, ButohConfig.evadeMax()), p.getRandom().nextDouble())) return;
        e.setCanceled(true);
        ButohDance.slip(p, src);
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ButohDance.stop(p, null);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ButohDance.forget(p);
    }

    /** A player who comes into range of a dancer mid-dance is told the seed, so they see the same moves. */
    static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer watcher) || !(e.getTarget() instanceof ServerPlayer dancer)) return;
        DanceStatePayload state = ButohDance.stateOf(dancer);
        if (state != null) PacketDistributor.sendToPlayer(watcher, state);
    }

    static void onServerStopping(ServerStoppingEvent e) { ButohDance.clearAll(); }
}
