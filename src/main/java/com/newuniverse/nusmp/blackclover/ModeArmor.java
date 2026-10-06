package com.newuniverse.nusmp.blackclover;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Transformation armour overlays (0.38): temporary, state-driven armour that appears on a mage while a high-tier mode is active.
 * It never uses an armour slot: the server keeps the mode and its end time in the player's persistent data, and
 * {@link ModePayload} tells the player and everyone tracking them, so the client layer ({@code client.mode.ModeArmorLayer})
 * swaps it on the instant the spell is cast and drops it when the mode ends.
 *
 * <p>Triggers: Spirit Dive on the Wind book or Spirit of Zephyr ({@link Mode#WIND_SPIRIT_DIVE}), Spirit Dive on the Fire book
 * ({@link Mode#FIRE_SPIRIT_DIVE}), Black Asta or the Anti-Magic Spirit Lord's Black Form ({@link Mode#DEMON}), Thunder Fiend
 * and Black Lightning Battle Fiend ({@link Mode#LIGHTNING_GOD}), Valkyrie Dress ({@link Mode#VALKYRIE}).
 */
public final class ModeArmor {
    /** Append only: the ordinal goes over the network. */
    public enum Mode { NONE, WIND_SPIRIT_DIVE, FIRE_SPIRIT_DIVE, DEMON, LIGHTNING_GOD, VALKYRIE }

    private static final String KEY = "nusmp_mode_armor", UNTIL = "nusmp_mode_armor_until";

    private ModeArmor() {}

    /** Put {@code mode} on for {@code ticks} (replaces any other mode). */
    public static void start(ServerPlayer p, Mode mode, int ticks) {
        long until = p.level().getGameTime() + ticks;
        p.getPersistentData().putInt(KEY, mode.ordinal());
        p.getPersistentData().putLong(UNTIL, until);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new ModePayload(p.getId(), mode.ordinal(), ticks));
    }

    /** End {@code mode} now, if it is the one showing (null = whatever is showing). */
    public static void stop(ServerPlayer p, Mode mode) {
        Mode now = active(p);
        if (now == Mode.NONE || (mode != null && now != mode)) return;
        p.getPersistentData().putLong(UNTIL, 0);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new ModePayload(p.getId(), Mode.NONE.ordinal(), 0));
    }

    public static Mode active(ServerPlayer p) {
        if (p.level().getGameTime() >= p.getPersistentData().getLong(UNTIL)) return Mode.NONE;
        int o = p.getPersistentData().getInt(KEY);
        return o > 0 && o < Mode.values().length ? Mode.values()[o] : Mode.NONE;
    }

    // ---------------------------------------------------------------- events
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer watcher) || !(e.getTarget() instanceof ServerPlayer target)) return;
        Mode m = active(target);
        if (m == Mode.NONE) return;
        int left = (int) (target.getPersistentData().getLong(UNTIL) - target.level().getGameTime());
        PacketDistributor.sendToPlayer(watcher, new ModePayload(target.getId(), m.ordinal(), left));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Mode m = active(p);
        if (m != Mode.NONE) PacketDistributor.sendToPlayer(p, new ModePayload(p.getId(), m.ordinal(),
                (int) (p.getPersistentData().getLong(UNTIL) - p.level().getGameTime())));
    }

    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) stop(p, null);
    }

    // ---------------------------------------------------------------- network
    /** Player {@code entityId} wears {@code mode} (ordinal; 0 = none) for {@code ticks} more ticks. */
    public record ModePayload(int entityId, int mode, int ticks) implements CustomPacketPayload {
        public static final Type<ModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "mode_armor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ModePayload::entityId,
                ByteBufCodecs.VAR_INT, ModePayload::mode,
                ByteBufCodecs.VAR_INT, ModePayload::ticks,
                ModePayload::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The handler only runs on clients, so the client class is never loaded on a server. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(ModePayload.TYPE, ModePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.mode.ModeArmorClient.receive(payload.entityId(), payload.mode(), payload.ticks())));
    }
}
