package com.newuniverse.nusmp.vfx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-side entry point: build a payload and send it to nearby players. Also registers the packet. */
public final class VfxSpawn {
    public static final double SEND_RANGE = 96.0;

    private VfxSpawn() {}

    /** Mod-bus listener: registers the VFX packet (client-bound only). */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(VfxPayload.TYPE, VfxPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.vfx.client.VfxManager.get().spawn(payload)));
    }

    /** Fire-and-forget effect between two points. */
    public static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int color, int duration, float power) {
        send(level, new VfxPayload(shape.ordinal(), from, to, color, duration, power, -1, level.random.nextLong()));
    }

    /** Effect that follows an entity (e.g. a magic circle under the caster's feet). */
    public static void sendFollowing(ServerLevel level, VfxShape shape, Entity follow, Vec3 to, int color, int duration, float power) {
        send(level, new VfxPayload(shape.ordinal(), follow.position(), to, color, duration, power, follow.getId(), level.random.nextLong()));
    }

    public static void send(ServerLevel level, VfxPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, payload.from().x, payload.from().y, payload.from().z, SEND_RANGE, payload);
    }
}
