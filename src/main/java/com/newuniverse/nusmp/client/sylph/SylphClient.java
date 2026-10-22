package com.newuniverse.nusmp.client.sylph;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.sylph.SylphNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Sylph is tiny. The "Sylph" key (under Controls > Multiverse of Anime, the right bracket by default) asks the server to link or unlink Yuno's Sylph; the server
 * decides (it must be Yuno's grimoire, the switch must be on, at most one ask per half second is answered). The Sylph herself is an ordinary magic prop drawn by
 * {@code client.prop.SylphPropPainter}, and the animation comes from the prop's own word, so there is nothing else to register. Call {@link #init} from the client setup with the mod bus.
 */
public final class SylphClient {
    public static final KeyMapping SYLPH = new KeyMapping("key.nusmp.sylph", InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, "key.categories.nusmp");

    private SylphClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(SYLPH));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (SYLPH.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(new SylphNet.Key(0));
            }
        });
    }
}
