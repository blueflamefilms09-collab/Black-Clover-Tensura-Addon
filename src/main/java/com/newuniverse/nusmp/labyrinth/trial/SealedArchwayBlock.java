package com.newuniverse.nusmp.labyrinth.trial;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the keystone of a trial chamber is a block WITH a block entity rather than a command block or a spawner because the trial must
 * survive everything a world does: it is saved with the chunk, it reloads with the chunk, it ticks only while its chunk ticks, and it is
 * placed by a Jigsaw template with its settings in the template's NBT (a spawner cannot seal doors, a command block cannot be rotated with
 * a piece or know who is inside). It implements {@link EntityBlock} directly instead of extending {@code BaseEntityBlock}, which would
 * hide the model ({@code RenderShape.INVISIBLE}) and demand a {@code MapCodec}: this is a plain cube with an eye on it. The {@link #PHASE}
 * property mirrors the block entity's state so the CLIENT needs nothing synced to draw the right face (idle: a dim eye; active: the eye
 * burns cyan and the block lights up; cleared: the eye is cracked): a block state travels in the chunk packet, a block entity's tag only
 * on request. The ticker is server-only, so a client never runs trial logic on its stale copy.
 */
public final class SealedArchwayBlock extends Block implements EntityBlock {
    /** The three faces of the keystone; the names are the blockstate variant keys. */
    public enum Phase implements StringRepresentable {
        IDLE("idle"), ACTIVE("active"), CLEARED("cleared");

        private final String key;

        Phase(String key) { this.key = key; }

        @Override
        public String getSerializedName() { return key; }

        public static Phase of(TrialState state) {
            return switch (state) {
                case ACTIVE -> ACTIVE;
                case CLEARED -> CLEARED;
                default -> IDLE;
            };
        }
    }

    public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);

    public SealedArchwayBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any().setValue(PHASE, Phase.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PHASE); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SealedArchwayBlockEntity(pos, state); }

    /** Server side only: the client's copy of the block entity carries the state for show and must never spawn or seal anything. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TrialRegistry.SEALED_ARCHWAY_BE.get()) return null;
        return (lvl, pos, st, be) -> { if (be instanceof SealedArchwayBlockEntity arch) arch.serverTick(lvl, pos, st); };
    }

    /** A sneaking operator who right-clicks the keystone reads its state (what it will spawn, what it tracks, which cells it sealed). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.hasPermissions(2)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SealedArchwayBlockEntity arch) player.displayClientMessage(arch.describe(), false);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** While the trial runs the eye throws sparks: the state is read from the block state, so this needs no synced tag. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (state.getValue(PHASE) != Phase.ACTIVE || rnd.nextInt(3) != 0) return;
        double x = pos.getX() + rnd.nextDouble(), y = pos.getY() + rnd.nextDouble(), z = pos.getZ() + rnd.nextDouble();
        Fx.add(level, FxThemes.DUNGEON_TRIAL, FxOld.ELECTRIC_SPARK, x, y, z, (rnd.nextDouble() - 0.5) * 0.05, rnd.nextDouble() * 0.05, (rnd.nextDouble() - 0.5) * 0.05);
    }
}
