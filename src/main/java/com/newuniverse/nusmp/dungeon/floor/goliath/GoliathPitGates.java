package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Opens the sealed gates of the Goliath's Pit to the caves outside, once the pit stands and the caves round it are loaded (the way
 * {@code MonarchGates} opens the Monarch's Vault): the passage is cut through the wall from the gate's face to the cell where an iron door
 * is hung open (a door's cell holds no water), and on through the rock outside by the cheapest route {@link GoliathPitWays} finds to a cave.
 * The pit's own stone is the mod's own building, so the cutting runs inside {@code DungeonGuard.internal}. Whether a gate is open is read from
 * the world (is the iron door there?), so nothing is saved, and a gate whose door is gone (the pit was rebuilt) is simply opened again.
 * Another Nexus's cell (a core cell or an arena) is never cut into.
 */
final class GoliathPitGates {
    private GoliathPitGates() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** A pit with a gate still shut is looked at again, near a player, this often (ticks): the caves round it may only now be loaded. */
    private static final long EVERY = 1200L;
    /** When each pit was last looked at (game ticks; not saved: a restart looks again). */
    private static final Map<Long, Long> TRIED = new HashMap<>();

    /** The world is closing: nothing is remembered of when a pit was looked at. */
    static void forget() { TRIED.clear(); }

    /** Is the gate open: is its iron door hung? False if the chunk of the door is not loaded (this never loads it). */
    static boolean open(ServerLevel level, ZonePlan plan, GoliathPitPlan.Hall hall) {
        if (level.getChunkSource().getChunkNow(hall.doorX() >> 4, hall.doorZ() >> 4) == null) return false;
        return level.getBlockState(new BlockPos(hall.doorX(), plan.floorY + 1, hall.doorZ())).is(Blocks.IRON_DOOR);
    }

    /** Are the chunks of the gate's face, its wall and its door all loaded? */
    private static boolean loaded(ServerLevel level, GoliathPitPlan.Hall hall) {
        return level.getChunkSource().getChunkNow(hall.firstX() >> 4, hall.firstZ() >> 4) != null
                && level.getChunkSource().getChunkNow(hall.doorX() >> 4, hall.doorZ() >> 4) != null;
    }

    /** Looks at the shut gates of this pit now and then: at most once in {@link #EVERY} ticks. */
    static void retry(ServerLevel level, ZonePlan plan) {
        long key = (((long) plan.cellX) << 32) ^ (plan.cellZ & 0xFFFFFFFFL);
        long now = level.getGameTime();
        Long last = TRIED.get(key);
        if (last != null && now >= last && now - last < EVERY) return;
        TRIED.put(key, now);
        openAll(level, plan);
    }

    /** Tries every shut gate of the pit. Returns how many were opened this time. */
    static int openAll(ServerLevel level, ZonePlan plan) {
        int opened = 0;
        for (GoliathPitPlan.Hall hall : GoliathPitPlan.halls(plan.cellX, plan.cellZ)) {
            try {
                if (!open(level, plan, hall) && openOne(level, plan, hall)) opened++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A gate of the Goliath's Pit of cell ({}, {}) could not be opened", plan.cellX, plan.cellZ, failure);
            }
        }
        return opened;
    }

    private static boolean openOne(ServerLevel level, ZonePlan plan, GoliathPitPlan.Hall hall) {
        if (!loaded(level, hall)) return false;
        final int y1 = plan.floorY + 1;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        GoliathPitWays.Route route = GoliathPitWays.route(hall.doorX(), hall.doorZ(), hall.dx(), hall.dz(), y1, (x, y, z) -> {
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return GoliathPitWays.UNKNOWN;
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1 || plan.footprint.contains(x, z)) return GoliathPitWays.BLOCKED;
            int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
            if ((cx != plan.cellX || cz != plan.cellZ) && NexusCells.isNexusCell(cx, cz)) return GoliathPitWays.BLOCKED;            // another Nexus: not to be cut into
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.is(Blocks.BEDROCK) || s.getFluidState().is(FluidTags.LAVA)) return GoliathPitWays.BLOCKED;
            return s.isAir() || s.is(Blocks.WATER) ? GoliathPitWays.OPEN : GoliathPitWays.SOLID;
        });
        if (route == null) return false;
        DungeonGuard.internal(() -> cut(level, hall, route, y1));
        return true;
    }

    /** Cuts the passage: the gate's face and the wall behind it, the door, and the rock to the cave. */
    private static void cut(ServerLevel level, GoliathPitPlan.Hall hall, GoliathPitWays.Route route, int y1) {
        final int flags = NexusSink.FLAGS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int inside = GoliathPitPlan.GATE_DOOR - GoliathPitPlan.GATE_FIRST;
        for (int i = 0; i < inside; i++) {                                                // the face of the gate and the wall, up to the door
            for (int y = y1; y <= y1 + 1; y++) level.setBlock(pos.set(hall.firstX() + hall.dx() * i, y, hall.firstZ() + hall.dz() * i), air, flags);
        }
        Direction facing = hall.dx() > 0 ? Direction.EAST : hall.dx() < 0 ? Direction.WEST : hall.dz() > 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlock(pos.set(hall.doorX(), y1, hall.doorZ()), door(facing, false), flags);
        level.setBlock(pos.set(hall.doorX(), y1 + 1, hall.doorZ()), door(facing, true), flags);
        List<int[]> cells = route.cells();
        for (int[] c : cells) {                                                           // the rock outside; a cave's water is left where it is
            for (int y = y1; y <= y1 + 1; y++) {
                pos.set(c[0], y, c[1]);
                BlockState cur = level.getBlockState(pos);
                if (cur.isAir() || cur.is(Blocks.WATER)) continue;
                level.setBlock(pos, air, flags);
            }
        }
    }

    /** One half of an iron door standing open along the passage: it keeps the water out of its cell and lets a swimmer through. */
    private static BlockState door(Direction facing, boolean upper) {
        return Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }
}
