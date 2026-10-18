package com.newuniverse.nusmp.heroweapon.curse;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the curse has two packets and they must be registered once, on the mod bus, with the handlers on the right side. {@link CurseSyncPayload} goes to the client
 * (its handler calls the client's receiver by name inside a lambda, so a dedicated server never loads a client class), {@link CurseActPayload} goes to the server
 * (its handler hops onto the server thread and calls the same {@link CurseSystem#embrace} / {@link CurseSystem#decline} the command and the Weapon Book use). Both
 * are optional channels, like the mod's other packets, so a client without the mod simply does not get them.
 */
public final class CurseNet {
    private CurseNet() {}

    /** Mod bus listener: registers both packets. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(CurseSyncPayload.TYPE, CurseSyncPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.heroweapon.curse.CurseClient.receive(p)));
        r.playToServer(CurseActPayload.TYPE, CurseActPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (p.action() == CurseActPayload.EMBRACE) CurseSystem.embrace(sp);
            else if (p.action() == CurseActPayload.DECLINE) CurseSystem.decline(sp);
        }));
    }
}
