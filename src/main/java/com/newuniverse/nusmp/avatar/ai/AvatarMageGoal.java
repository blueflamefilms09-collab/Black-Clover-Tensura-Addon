package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: a grimoire, a charter grimoire or a staff in the Avatar's hand makes it a mage, and a mage does not stand in the sword fight: it keeps the distance of its
 * band ({@link CombatPlanner#range}; a Cowardly mage stays farther away, an Aggressive one closer), turns to its target and lets the cast goal do the casting. This goal
 * only does the standing, the backing away and the sidestep. A mage the target has cornered strikes back with what it holds, because a mage that never defends
 * itself is just dead. An Avatar that was told to stay casts from its post and does not move. It runs only while the weapon AI's style is MAGE.
 */
public final class AvatarMageGoal extends Goal {
    private final AvatarEntity avatar;
    private int repath, sideTime, hitCooldown;
    private boolean clockwise;

    public AvatarMageGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !avatar.level().isClientSide && avatar.combatStyle() == CombatStyle.MAGE && AvatarCombat.mayEngage(avatar, avatar.getTarget());
    }

    @Override public boolean canContinueToUse() { return canUse(); }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() { avatar.setAggressive(true); }

    @Override
    public void stop() {
        avatar.setAggressive(false);
        avatar.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = avatar.getTarget();
        if (target == null) return;
        if (hitCooldown > 0) hitCooldown--;
        CombatPlanner.Range band = AvatarCombat.range(avatar, CombatStyle.MAGE);
        double dist = avatar.distanceTo(target);
        boolean sight = avatar.getSensing().hasLineOfSight(target);
        avatar.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (avatar.staying()) {
            avatar.getNavigation().stop();
        } else if (dist > band.max() || !sight) {
            if (--repath <= 0) {
                repath = 10;
                avatar.getNavigation().moveTo(target, 1.1);
            }
        } else if (CombatPlanner.tooClose(dist, band)) {
            avatar.getNavigation().stop();
            avatar.getMoveControl().strafe(-0.6f, clockwise ? 0.35f : -0.35f);
            avatar.lookAt(target, 30.0f, 30.0f);
        } else {
            avatar.getNavigation().stop();
            if (++sideTime >= 30) {
                sideTime = 0;
                clockwise = !clockwise;
            }
            avatar.getMoveControl().strafe(0.0f, clockwise ? 0.3f : -0.3f);
            avatar.lookAt(target, 30.0f, 30.0f);
        }
        // cornered: a target within arm's reach is hit back
        if (hitCooldown <= 0 && avatar.isWithinMeleeAttackRange(target) && dist < 2.6) {
            hitCooldown = 20;
            avatar.swing(InteractionHand.MAIN_HAND);
            avatar.doHurtTarget(target);
        }
    }
}
