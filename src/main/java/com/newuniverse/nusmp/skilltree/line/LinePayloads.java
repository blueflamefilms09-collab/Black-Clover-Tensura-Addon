package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteCodecs;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Why: the Unique Skill Lines add six messages and every one of them has a different trust story, so they are listed together to make the boundary easy to audit. CLIENT to SERVER (the client asks,
 * the server decides and re-checks everything, the packets carry ids and one flag and never a number): {@link CastArt} (use an art of the equipped line), {@link TogglePassive} (switch a passive
 * on or off), {@link Inspect} (an administrator asks for the full tree of one line, or for the list of cards again). SERVER to CLIENT (the client only stores or shows): {@link ArtState} (an art's
 * cooldown started, the stance that is active), {@link Cards} (the identity of every line, no nodes, see {@code LineCard}) and {@link More} (further routes to ADD to the client's copy: the chunks of
 * a sync after the first, and a line an administrator asked to inspect). Each string is bounded on the wire by the same limits as the data files, so an oversized packet fails while it is decoded.
 * They are registered by {@code LineNet} on the mod bus as optional payloads with protocol "1", like every other payload of this mod.
 */
public final class LinePayloads {
    private LinePayloads() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static final int NODE_KEY = RouteSchema.LEN_ROUTE_ID + 1 + RouteSchema.LEN_ID;

    /** C2S: use the art {@code node} of the line {@code route}. */
    public record CastArt(String route, String node) implements CustomPacketPayload {
        public static final Type<CastArt> TYPE = new Type<>(id("line_cast_art"));
        public static final StreamCodec<FriendlyByteBuf, CastArt> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(RouteSchema.LEN_ROUTE_ID), CastArt::route, ByteBufCodecs.stringUtf8(RouteSchema.LEN_ID), CastArt::node, CastArt::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** C2S: switch the passive {@code node} of {@code route} on or off. */
    public record TogglePassive(String route, String node, boolean on) implements CustomPacketPayload {
        public static final Type<TogglePassive> TYPE = new Type<>(id("line_toggle_passive"));
        public static final StreamCodec<FriendlyByteBuf, TogglePassive> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(RouteSchema.LEN_ROUTE_ID), TogglePassive::route, ByteBufCodecs.stringUtf8(RouteSchema.LEN_ID), TogglePassive::node,
                ByteBufCodecs.BOOL, TogglePassive::on, TogglePassive::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** C2S: an administrator asks for the full tree of one line ({@code route}), or, with an empty route, for the cards of every line again. */
    public record Inspect(String route) implements CustomPacketPayload {
        public static final Type<Inspect> TYPE = new Type<>(id("line_inspect"));
        public static final StreamCodec<FriendlyByteBuf, Inspect> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(RouteSchema.LEN_ROUTE_ID), Inspect::route, Inspect::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: the art {@code key} ({@code route#node}) is on cooldown for {@code cooldownTicks}; {@code stance} is the key of the stance that is active now, or empty. */
    public record ArtState(String key, int cooldownTicks, String stance) implements CustomPacketPayload {
        public static final Type<ArtState> TYPE = new Type<>(id("line_art_state"));
        public static final StreamCodec<FriendlyByteBuf, ArtState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(NODE_KEY), ArtState::key, ByteBufCodecs.VAR_INT, ArtState::cooldownTicks, ByteBufCodecs.stringUtf8(NODE_KEY), ArtState::stance, ArtState::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: the cards of every registered line (no nodes). */
    public record Cards(List<LineCard> cards) implements CustomPacketPayload {
        public static final Type<Cards> TYPE = new Type<>(id("line_cards"));
        public static final StreamCodec<FriendlyByteBuf, Cards> STREAM_CODEC = StreamCodec.composite(RouteCodecs.CARDS, Cards::cards, Cards::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: routes to ADD to the client's copy (a route with the same id is replaced). */
    public record More(List<SkillRoute> routes) implements CustomPacketPayload {
        public static final Type<More> TYPE = new Type<>(id("line_routes_more"));
        public static final StreamCodec<FriendlyByteBuf, More> STREAM_CODEC = StreamCodec.composite(RouteCodecs.ROUTES, More::routes, More::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
