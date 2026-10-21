package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the wall that closes a doorway of a locked room. It is the Magic Barrier of the trial halls in every physical respect (a full collision shape so nothing walks,
 * jumps or is knocked through it, bedrock hardness and a blast resistance of 3.6 million so the Dungeon Guard's rules are not the only thing holding it, piston-proof so a
 * barrier that was shoved aside cannot stay in the world, loot-less, nobody suffocates in it) and it is glass-like to the eye ({@code skipRendering} between two
 * barriers so a doorway reads as one sheet, the camera is never pushed by it) but WITH a block entity, which the trial's barrier does not have: the first barrier of a
 * room is the room's brain ({@link LockdownBarrierBlockEntity}: the state, the wave, the mobs, the cells), the others only remember where it is and take themselves
 * away if it is gone. A plain {@link EntityBlock}, like the Sealed Archway, not a {@code BaseEntityBlock} (that would hide the model and demand a codec). The ticker is
 * server-only. The colour is amber where the trial's is cyan, so a player can read the kind of seal at a glance. Translucency is the model's {@code render_type}.
 */
public final class LockdownBarrierBlock extends Block implements EntityBlock {
    public LockdownBarrierBlock(Properties props) { super(props); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LockdownBarrierBlockEntity(pos, state); }

    /** Server side only: a client's copy must never decide anything. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != LockdownRegistry.LOCKDOWN_BARRIER_BE.get()) return null;
        return (lvl, pos, st, be) -> { if (be instanceof LockdownBarrierBlockEntity b) b.serverTick(lvl, pos); };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    /** Like glass: the camera is never pushed by it and it never counts as an obstruction for lines of sight. */
    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, dir);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0f; }

    /** A sneaking operator who right-clicks a barrier reads what holds the room (the wave, the cells, the clock). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.hasPermissions(2)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LockdownBarrierBlockEntity be) player.displayClientMessage(be.describe(), false);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** A slow drift of embers along the sheet, so a sealed doorway is seen to be alive from across the room. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(6) != 0) return;
        double x = pos.getX() + rnd.nextDouble(), y = pos.getY() + rnd.nextDouble(), z = pos.getZ() + rnd.nextDouble();
        Fx.add(level, FxThemes.DUNGEON_SEAL, FxOld.END_ROD, x, y, z, 0.0, 0.01, 0.0);
    }
}
