package com.newuniverse.nusmp.client.spiritspace;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceNet;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceRules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Spirit of Space is tiny. Three keys (under Controls > Multiverse of Anime, all rebindable): the link key links or unlinks the spirit; the cycle key picks the ability
 * (portal, gravity, spatial cut, mark a waypoint), with Sneak it picks the waypoint slot; the use key asks for the picked ability. The client keeps only those two picked numbers and shows what is
 * picked on the action bar; the SERVER decides everything (it must be a spatial mage, the switch must be on, the cooldowns, the mana, at most one ask per few ticks is answered). The spirit
 * itself is an ordinary magic prop drawn by {@code client.prop.SpiritSpacePropPainter}. Call {@link #init} from the client setup with the mod bus.
 */
public final class SpiritSpaceClient {
    public static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping LINK = new KeyMapping("key.nusmp.spirit_space_link", InputConstants.Type.KEYSYM, InputConstants.KEY_PERIOD, CATEGORY),
            CYCLE = new KeyMapping("key.nusmp.spirit_space_cycle", InputConstants.Type.KEYSYM, InputConstants.KEY_SEMICOLON, CATEGORY),
            USE = new KeyMapping("key.nusmp.spirit_space_use", InputConstants.Type.KEYSYM, InputConstants.KEY_SLASH, CATEGORY);

    private static final String[] ABILITY_KEYS = {"nusmp.spirit_space.ability.portal", "nusmp.spirit_space.ability.gravity", "nusmp.spirit_space.ability.cut", "nusmp.spirit_space.ability.set"};
    private static final String[] ABILITY_TEXT = {"Portal to the waypoint", "Gravity pulse", "Spatial cut", "Mark the waypoint"};
    private static int ability, slot;

    private SpiritSpaceClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.register(LINK); e.register(CYCLE); e.register(USE); });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean ready = mc.player != null && mc.screen == null && mc.getConnection() != null;
            while (LINK.consumeClick()) if (ready) PacketDistributor.sendToServer(new SpiritSpaceNet.Key(SpiritSpaceRules.ACT_TOGGLE, 0, 0));
            while (CYCLE.consumeClick()) if (ready) cycle(mc);
            while (USE.consumeClick()) if (ready) PacketDistributor.sendToServer(new SpiritSpaceNet.Key(SpiritSpaceRules.ACT_USE, ability, slot));
        });
    }

    private static void cycle(Minecraft mc) {
        if (mc.player.isShiftKeyDown()) slot = (slot + 1) % SpiritSpaceRules.WAYPOINTS_DEFAULT;
        else ability = (ability + 1) % SpiritSpaceRules.A_COUNT;
        Component name = Component.translatableWithFallback(ABILITY_KEYS[ability], ABILITY_TEXT[ability]);
        mc.player.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_space.picked", "Spirit of Space: %s, waypoint %s", name, slot + 1), true);
    }
}
