package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Why: the Avatar used to run the plain vanilla melee goal whatever it held. Now the melee goal is only one of the ways it fights: it runs when the weapon AI
 * says "melee" (a sword, an axe, a trident, bare fists, a bow with no arrows), and it never hits from behind a raised shield (a player cannot attack and block
 * at once) and never chases a target out of the leash of an Avatar that was told to stay. Everything else (the approach, the swing, the cooldown) is vanilla's.
 */
public final class AvatarMeleeGoal extends MeleeAttackGoal {
    private final AvatarEntity avatar;

    public AvatarMeleeGoal(AvatarEntity avatar, double speed, boolean followUnseen) {
        super(avatar, speed, followUnseen);
        this.avatar = avatar;
    }

    /** Melee is the style of everything that does not shoot or cast; a mage with nothing to cast also fights this way (the planner chose MELEE for it). */
    private boolean mine() {
        CombatStyle s = avatar.combatStyle();
        return s == CombatStyle.MELEE && AvatarCombat.mayEngage(avatar, avatar.getTarget());
    }

    @Override public boolean canUse() { return mine() && super.canUse(); }

    @Override public boolean canContinueToUse() { return mine() && super.canContinueToUse(); }

    @Override
    protected boolean canPerformAttack(LivingEntity target) { return !avatar.isBlocking() && super.canPerformAttack(target); }
}
