package com.newuniverse.nusmp.labyrinth.trap;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Why: the hidden pitfall of the Labyrinth's pit room is ONE block with TWO states that look the same, not a floor tile plus a trap
 * block under it. {@code safe=true} is an ordinary solid floor, {@code safe=false} is the trap, and both show the Labyrinth tile
 * texture, so no resource pack, no x-ray of the model files and no palette swap can tell the route from the pits (the Spire's biome
 * palette swaps {@code labyrinth_tiles} for the local stone, which would have made a plain tile on a pitfall field stand out: the pit
 * room's whole floor is therefore this block, and the processor has nothing to swap). The block is driven by its own scheduled ticks,
 * not by a block entity: a step ({@link #stepOn}, which vanilla calls for the block under an entity's feet) schedules a five-tick crack,
 * the tick opens the floor ({@code open=true}: no collision, a framed hole), and a second tick a hundred ticks later closes it again once
 * nothing stands in the hole, so the room resets itself for the next party with nothing stored anywhere. Scheduled ticks are saved with
 * their chunk and de-duplicated by position, so a hundred steps on one tile still schedule one crack. The rules live in {@link TrapMath}.
 * Only a survival or adventure player springs it (never a mob, a creative builder or a spectator) and the whole block is inert when
 * {@code dnd.traps} is off, which turns the pit room into a plain chamber. The damage ({@code dnd.pitDamage}) is dealt once, when the
 * floor opens, to the players actually standing on it: a player who stepped off in the quarter second keeps both his health and the
 * lesson. Every block change runs inside {@link DungeonGuard#internal(Runnable)} because the Labyrinth is a guarded dimension.
 */
public final class PitfallTileBlock extends Block {
    /** A tile that never gives way: the route across the pit field and the landings. */
    public static final BooleanProperty SAFE = BooleanProperty.create("safe");
    /** The floor is gone (framed hole, no collision). */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    /** The open hole can still be aimed at (a thin rim at the top of the cell, where the model draws it), so an admin can break or pick it. */
    private static final VoxelShape HOLE_SHAPE = Block.box(0, 13, 0, 16, 16, 16);

    public PitfallTileBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(SAFE, false).setValue(OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(SAFE, OPEN); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return s.getValue(OPEN) ? HOLE_SHAPE : Shapes.block(); }

    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return s.getValue(OPEN) ? Shapes.empty() : Shapes.block(); }

    /** A hole in the floor is not a place to stand for a mob's path finder either. */
    @Override protected boolean isPathfindable(BlockState s, net.minecraft.world.level.pathfinder.PathComputationType type) { return !s.getValue(OPEN) && super.isPathfindable(s, type); }

    // ---------------------------------------------------------------- the step
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        boolean player = entity instanceof ServerPlayer;
        boolean creative = player && ((ServerPlayer) entity).isCreative();
        boolean spectator = entity.isSpectator();
        boolean springs = TrapMath.springs(player, creative, spectator, DndConfig.get(DndConfig.TRAPS));
        if (!TrapMath.stepArms(state.getValue(SAFE), state.getValue(OPEN), springs)) return;
        sl.scheduleTick(pos, this, TrapMath.CRACK_TICKS);                                   // de-duplicated by (block, pos): later steps change nothing
        sl.playSound(null, pos, SoundEvents.DEEPSLATE_TILES_BREAK, SoundSource.BLOCKS, 0.7f, 0.6f);   // the creak that tells a quick player to run
    }

    // ---------------------------------------------------------------- the ticks
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        boolean open = state.getValue(OPEN);
        TrapMath.PitAction action = TrapMath.onTick(state.getValue(SAFE), open, open && occupied(level, pos));
        switch (action) {
            case OPEN:
                DungeonGuard.internal(() -> level.setBlock(pos, state.setValue(OPEN, true), 3));
                level.playSound(null, pos, SoundEvents.DEEPSLATE_TILES_BREAK, SoundSource.BLOCKS, 1.0f, 0.4f);
                RukhFx.burst(level, new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), RukhFx.Alignment.DARK, 6);   // a puff of dark Rukh out of the hole
                dropStandingPlayers(level, pos);
                break;
            case CLOSE:
                DungeonGuard.internal(() -> level.setBlock(pos, state.setValue(OPEN, false), 3));
                level.playSound(null, pos, SoundEvents.DEEPSLATE_TILES_PLACE, SoundSource.BLOCKS, 0.6f, 0.8f);
                break;
            default:
                break;
        }
        int delay = TrapMath.nextDelay(action);
        if (delay > 0) level.scheduleTick(pos, this, delay);
    }

    /** Is any living thing inside this tile's cell (falling through it or standing in the hole)? Closing on it would bury it. */
    private static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty();
    }

    /** The damage of the fall, once, to the survival players standing on the tile when it opens (a thin slab just above the cell's top). */
    private static void dropStandingPlayers(ServerLevel level, BlockPos pos) {
        AABB above = new AABB(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.2, pos.getZ() + 1.0);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, above);
        float dmg = DndConfig.get(DndConfig.PIT_DAMAGE).floatValue();
        for (ServerPlayer p : players) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (dmg > 0f) p.hurt(level.damageSources().fall(), dmg);
            p.displayClientMessage(Component.translatable("message.nusmp.trap.pitfall").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    /** A placed or loaded OPEN tile (a structure saved mid-trap, an admin's test) closes by itself like any other. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && state.getValue(OPEN) && !state.is(old.getBlock())) level.scheduleTick(pos, this, TrapMath.OPEN_TICKS);
    }
}
