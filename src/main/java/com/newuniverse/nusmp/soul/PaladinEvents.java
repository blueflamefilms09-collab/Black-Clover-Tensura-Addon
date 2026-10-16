package com.newuniverse.nusmp.soul;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Why: Paladin Conversion needs facts only the game event bus can give, and they are about <i>other</i> entities, so they cannot
 * live in a spell class: a creature joining a level with the Paladin data (put its goals, buffs and roster place back), a creature
 * choosing a target (redirect it to the owner's foe, never the owner), blows (a Paladin never hurts its owner, their allies or its
 * fellow Paladins, however the blow arrives: melee, arrow, fireball or blast; the owner never kills their own Paladin; a Paladin's
 * blows on players and bosses are capped), deaths (the owner's frees every Paladin, a Paladin's frees its place), the server tick
 * (channels, and a drive every ten ticks), and the log-out / stop clean-up. Every handler starts with one cheap check and leaves
 * every other entity alone. Wire it in NUSMP once: {@code PaladinEvents.register(NeoForge.EVENT_BUS)} (idempotent).
 */
public final class PaladinEvents {
    private PaladinEvents() {}

    private static boolean done;
    private static final int DRIVE_EVERY = 10;

    public static void register(IEventBus gameBus) {
        if (done) return;
        done = true;
        gameBus.addListener(PaladinEvents::onJoin);
        gameBus.addListener(PaladinEvents::onLeave);
        gameBus.addListener(PaladinEvents::onIncoming);
        gameBus.addListener(PaladinEvents::onChangeTarget);
        gameBus.addListener(PaladinEvents::onDeath);
        gameBus.addListener(PaladinEvents::onTick);
        gameBus.addListener(PaladinEvents::onLogout);
        gameBus.addListener(PaladinEvents::onStopping);
    }

    // ================================================================ joining and leaving
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m) || !Paladins.isPaladin(m)) return;
        Paladins.onLoad(m);
    }

    static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() || !Paladins.isPaladin(e.getEntity())) return;
        Entity.RemovalReason why = e.getEntity().getRemovalReason();
        if (why != null && why.shouldDestroy()) Paladins.onGone(e.getEntity());           // killed or discarded; an unload or a portal keeps its place
    }

    // ================================================================ blows
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        if (victim instanceof ServerPlayer p) PaladinChannel.hurt(p);                       // a blow on the caster breaks the channel
        if (attacker == null) return;
        if (Paladins.isPaladin(attacker)) {
            UUID owner = Paladins.ownerOf(attacker);
            if (owner != null && protectedFrom(victim, owner)) { e.setCanceled(true); return; }
            boolean boss = BossIdentity.isBoss(victim) || BalanceLaw.isBoss(victim);
            e.setAmount(PaladinStats.capHit(e.getAmount(), victim.getMaxHealth(), victim instanceof Player, boss));
        }
        if (Paladins.isPaladin(victim)) {
            UUID owner = Paladins.ownerOf(victim);
            if (owner != null && (attacker.getUUID().equals(owner) || Paladins.isPaladin(attacker) && owner.equals(Paladins.ownerOf(attacker)))) e.setCanceled(true);   // no friendly fire
        }
    }

    /** Is 'victim' someone a Paladin of 'owner' must never hurt: the owner, their pets and allies, the owner's other Paladins. */
    private static boolean protectedFrom(LivingEntity victim, UUID owner) {
        if (victim.getUUID().equals(owner)) return true;
        if (Paladins.isPaladin(victim) && owner.equals(Paladins.ownerOf(victim))) return true;
        if (victim instanceof OwnableEntity o && owner.equals(o.getOwnerUUID())) return true;
        MinecraftServer server = victim.level().getServer();
        ServerPlayer op = server == null ? null : server.getPlayerList().getPlayer(owner);
        return op != null && victim.isAlliedTo(op);
    }

    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide || !(who instanceof Mob m) || !Paladins.isPaladin(m)) return;
        ServerPlayer owner = Paladins.ownerPlayer(m);
        LivingEntity next = e.getNewAboutToBeSetTarget();
        if (next == null) return;
        if (owner == null || !Paladins.fair(m, owner, next)) {
            LivingEntity want = owner == null ? null : Paladins.desiredTarget(m, owner);
            if (want != null) e.setNewAboutToBeSetTarget(want);
            else e.setCanceled(true);
        }
    }

    // ================================================================ deaths
    static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        if (v instanceof ServerPlayer p) Paladins.onOwnerDeath(p);
        else if (Paladins.isPaladin(v)) Paladins.onGone(v);
    }

    // ================================================================ the clock
    static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        PaladinChannel.tick(server);
        if (server.getTickCount() % DRIVE_EVERY != 4) return;
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, Integer> owner : Paladins.ROSTER.owners().entrySet()) {
            for (UUID id : Paladins.ROSTER.of(owner.getKey())) {
                if (Paladins.find(server, id) instanceof Mob m && m.isAlive()) Paladins.drive(server, m, now);
            }
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PaladinChannel.cancel(e.getEntity().getUUID());                                    // its Paladins stay where they are, saved with the world
    }

    static void onStopping(ServerStoppingEvent e) {
        Paladins.ROSTER.clear();
        PaladinChannel.clear();
    }
}
