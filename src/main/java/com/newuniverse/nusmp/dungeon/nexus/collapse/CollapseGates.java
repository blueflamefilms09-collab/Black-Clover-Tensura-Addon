package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.DungeonBlocks;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Root Gates the collapse opens at its exits (docs/living_dungeon.md section 8): the Dungeon's own lit block that takes whoever walks
 * into it back to where they came down from. A gate is saved as {x, y, z, how}: how is 1 if the collapse placed it (and takes it away
 * again at the end), 0 if a Root Gate already stood there (left alone), -1 if no place was found (given up); an empty array means not
 * tried yet because the chunk was not loaded.
 */
final class CollapseGates {
    private CollapseGates() {}

    static final int PLACED = 1, ADOPTED = 0, NONE = -1;
    /** Where to try, nearest first, round an exit: {dx, dy, dz}. */
    private static final int[][] TRIES = {
            {0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}, {-1, 0, -1}, {1, 0, -1}, {-1, 0, 1},
            {0, 1, 0}, {0, -1, 0}, {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2}};

    /** Tries to set a Root Gate at (or beside) the exit. An empty result means "try again later" (the chunk is not loaded). Run it inside {@code DungeonGuard.internal}. */
    static int[] place(ServerLevel level, int[] exit) {
        if (!CollapseGround.loaded(level, exit[0], exit[2])) return new int[0];
        BlockState gate = DungeonBlocks.DUNGEON_GATE.get().defaultBlockState();
        for (int[] d : TRIES) {
            int x = exit[0] + d[0], y = exit[1] + d[1], z = exit[2] + d[2];
            if (!CollapseGround.loaded(level, x, z)) continue;
            BlockPos at = new BlockPos(x, y, z);
            if (level.getBlockState(at).is(DungeonBlocks.DUNGEON_GATE.get())) return new int[]{x, y, z, ADOPTED};
            if (!CollapseGround.standable(level, x, y, z)) continue;
            level.setBlock(at, gate, 3);
            return new int[]{x, y, z, PLACED};
        }
        return new int[]{exit[0], exit[1], exit[2], NONE};
    }

    /** Takes away the gates this collapse placed. True when none is left to take (all gone, or their chunks are loaded and show nothing of ours). */
    static boolean remove(ServerLevel level, List<int[]> gates) {
        boolean all = true;
        for (int[] g : gates) {
            if (g.length < 4 || g[3] != PLACED) continue;
            if (!CollapseGround.loaded(level, g[0], g[2])) {
                all = false;
                continue;
            }
            BlockPos at = new BlockPos(g[0], g[1], g[2]);
            if (level.getBlockState(at).is(DungeonBlocks.DUNGEON_GATE.get())) DungeonGuard.internal(() -> level.setBlock(at, Blocks.AIR.defaultBlockState(), 3));
            g[3] = NONE;                                                                // taken away: never touched again
        }
        return all;
    }

    /** The positions of the gates that stand (placed or found) as {x, y, z}. */
    static List<int[]> standing(List<int[]> gates) {
        List<int[]> out = new java.util.ArrayList<>();
        for (int[] g : gates) if (g.length >= 4 && (g[3] == PLACED || g[3] == ADOPTED)) out.add(new int[]{g[0], g[1], g[2]});
        return out;
    }
}
