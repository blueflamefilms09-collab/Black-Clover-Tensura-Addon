package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Lust forms of the Spear take restraint away: the spear no longer lets its wielder aim away from prey (the owner's "restraint is lost"). That is
 * one rule that every thrust, lunge and beam must apply the same way, so the direction of a straight technique is asked of this class and not worked out
 * by each one: the wielder's look, or, with the Ravenous Aim passive, the line to the nearest foe within a short reach and a wide cone. It is bounded
 * (nine blocks, seventy-five degrees, line of sight needed) so it never reaches across a room or through a wall.
 */
final class SpearAim {
    private SpearAim() {}

    /** The unit direction a straight technique goes now: the look, or the snap to the nearest foe with Ravenous Aim. */
    static Vec3 dir(HeroCtx c) {
        Vec3 look = c.look();
        if (!c.form.has(SpearIds.RAVENOUS_AIM)) return look;
        LivingEntity prey = Foes.nearestInCone(c, c.eye(), look, SpearNumbers.AIM_SNAP_RANGE, SpearNumbers.AIM_SNAP_ANGLE);
        if (prey == null) return look;
        Vec3 to = prey.getBoundingBox().getCenter().subtract(c.eye());
        return to.lengthSqr() < 1.0E-6 ? look : to.normalize();
    }
}
