package com.newuniverse.nusmp.relics;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The relics' two packets: the Use Worn Relic key (to the server) and cooldown news (to the client). Register {@link #register} on the mod bus. */
public final class RelicPayloads {
    private RelicPayloads() {}

    /** Mod-bus listener. The client handler only runs on a client, so the client class is never loaded on a server. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(RelicActPayload.TYPE, RelicActPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) RelicEngine.onActPacket(sp, payload); }));
        r.playToClient(RelicCooldownPayload.TYPE, RelicCooldownPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.relics.RelicClientState.cooldown(payload.relicId(), payload.actKey(), payload.readyAt())));
    }
}
