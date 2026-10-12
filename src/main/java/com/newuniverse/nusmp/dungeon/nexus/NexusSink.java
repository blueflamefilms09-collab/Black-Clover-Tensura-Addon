package com.newuniverse.nusmp.dungeon.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Writes plan cells into the Dungeon. Bedrock is never replaced (nor written over by anything but bedrock), so a plan cannot open the
 * floor or the roof of the world. Every write is a quiet one (clients are told, neighbours and shapes are not updated), so a hundred
 * thousand blocks cannot cascade. Run it inside {@code DungeonGuard.internal}: the builder does, and a stage that calls it from
 * elsewhere must too.
 */
public final class NexusSink implements NexusCursor.Sink {
    /** Send the change to clients, but do not update shapes or neighbours. */
    public static final int FLAGS = 2 | 16;

    private final ServerLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private NexusOp last;
    private BlockState lastState;

    public NexusSink(ServerLevel level) { this.level = level; }

    @Override
    public boolean place(NexusOp op, int x, int y, int z) {
        pos.set(x, y, z);
        BlockState cur = level.getBlockState(pos);
        if (op.kind() == NexusOp.Kind.CARVE) {
            if (cur.isAir() || cur.is(Blocks.BEDROCK)) return false;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            return true;
        }
        if (op != last) {
            last = op;
            lastState = NexusStates.of(op.block(), op.props());
        }
        if (cur.is(Blocks.BEDROCK) && !lastState.is(Blocks.BEDROCK)) return false;
        if (op.kind() == NexusOp.Kind.FILL_AIR && !cur.isAir()) return false;
        if (cur == lastState || (lastState.isAir() && cur.isAir())) return false;
        level.setBlock(pos, lastState, FLAGS);
        return true;
    }
}
