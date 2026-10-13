package com.newuniverse.nusmp.client.relics.djinn;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.relics.djinn.DjinnNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Djinn keys, under Controls > Multiverse of Anime. J raises the phase one step (Lesser Summon, Half Equip, Full Equip, and from a Full Equip the
 * Extreme Magic); sneak + J dismisses the Djinn at once; K lowers it one step (and lets go of an Extreme Magic that has not struck). J and K are free in
 * vanilla and in the mod's other key maps (R is Use Relic, G the Grimoire Slot); Tensura's ability keys sit on other letters. The server decides what a
 * press means and answers at most every four ticks.
 */
public final class DjinnKeys {
    private DjinnKeys() {}

    public static final KeyMapping UP = new KeyMapping("key.nusmp.djinn_up", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_J, "key.categories.nusmp");
    public static final KeyMapping DOWN = new KeyMapping("key.nusmp.djinn_down", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_K, "key.categories.nusmp");

    /** Draw Rukh (default H): stand still with the bonded vessel on you, no Djinn out, and draw the Rukh of the place into your magicules; press again to stop. */
    public static final KeyMapping RUKH = new KeyMapping("key.nusmp.djinn_rukh", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_H, "key.categories.nusmp");

    /** Mod bus. */
    public static void register(RegisterKeyMappingsEvent e) {
        e.register(UP);
        e.register(DOWN);
        e.register(RUKH);
    }

    /** Game bus, client. */
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (UP.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            PacketDistributor.sendToServer(new DjinnNet.Act(mc.player.isShiftKeyDown() ? DjinnNet.ACT_OFF : DjinnNet.ACT_UP));
        }
        while (DOWN.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            PacketDistributor.sendToServer(new DjinnNet.Act(DjinnNet.ACT_DOWN));
        }
        while (RUKH.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            PacketDistributor.sendToServer(new DjinnNet.Act(DjinnNet.ACT_RUKH));
        }
    }
}
