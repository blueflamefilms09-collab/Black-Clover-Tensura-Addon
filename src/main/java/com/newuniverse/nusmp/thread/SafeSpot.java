package com.newuniverse.nusmp.thread;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Rouge moves people (a step aside when she rewrites a blow, a place to land when she hops, a spot for the Dancing Doll to set
 * an ally down) and every one of those moves must never put someone inside a wall, over lava or on a fire. One helper answers
 * "may this entity stand there?" the same way for all of them, and picks the first good spot from a list of candidates.
 */
public final class SafeSpot {
    private SafeSpot() {}

    /** May 'subject' stand with its feet at 'p'? Free of blocks and liquids, and (if it was walking) with solid, cool ground under it. */
    public static boolean ok(ServerLevel level, Entity subject, Vec3 p) {
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(p))) return false;
        AABB box = subject.getBoundingBox().move(p.x - subject.getX(), p.y - subject.getY(), p.z - subject.getZ());
        if (!level.noCollision(subject, box) || level.containsAnyLiquid(box)) return false;
        if (!subject.onGround() && subject.isNoGravity()) return true;                       // something that flies needs no floor
        BlockPos below = BlockPos.containing(p.x, p.y - 0.2, p.z);
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP)) return false;
        if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(BlockTags.FIRE) || floor.is(BlockTags.CAMPFIRES) || floor.is(Blocks.CACTUS) || floor.is(Blocks.SWEET_BERRY_BUSH)) return false;
        BlockState feet = level.getBlockState(BlockPos.containing(p));
        return !feet.is(BlockTags.FIRE) && !feet.is(Blocks.COBWEB) && !feet.is(Blocks.POWDER_SNOW);
    }

    /** The first safe spot among 'candidates' (tried level, one block up, one block down); null if none is safe. */
    public static Vec3 firstOk(ServerLevel level, Entity subject, Vec3... candidates) {
        for (Vec3 c : candidates) {
            for (double dy : new double[]{0, 1, -1}) {
                Vec3 p = new Vec3(c.x, c.y + dy, c.z);
                if (ok(level, subject, p)) return p;
            }
        }
        return null;
    }

    /**
     * Candidates for a step "away" from 'threat' (null: behind where 'subject' looks): straight away at 1.5 blocks, 60 degrees either
     * side, straight sideways, then a shorter step away.
     */
    public static Vec3[] awayFrom(Entity subject, Vec3 threat, double dist) {
        Vec3 away = threat == null ? subject.getLookAngle().scale(-1) : subject.position().subtract(threat);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
        away = away.normalize();
        Vec3 base = subject.position();
        return new Vec3[]{
                base.add(away.scale(dist)),
                base.add(rot(away, 1.05).scale(dist)),
                base.add(rot(away, -1.05).scale(dist)),
                base.add(rot(away, Math.PI / 2).scale(dist)),
                base.add(rot(away, -Math.PI / 2).scale(dist)),
                base.add(away.scale(dist * 0.6))};
    }

    private static Vec3 rot(Vec3 v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    /** A ring of candidates round 'center' at 'radius' (for a place to stand next to someone). */
    public static Vec3[] ringAround(Vec3 center, double radius, double phase) {
        Vec3[] out = new Vec3[8];
        for (int i = 0; i < out.length; i++) {
            double a = phase + Math.PI * 2 * i / out.length;
            out[i] = new Vec3(center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius);
        }
        return out;
    }
}
