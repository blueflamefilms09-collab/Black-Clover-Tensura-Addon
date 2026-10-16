package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: a Soul Awakening gives the Avatar a signature move (see {@link AvatarUniqueSkill}); this goal uses it against the Avatar's current target when
 * it is in range and the cooldown, shortened by {@link AvatarEntity#cooldownMultiplier()}, has passed. Damage goes through the balance law and the shared
 * {@link AvatarTargeting} rule, so the move can never hit the creator or the creator's friends.
 */
public class AvatarSignatureGoal extends Goal {
    private final AvatarEntity avatar;
    private long nextUse;

    public AvatarSignatureGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        AvatarUniqueSkill.Perk p = avatar.perk();
        LivingEntity t = avatar.getTarget();
        return p != null && t != null && t.isAlive() && avatar.level().getGameTime() >= nextUse && avatar.distanceToSqr(t) < 14.0 * 14.0 && avatar.hasLineOfSight(t);
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        AvatarUniqueSkill.Perk p = avatar.perk();
        LivingEntity t = avatar.getTarget();
        if (p == null || t == null) return;
        nextUse = avatar.level().getGameTime() + Math.max(20L, (long) (p.cooldownTicks() * avatar.cooldownMultiplier()));
        AvatarSignatures.perform(avatar, t, p);
    }
}
