package com.newuniverse.nusmp.client.firecast;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.firecast.FireCastNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the fire casting styles is tiny. The "Salamander" key (under Controls > Multiverse of Anime, the backslash by default) asks the server to link or unlink Fuegoleon's
 * Salamander; the server decides (it must be Fuegoleon's grimoire, the switch must be on, at most one ask per half second is answered). The props themselves (the ball, the spiral, the
 * Salamander) are ordinary magic props drawn by {@code client.prop.FireCastPropPainter}, and the animation clips come through the cast animation packet, so there is nothing else to register.
 * Call {@link #init} from the client setup with the mod bus.
 */
public final class FireCastClient {
    public static final KeyMapping SALAMANDER = new KeyMapping("key.nusmp.fire_salamander", InputConstants.Type.KEYSYM, InputConstants.KEY_BACKSLASH, "key.categories.nusmp");

    private FireCastClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(SALAMANDER));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (SALAMANDER.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(new FireCastNet.Key(0));
            }
        });
    }
}
