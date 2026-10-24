package com.newuniverse.nusmp.client.companion;

import com.newuniverse.nusmp.client.arc.ArcNpcRenderer;
import com.newuniverse.nusmp.client.playermodel.PlayerBodies;
import com.newuniverse.nusmp.client.playermodel.PlayerModelRegistry;
import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.companion.sample.CompanionEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the client half of the companion system, one entry point: the sample companion's renderer (the arcs' character renderer, and the player-model kit's body for it, exactly as the other arc characters are drawn),
 * the client config (the speech bubbles), the three hooks the common packets call (open or update the talk screen, close it, show a bubble), and the bubble renderer's events. The client decides nothing: the talk
 * screen only asks, the server answers. Called from the client block of the mod constructor.
 */
public final class CompanionClient {
    private CompanionClient() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, CompanionClientConfig.SPEC, "nusmp-companions-client.toml");
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(CompanionEntities.CHARMY.get(), ArcNpcRenderer::new));
        PlayerModelRegistry.extend(e -> PlayerModelRegistry.put(e, CompanionEntities.CHARMY.get(), new PlayerBodies.ArcNpc()));          // the same look as the other arc characters, after the kit's own conversions
        CompanionNet.clientShow = CompanionClient::onShow;
        CompanionNet.clientEnd = CompanionClient::onEnd;
        CompanionNet.clientBubble = BubbleClient::onLine;
        NeoForge.EVENT_BUS.addListener(BubbleClient::onRender);
        NeoForge.EVENT_BUS.addListener(BubbleClient::onTick);
        NeoForge.EVENT_BUS.addListener(BubbleClient::onLogout);
    }

    private static void onShow(CompanionNet.Show s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.screen instanceof TalkScreen open && open.entityId() == s.entityId()) open.update(s.view());
        else mc.setScreen(new TalkScreen(s.entityId(), s.view()));
    }

    private static void onEnd(CompanionNet.End e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TalkScreen open && open.entityId() == e.entityId()) open.closeFromServer();
    }
}
