package com.newuniverse.nusmp.client.undine;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.undine.UndineNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Undine is tiny. The "Undine" key (under Controls > Multiverse of Anime, the right bracket by default) asks the server to link or unlink Noelle's Undine; the server decides (it
 * must be Noelle's grimoire, the switch must be on, at most one ask per half second is answered). The Undine herself is an ordinary magic prop drawn by {@code client.prop.UndinePropPainter}, and her
 * sounds come from the server, so there is nothing else to register. Call {@link #init} from the client setup with the mod bus.
 */
public final class UndineClient {
    public static final KeyMapping UNDINE = new KeyMapping("key.nusmp.undine_link", InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, "key.categories.nusmp");

    private UndineClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(UNDINE));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (UNDINE.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(new UndineNet.Key(0));
            }
        });
    }
}
