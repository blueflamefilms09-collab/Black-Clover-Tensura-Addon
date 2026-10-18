package com.newuniverse.nusmp.elite.items.plush;

import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import com.newuniverse.nusmp.elite.items.EliteSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: ONE block class for all the mob plushies (the owner's "one block class"): the kind decides the name, the size of the hitbox and the pitch of the squeak, and the block state is only the way it
 * faces (so a plushie placed on a shelf looks at you). No block entity: a plushie has nothing to store. Right-click squeezes it (the squeak is the subsystem's own sound, pitched by size, a little
 * random so a row of them does not sound like one); placing one squeaks softly and counts it in the placing player's collection ({@link PlushCollection}). Plushies are soft: wool sound, break at once,
 * and a piston pushes them away instead of destroying them.
 */
public class PlushBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final PlushKinds.Kind kind;
    private final VoxelShape shape;

    public PlushBlock(PlushKinds.Kind kind) {
        super(Properties.of().mapColor(MapColor.WOOL).strength(0.4f).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.NORMAL));
        this.kind = kind;
        this.shape = shapeFor(kind.size());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public PlushKinds.Kind kind() { return kind; }

    /** The hitbox of a size class (the generated models stay inside it): 0 tiny ... 4 huge. */
    static VoxelShape shapeFor(int size) {
        return switch (Math.max(0, Math.min(4, size))) {
            case 0 -> Block.box(5, 0, 5, 11, 8, 11);
            case 1 -> Block.box(4, 0, 4, 12, 10, 12);
            case 2 -> Block.box(3, 0, 3, 13, 12, 13);
            case 3 -> Block.box(2, 0, 2, 14, 14, 14);
            default -> Block.box(1, 0, 1, 15, 16, 15);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return shape; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        squeak(level, pos, 1.0f);
        if (!level.isClientSide && player instanceof ServerPlayer p) PlushCollection.collect(p, kind.id());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        squeak(level, pos, 0.6f);
        if (!level.isClientSide && placer instanceof ServerPlayer p) PlushCollection.collect(p, kind.id());
    }

    private void squeak(Level level, BlockPos pos, float volumeScale) {
        if (level.isClientSide) return;
        float pitch = kind.pitch() * (0.92f + level.random.nextFloat() * 0.16f);
        level.playSound(null, pos, EliteSounds.PLUSH_SQUEAK.get(), SoundSource.BLOCKS, (float) (double) EliteItemsConfig.get(EliteItemsConfig.SQUEAK_VOLUME) * volumeScale, pitch);
    }
}
