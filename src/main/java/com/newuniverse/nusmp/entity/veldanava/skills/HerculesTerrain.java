package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Hercules' tremor and throw break ground, and breaking ground is the easiest way to lag or grief a server, so every block they remove goes through this one class and its limits: only a chunk that is
 * already loaded is looked at (never loaded for the cast), at most the budget the caller passes (a tremor {@link HerculesRules#TREMOR_MAX_BLOCKS}, a landing {@link HerculesRules#THROW_CRATER_BLOCKS}, a lifted
 * chunk {@link HerculesRules#CHUNK_BLOCKS}), only within the radius of a flat bowl, never a block that cannot be broken (bedrock, a block of hardness over the limit of {@link SolomonTerrain#breakable}), a
 * block that holds an inventory or any block entity, a fluid, and nothing at all in the Dungeon (its stone is never broken, {@link DungeonGuard}) or when the server's {@code mobGriefing} rule or the config's
 * {@code herculesTerrainDamage} is off. The blocks are replaced by air in one write each with no drops, no break particles and no sound packet of their own (the effect is the client's picture). Server thread only.
 */
public final class HerculesTerrain {
    private HerculesTerrain() {}

    /** True when the server allows the ground to be broken at all. */
    public static boolean allowed(ServerLevel level) { return JudgmentConfig.herculesTerrain() && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !DungeonGuard.on(level); }

    /**
     * A flat bowl round {@code centre}: for each column within {@code radius} the top {@code depth} blocks at the centre, one block at the rim, the deepest first at the middle. At most {@code budget} blocks.
     * Returns how many were removed.
     */
    public static int bowl(ServerLevel level, BlockPos centre, double radius, int depth, int budget) {
        if (budget <= 0 || centre == null || radius <= 0.0 || depth <= 0 || !allowed(level)) return 0;
        int r = (int) Math.ceil(radius);
        double r2 = radius * radius;
        int removed = 0;
        for (int dx = -r; dx <= r && removed < budget; dx++) {
            for (int dz = -r; dz <= r && removed < budget; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > r2) continue;
                int layers = Math.max(1, (int) Math.round(depth * (1.0 - Math.sqrt(d2) / radius)));
                for (int dy = 0; dy > -layers && removed < budget; dy--) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (!level.isLoaded(pos) || !SolomonTerrain.breakable(level, pos)) continue;
                    remove(level, pos);
                    removed++;
                }
            }
        }
        return removed;
    }

    /** The one place this class removes a block: replaced by air in a single write, no drops, no break particles (the effect is the client's picture). */
    private static void remove(ServerLevel level, BlockPos pos) { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); }

    /** The block a bowl is centred on for a creature standing at {@code feet}: the one under its feet. */
    public static BlockPos under(Vec3 feet) { return BlockPos.containing(feet.x, feet.y - 0.5, feet.z); }

    /**
     * Lifts a chunk of ground out of the block the owner looks at (within {@link HerculesRules#CHUNK_REACH} blocks): up to {@link HerculesRules#CHUNK_BLOCKS} blocks round the first one hit that can be broken
     * are removed, nearest first. Returns the number lifted (0 = nothing to throw, nothing changed).
     */
    public static int lift(ServerPlayer p, ServerLevel level) {
        if (!allowed(level)) return 0;
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1f).scale(HerculesRules.CHUNK_REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK || !level.isLoaded(hit.getBlockPos()) || !SolomonTerrain.breakable(level, hit.getBlockPos())) return 0;
        BlockPos c = hit.getBlockPos();
        int removed = 0;
        for (int ring = 0; ring <= 2 && removed < HerculesRules.CHUNK_BLOCKS; ring++) {
            for (int dx = -ring; dx <= ring && removed < HerculesRules.CHUNK_BLOCKS; dx++) {
                for (int dy = -ring; dy <= ring && removed < HerculesRules.CHUNK_BLOCKS; dy++) {
                    for (int dz = -ring; dz <= ring && removed < HerculesRules.CHUNK_BLOCKS; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != ring) continue;      // only this ring's shell: each block once
                        BlockPos pos = c.offset(dx, dy, dz);
                        if (!level.isLoaded(pos) || !SolomonTerrain.breakable(level, pos)) continue;
                        remove(level, pos);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }
}
