package com.newuniverse.nusmp.skilltree.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Why: NeoForge 21.1 payloads are typed records with a {@code StreamCodec}, and a route (a record of up to 40 nodes with lists and tables inside) is too large for the stock
 * {@code StreamCodec.composite} helpers, which stop at six fields. The byte layout is owned by the pure {@link RouteWire} (so it can be proved without a game); this class is only the thin
 * binding of its three primitives to a {@code FriendlyByteBuf}, which is what the registered payload uses. Decoding throws on anything out of bounds (an exception drops that one connection,
 * the safe answer to a packet that cannot be right); encoding never writes more routes than the reader accepts, so a registry that somehow held more than the cap still produces a packet its
 * peers can read.
 */
public final class RouteCodecs {
    private RouteCodecs() {}

    /** One route. */
    public static final StreamCodec<FriendlyByteBuf, SkillRoute> ROUTE = StreamCodec.of(
            (buf, route) -> RouteWire.write(new BufSink(buf), route),
            buf -> RouteWire.read(new BufSource(buf)));

    /** A list of routes (at most {@link RouteWire#MAX_ROUTES}). */
    public static final StreamCodec<FriendlyByteBuf, List<SkillRoute>> ROUTES = StreamCodec.of(
            (buf, routes) -> RouteWire.writeAll(new BufSink(buf), routes.size() > RouteWire.MAX_ROUTES ? routes.subList(0, RouteWire.MAX_ROUTES) : routes),
            buf -> RouteWire.readAll(new BufSource(buf)));

    private record BufSink(FriendlyByteBuf buf) implements RouteWire.Sink {
        @Override public void varInt(int value) { buf.writeVarInt(value); }

        @Override public void utf(String value, int maxChars) { buf.writeUtf(value, maxChars); }

        @Override public void f64(double value) { buf.writeDouble(value); }
    }

    private record BufSource(FriendlyByteBuf buf) implements RouteWire.Source {
        @Override public int varInt() { return buf.readVarInt(); }

        @Override public String utf(int maxChars) { return buf.readUtf(maxChars); }

        @Override public double f64() { return buf.readDouble(); }
    }
}
