package com.newuniverse.nusmp.dungeon.floor.charybdis;

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
 * Opens the three airlocks of the Charybdis Basin to the caves outside, once the basin stands and the caves round it are loaded (the way
 * {@code MonarchGates} opens the Monarch's Vault): the passage is cut through the sealed face of the airlock to the cell where an open iron door is
 * hung (a door's cell cannot hold water, so the airlock stays dry however flooded the cave), and on through the rock outside by the cheapest route
 * {@link CharybdisWays} finds to a cave. The basin's own stone is the mod's building, so the cutting runs inside {@code DungeonGuard.internal}.
 * Whether an airlock is open is read from the world (is the iron door there?), so nothing is saved, and an airlock whose door is gone (the basin was
 * rebuilt) is simply opened again.
 */
final class CharybdisGates {
    private CharybdisGates() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** A basin with an airlock still shut is looked at again, near a player, this often (ticks): the caves round it may only now be loaded. */
    private static final long EVERY = 1200L;
    /** When each basin was last looked at (game ticks; not saved: a restart looks again). */
    private static final Map<Long, Long> TRIED = new HashMap<>();

    static void clear() { TRIED.clear(); }

    /** Is the airlock open: is its outer iron door hung? False if the chunk of the door is not loaded (this never loads it). */
    static boolean open(ServerLevel level, ZonePlan plan, CharybdisBasinPlan.Hall hall) {
        if (level.getChunkSource().getChunkNow(hall.doorX() >> 4, hall.doorZ() >> 4) == null) return false;
        return level.getBlockState(new BlockPos(hall.doorX(), plan.floorY + 1, hall.doorZ())).is(Blocks.IRON_DOOR);
    }

    /** Are the chunks of the airlock's sealed face and of its door both loaded? */
    private static boolean loaded(ServerLevel level, CharybdisBasinPlan.Hall hall) {
        return level.getChunkSource().getChunkNow(hall.firstX() >> 4, hall.firstZ() >> 4) != null
                && level.getChunkSource().getChunkNow(hall.doorX() >> 4, hall.doorZ() >> 4) != null;
    }

    /** Looks at the shut airlocks of this basin now and then: at most once in {@link #EVERY} ticks. */
    static void retry(ServerLevel level, ZonePlan plan) {
        long key = NexusCells.key(plan.cellX, plan.cellZ);
        long now = level.getGameTime();
        Long last = TRIED.get(key);
        if (last != null && now >= last && now - last < EVERY) return;
        TRIED.put(key, now);
        openAll(level, plan);
    }

    /** Tries every shut airlock of the basin. Returns how many were opened this time. */
    static int openAll(ServerLevel level, ZonePlan plan) {
        int opened = 0;
        for (CharybdisBasinPlan.Hall hall : CharybdisBasinPlan.halls(plan.cellX, plan.cellZ)) {
            try {
                if (!open(level, plan, hall) && openOne(level, plan, hall)) opened++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] An airlock of the Charybdis Basin of cell ({}, {}) could not be opened", plan.cellX, plan.cellZ, failure);
            }
        }
        return opened;
    }

    private static boolean openOne(ServerLevel level, ZonePlan plan, CharybdisBasinPlan.Hall hall) {
        if (!loaded(level, hall)) return false;
        final int y1 = plan.floorY + 1;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        CharybdisWays.Route route = CharybdisWays.route(hall.doorX(), hall.doorZ(), hall.dx(), hall.dz(), y1, (x, y, z) -> {
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return CharybdisWays.UNKNOWN;
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1 || plan.footprint.contains(x, z)) return CharybdisWays.BLOCKED;
            int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
            if ((cx != plan.cellX || cz != plan.cellZ) && NexusCells.isNexusCell(cx, cz)) return CharybdisWays.BLOCKED;       // another Nexus: not to be cut into
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.is(Blocks.BEDROCK) || s.getFluidState().is(FluidTags.LAVA)) return CharybdisWays.BLOCKED;
            return s.isAir() || s.is(Blocks.WATER) ? CharybdisWays.OPEN : CharybdisWays.SOLID;
        });
        if (route == null) return false;
        DungeonGuard.internal(() -> cut(level, hall, route, y1));
        return true;
    }

    /** Cuts the passage: the airlock's sealed face, the door, and the rock to the cave. */
    private static void cut(ServerLevel level, CharybdisBasinPlan.Hall hall, CharybdisWays.Route route, int y1) {
        final int flags = NexusSink.FLAGS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int inside = CharybdisBasinPlan.GATE_DOOR - CharybdisBasinPlan.GATE_FIRST;
        for (int i = 0; i < inside; i++) {                                                // the sealed face, up to the door
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
