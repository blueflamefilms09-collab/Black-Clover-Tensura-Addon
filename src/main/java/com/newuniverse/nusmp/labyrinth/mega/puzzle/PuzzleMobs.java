package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MobGroups;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.PuzzleBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.SplittableRandom;

/**
 * Why: the only monsters a puzzle ever lets out are the mimic's little brood and the penalty "ambush" (a cost, never a siege), and they must be dungeon monsters like any other: the roster is
 * the mechanics library's own data-driven one ({@link MobGroups}, checked for existence so a missing mod only skips an entry), they are stamped with the Dungeon Master's tag (so the
 * economy and the health buffer apply) and the puzzle's own tag (so the puzzle can find and remove them), and they are scaled by depth with the same permanent modifiers. A cell must be
 * floored, free and dry; if none is found nothing spawns (a cost that cannot be paid is not charged).
 */
final class PuzzleMobs {
    private PuzzleMobs() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final int[][] RING = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}};
    static final String TAG = "nusmp_puzzle";

    private static boolean fit(ServerLevel level, BlockPos p) {
        if (!level.hasChunksAt(p.offset(-1, -1, -1), p.offset(1, 3, 1))) return false;
        if (!PuzzleBridge.allowedAt(level, p)) return false;
        BlockState floor = level.getBlockState(p.below());
        if (floor.getCollisionShape(level, p.below()).isEmpty()) return false;
        if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) return false;
        return level.getFluidState(p).isEmpty();
    }

    /** One monster of the group near the cell, tagged with the puzzle's tag and {@code instanceTag}; null when nothing fits. */
    static Mob spawn(ServerLevel level, String group, int floor, BlockPos near, SplittableRandom rng, String instanceTag, double hp) {
        MobGroups.Entry e = MobGroups.pick(group, PuzzleBridge.tier(floor), rng, x -> x.variant().isEmpty() && BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(x.id())));
        if (e == null) return null;
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(e.id()));
        if (type.isEmpty()) return null;
        Mob mob = null;
        try {
            Entity made = type.get().create(level);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            mob = m;
            BlockPos at = null;
            for (int[] o : RING) {
                BlockPos q = near.offset(o[0], 0, o[1]);
                if (!fit(level, q)) continue;
                mob.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, rng.nextFloat() * 360f, 0f);
                if (level.noCollision(mob)) { at = q; break; }
            }
            if (at == null) { mob.discard(); return null; }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            mob.addTag(MegaDungeon.DM_TAG);
            mob.addTag(TAG);
            mob.addTag(instanceTag);
            double s = PuzzleBridge.depth(floor);
            PuzzleBridge.scaleMob(mob, Math.max(0.0, hp - 1.0) + 0.45 * s, 0.2 * s);
            if (!level.addFreshEntity(mob)) { mob.discard(); return null; }
            return mob;
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] puzzle could not spawn {}: {}", e.id(), ex.toString());
            if (mob != null) mob.discard();
            return null;
        }
    }
}
