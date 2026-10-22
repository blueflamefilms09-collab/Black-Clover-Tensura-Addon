package com.newuniverse.nusmp.shishou;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: Shishou talks over three tiny packets and nothing else (2.1.0: the key packet is gone: his abilities are the Tensura Ultimate skill {@code nusmp:shishou}, whose presses travel in the skill system's own
 * packets). One goes UP and is only an ask: an Appraisal request (the id of the entity under the crosshair, at most one answered a second). The server decides everything: the sender must be a real player,
 * hold the sword, and every ask is rate limited. Two go DOWN, only to the wielder: the sword's state for the HUD (once a second while he is held, and when a cast changes it) and the answer
 * of an Appraisal. The client side is hooked in by {@link #clientSync} and {@link #clientAppraisal} (set by the client class), so this common file names no client class. Both directions are optional.
 */
public final class ShishouNet {
    private ShishouNet() {}

    public static final int NAME_MAX = 96;

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Sync> clientSync;
    public static volatile Consumer<AppraisalReply> clientAppraisal;

    // ---------------------------------------------------------------- client to server
    /** What is under the crosshair: an entity id, or -1 for the item in the off hand. */
    public record AppraiseRequest(int entityId) implements CustomPacketPayload {
        public static final Type<AppraiseRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_appraise"));
        public static final StreamCodec<FriendlyByteBuf, AppraiseRequest> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeVarInt(p.entityId() + 1), buf -> new AppraiseRequest(buf.readVarInt() - 1));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- server to client
    /** The sword's state for the HUD: level, mana, the selected skill's ordinal (-1 none) and its cooldown share, the window flags, the share of the way to the next level (all 0..100). */
    public record Sync(int level, int mana, int maxMana, int selected, int cooldownShare, int flags, int expShare) implements CustomPacketPayload {
        public static final int F_BARRIER = 1, F_SENSE = 2, F_STEALTH = 4, F_RELEASED = 8, F_LOW = 16;
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.level());
            buf.writeVarInt(p.mana());
            buf.writeVarInt(p.maxMana());
            buf.writeByte(p.selected() + 1);
            buf.writeByte(p.cooldownShare());
            buf.writeByte(p.flags());
            buf.writeByte(p.expShare());
        }, buf -> new Sync(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUnsignedByte() - 1, buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The Appraisal readout: kind 0 a mob, 1 an item, 2 nothing found. */
    public record AppraisalReply(int entityId, int kind, String nameKey, float health, float maxHealth, int levelish, int manaish, int weakMask, String skillKey, boolean concealed)
            implements CustomPacketPayload {
        public static final Type<AppraisalReply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_appraisal"));
        public static final StreamCodec<FriendlyByteBuf, AppraisalReply> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.entityId() + 1);
            buf.writeByte(p.kind());
            buf.writeUtf(p.nameKey(), NAME_MAX);
            buf.writeFloat(p.health());
            buf.writeFloat(p.maxHealth());
            buf.writeVarInt(p.levelish());
            buf.writeVarInt(p.manaish());
            buf.writeByte(p.weakMask());
            buf.writeUtf(p.skillKey(), NAME_MAX);
            buf.writeBoolean(p.concealed());
        }, buf -> new AppraisalReply(buf.readVarInt() - 1, buf.readUnsignedByte(), buf.readUtf(NAME_MAX), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readUnsignedByte(),
                buf.readUtf(NAME_MAX), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- the server's side of the asks
    static void onAppraise(ServerPlayer p, int entityId) {
        if (entityId < -1 || entityId > 100_000_000) return;                  // not an id the game could have handed out
        ShishouService.onAppraise(p, entityId);
    }

    /** Sends a state packet to the wielder only. */
    public static void send(ServerPlayer p, Sync s) { net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, s); }

    public static void send(ServerPlayer p, AppraisalReply r) { net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, r); }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(AppraiseRequest.TYPE, AppraiseRequest.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onAppraise(sp, payload.entityId()); }));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Sync> c = clientSync; if (c != null) c.accept(payload); }));
        r.playToClient(AppraisalReply.TYPE, AppraisalReply.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<AppraisalReply> c = clientAppraisal; if (c != null) c.accept(payload); }));
    }
}
