package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: this is how two factions end up fighting in a hall without any player in it. A monster picks its targets with TARGET goals; adding one more
 * at priority 3 (below the hurt-by goal at 1 and the nearest-player goal at 2 that vanilla gives hostile mobs) means a monster looks for a rival only
 * when nothing more urgent has its attention, and any higher-priority goal that wants the TARGET flag (a player in reach, a monster that struck it)
 * takes over from this one on the next tick without any code here. It looks once per {@code scanEveryTicks} (staggered per monster by a random
 * offset so a whole room does not scan on the same tick): one entity query of the mobs in a box, then the nearest one the rules allow
 * ({@link TurfRules}): a rival faction within range (farther when it stands in this monster's native room), in line of sight, wild, attackable.
 * It yields while a player is within {@code playerYieldRadius}, because the player is who the monster should be hunting; it only starts when the
 * monster has no live target; it lets go when the target dies, is retargeted by someone else, leaves the dimension or the reach, stays out of sight
 * for four seconds, stops being a rival, or a player comes close. It never clears a target it did not set. A monster whose standing changed
 * (a boss tag, a new owner) simply finds nothing at its next look. Time is the monster's own {@code tickCount} (not the level clock): a scan is due
 * after that many ticks OF THIS MONSTER, which is what a goal wants anyway (a monster in a slow area scans at its own pace) and what lets the
 * in-game self-test drive a monster tick by tick inside one command. Every method swallows a runtime exception (logged once) and answers "no",
 * because an exception out of a goal would crash the server tick. Allocation per look: one box and the query's list.
 */
final class TurfWarGoal extends TargetGoal {
    private static final int UNSEEN_LIMIT = 8, CHECK_EVERY = 10;

    private final Mob self;
    private final Predicate<Mob> notMe;
    private int nextScan, nextCheck;
    private LivingEntity victim;
    private double giveUpSq;
    private int unseen;
    private Room home;

    TurfWarGoal(Mob self) {
        super(self, false);
        this.self = self;
        this.notMe = m -> m != this.self && m.isAlive();
        setFlags(EnumSet.of(Flag.TARGET));
        this.nextScan = self.tickCount + self.getRandom().nextInt(Math.max(1, scanEvery()));
    }

    private static int scanEvery() { return Math.max(10, EcoConfig.get(EcoConfig.SCAN_EVERY)); }

    @Override
    public boolean canUse() {
        try {
            int now = self.tickCount;
            if (now < nextScan) return false;
            nextScan = now + scanEvery() + self.getRandom().nextInt(5);
            LivingEntity current = self.getTarget();
            if (current != null && current.isAlive()) return false;
            victim = look();
            return victim != null;
        } catch (RuntimeException ex) {
            TurfWars.warnOnce("a turf-war look failed", ex);
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        try {
            LivingEntity v = victim;
            if (v == null || self.getTarget() != v || !v.isAlive() || v.level() != self.level()) return false;
            if (self.distanceToSqr(v) > giveUpSq) return false;
            int now = self.tickCount;
            if (now < nextCheck) return true;
            nextCheck = now + CHECK_EVERY;
            return stillWorthIt(v);
        } catch (RuntimeException ex) {
            TurfWars.warnOnce("a turf-war check failed", ex);
            return false;
        }
    }

    @Override
    public void start() {
        try {
            self.setTarget(victim);
            super.start();
            unseen = 0;
            nextCheck = self.tickCount + CHECK_EVERY;
        } catch (RuntimeException ex) {
            TurfWars.warnOnce("a turf-war start failed", ex);
        }
    }

    @Override
    public void stop() {
        try {
            if (victim != null && self.getTarget() == victim) self.setTarget(null);
        } catch (RuntimeException ex) {
            TurfWars.warnOnce("a turf-war stop failed", ex);
        } finally {
            victim = null;
        }
    }

    /** The nearest mob this one may go for right now, or null. */
    private LivingEntity look() {
        if (!com.newuniverse.nusmp.labyrinth.mega.poise.Passivity.turfWarsRun() || !(self.level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return null;       // 1.107.0: off while the dungeon is passive
        String own = FactionRegistry.factionOf(self.getType());
        if (own == null || !TurfEligibility.wild(self)) return null;
        FactionGraph graph = FactionRegistry.graph();
        FactionDef def = graph.def(own);
        if (def == null) return null;
        int range = TurfRules.range(def, EcoConfig.get(EcoConfig.RANGE)), bonus = TurfRules.nativeBonus(def, EcoConfig.get(EcoConfig.NATIVE_BONUS));
        if (range + bonus <= 0 || playerNear(level)) return null;
        Room room = nativeRoom(level, own);
        double span = range + bonus;
        List<Mob> near = level.getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(span, Math.min(span, 10.0), span), notMe);
        Mob best = null;
        double bestSq = Double.MAX_VALUE;
        for (int i = 0, n = near.size(); i < n; i++) {
            Mob m = near.get(i);
            String other = FactionRegistry.factionOf(m.getType());
            if (other == null) continue;
            double reach = TurfRules.reach(graph, def, other, room != null && room.contains(m.blockPosition()), range, bonus);
            if (reach <= 0.0) continue;
            double dsq = self.distanceToSqr(m);
            if (dsq > reach * reach || dsq >= bestSq) continue;
            if (!fair(m)) continue;
            best = m;
            bestSq = dsq;
        }
        if (best != null) giveUpSq = square(TurfRules.giveUpDistance(range, bonus));
        return best;
    }

    /** The checks that cost more than a comparison, done only for a candidate that is already the nearest so far. */
    private boolean fair(Mob m) {
        return TurfEligibility.wild(m) && self.canAttack(m) && self.canAttackType(m.getType()) && !self.isAlliedTo(m) && self.getSensing().hasLineOfSight(m);
    }

    /** The periodic re-check of a running hunt: still allowed, still a rival, no player close, not out of sight for too long. */
    private boolean stillWorthIt(LivingEntity v) {
        if (!com.newuniverse.nusmp.labyrinth.mega.poise.Passivity.turfWarsRun() || !(self.level() instanceof ServerLevel level) || !Labyrinth.inMega(level) || playerNear(level)) return false;
        String own = FactionRegistry.factionOf(self.getType()), other = FactionRegistry.factionOf(v.getType());
        if (own == null || other == null || !TurfEligibility.wild(self) || !(v instanceof Mob victimMob) || !TurfEligibility.wild(victimMob)) return false;
        FactionGraph graph = FactionRegistry.graph();
        Room room = nativeRoom(level, own);
        if (TurfRules.relation(graph, graph.def(own), other, room != null && room.contains(v.blockPosition())) == TurfRules.Relation.NONE) return false;
        if (!self.canAttack(v)) return false;
        if (self.getSensing().hasLineOfSight(v)) unseen = 0;
        else if (++unseen > UNSEEN_LIMIT) return false;
        return true;
    }

    /** Is a player (survival or adventure) within the yield radius? Players keep priority over every quarrel. */
    private boolean playerNear(ServerLevel level) {
        int radius = EcoConfig.get(EcoConfig.PLAYER_YIELD);
        return radius > 0 && level.getNearestPlayer(self, radius) != null;
    }

    /** The room this monster stands in when that room is native to its faction, else null. The room is cached while the monster stays inside its box. */
    private Room nativeRoom(ServerLevel level, String own) {
        BlockPos pos = self.blockPosition();
        Room r = home;
        if (r == null || !r.contains(pos)) {
            r = MegaDungeon.roomAt(level, pos);
            home = r;
        }
        return r != null && own.equals(r.faction) ? r : null;
    }

    private static double square(double d) { return d * d; }
}
