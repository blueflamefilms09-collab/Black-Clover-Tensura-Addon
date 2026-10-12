package com.newuniverse.nusmp.dungeon;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.MawShapes.Kind;
import com.newuniverse.nusmp.dungeon.MawShapes.Mat;
import com.newuniverse.nusmp.dungeon.MawShapes.Plan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a Maw as a plain stone tower, ruins or an isolated gate (docs/living_dungeon.md section 2) from a {@link MawShapes} plan.
 * The Overworld surface is not under the Dungeon's guard, so the safety is here: {@link MawFit} checks the whole patch against the world
 * before the first block is written (loaded chunks, a flat enough surface, nothing but grass, leaves and natural ground in the way, no
 * water, nobody walled in, a hard cap on the writes), every block that is then written is remembered with what it replaced so a failure
 * puts the patch back, and the roots and the Maw are returned for DungeonSave so they alone vanish when the Maw closes: the stone stays
 * as flavour ruins.
 */
public final class MawManifest {
    private MawManifest() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    /** setBlock flags: stone is sent to the clients without shape updates (its states are set by hand); the Maw and its roots update their neighbours like the 1.17 mouth did. */
    private static final int STONE = 2 | 16, LIVING = 3;
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** A Maw that was written: how it manifested, where the mouth is, and every root round it (all of them go when it closes; the stone stays). */
    public record Built(Kind kind, BlockPos maw, long[] roots) {}

    /** The readable id path of the biome here ("dark_forest"); empty if it has no id. */
    public static String biomePath(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    // ---------------------------------------------------------------- trying the manifestations
    /**
     * Tries the manifestations on the ground at {@code at} (the spot DungeonMaw's search accepted): the one the hash of the place and the
     * biome picked first, then the others, or only {@code forced} if an admin asked for one. Returns what was built, or null if none fits
     * (the caller then makes the plain mouth in the ground). Never in the Dungeon itself: an admin's test there keeps the plain mouth.
     */
    static Built tryBuild(ServerLevel level, BlockPos at, Kind forced) {
        if (forced == Kind.HOLE || LivingDungeon.in(level)) return null;
        try {
            String biome = biomePath(level, at);
            long seed = MawStyle.seed(at.getX(), at.getZ(), biome);
            MawStyle.Family family = MawStyle.family(biome);
            WorldTerrain terrain = new WorldTerrain(level);
            for (Kind kind : MawShapes.order(forced != null ? forced : MawStyle.choose(seed, family))) {
                if (forced != null && kind != forced) continue;
                Plan plan = MawShapes.plan(kind, MawStyle.rotation(seed), seed, MawStyle.styleOf(family));
                MawFit.Fit fit = MawFit.scan(plan, terrain, at.getX(), at.getZ());
                Built built = fit == null ? null : write(level, plan, fit);
                if (built != null) return built;
            }
        } catch (RuntimeException unexpected) {                      // a Maw must never be lost to a bad patch: the plain mouth is the answer
            LOGGER.warn("Dungeon Maw: could not manifest near {}: {}", at, unexpected.toString());
        }
        return null;
    }

    // ---------------------------------------------------------------- reading the ground
    /** The world as {@link MawFit} reads it. */
    private static final class WorldTerrain implements MawFit.Terrain {
        private final ServerLevel level;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        WorldTerrain(ServerLevel level) { this.level = level; }

        @Override
        public boolean loaded(int x, int z) { return level.hasChunk(x >> 4, z >> 4); }

        @Override
        public int height(int x, int z) { return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); }

        @Override
        public MawFit.Cell cell(int x, int y, int z) { return classify(level.getBlockState(pos.set(x, y, z))); }

        @Override
        public boolean occupied(int x1, int y1, int z1, int x2, int y2, int z2) {
            return !level.getEntitiesOfClass(LivingEntity.class, new AABB(x1, y1, z1, x2 + 1, y2 + 1, z2 + 1)).isEmpty();
        }

        @Override
        public int minY() { return level.getMinBuildHeight(); }

        @Override
        public int maxY() { return level.getMaxBuildHeight(); }
    }

    /**
     * What a block is to the scan: air; soft (grass, flowers, leaves, snow: it may be built over); natural ground (no ore, nothing built:
     * what a floor may replace and a sunken chamber may be cut into); a fluid; or anything else, a block entity included.
     */
    static MawFit.Cell classify(BlockState s) {
        if (!s.getFluidState().isEmpty()) return MawFit.Cell.FLUID;
        if (s.isAir()) return MawFit.Cell.AIR;
        if (s.hasBlockEntity()) return MawFit.Cell.OTHER;
        if (s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)) return MawFit.Cell.SOFT;
        if (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.SNOW)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)) return MawFit.Cell.GROUND;
        return MawFit.Cell.OTHER;
    }

    // ---------------------------------------------------------------- writing it
    /** Writes the checked patch. Every block is remembered with the state it replaced: if anything throws, the patch is put back as it was. */
    private static Built write(ServerLevel level, Plan plan, MawFit.Fit fit) {
        List<BlockPos> done = new ArrayList<>(fit.places().size());
        List<BlockState> was = new ArrayList<>(fit.places().size());
        List<Long> roots = new ArrayList<>();
        BlockPos maw = null;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        try {
            for (MawFit.Place p : fit.places()) {
                at.set(p.x(), p.y(), p.z());
                BlockPos pos = at.immutable();
                done.add(pos);
                was.add(level.getBlockState(pos));
                level.setBlock(pos, state(p.mat(), p.variant()), p.mat() == Mat.ROOT || p.mat() == Mat.MAW ? LIVING : STONE);
                if (p.mat() == Mat.ROOT) roots.add(pos.asLong());
                if (p.mat() == Mat.MAW) maw = pos;
            }
            if (maw == null) throw new IllegalStateException("the plan wrote no mouth");
        } catch (RuntimeException failed) {
            LOGGER.warn("Dungeon Maw: the {} could not be written, the ground is put back: {}", plan.kind, failed.toString());
            for (int i = done.size() - 1; i >= 0; i--) level.setBlock(done.get(i), was.get(i), STONE);
            return null;
        }
        LOGGER.info("Dungeon Maw: a {} manifests at {} ({} blocks, {} roots)", plan.kind, maw, done.size(), roots.size());
        return new Built(plan.kind, maw, roots.stream().mapToLong(Long::longValue).toArray());
    }

    // ---------------------------------------------------------------- the palette
    /** The block state of a plan cell. */
    static BlockState state(Mat mat, int variant) {
        boolean flip = (variant & MawShapes.FLIP) != 0;
        Direction facing = FACINGS[variant & 3];
        return switch (mat) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case BRICK -> Blocks.STONE_BRICKS.defaultBlockState();
            case MOSSY -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case CRACKED -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case CHISELED -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
            case MOSSY_COBBLE -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case STAIR -> Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.HALF, flip ? Half.TOP : Half.BOTTOM);
            case SLAB -> Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, flip ? SlabType.TOP : SlabType.BOTTOM);
            case LADDER -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
            case ROOT -> DungeonBlocks.MAW_ROOT.get().defaultBlockState();
            case MAW -> DungeonBlocks.DUNGEON_MAW.get().defaultBlockState();
        };
    }
}
