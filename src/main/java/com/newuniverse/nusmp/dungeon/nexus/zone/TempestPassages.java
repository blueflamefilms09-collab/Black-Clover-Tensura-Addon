package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestMiragePlan.Gateway;
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
 * Opens the sealed gates of the Tempest Mirage to the caves outside, once the village stands and the caves round it are loaded (the way
 * dungeon/city/CityBuilder opens the Golden City): the passage is cut through the plug in the shell from the gate's first cell to the cell
 * where an iron door is hung open (a door's cell holds no water), and on through the rock outside by the cheapest route {@link TempestWays}
 * finds to a cave. The village's own stone is the mod's own building, so the cutting runs inside {@code DungeonGuard.internal}. Whether a
 * gate is open is read from the world (is the iron door there?), so nothing is saved, and a gate whose door is gone (the village was rebuilt)
 * is simply opened again.
 */
final class TempestPassages {
    private TempestPassages() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** A village with a gate still shut is looked at again, near a player, this often (ticks): the caves round it may only now be loaded. */
    private static final long EVERY = 1200L;
    /** When each village was last looked at (game ticks; not saved: a restart looks again). */
    private static final Map<Long, Long> TRIED = new HashMap<>();

    /** Forgets when each village was looked at (the world is closing). */
    static void clear() { TRIED.clear(); }

    /** Is the gate open: is its iron door hung? False if the chunk of the door is not loaded (this never loads it). */
    static boolean open(ServerLevel level, ZonePlan plan, Gateway g) {
        if (level.getChunkSource().getChunkNow(g.doorX() >> 4, g.doorZ() >> 4) == null) return false;
        return level.getBlockState(new BlockPos(g.doorX(), plan.floorY + 1, g.doorZ())).is(Blocks.IRON_DOOR);
    }

    /** Are the chunks of the gate's first cell and its door both loaded? */
    private static boolean loaded(ServerLevel level, Gateway g) {
        return level.getChunkSource().getChunkNow(g.firstX() >> 4, g.firstZ() >> 4) != null
                && level.getChunkSource().getChunkNow(g.doorX() >> 4, g.doorZ() >> 4) != null;
    }

    /** Looks at the shut gates of this village now and then: at most once in {@link #EVERY} ticks. */
    static void retry(ServerLevel level, TempestMiragePlan.Layout layout) {
        ZonePlan plan = layout.plan();
        long key = (((long) plan.cellX) << 32) ^ (plan.cellZ & 0xFFFFFFFFL);
        long now = level.getGameTime();
        Long last = TRIED.get(key);
        if (last != null && now >= last && now - last < EVERY) return;
        TRIED.put(key, now);
        openAll(level, layout);
    }

    /** Tries every shut gate of the village. Returns how many were opened this time. */
    static int openAll(ServerLevel level, TempestMiragePlan.Layout layout) {
        ZonePlan plan = layout.plan();
        int opened = 0;
        for (Gateway g : layout.gateways()) {
            try {
                if (!open(level, plan, g) && openOne(level, plan, g)) opened++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A gate of the Tempest Mirage of cell ({}, {}) could not be opened", plan.cellX, plan.cellZ, failure);
            }
        }
        return opened;
    }

    private static boolean openOne(ServerLevel level, ZonePlan plan, Gateway g) {
        if (!loaded(level, g)) return false;
        final int y1 = plan.floorY + 1;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        TempestWays.Route route = TempestWays.route(g.doorX(), g.doorZ(), g.dx(), g.dz(), y1, (x, y, z) -> {
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return TempestWays.UNKNOWN;
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1 || plan.footprint.contains(x, z)) return TempestWays.BLOCKED;
            int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
            if ((cx != plan.cellX || cz != plan.cellZ) && DungeonTier.isCityCell(cx, cz)) return TempestWays.BLOCKED;            // another Nexus: not to be cut into
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.is(Blocks.BEDROCK)) return TempestWays.BLOCKED;
            return s.isAir() || s.is(Blocks.WATER) ? TempestWays.OPEN : TempestWays.SOLID;
        });
        if (route == null) return false;
        DungeonGuard.internal(() -> cut(level, g, route, y1));
        return true;
    }

    /** Cuts the passage: the plug of the gate, the door, and the rock to the cave. */
    private static void cut(ServerLevel level, Gateway g, TempestWays.Route route, int y1) {
        final int flags = NexusSink.FLAGS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int inside = TempestMiragePlan.GATE_DOOR - TempestMiragePlan.GATE_FIRST;
        for (int i = 0; i < inside; i++) {                                                // the plug of the gate and the shell, up to the door
            for (int y = y1; y <= y1 + 1; y++) level.setBlock(pos.set(g.firstX() + g.dx() * i, y, g.firstZ() + g.dz() * i), air, flags);
        }
        Direction facing = g.dx() > 0 ? Direction.EAST : g.dx() < 0 ? Direction.WEST : g.dz() > 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlock(pos.set(g.doorX(), y1, g.doorZ()), door(facing, false), flags);
        level.setBlock(pos.set(g.doorX(), y1 + 1, g.doorZ()), door(facing, true), flags);
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
