package com.newuniverse.nusmp.heroweapon.weapon.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Gauntlet lunges and blinks, the Carriage portal and the Projectile's pull all move a person through the world, and a move that drops the wielder into
 * a wall, onto lava or off a cliff is worse than no move. These helpers push the wielder along a line for a chosen distance (and cancel the fall that follows),
 * and find a spot to stand near a wanted one that has room for the body, solid ground within three blocks and no lava or fire underfoot. They never change a
 * block. Server thread only.
 */
public final class Moves {
    private Moves() {}

    /** The farthest a lunge may go on level ground, in blocks. */
    public static final double MAX_BLOCKS = 12.0;

    /**
     * Pushes the player along the horizontal part of {@code dir} so that it covers about {@code blocks} blocks on level ground (the first tick's speed is that
     * distance times 0.45, which is what ground friction gives back as distance), with a little lift, and forgets the fall that follows. No effect while riding
     * or gliding.
     */
    public static void dash(ServerPlayer p, Vec3 dir, double blocks, double lift) {
        if (p == null || !p.isAlive() || p.isPassenger() || p.isFallFlying()) return;
        Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
        if (flat.lengthSqr() < 1.0e-6) return;
        Vec3 n = flat.normalize();
        double v = Mth.clamp(blocks, 0.0, MAX_BLOCKS) * 0.45;
        p.setDeltaMovement(n.x * v, Math.max(lift, 0.0), n.z * v);
        p.hurtMarked = true;
        p.fallDistance = 0.0f;
    }

    /** A place for {@code who}'s body near (x, y, z): the first of the heights y, y+1, y-1, y+2, y-2 with room and ground, or null. */
    public static Vec3 findSpot(ServerLevel level, ServerPlayer who, double x, double y, double z) {
        for (int dy : new int[]{0, 1, -1, 2, -2}) {
            double yy = y + dy;
            AABB box = who.getBoundingBox().move(x - who.getX(), yy - who.getY(), z - who.getZ());
            if (!level.noCollision(who, box)) continue;
            if (groundBelow(level, BlockPos.containing(x, yy, z))) return new Vec3(x, yy, z);
        }
        return null;
    }

    /** True when something solid lies within three blocks under {@code feet} and neither lava nor fire is on the way. */
    public static boolean groundBelow(ServerLevel level, BlockPos feet) {
        if (level.getFluidState(feet).is(FluidTags.LAVA)) return false;
        for (int i = 1; i <= 3; i++) {
            BlockPos at = feet.below(i);
            BlockState below = level.getBlockState(at);
            if (below.is(Blocks.LAVA) || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.MAGMA_BLOCK)) return false;
            if (!below.getCollisionShape(level, at).isEmpty()) return true;
        }
        return false;
    }

    /** Teleports the player to a spot found by {@link #findSpot} near the wanted point; false (and no move) when there is none or the player is riding. */
    public static boolean blinkTo(ServerLevel level, ServerPlayer p, double x, double y, double z) { return blinkTo(level, p, x, y, z, p.getYRot()); }

    /** The same, and the player ends up looking along {@code yaw} (Minecraft's sense). */
    public static boolean blinkTo(ServerLevel level, ServerPlayer p, double x, double y, double z, float yaw) {
        if (p == null || !p.isAlive() || p.isPassenger()) return false;
        Vec3 spot = findSpot(level, p, x, y, z);
        if (spot == null) return false;
        p.teleportTo(level, spot.x, spot.y, spot.z, yaw, p.getXRot());
        p.fallDistance = 0.0f;
        return true;
    }
}
