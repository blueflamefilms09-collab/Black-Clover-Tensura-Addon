package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: every line technique of the Spear and Bow (a thrust, a beam, an arrow) must stop at the first wall, as the owner's rule "a technique is answered by
 * cover" needs, and every one of them asks the same question: where does the line from here to there end? One method answers it with the collision shapes
 * a bolt or an arrow would meet (not the selection outline: a fence or a pane stops it, a flower does not), so the lines look and act alike.
 */
public final class Rays {
    private Rays() {}

    /** The end of the segment from {@code from} to {@code to}: the first solid block it meets, or {@code to} when the way is clear. */
    public static Vec3 clipEnd(HeroCtx c, Vec3 from, Vec3 to) {
        HitResult hit = c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** The unit direction from a to b; {@code fallback} when they are the same point. */
    public static Vec3 dir(Vec3 a, Vec3 b, Vec3 fallback) {
        Vec3 d = b.subtract(a);
        return d.lengthSqr() < 1.0E-8 ? fallback : d.normalize();
    }
}
