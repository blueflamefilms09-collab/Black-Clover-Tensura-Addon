package com.newuniverse.nusmp.doublesui;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Why: declares the two packets of the Real Double stack (docs/doubles_ui.md) and who handles them. Registered {@code optional()} like the mod's other nets. The server handler
 * trusts nothing from the packet but the op and the id: the sender is {@code ctx.player()}, and {@link DoublesRoster#select} checks the sender's own doubles, the level, the range
 * and the rate. The client handler sits in {@code client.doublesui.DoublesClient} and is only named inside a lambda that runs on a client, so a dedicated server never loads it.
 */
public final class DoublesUiNet {
    private DoublesUiNet() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToServer(C2S_DoubleSelectPayload.TYPE, C2S_DoubleSelectPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) DoublesRoster.select(sp, p.op(), p.id()); }));
        r.playToClient(S2C_DoublesPayload.TYPE, S2C_DoublesPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.doublesui.DoublesClient.onRoster(p)));
    }
}
