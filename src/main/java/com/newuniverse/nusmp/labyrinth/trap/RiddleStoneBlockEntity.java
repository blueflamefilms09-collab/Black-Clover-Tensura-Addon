package com.newuniverse.nusmp.labyrinth.trap;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.labyrinth.seal.GatekeeperDoorBlock;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Why: the Riddle Stone is the memory of a puzzle room, and it is a block entity because that memory must survive a reload and be shared by
 * every player in the room: the target sequence (written into the template as {@code {sequence:[0,2,1,3]}}), how far the party has got,
 * whether the door is already open, and until when a wrong order keeps the plates dead. The plates themselves are stateless blocks (a colour
 * and a lit flag), so the puzzle cannot be solved by moving a plate or by two players holding two half-sequences. All the rules are in
 * {@link PuzzleState}; this class reads and saves it ({@code sequence} as an int array, tolerant of the template writer's int list),
 * finds the plates and doors NEAR the stone by scanning a box (rotation-safe: a Jigsaw piece is rotated at placement, stored offsets would
 * point the wrong way, the same reason the Sealed Archway scans for its runes), lights the plates, hurts and speaks, and opens the
 * {@code kind=puzzle} doors with {@link GatekeeperDoorBlock#openConnected} when the last colour lands. A stone whose template carried no
 * valid sequence derives one from its position ({@link PuzzleState#defaultSequence}). Block changes run inside
 * {@link DungeonGuard#internal}. With {@code dnd.puzzles} off the stone only says the sigils are quiet; the puzzle doors then open to a
 * plain click (see {@link GatekeeperDoorBlock}), so switching puzzles off can never lock a treasure away.
 */
public final class RiddleStoneBlockEntity extends BlockEntity {
    /** How far from the stone plates and doors are searched: 12 to each side, 3 below and 5 above. */
    private static final int RANGE_XZ = 12, RANGE_DOWN = 3, RANGE_UP = 5;
    private static final ChatFormatting[] COLOUR_FORMAT = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.GOLD, ChatFormatting.GREEN};

    private int[] rawSequence;
    private PuzzleState state;
    private int savedProgress;
    private boolean savedSolved;
    private long savedLock;

    public RiddleStoneBlockEntity(BlockPos pos, BlockState blockState) { super(TrapRegistry.RIDDLE_STONE_BE.get(), pos, blockState); }

    /** The live puzzle, built on first use from the template's sequence (or the position default) and the saved progress. */
    PuzzleState state() {
        if (state == null) {
            state = new PuzzleState(PuzzleState.sanitize(rawSequence, worldPosition.asLong()));
            state.restore(savedProgress, savedSolved, savedLock);
        }
        return state;
    }

    // ---------------------------------------------------------------- reading the stone
    /** A right click on the stone: tells the player the target sequence in colour (or that the door is already open). */
    public void read(ServerPlayer player) {
        if (!DndConfig.get(DndConfig.PUZZLES)) {
            player.displayClientMessage(Component.translatable("message.nusmp.puzzle.quiet").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        PuzzleState s = state();
        if (s.solved()) {
            player.displayClientMessage(Component.translatable("message.nusmp.puzzle.done").withStyle(ChatFormatting.GREEN), true);
            return;
        }
        MutableComponent line = Component.translatable("message.nusmp.puzzle.riddle").withStyle(ChatFormatting.GRAY);
        int[] seq = s.sequence();
        for (int i = 0; i < seq.length; i++) {
            line.append(Component.literal(i == 0 ? " " : ", "));
            line.append(Component.translatable("label.nusmp.sigil." + PuzzleState.COLOUR_NAMES[seq[i]]).withStyle(COLOUR_FORMAT[seq[i]], ChatFormatting.BOLD));
        }
        player.sendSystemMessage(line);
        if (level instanceof ServerLevel sl) RukhFx.burst(sl, Vec3.atCenterOf(worldPosition).add(0, 0.8, 0), RukhFx.Alignment.LIGHT, 4);
        player.displayClientMessage(line, true);
        if (s.progress() > 0) player.displayClientMessage(Component.translatable("message.nusmp.puzzle.progress", s.progress(), s.length()).withStyle(ChatFormatting.AQUA), false);
        long wait = s.lockRemaining(player.level().getGameTime());
        if (wait > 0) player.sendSystemMessage(Component.translatable("message.nusmp.puzzle.locked", (wait + 19) / 20).withStyle(ChatFormatting.DARK_RED));
    }

    // ---------------------------------------------------------------- pressing a plate
    /** A plate of {@code colour} at {@code platePos} was clicked by {@code player}: runs the state machine and applies the answer. */
    public void press(ServerPlayer player, int colour, BlockPos platePos) {
        if (!(level instanceof ServerLevel sl)) return;
        if (!DndConfig.get(DndConfig.PUZZLES)) {
            player.displayClientMessage(Component.translatable("message.nusmp.puzzle.quiet").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        PuzzleState s = state();
        int resetTicks = DndConfig.get(DndConfig.PUZZLE_RESET_SECONDS) * 20;
        PuzzleState.Outcome o = s.press(colour, sl.getGameTime(), resetTicks);
        setChanged();
        switch (o) {
            case CORRECT:
                lightPlate(sl, platePos, true);
                RukhFx.burst(sl, Vec3.atCenterOf(platePos).add(0, 0.4, 0), RukhFx.Alignment.LIGHT, 3);
                sl.playSound(null, platePos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 0.6f + 0.25f * s.progress());
                player.displayClientMessage(Component.translatable("message.nusmp.puzzle.progress", s.progress(), s.length()).withStyle(ChatFormatting.AQUA), true);
                break;
            case SOLVED:
                lightPlate(sl, platePos, true);
                forEachPlate(sl, p -> lightPlate(sl, p, true));
                RukhFx.burst(sl, Vec3.atCenterOf(worldPosition).add(0, 0.8, 0), RukhFx.Alignment.LIGHT, 12);
                sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
                int opened = openDoors(sl);
                player.displayClientMessage(Component.translatable(opened > 0 ? "message.nusmp.puzzle.solved" : "message.nusmp.puzzle.solved_nodoor").withStyle(ChatFormatting.GREEN), true);
                break;
            case WRONG:
                forEachPlate(sl, p -> lightPlate(sl, p, false));
                RukhFx.burst(sl, Vec3.atCenterOf(platePos).add(0, 0.4, 0), RukhFx.Alignment.DARK, 6);
                sl.playSound(null, platePos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 0.8f);
                if (!player.isCreative() && !player.isSpectator()) player.hurt(sl.damageSources().magic(), 2.0f);
                player.displayClientMessage(Component.translatable("message.nusmp.puzzle.wrong", resetTicks / 20).withStyle(ChatFormatting.DARK_RED), true);
                break;
            case LOCKED:
                player.displayClientMessage(Component.translatable("message.nusmp.puzzle.locked", (s.lockRemaining(sl.getGameTime()) + 19) / 20).withStyle(ChatFormatting.DARK_RED), true);
                break;
            default:
                player.displayClientMessage(Component.translatable("message.nusmp.puzzle.done").withStyle(ChatFormatting.GREEN), true);
                break;
        }
    }

    // ---------------------------------------------------------------- the room around the stone
    private void forEachPlate(ServerLevel sl, Consumer<BlockPos> action) {
        scan(sl, st -> st.is(TrapRegistry.SIGIL_PLATE.get()), action);
    }

    private void scan(ServerLevel sl, Predicate<BlockState> test, Consumer<BlockPos> action) {
        BlockPos lo = worldPosition.offset(-RANGE_XZ, -RANGE_DOWN, -RANGE_XZ), hi = worldPosition.offset(RANGE_XZ, RANGE_UP, RANGE_XZ);
        if (!sl.hasChunksAt(lo, hi)) return;
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) if (test.test(sl.getBlockState(p))) action.accept(p.immutable());
    }

    private static void lightPlate(ServerLevel sl, BlockPos p, boolean lit) {
        BlockState st = sl.getBlockState(p);
        if (st.getBlock() instanceof SigilPlateBlock && st.getValue(SigilPlateBlock.LIT) != lit)
            DungeonGuard.internal(() -> sl.setBlock(p, st.setValue(SigilPlateBlock.LIT, lit), 3));
    }

    /** Opens every closed {@code kind=puzzle} Gatekeeper door in range (each one with its connected blocks); returns how many blocks opened. */
    private int openDoors(ServerLevel sl) {
        int[] n = {0};
        scan(sl, st -> st.getBlock() instanceof GatekeeperDoorBlock && st.getValue(GatekeeperDoorBlock.KIND) == GatekeeperDoorBlock.Kind.PUZZLE && !st.getValue(GatekeeperDoorBlock.OPEN),
                p -> n[0] += GatekeeperDoorBlock.openConnected(sl, p));
        return n[0];
    }

    // ---------------------------------------------------------------- NBT
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        PuzzleState s = state();
        tag.putIntArray("sequence", s.sequence());
        tag.putInt("progress", s.progress());
        tag.putBoolean("solved", s.solved());
        tag.putLong("lockedUntil", s.lockedUntil());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rawSequence = null;
        if (tag.contains("sequence", Tag.TAG_INT_ARRAY)) {
            rawSequence = tag.getIntArray("sequence");
        } else if (tag.contains("sequence", Tag.TAG_LIST)) {
            ListTag list = tag.getList("sequence", Tag.TAG_INT);                         // the template generator writes an int list
            rawSequence = new int[list.size()];
            for (int i = 0; i < rawSequence.length; i++) rawSequence[i] = list.getInt(i);
        }
        savedProgress = tag.getInt("progress");
        savedSolved = tag.getBoolean("solved");
        savedLock = tag.getLong("lockedUntil");
        state = null;                                                                  // rebuilt lazily from the fields above
    }
}
