package com.newuniverse.nusmp.mana.method;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The one packet of the Mana Method: server to client only, optional (a client without the mod simply sees no rune), about 28 bytes. It tells the clients near a cast WHERE the rune
 * forms, which way the caster looks, what it says ({@link RuneSpec#pack}) and for how many ticks it draws itself; the client works out the rings, the symbols and the particles
 * itself ({@link RuneGeneratorUtil}). Sent once per cast to the players within {@value #RANGE} blocks, and only if the effect packet budget of the tick allows it.
 *
 * <p><b>3.5.0, the hold.</b> A second small clientbound packet, {@link HoldPayload} (about 7 bytes), tells the clients near a caster that a page is being HELD, was RELEASED after N ticks or was
 * CANCELLED, so the rings can be written while the player holds. It is a new packet on purpose: {@link RunePayload} is unchanged (28 bytes), so a client that does not know the hold packet
 * still draws the rune of the cast exactly as before. The client works out the picture from the clock of the hold ({@link RuneHoldView}); the server sends three packets per cast at most.
 *
 * <p>There is no serverbound packet: casting goes through the grimoire pages, so there is no sender to trust. The client still validates what it receives (it rejects bad numbers
 * and a charge outside 1..60), because a client must not trust a server it did not choose either.
 */
public final class RuneNet {
    private RuneNet() {}

    public static final double RANGE = 64.0;

    /** x, y, z of the rune's centre, the caster's look (a unit vector), the packed spec, the ticks of drawing, the entity id of the caster (the own rune is drawn at full detail). */
    public record RunePayload(float x, float y, float z, float lx, float ly, float lz, int spec, int charge, int ownerId) implements CustomPacketPayload {
        public static final Type<RunePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "mana_method_rune"));
        public static final StreamCodec<FriendlyByteBuf, RunePayload> CODEC = CustomPacketPayload.codec(RunePayload::write, RunePayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeFloat(x);
            b.writeFloat(y);
            b.writeFloat(z);
            b.writeFloat(lx);
            b.writeFloat(ly);
            b.writeFloat(lz);
            b.writeByte(spec);
            b.writeByte(charge);
            b.writeVarInt(ownerId);
        }

        private static RunePayload read(FriendlyByteBuf b) {
            return new RunePayload(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readByte() & 0x3F, b.readByte() & 0x7F, b.readVarInt());
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static final int PHASE_HOLD = 1, PHASE_RELEASE = 2, PHASE_CANCEL = 3;

    /** The hold of a page: who (the entity id of the caster), what happened, the packed spec (HOLD), the ticks held (RELEASE), and the two hold limits the client times the rings by. */
    public record HoldPayload(int ownerId, int phase, int spec, int held, int minHold, int maxHold) implements CustomPacketPayload {
        public static final Type<HoldPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "mana_method_hold"));
        public static final StreamCodec<FriendlyByteBuf, HoldPayload> CODEC = CustomPacketPayload.codec(HoldPayload::write, HoldPayload::read);

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(ownerId);
            b.writeByte(phase);
            b.writeByte(spec);
            b.writeByte(held);
            b.writeByte(minHold);
            b.writeByte(maxHold);
        }

        private static HoldPayload read(FriendlyByteBuf b) {
            return new HoldPayload(b.readVarInt(), b.readByte() & 0x07, b.readByte() & 0x3F, b.readByte() & 0xFF, b.readByte() & 0xFF, b.readByte() & 0xFF);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What the client may trust of a hold packet: a known phase, a hold inside the grimoire's own limit, the two limits inside the ranges the settings allow. */
    public static boolean validHold(HoldPayload p) {
        if (p.phase() < PHASE_HOLD || p.phase() > PHASE_CANCEL || p.ownerId() < 0) return false;
        if (p.held() > RuneCharge.HOLD_CAP) return false;
        return p.minHold() >= RuneCharge.MIN_HOLD_LOW && p.minHold() <= RuneCharge.MIN_HOLD_HIGH && p.maxHold() >= p.minHold() + RuneCharge.MIN_SPAN && p.maxHold() <= RuneCharge.MAX_HOLD_HIGH;
    }

    /**
     * Tells the players near the caster about a hold. Dropped (false) when the tick's effect packet budget is spent: the hold and the cast are not affected, the rune of that cast is then
     * drawn by the old fixed path. At most one HOLD, one RELEASE (or CANCEL) per cast.
     */
    public static boolean sendHold(ServerPlayer caster, int phase, RuneSpec spec, int held, int minHold, int maxHold) {
        if (!com.newuniverse.nusmp.perf.PerfBudgets.effectPacket(caster.level().getGameTime())) return false;
        HoldPayload p = new HoldPayload(caster.getId(), phase, spec == null ? 0 : spec.pack(), Math.max(0, Math.min(RuneCharge.HOLD_CAP, held)), minHold, maxHold);
        PacketDistributor.sendToPlayersNear(caster.serverLevel(), null, caster.getX(), caster.getY(), caster.getZ(), RANGE, p);
        return true;
    }

    /** What the client may trust of a received payload: finite numbers, a unit look, a charge in 1..60. */
    public static boolean valid(RunePayload p) {
        for (float f : new float[]{p.x(), p.y(), p.z(), p.lx(), p.ly(), p.lz()}) if (Float.isNaN(f) || Float.isInfinite(f)) return false;
        return p.charge() >= 1 && p.charge() <= 60;
    }

    /** Sends the rune to the players near it; false when the tick's effect packet budget is spent (the cast itself is not affected). */
    public static boolean send(ServerLevel level, Vec3 center, Vec3 look, RuneSpec spec, int charge, int ownerId) {
        if (!com.newuniverse.nusmp.perf.PerfBudgets.effectPacket(level.getGameTime())) return false;
        RunePayload p = new RunePayload((float) center.x, (float) center.y, (float) center.z, (float) look.x, (float) look.y, (float) look.z, spec.pack(), Math.min(charge, 60), ownerId);
        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, RANGE, p);
        return true;
    }

    /** Mod-bus listener: registers the packet (client-bound only, optional). */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(RunePayload.TYPE, RunePayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.manamethod.RuneClient.onRune(p)));
        r.playToClient(HoldPayload.TYPE, HoldPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.manamethod.RuneClient.onHold(p)));
    }
}
