package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: the tank's job is not to chase the target, it is to stand between the target and whoever the squad cannot afford to lose. The point it walks to is computed by
 * {@link SquadGeometry#interceptPoint} (1.5 blocks from the ward on the segment ward-to-target) every few ticks, because both ends of that segment move; once it stands
 * on the segment the decision turns to "hold" and this goal lets go, so the mob's own melee goal (which the goal never replaces) strikes whatever steps into reach. The
 * taunt is the other half: every two seconds, if the thing it faces is itself a mob (a player's pet, a summoned shadow, a minion) its target is turned on the tank, so
 * the creatures that would have gone for the healer go for the one that can take it; a player cannot be forced, so for a player the body in the way is the taunt, and
 * the squad's focus goal already gives the tank first claim on the target. It claims only the MOVE flag, and only while it has a point to walk to.
 */
public final class InterceptAttackGoal extends SquadGoal {
    private static final int TAUNT_EVERY = 40;
    private int lastTaunt;

    public InterceptAttackGoal(Mob mob, UUID squadId) {
        super(mob, squadId);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % REFRESH != 0) return false;
        Squad s = squad();
        if (s == null || s.target() == null) return false;
        return wants(Kind.INTERCEPT);
    }

    @Override
    public boolean canContinueToUse() {
        Squad s = squad();
        return s != null && s.target() != null && mob.isAlive() && wants(Kind.INTERCEPT);
    }

    @Override
    public boolean isInterruptable() { return true; }

    @Override
    public void start() {
        Move m = decision();
        if (m != null) walk(m);
    }

    @Override
    public void tick() {
        Squad s = squad();
        LivingEntity t = s == null ? null : s.target();
        face(t);
        if (mob.tickCount % REFRESH == 0) {
            Move m = decision();
            if (m != null && m.kind() == Kind.INTERCEPT && (mob.getNavigation().isDone() || mob.tickCount % 10 == 0)) walk(m);
        }
        if (t instanceof Mob foe && mob.tickCount - lastTaunt >= TAUNT_EVERY && foe.getTarget() != mob && mob.distanceToSqr(foe) < 24.0 * 24.0) {
            foe.setTarget(mob);
            lastTaunt = mob.tickCount;
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        forget();
    }
}
