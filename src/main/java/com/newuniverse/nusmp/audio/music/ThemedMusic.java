package com.newuniverse.nusmp.audio.music;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the registration entry of the themed music, in the shape of the other builders' registries so the main mod class needs one line each: {@link #init} on the mod bus (the 25
 * sound events and the state payload) and {@link #register} on the game bus (the once-a-second server evaluation and its clean-up). The client half is
 * {@code client/music/ThemedMusicEngine}, started from the client block of the mod class. The self-test {@code dungeon.music} is {@link MusicSelfTests#register()}.
 */
public final class ThemedMusic {
    private ThemedMusic() {}

    public static void init(IEventBus modBus) {
        MusicSounds.SOUNDS.register(modBus);
        modBus.addListener(MusicNet::register);
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((ServerTickEvent.Post e) -> MusicServer.tick(e.getServer()));
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> MusicServer.forget(e.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent e) -> MusicServer.clear());
    }
}
