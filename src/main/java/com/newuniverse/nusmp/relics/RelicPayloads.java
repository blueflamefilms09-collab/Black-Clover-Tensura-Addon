package com.newuniverse.nusmp.relics;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The relics' packets (1.99.0: and the worn-list sync and the debug request): the Use Worn Relic key (to the server) and cooldown news (to the client). Register {@link #register} on the mod bus. */
public final class RelicPayloads {
    private RelicPayloads() {}

    /** Mod-bus listener. The client handler only runs on a client, so the client class is never loaded on a server. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(RelicActPayload.TYPE, RelicActPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) RelicEngine.onActPacket(sp, payload); }));
        r.playToClient(RelicCooldownPayload.TYPE, RelicCooldownPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { com.newuniverse.nusmp.client.relics.RelicClientState.cooldown(payload.relicId(), payload.actKey(), payload.readyAt()); com.newuniverse.nusmp.client.relics.item.vu.VuFx.onAct(payload.relicId(), payload.actKey(), payload.readyAt()); }));
        r.playToClient(WornSync.TYPE, WornSync.STREAM_CODEC,                                                  // 1.99.0: what a wearer carries outside the vanilla equipment (Curios, a mob's pack)
                (payload, ctx) -> ctx.enqueueWork(() -> WornSync.put(payload.wearer(), payload.stacks())));
        r.playToClient(WornDebugPayload.TYPE, WornDebugPayload.STREAM_CODEC,                                  // 1.99.0: /multiverse relic debug asks the client for its half of the report
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.relics.worn.WornDebugClient.report(payload.target(), payload.serverLines())));
    }
}
