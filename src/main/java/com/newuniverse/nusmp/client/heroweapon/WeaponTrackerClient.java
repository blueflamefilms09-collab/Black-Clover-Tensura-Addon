package com.newuniverse.nusmp.client.heroweapon;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.heroweapon.tracker.WeaponTrackerNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Weapon Tracker decides nothing. The server's reply (to {@code /multiverse weapons}, or to a request) comes in here: a reply that says "open" opens {@link WeaponTrackerScreen}; any
 * other reply only updates a tracker that is already open (a late answer never pops a screen up on its own). The key (Controls > Multiverse of Anime, unbound by default because the mod's keys are already many)
 * opens the screen at once and the screen asks the server; {@link #request} is the one place a request leaves the client, and the server answers a player at most once per two seconds.
 */
public final class WeaponTrackerClient {
    private WeaponTrackerClient() {}

    /** Opens the Weapon Tracker. Unbound by default; the owner or a player binds it in Controls. */
    public static final KeyMapping KEY = new KeyMapping("key.anime_multiverse.hero_tracker", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.anime_multiverse");

    public static void init(IEventBus modBus) {
        WeaponTrackerNet.clientReceive = WeaponTrackerClient::onReply;
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (KEY.consumeClick()) if (mc.player != null && mc.screen == null) mc.setScreen(new WeaponTrackerScreen(null));
        });
    }

    private static void onReply(WeaponTrackerNet.Reply r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof WeaponTrackerScreen s) s.accept(r.table());
        else if (r.open()) mc.setScreen(new WeaponTrackerScreen(r.table()));
    }

    /** Asks the server for a fresh table. */
    static void request() {
        if (Minecraft.getInstance().getConnection() == null) return;
        try {
            PacketDistributor.sendToServer(new WeaponTrackerNet.Request());
        } catch (RuntimeException serverWithoutTheMod) {
            // the channel is optional: a server without the tracker simply never answers, and the screen says so
        }
    }
}
