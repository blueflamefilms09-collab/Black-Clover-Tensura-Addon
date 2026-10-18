package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the Spear dashes, vaults, blinks and rewinds, the Bow springs back, and every one of those puts the wielder somewhere they did not walk to. The
 * rules are the owner's: only to a place with room for the body, something to stand on and nothing harmful in it (no lava or other liquid, fire, magma,
 * cactus, berry bushes, wither roses, powder snow, cobwebs or portals), never into an unloaded chunk (nothing here ever loads one), never out of the world.
 * This is the game side of {@link SafeSpot}: it answers the probe's one question against the real world. A person can stand on more than whole blocks
 * (a slab, a path, a carpet, a layer of snow, soul sand, a fence), so the height they would rest at is worked out from the collision shapes of the block
 * under their feet and of their own cell, not assumed to be a whole block. It also moves the wielder in a way that survives a stand-in player with no
 * network connection (the self-tests use one) and resets the fall so a dash over a gap does not end in damage.
 */
public final class Moves {
    private Moves() {}

    /** Answers {@link SafeSpot.Probe} for this player in this level. */
    public static SafeSpot.Probe probe(ServerLevel level, ServerPlayer p) { return (x, y, z) -> standable(level, p, x, y, z); }

    /**
     * The height at which a person standing in the cell of the feet block (x, y, z) rests: the top of the collision under their feet or of their own cell,
     * whichever is higher; NaN when there is nothing to stand on within a step (half a block down, 0.6 up) or the chunk is not loaded.
     */
    public static double standingY(ServerLevel level, int x, int y, int z) {
        BlockPos feet = BlockPos.containing(x, y, z), below = feet.below();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(below)) return Double.NaN;
        VoxelShape under = level.getBlockState(below).getCollisionShape(level, below), here = level.getBlockState(feet).getCollisionShape(level, feet);
        double top = Double.NaN;
        if (!under.isEmpty()) top = below.getY() + under.bounds().maxY;
        if (!here.isEmpty()) {
            double h = feet.getY() + here.bounds().maxY;
            top = Double.isNaN(top) ? h : Math.max(top, h);
        }
        return Double.isNaN(top) || top < y - 0.5 || top > y + 0.6 ? Double.NaN : top;
    }

    /** True when {@code p} can stand in the cell of the feet block (x, y, z): loaded, something to stand on, room for the body, nothing harmful. */
    public static boolean standable(ServerLevel level, ServerPlayer p, int x, int y, int z) {
        if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) return false;
        double top = standingY(level, x, y, z);
        if (Double.isNaN(top)) return false;
        BlockPos feet = BlockPos.containing(x, y, z), head = feet.above();
        if (!level.hasChunkAt(head)) return false;
        if (hazard(level.getBlockState(feet.below())) || hazard(level.getBlockState(feet)) || hazard(level.getBlockState(head))) return false;
        AABB box = p.getBoundingBox().move(x + 0.5 - p.getX(), top - p.getY(), z + 0.5 - p.getZ());
        return level.noCollision(p, box) && !level.containsAnyLiquid(box);
    }

    /** Blocks that hurt or hold whoever stands in or on them. */
    public static boolean hazard(BlockState s) {
        return s.is(BlockTags.FIRE) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.COBWEB)
                || s.is(Blocks.END_PORTAL) || s.is(Blocks.NETHER_PORTAL);
    }

    /**
     * The feet block y of the first cell, looking down from the one at (x, y, z) and at most {@code maxDrop} cells further, that has something to stand
     * on ({@link #standingY} is not NaN); {@code Integer.MIN_VALUE} when there is none (open air, a pit, an unloaded chunk).
     */
    public static int surfaceBelow(ServerLevel level, double x, double y, double z, int maxDrop) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), top = (int) Math.floor(y);
        for (int dy = 0; dy <= maxDrop; dy++) if (!Double.isNaN(standingY(level, bx, top - dy, bz))) return top - dy;
        return Integer.MIN_VALUE;
    }

    /** The place a person rests at in the column of (x, z) whose feet block is y: the exact x and z, and the real standing height. */
    public static Vec3 feet(ServerLevel level, double x, int y, double z) {
        double top = standingY(level, (int) Math.floor(x), y, (int) Math.floor(z));
        return new Vec3(x, Double.isNaN(top) ? y : top, z);
    }

    /**
     * Puts the wielder at the exact spot. Falls back to setting the position when the network teleport is not available (a stand-in player), clears the
     * fall and the speed, and tells the client. Returns false when it failed for any reason.
     */
    public static boolean teleport(ServerPlayer p, double x, double y, double z) {
        try {
            try {
                p.teleportTo(x, y, z);
            } catch (RuntimeException noConnection) {
                // a stand-in has no connection to teleport through: the position check below moves it
            }
            if (p.distanceToSqr(x, y, z) > 0.25) p.setPos(x, y, z);          // the network teleport did not (or could not) move the player: put them there
            p.fallDistance = 0f;
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
            return true;
        } catch (RuntimeException failed) {
            return false;
        }
    }

    public static boolean teleport(ServerPlayer p, Vec3 to) { return teleport(p, to.x, to.y, to.z); }

    /** Throws the wielder along {@code v} (blocks per tick), tells the client, and clears the fall. */
    public static void launch(ServerPlayer p, double vx, double vy, double vz) {
        p.setDeltaMovement(vx, vy, vz);
        p.hurtMarked = true;
        p.fallDistance = 0f;
    }

    /** The horizontal part of a direction, normalised; {@code fallback} (also horizontal) when it points straight up or down. */
    public static Vec3 flat(Vec3 dir, Vec3 fallback) {
        Vec3 f = new Vec3(dir.x, 0.0, dir.z);
        if (f.lengthSqr() < 1.0E-6) return fallback;
        return f.normalize();
    }
}
