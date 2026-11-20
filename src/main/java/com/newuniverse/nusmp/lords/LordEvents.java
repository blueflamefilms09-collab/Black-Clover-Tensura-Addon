package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the game events the Lord weapons listen to, each cheap for everybody else by construction: the player tick is one int compare for every player but one tick in twenty (then the inventory scan and, for a wielder,
 * the second-tick), a death looks at its killer and victim only, a heal is one float read for a player and returns at once for anything else, the pickup and drop events return unless the stack is a Lord weapon.
 * Cleanup at logout, respawn and server stop drops every transient thing of the player. Server side only.
 */
public final class LordEvents {
    private LordEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(LordEvents::onStarted);
        bus.addListener(LordEvents::onStopping);
        bus.addListener(LordEvents::onPlayerTick);
        bus.addListener(LordEvents::onServerTick);
        bus.addListener(EventPriority.HIGHEST, LordEvents::onDeath);
        bus.addListener(EventPriority.HIGHEST, LordEvents::onDrops);
        bus.addListener(EventPriority.LOW, LordEvents::onHeal);
        bus.addListener(LordEvents::onPickup);
        bus.addListener(LordEvents::onLogin);
        bus.addListener(LordEvents::onRespawn);
        bus.addListener(LordEvents::onLogout);
    }

    static void onStarted(ServerStartedEvent e) { LordData.bind(e.getServer()); }

    static void onStopping(ServerStoppingEvent e) {
        LordService.clearAll();
        LordData.bind(null);
    }

    /** Once a second per player (spread by id): the scan of what they carry and, for a wielder, the second-tick of the burden and the mastery. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0 || !LordConfig.enabled()) return;
        SafeCast.tick("lords.scan", () -> {
            LordService.scan(p);
            LordService.tick(p);
            if ((p.tickCount / 20) % 5 == 0) LordGrantService.syncAll(p);
        });
    }

    /** Every 6000 ticks: the 14 day release. */
    static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 6000 == 0) SafeCast.tick("lords.inactivity", () -> LordService.inactivityCheck(e.getServer()));
    }

    /** A kill feeds the weapon in the killer's hand; a wielder killed by another player loses the weapon to the place where they fell. */
    static void onDeath(LivingDeathEvent e) {
        if (!LordConfig.enabled() || !LordData.bound()) return;
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        if (e.getSource().getEntity() instanceof ServerPlayer killer) SafeCast.tick("lords.kill", () -> LordService.onKill(v, killer));
        if (v instanceof ServerPlayer vp) {
            LordData d = LordData.get();
            for (LordRules.Sin s : LordRules.Sin.values()) {
                if (!s.isLord() || !d.isOwner(s, vp.getUUID())) continue;
                if (e.getSource().getEntity() instanceof ServerPlayer other && other != vp) {
                    SafeCast.tick("lords.release", () -> {
                        LordService.release(vp.getServer(), s, vp.getUUID(), vp.serverLevel(), vp.position());
                        other.sendSystemMessage(Component.translatable("lords.msg.fallen", Component.translatable("lords.name." + s.id)).withStyle(ChatFormatting.RED));
                    });
                } else {
                    LordService.markReturn(vp.getUUID(), s);
                }
            }
        }
    }

    /** A Lord weapon is never among a player's death drops (a plain death returns it at the respawn; a PvP death released it). */
    static void onDrops(LivingDropsEvent e) {
        if (e.getEntity().level().isClientSide || !LordData.bound() || !(e.getEntity() instanceof ServerPlayer)) return;
        e.getDrops().removeIf(i -> LordItems.sinOf(i.getItem()) != null && LordItems.sinOf(i.getItem()).isLord());
    }

    /** Scales the healing of a wielder whose drawback says so (Penance halves it while strained; Greed's ravenous blade takes half). */
    static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !LordConfig.enabled()) return;
        float f = LordService.healFactor(p);
        if (f < 1.0f) e.setAmount(e.getAmount() * f);
    }

    /** Only the wielder picks a Lord weapon up from the ground. */
    static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack s = e.getItemEntity().getItem();
        LordRules.Sin sin = LordItems.sinOf(s);
        if (sin == null || !sin.isLord() || !LordData.bound()) return;
        var owner = LordData.get().owner(sin);
        if (owner != null && !owner.equals(e.getPlayer().getUUID())) e.setCanPickup(TriState.FALSE);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SafeCast.tick("lords.login", () -> LordService.onLogin(p));
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SafeCast.tick("lords.respawn", () -> LordService.onRespawn(p));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LordService.forget(e.getEntity().getUUID()); }
}
