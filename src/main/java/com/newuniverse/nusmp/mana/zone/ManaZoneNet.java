package com.newuniverse.nusmp.mana.zone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The three small packets of the Mana Zone, all server to client and all optional (a client without the mod simply sees no zone):
 * <ul>
 *   <li>{@link ZonePayload}: the TRACKED STATE of one caster's zone (centre, radius cap, tier, style, tint, start tick and the ticks elapsed). Sent when the
 *       zone starts, when it ends, and as a cheap refresh every {@code refresh_interval} ticks (100 by default: five seconds) so a player who comes into range
 *       later picks it up. The radius is never sent: the client computes it from the start (the growth is a function of the elapsed ticks).</li>
 *   <li>{@link GrandZonePayload}: the tiny state of the Grand Magic Zone a player stands in (the region's shape, theme, effect mask, density and the player's own
 *       harm and boon), sent to that player only, on entering, on leaving, when the harm or boon moves a twentieth, and every 100 ticks.</li>
 *   <li>{@link GatePayload}: whether the late-game gate is open for this player (the grimoire's menu shows the Mana Zone pages only then) and the attunement.</li>
 * </ul>
 */
public final class ManaZoneNet {
    private ManaZoneNet() {}

    public static final int START = 0, REFRESH = 1, END = 2, COLLAPSE = 3;

    /** op, owner entity id, serial (the zone's unique number, the effect's seed), centre, radius cap, growth ticks, tier (0 Mana Zone), style, tint, start tick, ticks elapsed. */
    public record ZonePayload(int op, int ownerId, long serial, float cx, float cy, float cz, float radiusCap, int growTicks, int tier, int style, int tint, long startTick, int elapsed)
            implements CustomPacketPayload {
        public static final Type<ZonePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone"));
        public static final StreamCodec<FriendlyByteBuf, ZonePayload> CODEC = CustomPacketPayload.codec(ZonePayload::write, ZonePayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeByte(op);
            b.writeVarInt(ownerId);
            b.writeLong(serial);
            b.writeFloat(cx);
            b.writeFloat(cy);
            b.writeFloat(cz);
            b.writeFloat(radiusCap);
            b.writeVarInt(growTicks);
            b.writeByte(tier);
            b.writeByte(style);
            b.writeInt(tint);
            b.writeLong(startTick);
            b.writeVarInt(elapsed);
        }

        private static ZonePayload read(FriendlyByteBuf b) {
            return new ZonePayload(b.readByte(), b.readVarInt(), b.readLong(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readByte(), b.readByte(),
                    b.readInt(), b.readLong(), b.readVarInt());
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * present false = the player left the zone (the rest is ignored). Shape: sphere (centre, radius) or box (min, max); theme and effect mask are the ordinals
     * of GrandZoneRegion.Theme / Effect; density in tenths, harm and boon in hundredths (the player's own, before the edge fade, which the client computes).
     */
    public record GrandZonePayload(boolean present, String id, String name, boolean box, float cx, float cy, float cz, float radius,
                                   float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int theme, int effects, int densityTenths, int harmHundredths,
                                   int boonHundredths, int tint) implements CustomPacketPayload {
        public static final Type<GrandZonePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "grand_zone"));
        public static final StreamCodec<FriendlyByteBuf, GrandZonePayload> CODEC = CustomPacketPayload.codec(GrandZonePayload::write, GrandZonePayload::read);

        public static GrandZonePayload none() { return new GrandZonePayload(false, "", "", false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }

        private void write(FriendlyByteBuf b) {
            b.writeBoolean(present);
            if (!present) return;
            b.writeUtf(id, 64);
            b.writeUtf(name, 64);
            b.writeBoolean(box);
            for (float f : new float[]{cx, cy, cz, radius, minX, minY, minZ, maxX, maxY, maxZ}) b.writeFloat(f);
            b.writeByte(theme);
            b.writeVarInt(effects);
            b.writeByte(densityTenths);
            b.writeByte(harmHundredths);
            b.writeByte(boonHundredths);
            b.writeInt(tint);
        }

        private static GrandZonePayload read(FriendlyByteBuf b) {
            if (!b.readBoolean()) return none();
            String id = b.readUtf(64), name = b.readUtf(64);
            boolean box = b.readBoolean();
            float cx = b.readFloat(), cy = b.readFloat(), cz = b.readFloat(), radius = b.readFloat(), minX = b.readFloat(), minY = b.readFloat(), minZ = b.readFloat(),
                    maxX = b.readFloat(), maxY = b.readFloat(), maxZ = b.readFloat();
            return new GrandZonePayload(true, id, name, box, cx, cy, cz, radius, minX, minY, minZ, maxX, maxY, maxZ, b.readByte(), b.readVarInt(), b.readByte(), b.readByte(), b.readByte(), b.readInt());
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record GatePayload(boolean open, int attuneSeconds, int attuneNeeded) implements CustomPacketPayload {
        public static final Type<GatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_gate"));
        public static final StreamCodec<FriendlyByteBuf, GatePayload> CODEC = CustomPacketPayload.codec(GatePayload::write, GatePayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeBoolean(open);
            b.writeVarInt(attuneSeconds);
            b.writeVarInt(attuneNeeded);
        }

        private static GatePayload read(FriendlyByteBuf b) { return new GatePayload(b.readBoolean(), b.readVarInt(), b.readVarInt()); }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener: registers the three packets (client-bound only, optional). */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(ZonePayload.TYPE, ZonePayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.manazone.ManaZoneClient.onZone(p)));
        r.playToClient(GrandZonePayload.TYPE, GrandZonePayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.manazone.ManaZoneClient.onGrandZone(p)));
        r.playToClient(GatePayload.TYPE, GatePayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ManaZoneClientGate.set(p.open(), p.attuneSeconds(), p.attuneNeeded())));
    }
}
