package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: "out of sight" must be the same thing for the Dungeon Master as it is for the player: a block between the eye and the monster. The
 * game's own line-of-sight test ({@code LivingEntity.hasLineOfSight}) is a ray cast with the collision shapes of the blocks, so a glass pane or
 * iron bars (which a player sees through but cannot walk through) do not block it while a wall or a closed door do. This is the same cast from a
 * player's eyes to the cell, tried to the monster's feet and to its head (a monster half behind a pillar is seen). One thing differs from a
 * plain {@code level.clip}: the cast must never LOAD a chunk, because a ray from a player to a spot 48 blocks away may cross a chunk nobody is
 * near (a block read in an unloaded chunk makes the server generate or load it). Before casting, the chunks along the line are sampled with
 * {@code hasChunk}; if any is not loaded the cell is reported as seen, which makes the picker skip it. Server thread only.
 */
final class LevelSight implements SpawnPicker.LineOfSight {
    private final ServerLevel level;
    private final Entity anchor;

    LevelSight(ServerLevel level, Entity anchor) {
        this.level = level;
        this.anchor = anchor;
    }

    @Override
    public boolean sees(double ex, double ey, double ez, int cx, int cy, int cz) {
        Vec3 eye = new Vec3(ex, ey, ez);
        if (!chunksLoaded(eye, cx + 0.5, cz + 0.5)) return true;
        return clear(eye, cx + 0.5, cy + 0.9, cz + 0.5) || clear(eye, cx + 0.5, cy + 1.7, cz + 0.5);
    }

    private boolean clear(Vec3 eye, double x, double y, double z) {
        return level.clip(new ClipContext(eye, new Vec3(x, y, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, anchor)).getType() == HitResult.Type.MISS;
    }

    /** Are the chunks under the line (eye to cell, sampled every ten blocks) all loaded? */
    private boolean chunksLoaded(Vec3 eye, double tx, double tz) {
        double dx = tx - eye.x, dz = tz - eye.z;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / 10.0));
        for (int i = 0; i <= steps; i++) {
            double f = i / (double) steps;
            int bx = (int) Math.floor(eye.x + dx * f), bz = (int) Math.floor(eye.z + dz * f);
            if (!level.hasChunk(bx >> 4, bz >> 4)) return false;
        }
        return true;
    }
}
