package com.newuniverse.nusmp.elite.races.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.races.net.RaceClientState;
import com.newuniverse.nusmp.elite.races.net.RacePayloads;
import com.newuniverse.nusmp.elite.races.runtime.RaceMessages;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Why: the client half of the transformations: ONE key (default {@code .}, rebindable under Controls, chosen because no other key of this mod uses it) that asks the server to
 * toggle the best form, or, with Shift held, to step to the next form (and back to base after the last). The key sends a word and nothing else; the server decides. A small line
 * above the hotbar shows the form and the time left, in the form's own colour, from the last word of the server ({@link RaceClientState}). A draw that throws switches the line off
 * with one log line. Everything is reset on leaving the world.
 */
public final class RaceClient {
    private RaceClient() {}

    private static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping TRANSFORM = new KeyMapping("key.nusmp.elite_race_transform", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, CATEGORY);
    private static boolean broken;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(TRANSFORM));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "elite_race_form"), RaceClient::render));
        NeoForge.EVENT_BUS.addListener(RaceClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            RaceClientState.clear();
            broken = false;
        });
    }

    private static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (TRANSFORM.consumeClick()) {
            if (mc.screen != null) continue;
            PacketDistributor.sendToServer(new RacePayloads.Request(mc.player.isShiftKeyDown() ? "cycle" : "auto"));
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (broken || !RaceClientState.active()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        try {
            TransformState s = TransformState.byId(RaceClientState.state());
            if (s == null) return;
            Font font = mc.font;
            long left = RaceClientState.remaining(mc.level == null ? 0L : mc.level.getGameTime());
            Component name = RaceMessages.stateName(s);
            Component line = left < 0 ? name : Component.empty().append(name).append(Component.literal("  " + (left + 19) / 20 + "s"));
            int w = g.guiWidth(), h = g.guiHeight();
            int x = (w - font.width(line)) / 2;
            g.drawString(font, line, x, h - 62, RaceClientState.color(), true);
        } catch (RuntimeException ex) {
            broken = true;
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] elite races: the form line could not be drawn; it is switched off", ex);
        }
    }
}
