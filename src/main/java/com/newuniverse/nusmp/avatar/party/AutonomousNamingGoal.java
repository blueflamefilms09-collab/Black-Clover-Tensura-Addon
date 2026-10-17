package com.newuniverse.nusmp.avatar.party;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ai.AvatarGates;
import com.newuniverse.nusmp.avatar.delve.AvatarDelve;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: the owner's rule is that an Avatar with magicules to spare names a wild Tensura monster by itself and keeps it as a member of its NPC party. This goal
 * looks around every few seconds; when the Avatar is loyal, calm, not on a trip and {@link PartyRules#mayName} agrees (the setting on, room in the party, the
 * cooldown over, enough magicules), it picks the nearest fit monster ({@link PartyNaming#nearest}), walks to within a couple of blocks, looks at it and names it. It
 * gives up when the monster flees or something else needs the Avatar (a fight pushes it off by priority). The scan is a small box every couple of seconds, not every tick.
 */
public final class AutonomousNamingGoal extends Goal {
    private static final int SCAN_EVERY = 60, GIVE_UP = 200;
    private static final double REACH = 3.0;

    private final AvatarEntity avatar;
    private Mob target;
    private int left, repath;

    public AutonomousNamingGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean calm() {
        return avatar.isLoyal() && !avatar.frenzied() && avatar.getTarget() == null && !AvatarDelve.onTrip(avatar) && !avatar.staying();
    }

    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || !(avatar.level() instanceof ServerLevel sl) || !AvatarGates.slot(avatar.tickCount, SCAN_EVERY) || !calm()) return false;
        if (!PartyRules.mayName(PartyNaming.facts(avatar, AvatarPartyManager.get(sl.getServer())))) return false;
        target = PartyNaming.nearest(avatar);
        left = GIVE_UP;
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && left > 0 && calm() && PartyNaming.candidate(avatar, target);
    }

    @Override
    public void tick() {
        if (target == null) return;
        left--;
        avatar.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (avatar.distanceToSqr(target) > REACH * REACH) {
            if (--repath <= 0) {
                repath = 10;
                avatar.getNavigation().moveTo(target, 1.1);
            }
            return;
        }
        avatar.getNavigation().stop();
        PartyNaming.name(avatar, target);
        target = null;
    }

    @Override
    public void stop() {
        target = null;
        avatar.getNavigation().stop();
    }
}
