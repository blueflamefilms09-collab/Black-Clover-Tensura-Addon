package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Opens the sealed gates of the Golden City's garden to the caves outside, once the garden stands and the caves round it are loaded (the
 * way dungeon/city/CityBuilder opens the old city's ways): the passage is cut through the shell from the gate's last cell to the cell
 * where an iron door is hung open (a door's cell holds no water), and on through the rock outside by the cheapest route {@link GoldenWays}
 * finds to a cave. The garden's own stone is the mod's own building, so the cutting runs inside {@code DungeonGuard.internal}. Whether a
 * gate is open is read from the world (is the iron door there?), so nothing is saved, and a gate whose door is gone (the garden was
 * rebuilt) is simply opened again.
 */
final class GoldenGates {
    private GoldenGates() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** A garden with a gate still shut is looked at again, near a player, this often (ticks): the caves round it may only now be loaded. */
    private static final long EVERY = 1200L;
    /** When each garden was last looked at (game ticks; not saved: a restart looks again). */
    private static final Map<Long, Long> TRIED = new HashMap<>();

    /** The world column of a gate's cell `depth` blocks beyond its last cell of air (depth 0 is that cell; the door stands at depth {@link GoldenGrid#WALL}). */
    static int[] cell(GoldenGrid g, GoldenGrid.Gate gate, int depth) {
        return new int[]{g.cx + GoldenGrid.sideU(gate.side(), gate.tP() + depth, gate.w()), g.cz + GoldenGrid.sideV(gate.side(), gate.tP() + depth, gate.w())};
    }

    /** Is the gate open: is its iron door hung? False if the chunk of the door is not loaded (this never loads it). */
    static boolean open(ServerLevel level, GoldenGrid g, GoldenGrid.Gate gate) {
        int[] door = cell(g, gate, GoldenGrid.WALL);
        if (level.getChunkSource().getChunkNow(door[0] >> 4, door[1] >> 4) == null) return false;
        return level.getBlockState(new BlockPos(door[0], g.floorY + 1, door[1])).is(Blocks.IRON_DOOR);
    }

    /** Looks at the shut gates of this garden now and then: at most once in {@link #EVERY} ticks. */
    static void retry(ServerLevel level, GoldenGrid g) {
        long key = (((long) g.cellX) << 32) ^ (g.cellZ & 0xFFFFFFFFL);
        long now = level.getGameTime();
        Long last = TRIED.get(key);
        if (last != null && now >= last && now - last < EVERY) return;
        TRIED.put(key, now);
        openAll(level, g);
    }

    /** Tries every shut gate of the garden. Returns how many were opened this time. */
    static int openAll(ServerLevel level, GoldenGrid g) {
        int opened = 0;
        for (GoldenGrid.Gate gate : g.gates) {
            try {
                if (!open(level, g, gate) && openOne(level, g, gate)) opened++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A gate of the Golden City garden of cell ({}, {}) could not be opened", g.cellX, g.cellZ, failure);
            }
        }
        return opened;
    }

    private static boolean openOne(ServerLevel level, GoldenGrid g, GoldenGrid.Gate gate) {
        int[] first = cell(g, gate, 1), door = cell(g, gate, GoldenGrid.WALL);
        if (level.getChunkSource().getChunkNow(first[0] >> 4, first[1] >> 4) == null || level.getChunkSource().getChunkNow(door[0] >> 4, door[1] >> 4) == null) return false;
        final int y1 = g.floorY + 1;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        GoldenWays.Route route = GoldenWays.route(door[0], door[1], gate.dx(), gate.dz(), y1, (x, y, z) -> {
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return GoldenWays.UNKNOWN;
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1 || g.garden(x - g.cx, z - g.cz)) return GoldenWays.BLOCKED;
            int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
            if ((cx != g.cellX || cz != g.cellZ) && DungeonTier.isCityCell(cx, cz)) return GoldenWays.BLOCKED;            // another Nexus: not to be cut into
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.is(Blocks.BEDROCK)) return GoldenWays.BLOCKED;
            return s.isAir() || s.is(Blocks.WATER) ? GoldenWays.OPEN : GoldenWays.SOLID;
        });
        if (route == null) return false;
        DungeonGuard.internal(() -> cut(level, g, gate, route, y1));
        return true;
    }

    /** Cuts the passage: the cells of the shell up to the door, the door, and the rock to the cave. */
    private static void cut(ServerLevel level, GoldenGrid g, GoldenGrid.Gate gate, GoldenWays.Route route, int y1) {
        final int flags = NexusSink.FLAGS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int depth = 1; depth < GoldenGrid.WALL; depth++) {
            int[] c = cell(g, gate, depth);
            for (int y = y1; y <= y1 + 1; y++) level.setBlock(pos.set(c[0], y, c[1]), air, flags);
        }
        Direction facing = gate.dx() > 0 ? Direction.EAST : gate.dx() < 0 ? Direction.WEST : gate.dz() > 0 ? Direction.SOUTH : Direction.NORTH;
        int[] door = cell(g, gate, GoldenGrid.WALL);
        level.setBlock(pos.set(door[0], y1, door[1]), door(facing, false), flags);
        level.setBlock(pos.set(door[0], y1 + 1, door[1]), door(facing, true), flags);
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
