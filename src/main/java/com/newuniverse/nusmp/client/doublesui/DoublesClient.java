package com.newuniverse.nusmp.client.doublesui;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.doublesui.C2S_DoubleSelectPayload;
import com.newuniverse.nusmp.doublesui.S2C_DoublesPayload;
import com.newuniverse.nusmp.entity.MirrorDoubleEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Why: the client half of the Real Double stack (docs/doubles_ui.md). It keeps the last roster the server sent ({@link DoublesView}), draws it ({@link DoublesOverlay}) and carries
 * the three keys that move the selection: Next double and Previous double (the cycle also has a stop for "none selected: trade with the nearest") and Pick aimed double (select the
 * double under the crosshair). A key press only sends a small packet; the server decides whether it counts. Default keys are the numpad 9, 8 and 0, which nothing else of the mod
 * uses, listed under the mod's Controls category and rebindable. Client only: reached from the client section of NUSMP and, by name inside a lambda, from the packet registration.
 */
public final class DoublesClient {
    private DoublesClient() {}

    public static final String CATEGORY = "key.categories.anime_multiverse";
    public static final KeyMapping NEXT = new KeyMapping("key.anime_multiverse.doubles_next", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_9, CATEGORY);
    public static final KeyMapping PREV = new KeyMapping("key.anime_multiverse.doubles_prev", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_8, CATEGORY);
    public static final KeyMapping PICK = new KeyMapping("key.anime_multiverse.doubles_pick", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0, CATEGORY);

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DoublesClientConfig.SPEC, "nusmp-doubles-ui-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.register(NEXT); e.register(PREV); e.register(PICK); });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "doubles_stack"), DoublesOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> DoublesView.clear());
    }

    /** The server's picture (called on the client thread by the packet handler). */
    public static void onRoster(S2C_DoublesPayload p) {
        Minecraft mc = Minecraft.getInstance();
        DoublesView.apply(p, mc.level == null ? 0L : mc.level.getGameTime());
    }

    /** One packet at most per key per tick; with no doubles out the keys do nothing. */
    private static void tick() {
        boolean next = NEXT.consumeClick(), prev = PREV.consumeClick(), pick = PICK.consumeClick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || DoublesView.n == 0) return;
        if (next) send(C2S_DoubleSelectPayload.NEXT, new UUID(0L, 0L));
        else if (prev) send(C2S_DoubleSelectPayload.PREV, new UUID(0L, 0L));
        if (pick && mc.crosshairPickEntity instanceof MirrorDoubleEntity d) send(C2S_DoubleSelectPayload.SET, d.getUUID());
    }

    private static void send(byte op, UUID id) { PacketDistributor.sendToServer(new C2S_DoubleSelectPayload(op, id)); }
}
