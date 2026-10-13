package com.newuniverse.nusmp.dungeon.nexus.relic;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Every game-bus handler of the Treasury's relics, attached in one call: {@code RelicEvents.register(NeoForge.EVENT_BUS)}. The work itself is
 * in {@link Attunement}, {@link ChaliceHandler}, {@link MantleHandler}, {@link SheathHandler}, {@link GamblerPage}, {@link AmuletHandler} and
 * {@link SurgeHandler}; this class only routes. A player is looked at every tick only for the ten second attunement; everything else runs a few
 * times a second, and only for players who wear a relic (or are still under the effect of one). Once a second every player's Wild Surge cost is
 * checked against the expiry saved in their data, which is a handful of lookups.
 */
public final class RelicEvents {
    private RelicEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(RelicEvents::onPlayerTick);
        bus.addListener(RelicEvents::onServerTick);
        bus.addListener(RelicEvents::onIncomingDamage);
        bus.addListener(EventPriority.LOWEST, true, RelicEvents::onIncomingDamageLast);          // after the shields; cancelled blows too
        bus.addListener(RelicEvents::onEat);
        bus.addListener(RelicEvents::onChangeTarget);
        bus.addListener(RelicEvents::onItemJoin);
        bus.addListener(RelicEvents::onDeath);
        bus.addListener(RelicEvents::onClone);
        bus.addListener(RelicEvents::onLogin);
        bus.addListener(RelicEvents::onRespawn);
        bus.addListener(RelicEvents::onLogout);
        bus.addListener(RelicEvents::onDimension);
        bus.addListener(RelicEvents::onServerStopping);
    }

    // ---------------------------------------------------------------- ticks
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Attunement.tick(p);
        int t = p.tickCount;
        if (t % 5 != 2) return;
        int mask = Worn.mask(p);
        if (t % 20 == 2) {                                                          // the costs that must follow what is worn, or outlast it
            SurgeHandler.sync(p);
            AmuletHandler.tick(p, mask);
        }
        if (mask == 0 && !RelicState.IMMUNE.containsKey(p.getUUID()) && !RelicState.MANTLE.containsKey(p.getUUID())) return;
        ChaliceHandler.shield(p, mask);
        if (t % 20 == 2) {
            ChaliceHandler.hunger(p, mask);
            SheathHandler.tick(p, mask);
            MantleHandler.watch(p);
        }
    }

    static void onServerTick(ServerTickEvent.Post e) { MantleHandler.onServerTick(e.getServer()); }

    // ---------------------------------------------------------------- damage and targets
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        Attunement.onHurt(e);
        ChaliceHandler.onDamage(e);
        MantleHandler.onOutgoing(e);
    }

    static void onIncomingDamageLast(LivingIncomingDamageEvent e) { SheathHandler.onDamageLast(e); }

    static void onChangeTarget(LivingChangeTargetEvent e) { MantleHandler.onChangeTarget(e); }

    // ---------------------------------------------------------------- eating, items
    static void onEat(LivingEntityUseItemEvent.Finish e) { ChaliceHandler.onEat(e); }

    static void onItemJoin(EntityJoinLevelEvent e) { Attunement.onItemJoin(e); }

    // ---------------------------------------------------------------- the end of a stay
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) AmuletHandler.clear(p);
        Attunement.onDeath(e);
    }

    static void onClone(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer from) AmuletHandler.clear(from);
        if (e.getEntity() instanceof ServerPlayer to) AmuletHandler.clear(to);
        SurgeHandler.copy(e.getOriginal(), e.getEntity());                           // the Wild Surge's expiry goes with the player, a death or not
        Attunement.onClone(e);
    }

    /** Back in the world (a login, a respawn): the Wild Surge's cost, if it has not run out, is put back on their mana pool. */
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SurgeHandler.sync(p);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SurgeHandler.sync(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) AmuletHandler.clear(p);
        Attunement.onLogout(e);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            AmuletHandler.clear(p);
            SurgeHandler.sync(p);
        }
        Attunement.onDimension(e);
    }

    static void onServerStopping(ServerStoppingEvent e) {
        AmuletHandler.clearAll();
        Attunement.clearAll();
    }
}
