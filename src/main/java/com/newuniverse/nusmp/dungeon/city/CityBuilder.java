package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the Golden Cities where players come near them. Every 40 ticks it looks for a player of the Dungeon within 56 blocks of the centre
 * of a city cell that is not built yet; it then applies the city's plan ({@link CityPlan}) over several ticks, a few thousand blocks at a
 * time, only while every chunk of the district is loaded, and saves its progress ({@link CitySave}) so a restart carries on. When the last
 * op is placed it opens the ways in ({@link CityWays}) and stands the plaza's guards, creatures of the base Tensura mod ({@link Wildlife}).
 * It writes only into the Dungeon's own level.
 */
public final class CityBuilder {
    private CityBuilder() {}

    /** Send the change to clients, but do not update shapes or neighbours: a city of a hundred thousand blocks must not cascade. */
    private static final int FLAGS = 2 | 16;
    /** Units of work a tick (a write is 4, a block that is already right is 1): at most 3000 blocks written. */
    private static final long BUDGET = 12_000L;
    private static final double RANGE = 56.0;
    private static final int SCAN_EVERY = 40;
    /** A built city that has no way in yet is looked at again, near a player, this often (ticks): the caves round it may only now be loaded. */
    private static final long WAYS_EVERY = 1200L;
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static final Map<Long, CityPlan> PLANS = new LinkedHashMap<Long, CityPlan>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CityPlan> eldest) { return size() > 6; }
    };
    private static final Map<Integer, BlockState> STATES = new HashMap<>();
    /** When each city was last looked at for ways in (game ticks; not saved: a restart looks again). */
    private static final Map<Long, Long> WAYS_TRIED = new HashMap<>();
    private static Job job;
    /** After a failure the builder rests until this tick, so one bad spot cannot flood the log. */
    private static long pausedFrom, pausedUntil;

    /** The city being built right now. */
    private static final class Job {
        final ServerLevel level;
        final CityPlan plan;
        final long key;
        final CityCursor cursor;
        final WorldSink sink;
        final List<int[]> chunks;
        int idle;

        Job(CityPlan plan, long key, CityCursor cursor, ServerLevel level) {
            this.level = level;
            this.plan = plan;
            this.key = key;
            this.cursor = cursor;
            this.sink = new WorldSink(level);
            this.chunks = plan.chunks();
        }
    }

    /** The plan of a cell (kept for a few, planning is cheap but not free). */
    public static CityPlan plan(int cellX, int cellZ) {
        long key = CitySave.key(cellX, cellZ);
        CityPlan p = PLANS.get(key);
        if (p == null) {
            p = CityPlan.of(cellX, cellZ);
            PLANS.put(key, p);
        }
        return p;
    }

    // ---------------------------------------------------------------- the server tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(LivingDungeon.DIMENSION);
        if (level == null) {
            job = null;
            return;
        }
        long tick = server.getTickCount();
        if (tick >= pausedFrom && tick < pausedUntil) return;
        if (job != null && job.level != level) job = null;                 // left over from another world
        try {
            if (job != null) {
                if (!work(server, level, job)) job = null;
            } else if (tick % SCAN_EVERY == 29) {
                job = start(server, level);
                if (job == null) retryWays(server, level, tick);
            }
        } catch (RuntimeException failure) {
            LOG.error("[nusmp] The Golden City builder failed; the job is dropped and tried again in a minute", failure);
            job = null;
            pausedFrom = tick;
            pausedUntil = tick + 1200;
        }
    }

    /** A job for the first unbuilt city that a player is close to and nobody stands inside of; null if there is none. */
    private static Job start(MinecraftServer server, ServerLevel level) {
        CitySave save = null;
        for (ServerPlayer p : level.players()) {
            BlockPos at = p.blockPosition();
            int pcx = Math.floorDiv(at.getX(), CityPlan.CELL), pcz = Math.floorDiv(at.getZ(), CityPlan.CELL);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    if (!DungeonTier.isCityCell(cx, cz)) continue;
                    if (save == null) save = CitySave.get(server);
                    long key = CitySave.key(cx, cz);
                    if (save.isDone(key)) continue;
                    CityPlan plan = plan(cx, cz);
                    double ex = at.getX() - plan.centreX, ez = at.getZ() - plan.centreZ;
                    if (ex * ex + ez * ez > RANGE * RANGE || someoneInside(level, plan)) continue;
                    CitySave.Entry e = save.lookup(key);
                    CityCursor cursor = e == null ? new CityCursor(plan, 0, 0) : CityCursor.resume(plan, e.op);
                    return new Job(plan, key, cursor, level);
                }
            }
        }
        return null;
    }

    /** One tick of work. False once the job is over (done, or given up because nobody is about). */
    private static boolean work(MinecraftServer server, ServerLevel level, Job j) {
        if (!someoneNear(level, j.plan, 96.0)) {
            if (++j.idle > 400) return false;
        } else {
            j.idle = 0;
        }
        for (int[] c : j.chunks) if (level.getChunkSource().getChunkNow(c[0], c[1]) == null) return true;      // waits until the district is loaded
        CitySave save = CitySave.get(server);
        building(() -> j.cursor.run(j.sink, BUDGET));
        if (!j.cursor.done()) {
            save.progress(j.key, j.cursor.op(), j.cursor.cell());
            return true;
        }
        finish(level, j.plan, j.key, save, true);
        return false;
    }

    private static boolean someoneNear(ServerLevel level, CityPlan plan, double range) {
        for (ServerPlayer p : level.players()) {
            double ex = p.getX() - plan.centreX, ez = p.getZ() - plan.centreZ;
            if (ex * ex + ez * ez <= range * range) return true;
        }
        return false;
    }

    /** Is a player standing in the district's volume? Nothing is built round them. */
    private static boolean someoneInside(ServerLevel level, CityPlan plan) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            BlockPos at = p.blockPosition();
            if (at.getX() >= plan.minX() - 2 && at.getX() <= plan.maxX() + 2 && at.getZ() >= plan.minZ() - 2 && at.getZ() <= plan.maxZ() + 2
                    && at.getY() >= plan.floorY - 4 && at.getY() <= CityPlan.CAP_Y + 2) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- finishing
    /**
     * Every block the builder writes is written inside this call, which tells the Dungeon's guard (the rule that its blocks cannot be
     * broken) that the mod itself is building, so that the guard lets the carve, the shell, the water of the moat and the ways in through.
     */
    private static void building(Runnable work) { DungeonGuard.internal(work); }

    private static void finish(ServerLevel level, CityPlan plan, long key, CitySave save, boolean guards) {
        save.progress(key, plan.ops.size(), 0);
        save.finish(key);                                                   // first: whatever fails below, the city is never built twice
        try {
            openWays(level, plan, key, save);
        } catch (RuntimeException failure) {
            LOG.warn("[nusmp] The ways into the Golden City of cell ({}, {}) could not be opened", plan.cellX, plan.cellZ, failure);
        }
        rescue(level, plan);
        if (guards) placeGuards(level, plan);
    }

    /** Two to four mid-tier creatures of the base Tensura mod stand guard on the plaza (the Dungeon's own, they may starve like the rest). */
    private static void placeGuards(ServerLevel level, CityPlan plan) {
        int spawned = 0;
        for (int[] s : plan.sentinels) {
            try {
                if (Wildlife.spawnGuard(level, new BlockPos(s[0], s[1], s[2]))) spawned++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A guard of the Golden City could not be spawned", failure);
            }
        }
        if (spawned == 0) LOG.warn("[nusmp] The Golden City of cell ({}, {}) has no guards", plan.cellX, plan.cellZ);
    }

    // ---------------------------------------------------------------- the ways in
    /**
     * Opens the ways into a built city (see {@link CityWays}): a passage through the wall and the shell, and through the rock between
     * them and a cave when the cave is not at the wall, ending in an open iron door. The door cell cannot hold water, so the flooded caves
     * stay out. The doors are saved. Returns how many ways were opened; 0 if no cave was close enough, or the caves round the city are not loaded.
     */
    static int openWays(ServerLevel level, CityPlan plan, long key, CitySave save) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<CityWays.Way> ways = CityWays.choose(plan, (x, y, z) -> {
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return CityWays.UNKNOWN;
            BlockState s = level.getBlockState(pos.set(x, y, z));
            return s.isAir() || s.is(Blocks.WATER) ? CityWays.OPEN : CityWays.SOLID;
        });
        if (ways.isEmpty()) return 0;
        long[] doors = new long[ways.size()];
        building(() -> {
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int w = 0; w < ways.size(); w++) {
                CityWays.Way way = ways.get(w);
                Direction facing = way.dx() > 0 ? Direction.EAST : way.dx() < 0 ? Direction.WEST : way.dz() > 0 ? Direction.SOUTH : Direction.NORTH;
                int y1 = plan.floorY + 1, y2 = plan.floorY + 2;
                int doorAt = Math.abs(way.doorX() - way.fromX()) + Math.abs(way.doorZ() - way.fromZ());      // the cells up to the door are the city's, the ones beyond it rock
                for (int i = 0; i < way.length(); i++) {
                    int x = way.fromX() + way.dx() * i, z = way.fromZ() + way.dz() * i;
                    for (int y = y1; y <= y2; y++) {
                        pos.set(x, y, z);
                        if (i == doorAt) {
                            level.setBlock(pos, doorState(facing, y == y2), FLAGS);
                        } else {
                            BlockState cur = level.getBlockState(pos);
                            if (cur.isAir() || (i > doorAt && cur.is(Blocks.WATER))) continue;               // wall, shell and rock are cut; the water of a cave is left alone
                            level.setBlock(pos, air, FLAGS);
                        }
                    }
                }
                doors[w] = BlockPos.asLong(way.doorX(), y1, way.doorZ());
            }
        });
        save.setWays(key, doors);
        return doors.length;
    }

    /** One half of an iron door, standing open along the passage: it keeps the water out of its cell and lets a swimmer through. */
    private static BlockState doorState(Direction facing, boolean upper) {
        return Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    /** A built city with no way in is looked at again when a player is near it, now and then: the caves round it may not have been loaded before. */
    private static void retryWays(MinecraftServer server, ServerLevel level, long tick) {
        CitySave save = null;
        for (ServerPlayer p : level.players()) {
            BlockPos at = p.blockPosition();
            int pcx = Math.floorDiv(at.getX(), CityPlan.CELL), pcz = Math.floorDiv(at.getZ(), CityPlan.CELL);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    if (!DungeonTier.isCityCell(cx, cz)) continue;
                    if (save == null) save = CitySave.get(server);
                    long key = CitySave.key(cx, cz);
                    if (!save.isDone(key) || save.ways(key).length > 0) continue;
                    Long last = WAYS_TRIED.get(key);
                    if (last != null && tick - last < WAYS_EVERY && tick >= last) continue;
                    CityPlan plan = plan(cx, cz);
                    double ex = at.getX() - plan.centreX, ez = at.getZ() - plan.centreZ;
                    if (ex * ex + ez * ez > 80.0 * 80.0) continue;
                    for (int[] c : plan.chunks()) if (level.getChunkSource().getChunkNow(c[0], c[1]) == null) return;
                    WAYS_TRIED.put(key, tick);
                    openWays(level, plan, key, save);
                    return;                                                     // one city a pass
                }
            }
        }
    }

    /** Anyone who was caught inside the new stonework is set down on the plaza. */
    private static void rescue(ServerLevel level, CityPlan plan) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !plan.contains(p.blockPosition().getX(), p.blockPosition().getZ())) continue;
            BlockPos feet = p.blockPosition();
            if (feet.getY() < plan.floorY - 4 || feet.getY() > CityPlan.CAP_Y) continue;
            if (!level.noCollision(p, p.getBoundingBox())) p.teleportTo(level, plan.centreX + 0.5, plan.floorY + 1, plan.centreZ + 5.5, p.getYRot(), p.getXRot());
        }
    }

    // ---------------------------------------------------------------- for the admin command
    /** Builds a city right now, in one go: loads and generates its chunks, places every op, saves it as built. Returns the number of ops. */
    public static int buildNow(ServerLevel level, int cellX, int cellZ) {
        if (!LivingDungeon.in(level)) return 0;                             // the Golden Cities exist in the Dungeon and nowhere else
        CityPlan plan = plan(cellX, cellZ);
        long key = CitySave.key(cellX, cellZ);
        if (job != null && job.key == key) job = null;
        for (int[] c : plan.chunks()) level.getChunk(c[0], c[1]);
        CitySave save = CitySave.get(level.getServer());
        boolean rebuilt = save.isDone(key);
        WorldSink sink = new WorldSink(level);
        building(() -> new CityCursor(plan, 0, 0).run(sink, Long.MAX_VALUE));
        finish(level, plan, key, save, !rebuilt);
        return plan.ops.size();
    }

    /** Is this block inside the vault of a Golden City (its oval, from the foundation to the roof)? For code that must leave a city's blocks alone. */
    public static boolean inCity(int x, int y, int z) {
        int cx = Math.floorDiv(x, CityPlan.CELL), cz = Math.floorDiv(z, CityPlan.CELL);
        if (y > CityPlan.CAP_Y || y < -60 || !DungeonTier.isCityCell(cx, cz)) return false;
        CityPlan plan = plan(cx, cz);
        return y >= plan.floorY - 3 && plan.contains(x, z);
    }

    /** Drops the running job if it is this city's (its saved record is forgotten by the caller). */
    public static void forget(long key) {
        if (job != null && job.key == key) job = null;
    }

    // ---------------------------------------------------------------- the world
    /** Writes plan cells into the Dungeon. */
    private static final class WorldSink implements CityCursor.Sink {
        private final ServerLevel level;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private CityPlan.Op last;
        private BlockState lastState;

        WorldSink(ServerLevel level) { this.level = level; }

        @Override
        public boolean place(CityPlan.Op op, int x, int y, int z) {
            pos.set(x, y, z);
            BlockState cur = level.getBlockState(pos);
            if (op.kind() == CityPlan.Kind.CARVE) {
                if (cur.isAir() || cur.is(Blocks.BEDROCK)) return false;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                return true;
            }
            if (op != last) {
                last = op;
                lastState = state(op.pal(), op.variant());
            }
            if (cur == lastState || (lastState.isAir() && cur.isAir())) return false;
            level.setBlock(pos, lastState, FLAGS);
            return true;
        }
    }

    // ---------------------------------------------------------------- palette
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** The block state of a palette entry and variant (see CityPlan: bits 0-1 facing, bit 2 flip, bits 3-4 candles minus one). */
    static BlockState state(CityPlan.Pal pal, int variant) {
        int key = pal.ordinal() * 64 + variant;
        BlockState s = STATES.get(key);
        if (s == null) {
            try { s = shaped(pal, variant); } catch (RuntimeException unexpected) { s = plain(pal); }
            STATES.put(key, s);
        }
        return s;
    }

    private static BlockState shaped(CityPlan.Pal pal, int variant) {
        boolean flip = (variant & CityPlan.FLIP) != 0;
        BlockState s = plain(pal);
        switch (pal) {
            case STAIRS -> s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, FACINGS[variant & 3]).setValue(BlockStateProperties.HALF, flip ? Half.TOP : Half.BOTTOM);
            case SLAB -> s = s.setValue(BlockStateProperties.SLAB_TYPE, flip ? SlabType.TOP : SlabType.BOTTOM);
            case CLUSTER -> s = s.setValue(BlockStateProperties.FACING, flip ? Direction.DOWN : Direction.UP);
            case LANTERN, SOUL_LANTERN -> s = s.setValue(BlockStateProperties.HANGING, flip);
            case CANDLE -> s = s.setValue(BlockStateProperties.CANDLES, ((variant >> 3) & 3) + 1).setValue(BlockStateProperties.LIT, true);
            default -> { }
        }
        return s;
    }

    private static BlockState plain(CityPlan.Pal pal) {
        return switch (pal) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case GOLD -> Blocks.GOLD_BLOCK.defaultBlockState();
            case GILDED -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
            case BRICKS -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case CRACKED -> Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case SMOOTH -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case CHISELED -> Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            case BLACKSTONE -> Blocks.BLACKSTONE.defaultBlockState();
            case STAIRS -> Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState();
            case SLAB -> Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState();
            case AMETHYST -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            case CLUSTER -> Blocks.AMETHYST_CLUSTER.defaultBlockState();
            case SEA_LANTERN -> Blocks.SEA_LANTERN.defaultBlockState();
            case LANTERN -> Blocks.LANTERN.defaultBlockState();
            case SOUL_LANTERN -> Blocks.SOUL_LANTERN.defaultBlockState();
            case CHAIN -> Blocks.CHAIN.defaultBlockState();
            case CANDLE -> Blocks.CANDLE.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case END_ROD -> Blocks.END_ROD.defaultBlockState();
            case CARPET_GOLD -> Blocks.YELLOW_CARPET.defaultBlockState();
            case CARPET_PURPLE -> Blocks.PURPLE_CARPET.defaultBlockState();
            case GLASS -> Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
            case CRYING -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case ALTAR -> CityBlocks.AURELIAN_ALTAR.get().defaultBlockState();
        };
    }
}
