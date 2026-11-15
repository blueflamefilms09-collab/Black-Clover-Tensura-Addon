package com.newuniverse.nusmp.greed;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/**
 * Why: the Black Sword talks over two tiny packets and nothing else. One goes UP and is only an ask: switch to a form (the form key). The server decides everything: the sender must be a real player and the wielder, the form
 * must be open, and a switch is rate limited ({@link GreedRules#SWITCH_GAP_TICKS}). One goes DOWN, only to the player it is about: the state for the vignette and the HUD (satiation, tier, form, flags, vault size), sent only when
 * it changes (at most once a second). The client hook is {@link #clientHud} (set by the client class), so this common file names no client class. Both directions are optional.
 */
public final class GreedNet {
    private GreedNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Hud> clientHud;

    /** The form key: {@code form} 0..7 switches to it; {@code -1} the next open form, {@code -2} the previous. */
    public record SwitchForm(int form) implements CustomPacketPayload {
        public static final Type<SwitchForm> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "greed_switch"));
        public static final StreamCodec<FriendlyByteBuf, SwitchForm> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.form() + 2), buf -> new SwitchForm(buf.readUnsignedByte() - 2));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The state for the HUD: satiation 0..100, tier 0..7, form 0..7, flags (1 wielder, 2 starving, 4 half-starvation), the vault size. */
    public record Hud(int satiation, int tier, int form, int flags, int vault) implements CustomPacketPayload {
        public static final int F_OWNER = 1, F_STARVING = 2, F_HALF = 4;
        public static final Type<Hud> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "greed_hud"));
        public static final StreamCodec<FriendlyByteBuf, Hud> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeByte(p.satiation());
            buf.writeByte(p.tier());
            buf.writeByte(p.form());
            buf.writeByte(p.flags());
            buf.writeByte(p.vault());
        }, buf -> new Hud(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void onSwitch(ServerPlayer p, int form) {
        if (form < -2 || form >= GreedRules.FORMS) return;                    // not a value the client could send
        if (form == -1) GreedService.cycleForm(p, false);
        else if (form == -2) GreedService.cycleForm(p, true);
        else GreedService.switchForm(p, form);
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
        r.playToServer(SwitchForm.TYPE, SwitchForm.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onSwitch(sp, payload.form()); }));
        r.playToClient(Hud.TYPE, Hud.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Hud> c = clientHud; if (c != null) c.accept(payload); }));
    }
}
