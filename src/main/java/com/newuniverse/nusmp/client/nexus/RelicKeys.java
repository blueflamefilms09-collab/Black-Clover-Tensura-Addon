package com.newuniverse.nusmp.client.nexus;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.dungeon.nexus.relic.RelicNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The "Use Relic" key (default R, under Controls > Multiverse of Anime): uses the Gambler's Grimoire Page worn in a Dungeon Relic slot, which a
 * Curios item cannot be right-clicked for ({@link RelicNet}). Register from the client section of NUSMP:
 * {@code modEventBus.addListener(RelicKeys::register)} on the mod bus and {@code NeoForge.EVENT_BUS.addListener(RelicKeys::onClientTick)} on the game bus.
 */
public final class RelicKeys {
    private RelicKeys() {}

    public static final KeyMapping USE = new KeyMapping("key.nusmp.use_relic", InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_R, "key.categories.nusmp");

    /** Mod bus: puts the key in the Controls screen. */
    public static void register(RegisterKeyMappingsEvent e) { e.register(USE); }

    /** Game bus, client: every press goes to the server, which decides whether there is a Page to use. */
    public static void onClientTick(ClientTickEvent.Post e) {
        while (USE.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) PacketDistributor.sendToServer(new RelicNet.UseRelic());
        }
    }
}
