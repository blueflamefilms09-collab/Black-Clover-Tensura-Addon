package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finds the standing spot for the God of Order: eight blocks from the holder when it can, nearer when the room is tight. */
final class OrderSpots {
    private OrderSpots() {}

    /** Half the God's width and its height, for the check that it fits. */
    private static final double HALF_WIDTH = 0.45, HEIGHT = 3.4;

    /**
     * A spot on firm ground at one of the radii of {@link OrderTable#SPAWN_RADII} round 'centre', starting at the angle 'startAngle' (radians) and trying
     * {@link OrderTable#SPAWN_ANGLES} directions on each ring. Only loaded blocks, free of water and lava, with room for the God's body. If nothing fits
     * the centre itself is used when it is standable, else null.
     */
    static Vec3 find(ServerLevel level, Vec3 centre, double startAngle) {
        for (double radius : OrderTable.SPAWN_RADII) {
            for (int k = 0; k < OrderTable.SPAWN_ANGLES; k++) {
                double a = startAngle + k * (Math.PI * 2.0 / OrderTable.SPAWN_ANGLES);
                Vec3 p = fits(level, new Vec3(centre.x + Math.cos(a) * radius, centre.y, centre.z + Math.sin(a) * radius));
                if (p != null) return p;
            }
        }
        return fits(level, centre);
    }

    /** The point on the ground under/over 'p' where the God fits, or null. */
    static Vec3 fits(ServerLevel level, Vec3 p) {
        BlockPos base = BlockPos.containing(p);
        if (!level.isLoaded(base)) return null;
        Vec3 g = ZogratisBoss.groundOrNull(level, p);
        if (g == null) return null;
        BlockPos feet = BlockPos.containing(g);
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.below()).isEmpty()) return null;
        AABB box = new AABB(g.x - HALF_WIDTH, g.y, g.z - HALF_WIDTH, g.x + HALF_WIDTH, g.y + HEIGHT, g.z + HALF_WIDTH);
        return level.noCollision(box) ? g : null;
    }
}
