package com.newuniverse.nusmp.soul;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the halo and wings are drawn when the creature carries the {@code nusmp:paladin} effect, which vanilla syncs to trackers. A
 * second, tiny announcement runs beside it as a safety net: once a second the server tells everyone tracking a Paladin "this entity
 * is one for the next 45 ticks" (the same pattern as {@code RougeCat}), so the look cannot depend on one sync path alone, and a
 * Paladin whose effect was cleansed away still shows until the next sweep puts it back. The client keeps only entity ids and an end
 * time in a small map that is emptied on log-out.
 */
public final class PaladinNet {
    private PaladinNet() {}

    /** Client side: entity id -> client game time the mark lasts until. */
    public static final Map<Integer, Long> UNTIL = new HashMap<>();

    public record Mark(int entity, int ticks) implements CustomPacketPayload {
        public static final Type<Mark> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "paladin_mark"));
        public static final StreamCodec<FriendlyByteBuf, Mark> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.entity()); buf.writeVarInt(p.ticks()); }, buf -> new Mark(buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Mark.TYPE, Mark.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() != null) UNTIL.put(p.entity(), ctx.player().level().getGameTime() + p.ticks());
        }));
    }

    /** Announces one Paladin to everyone who tracks it (server side). */
    public static void announce(Entity paladin) {
        PacketDistributor.sendToPlayersTrackingEntity(paladin, new Mark(paladin.getId(), 45));
    }

    /** Tells everyone tracking a creature that it is no longer a Paladin (the client drops the mark at once instead of letting it run out). */
    public static void withdraw(Entity creature) {
        PacketDistributor.sendToPlayersTrackingEntity(creature, new Mark(creature.getId(), 0));
    }

    /** Client: is this entity id marked right now? */
    public static boolean marked(int entity, long now) {
        Long until = UNTIL.get(entity);
        if (until == null) return false;
        if (now >= until) { UNTIL.remove(entity); return false; }
        return true;
    }

    /** Client: forget everything (log-out, world change). */
    public static void clear() { UNTIL.clear(); }
}
