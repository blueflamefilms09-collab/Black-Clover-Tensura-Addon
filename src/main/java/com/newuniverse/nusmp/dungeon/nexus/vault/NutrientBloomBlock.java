package com.newuniverse.nusmp.dungeon.nexus.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Nutrient Bloom of a Nutrient Garden (docs/living_dungeon.md, "Hidden Mana Vaults"): a glowing flower on moss. It ripens slowly
 * (four stages, each a random tick that is taken one time in eight: about half an hour from bud to bloom); a ripe one gives Nutrient Nectar
 * when it is right-clicked and goes back to a bud, so a garden never runs dry but never overflows either. The Dungeon's stone guard keeps
 * the flower itself from being picked.
 */
public class NutrientBloomBlock extends Block {
    /** 0 bud, 1 and 2 growing, 3 in bloom. */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public NutrientBloomBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AGE, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 3 && random.nextInt(8) == 0) level.setBlock(pos, state.setValue(AGE, age + 1), 2);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < 3) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack nectar = new ItemStack(NexusBlocks.NUTRIENT_NECTAR.get(), 1 + level.random.nextInt(2));
        if (!player.getInventory().add(nectar)) player.drop(nectar, false);
        level.setBlock(pos, state.setValue(AGE, 0), 2);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (state.getValue(AGE) < 2 || rnd.nextInt(4) != 0) return;
        level.addParticle(state.getValue(AGE) == 3 ? ParticleTypes.END_ROD : ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.3 + rnd.nextDouble() * 0.4, pos.getY() + 0.5 + rnd.nextDouble() * 0.5,
                pos.getZ() + 0.3 + rnd.nextDouble() * 0.4, 0.0, 0.015, 0.0);
    }
}
