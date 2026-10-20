package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: when a tomb is over, its cell has to be given back to the void so the next tomb can be built in it, and a sweep of 192 x 192 x 36 blocks in one tick would be a stall of a second. This job
 * clears it over the next ticks, chunk by chunk and section by section: a section that holds only air is skipped without a look at its blocks ({@link LevelChunkSection#hasOnlyAir}), so a tomb
 * that fills a third of its cell costs about a hundred sections, not three thousand chunks of blocks; a section with blocks is set to air a budget of blocks per tick (flags 2, 16 and 32: sent to the
 * players, no neighbour updates, no drops, so water does not run and nothing pops out), and the block entities of the chunk go with it. Each chunk's biome is put back to the dungeon's own when its
 * blocks are gone, and every entity that is not a player is removed at the start and again at the end. It reads the cell and nothing else, so it works for a tomb whose definition has been
 * lost (an orphan) just as well. The slot is free again only when the job says {@link #done()}; until then the record stays in the pocket map, so no new tomb is built over a half-cleared one.
 */
public final class PocketClear {
    private static final Logger LOG = LogUtils.getLogger();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int FLAGS = 2 | 16 | 32;

    private final int slot;
    private final List<int[]> chunks;
    private final int minSection, maxSection;
    private int chunkIdx, sectionIdx, blockIdx;
    private boolean started, biomeDone, finished;
    private long cleared;
    private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

    public PocketClear(int slot) {
        this.slot = slot;
        this.chunks = PocketBiomes.chunksOf(slot);
        this.minSection = PocketCell.Y_MIN >> 4;
        this.maxSection = PocketCell.Y_MAX >> 4;
        this.sectionIdx = minSection;
    }

    public int slot() { return slot; }

    public boolean done() { return finished; }

    /** Percent of the chunks swept. */
    public int percent() { return finished ? 100 : Math.min(99, chunkIdx * 100 / Math.max(1, chunks.size())); }

    public long cleared() { return cleared; }

    /** One slice of the sweep: at most {@code budget} blocks are set (a section that is only air is free). Returns true when the cell is empty. */
    public boolean step(ServerLevel level, int budget) {
        if (finished) return true;
        try {
            if (!started) {
                started = true;
                sweepEntities(level);
            }
            int left = Math.max(512, budget);
            while (left > 0 && chunkIdx < chunks.size()) {
                int[] c = chunks.get(chunkIdx);
                LevelChunk chunk = level.getChunk(c[0], c[1]);
                if (sectionIdx > maxSection) {                                  // this chunk's blocks are gone: its biome goes back, then the next chunk
                    PocketBiomes.applyChunk(level, c[0], c[1], PocketBiomes.DEFAULT_BIOME);
                    chunkIdx++;
                    sectionIdx = minSection;
                    blockIdx = 0;
                    left -= 64;
                    continue;
                }
                int index = chunk.getSectionIndex(sectionIdx << 4);
                if (index < 0 || index >= chunk.getSections().length) {
                    sectionIdx++;
                    continue;
                }
                LevelChunkSection section = chunk.getSection(index);
                if (section.hasOnlyAir()) {
                    sectionIdx++;
                    blockIdx = 0;
                    left -= 8;
                    continue;
                }
                if (blockIdx == 0) {
                    for (BlockPos bp : List.copyOf(chunk.getBlockEntitiesPos())) if ((bp.getY() >> 4) == sectionIdx) chunk.removeBlockEntity(bp);
                }
                int x0 = c[0] << 4, z0 = c[1] << 4, y0 = sectionIdx << 4;
                while (blockIdx < 4096 && left > 0) {
                    int i = blockIdx++;
                    left--;
                    int lx = i & 15, lz = (i >> 4) & 15, ly = (i >> 8) & 15;
                    m.set(x0 + lx, y0 + ly, z0 + lz);
                    if (!level.getBlockState(m).isAir()) {
                        level.setBlock(m, AIR, FLAGS);
                        cleared++;
                    }
                }
                if (blockIdx >= 4096) {
                    sectionIdx++;
                    blockIdx = 0;
                }
            }
            if (chunkIdx >= chunks.size()) {
                sweepEntities(level);
                finished = true;
                LOG.debug("[nusmp] The cell of slot {} is clear ({} blocks removed).", slot, cleared);
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The sweep of the pocket cell {} failed at chunk {} (it goes on with the next chunk): {}", slot, chunkIdx, ex.toString());
            chunkIdx++;
            sectionIdx = minSection;
            blockIdx = 0;
        }
        return finished;
    }

    /** Removes every entity of the cell that is not a player (items, monsters, the boss, spawned adds). */
    private void sweepEntities(ServerLevel level) {
        for (Entity e : level.getEntitiesOfClass(Entity.class, PocketCell.bounds(slot).expandTowards(0, 60, 0), en -> !(en instanceof ServerPlayer))) e.discard();
    }
}
