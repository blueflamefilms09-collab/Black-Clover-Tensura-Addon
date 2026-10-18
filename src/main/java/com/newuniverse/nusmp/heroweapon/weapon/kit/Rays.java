package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the thrown blades, the bolts and the lances of these weapons are hitscan lines (no entity is created for them), and every one of them must stop at a wall
 * the same way and know whether it stopped at one (a blade that hits a wall ricochets, a bolt that hits one just ends). One method answers both, so no technique
 * passes through stone by forgetting the clip. Server thread only.
 */
public final class Rays {
    private Rays() {}

    /** Where a straight flight ended and whether a block ended it. {@code face} is the normal of the block face that was hit (zero when no block was hit). */
    public record Reach(Vec3 end, boolean wall, Vec3 face) {}

    /** The flight from {@code from} along {@code dir} (any length) for {@code range} blocks, stopped by the first solid block. */
    public static Reach reach(HeroCtx c, Vec3 from, Vec3 dir, double range) {
        Vec3 n = dir.lengthSqr() < 1.0e-9 ? c.look() : dir.normalize();
        Vec3 to = from.add(n.scale(Math.max(0.0, range)));
        HitResult hit = c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player));
        if (hit.getType() == HitResult.Type.MISS) return new Reach(to, false, Vec3.ZERO);
        Vec3 face = hit instanceof BlockHitResult b ? Vec3.atLowerCornerOf(b.getDirection().getNormal()) : Vec3.ZERO;
        return new Reach(hit.getLocation(), true, face);
    }

    /** The reflection of a direction off a surface with the given normal (the normal need not be a unit vector; a zero normal returns the direction reversed). */
    public static Vec3 bounce(Vec3 dir, Vec3 normal) {
        if (normal.lengthSqr() < 1.0e-9) return dir.scale(-1.0);
        Vec3 n = normal.normalize();
        return dir.subtract(n.scale(2.0 * dir.dot(n)));
    }
}
