package com.newuniverse.nusmp.labyrinth.trial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the wall of light a Sealed Archway raises in a doorway. It is a glass-like full block: a full collision shape so nothing walks,
 * jumps or is knocked through it, {@code noOcclusion} plus {@link #propagatesSkylightDown} and a shade brightness of one so the chamber
 * behind it is not darkened or culled, and {@link #skipRendering} between two barriers so a column reads as one sheet of light instead of
 * four stacked panes with seams. Its hardness is bedrock's ({@code strength(-1, 3600000)}): a trial that could be mined through is not a
 * trial, and the Dungeon Guard holds the Labyrinth on top of that, so even creative mode and explosions leave it. {@code pushReaction
 * BLOCK} stops a piston (vanilla or another mod's) from shoving the column aside: a barrier that moved would stay in the world forever,
 * because the archway only clears the cells it remembers. It drops nothing and suffocates nobody (the doorway check already keeps players
 * out of the cells, but a mob caught in one must not die of it). Translucency is the model's {@code render_type}, not Java.
 */
public final class MagicBarrierBlock extends Block {
    public MagicBarrierBlock(Properties props) { super(props); }

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

    /** A slow drift of motes along the sheet, so a sealed doorway is seen to be alive from across the room. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(6) != 0) return;
        double x = pos.getX() + rnd.nextDouble(), y = pos.getY() + rnd.nextDouble(), z = pos.getZ() + rnd.nextDouble();
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
    }
}
