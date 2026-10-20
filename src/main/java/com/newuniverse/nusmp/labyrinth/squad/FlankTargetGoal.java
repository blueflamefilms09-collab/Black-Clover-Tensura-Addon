package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: a flanker that runs straight at its target is just another mouth at the front. This goal holds the damage dealers back until a tank has reached the target (so the
 * target's attention is spent), then sends each to a point on a circle round the target 110 to 150 degrees off the way the target faces, which is its side or its back.
 * Two flankers never take the same angle or the same side ({@link SquadGeometry#flankAngle}) and start a few ticks apart ({@code RoleMotion.STAGGER} per slot), so they
 * arrive one after the other and do not clump. If the target turns to look at a flanker from close by, or the flanker is badly hurt, the plan is abandoned: the mob goes
 * back to its own attack goal and the flanker tries the other flank after a pause. Near enough to strike, the decision becomes nothing and the mob's melee goal takes
 * over. It claims only the MOVE flag and only while it has a point to walk to or a reason to wait.
 */
public final class FlankTargetGoal extends SquadGoal {
    private static final int PAUSE = 60;
    private long pausedUntil;

    public FlankTargetGoal(Mob mob, UUID squadId) {
        super(mob, squadId);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % REFRESH != 0 || now() < pausedUntil) return false;
        Squad s = squad();
        if (s == null || s.target() == null) return false;
        Move m = decision();
        if (m != null && m.kind() == Kind.ABORT) {
            abort();
            return false;
        }
        return wants(Kind.FLANK, Kind.HOLD);
    }

    @Override
    public boolean canContinueToUse() {
        Squad s = squad();
        if (s == null || s.target() == null || !mob.isAlive() || now() < pausedUntil) return false;
        Move m = decision();
        if (m != null && m.kind() == Kind.ABORT) {
            abort();
            return false;
        }
        return wants(Kind.FLANK, Kind.HOLD);
    }

    private void abort() {
        Squad.Member me = me();
        if (me != null) me.flip = !me.flip;
        pausedUntil = now() + PAUSE;
    }

    @Override
    public void start() { act(); }

    @Override
    public void tick() {
        Squad s = squad();
        face(s == null ? null : s.target());
        if (mob.tickCount % REFRESH == 0) act();
    }

    private void act() {
        Move m = decision();
        if (m == null) return;
        if (m.kind() == Kind.HOLD) mob.getNavigation().stop();
        else if (m.kind() == Kind.FLANK && (mob.getNavigation().isDone() || mob.tickCount % 10 == 0)) walk(m);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        forget();
    }
}
