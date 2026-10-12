package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.dungeon.RadiationShield;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.mana.ManaLaw;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The mana-locked gate before a Treasury (docs/living_dungeon.md section 7). Closed it is a solid wall of glowing sigils; it opens, and
 * stays passable, while a player with an active Mana Zone or with Magic Sense stands within {@link #REACH} blocks, and shuts again about five
 * seconds after the last of them is gone. A scheduled tick polls every half second (a random tick restarts the chain if it is ever lost).
 * It never shuts on someone standing in the doorway, so it cannot trap anyone; once the central relic of its Nexus has been taken (the Treasury
 * Collapse has begun) it opens to anyone who comes near, so nobody is locked in a falling room; like the rest of the Dungeon it cannot be broken.
 */
public class ManaGateBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Ticks between two looks while shut, the time an open gate waits before looking again, and how near a player must be (blocks). */
    static final int POLL = 10, STAY = 100, REACH = 3, RETRY = 20, HINT_EVERY = 200;
    private static final String K_HINT = "nusmp_relic_gate_hint";

    public ManaGateBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPEN); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) level.scheduleTick(pos, this, POLL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { level.scheduleTick(pos, this, POLL); }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<Player> near = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(REACH), p -> !p.isSpectator());
        boolean worthy = false;
        for (Player p : near) if (worthy(p)) worthy = true;
        if (!worthy && !near.isEmpty() && robbed(level, pos)) worthy = true;      // the Nexus is coming down: nobody is left locked in a falling room
        if (state.getValue(OPEN)) {
            if (worthy) {
                level.scheduleTick(pos, this, STAY);                               // still being asked
            } else if (!level.getEntitiesOfClass(Entity.class, new AABB(pos), e -> !e.isSpectator()).isEmpty()) {
                level.scheduleTick(pos, this, RETRY);                              // someone stands in the doorway: never shut on them
            } else {
                level.setBlock(pos, state.setValue(OPEN, false), 3);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 0.6f);
                level.scheduleTick(pos, this, POLL);
            }
            return;
        }
        if (worthy) {
            level.setBlock(pos, state.setValue(OPEN, true), 3);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4f, 0.7f);
            level.scheduleTick(pos, this, STAY);
            return;
        }
        long now = level.getGameTime();
        for (Player p : near) {
            CompoundTag d = p.getPersistentData();
            long last = d.getLong(K_HINT);
            if (last != 0L && now >= last && now - last < HINT_EVERY) continue;
            d.putLong(K_HINT, now);
            p.displayClientMessage(Component.literal("The sigils stay dark. They answer only to an open Mana Zone, or to Magic Sense.").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        level.scheduleTick(pos, this, POLL);
    }

    private static boolean worthy(Player p) { return ManaLaw.zoneActive(p) || RadiationShield.hasMagicSense(p); }

    /** Has the Nexus this gate belongs to lost its central relic? Then the Treasury Collapse is under way, and the gate lets anyone out (or in). */
    private static boolean robbed(ServerLevel level, BlockPos pos) {
        try {
            return NexusZones.centreRelicTaken(level, pos);
        } catch (RuntimeException | LinkageError unavailable) {
            return false;
        }
    }
}
