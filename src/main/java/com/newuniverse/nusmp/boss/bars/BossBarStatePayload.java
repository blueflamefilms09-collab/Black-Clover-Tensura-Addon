package com.newuniverse.nusmp.boss.bars;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the server-to-client packet of the custom boss bar: one {@link BossBarState} (style, phase, thresholds, shield, flags, lair action, adds, wind-up, fighters). The byte layout lives in the
 * pure {@link BossBarState#write}, so a unit test round-trips it; this class only lends the {@code FriendlyByteBuf} to it. Registered by {@code BossBarNet.registerPayloads}.
 */
public record BossBarStatePayload(BossBarState state) implements CustomPacketPayload {
    public static final Type<BossBarStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "boss_bar_state"));

    public static final StreamCodec<FriendlyByteBuf, BossBarStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.state().write(new Wr(buf)),
            buf -> new BossBarStatePayload(BossBarState.read(new Rd(buf))));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private record Wr(FriendlyByteBuf b) implements BossBarState.Out {
        @Override public void writeByte(int v) { b.writeByte(v); }
        @Override public void writeShort(int v) { b.writeShort(v); }
        @Override public void writeInt(int v) { b.writeInt(v); }
        @Override public void writeLong(long v) { b.writeLong(v); }
        @Override public void writeUtf(String s, int max) { b.writeUtf(s, max); }
    }

    private record Rd(FriendlyByteBuf b) implements BossBarState.In {
        @Override public int readByte() { return b.readByte(); }
        @Override public int readShort() { return b.readShort(); }
        @Override public int readInt() { return b.readInt(); }
        @Override public long readLong() { return b.readLong(); }
        @Override public String readUtf(int max) { return b.readUtf(max); }
    }

    /** {@code /multiverse bossbar preview}: the server asks one client to show a bar of a style with no boss behind it. */
    public record Preview(String style, int hpPercent, int phase, int phases) implements CustomPacketPayload {
        public static final Type<Preview> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "boss_bar_preview"));
        public static final StreamCodec<FriendlyByteBuf, Preview> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.style(), BossBarState.MAX_STYLE);
                    buf.writeByte(p.hpPercent());
                    buf.writeByte(p.phase());
                    buf.writeByte(p.phases());
                },
                buf -> new Preview(buf.readUtf(BossBarState.MAX_STYLE), buf.readByte() & 0xFF, buf.readByte() & 0xFF, buf.readByte() & 0xFF));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
