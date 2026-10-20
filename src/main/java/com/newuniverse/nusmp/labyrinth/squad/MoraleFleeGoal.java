package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: when a squad's leader falls or its ranks are cut to nothing, the survivors that the morale rule sends running must actually run, and run away from the
 * party, not stand in the middle of the room waiting to be killed. This goal has the highest priority of all of them: while the member's reaction is "flee" and its timer has not
 * run out, it walks to a point about fourteen blocks from the nearest threat in the opposite direction (trying two other angles when the first has no path) and
 * keeps going; the enraged survivors need no goal, their bonus is an attribute modifier and their own attack goals do the rest.
 */
public final class MoraleFleeGoal extends Goal {
    private static final double DISTANCE = 14.0;
    private final Mob mob;
    private final UUID squadId;
    private int next;

    public MoraleFleeGoal(Mob mob, UUID squadId) {
        this.mob = mob;
        this.squadId = squadId;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean fleeing() {
        Squad s = SquadManager.get(squadId);
        if (s == null || !mob.isAlive()) return false;
        Squad.Member me = s.member(mob.getUUID());
        return me != null && me.fleeing(mob.level().getGameTime());
    }

    @Override
    public boolean canUse() {
        if (++next < 4) return false;
        next = 0;
        return fleeing();
    }

    @Override
    public boolean canContinueToUse() { return fleeing(); }

    @Override
    public void start() { run(); }

    @Override
    public void tick() {
        if (mob.tickCount % 10 == 0 && (mob.getNavigation().isDone() || mob.tickCount % 20 == 0)) run();
    }

    private void run() {
        Squad s = SquadManager.get(squadId);
        flee(mob, s == null ? null : s.target());
    }

    /** Runs the mob away from the threat (the squad's target, else its own, else the nearest player); shared with the driver of mobs without goals. */
    static void flee(Mob mob, LivingEntity threat) {
        if (threat == null) threat = mob.getTarget();
        if (threat == null) threat = mob.level().getNearestPlayer(mob, 32.0);
        mob.setTarget(null);
        if (threat == null) return;
        V2 me = new V2(mob.getX(), mob.getZ());
        V2 away = me.minus(new V2(threat.getX(), threat.getZ())).unit();
        if (away.len() < 1e-9) away = new V2(1, 0);
        double[] turns = {0.0, 45.0, -45.0, 90.0, -90.0};
        for (double deg : turns) {
            double r = Math.toRadians(deg);
            V2 d = new V2(away.x() * Math.cos(r) - away.z() * Math.sin(r), away.x() * Math.sin(r) + away.z() * Math.cos(r));
            if (mob.getNavigation().moveTo(mob.getX() + d.x() * DISTANCE, mob.getY(), mob.getZ() + d.z() * DISTANCE, 1.3)) return;
        }
    }

    @Override
    public void stop() { mob.getNavigation().stop(); }
}
