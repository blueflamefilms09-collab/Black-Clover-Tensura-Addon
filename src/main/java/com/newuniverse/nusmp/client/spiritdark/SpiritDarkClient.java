package com.newuniverse.nusmp.client.spiritdark;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.spiritdark.SpiritDarkNet;
import com.newuniverse.nusmp.spiritdark.SpiritDarkRules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Spirit of Darkness is tiny. Three keys (under Controls > Multiverse of Anime; the right bracket, the semicolon and the apostrophe by default, all rebindable) ask the
 * server to link or unlink the spirit, to Shadow Step and to call or dismiss the powered form; the server decides (it must be a Dark Magic caster, the switch must be on, at most one ask per half
 * second is answered). The props themselves are ordinary magic props drawn by {@code client.prop.SpiritDarkPropPainter}, the sounds come as ordinary sound packets from the server, so there is
 * nothing else to register. Call {@link #init} from the client setup with the mod bus.
 */
public final class SpiritDarkClient {
    public static final KeyMapping LINK = new KeyMapping("key.nusmp.spirit_dark_link", InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, "key.categories.nusmp");
    public static final KeyMapping STEP = new KeyMapping("key.nusmp.spirit_dark_step", InputConstants.Type.KEYSYM, InputConstants.KEY_SEMICOLON, "key.categories.nusmp");
    public static final KeyMapping POWER = new KeyMapping("key.nusmp.spirit_dark_power", InputConstants.Type.KEYSYM, InputConstants.KEY_APOSTROPHE, "key.categories.nusmp");

    private SpiritDarkClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.register(LINK); e.register(STEP); e.register(POWER); });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            ask(LINK, SpiritDarkRules.ACT_LINK);
            ask(STEP, SpiritDarkRules.ACT_STEP);
            ask(POWER, SpiritDarkRules.ACT_POWER);
        });
    }

    private static void ask(KeyMapping key, int action) {
        while (key.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(new SpiritDarkNet.Key(action));
        }
    }
}
