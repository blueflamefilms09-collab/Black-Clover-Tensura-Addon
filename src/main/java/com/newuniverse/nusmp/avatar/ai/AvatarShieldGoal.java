package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: the owner asked that "if it holds a shield, it raises it when a melee enemy is near". The Avatar holds the shield up for thirty ticks of every fifty while
 * its target is within reach ({@link CombatPlanner#shieldUp}; the Avatar's id staggers the cycle so a group does not block in step), and the melee goal does not
 * swing while it is up, so the twenty ticks in between are the Avatar's strikes. It takes no movement flag. A shooter or a mage never raises it (they are busy with
 * the bow or the spell, and an item can only be in use once). The shield wears out like a player's, see {@code AvatarEntity.hurtCurrentlyUsedShield}.
 */
public final class AvatarShieldGoal extends Goal {
    /** A target closer than this is a melee threat. */
    private static final double NEAR = 5.5;

    private final AvatarEntity avatar;
    private InteractionHand raised;

    public AvatarShieldGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    private InteractionHand shieldHand() { return AvatarCombat.handHolding(avatar, WeaponKind.SHIELD); }

    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || !AvatarAiConfig.shieldAi() || !AvatarAiConfig.weaponAi() || avatar.combatStyle() != CombatStyle.MELEE) return false;
        return avatar.getTarget() != null && shieldHand() != null;
    }

    @Override public boolean canContinueToUse() { return canUse(); }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        InteractionHand hand = shieldHand();
        LivingEntity target = avatar.getTarget();
        if (hand == null || target == null) return;
        boolean near = avatar.distanceToSqr(target) < NEAR * NEAR;
        boolean up = CombatPlanner.shieldUp(true, near, avatar.level().getGameTime(), avatar.getId());
        if (up && !avatar.isUsingItem()) {
            avatar.startUsingItem(hand);
            raised = hand;
        } else if (!up && raised != null && avatar.isUsingItem() && avatar.getUsedItemHand() == raised) {
            avatar.stopUsingItem();
            raised = null;
        }
    }

    @Override
    public void stop() {
        if (raised != null && avatar.isUsingItem() && avatar.getUsedItemHand() == raised) avatar.stopUsingItem();
        raised = null;
    }
}
