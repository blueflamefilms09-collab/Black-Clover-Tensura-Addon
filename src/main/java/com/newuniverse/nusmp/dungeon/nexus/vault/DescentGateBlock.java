package com.newuniverse.nusmp.dungeon.nexus.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Descent Gate (docs/living_dungeon.md, "Navigation"): a glowing spatial gate. Stepping into it, or right-clicking it, takes a player
 * down to the next floor of the Dungeon at the same x and z ({@link GateTravel}); the gate at the other end ({@code rising}) takes them
 * back up. It cannot be broken or pushed, and it keeps the {@link GateIndex} (which the Ki-Resonance Lantern reads) up to date by itself.
 */
public class DescentGateBlock extends Block {
    /** A gate that leads up, to the gate it was reached from. */
    public static final BooleanProperty RISING = BooleanProperty.create("rising");

    public DescentGateBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(RISING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(RISING); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel sl) || !(entity instanceof ServerPlayer p)) return;
        GateTravel.use(p, sl, pos, state.getValue(RISING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) GateTravel.use(p, sl, pos, state.getValue(RISING));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level instanceof ServerLevel sl && !old.is(this)) GateIndex.noted(sl, pos, state.getValue(RISING));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !newState.is(this)) GateIndex.forgotten(sl, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    /** A slow swirl: motes wind in towards the gate (down) or rise out of it (up). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        boolean rising = state.getValue(RISING);
        double t = (level.getGameTime() % 60) / 60.0 * Math.PI * 2.0;
        for (int i = 0; i < 3; i++) {
            double a = t + i * 2.0944 + rnd.nextDouble() * 0.3, r = 0.62 - (i * 0.05);
            double x = pos.getX() + 0.5 + Math.cos(a) * r, z = pos.getZ() + 0.5 + Math.sin(a) * r;
            double y = pos.getY() + (rising ? 0.1 + rnd.nextDouble() * 0.5 : 0.9 - rnd.nextDouble() * 0.5);
            level.addParticle(rising ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL, x, y, z, -Math.cos(a) * 0.12, rising ? 0.08 : -0.08, -Math.sin(a) * 0.12);
        }
        if (rnd.nextInt(3) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + 0.2 + rnd.nextDouble() * 0.6, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        if (rnd.nextInt(90) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.35f, 0.7f + rnd.nextFloat() * 0.4f, false);
    }
}
