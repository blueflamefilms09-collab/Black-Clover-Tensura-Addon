package com.newuniverse.nusmp.saiyan;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: the Saiyan system talks over two tiny packets (3.63.0) and the server decides everything. UP: {@link Action}, one byte for what the player asked (a Ki Blast, a Rapid Ki Barrage); it is only an ask:
 * the sender must be a real Saiyan, the number is range-checked and asks are rate limited ({@link #MIN_GAP_TICKS}). DOWN, only to that player: {@link Sync}, the HUD state (Ki, stamina, Battle Power, rank,
 * cooldowns), at most four a second and only when something changed. The client side is hooked in by {@link #clientSync} (set by the client class), so this common file names no client class. Both
 * directions are optional: a client without the mod simply never gets them.
 */
public final class SaiyanNet {
    private SaiyanNet() {}

    public static final int OP_BLAST = 1, OP_BARRAGE = 2;
    public static final int MIN_GAP_TICKS = 4;

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Sync> clientSync;

    /** What the player asked: {@code op} (see OP_*). */
    public record Action(int op) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "saiyan_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.op()), buf -> new Action(buf.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * The HUD state. {@code saiyan} is whether the player is a Saiyan at all (false = hide the Saiyan HUD and show Tensura's own magicule bar again); {@code ki} and {@code kiMax} are the player's
     * Tensura magicule, shown as Ki; {@code stamina}/{@code staminaMax}; {@code battlePower}; {@code rank} (ordinal); {@code awakened}; the two cooldowns in ticks left; and {@code meditate}, the
     * awakening progress in percent. The numbers are display only; the server decides.
     */
    public record Sync(boolean saiyan, float ki, float kiMax, float stamina, float staminaMax, double battlePower, int rank, boolean awakened, int blastCd, int barrageCd, int meditate)
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
        }, buf -> new Sync(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readDouble(), buf.readByte(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** True for an ask the server will look at; everything else is dropped without a word. */
    static boolean valid(int op) { return op == OP_BLAST || op == OP_BARRAGE; }

    static void onAction(ServerPlayer p, int op) {
        if (!valid(op)) return;
        SaiyanManager.onAction(p, op);
    }

    public static void send(ServerPlayer p, Sync s) { if (p.connection != null) PacketDistributor.sendToPlayer(p, s); }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Action.TYPE, Action.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onAction(sp, payload.op()); }));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Sync> c = clientSync; if (c != null) c.accept(payload); }));
    }
}
