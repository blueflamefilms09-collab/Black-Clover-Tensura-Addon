package com.newuniverse.nusmp.heroweapon.gem;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the Choosing has to hear four things the item itself is never told: that the player was hurt (a blow breaks the circle, and the game does not stop an
 * item use for damage), that a tick passed (a chosen weapon is put in the hand one tick after the gem is spent, and a ritual whose use ended without a word
 * is cleaned up), that a player left (their record must not outlive them), and that the server stopped (nothing may survive into the next world). One more
 * for honesty: when the survival route is switched off ({@code recipes = false}) the crafting recipes still exist (a recipe cannot read a server config), so
 * a player who crafts a gem anyway is told at once that it will not answer. Every handler is wrapped so that nothing can throw out of it.
 */
final class GemEvents {
    private GemEvents() {}

    private static final Logger LOG = LogUtils.getLogger();

    static void register(IEventBus bus) {
        bus.addListener(GemEvents::onHurt);
        bus.addListener(GemEvents::onPlayerTick);
        bus.addListener(GemEvents::onLogout);
        bus.addListener(GemEvents::onRespawn);
        bus.addListener(GemEvents::onCrafted);
        bus.addListener(GemEvents::onServerStopped);
    }

    /** Damage that got through ends a running ritual: the glue only sets a flag, the ritual's next tick breaks the circle. */
    static void onHurt(LivingDamageEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && e.getNewDamage() > 0.0f) GemChoosing.hurt(p);
        } catch (RuntimeException ex) {
            LOG.warn("gem hurt handler failed", ex);
        }
    }

    static void onPlayerTick(PlayerTickEvent.Post e) {
        try {
            if (GemChoosing.busy() && e.getEntity() instanceof ServerPlayer p) GemChoosing.housekeeping(p);
        } catch (RuntimeException ex) {
            LOG.warn("gem tick handler failed", ex);
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        try {
            GemChoosing.forget(e.getEntity().getUUID());
        } catch (RuntimeException ex) {
            LOG.warn("gem logout handler failed", ex);
        }
    }

    /** A player who died and came back is a new body: whatever the old one was doing is over. */
    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        try {
            GemChoosing.forget(e.getEntity().getUUID());
        } catch (RuntimeException ex) {
            LOG.warn("gem respawn handler failed", ex);
        }
    }

    /** A gem crafted while the survival route is closed: the recipe worked (it cannot know), the ritual will not, and the player should hear that now. */
    static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && e.getCrafting().getItem() instanceof CallingGemItem && !GemRules.routeOpen(GemConfig.recipes(), GemChoosing.privileged(p)))
                p.sendSystemMessage(GemText.msg(GemMessages.CLOSED_CRAFT));
        } catch (RuntimeException ex) {
            LOG.warn("gem craft handler failed", ex);
        }
    }

    static void onServerStopped(ServerStoppedEvent e) { GemChoosing.clear(); }
}
