package com.newuniverse.nusmp.labyrinth.trap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: one floor plate, four colours, one block. The colour is a block state property ({@code color} 0..3: red, blue, gold, green), so a
 * template writes the colour of each plate as a property and Jigsaw rotation cannot disturb it; the {@code lit} flag only shows which plates
 * the party has already pressed. The plate holds no memory: a right click asks the nearest Riddle Stone to judge the press
 * ({@link RiddleStoneBlockEntity#press}), because a sequence that lived in the plates would let two players solve half a puzzle each and
 * would break when an admin moved a plate. It is a click (not a pressure plate): walking across a room full of plates must never press one
 * by accident, and a mob must never solve a puzzle. A creative player who sneaks with an empty hand cycles the colour, the builder's tool
 * for placing the four by hand (the block item always places colour 0). The plate is a 2-pixel slab, so the room's floor reads flat.
 */
public final class SigilPlateBlock extends Block {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 3);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 2, 15);

    public SigilPlateBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(COLOR, 0).setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(COLOR, LIT); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isCreative() && player.isShiftKeyDown()) {                                 // the builder's colour cycle
            int next = (state.getValue(COLOR) + 1) % PuzzleState.COLOURS;
            level.setBlock(pos, state.setValue(COLOR, next).setValue(LIT, false), 3);
            player.displayClientMessage(Component.translatable("label.nusmp.sigil." + PuzzleState.COLOUR_NAMES[next]), true);
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.CONSUME;
        RiddleStoneBlockEntity stone = RiddleStones.nearest(sl, pos);
        if (stone == null) {
            sp.displayClientMessage(Component.translatable("message.nusmp.puzzle.no_stone").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        stone.press(sp, state.getValue(COLOR), pos);
        return InteractionResult.CONSUME;
    }
}
