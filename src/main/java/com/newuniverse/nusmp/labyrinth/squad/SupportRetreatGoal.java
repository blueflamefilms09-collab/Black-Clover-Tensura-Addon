package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: a healer is worth more alive at the back than dead at the front. While the squad fights, this goal keeps the support member 8 to 12 blocks from the nearest
 * threat ({@link SquadGeometry#retreatPoint}): inside that band it backs away along the line from the threat, bent toward the squad's centre so it runs to its friends
 * and not into a corner; outside it comes back; inside it stands and works. The work is {@link SupportCast}: heal the wounded, else buff, each on its own cooldown, using
 * the mod's capped {@code BalanceLaw.heal} and never a boss. It runs for the whole fight (a healer who only cast when idle would never cast), claims the MOVE flag
 * only to walk and gives it back at once when the squad has nothing to fear, and leaves the mob's own ranged attack goals alone (a caster support still shoots from where it stands).
 */
public final class SupportRetreatGoal extends SquadGoal {
    public SupportRetreatGoal(Mob mob, UUID squadId) {
        super(mob, squadId);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean fighting() {
        Squad s = squad();
        return s != null && s.target() != null && mob.isAlive();
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % REFRESH != 0) return false;
        Squad.Member me = me();
        if (me == null || me.role != SquadRole.SUPPORT || !fighting()) return false;
        return wants(Kind.RETREAT, Kind.HOLD);
    }

    @Override
    public boolean canContinueToUse() { return fighting() && me() != null && me().role == SquadRole.SUPPORT && wants(Kind.RETREAT, Kind.HOLD); }

    @Override
    public void start() { act(); }

    @Override
    public void tick() {
        Squad s = squad();
        if (s != null && s.target() != null) face(s.target());
        if (mob.tickCount % REFRESH == 0) act();
    }

    private void act() {
        Squad s = squad();
        Squad.Member me = me();
        if (s == null || me == null) return;
        Move m = decision();
        if (m != null && m.kind() == Kind.RETREAT && (mob.getNavigation().isDone() || mob.tickCount % 10 == 0)) walk(m);
        else if (m != null && m.kind() == Kind.HOLD) mob.getNavigation().stop();
        if (mob.level() instanceof ServerLevel level) SupportCast.tick(level, s, mob, me, now());
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        forget();
    }
}
