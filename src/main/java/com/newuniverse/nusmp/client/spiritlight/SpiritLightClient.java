package com.newuniverse.nusmp.client.spiritlight;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.spiritlight.SpiritLightNet;
import com.newuniverse.nusmp.spiritlight.SpiritLightRules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Spirit of Light is tiny. Two keys (under Controls > Multiverse of Anime): the LINK key (right bracket by default) links or unlinks the spirit, and with sneak held it
 * calls or sends away the powered form; the COMMAND key (left bracket by default) fires the light beam, and with sneak held the Glowing pulse. The server decides everything (it must be a Light Magic
 * caster, the switch must be on, at most one ask per few ticks is answered, the cooldowns and caps are the server's). The props themselves are ordinary magic props drawn by
 * {@code client.prop.SpiritLightPropPainter}, and the sounds are ordinary level sounds, so there is nothing else to register. Call {@link #init} from the client setup with the mod bus.
 */
public final class SpiritLightClient {
    public static final KeyMapping LINK = new KeyMapping("key.nusmp.spirit_light_link", InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, "key.categories.nusmp");
    public static final KeyMapping COMMAND = new KeyMapping("key.nusmp.spirit_light_command", InputConstants.Type.KEYSYM, InputConstants.KEY_LBRACKET, "key.categories.nusmp");

    private SpiritLightClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.register(LINK); e.register(COMMAND); });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean ready = mc.player != null && mc.screen == null && mc.getConnection() != null;
            boolean sneak = ready && mc.player.isShiftKeyDown();
            while (LINK.consumeClick()) if (ready) PacketDistributor.sendToServer(new SpiritLightNet.Key(sneak ? SpiritLightRules.ACT_POWER : SpiritLightRules.ACT_LINK));
            while (COMMAND.consumeClick()) if (ready) PacketDistributor.sendToServer(new SpiritLightNet.Key(sneak ? SpiritLightRules.ACT_PULSE : SpiritLightRules.ACT_BEAM));
        });
    }
}
