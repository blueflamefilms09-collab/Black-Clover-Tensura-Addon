package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.Stance;
import com.newuniverse.nusmp.avatar.delve.AvatarDelve;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/**
 * Why: an Avatar that was told to stay must stay, and one that is on a trip into the dungeon must not wander off the path of its trip. The vanilla stroll goal
 * knows neither, so this one wraps it and says "not now" in those two cases; at the creator's side it strolls exactly as before.
 */
public final class AvatarStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final AvatarEntity avatar;

    public AvatarStrollGoal(AvatarEntity avatar, double speed) {
        super(avatar, speed);
        this.avatar = avatar;
    }

    private boolean free() { return avatar.stance() != Stance.STAY && !AvatarDelve.onTrip(avatar); }

    @Override public boolean canUse() { return free() && super.canUse(); }

    @Override public boolean canContinueToUse() { return free() && super.canContinueToUse(); }
}
