package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
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
 * The world side of the ways into the Underworld Gate: opens the four sealed gates of its shell to the caves round it (see
 * {@link UnderworldWays}) once the hall stands, and looks again, now and then, at a gate that is still shut (the caves round it may not have
 * been loaded the first time). A way is a passage a block wide and two high through the seal, the shell and the rock beyond to a cave,
 * ending in an open iron door in the outermost cell of the shell: a door's cell holds no water, so the flooded caves stay out and a swimmer
 * can still come in. A gate with a door in its door cell is open; nothing else is saved. Every write is inside {@link DungeonGuard#internal}.
 */
final class UnderworldGates {
    private UnderworldGates() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Game ticks between two looks at a gate that is still shut. */
    private static final long RETRY = 1200L;
    private static final Map<Long, Long> TRIED = new HashMap<>();

    /** Is a gate already open: an iron door stands in its door cell? */
    static boolean isOpen(ServerLevel level, ZonePlan plan, UnderworldGatePlan.Gate g) {
        return level.getBlockState(new BlockPos(g.doorX(), plan.floorY + 1, g.doorZ())).is(Blocks.IRON_DOOR);
    }

    /** Tries every gate that is shut; returns how many were opened. */
    static int openAll(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        int opened = 0;
        List<UnderworldGatePlan.Gate> gates = UnderworldGatePlan.gates(cellX, cellZ);
        for (UnderworldGatePlan.Gate g : gates) {
            try {
                if (!isOpen(level, plan, g) && open(level, plan, cellX, cellZ, g)) opened++;
            } catch (RuntimeException failure) {
                LOG.warn("[nusmp] A gate of the Underworld Gate of cell ({}, {}) could not be opened", cellX, cellZ, failure);
            }
        }
        return opened;
    }

    /** A hall whose gates are not all open is looked at again when players are near, at most every minute. */
    static void retry(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        long key = NexusCells.key(cellX, cellZ), now = level.getGameTime();
        Long last = TRIED.get(key);
        if (last != null && now >= last && now - last < RETRY) return;
        for (int[] c : plan.chunks()) if (level.getChunkSource().getChunkNow(c[0], c[1]) == null) return;       // reading a block of an unloaded chunk would load it
        TRIED.put(key, now);
        boolean shut = false;
        for (UnderworldGatePlan.Gate g : UnderworldGatePlan.gates(cellX, cellZ)) if (!isOpen(level, plan, g)) shut = true;
        if (shut) openAll(level, plan, cellX, cellZ);
    }

    /** Forgets what was tried (the world is closing, or the hall was reset). */
    static void forget() { TRIED.clear(); }

    /** The passage and the door of one gate. False if no cave was found within reach, or the chunks round it are not loaded. */
    private static boolean open(ServerLevel level, ZonePlan plan, int cellX, int cellZ, UnderworldGatePlan.Gate g) {
        final int y1 = plan.floorY + 1, y2 = plan.floorY + 2;
        final int x0 = cellX * NexusZone.CELL, z0 = cellZ * NexusZone.CELL;
        final long outer2 = (long) UnderworldGatePlan.OUTER * UnderworldGatePlan.OUTER;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        UnderworldWays.Probe probe = (x, y, z) -> {
            if (x < x0 || x >= x0 + NexusZone.CELL || z < z0 || z >= z0 + NexusZone.CELL) return UnderworldWays.BLOCKED;       // never into another core cell
            long dx = x - plan.centreX(), dz = z - plan.centreZ();
            if (dx * dx + dz * dz <= outer2) return UnderworldWays.BLOCKED;                                                    // never back into the shell
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return UnderworldWays.UNKNOWN;
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.getFluidState().is(FluidTags.LAVA)) return UnderworldWays.BLOCKED;
            return s.isAir() || s.is(Blocks.WATER) ? UnderworldWays.OPEN : UnderworldWays.SOLID;
        };
        UnderworldWays.Route route = UnderworldWays.route(g.doorX(), g.doorZ(), g.dx(), g.dz(), y1, probe);
        if (route == null) return false;
        Direction facing = g.dx() > 0 ? Direction.EAST : g.dx() < 0 ? Direction.WEST : g.dz() > 0 ? Direction.SOUTH : Direction.NORTH;
        DungeonGuard.internal(() -> {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int y = y1; y <= y2; y++) {                                                 // the seal of the porch, cut through
                p.set(g.firstX(), y, g.firstZ());
                if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2 | 16);
            }
            for (int[] c : route.cells()) {                                                  // the rock beyond the door; the water of a cave is left alone
                for (int y = y1; y <= y2; y++) {
                    p.set(c[0], y, c[1]);
                    BlockState cur = level.getBlockState(p);
                    if (cur.isAir() || cur.is(Blocks.WATER)) continue;
                    level.setBlock(p, air, 2 | 16);
                }
            }
            level.setBlock(p.set(g.doorX(), y1, g.doorZ()), doorState(facing, false), 2 | 16);       // the door goes in last
            level.setBlock(p.set(g.doorX(), y2, g.doorZ()), doorState(facing, true), 2 | 16);
        });
        return true;
    }

    /** One half of an iron door, standing open along the passage: it keeps the water out of its cell and lets a swimmer through. */
    static BlockState doorState(Direction facing, boolean upper) {
        return Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }
}
