package com.newuniverse.nusmp.dungeon.nexus.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Writes vault plan cells into the Dungeon. Rock matches the rock beside it (deepslate or stone), an Illusion wall matches the cell it
 * replaces, bedrock is never touched, and every write is a quiet one (clients are told, neighbours and shapes are not updated; iron bars
 * are the exception: they work out their joins to the walls beside them and tell their neighbours). Run it inside
 * {@code DungeonGuard.internal}: the director does.
 */
final class VaultSink implements VaultCursor.Sink {
    /** Send the change to clients, but do not update shapes or neighbours; or, for the bars, let them connect. */
    static final int FLAGS = 2 | 16, FLAGS_CONNECT = 3;

    private final ServerLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    VaultSink(ServerLevel level) { this.level = level; }

    @Override
    public boolean place(VaultOp op, int x, int y, int z) {
        pos.set(x, y, z);
        BlockState cur = level.getBlockState(pos);
        if (cur.is(Blocks.BEDROCK)) return false;
        switch (op.kind()) {
            case CARVE -> {
                if (cur.isAir()) return false;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                return true;
            }
            case SOFT -> {
                if (VaultTerrain.solid(WorldTerrain.classify(cur))) return false;
                level.setBlock(pos, state(op.pal(), deep(cur)), FLAGS);
                return true;
            }
            default -> {
                BlockState want = state(op.pal(), deep(cur));
                if (op.pal() == VaultOp.Pal.IRON_BARS) want = Block.updateFromNeighbourShapes(want, level, pos.immutable());         // a placed bar does not join its neighbours by itself
                if (cur == want || (want.isAir() && cur.isAir())) return false;
                level.setBlock(pos, want, op.pal() == VaultOp.Pal.IRON_BARS ? FLAGS_CONNECT : FLAGS);
                return true;
            }
        }
    }

    /** Is the rock here (or, where there is none, round here) deepslate? A cell that already holds either kind of rock keeps its kind. */
    private boolean deep(BlockState cur) {
        if (cur.is(Blocks.DEEPSLATE) || cur.is(NexusBlocks.ILLUSION_DEEPSLATE.get())) return true;
        if (cur.is(Blocks.STONE) || cur.is(NexusBlocks.ILLUSION_STONE.get())) return false;
        int deep = 0, stone = 0;
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(Blocks.DEEPSLATE) || n.is(NexusBlocks.ILLUSION_DEEPSLATE.get())) deep++;
            else if (n.is(Blocks.STONE) || n.is(NexusBlocks.ILLUSION_STONE.get())) stone++;
        }
        if (deep != stone) return deep > stone;
        return pos.getY() < 4;                                                                          // the world's own deepslate line
    }

    /** The block state of a palette entry. {@code deep} picks deepslate for the entries that follow the rock. */
    static BlockState state(VaultOp.Pal pal, boolean deep) {
        return switch (pal) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case ROCK -> (deep ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
            case ILLUSION -> (deep ? NexusBlocks.ILLUSION_DEEPSLATE.get() : NexusBlocks.ILLUSION_STONE.get()).defaultBlockState();
            case MOSS -> Blocks.MOSS_BLOCK.defaultBlockState();
            case MOSS_CARPET -> Blocks.MOSS_CARPET.defaultBlockState();
            case SPORE_BLOSSOM -> Blocks.SPORE_BLOSSOM.defaultBlockState();
            case SOUL_LANTERN_HANGING -> Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
            case PLATE_IDLE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case PLATE_LIT -> Blocks.SEA_LANTERN.defaultBlockState();
            case PLATE_DONE -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            case IRON_BARS -> Blocks.IRON_BARS.defaultBlockState();
            case BLACKSTONE_BRICKS -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case POLISHED_BLACKSTONE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case CRYING_OBSIDIAN -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case BLOOM_0 -> bloom(0);
            case BLOOM_1 -> bloom(1);
            case BLOOM_2 -> bloom(2);
            case BLOOM_3 -> bloom(3);
            case GATE_DOWN -> NexusBlocks.DESCENT_GATE.get().defaultBlockState().setValue(DescentGateBlock.RISING, false);
            case GATE_UP -> NexusBlocks.DESCENT_GATE.get().defaultBlockState().setValue(DescentGateBlock.RISING, true);
        };
    }

    private static BlockState bloom(int age) {
        Block b = NexusBlocks.NUTRIENT_BLOOM.get();
        return b.defaultBlockState().setValue(NutrientBloomBlock.AGE, age);
    }
}
