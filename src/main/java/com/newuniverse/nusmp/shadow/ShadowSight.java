package com.newuniverse.nusmp.shadow;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: Unite Felis gives the wearer an outline on living things within 24 blocks, visible through walls, but only to the wearer. The glowing
 * effect would show it to everyone, so this sends the wearer alone a data packet that turns on the "glowing" bit of each nearby entity's shared
 * flags (the outline is drawn by the client from that bit); everyone else is never told. The bit is refreshed twice a second because the server's
 * own flag updates overwrite it, and it is switched off again (to the entity's real glowing state) when the thing leaves range or the mode ends.
 */
public final class ShadowSight {
    private ShadowSight() {}

    /** Shared flags are entity data id 0, a byte: bit 6 is "glowing". */
    private static final int DATA_ID_FLAGS = 0, GLOW_BIT = 0x40, MAX_OUTLINED = 48;
    private static final Map<UUID, Set<Integer>> LIT = new HashMap<>();

    /** Outlines what is within {@code range} blocks of the wearer; un-outlines what left. Call about every 10 ticks. */
    public static void update(ServerPlayer p, int range) {
        double r2 = (double) range * range;
        Set<Integer> now = new HashSet<>();
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                e -> e != p && e.isAlive() && !e.isSpectator() && e.distanceToSqr(p) <= r2);
        near.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
        for (LivingEntity e : near) {
            if (now.size() >= MAX_OUTLINED) break;
            now.add(e.getId());
            send(p, e, true);
        }
        Set<Integer> before = LIT.getOrDefault(p.getUUID(), Set.of());
        for (int id : before) {
            if (now.contains(id)) continue;
            Entity e = p.serverLevel().getEntity(id);
            if (e != null) send(p, e, false);
        }
        LIT.put(p.getUUID(), now);
    }

    /** Takes every outline off (the mode ended, the wearer left or died). */
    public static void clear(ServerPlayer p) {
        Set<Integer> before = LIT.remove(p.getUUID());
        if (before == null) return;
        for (int id : before) {
            Entity e = p.serverLevel().getEntity(id);
            if (e != null) send(p, e, false);
        }
    }

    /** Forgets a wearer without sending anything (logout: the client is gone). */
    public static void forget(UUID player) { LIT.remove(player); }

    private static void send(ServerPlayer to, Entity e, boolean glow) {
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(1);
        values.add(new SynchedEntityData.DataValue<>(DATA_ID_FLAGS, EntityDataSerializers.BYTE, flags(e, glow)));
        to.connection.send(new ClientboundSetEntityDataPacket(e.getId(), values));
    }

    /** The entity's shared-flags byte as the server has it, with the glowing bit forced on or left at its real state. */
    static byte flags(Entity e, boolean glow) {
        int f = 0;
        if (e.getRemainingFireTicks() > 0) f |= 0x01;
        if (e.isShiftKeyDown()) f |= 0x02;
        if (e.isSprinting()) f |= 0x08;
        if (e.isSwimming()) f |= 0x10;
        if (e.isInvisible()) f |= 0x20;
        if (glow || e.isCurrentlyGlowing()) f |= GLOW_BIT;
        if (e instanceof LivingEntity l && l.isFallFlying()) f |= 0x80;
        return (byte) f;
    }
}
