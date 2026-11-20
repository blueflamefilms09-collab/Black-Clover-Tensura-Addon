package com.newuniverse.nusmp.lords;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: the Lord weapons talk over two tiny packets and nothing else. One goes UP and is only an ask: switch form (the form key: next / previous). The server decides everything: the sender must be a real player holding a
 * Lord weapon they own, the form must be open, and a switch is rate limited ({@link LordRules#FORM_SWITCH_GAP_TICKS}). One goes DOWN, only to the player it is about: the mastery bar and the burden (or Greed's satiation)
 * of one sin (level, progress, meter, flags), sent only when it changed (at most once a second). The client hook {@link #clientHud} is set by the client class, so this common file names no client class.
 */
public final class LordNet {
    private LordNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Hud> clientHud;

    /** The form key: -1 the next open form, -2 the previous. */
    public record SwitchForm(int dir) implements CustomPacketPayload {
        public static final Type<SwitchForm> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "lords_switch"));
        public static final StreamCodec<FriendlyByteBuf, SwitchForm> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.dir() + 2), buf -> new SwitchForm(buf.readUnsignedByte() - 2));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * The state of one sin's mastery for its wielder: {@code sin} (ordinal), {@code level} 0..10, {@code pct} progress to the next level 0..100, {@code meter} the burden 0..100 (Greed: the satiation), {@code flags}
     * (1 wielder, 2 strained / ravenous, 4 overloaded / dormant, 8 half-starved) and {@code form} (the current form). With flags 0 the player is not the wielder and the client forgets the sin.
     */
    public record Hud(int sin, int level, int pct, int meter, int flags, int form) implements CustomPacketPayload {
        public static final int F_OWNER = 1, F_STRAINED = 2, F_OVERLOAD = 4, F_HALF = 8;
        public static final Type<Hud> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "lords_hud"));
        public static final StreamCodec<FriendlyByteBuf, Hud> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeByte(p.sin());
            buf.writeByte(p.level());
            buf.writeByte(p.pct());
            buf.writeByte(p.meter());
            buf.writeByte(p.flags());
            buf.writeByte(p.form());
        }, buf -> new Hud(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onSwitch(ServerPlayer p, int dir) {
        if (dir != -1 && dir != -2) return;                                    // not a value the client could send
        LordService.cycleForm(p, dir == -2);
    }

    public static void send(ServerPlayer p, Hud h) {
        if (p.connection == null) return;                                    // a stand-in of a self-test has no connection
        try {
            PacketDistributor.sendToPlayer(p, h);
        } catch (RuntimeException noConnection) {
            // a player who is leaving: nothing to tell
        }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(SwitchForm.TYPE, SwitchForm.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onSwitch(sp, payload.dir()); }));
        r.playToClient(Hud.TYPE, Hud.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Hud> c = clientHud; if (c != null) c.accept(payload); }));
    }
}
