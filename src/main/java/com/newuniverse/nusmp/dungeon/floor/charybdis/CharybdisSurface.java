package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How the leviathan comes up: when a player (not in creative, not a spectator) comes within {@link CharybdisTable#SURFACE_RANGE} blocks of the dais
 * of a standing basin whose central relic is still on its pedestal and which has no Charybdis Spawn, the water shudders, a column of bubbles climbs
 * from the well for three seconds, and then the boss appears at the bottom of the well and rises six blocks over three more, unhurt and with no target
 * (see {@link CharybdisSpawnBoss#beginRise}). One surfacing at a time per basin; nothing is saved: after a restart the basin simply looks again.
 */
final class CharybdisSurface {
    private CharybdisSurface() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The basins whose leviathan is on its way up, by cell key, with the game tick it began; a stale entry (the server was busy) is forgotten after a while. */
    private static final Map<Long, Long> SURFACING = new HashMap<>();
    private static final int GRACE = 100;

    static void clear() { SURFACING.clear(); }

    /** The volume of the basin where its boss may be found. */
    static AABB volume(ZonePlan plan) {
        return new AABB(plan.footprint.minX(), plan.floorY - 6, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
    }

    /** Is there a living Charybdis Spawn in the basin? */
    static boolean exists(ServerLevel level, ZonePlan plan) {
        return !level.getEntitiesOfClass(CharybdisSpawnBoss.class, volume(plan), LivingEntity::isAlive).isEmpty();
    }

    /** Called about once a second with the players inside the basin: starts the surfacing when the conditions hold. */
    static void tick(ServerLevel level, ZonePlan plan, BlockPos centre, List<ServerPlayer> inside) {
        long key = NexusCells.key(plan.cellX, plan.cellZ), now = level.getGameTime();
        Long since = SURFACING.get(key);
        if (since != null) {
            if (now >= since && now - since < CharybdisTable.BUBBLE_TICKS + GRACE) return;
            SURFACING.remove(key);
        }
        if (NexusZones.centreRelicTaken(level.getServer(), plan.cellX, plan.cellZ) || !level.isLoaded(centre)) return;
        ServerPlayer near = null;
        double r2 = CharybdisTable.SURFACE_RANGE * CharybdisTable.SURFACE_RANGE;
        for (ServerPlayer p : inside) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double dx = p.getX() - (centre.getX() + 0.5), dz = p.getZ() - (centre.getZ() + 0.5);
            if (dx * dx + dz * dz <= r2 && Math.abs(p.getY() - centre.getY()) <= CharybdisBasinPlan.HEIGHT) near = p;
        }
        if (near == null || exists(level, plan)) return;
        begin(level, plan, centre, key, inside);
    }

    private static void begin(ServerLevel level, ZonePlan plan, BlockPos centre, long key, List<ServerPlayer> inside) {
        SURFACING.put(key, level.getGameTime());
        for (ServerPlayer p : inside) if (!p.isSpectator()) p.displayClientMessage(Component.literal(CharybdisText.surfacing()).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), true);
        level.playSound(null, centre, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0f, 0.5f);
        SpellRuntime.zone(level, CharybdisTable.BUBBLE_TICKS, 2, CharybdisMoves.safe("surfacing", age -> bubbles(level, centre, age)));
        SpellRuntime.later(level, CharybdisTable.BUBBLE_TICKS, CharybdisMoves.safe("surfacing", () -> finish(level, plan, centre, key)));
    }

    /** The column of bubbles that climbs the well, thicker as the time comes. */
    private static void bubbles(ServerLevel level, BlockPos centre, int age) {
        double t = Math.min(1.0, age / (double) CharybdisTable.BUBBLE_TICKS), x = centre.getX() + 0.5, z = centre.getZ() + 0.5;
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, x, centre.getY() - 1.5 + 5 * t, z, 8 + (int) (16 * t), 1.6 * (1 - 0.4 * t), 1.2, 1.6 * (1 - 0.4 * t), 0.08);
        level.sendParticles(ParticleTypes.BUBBLE, x, centre.getY() + 0.5, z, 6, 4.0, 0.5, 4.0, 0.05);
        if (age % 20 == 0) level.playSound(null, centre, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0f, 0.5f + 0.3f * (float) t);
    }

    private static void finish(ServerLevel level, ZonePlan plan, BlockPos centre, long key) {
        SURFACING.remove(key);
        if (NexusZones.centreRelicTaken(level.getServer(), plan.cellX, plan.cellZ) || !level.isLoaded(centre) || exists(level, plan)) return;
        CharybdisSpawnBoss boss = spawn(level, centre, true);
        if (boss == null) {
            LOG.warn("[nusmp] The Charybdis Spawn of the basin in cell ({}, {}) could not be made", plan.cellX, plan.cellZ);
            return;
        }
        level.playSound(null, centre, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 4.0f, 0.5f);
    }

    /**
     * Makes a Charybdis Spawn for the basin whose floor block in the middle is 'centre'. Risen: it starts at the bottom of the well and climbs
     * (the surfacing); otherwise it is placed at its hover point at once (the admin command). Null if it could not be made.
     */
    static CharybdisSpawnBoss spawn(ServerLevel level, BlockPos centre, boolean rise) {
        CharybdisSpawnBoss boss = CharybdisEntities.CHARYBDIS_SPAWN.get().create(level);
        if (boss == null) return null;
        int hover = centre.getY() + CharybdisBasinPlan.HOVER;
        double y = rise ? centre.getY() - CharybdisBasinPlan.DEPTH_WELL + 1 : hover;
        boss.moveTo(centre.getX() + 0.5, y, centre.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        boss.setArena(new BlockPos(centre.getX(), hover, centre.getZ()));
        if (rise) boss.beginRise(CharybdisTable.RISE_TICKS);
        level.addFreshEntity(boss);
        return boss;
    }
}
