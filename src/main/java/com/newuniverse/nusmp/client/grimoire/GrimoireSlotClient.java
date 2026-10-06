package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.client.multiverse.MultiverseClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client side of the Grimoire Slot (0.22): the slot screen, the "Grimoire Slot" key (unbound by default, under Controls >
 * Multiverse of Anime; the Multiverse status screen also has a button), and the HUD cleanup: no floating names, titles or other
 * text above players' heads (config {@code [hud] hideHeadText}, default on).
 */
public final class GrimoireSlotClient {
    public static final KeyMapping KEY = new KeyMapping("key.nusmp.grimoire_slot", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.nusmp");

    /** Summon Grimoire (default G): your book floats from the hip to your right hand; with shift held it goes back to the hip. */
    public static final KeyMapping SUMMON = new KeyMapping("key.nusmp.grimoire_summon", InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_G, "key.categories.nusmp");

    private GrimoireSlotClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.register(KEY); e.register(SUMMON); });
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(GrimoireSlot.MENU.get(), GrimoireSlotScreen::new));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (KEY.consumeClick()) open();
            while (SUMMON.consumeClick()) {
                var mc = Minecraft.getInstance();
                if (mc.player != null) PacketDistributor.sendToServer(new com.newuniverse.nusmp.book.GrimoireSummon.KeyPayload(mc.player.isShiftKeyDown()));
            }
        });
        NeoForge.EVENT_BUS.addListener(GrimoireSlotClient::onNameTag);
    }

    /** Asks the server to open the Grimoire Slot. */
    public static void open() {
        if (Minecraft.getInstance().player != null) PacketDistributor.sendToServer(GrimoireSlot.OpenPayload.INSTANCE);
    }

    private static void onNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player && MultiverseClientConfig.get(MultiverseClientConfig.HIDE_HEAD_TEXT)) event.setCanRender(TriState.FALSE);
    }
}
