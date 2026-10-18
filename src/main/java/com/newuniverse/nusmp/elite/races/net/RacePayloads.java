package com.newuniverse.nusmp.elite.races.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the two messages of the race transformations. CLIENT to SERVER {@link Request}: the hotkey asks for something by NAME ({@code auto}, {@code cycle}, {@code off}) and never
 * sends a number or a state to force; the server re-checks every rule, so a modified client gains nothing. SERVER to CLIENT {@link State}: what the HUD draws (the state id, the
 * ticks left, the colour); the client only stores it. Strings are bounded on the wire.
 */
public final class RacePayloads {
    private RacePayloads() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** C2S: {@code auto} (toggle the best form), {@code cycle} (next form, then back to base) or {@code off}. */
    public record Request(String want) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("elite_race_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Request::want, Request::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: the player's current form for the HUD. {@code state} is a TransformState id; {@code remaining} is ticks left (0 for base). */
    public record State(String state, int remaining, int color) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id("elite_race_state"));
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(16), State::state, ByteBufCodecs.VAR_INT, State::remaining, ByteBufCodecs.INT, State::color, State::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
