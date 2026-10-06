package com.newuniverse.nusmp.client.mode;

import com.newuniverse.nusmp.blackclover.ModeArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side of the transformation armour overlays (0.38): which player wears which mode until when (from
 * {@link ModeArmor.ModePayload}), and the {@link ModeArmorLayer} added to both player skins.
 */
public final class ModeArmorClient {
    /** entity id -> {mode ordinal, start game time, end game time}. */
    private static final Map<Integer, long[]> WORN = new HashMap<>();

    private ModeArmorClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ModeArmorClient::addLayers);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> WORN.clear());
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer r) r.addLayer(new ModeArmorLayer(r));
        }
    }

    public static void receive(int entityId, int mode, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mode <= 0 || mode >= ModeArmor.Mode.values().length || ticks <= 0) { WORN.remove(entityId); return; }
        long now = mc.level.getGameTime();
        long[] old = WORN.get(entityId);
        long start = old != null && old[0] == mode && now < old[2] ? old[1] : now;      // a refresh keeps its start (no re-grow)
        WORN.put(entityId, new long[]{mode, start, now + ticks});
    }

    /** The mode player {@code entityId} wears now, or NONE. */
    static ModeArmor.Mode mode(int entityId, long now) {
        long[] w = WORN.get(entityId);
        if (w == null) return ModeArmor.Mode.NONE;
        if (now >= w[2]) { WORN.remove(entityId); return ModeArmor.Mode.NONE; }
        return ModeArmor.Mode.values()[(int) w[0]];
    }

    /** Ticks since the mode went on (for the materialise-in growth). */
    static float age(int entityId, float now) {
        long[] w = WORN.get(entityId);
        return w == null ? 0f : now - w[1];
    }

    /** Ticks until the mode ends (for the fade-out). */
    static float left(int entityId, float now) {
        long[] w = WORN.get(entityId);
        return w == null ? 0f : w[2] - now;
    }
}
