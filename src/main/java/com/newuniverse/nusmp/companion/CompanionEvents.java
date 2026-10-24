package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.companion.bond.Companion;
import com.newuniverse.nusmp.companion.follow.CompanionTravel;
import com.newuniverse.nusmp.companion.talk.Bubble;
import com.newuniverse.nusmp.companion.talk.Talk;
import com.newuniverse.nusmp.companion.talk.Talkable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the game events the companion system listens to, in one place. Once a second (not every tick) the conversations and crossings are swept; a player who logs out, dies or changes dimension loses their open
 * conversation; the follower crossing is the two small steps of {@code CompanionTravel}; a speaker that leaves the world loses its bubble record; the server stop drops everything. The protection rule lives here too:
 * a player's blow or another companion's blow never lands on a companion (creative players and the void / {@code /kill} still can, as for every NPC of the mod), and a companion never hurts a player or another
 * companion by accident. It costs two {@code instanceof} checks per damage event.
 */
public final class CompanionEvents {
    private CompanionEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(CompanionEvents::onServerTick);
        bus.addListener(CompanionEvents::onLogout);
        bus.addListener(CompanionEvents::onDimension);
        bus.addListener(CompanionEvents::onTravel);
        bus.addListener(CompanionEvents::onDeath);
        bus.addListener(CompanionEvents::onLeave);
        bus.addListener(CompanionEvents::onStopped);
        bus.addListener(CompanionEvents::onDamage);
    }

    static void onServerTick(ServerTickEvent.Post e) {
        if (CompanionTravel.busy()) CompanionTravel.tick(e.getServer());                // nothing at all while no crossing is in progress
        if (e.getServer().getTickCount() % 20 != 3) return;
        Talk.sweep(e.getServer());
        long now = e.getServer().overworld().getGameTime();
        CompanionTravel.sweep(now);
        Bubble.sweep(now);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Talk.forget(p);
        CompanionTravel.forget(p.getUUID());
    }

    static void onTravel(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent e) { CompanionTravel.onTravel(e); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        CompanionTravel.onArrived(e);
        if (e.getEntity() instanceof ServerPlayer p) Talk.end(p, true);
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Talk.end(p, true);
    }

    static void onLeave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide && Bubble.recorded() > 0) Bubble.forget(e.getEntity().getUUID());
    }

    static void onStopped(ServerStoppedEvent e) {
        Talk.clear();
        CompanionTravel.clear();
        Bubble.clear();
    }

    /** Is this entity somebody's companion (a bond companion or anything that can be talked to)? */
    public static boolean isCompanion(Entity e) { return e instanceof Companion || e instanceof Talkable; }

    static void onDamage(LivingIncomingDamageEvent e) {
        Entity victim = e.getEntity();
        if (!isCompanion(victim) || !CompanionConfig.enabled()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.isCreativePlayer()) return;
        Entity attacker = src.getEntity();
        if (attacker instanceof net.minecraft.world.entity.player.Player || isCompanion(attacker)) e.setCanceled(true);
    }
}
