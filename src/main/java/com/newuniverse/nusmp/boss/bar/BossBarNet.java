package com.newuniverse.nusmp.boss.bar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

/**
 * The two server-to-client packets of the animated boss bar. {@link Style} says how the bar of a mod boss looks (the vanilla boss bar
 * packets still carry its name, progress and visibility); {@link Clear} says the bar is gone. A bar without a style is drawn like vanilla.
 * Register {@link #registerPayloads} on the mod bus.
 */
public final class BossBarNet {
    private BossBarNet() {}

    /**
     * Style of the vanilla boss bar {@code bar} ({@code ServerBossEvent.getId()}) of boss {@code bossId} (its entity type path):
     * 0xRRGGBB tints, the phase 0-4 and the rage flag.
     */
    public record Style(UUID bar, String bossId, int primary, int secondary, int phase, boolean rage) implements CustomPacketPayload {
        public static final Type<Style> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "boss_bar_style"));
        public static final StreamCodec<FriendlyByteBuf, Style> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.bar());
                    buf.writeUtf(p.bossId(), 64);
                    buf.writeInt(p.primary() & 0xFFFFFF);
                    buf.writeInt(p.secondary() & 0xFFFFFF);
                    buf.writeByte(p.phase());
                    buf.writeBoolean(p.rage());
                },
                buf -> new Style(buf.readUUID(), buf.readUtf(64), buf.readInt(), buf.readInt(), buf.readByte(), buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The bar {@code bar} was removed: forget its style. */
    public record Clear(UUID bar) implements CustomPacketPayload {
        public static final Type<Clear> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "boss_bar_clear"));
        public static final StreamCodec<FriendlyByteBuf, Clear> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUUID(p.bar()),
                buf -> new Clear(buf.readUUID()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Style.TYPE, Style.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.gui.bossbar.BossBarClientState.receive(payload)));
        r.playToClient(Clear.TYPE, Clear.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    com.newuniverse.nusmp.client.gui.bossbar.BossBarClientState.receive(payload);
                    com.newuniverse.nusmp.client.gui.bossbar.CustomBarClient.receive(payload);       // 1.106.0: the custom bar forgets it too (or plays its death animation)
                }));
        // 1.106.0: the custom boss bar (boss/bars): the state of a bar, and the preview of a style
        r.playToClient(com.newuniverse.nusmp.boss.bars.BossBarStatePayload.TYPE, com.newuniverse.nusmp.boss.bars.BossBarStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.gui.bossbar.CustomBarClient.receive(payload.state())));
        r.playToClient(com.newuniverse.nusmp.boss.bars.BossBarStatePayload.Preview.TYPE, com.newuniverse.nusmp.boss.bars.BossBarStatePayload.Preview.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.gui.bossbar.CustomBarClient.preview(payload.style(), payload.hpPercent(), payload.phase(), payload.phases())));
    }
}
