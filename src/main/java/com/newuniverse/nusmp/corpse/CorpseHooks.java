package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * Why: everything Soul Corpse Magic does outside a spell cast is a reaction to something the game does, so it is all in one set of
 * listeners: a creature dies (write the ledger; save a covered player from a pact), a body picks a target (only the owner's foes), a
 * body deals or receives damage (+30% for raised ones, never at the owner or their friends, players capped by the balance law), a
 * body would heal (it does not: it only loses), a body would drop loot or experience (it does not), a saved body joins a world (an
 * orphan of a crash: it is removed), a player logs out or the server stops (their bodies crumble), and the sneak-use that binds an
 * ally to the pact. Wire it in NUSMP once: {@code CorpseHooks.register()} (idempotent).
 */
public final class CorpseHooks {
    private CorpseHooks() {}

    private static boolean done;

    public static void register() {
        if (done) return;
        done = true;
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(CorpseHooks::onTick);
        bus.addListener(EventPriority.HIGH, CorpseHooks::onDeathPact);
        bus.addListener(CorpseHooks::onDeath);
        bus.addListener(CorpseHooks::onChangeTarget);
        bus.addListener(CorpseHooks::onIncoming);
        bus.addListener(CorpseHooks::onHeal);
        bus.addListener(CorpseHooks::onDrops);
        bus.addListener(CorpseHooks::onXp);
        bus.addListener(CorpseHooks::onJoin);
        bus.addListener(CorpseHooks::onLogout);
        bus.addListener(CorpseHooks::onLogin);
        bus.addListener(CorpseHooks::onRespawn);
        bus.addListener(CorpseHooks::onInteract);
        bus.addListener(CorpseHooks::onStopping);
    }

    static void onTick(ServerTickEvent.Post e) {
        Corpses.tick(e.getServer());
        Pacts.tick(e.getServer());
    }

    static void onDeathPact(LivingDeathEvent e) { Pacts.onDeath(e); }

    static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide) return;
        if (Corpses.isCorpse(dead)) return;                              // a body falling is not a death worth remembering
        Corpses.recordDeath(dead);
    }

    /** A body may only pick the owner's foes; anything else is refused (a zombie body must not go for the owner or a bystander). */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide || !Corpses.isCorpse(who)) return;
        LivingEntity next = e.getNewAboutToBeSetTarget();
        if (next == null) return;
        UUID ownerId = Corpses.ownerOf(who);
        Player owner = ownerId == null ? null : who.level().getPlayerByUUID(ownerId);
        if (!(owner instanceof ServerPlayer sp) || !Corpses.isFoe(sp, next)) e.setCanceled(true);
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity body = Corpses.bodyBehind(e.getSource());
        if (body == null) return;
        LivingEntity victim = e.getEntity();
        UUID ownerId = Corpses.ownerOf(body);
        if (ownerId != null && (ownerId.equals(victim.getUUID()) || ownerId.equals(Corpses.ownerOf(victim)) || Pacts.RULES.allies(ownerId).contains(victim.getUUID()))) {
            e.setCanceled(true);                                         // friendly fire: an explosion, a stray arrow
            return;
        }
        float amount = e.getAmount();
        if (Corpses.isRaised(body)) amount *= (float) CorpseConfig.damageMult();
        if (victim instanceof Player p) amount = BalanceLaw.cap(p, amount);
        e.setAmount(amount);
    }

    /** A decaying body never regenerates: it only loses. */
    static void onHeal(LivingHealEvent e) {
        if (Corpses.isRaised(e.getEntity())) e.setCanceled(true);
    }

    static void onDrops(LivingDropsEvent e) {
        if (Corpses.isCorpse(e.getEntity())) e.setCanceled(true);
    }

    static void onXp(LivingExperienceDropEvent e) {
        if (Corpses.isCorpse(e.getEntity())) e.setCanceled(true);
    }

    /** A tagged body that arrives from a saved chunk is a leftover of a crash (bodies are discarded on a clean stop): it does not join. */
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !e.loadedFromDisk()) return;
        Entity ent = e.getEntity();
        if (ent instanceof Mob && Corpses.isCorpse(ent) && !Corpses.listed(ent)) e.setCanceled(true);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Corpses.crumbleAllOf(p.getServer(), p.getUUID());
        Corpses.forget(p.getUUID());
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Pacts.onLogin(p);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Pacts.onRespawn(p);
    }

    /** Sneak and use on another player while your pact is armed: binds them (at most three). Nothing else about the use changes. */
    static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getEntity() instanceof ServerPlayer caster) || !(e.getTarget() instanceof ServerPlayer ally) || !caster.isShiftKeyDown()) return;
        if (!Pacts.armed(caster)) return;
        PactRules.Tap r = Pacts.tap(caster, ally);
        String text = switch (r) {
            case ADDED -> ally.getGameProfile().getName() + " is bound to your pact.";
            case ALREADY -> ally.getGameProfile().getName() + " is already bound.";
            case FULL -> "Your pact already holds " + PactRules.MAX_ALLIES + " people.";
            default -> null;
        };
        if (text == null) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        caster.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true);
        if (r == PactRules.Tap.ADDED) ally.displayClientMessage(Component.literal(caster.getGameProfile().getName() + " has bound you to a Soul Corpse pact.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    static void onStopping(ServerStoppingEvent e) { Corpses.discardAll(e.getServer()); }
}
