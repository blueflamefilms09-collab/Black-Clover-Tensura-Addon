package com.newuniverse.nusmp.skilltree.net;

import com.newuniverse.nusmp.skilltree.data.RouteCodecs;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Why: the game does not send a mod's own datapack JSON to clients, yet the screen needs every route's name, nodes, prices and texts, and the cast gate needs each route's class. So the
 * server sends the routes it validated, ALL of them in one payload (they are small, capped at 900 KB in total by the registry, well under what a clientbound payload may carry), when a player
 * joins and every time the datapacks reload, and the client replaces its whole copy with the list. One payload for the whole set keeps the client from ever holding half of a reload. It is
 * server-to-client only: a client cannot change the routes. Payload id {@code nusmp:tree_routes}; the byte layout is {@code RouteWire}.
 *
 * @param routes every loaded route, in id order
 */
public record RoutesPayload(List<SkillRoute> routes) implements CustomPacketPayload {
    public static final Type<RoutesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "tree_routes"));
    public static final StreamCodec<FriendlyByteBuf, RoutesPayload> STREAM_CODEC = StreamCodec.composite(RouteCodecs.ROUTES, RoutesPayload::routes, RoutesPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
