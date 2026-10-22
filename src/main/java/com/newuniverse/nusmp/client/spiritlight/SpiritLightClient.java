package com.newuniverse.nusmp.client.spiritlight;

import com.newuniverse.nusmp.spiritlight.SpiritLightNet;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: the client half of the Spirit of Light is tiny. Until 3.0.0 it also had two keys; they are gone (the spirit is called from the grimoire's spirit pages and {@code /multiverse spirit}, decided on the
 * server, see {@code spiritcontrol}). What is left is the hook that plays the seraph's spoken lines: the server sends the id of a line and the speaker's entity id to the players within 32 blocks, and the
 * common packet class names no client class, so this setup puts the player into {@link SpiritLightNet#CLIENT_VOICE}. The props themselves are ordinary magic props drawn by
 * {@code client.prop.SpiritLightPropPainter}, and the sounds are ordinary level sounds. Call {@link #init} from the client setup with the mod bus.
 */
public final class SpiritLightClient {
    private SpiritLightClient() {}

    public static void init(IEventBus modBus) {
        SpiritLightNet.CLIENT_VOICE = SpiritLightVoiceClient::play;                                    // the seraph's spoken lines are played here (the common packet class names no client class)
    }
}
