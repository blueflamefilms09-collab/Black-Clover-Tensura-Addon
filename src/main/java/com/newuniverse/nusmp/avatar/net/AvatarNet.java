package com.newuniverse.nusmp.avatar.net;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Why: one place that declares every packet of the creation screens and says which side handles it. Registered with {@code optional()} like the mod's
 * other nets (a client without the mod does not break a server, and the reverse). Handlers run on the main thread ({@code enqueueWork}); the client
 * handlers sit in {@code client.avatar.AvatarClientNet} and are only touched when a packet arrives on a client (the server never loads them).
 * 1.58.0 adds the six packets of the Soul Forge (finalize, the free aptitude roll, the sample line, the state, the roll result, the birth shake); the
 * five of the classic screen stay exactly as they were.
 */
public final class AvatarNet {
    private AvatarNet() {}

    /** Game-bus listeners (forgets a player's rate-limit entries when they leave). AvatarSystem calls this once with the game bus. */
    public static void registerEvents(IEventBus gameBus) {
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) { AvatarNetServer.onLogout(sp); SoulForgeNetServer.onLogout(sp); } });
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToServer(C2S_RequestStatRollPayload.TYPE, C2S_RequestStatRollPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) AvatarNetServer.onRoll(sp); }));
        r.playToServer(C2S_FinalizeAvatarPayload.TYPE, C2S_FinalizeAvatarPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) AvatarNetServer.onFinalize(sp, p); }));
        r.playToServer(C2S_NameAvatarPayload.TYPE, C2S_NameAvatarPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) AvatarNetServer.onName(sp, p); }));
        r.playToClient(S2C_RollPayload.TYPE, S2C_RollPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onRoll(p)));
        r.playToClient(S2C_RacesPayload.TYPE, S2C_RacesPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onRaces(p)));
        r.playToClient(S2C_CreationResultPayload.TYPE, S2C_CreationResultPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onResult(p)));
        r.playToClient(S2C_OpenActionMenuPayload.TYPE, S2C_OpenActionMenuPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onOpenActions(p)));
        // ---- 1.58.0: the Soul Forge
        r.playToServer(C2S_FinalizeSoulForgePayload.TYPE, C2S_FinalizeSoulForgePayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) SoulForgeNetServer.onFinalize(sp, p); }));
        r.playToServer(C2S_AptitudeRollPayload.TYPE, C2S_AptitudeRollPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) SoulForgeNetServer.onAptitudeRoll(sp, p); }));
        r.playToServer(C2S_BarkRequestPayload.TYPE, C2S_BarkRequestPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) SoulForgeNetServer.onBark(sp, p); }));
        r.playToClient(S2C_SoulForgeStatePayload.TYPE, S2C_SoulForgeStatePayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onForgeState(p)));
        r.playToClient(S2C_AptitudeRollPayload.TYPE, S2C_AptitudeRollPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onAptitudeRoll(p)));
        r.playToClient(S2C_SoulForgeBirthPayload.TYPE, S2C_SoulForgeBirthPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.avatar.AvatarClientNet.onForgeBirth(p)));
    }
}
