package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 0.53: the client half of the player render layers: which player has which aura until when (from PlayerAuraPayload), the painters
 * registered for each {@link Aura} (each attribute's {@code client.aura.<Name>Aura.register()}, listed in {@link AuraRegistry}), and the
 * {@link PlayerAuraLayer} added to both player skins.
 */
public final class PlayerAuraClient {
    private PlayerAuraClient() {}

    /** entity id -> aura -> {start game time, end game time, style}. */
    private static final Map<Integer, EnumMap<Aura, long[]>> ACTIVE = new HashMap<>();
    private static final EnumMap<Aura, AuraPainter> PAINTERS = new EnumMap<>(Aura.class);

    public static void register(Aura a, AuraPainter p) { PAINTERS.put(a, p); }
    static AuraPainter painter(Aura a) { return PAINTERS.get(a); }
    static EnumMap<Aura, long[]> running(int entityId) { return ACTIVE.get(entityId); }

    public static void init(IEventBus modBus) {
        AuraRegistry.registerAll();
        modBus.addListener(PlayerAuraClient::addLayers);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ACTIVE.clear());
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer r) r.addLayer(new PlayerAuraLayer(r));
        }
    }

    public static void receive(int entityId, int aura, int ticks, int style) {
        Minecraft mc = Minecraft.getInstance();
        Aura[] all = Aura.values();
        if (mc.level == null || aura < 0 || aura >= all.length) return;
        if (ticks <= 0) {
            var m = ACTIVE.get(entityId);
            if (m != null) { m.remove(all[aura]); if (m.isEmpty()) ACTIVE.remove(entityId); }
            return;
        }
        long now = mc.level.getGameTime();
        var m = ACTIVE.computeIfAbsent(entityId, k -> new EnumMap<>(Aura.class));
        long[] old = m.get(all[aura]);
        long start = old != null && old[2] == style && now < old[1] ? old[0] : now;          // a refresh keeps its start (no re-grow)
        m.put(all[aura], new long[]{start, now + ticks, style});
    }

    /** True while the player has the aura (for particles and other client logic). */
    public static boolean has(int entityId, Aura a) {
        var m = ACTIVE.get(entityId);
        Minecraft mc = Minecraft.getInstance();
        long[] w = m == null ? null : m.get(a);
        return w != null && mc.level != null && mc.level.getGameTime() < w[1];
    }
}
