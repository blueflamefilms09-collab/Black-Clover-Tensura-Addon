package com.newuniverse.nusmp.labyrinth.squad;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: focus fire is what turns six monsters into one problem. The squad thinks out ONE target ({@link SquadMind#focusTarget}) and this target goal, installed on every
 * member's target selector, hands it to the member: at once to tanks and the leader (they have first claim, the taunt), and to the others when they have no target, their
 * target died, or their own is farther than twelve blocks. It never overrides a member that is already fighting something close (a mob that was hit by a pet keeps hitting
 * the pet), and it only assigns a target the member can see or is near (a member told to attack someone behind a wall would stop walking, which would undo the
 * Director's hunt, whose job is to bring the squad through the rooms). A squad whose nerve broke stops giving orders. It is a one-shot goal: it sets the target and
 * lets go, and the mob's own target goals go on working beside it.
 */
public final class SquadFocusGoal extends Goal {
    private final Mob mob;
    private final UUID squadId;
    private int next;

    public SquadFocusGoal(Mob mob, UUID squadId) {
        this.mob = mob;
        this.squadId = squadId;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (++next < 5) return false;
        next = 0;
        Squad s = SquadManager.get(squadId);
        if (s == null || s.isBroken() || !mob.isAlive()) return false;
        LivingEntity t = s.target();
        if (t == null || t == mob.getTarget()) return false;
        Squad.Member me = s.member(mob.getUUID());
        if (me == null || me.fleeing(mob.level().getGameTime())) return false;
        if (!mob.hasLineOfSight(t) && mob.distanceToSqr(t) > 16.0 * 16.0) return false;
        LivingEntity own = mob.getTarget();
        if (own == null || !own.isAlive()) return true;
        if (me.role().isFrontLine()) return true;
        return mob.distanceToSqr(own) > 12.0 * 12.0;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        Squad s = SquadManager.get(squadId);
        LivingEntity t = s == null ? null : s.target();
        if (t != null) SquadManager.aim(mob, t);
    }
}
