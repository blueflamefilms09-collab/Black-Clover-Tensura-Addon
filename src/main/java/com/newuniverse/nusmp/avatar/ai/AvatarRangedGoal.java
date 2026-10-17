package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.inventory.AvatarGear;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Why: "a Bow makes it keep its distance and shoot" is what the owner asked for, and vanilla's own bow and crossbow goals only accept a {@code Monster} that is a
 * {@code RangedAttackMob}, which the Avatar is not. So this goal does the same job for it: it walks to the edge of its shooting band, stops there, strafes and
 * backs away when the target comes closer than the band allows (a Cowardly Avatar stands farther away, an Aggressive one closer, see {@link CombatPlanner}),
 * draws a bow for a full second or loads a crossbow, and looses the shot through {@link AvatarCombat}. An Avatar that was told to stay shoots from its post and does not
 * move. It runs only while the weapon AI's style is BOW or CROSSBOW, which is also what hands the fight back to the melee goal when the arrows run out.
 */
public final class AvatarRangedGoal extends Goal {
    private enum Load { UNCHARGED, CHARGING, CHARGED, READY }

    private final AvatarEntity avatar;
    private int attackTime = -1, seeTime, strafingTime = -1, loadDelay, repath;
    private boolean strafeClockwise, strafeBack;
    private Load load = Load.UNCHARGED;

    public AvatarRangedGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean shooter() {
        CombatStyle s = avatar.combatStyle();
        return s == CombatStyle.BOW || s == CombatStyle.CROSSBOW;
    }

    @Override
    public boolean canUse() {
        return !avatar.level().isClientSide && shooter() && AvatarCombat.mayEngage(avatar, avatar.getTarget());
    }

    @Override public boolean canContinueToUse() { return canUse(); }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        avatar.setAggressive(true);
        attackTime = -1;
        seeTime = 0;
        load = Load.UNCHARGED;
    }

    @Override
    public void stop() {
        avatar.setAggressive(false);
        seeTime = 0;
        attackTime = -1;
        load = Load.UNCHARGED;
        if (avatar.isUsingItem()) avatar.stopUsingItem();
        avatar.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = avatar.getTarget();
        if (target == null) return;
        CombatStyle style = avatar.combatStyle();
        CombatPlanner.Range band = AvatarCombat.range(avatar, style);
        double distSq = avatar.distanceToSqr(target);
        boolean sight = avatar.getSensing().hasLineOfSight(target);
        if (sight != seeTime > 0) seeTime = 0;
        seeTime += sight ? 1 : -1;
        boolean staying = avatar.staying();
        boolean inBand = distSq <= band.max() * band.max() && seeTime >= 20;

        // ---- where to stand
        if (staying) {
            avatar.getNavigation().stop();
            avatar.getLookControl().setLookAt(target, 30.0f, 30.0f);
        } else if (!inBand) {
            strafingTime = -1;
            if (--repath <= 0) {
                repath = 10;
                avatar.getNavigation().moveTo(target, 1.15);
            }
            avatar.getLookControl().setLookAt(target, 30.0f, 30.0f);
        } else {
            avatar.getNavigation().stop();
            strafingTime++;
            if (strafingTime >= 20) {
                if (avatar.getRandom().nextFloat() < 0.3f) strafeClockwise = !strafeClockwise;
                strafingTime = 0;
            }
            // back away when the target is closer than the band's near edge: vanilla's hysteresis with the reach set to twice the near edge, so the quarter and
            // three-quarter lines of its squared reach fall at the near edge and a little beyond it
            strafeBack = CombatPlanner.strafeBackwards(distSq, band.min() * 2.0, strafeBack);
            avatar.getMoveControl().strafe(strafeBack ? -0.5f : 0.5f, strafeClockwise ? 0.5f : -0.5f);
            avatar.lookAt(target, 30.0f, 30.0f);
        }

        // ---- the shot
        if (style == CombatStyle.BOW) bow(target, sight);
        else crossbow(target, sight);
    }

    private void bow(LivingEntity target, boolean sight) {
        InteractionHand hand = AvatarCombat.handHolding(avatar, WeaponKind.BOW);
        if (hand == null) return;
        if (avatar.isUsingItem()) {
            if (!sight && seeTime < -60) {
                avatar.stopUsingItem();
            } else if (sight) {
                int drawn = avatar.getTicksUsingItem();
                if (drawn >= CombatPlanner.BOW_DRAW_TICKS) {
                    avatar.stopUsingItem();
                    AvatarCombat.shootBow(avatar, target, CombatPlanner.bowPower(drawn));
                    attackTime = CombatPlanner.shotInterval(CombatStyle.BOW, AvatarCombat.aggressive(avatar));
                }
            }
        } else if (--attackTime <= 0 && seeTime >= -60 && AvatarGear.hasAmmo(avatar)) {
            avatar.startUsingItem(hand);
        }
    }

    private void crossbow(LivingEntity target, boolean sight) {
        InteractionHand hand = AvatarCombat.handHolding(avatar, WeaponKind.CROSSBOW);
        if (hand == null) return;
        ItemStack stack = avatar.getItemInHand(hand);
        switch (load) {
            case UNCHARGED -> {
                if (CrossbowItem.isCharged(stack)) load = Load.CHARGED;                       // it came loaded (picked up, or a shot that was never fired)
                else if (--attackTime <= 0 && sight && !avatar.isUsingItem() && AvatarGear.hasAmmo(avatar)) {
                    avatar.startUsingItem(hand);
                    load = Load.CHARGING;
                }
            }
            case CHARGING -> {
                if (!avatar.isUsingItem()) {
                    load = Load.UNCHARGED;
                } else if (avatar.getTicksUsingItem() >= CrossbowItem.getChargeDuration(avatar.getUseItem(), avatar)) {
                    avatar.releaseUsingItem();                                                   // this loads it (and takes the arrow from the pockets)
                    load = CrossbowItem.isCharged(stack) ? Load.CHARGED : Load.UNCHARGED;
                    loadDelay = 10 + avatar.getRandom().nextInt(10);
                }
            }
            case CHARGED -> {
                if (--loadDelay <= 0) load = Load.READY;
            }
            case READY -> {
                if (sight) {
                    AvatarCombat.shootCrossbow(avatar, target);
                    load = Load.UNCHARGED;
                    attackTime = CombatPlanner.shotInterval(CombatStyle.CROSSBOW, AvatarCombat.aggressive(avatar));
                }
            }
        }
    }
}
