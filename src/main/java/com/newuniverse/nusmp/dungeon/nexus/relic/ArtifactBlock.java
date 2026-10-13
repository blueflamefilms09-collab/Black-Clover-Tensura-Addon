package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The pedestal a Treasury keeps a relic on (docs/living_dungeon.md sections 7 and 8). The block state says which relic it holds
 * ({@link #RELIC}), whether it is the central one ({@link #CENTRAL}) and whether it has been taken ({@link #TAKEN}); there is no block
 * entity. Use it to take the relic: you get the item, unattuned. Taking a central relic robs the Nexus, and a {@link DungeonCollapseEvent}
 * is posted for the Treasury Collapse. Like the rest of the Dungeon it cannot be broken; a glowing relic floats above it until it is taken.
 */
public class ArtifactBlock extends Block {
    public static final EnumProperty<RelicSlot> RELIC = EnumProperty.create("relic", RelicSlot.class);
    public static final BooleanProperty CENTRAL = BooleanProperty.create("central");
    public static final BooleanProperty TAKEN = BooleanProperty.create("taken");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public ArtifactBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(RELIC, RelicSlot.EMPTY).setValue(CENTRAL, false).setValue(TAKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(RELIC, CENTRAL, TAKEN); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    /** A pedestal holding this relic, not taken. */
    public BlockState holding(Relic relic, boolean central) {
        return defaultBlockState().setValue(RELIC, RelicSlot.of(relic)).setValue(CENTRAL, central).setValue(TAKEN, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Relic relic = state.getValue(RELIC).relic();
        if (relic == null || state.getValue(TAKEN)) {
            player.displayClientMessage(Component.literal("The pedestal is bare.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        level.setBlock(pos, state.setValue(TAKEN, true), 3);                      // marked first: a second click can never take it twice
        ItemStack stack = relic.stack();
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.6f, 0.6f);
        player.displayClientMessage(Component.literal("You take " + relic.label + ". It is unattuned: hold it and sneak for 10 s.").withStyle(ChatFormatting.GOLD), true);
        if (state.getValue(CENTRAL) && level instanceof ServerLevel sl && LivingDungeon.in(sl)) {
            ServerPlayer cause = player instanceof ServerPlayer sp ? sp : null;
            Relics.markCentreRelicTaken(sl, pos);
            NeoForge.EVENT_BUS.post(new DungeonCollapseEvent(sl, pos, Relics.zoneAt(sl, pos.getX(), pos.getZ()), cause, false));
        }
        return InteractionResult.CONSUME;
    }

    /** Motes of the relic's own light drift up from a pedestal that still holds one. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(TAKEN) || state.getValue(RELIC) == RelicSlot.EMPTY || random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.0 + random.nextDouble() * 0.4, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                0.0, 0.01, 0.0);
    }
}
