package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: choosing where a stat point goes needs two things on the wire that the old window packet ({@link SystemNet.Sync}, whose format is deliberately left
 * alone) does not carry. Client to server: the player's REQUESTS, {@link Allocate} (move this many points from the free pool into one stat, or back with a
 * negative number) and {@link Reset} (move every placed point back). They carry only the stat's index and a number and nothing else; the server decides
 * everything ({@link SystemPoints}, {@link PointRules}), so a crafted packet can ask for anything and still gets only what the rules allow. Server to client:
 * {@link PointsSync}, the facts the stat screen needs and the client cannot know: the size of the free pool, the per-stat cap, whether taking points back is
 * allowed, the point mode, and the effect table (what ONE point of each stat does and its ceiling, read from {@code stats.json} on the server, because on a
 * dedicated server the client has no copy of that data pack file). It is sent only when something in it changed. All strings and counts are bounded, so a hostile
 * or damaged packet cannot make the receiver allocate without limit. The channel is optional like the rest of the System: a client without the mod just never
 * asks and never receives.
 */
public final class SystemPointsNet {
    private SystemPointsNet() {}

    /** The most effects one sync carries (the shipped table has six). */
    public static final int MAX_EFFECTS = 32;

    /** Client to server: move {@code amount} points between the free pool and the stat with this index ({@link Stat#ordinal()}). Negative = back to the pool. */
    public record Allocate(int stat, int amount) implements CustomPacketPayload {
        public static final Type<Allocate> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_allocate"));
        public static final StreamCodec<FriendlyByteBuf, Allocate> CODEC = StreamCodec.of(Allocate::write, Allocate::read);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, Allocate p) {
            b.writeByte(p.stat());
            b.writeVarInt(p.amount());
        }

        static Allocate read(FriendlyByteBuf b) {
            int stat = b.readByte();
            int amount = b.readVarInt();
            return new Allocate(stat, amount);
        }
    }

    /** Client to server: move every placed point back into the free pool (when the server allows it). */
    public record Reset() implements CustomPacketPayload {
        public static final Type<Reset> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_reset_points"));
        public static final StreamCodec<FriendlyByteBuf, Reset> CODEC = StreamCodec.of((b, p) -> { }, b -> new Reset());

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What one point of a stat does: the stat's index, the attribute id, the operation ({@link StatTable.Op#ordinal()}), the amount per point and its ceiling (0 = none). */
    public record Effect(int stat, String attribute, int op, double perPoint, double cap) {}

    /**
     * The facts of the stat screen.
     *
     * @param free         unspent points in the pool
     * @param cap          the most points one stat can hold ({@code system.maxStatPoints})
     * @param allowRespec  the player may take placed points back
     * @param mode         the {@link PointMode} code (its ordinal)
     * @param effects      what one point of each stat does
     */
    public record PointsSync(int free, int cap, boolean allowRespec, int mode, List<Effect> effects) implements CustomPacketPayload {
        public static final Type<PointsSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_points"));
        public static final StreamCodec<FriendlyByteBuf, PointsSync> CODEC = StreamCodec.of(PointsSync::write, PointsSync::read);

        /** An empty picture (no free points, the defaults): what the client holds before the server has spoken. */
        public static PointsSync empty() { return new PointsSync(0, 30, true, PointMode.CHOOSE.ordinal(), List.of()); }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, PointsSync p) {
            b.writeVarInt(Math.max(0, p.free()));
            b.writeVarInt(Math.max(0, p.cap()));
            b.writeBoolean(p.allowRespec());
            b.writeByte(p.mode());
            int n = Math.min(p.effects().size(), MAX_EFFECTS);
            b.writeByte(n);
            for (int i = 0; i < n; i++) {
                Effect e = p.effects().get(i);
                b.writeByte(e.stat());
                b.writeUtf(SystemNet.clip(e.attribute(), 96), 96);
                b.writeByte(e.op());
                b.writeDouble(e.perPoint());
                b.writeDouble(e.cap());
            }
        }

        static PointsSync read(FriendlyByteBuf b) {
            int free = b.readVarInt();
            int cap = b.readVarInt();
            boolean respec = b.readBoolean();
            int mode = b.readByte();
            int n = Math.max(0, Math.min(b.readByte(), MAX_EFFECTS));
            List<Effect> effects = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int stat = b.readByte();
                String attribute = b.readUtf(96);
                int op = b.readByte();
                double perPoint = b.readDouble();
                double effectCap = b.readDouble();
                effects.add(new Effect(stat, attribute, op, perPoint, effectCap));
            }
            return new PointsSync(free, cap, respec, mode, effects);
        }
    }

    /** Mod bus: registers the three payloads (optional channels). The client handler is client-only code, reached by name inside the lambda. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(PointsSync.TYPE, PointsSync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.mega.system.SystemClient.receivePoints(p)));
        r.playToServer(Allocate.TYPE, Allocate.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) SystemPoints.allocate(sp, p.stat(), p.amount());
        }));
        r.playToServer(Reset.TYPE, Reset.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) SystemPoints.reset(sp);
        }));
    }
}
