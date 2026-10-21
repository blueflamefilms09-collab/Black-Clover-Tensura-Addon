package com.newuniverse.nusmp.audio.voice;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the registration entry of the spoken voices, in the shape of the other builders' registries so the main mod class needs one line each: {@link #init} on the mod bus (the 120 sound
 * events, the payload, the server config {@code nusmp-voice-server.toml}) and {@link #register} on the game bus (the per-player clean-up and the self-test {@code audio.voices}). The
 * client half is {@code client/voice/VoiceClient}, whose config {@code nusmp-voice-client.toml} is registered from the client block of the mod class.
 */
public final class VoiceBoot {
    private VoiceBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        VoiceSounds.SOUNDS.register(modBus);
        modBus.addListener(VoiceNet::register);
        container.registerConfig(ModConfig.Type.SERVER, VoiceConfig.SPEC, "nusmp-voice-server.toml");
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Voice.forget(e.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent e) -> Voice.clear());
        gameBus.addListener(VoiceBoot::onDamage);
        VoiceSelfTests.register();
    }

    /** The System warns a delver whose hit points just fell to a quarter or less (inside the Mega-Dungeon only, at most once a minute per player). */
    private static void onDamage(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !com.newuniverse.nusmp.labyrinth.Labyrinth.inMega(p.level())) return;
        if (VoicePicker.lowHealth(p.getHealth(), p.getMaxHealth())) Voice.say(p, "system_low_health", 1200L);
    }
}
