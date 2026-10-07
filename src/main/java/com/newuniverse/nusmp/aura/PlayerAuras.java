package com.newuniverse.nusmp.aura;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.53: the server side of the player render layers. A spell that makes something grow over the caster (a spectral beast, swollen
 * muscle, a crystal shell, a bubble ...) calls {@link #set}: every client that can see the player then draws that {@link Aura}
 * through the painter registered for it ({@code client.aura.<Name>Aura}) for 'ticks' ticks. The server keeps the same table so
 * game logic can ask {@link #active}.
 *
 * <pre>
 *   PlayerAuras.set(player, Aura.BODY, 200);            // 10 s
 *   PlayerAuras.set(player, Aura.BEAST, 160, 2);        // style 2: a painter-defined variant (a different beast)
 *   if (PlayerAuras.active(player, Aura.BODY)) ...
 *   PlayerAuras.clear(player, Aura.BODY);
 * </pre>
 */
public final class PlayerAuras {
    private PlayerAuras() {}

    /** player uuid -> aura -> {start game time, end game time, style}. */
    private static final Map<UUID, EnumMap<Aura, long[]>> ACTIVE = new HashMap<>();

    public static void set(ServerPlayer p, Aura a, int ticks) { set(p, a, ticks, 0); }

    public static void set(ServerPlayer p, Aura a, int ticks, int style) {
        long now = p.level().getGameTime();
        ACTIVE.computeIfAbsent(p.getUUID(), k -> new EnumMap<>(Aura.class)).put(a, new long[]{now, now + ticks, style});
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new PlayerAuraPayload(p.getId(), a.ordinal(), ticks, style));
    }

    public static void clear(ServerPlayer p, Aura a) {
        var m = ACTIVE.get(p.getUUID());
        if (m != null) m.remove(a);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new PlayerAuraPayload(p.getId(), a.ordinal(), 0, 0));
    }

    public static boolean active(LivingEntity e, Aura a) {
        var m = ACTIVE.get(e.getUUID());
        long[] w = m == null ? null : m.get(a);
        return w != null && e.level().getGameTime() < w[1];
    }

    /** The style the aura was set with (0 if it is not active). */
    public static int style(LivingEntity e, Aura a) {
        var m = ACTIVE.get(e.getUUID());
        long[] w = m == null ? null : m.get(a);
        return w != null && e.level().getGameTime() < w[1] ? (int) w[2] : 0;
    }

    // ---------------------------------------------------------------- events
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { ACTIVE.remove(e.getEntity().getUUID()); }

    /** Someone who starts seeing a player sees that player's running auras too. */
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getTarget() instanceof ServerPlayer target) || !(e.getEntity() instanceof ServerPlayer viewer)) return;
        var m = ACTIVE.get(target.getUUID());
        if (m == null) return;
        long now = target.level().getGameTime();
        for (var en : m.entrySet()) {
            long[] w = en.getValue();
            if (now < w[1]) PacketDistributor.sendToPlayer(viewer, new PlayerAuraPayload(target.getId(), en.getKey().ordinal(), (int) (w[1] - now), (int) w[2]));
        }
    }
}
