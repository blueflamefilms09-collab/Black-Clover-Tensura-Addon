package com.newuniverse.nusmp.skilltree.net;

import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: buying a node is the one thing the screen asks the server to do, and the request carries NOTHING but a route id and a node id: no price, no level, no Skill Point count. The server
 * looks the node up in its own registry, reads the player's own tree, and decides (the route must be unlocked, the node must exist, the parents must be at their levels, the node must not be
 * maxed, the points must cover the price, the player must be alive, and no more than the configured number of requests per second may come in). A modified client therefore cannot buy what it
 * cannot afford; at worst it can ask for something the server refuses. The two strings are bounded on the wire with the same limits as the data files, so an oversized packet is rejected while
 * it is decoded. The packet cannot name a Black Clover grimoire page: it has no field that could. Payload id {@code nusmp:tree_unlock_node}.
 *
 * @param route the route id, {@code nusmp:fire_dragon_slayer}
 * @param node  the node id inside that route, {@code roar}
 */
public record UnlockNodePayload(String route, String node) implements CustomPacketPayload {
    public static final Type<UnlockNodePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "tree_unlock_node"));
    public static final StreamCodec<FriendlyByteBuf, UnlockNodePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(RouteSchema.LEN_ROUTE_ID), UnlockNodePayload::route,
            ByteBufCodecs.stringUtf8(RouteSchema.LEN_ID), UnlockNodePayload::node,
            UnlockNodePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
