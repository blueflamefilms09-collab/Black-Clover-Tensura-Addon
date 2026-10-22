package com.newuniverse.nusmp.client.gnome;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.gnome.GnomeNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Gnome is tiny. The "Gnome" key (under Controls > Multiverse of Anime, the right bracket by default) asks the server to link or unlink the Earth Spirit's companion; the
 * server decides (it must be an Earth Magic mage, the switch must be on, at most one ask per half second is answered). The props themselves are ordinary magic props drawn by
 * {@code client.prop.GnomePropPainter}, and the animation clips of the caster come through the cast animation packet, so there is nothing else to register. Call {@link #init} from the client setup
 * with the mod bus.
 */
public final class GnomeClient {
    public static final KeyMapping GNOME = new KeyMapping("key.nusmp.gnome", InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, "key.categories.nusmp");

    private GnomeClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(GNOME));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (GNOME.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(new GnomeNet.Key(0));
            }
        });
    }
}
