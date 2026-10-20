package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.UUID;

/**
 * Why: the four movement goals and the focus goal share their plumbing, and that plumbing is where a cheap goal turns into an expensive one. A goal's {@code canUse}
 * runs on EVERY tick for every goal that is not running, so none of these goals thinks in it: each asks the squad for a snapshot at most once every five ticks
 * ({@link #decision}), asks the pure {@link RoleMotion} what the role should do, and answers from the cached result in between. The goals hold the squad's id and not the
 * squad (a squad can be forgotten while a mob lives), look it up in {@link SquadManager} when they need it, and answer "no" when it is gone, so a mob whose squad
 * ended is just a mob again. Every goal is additive: it takes a MOVE or TARGET flag only while it has something to say, and says nothing (so the mob's own attack and
 * wander goals act) whenever {@link RoleMotion} returns nothing.
 */
abstract class SquadGoal extends Goal {
    /** Ticks a cached decision is trusted. */
    static final int REFRESH = 5;

    final Mob mob;
    final UUID squadId;
    private long decidedAt = Long.MIN_VALUE;
    private Move cached;

    SquadGoal(Mob mob, UUID squadId) {
        this.mob = mob;
        this.squadId = squadId;
    }

    final Squad squad() { return SquadManager.get(squadId); }

    final long now() { return mob.level().getGameTime(); }

    /** The member record of this mob in its squad, or null. */
    final Squad.Member me() {
        Squad s = squad();
        return s == null ? null : s.member(mob.getUUID());
    }

    /** The role decision for this mob, recomputed at most every {@link #REFRESH} ticks; null means "nothing special". */
    final Move decision() {
        long now = now();
        if (now - decidedAt < REFRESH && now >= decidedAt) return cached;
        decidedAt = now;
        cached = null;
        Squad s = squad();
        if (s == null || !mob.isAlive()) return null;
        Squad.Member me = s.member(mob.getUUID());
        if (me == null) return null;
        RoleMotion.Ctx c = s.contextFor(mob, now);
        if (c == null) return null;
        if (c.target() == null) me.waited = 0;
        else if (!c.tankEngaged()) me.waited += REFRESH;
        if (mob.getLastHurtByMob() != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < 60) me.waited = Math.max(me.waited, RoleMotion.MAX_WAIT);       // hit while it waited: no more waiting
        cached = RoleMotion.decide(c).orElse(null);
        return cached;
    }

    /** Forgets the cached decision so the next call recomputes (a goal that just ended must not trust a stale answer). */
    final void forget() { decidedAt = Long.MIN_VALUE; }

    final boolean fleeing() {
        Squad.Member me = me();
        return me != null && me.fleeing(now());
    }

    /** Is the decision of this kind? */
    final boolean wants(Kind... kinds) {
        Move m = decision();
        if (m == null || fleeing()) return false;
        for (Kind k : kinds) if (m.kind() == k) return true;
        return false;
    }

    final void walk(Move m) {
        if (m.to() == null) return;
        LivingEntity t = squad() == null ? null : squad().target();
        double y = t != null ? t.getY() : mob.getY();
        if (m.kind() == Kind.FORM || m.kind() == Kind.RETREAT) y = mob.getY();
        mob.getNavigation().moveTo(m.to().x(), y, m.to().z(), m.speed());
    }

    final void face(LivingEntity t) {
        if (t != null) mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
    }
}
