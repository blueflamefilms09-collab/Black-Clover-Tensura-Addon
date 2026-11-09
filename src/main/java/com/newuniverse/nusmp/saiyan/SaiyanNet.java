package com.newuniverse.nusmp.saiyan;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why: the Saiyan system talks over three tiny packets and the server decides everything (3.63.0, extended in 3.65.0). UP: {@link Action}, one byte for what the player asked (a Ki attack, take off, sense,
 * a Kamehameha press or release, a form); it is only an ask: the sender must be a real Saiyan, the number is range-checked and asks are rate limited ({@link #MIN_GAP_TICKS}; a Kamehameha release is never
 * dropped, so a charge can not be stuck). DOWN, only to that player: {@link Sync}, the HUD state, at most four a second and only when something changed; and {@link Ping}, the radar of the Saiyan Senses
 * (at most {@link SaiyanSenseRules#MAX_ENTRIES} entries, one every two seconds while the radar is on). The client side is hooked in by {@link #clientSync} / {@link #clientPing} (set by the client class), so
 * this common file names no client class. Both directions are optional: a client without the mod simply never gets them.
 */
public final class SaiyanNet {
    private SaiyanNet() {}

    public static final int OP_BLAST = 1, OP_BARRAGE = 2, OP_FLIGHT = 3, OP_BURST = 4, OP_SENSE = 5, OP_SUPPRESS = 6, OP_COMBO = 7, OP_KAME_START = 8, OP_KAME_RELEASE = 9,
            OP_FORM_TOGGLE = 10, OP_FORM_NEXT = 11, OP_FORM_PREV = 12,
            OP_TRANSMIT = 13, OP_BARRIER = 14, OP_SPIRIT_START = 15, OP_SPIRIT_RELEASE = 16, OP_METEOR = 17,
            OP_DRAGON = 18, OP_BIND = 19, OP_DEFLECT = 20, OP_ENERGY = 21;
    public static final int MIN_GAP_TICKS = 4;

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Sync> clientSync;
    public static volatile Consumer<Ping> clientPing;

    /** What the player asked: {@code op} (see OP_*). */
    public record Action(int op) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "saiyan_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.op()), buf -> new Action(buf.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * The HUD state. {@code saiyan} is whether the player is a Saiyan at all (false = hide the Saiyan HUD and show Tensura's own magicule bar again); {@code ki} and {@code kiMax} are the player's
     * Tensura magicule, shown as Ki; {@code stamina}/{@code staminaMax}; {@code battlePower}; {@code rank} (ordinal); {@code awakened}; the two Stage 1 cooldowns in ticks left; {@code meditate}, the
     * awakening progress in percent. 3.65.0 adds: {@code form} / {@code selected} (a {@link SaiyanForms#code}, 0 = Base), {@code flightTier}, {@code flying}, {@code senseTier}, {@code suppressed},
     * {@code goku} (the Ultimate is awakened), {@code charge} (Kamehameha, percent), {@code combo} (the part of Rock Scissors Paper reached, 0..3), {@code formMastery} and {@code strain} (percent), and the
     * Kamehameha and sensing cooldowns. The numbers are display only; the server decides.
     */
    public record Sync(boolean saiyan, float ki, float kiMax, float stamina, float staminaMax, double battlePower, int rank, boolean awakened, int blastCd, int barrageCd, int meditate,
                       int form, int selected, int flightTier, boolean flying, int senseTier, boolean suppressed, boolean goku, int charge, int combo, int formMastery, int strain, int kameCd, int senseCd)
            implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "saiyan_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeBoolean(p.saiyan());
            buf.writeFloat(p.ki());
            buf.writeFloat(p.kiMax());
            buf.writeFloat(p.stamina());
            buf.writeFloat(p.staminaMax());
            buf.writeDouble(p.battlePower());
            buf.writeByte(p.rank());
            buf.writeBoolean(p.awakened());
            buf.writeVarInt(p.blastCd());
            buf.writeVarInt(p.barrageCd());
            buf.writeByte(p.meditate());
            buf.writeByte(p.form());
            buf.writeByte(p.selected());
            buf.writeByte(p.flightTier());
            buf.writeBoolean(p.flying());
            buf.writeByte(p.senseTier());
            buf.writeBoolean(p.suppressed());
            buf.writeBoolean(p.goku());
            buf.writeByte(p.charge());
            buf.writeByte(p.combo());
            buf.writeByte(p.formMastery());
            buf.writeByte(p.strain());
            buf.writeVarInt(p.kameCd());
            buf.writeVarInt(p.senseCd());
        }, buf -> new Sync(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readDouble(), buf.readByte(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                buf.readByte(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readByte(), buf.readBoolean(), buf.readBoolean(), buf.readByte(), buf.readByte(), buf.readByte(),
                buf.readByte(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** One felt presence: where it is (an eighth of a block each unit) relative to the sensing Saiyan, how strong (0..4), and what (0 creature, 1 player, 2 boss). */
    public record Entry(short dx, short dz, byte dy, byte signature, byte kind) {}

    /** The radar: how many seconds it stays on, the radius it covers, and the presences (at most {@link SaiyanSenseRules#MAX_ENTRIES}). */
    public record Ping(int seconds, int radius, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Ping> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "saiyan_ping"));
        public static final StreamCodec<FriendlyByteBuf, Ping> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeByte(p.seconds());
            buf.writeShort(p.radius());
            int n = Math.min(SaiyanSenseRules.MAX_ENTRIES, p.entries().size());
            buf.writeByte(n);
            for (int i = 0; i < n; i++) {
                Entry e = p.entries().get(i);
                buf.writeShort(e.dx());
                buf.writeShort(e.dz());
                buf.writeByte(e.dy());
                buf.writeByte(e.signature());
                buf.writeByte(e.kind());
            }
        }, buf -> {
            int seconds = Math.max(0, Math.min(60, buf.readByte()));
            int radius = Math.max(1, Math.min(256, buf.readShort()));
            int n = Math.max(0, Math.min(SaiyanSenseRules.MAX_ENTRIES, buf.readByte()));
            List<Entry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new Entry(buf.readShort(), buf.readShort(), buf.readByte(), (byte) Math.max(0, Math.min(4, buf.readByte())), (byte) Math.max(0, Math.min(2, buf.readByte()))));
            return new Ping(seconds, radius, list);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** True for an ask the server will look at; everything else is dropped without a word. */
    static boolean valid(int op) { return op >= OP_BLAST && op <= OP_ENERGY; }

    static void onAction(ServerPlayer p, int op) {
        if (!valid(op)) return;
        SaiyanManager.onAction(p, op);
    }

    public static void send(ServerPlayer p, Sync s) { if (p.connection != null) PacketDistributor.sendToPlayer(p, s); }

    public static void send(ServerPlayer p, Ping s) { if (p.connection != null) PacketDistributor.sendToPlayer(p, s); }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Action.TYPE, Action.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onAction(sp, payload.op()); }));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Sync> c = clientSync; if (c != null) c.accept(payload); }));
        r.playToClient(Ping.TYPE, Ping.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Ping> c = clientPing; if (c != null) c.accept(payload); }));
    }
}
