package com.newuniverse.nusmp.client.song;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.song.SongClientConfig;
import com.newuniverse.nusmp.song.SongNet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of Song Magic is small: the "Sing" key (hold) that stands in for a microphone when Simple Voice Chat is not
 * installed, and a thin level meter near the hotbar that shows how loudly the server hears you (and the Crescendo charge). The key
 * only talks to the server while it is held, and the server ignores it entirely when the voice mod is present.
 * Call {@link #init} from the client setup with the mod bus.
 */
public final class SongClient {
    public static final KeyMapping SING = new KeyMapping("key.nusmp.song_sing", InputConstants.Type.KEYSYM,
            InputConstants.KEY_V, "key.categories.nusmp");

    private static boolean down;
    private static int sinceSent;

    private SongClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(SING));
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "song_meter"), SongClient::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { down = false; SongHudState.clear(); });
    }

    /** Edge-triggered with a keep-alive every 4 ticks while held (the server lets a level lapse shortly after the last message). */
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        boolean now = SING.isDown() && mc.screen == null && SongHudState.mode() != 0;
        if (now != down || (now && ++sinceSent >= 4)) {
            down = now;
            sinceSent = 0;
            PacketDistributor.sendToServer(new SongNet.Key(now));
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (SongClientConfig.hideHud() || !SongHudState.visible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        int w = 62, h = 5;
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() - 58;
        int level = SongHudState.level();
        com.newuniverse.nusmp.client.ui.move.GuiMove.report("song_meter", x - 1, y - 1, w + 2, SongHudState.charge() >= 0 ? h + 7 : h + 2);    // GUI Move: where the meter is drawn (the layer hook moves it)
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xA0100818);
        int filled = Math.round(w * Math.min(100, level) / 100f);
        int col = level >= 60 ? 0xFFFFE6F8 : 0xFFFFB4E8;
        if (filled > 0) g.fill(x, y, x + filled, y + h, col);
        int charge = SongHudState.charge();
        if (charge >= 0) {
            int cw = Math.round(w * charge / 100f);
            g.fill(x, y + h + 2, x + w, y + h + 4, 0x80100818);
            if (cw > 0) g.fill(x, y + h + 2, x + cw, y + h + 4, 0xFFFFD27A);
        }
    }
}
