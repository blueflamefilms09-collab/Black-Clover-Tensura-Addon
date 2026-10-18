package com.newuniverse.nusmp.skilltree.net;

import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: choosing which unlocked route is the ACTIVE class (the one whose techniques can be cast) is the screen's second request to the server, and like the purchase it carries only an id: the
 * class id of a route. The server checks that the player owns a route with that class, that the switch cooldown has run out (the first class is free), and the rate limit, then changes the
 * tree and syncs it back; the client never says "I am now this class", it only asks. The string is bounded on the wire with the data files' limit. Payload id {@code nusmp:tree_set_class}.
 *
 * @param classId the {@code class_id} of the route to make active, {@code fire_dragon_slayer}
 */
public record SetClassPayload(String classId) implements CustomPacketPayload {
    public static final Type<SetClassPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "tree_set_class"));
    public static final StreamCodec<FriendlyByteBuf, SetClassPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(RouteSchema.LEN_ID), SetClassPayload::classId,
            SetClassPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
