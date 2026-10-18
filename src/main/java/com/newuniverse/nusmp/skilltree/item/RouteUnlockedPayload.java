package com.newuniverse.nusmp.skilltree.item;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the banner and the screen flash of an unlock are drawn by the CLIENT of the player who unlocked the route, and the only thing the client cannot know for itself
 * is that the unlock just happened (the tree it receives afterwards is a silent snapshot, not an event). So the server sends this one small server-to-client packet,
 * {@code nusmp:tree_route_unlocked}, carrying only the route id: the client looks the route up in its own synced copy of the registry for the name and the colour, so
 * the packet stays a few bytes, cannot disagree with what the screen shows elsewhere, and cannot carry anything a modified server would not already be able to say. It
 * is a {@link CustomPacketPayload} record with a {@link StreamCodec} registered in {@code RegisterPayloadHandlersEvent} on the mod bus because NeoForge 21.1 has no
 * {@code SimpleChannel}; the codec bounds the string, so a malformed packet fails loudly at decode instead of allocating. It is registered as optional (like the mod's
 * other cosmetic packets) so a client without the mod is not refused by a server that has it. The handler goes through {@code enqueueWork} to the render thread and
 * reaches the client class by name inside a lambda, so a dedicated server never loads a client class.
 */
public record RouteUnlockedPayload(String routeId) implements CustomPacketPayload {
    public static final Type<RouteUnlockedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "tree_route_unlocked"));

    /** Longest route id the codec accepts (a resource location is far shorter in practice). */
    public static final int MAX_ID = 128;

    public static final StreamCodec<FriendlyByteBuf, RouteUnlockedPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeUtf(p.routeId(), MAX_ID),
            buf -> new RouteUnlockedPayload(buf.readUtf(MAX_ID)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Mod-bus listener: registers the packet (server to client only). */
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToClient(TYPE, STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.skilltree.ItemsClient.onRouteUnlocked(payload.routeId())));
    }
}
