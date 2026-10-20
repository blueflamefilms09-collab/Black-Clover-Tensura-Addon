package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import net.minecraft.world.entity.Mob;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: when nothing is in sight a squad should still look like one. The Dungeon Master sends the squad's LEADER hunting; this goal makes the others follow it in
 * formation, each to its own slot ({@link SquadFormation}: tanks ahead, the healer last) turned with the way the squad faces. It claims the MOVE flag only while the squad
 * has no target and only at a lower priority than the combat goals, so the moment a fight starts the intercept, flank and retreat goals (and the mob's own attack
 * goals) take over; standing in its slot it holds still, which also keeps a squad member from wandering off on its random stroll. The leader itself is not moved here
 * (the Director's hunt walks it), and a squad whose nerve broke does not follow anyone.
 */
public final class FormationFollowGoal extends SquadGoal {
    public FormationFollowGoal(Mob mob, UUID squadId) {
        super(mob, squadId);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean calm() {
        Squad s = squad();
        return s != null && !s.isBroken() && s.target() == null && mob.getTarget() == null && mob.isAlive();
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % REFRESH != 0 || !calm()) return false;
        return wants(Kind.FORM, Kind.HOLD);
    }

    @Override
    public boolean canContinueToUse() { return calm() && wants(Kind.FORM, Kind.HOLD); }

    @Override
    public void start() { act(); }

    @Override
    public void tick() {
        if (mob.tickCount % REFRESH == 0) act();
    }

    private void act() {
        Move m = decision();
        if (m == null) return;
        if (m.kind() == Kind.FORM && (mob.getNavigation().isDone() || mob.tickCount % 10 == 0)) walk(m);
        else if (m.kind() == Kind.HOLD) mob.getNavigation().stop();
    }

    @Override
    public void stop() { forget(); }
}
