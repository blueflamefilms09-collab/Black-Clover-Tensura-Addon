package com.newuniverse.nusmp.client.requip;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.requip.RequipArmoury;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client side of the Requip Armoury: the window ({@link RequipArmouryScreen}) and the "Requip Armoury" key (unbound by default, under Controls > Multiverse of Anime) that asks the server to
 * open it, as the Armoury mode of Requip does. The server decides: without the skill the key does nothing.
 */
public final class RequipArmouryClient {
    public static final KeyMapping KEY = new KeyMapping("key.nusmp.requip_armoury", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.nusmp");

    private RequipArmouryClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(RequipArmoury.MENU.get(), RequipArmouryScreen::new));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(RequipArmoury.OpenPayload.INSTANCE);
            }
        });
    }
}
