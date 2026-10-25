package com.newuniverse.nusmp.entity.veldanava;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: three of the Creator's gifts act on players and on other mobs, and the game tells a mod about those through its event bus, not through the boss. This is the
 * one subscriber (wire it once from NUSMP with {@link #register}): the Michael barrier (one fatal blow a day turned away), the Nihility void rot (what a player strikes
 * with the fragment in the off hand cannot heal for a few seconds), and the housekeeping of Veldanava himself (a seized soul comes back at login, a boon is put back at
 * login and respawn, the registries are emptied when the server stops). Everything is per UUID, bounded, and server side only.
 */
public final class VeldanavaEvents {
    private VeldanavaEvents() {}

    /** How long a struck creature cannot heal, and the most creatures marked at once (the oldest marks are dropped first when the table is full). */
    public static final int VOID_ROT_TICKS = 100, VOID_ROT_MAX = 256;

    /** creature -> the game time its mark ends (server thread only). */
    private static final Map<UUID, Long> VOID_ROT = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, VeldanavaEvents::onIncomingDamage);
        bus.addListener(VeldanavaEvents::onHeal);
        bus.addListener(VeldanavaEvents::onLogin);
        bus.addListener(VeldanavaEvents::onRespawn);
        bus.addListener(VeldanavaEvents::onServerStopping);
    }

    public static boolean voidRotted(UUID id, long now) {
        Long until = VOID_ROT.get(id);
        return until != null && now < until;
    }

    public static int voidRotCount() { return VOID_ROT.size(); }

    static void markVoidRot(UUID id, long now) {
        if (VOID_ROT.size() >= VOID_ROT_MAX) {
            for (Iterator<Map.Entry<UUID, Long>> it = VOID_ROT.entrySet().iterator(); it.hasNext(); ) if (now >= it.next().getValue()) it.remove();
            if (VOID_ROT.size() >= VOID_ROT_MAX) return;                       // a full table of live marks: the new one is skipped, never grown past the cap
        }
        VOID_ROT.put(id, now + VOID_ROT_TICKS);
    }

    private static boolean holdsNihility(LivingEntity e) {
        ItemStack off = e.getOffhandItem();
        return off.getItem() instanceof VeldanavaItem v && v.kind() == VeldanavaItem.Kind.NIHILITY;
    }

    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || e.getAmount() <= 0f) return;
        // Nihility: the one who strikes holds the fragment in the off hand, so the victim cannot heal for a few seconds
        if (e.getSource().getEntity() instanceof ServerPlayer hitter && holdsNihility(hitter) && victim != hitter)
            markVoidRot(victim.getUUID(), victim.level().getGameTime());
        // Michael: one fatal blow a day, for a player who consumed the fragment
        if (victim instanceof ServerPlayer p && p.getPersistentData().getBoolean(VeldanavaItem.TAG_MICHAEL)
                && VeldanavaRules.isFatal(p.getHealth(), p.getAbsorptionAmount(), e.getAmount())) {
            long day = p.level().getDayTime() / 24000L;
            if (p.getPersistentData().getLong(VeldanavaItem.TAG_MICHAEL_DAY) != day + 1) {          // stored as day + 1 so that 0 means never
                p.getPersistentData().putLong(VeldanavaItem.TAG_MICHAEL_DAY, day + 1);
                e.setCanceled(true);
                p.displayClientMessage(Component.literal("Justice King Michael turns the blow away.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            }
        }
    }

    static void onHeal(LivingHealEvent e) {
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide || VOID_ROT.isEmpty()) return;
        if (voidRotted(who.getUUID(), who.level().getGameTime())) e.setCanceled(true);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        VeldanavaSoul.onLogin(p);
        VeldanavaBoons.apply(p);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) VeldanavaBoons.apply(p);
    }

    static void onServerStopping(ServerStoppingEvent e) {
        VeldanavaSoul.clear();
        VOID_ROT.clear();
    }
}
