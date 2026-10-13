package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonBlocks;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The collapse's hands: the Dungeon seen by {@link ColumnSweep}. What stands, what is free underneath, and taking a block away, either as
 * a falling block that vanishes where it lands or simply crumbling in a puff of dust. Every write is quiet (clients are told, neighbours
 * and shapes are not updated, so nothing cascades or drops) and must run inside {@code DungeonGuard.internal}, which the caller does.
 *
 * <p>A collapse of a standing Nexus really takes the blocks. A "pocket" (a collapse outside any Nexus: a Wild Magic Surge in the
 * catacombs) takes nothing: the ceiling's underside rains down as falling blocks that vanish on landing, and the rock stays. The
 * Dungeon's terrain is only ever removed where a Nexus stood.
 */
final class CollapseWorld implements ColumnSweep.Sink {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Dust puffs a tick at the most, and one crumbling block in this many shows one. */
    private static final int PUFFS = 24, PUFF_EVERY = 7;

    private final ServerLevel level;
    private final boolean destroys;
    private final CollapseSweep order;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), below = new BlockPos.MutableBlockPos();
    private LevelChunk chunk;
    private int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
    private CollapsePlan.Stage stage = CollapsePlan.Stage.CRACKS;
    private int puffs, crumbled, started, refused;
    /** Falling blocks are turned off for the rest of the collapse once the Dungeon's guard refuses them (see {@link #spawn}). */
    private boolean debrisOn = true;
    /** True only while this collapse is making a falling block, so that {@link CollapseManager#onFallingBlockJoin} lets that one (and no other) past the Dungeon's guard. */
    static boolean spawning;

    CollapseWorld(ServerLevel level, boolean destroys, CollapseSweep order) {
        this.level = level;
        this.destroys = destroys;
        this.order = order;
    }

    /** Called at the start of every tick: forgets the cached chunk and the tick's dust, and learns which stage the clock is in. */
    void begin(CollapsePlan.Stage now) {
        stage = now;
        chunk = null;
        chunkX = Integer.MIN_VALUE;
        puffs = 0;
    }

    /** Falling blocks started and refused so far (for the status line). */
    int started() { return started; }

    int refused() { return refused; }

    private LevelChunk chunkOf(int x, int z) {
        int cx = x >> 4, cz = z >> 4;
        if (chunk == null || cx != chunkX || cz != chunkZ) {
            chunk = level.getChunkSource().getChunkNow(cx, cz);
            chunkX = cx;
            chunkZ = cz;
        }
        return chunk;
    }

    // ---------------------------------------------------------------- what the sweep asks
    @Override
    public boolean loaded(int x, int z) { return chunkOf(x, z) != null; }

    @Override
    public boolean solid(int x, int y, int z, boolean wipe) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;
        LevelChunk c = chunkOf(x, z);
        if (c == null) return false;
        pos.set(x, y, z);
        BlockState s = c.getBlockState(pos);
        if (s.isAir() || DungeonGuard.isFluidOnly(s) || s.is(Blocks.BEDROCK)) return false;
        return !s.is(DungeonBlocks.DUNGEON_GATE.get());                                // the Root Gates stand until the collapse takes them down itself
    }

    @Override
    public boolean freeBelow(int x, int y, int z) {
        if (!debrisOn || y - 1 < level.getMinBuildHeight()) return false;
        LevelChunk c = chunkOf(x, z);
        if (c == null) return false;
        pos.set(x, y, z);
        if (!level.isPositionEntityTicking(pos)) return false;                       // a falling block in a chunk that does not tick would hang in the air
        below.set(x, y - 1, z);
        BlockState b = c.getBlockState(below);
        return destroys ? b.getCollisionShape(level, below).isEmpty() : b.isAir();
    }

    @Override
    public boolean remove(int x, int y, int z, boolean asEntity) {
        LevelChunk c = chunkOf(x, z);
        if (c == null) return false;
        pos.set(x, y, z);
        BlockState state = c.getBlockState(pos);
        if (state.isAir()) return false;
        BlockPos at = pos.immutable();
        if (destroys) {
            if (!level.setBlock(at, Blocks.AIR.defaultBlockState(), NexusSink.FLAGS)) return false;
            if (!(asEntity && spawn(at, state, x, z))) crumble(at, state);
        } else if (!(asEntity && spawn(at.below(), state, x, z))) {
            crumble(at, state);                                                      // a pocket takes nothing: the dust is all that shows
        }
        return true;
    }

    // ---------------------------------------------------------------- a falling block
    /**
     * Starts a falling block at `at` (where the block was, or the free cell under it in a pocket). It lands without placing itself again
     * and without dropping an item, so it can never pile up and wall a corridor; it hurts whoever it lands on a little. Returns false if
     * the world refused it: {@code DungeonGuard.onEntityJoin} cancels every plain falling block in the Dungeon, even inside
     * {@code DungeonGuard.internal}. {@link CollapseManager#onFallingBlockJoin} (registered after it, receiving cancelled events) lets the
     * blocks made here through while {@link #spawning} is set; if it is not registered, falling blocks are switched off after three
     * refusals and the blocks crumble in dust instead.
     */
    private boolean spawn(BlockPos at, BlockState state, int x, int z) {
        BlockState shown = state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE) : state;
        if (!shown.getFluidState().isEmpty()) return false;                           // kelp and the like hold water of their own: falling, they would leave a block of it behind
        FallingBlockEntity fall;
        spawning = true;
        try {
            fall = FallingBlockEntity.fall(level, at, shown);
        } finally {
            spawning = false;
        }
        fall.disableDrop();
        int i = order.indexOf(x, z);
        fall.setHurtsEntities(1.0f, CollapseRules.hit(stage == CollapsePlan.Stage.CRACKS || (i >= 0 && order.spared(i))));
        if (level.getEntity(fall.getUUID()) == null) {
            refused++;
            if (refused == 3) {
                debrisOn = false;
                LOG.warn("[nusmp] The Dungeon refuses the collapse's falling blocks (DungeonGuard.onEntityJoin cancels them; register CollapseManager.onFallingBlockJoin at LOWEST priority receiving cancelled events); the collapse crumbles its blocks instead");
            }
            return false;
        }
        started++;
        return true;
    }

    // ---------------------------------------------------------------- dust
    private void crumble(BlockPos at, BlockState state) {
        if (puffs >= PUFFS || ++crumbled % PUFF_EVERY != 0) return;
        puffs++;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
    }
}
