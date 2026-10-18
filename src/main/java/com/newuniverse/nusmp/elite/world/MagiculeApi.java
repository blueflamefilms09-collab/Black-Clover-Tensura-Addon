package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.world.block.MotherRockBlock;
import com.newuniverse.nusmp.elite.world.rules.CrystalGrowth;
import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Iterator;

/**
 * Why: the one place that reads and writes the chunk density of the world engine. Everything else (the blocks, the engine tick, the commands, the hook other systems read) goes through here,
 * so the model is applied the same way everywhere. Server side; every method tolerates a chunk that is not loaded (it answers the natural baseline or does nothing).
 *
 * <p>Reading: {@link #stored} is the density of the chunk column, {@link #effective} adds the depth bonus for a height, {@link #effectiveAt} is the number {@code EliteHooks.chunkMagicule}
 * returns for a player. A chunk that was never updated answers its natural baseline (biome only), without creating data.
 */
public final class MagiculeApi {
    private MagiculeApi() {}

    // ---------------------------------------------------------------- access

    /** The loaded chunk at a position, or null. Never loads or generates a chunk. */
    public static LevelChunk loaded(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return level == null ? null : level.getChunkAt(pos);
        return sl.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static LevelChunk loaded(ServerLevel level, ChunkPos cp) { return level.getChunkSource().getChunkNow(cp.x, cp.z); }

    /** The data of a chunk, created when missing. */
    public static ChunkMagicule data(LevelChunk chunk) { return chunk.getData(WorldAttachments.CHUNK); }

    /** The data of a chunk or null when it has none. */
    public static ChunkMagicule existing(LevelChunk chunk) { return chunk.getExistingDataOrNull(WorldAttachments.CHUNK); }

    // ---------------------------------------------------------------- the model applied to a chunk

    /** The biome factor of a chunk (cached in its data for the session). */
    public static double biomeFactor(ServerLevel level, LevelChunk chunk, ChunkMagicule d) {
        if (d != null && !Double.isNaN(d.biomeFactor)) return d.biomeFactor;
        ChunkPos cp = chunk.getPos();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, 8, 8);
        String id = level.getBiome(new BlockPos(cp.getMinBlockX() + 8, y, cp.getMinBlockZ() + 8)).unwrapKey().map(k -> k.location().toString()).orElse("");
        double f = WorldData.biomes().factorOf(id);
        if (d != null) d.biomeFactor = f;
        return f;
    }

    /** The number of Mother Rocks of the chunk that still stand, and the sum of their charges; also forgets the positions that no longer hold one. */
    public static int[] validateMothers(LevelChunk chunk, ChunkMagicule d) {
        int rocks = 0, charge = 0;
        if (d.mothers.isEmpty() || !WorldBlocks.MOTHER_ROCK.isBound()) return new int[]{0, 0};
        Iterator<Long> it = d.mothers.iterator();
        while (it.hasNext()) {
            BlockPos p = BlockPos.of(it.next());
            BlockState s = chunk.getBlockState(p);
            if (s.getBlock() instanceof MotherRockBlock) {
                rocks++;
                charge += s.getValue(MotherRockBlock.CHARGE);
            } else {
                it.remove();
            }
        }
        return new int[]{rocks, charge};
    }

    /** What the density of this chunk is pulled toward now. */
    public static double target(ServerLevel level, LevelChunk chunk, ChunkMagicule d) {
        int[] m = validateMothers(chunk, d);
        return MagiculeModel.target(MagiculeModel.DIMENSION_FLOOR, biomeFactor(level, chunk, d), d.crystalScore, CrystalGrowth.motherBonus(m[0], m[1]), d.event);
    }

    /** One update of one chunk: decay the event, move the density toward the target, stamp the time. The first update starts the chunk AT its target. */
    public static void update(ServerLevel level, LevelChunk chunk) {
        ChunkMagicule d = data(chunk);
        long now = Math.max(1L, level.getGameTime());
        double seconds = d.initialised() ? Math.max(0L, now - d.lastTick) / 20.0 : 0.0;
        d.event = (float) MagiculeModel.decayEvent(d.event, seconds, WorldConfig.eventDecayPerMinute());
        double tgt = target(level, chunk, d);
        d.density = (float) (d.initialised() ? MagiculeModel.step(d.density, tgt, seconds, WorldConfig.risePerMinute(), WorldConfig.fallPerMinute()) : tgt);
        d.lastTick = now;
        chunk.setUnsaved(true);
    }

    // ---------------------------------------------------------------- reading

    /** The stored density of the chunk column at a position (the natural baseline when the chunk was never updated), or 0 when the chunk is not loaded. */
    public static double stored(Level level, BlockPos pos) {
        LevelChunk c = loaded(level, pos);
        if (c == null) return 0.0;
        ChunkMagicule d = existing(c);
        if (d != null && d.initialised()) return d.density;
        if (level instanceof ServerLevel sl) return MagiculeModel.target(MagiculeModel.DIMENSION_FLOOR, biomeFactor(sl, c, d), 0, 0.0, 0.0);
        return 0.0;
    }

    /** The density read at a position: the stored value plus the depth bonus. 0 where the engine is not allowed to act. */
    public static double effective(Level level, BlockPos pos) {
        if (!WorldGuard.allowed(level)) return 0.0;
        return MagiculeModel.effective(stored(level, pos), pos.getY(), level.getSeaLevel());
    }

    /** What {@code EliteHooks.chunkMagicule} answers: the density where the player stands. */
    public static double effectiveAt(ServerPlayer player) {
        if (player == null) return 0.0;
        return effective(player.serverLevel(), player.blockPosition());
    }

    // ---------------------------------------------------------------- writing

    /** Adds an event bump (signed, e.g. +0.3 for a fallen calamity) to the chunk at a position and to the chunks within {@code radiusChunks}. */
    public static void bump(ServerLevel level, BlockPos pos, double delta, int radiusChunks) {
        if (!WorldGuard.allowed(level)) return;
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4, r = Math.max(0, radiusChunks);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk c = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (c == null) continue;
                ChunkMagicule d = data(c);
                if (!d.initialised()) update(level, c);
                d.event = (float) MagiculeModel.clampEvent(d.event + delta / (1.0 + Math.abs(dx) + Math.abs(dz)));
                c.setUnsaved(true);
            }
        }
    }

    /** Forces the stored density of a chunk to a value (an operator's tool): the density is set now and the event bump is chosen so that the target holds it there for a while. */
    public static void force(ServerLevel level, LevelChunk chunk, double value) {
        ChunkMagicule d = data(chunk);
        if (!d.initialised()) update(level, chunk);
        d.event = 0f;
        double natural = target(level, chunk, d);
        d.event = (float) MagiculeModel.clampEvent(value - natural);
        d.density = (float) MagiculeModel.clamp01(value);
        d.lastTick = Math.max(1L, level.getGameTime());
        chunk.setUnsaved(true);
    }

    /** Adds (or removes, negative) crystal score to the chunk at a position. */
    public static void addScore(Level level, BlockPos pos, int delta) {
        if (level == null || level.isClientSide() || delta == 0) return;
        LevelChunk c = loaded(level, pos);
        if (c == null) return;
        ChunkMagicule d = data(c);
        d.crystalScore = Math.max(0, d.crystalScore + delta);
        c.setUnsaved(true);
    }

    /** A Mother Rock stands at this position (remembered by its chunk). */
    public static void addMother(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) return;
        LevelChunk c = loaded(level, pos);
        if (c == null) return;
        ChunkMagicule d = data(c);
        if (d.mothers.add(pos.asLong())) c.setUnsaved(true);
    }

    public static void removeMother(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) return;
        LevelChunk c = loaded(level, pos);
        ChunkMagicule d = c == null ? null : existing(c);
        if (d != null && d.mothers.remove(pos.asLong())) c.setUnsaved(true);
    }

    /** How many Mother Rocks the chunk at this position has. */
    public static int mothersAt(Level level, BlockPos pos) {
        LevelChunk c = loaded(level, pos);
        ChunkMagicule d = c == null ? null : existing(c);
        return d == null ? 0 : validateMothers(c, d)[0];
    }
}
