package com.newuniverse.nusmp.unique;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the shared helper that keeps ordinary mishaps from destroying a dropped 1-of-1: the item entity is invulnerable (fire, lava, cactus, explosions) with an unlimited lifetime (no despawn), and one that falls out of
 * the world is lifted back to a safe spot of the same dimension. It is the approach the Black Sword and the Canon Grimoire already use, now used for every kind. Cost: two field writes when a dropped unique joins a
 * level, and one height-map read (never a chunk load: the item is in a loaded chunk) when one is in the void.
 */
public final class ItemGuard {
    private ItemGuard() {}

    /** Makes the item entity indestructible and permanent. */
    public static void protect(ItemEntity e) {
        if (!e.isInvulnerable()) e.setInvulnerable(true);
        e.setUnlimitedLifetime();
    }

    /** If the item entity is in the void, moves it to the surface of its own column (or the safe spot of the dimension). True when it was moved. */
    public static boolean rescue(ItemEntity e, ServerLevel level) {
        if (!UniqueRules.inVoid(e.getY(), level.getMinBuildHeight())) return false;
        double x = e.getX(), z = e.getZ(), y;
        if (level.dimension() == Level.END) {
            BlockPos p = ServerLevel.END_SPAWN_POINT;
            x = p.getX() + 0.5;
            y = p.getY() + 1.0;
            z = p.getZ() + 0.5;
        } else if (level.dimensionType().hasCeiling()) {
            y = Math.max(level.getSeaLevel(), level.getMinBuildHeight()) + 1.0;                  // the Nether: the surface height is the roof, so use the sea level of the column
        } else {
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, e.getBlockX(), e.getBlockZ());
            if (h <= level.getMinBuildHeight() + 1) {
                BlockPos sp = level.getSharedSpawnPos();
                x = sp.getX() + 0.5;
                z = sp.getZ() + 0.5;
                y = sp.getY() + 1.0;
            } else {
                y = h + 1.0;
            }
        }
        e.setPos(x, y, z);
        e.setDeltaMovement(Vec3.ZERO);
        return true;
    }
}
