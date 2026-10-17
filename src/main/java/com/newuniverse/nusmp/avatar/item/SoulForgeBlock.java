package com.newuniverse.nusmp.avatar.item;

import com.newuniverse.nusmp.avatar.ritual.SoulForgeRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the Soul Forge is the mechanical trigger of the Avatar ritual: right-clicking it with a Genesis Core in the inventory locks the player in place,
 * draws the great circle and opens the Soul Forge screen (see {@link SoulForgeRitual}). It is a plain block with no block entity and no menu of its own:
 * it only hands the player and its position to the ritual, which keeps all the state on the server. {@code useWithoutItem} is used rather than the
 * item-aware use so the forge answers whatever the player holds (a Genesis Core in the hand is one of the things it answers), and a right click with a
 * block in hand while sneaking still places it, as everywhere in vanilla.
 */
public class SoulForgeBlock extends Block {
    /** The plinth, pillar and basin (the floating gem is decoration above the shape). */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public SoulForgeBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) SoulForgeRitual.interact(sp, pos);
        return InteractionResult.CONSUME;
    }
}
