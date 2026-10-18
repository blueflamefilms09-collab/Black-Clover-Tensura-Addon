package com.newuniverse.nusmp.client.elite.nations;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.elite.nations.net.NationNet;
import com.newuniverse.nusmp.elite.nations.net.RequestPayload;
import com.newuniverse.nusmp.elite.nations.net.SnapshotPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Why: the client half of the Nations screen: the key that opens it (default N, rebindable under Controls, category nusmp), the cache of the last snapshot the server sent, and the hand-over to
 * {@link NationScreen}. The server pushes a snapshot when the screen is open and something changed; a snapshot with {@code openTab >= 0} (a right click on the banner or the board) opens the screen on that
 * tab. The client never decides anything: every button sends one action word to the server, which checks it. Everything is forgotten on leaving the world.
 */
public final class NationsClient {
    private NationsClient() {}

    public static final KeyMapping OPEN = new KeyMapping("key.nusmp.nation_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.nusmp");

    private static CompoundTag data = new CompoundTag();
    private static boolean pendingOpen;

    public static CompoundTag data() { return data; }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN));
        NeoForge.EVENT_BUS.addListener(NationsClient::onTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { data = new CompoundTag(); pendingOpen = false; });
        NationNet.clientSink = NationsClient::onSnapshot;
    }

    private static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (OPEN.consumeClick()) {
            if (mc.screen == null) {
                pendingOpen = true;
                PacketDistributor.sendToServer(new RequestPayload(true, 0));
            }
        }
    }

    private static void onSnapshot(SnapshotPayload p) {
        data = p.data();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof NationScreen s) {
            s.refresh(p.openTab());
        } else if (p.openTab() >= 0 || pendingOpen) {
            mc.setScreen(new NationScreen(Math.max(0, p.openTab())));
        }
        pendingOpen = false;
    }

    /** The screen closed: tell the server to stop sending. */
    static void closed() {
        if (Minecraft.getInstance().player != null) PacketDistributor.sendToServer(new RequestPayload(false, 0));
    }
}
