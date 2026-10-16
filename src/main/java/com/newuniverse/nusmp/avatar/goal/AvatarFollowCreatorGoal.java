package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.betrayal.BetrayalDimensionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: a loyal Avatar has to stay with its creator. It walks to them when they are more than a few blocks away, teleports to them when they get too far,
 * and follows them across a dimension change on the same server (through {@link BetrayalDimensionHandler}, the same utility the betrayed hunt uses). After
 * an Ordinary Existence the Avatar only follows while its creator is within a fixed range in the same dimension: it has a life of its own now.
 */
public class AvatarFollowCreatorGoal extends Goal {
    private static final double START = 7.0, STOP = 3.0, ORDINARY_RANGE = 32.0;
    private final AvatarEntity avatar;
    private final boolean limited;
    private ServerPlayer creator;
    private long nextTeleport;
    private int repath;

    public AvatarFollowCreatorGoal(AvatarEntity avatar, boolean limited) {
        this.avatar = avatar;
        this.limited = limited;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!avatar.isLoyal() || avatar.isPassenger()) return false;
        ServerPlayer p = avatar.creatorPlayer();
        if (p == null || p.isSpectator()) return false;
        boolean sameLevel = p.level() == avatar.level();
        if (limited && (!sameLevel || avatar.distanceToSqr(p) > ORDINARY_RANGE * ORDINARY_RANGE)) return false;
        if (sameLevel && avatar.distanceToSqr(p) < START * START) return false;
        creator = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (creator == null || !creator.isAlive() || !avatar.isLoyal()) return false;
        if (creator.level() != avatar.level()) return !limited;
        return avatar.distanceToSqr(creator) > STOP * STOP && !(limited && avatar.distanceToSqr(creator) > ORDINARY_RANGE * ORDINARY_RANGE);
    }

    @Override
    public void start() { repath = 0; }

    @Override
    public void stop() {
        creator = null;
        avatar.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (creator == null) return;
        long now = avatar.level().getGameTime();
        boolean otherLevel = creator.level() != avatar.level();
        double far = AvatarConfig.followTeleportDistance();
        boolean fighting = avatar.getTarget() != null && avatar.getTarget().isAlive();
        if (!limited && now >= nextTeleport && (otherLevel || (!fighting && avatar.distanceToSqr(creator) > far * far) || avatar.distanceToSqr(creator) > far * far * 4)) {
            nextTeleport = now + 60;
            AvatarEntity moved = BetrayalDimensionHandler.teleportNear(avatar, creator);
            if (moved != null) moved.getNavigation().stop();
            return;
        }
        if (otherLevel) return;
        avatar.getLookControl().setLookAt(creator, 10.0f, avatar.getMaxHeadXRot());
        if (--repath <= 0) {
            repath = adjustedTickDelay(10);
            avatar.getNavigation().moveTo(creator, 1.2);
        }
    }
}
