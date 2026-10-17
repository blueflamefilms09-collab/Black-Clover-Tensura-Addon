package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ai.AvatarCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: a loyal Avatar fights for its creator and for itself: it targets whoever hurt it, whoever hurt its creator, and whatever its creator is attacking,
 * recently enough that the fight is still on. Who may be a target is decided by {@link AvatarTargeting} (never the creator, FTB squadmates, other Avatars of
 * the creator or the creator's pets).
 */
public class AvatarDefendSelfOrCreatorGoal extends Goal {
    /** A grudge is remembered for this many ticks after the hit. */
    private static final int MEMORY = 100;
    private final AvatarEntity avatar;
    private LivingEntity pick;

    public AvatarDefendSelfOrCreatorGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!avatar.isLoyal()) return false;
        LivingEntity own = recent(avatar.getLastHurtByMob(), avatar.getLastHurtByMobTimestamp(), avatar.tickCount);
        if (own != null) { pick = own; return true; }
        ServerPlayer c = avatar.creatorPlayer();
        if (c == null || c.level() != avatar.level()) return false;
        LivingEntity hurtCreator = recent(c.getLastHurtByMob(), c.getLastHurtByMobTimestamp(), c.tickCount);
        if (hurtCreator != null) { pick = hurtCreator; return true; }
        LivingEntity theirs = recent(c.getLastHurtMob(), c.getLastHurtMobTimestamp(), c.tickCount);
        if (theirs != null) { pick = theirs; return true; }
        return false;
    }

    private LivingEntity recent(LivingEntity e, int stamp, int now) {
        if (e == null || now - stamp > MEMORY || !AvatarTargeting.canHarm(avatar, e) || !AvatarCombat.mayEngage(avatar, e)) return null;
        return e;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = avatar.getTarget();
        return avatar.isLoyal() && t != null && t.isAlive() && AvatarTargeting.canHarm(avatar, t) && avatar.distanceToSqr(t) < 40.0 * 40.0 && AvatarCombat.mayEngage(avatar, t);
    }

    @Override
    public void start() { avatar.setTarget(pick); }

    @Override
    public void stop() {
        avatar.setTarget(null);
        pick = null;
    }
}
