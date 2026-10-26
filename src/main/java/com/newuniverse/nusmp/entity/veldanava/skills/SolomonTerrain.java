package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Why: the 72 Pillars destroy terrain, and destroying terrain is the easiest way to lag or grief a server, so every block it removes goes through this one class and its limits: only a chunk that is
 * already loaded is looked at (never loaded for the cast), at most {@link SolomonRules#TERRAIN_PER_PILLAR} blocks per pillar and {@link SolomonRules#TERRAIN_MAX_BLOCKS} per cast (the caller keeps
 * the count), only within {@link SolomonRules#CRATER_RADIUS} of the ground point, never a block that cannot be broken (bedrock, a block of hardness over {@link SolomonRules#TERRAIN_MAX_HARDNESS}),
 * a block that holds an inventory or any block entity, a fluid, and nothing at all in the Dungeon (its stone is never broken, {@link DungeonGuard}) or when the server's {@code mobGriefing} rule or the config's {@code solomonTerrainDamage} is off. The blocks are replaced
 * by air in one write each with no drops, no break particles and no sound packet of their own (the effect is the client's picture), so one pillar costs a handful of block updates. Server thread only.
 */
public final class SolomonTerrain {
    private SolomonTerrain() {}

    /** True when the server allows the terrain to be broken at all. */
    public static boolean allowed(ServerLevel level) { return JudgmentConfig.solomonTerrain() && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !DungeonGuard.on(level); }

    /** The block a crater would be centred on: the top solid block under {@code (x, z)} in a loaded chunk, or null. */
    public static BlockPos surface(ServerLevel level, double x, double z) {
        BlockPos probe = BlockPos.containing(x, level.getMinBuildHeight() + 1, z);
        if (!level.isLoaded(probe)) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe.getX(), probe.getZ()) - 1;
        return y <= level.getMinBuildHeight() ? null : new BlockPos(probe.getX(), y, probe.getZ());
    }

    /**
     * Removes up to {@code budget} blocks round {@code centre}, the nearest first (the crater is a bowl: only blocks in the sphere that are exposed or just under the surface). Returns how many were removed.
     */
    public static int crater(ServerLevel level, BlockPos centre, int budget) {
        if (budget <= 0 || centre == null || !allowed(level)) return 0;
        int r = (int) Math.ceil(SolomonRules.CRATER_RADIUS);
        double r2 = SolomonRules.CRATER_RADIUS * SolomonRules.CRATER_RADIUS;
        int removed = 0;
        // layers from the top down, so the bowl is dug from the surface
        for (int dy = 0; dy >= -r && removed < budget; dy--) {
            for (int dx = -r; dx <= r && removed < budget; dx++) {
                for (int dz = -r; dz <= r && removed < budget; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (!level.isLoaded(pos) || !breakable(level, pos)) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    removed++;
                }
            }
        }
        return removed;
    }

    /** True when the block may be removed by a pillar. */
    public static boolean breakable(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir() || !s.getFluidState().isEmpty() || level.getBlockEntity(pos) != null) return false;
        float hardness = s.getDestroySpeed(level, pos);
        if (hardness < 0f || hardness > SolomonRules.TERRAIN_MAX_HARDNESS) return false;
        return true;
    }
}
