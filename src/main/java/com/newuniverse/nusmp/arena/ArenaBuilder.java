package com.newuniverse.nusmp.arena;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Why: the arena is a flat void until somebody arrives, and its courtyard (the floor, the dais, the pillars, the seat, the wall) has to
 * exist before the first traveller is set down. The first arrival builds it here, once, from the formula in {@link ArenaLayout} (the arena save's
 * {@code built} flag remembers it): procedural building in code needs no structure file to ship and no template to keep in step with the formula,
 * and the pure layout is unit tested. The blocks are placed inside {@link DungeonGuard#internal(Runnable)}, because the guard that makes the arena
 * unbreakable would otherwise refuse the builder's own writes in a guarded dimension; the chunks are loaded first, so nothing is placed into a chunk that is not
 * there. {@link #repair} puts back anything that is missing (an operator's {@code /fill}, a bug), which is cheap and safe to call on every arrival.
 * Vanilla blocks only (blackstone, deepslate, quartz and gold): no texture is needed and nothing here can be missing from a resource pack.
 */
public final class ArenaBuilder {
    private ArenaBuilder() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** What block a part of the layout is. The Return Sigil is the mod's own block. */
    static BlockState stateOf(ArenaLayout.Part part) {
        Block b = switch (part) {
            case FOUNDATION -> Blocks.BLACKSTONE;
            case FLOOR_A -> Blocks.POLISHED_BLACKSTONE_BRICKS;
            case FLOOR_B -> Blocks.POLISHED_BLACKSTONE;
            case FLOOR_GOLD -> Blocks.GOLD_BLOCK;
            case DAIS_BASE -> Blocks.CHISELED_POLISHED_BLACKSTONE;
            case DAIS_TOP -> Blocks.QUARTZ_BLOCK;
            case DAIS_EDGE -> Blocks.GOLD_BLOCK;
            case DAIS_RUNE -> Blocks.CRYING_OBSIDIAN;
            case SEAT_LOW, SEAT_HIGH -> Blocks.DEEPSLATE_TILES;
            case WALL, MERLON -> Blocks.POLISHED_BLACKSTONE_BRICKS;
            case LANTERN -> Blocks.SOUL_LANTERN;
            case PILLAR -> Blocks.QUARTZ_PILLAR;
            case PILLAR_BAND -> Blocks.GILDED_BLACKSTONE;
            case PILLAR_LIGHT -> Blocks.SEA_LANTERN;
            case SIGIL -> ArenaSystem.RETURN_SIGIL.get();
        };
        return b.defaultBlockState();
    }

    /** Builds the courtyard if the arena save says it is not built yet. Returns whether it was built now. */
    public static boolean ensureBuilt(ServerLevel level, ArenaSave save) {
        if (save.built()) return false;
        place(level, false);
        save.built(true);
        return true;
    }

    /** Puts back every block of the layout that is not what it should be. Returns how many were set. */
    public static int repair(ServerLevel level) { return place(level, true); }

    private static int place(ServerLevel level, boolean onlyMissing) {
        int range = (ArenaLayout.RADIUS >> 4) + 1;
        for (int cx = -range; cx <= range; cx++) for (int cz = -range; cz <= range; cz++) level.getChunk(cx, cz);       // generate and load first
        int[] set = {0};
        DungeonGuard.internal(() -> {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = -ArenaLayout.RADIUS - 1; dx <= ArenaLayout.RADIUS + 1; dx++) {
                for (int dz = -ArenaLayout.RADIUS - 1; dz <= ArenaLayout.RADIUS + 1; dz++) {
                    for (ArenaLayout.Cell c : ArenaLayout.column(dx, dz)) {
                        pos.set(dx, ArenaLayout.FLOOR_Y + c.dy(), dz);
                        BlockState want = stateOf(c.part());
                        if (onlyMissing && level.getBlockState(pos).is(want.getBlock())) continue;
                        level.setBlock(pos, want, 2);
                        set[0]++;
                    }
                }
            }
        });
        LOG.info("[nusmp] The Zogratis arena courtyard {}: {} blocks set", onlyMissing ? "repaired" : "built", set[0]);
        return set[0];
    }
}
