package com.newuniverse.nusmp.client.attune;

import com.newuniverse.nusmp.attune.AttuneNet;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Why: the client half of meditation decides nothing: the server's packet (sent on /meditate) opens {@link MeditationScreen}; Confirm sends the chosen ids back in one small packet, and the server
 * checks everything again. No key (the owner has too many already): the command opens it.
 */
public final class AttuneClient {
    private AttuneClient() {}

    public static void init(IEventBus modBus) {
        AttuneNet.clientOpen = AttuneClient::onOpen;
    }

    private static void onOpen(AttuneNet.Open o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new MeditationScreen(o));
    }

    static void confirm(List<String> ids) {
        if (Minecraft.getInstance().getConnection() != null) PacketDistributor.sendToServer(new AttuneNet.Confirm(List.copyOf(ids)));
    }
}
