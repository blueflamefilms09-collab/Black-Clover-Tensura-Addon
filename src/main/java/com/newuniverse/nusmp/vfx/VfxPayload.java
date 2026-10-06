package com.newuniverse.nusmp.vfx;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The ONLY thing the server sends for an effect (~60 bytes). Clients build every vertex themselves.
 *
 * @param shape        which effect (VfxShape ordinal)
 * @param from         start / caster position
 * @param to           end / target position
 * @param color        ARGB tint (0 = use the effect's default from vfx/effects.json)
 * @param duration     ticks (0 = default)
 * @param power        scale/intensity (1 = normal)
 * @param followEntity entity id the effect sticks to, or -1
 * @param seed         randomness seed so every client sees the same pattern
 */
public record VfxPayload(int shape, Vec3 from, Vec3 to, int color, int duration, float power, int followEntity, long seed)
        implements CustomPacketPayload {

    public static final Type<VfxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "vfx"));
    public static final StreamCodec<FriendlyByteBuf, VfxPayload> STREAM_CODEC =
            CustomPacketPayload.codec(VfxPayload::write, VfxPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(shape);
        buf.writeVec3(from);
        buf.writeVec3(to);
        buf.writeInt(color);
        buf.writeVarInt(duration);
        buf.writeFloat(power);
        buf.writeVarInt(followEntity + 1);
        buf.writeLong(seed);
    }

    private static VfxPayload read(FriendlyByteBuf buf) {
        return new VfxPayload(buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readInt(),
                buf.readVarInt(), buf.readFloat(), buf.readVarInt() - 1, buf.readLong());
    }

    public VfxShape shapeType() { return VfxShape.byId(shape); }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
