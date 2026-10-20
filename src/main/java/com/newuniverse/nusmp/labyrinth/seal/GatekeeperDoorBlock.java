package com.newuniverse.nusmp.labyrinth.seal;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the Labyrinth's sealed doors are a BLOCKSTATE MACHINE and not a block entity. A door is a column pair of ordinary blocks with three
 * properties: {@code kind} (warden: opened by a Shadow Key; boss: opened by the Peak Altar when the floor's master dies), {@code open} and
 * {@code half} (which part of the door texture this block shows). No block entity means no NBT to serialise, nothing to tick, nothing that
 * breaks when Jigsaw rotates the template (there is no facing property at all: the door face is drawn on all four sides, so a doorway
 * reads the same from both approaches whatever rotation the piece got), and the KIND is written into the template as a block property, so
 * the structure builder decides which gate is which without a single line of Java. Opening is a flood over the neighbouring blocks of the
 * same kind ({@link DoorFlood}): the whole doorway opens from one click as one door, and the halves are never linked to each other, so a
 * column of 2, 3 or 4 blocks all works. The block is unbreakable ({@code strength(-1, 3600000)}) and pushes nothing, so the only way past
 * is the key, and the mod's own opening runs inside {@link DungeonGuard#internal} because the Labyrinth is a guarded dimension.
 */
public class GatekeeperDoorBlock extends Block {
    /** Which seal: the Warden's gate before the Boss Peak (a key), or the Vault door (the altar's boss). */
    public enum Kind implements StringRepresentable {
        WARDEN("warden"), BOSS("boss"),
        /** 1.36.0 (appended): a puzzle door, opened by the Riddle Stone of its room and blind to the Shadow Key. */
        PUZZLE("puzzle"),
        /** 1.54.0 (appended): the sector door of the Mega-Dungeon, opened only by the Mana Core of the sector that holds it and blind to the Shadow Key (mega.guard.SectorDoor). */
        SECTOR("sector");

        private final String name;

        Kind(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /** The open doorway: four thin corner posts you can walk between (the collision shape is empty). */
    private static final VoxelShape FRAME = Shapes.or(Block.box(0, 0, 0, 2, 16, 2), Block.box(14, 0, 0, 16, 16, 2),
            Block.box(0, 0, 14, 2, 16, 16), Block.box(14, 0, 14, 16, 16, 16));

    public GatekeeperDoorBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.WARDEN).setValue(OPEN, false).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(KIND, OPEN, HALF); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return s.getValue(OPEN) ? FRAME : Shapes.block(); }

    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return s.getValue(OPEN) ? Shapes.empty() : Shapes.block(); }

    @Override protected boolean isPathfindable(BlockState s, PathComputationType type) { return s.getValue(OPEN); }

    // ---------------------------------------------------------------- placing by hand (creative testing; templates place the pair themselves)
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(ctx)) return defaultBlockState();
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    // ---------------------------------------------------------------- the key
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(KIND) == Kind.SECTOR) return com.newuniverse.nusmp.labyrinth.mega.guard.SectorDoor.useItem(stack, state, level, pos, player);   // 1.54.0: a Mana Core, nothing else
        if (!(stack.getItem() instanceof ShadowKeyItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (state.getValue(OPEN)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(KIND) == Kind.PUZZLE) {                                         // the Shadow Key does nothing here (1.36.0)
            puzzleClick(state, level, pos, player);
            return ItemInteractionResult.CONSUME;
        }
        if (state.getValue(KIND) != Kind.WARDEN) {
            tell(player, "message.nusmp.gatekeeper.boss_sealed");
            return ItemInteractionResult.CONSUME;
        }
        if (!player.isCreative()) stack.shrink(1);                                        // one key per run; creative keeps testing freely
        int opened = openConnected(level, pos);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.6f);
        if (opened > 0 && player instanceof net.minecraft.server.level.ServerPlayer sp) com.newuniverse.nusmp.quests.NusmpEvents.fire(sp, "labyrinth.seal_opened", "warden");
        if (level instanceof ServerLevel sl) {
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 24, 0.6, 0.9, 0.6, 0.02);
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.5, 0.8, 0.5, 0.03);
        }
        tell(player, "message.nusmp.gatekeeper.warden_open");
        return opened > 0 ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
    }

    /** An empty hand, or anything that is not a key: the door says what it wants. Sneaking lets the click through so blocks can be placed against it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(OPEN) || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (state.getValue(KIND) == Kind.PUZZLE) puzzleClick(state, level, pos, player);
            else if (state.getValue(KIND) == Kind.SECTOR) tell(player, "message.nusmp.gatekeeper.sector_sealed");
            else tell(player, state.getValue(KIND) == Kind.WARDEN ? "message.nusmp.gatekeeper.warden_sealed" : "message.nusmp.gatekeeper.boss_sealed");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * A click on a puzzle door (1.36.0): it says it answers to the sigils, unless the server switched puzzles off ({@code dnd.puzzles}), in which
     * case it simply opens, so a server that disabled the puzzle can never lock its treasure away. Returns whether it opened.
     */
    private static boolean puzzleClick(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return false;
        if (!com.newuniverse.nusmp.labyrinth.dnd.DndConfig.get(com.newuniverse.nusmp.labyrinth.dnd.DndConfig.PUZZLES)) {
            boolean opened = openConnected(level, pos) > 0;
            if (opened) level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.8f);
            return opened;
        }
        tell(player, "message.nusmp.gatekeeper.puzzle_sealed");
        return false;
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------------------------------------------------------------- opening
    /**
     * Opens every closed Gatekeeper block of the same kind connected (by faces) to {@code start}, at most {@link DoorFlood#LIMIT}, inside the
     * guard's internal span. Returns how many blocks changed. Safe to call on any position: a non-door start opens nothing.
     */
    public static int openConnected(Level level, BlockPos start) {
        BlockState s0 = level.getBlockState(start);
        if (!(s0.getBlock() instanceof GatekeeperDoorBlock door) || s0.getValue(OPEN)) return 0;
        Kind kind = s0.getValue(KIND);
        List<Long> keys = DoorFlood.flood(DoorFlood.pack(start.getX(), start.getY(), start.getZ()), key -> {
            BlockState s = level.getBlockState(new BlockPos(DoorFlood.x(key), DoorFlood.y(key), DoorFlood.z(key)));
            return s.is(door) && s.getValue(KIND) == kind && !s.getValue(OPEN);
        }, DoorFlood.LIMIT);
        DungeonGuard.internal(() -> {
            for (long key : keys) {
                BlockPos p = new BlockPos(DoorFlood.x(key), DoorFlood.y(key), DoorFlood.z(key));
                level.setBlock(p, level.getBlockState(p).setValue(OPEN, true), 3);
            }
        });
        return keys.size();
    }

    /** Opens this one block if it is a closed door of {@code kind} (the altar's scan; the caller holds the guard's internal span). */
    public static boolean openOne(Level level, BlockPos pos, Kind kind) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof GatekeeperDoorBlock) || s.getValue(OPEN) || s.getValue(KIND) != kind) return false;
        level.setBlock(pos, s.setValue(OPEN, true), 3);
        return true;
    }
}
