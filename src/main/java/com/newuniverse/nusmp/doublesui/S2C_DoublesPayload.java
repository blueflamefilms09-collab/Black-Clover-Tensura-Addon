package com.newuniverse.nusmp.doublesui;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the owner's picture of their Real Doubles, sent to that owner only and only when it changes (one double appears or falls, one is hit by a few percent, the selection moves).
 * At most {@link DoublesUiMath#MAX_ROWS} rows of about 30 bytes; an empty list tells the client to clear the stack. {@code active} is the selected double, or {@link #NONE}.
 * The codec clamps what it reads, so a forged packet cannot hand the client a giant list or a NaN.
 */
public record S2C_DoublesPayload(UUID active, List<DoubleRow> rows) implements CustomPacketPayload {
    public static final UUID NONE = new UUID(0L, 0L);
    public static final Type<S2C_DoublesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "doubles_roster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_DoublesPayload> CODEC = StreamCodec.of(S2C_DoublesPayload::write, S2C_DoublesPayload::read);

    private static void write(RegistryFriendlyByteBuf b, S2C_DoublesPayload p) {
        UUIDUtil.STREAM_CODEC.encode(b, p.active == null ? NONE : p.active);
        int n = Math.min(p.rows.size(), DoublesUiMath.MAX_ROWS);
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            DoubleRow r = p.rows.get(i);
            UUIDUtil.STREAM_CODEC.encode(b, r.id());
            b.writeVarInt(Math.max(0, r.entityId()));
            b.writeFloat(r.health()); b.writeFloat(r.maxHealth());
            b.writeVarInt(Math.max(0, r.lifeLeft())); b.writeVarInt(Math.max(0, r.lifeMax()));
        }
    }

    private static S2C_DoublesPayload read(RegistryFriendlyByteBuf b) {
        UUID active = UUIDUtil.STREAM_CODEC.decode(b);
        int n = Math.min(Math.max(0, b.readVarInt()), DoublesUiMath.MAX_ROWS);
        List<DoubleRow> rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            UUID id = UUIDUtil.STREAM_CODEC.decode(b);
            int eid = b.readVarInt();
            float hp = b.readFloat(), max = b.readFloat();
            int left = b.readVarInt(), lifeMax = b.readVarInt();
            rows.add(new DoubleRow(id, eid, clean(hp), clean(max), Math.min(left, 1 << 20), Math.min(lifeMax, 1 << 20)));
        }
        return new S2C_DoublesPayload(active, rows);
    }

    private static float clean(float v) { return Float.isNaN(v) || Float.isInfinite(v) ? 0f : Math.max(0f, Math.min(1.0e6f, v)); }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
