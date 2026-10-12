package com.newuniverse.nusmp.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What a creature of the Dungeon leaves when it dies (docs/living_dungeon.md): no items, a Carcass. Scavengers eat it a portion at a time;
 * left alone it decays through three stages and then returns to the soil: its nutrition goes back into the chunk's magicules and a
 * Dungeon Mycelium veil grows where it lay, which the slimes graze. A Butcher Cleaver (stage 3) cuts the meat out of it first.
 */
public class CarcassBlock extends Block {
    /** 0 slime, 1 Living Armor, 2 Basilisk, 3 Direwolf, 4 anything else. */
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 4);
    /** Portions of meat left (1-3). */
    public static final IntegerProperty MEAT = IntegerProperty.create("meat", 1, 3);
    /** Decay stage (0 fresh .. 3 rotten, which is the end). */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 6, 15);
    private static final int DECAY_TICKS = 1200;

    public CarcassBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(KIND, 0).setValue(MEAT, 2).setValue(AGE, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(KIND, MEAT, AGE); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    /** Cut meat out of it (see Harvest): any tool, the Butcher Cleaver best. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        return level instanceof ServerLevel sl && Harvest.cut(sl, pos, player, stack) ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !old.is(this)) level.scheduleTick(pos, this, DECAY_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        int age = state.getValue(AGE);
        if (age < 3) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            level.scheduleTick(pos, this, DECAY_TICKS);
            return;
        }
        returnToSoil(level, pos, state.getValue(MEAT));
    }

    /** The end of a carcass: its nutrition goes back to the chunk and a veil of mycelium grows. */
    static void returnToSoil(ServerLevel level, BlockPos pos, int meat) {
        EcosystemData eco = LivingDungeon.eco(level, pos);
        if (eco != null) {
            eco.magicule += 30f * meat;
            eco.clamp();
            LivingDungeon.touch(level, pos);
        }
        level.setBlock(pos, DungeonBlocks.DUNGEON_MYCELIUM.get().defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12, 0.4, 0.2, 0.4, 0.0);
    }

    /** A scavenger eats a portion: one fewer, and the carcass is gone with the last. */
    public static void nibble(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!s.is(DungeonBlocks.CARCASS.get())) return;
        int meat = s.getValue(MEAT);
        if (meat <= 1) level.removeBlock(pos, false); else level.setBlock(pos, s.setValue(MEAT, meat - 1), 3);
    }

    /** Leaves a carcass where something died (or beside it if the spot is taken). Heavier creatures leave more meat. */
    public static void leave(ServerLevel level, BlockPos at, int kind, float weight) {
        if (!LivingDungeon.in(level)) return;
        int meat = weight >= 20 ? 3 : weight >= 6 ? 2 : 1;
        BlockState state = DungeonBlocks.CARCASS.get().defaultBlockState().setValue(KIND, Math.max(0, Math.min(4, kind))).setValue(MEAT, meat);
        for (BlockPos p : new BlockPos[]{at, at.above(), at.north(), at.south(), at.east(), at.west()}) {
            BlockState cur = level.getBlockState(p);
            if ((cur.isAir() || cur.canBeReplaced()) && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) {
                level.setBlock(p, state, 3);
                return;
            }
        }
    }
}
