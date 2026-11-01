package com.newuniverse.nusmp.devil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: the Devil Contract system talks over three tiny packets (3.48.0) and the server decides everything.
 * <ul>
 * <li>{@link Toggle}, client to server, carries nothing: it is only an ask ("switch assimilation on or off"). The server checks that the sender is a real player with a contract, that the system is on, and
 * that the last ask was at least {@link DevilRules#MIN_ASK_GAP} ticks ago.</li>
 * <li>{@link Sync}, server to the owner only: the HUD and the eye shade (tier, attribute, level, flags, the sacrifice mask, and the seconds left of the union and of the cooldown, which the client counts down
 * itself). Sent only when something other than those counters changed, so at most once a second while the level moves.</li>
 * <li>{@link Visual}, server to the players who can see the owner: what the body looks like (entity id, tier, level in steps of five percent, union flag). Sent only when the tier, the step or the union changes,
 * and once to a player who starts seeing the owner. Level 0 tells the client to forget the player.</li>
 * </ul>
 * Both directions are optional: a client without the mod simply never gets them. The client side is hooked in by {@link #clientSync} and {@link #clientVisual} (set by the client class), so this common file names no client class.
 */
public final class DevilNet {
    private DevilNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Sync> clientSync;
    public static volatile Consumer<Visual> clientVisual;

    public static final int F_ASSIMILATING = 1, F_UNION = 2, F_COOLING = 4, F_BLOCKED = 8;

    // ---------------------------------------------------------------- client to server
    public record Toggle() implements CustomPacketPayload {
        public static final Type<Toggle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "devil_toggle"));
        public static final StreamCodec<FriendlyByteBuf, Toggle> STREAM_CODEC = StreamCodec.of((buf, p) -> {}, buf -> new Toggle());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- server to client
    /** The owner's state. {@code unionSec} and {@code coolSec} are whole seconds at the moment of sending. */
    public record Sync(int tier, int attribute, int level, int flags, int sacMask, int unionSec, int coolSec) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "devil_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeByte(p.tier());
            buf.writeByte(p.attribute());
            buf.writeByte(p.level());
            buf.writeByte(p.flags());
            buf.writeByte(p.sacMask());
            buf.writeVarInt(p.unionSec());
            buf.writeVarInt(p.coolSec());
        }, buf -> new Sync(DevilRules.clampInt(buf.readByte(), 0, DevilTier.values().length - 1), DevilRules.clampInt(buf.readByte(), 0, DevilAttribute.values().length - 1),
                DevilRules.clampInt(buf.readByte(), 0, 100), buf.readByte() & 0xF, buf.readByte() & 0x7, Math.max(0, buf.readVarInt()), Math.max(0, buf.readVarInt())));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What one player looks like to those who can see them: {@code level} 0 to 100 (already rounded to a step), {@code union} the Devil Union. */
    public record Visual(int entity, int tier, int level, boolean union) implements CustomPacketPayload {
        public static final Type<Visual> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "devil_visual"));
        public static final StreamCodec<FriendlyByteBuf, Visual> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.entity());
            buf.writeByte(p.tier());
            buf.writeByte(p.level());
            buf.writeBoolean(p.union());
        }, buf -> new Visual(buf.readVarInt(), DevilRules.clampInt(buf.readByte(), 0, DevilTier.values().length - 1), DevilRules.clampInt(buf.readByte(), 0, 100), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- registration
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Toggle.TYPE, Toggle.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) DevilContracts.onToggleAsk(sp);
        }));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            Consumer<Sync> c = clientSync;
            if (c != null) c.accept(p);
        }));
        r.playToClient(Visual.TYPE, Visual.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            Consumer<Visual> c = clientVisual;
            if (c != null) c.accept(p);
        }));
    }

    // ---------------------------------------------------------------- sending (server side)
    static void sendSelf(ServerPlayer p, Sync s) { PacketDistributor.sendToPlayer(p, s); }

    static void sendVisual(ServerPlayer p, Visual v) { PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, v); }

    static void sendVisualTo(ServerPlayer viewer, Visual v) { PacketDistributor.sendToPlayer(viewer, v); }
}
