package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.client.skilltree.shop.ShopScreen;
import com.newuniverse.nusmp.skilltree.shop.BlackMarketWitch;
import com.newuniverse.nusmp.skilltree.shop.ShopSnapshot;
import com.newuniverse.nusmp.skilltree.shop.ShopSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the client side of the Black Market is two small jobs. First, it is where the two server packets land: {@link #open} puts the shop screen on the player's screen when the
 * server says the shop opened, and {@link #sync} hands a screen that is already open the new picture the server sent after a deal; the packets are registered in common code
 * ({@code ShopNet}) but their handlers call into this client-only class, so a dedicated server never loads a single client class. Second, it makes the hidden shop DISCOVERABLE
 * without a keybind (the owner wants no separate key): while the player's crosshair rests on the old witch it shows a one-line hint in the action bar (sneak and right click,
 * or, once sneaking, right click), refreshed once a second so it does not flicker, in addition to the witch's own hint in her dialogue. Everything the screen shows comes from the
 * server's picture; this class decides nothing.
 */
public final class ShopClient {
    private ShopClient() {}

    private static boolean done;

    /** Called once on the client, from {@code SkillTreeClient.init}. */
    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(ShopClient::onTick);
    }

    /** The server opened the shop: show the screen (or refresh the one that is already open). */
    public static void open(ShopSnapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen open) open.update(snapshot, null);
        else mc.setScreen(new ShopScreen(snapshot));
    }

    /** The server answered a deal: update the open screen; a late packet with no screen open is dropped. */
    public static void sync(ShopSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen open) open.update(payload.snapshot(), payload);
    }

    private static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.level.getGameTime() % 20 != 0) return;
        Entity looked = mc.crosshairPickEntity;
        if (looked instanceof ArcNpcEntity npc && BlackMarketWitch.ID.equals(npc.characterId()) && npc.distanceTo(mc.player) < 6.0f) {
            mc.gui.setOverlayMessage(Component.translatable(mc.player.isShiftKeyDown() ? "gui.nusmp.shop.hint_open" : "gui.nusmp.shop.hint_sneak"), false);
        }
    }
}
