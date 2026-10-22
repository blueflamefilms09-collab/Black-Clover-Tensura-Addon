package com.newuniverse.nusmp.shishou;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Why: the behaviours of the sword (2.1.0: a fast hunter, see {@link ShishouHunt}) (docs/items/shishou.md). Each is a vanilla goal with a cheap guard, so a body that has nothing to do costs almost nothing:
 * {@link WanderAirGoal} (hovers and explores), {@link HuntHostilesGoal} (a bounded box-query target goal that only ever picks the creatures {@link ShishouEntity#isValidTarget} allows),
 * {@link TelekineticStrikeGoal} (charges at the target and strikes in reach), {@link StayByWielderGoal} (a thrown sword hovers near its wielder and comes back when it is too far) and
 * {@link DormantPedestalGoal} (after its kills, days or mana, finds the ground by a BOUNDED search, flies to it, embeds itself and goes static). All state is on the server.
 */
public final class ShishouGoals {
    private ShishouGoals() {}

    /** Goal 1: hovers and explores the air around it (wild, or a thrown sword with nothing to do and no wielder in the world). */
    public static class WanderAirGoal extends WaterAvoidingRandomFlyingGoal {
        private final ShishouEntity sword;

        public WanderAirGoal(ShishouEntity sword) {
            super(sword, 1.6);
            this.sword = sword;
        }

        @Override
        public boolean canUse() { return !sword.isDormant() && sword.getTarget() == null && (!sword.isMinion() || sword.owner() == null) && super.canUse(); }

        @Override
        public boolean canContinueToUse() { return !sword.isDormant() && sword.getTarget() == null && super.canContinueToUse(); }
    }

    /**
     * Goal 2 (2.1.0): the prey. Every {@link ShishouHunt#SCAN_TICKS} ticks (config) while he has no target, ONE box query around himself (a thrown sword: around its wielder) in a cube of the hunt radius, and
     * of the creatures it returns at most {@link ShishouHunt#MAX_EXAMINED} (config) are looked at, the best by {@link ShishouHunt#score} wins: the nearest threat, bigger and player-hurting ones first. Never a player,
     * a villager, a tamed creature or an ally ({@link ShishouEntity#isValidTarget}). After a kill the next look is immediate. No chunk is loaded, nothing is sorted, nothing is allocated but the box and its list.
     */
    public static class HuntHostilesGoal extends Goal {
        private final ShishouEntity sword;
        private LivingEntity found;

        public HuntHostilesGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (sword.isDormant() || !ShishouConfig.enabled() || sword.isMinion() && sword.owner() == null || !(sword.level() instanceof ServerLevel sl)) return false;
            LivingEntity cur = sword.getTarget();
            if (cur != null && cur.isAlive() && sword.isValidTarget(cur)) return false;
            long now = sl.getGameTime();
            if (now < sword.huntNextScan()) return false;
            sword.setHuntNextScan(now + ShishouHunt.scanGap(ShishouConfig.huntScanTicks(), sword.getId()));
            found = scan(sword, sl, now);
            return found != null;
        }

        @Override
        public void start() { sword.setTarget(found); }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = sword.getTarget();
            return !sword.isDormant() && t != null && t.isAlive() && sword.isValidTarget(t) && !sword.isIgnored(t);
        }

        @Override
        public void stop() {
            found = null;
            LivingEntity t = sword.getTarget();
            if (t == null || !t.isAlive() || !sword.isValidTarget(t) || sword.isIgnored(t)) sword.setTarget(null);
        }
    }

    /** One look for prey: a single box query, at most the configured number of creatures examined, the best by score; null when there is none. */
    static LivingEntity scan(ShishouEntity s, ServerLevel sl, long now) {
        boolean minion = s.isMinion();
        ServerPlayer o = minion ? s.owner() : null;
        if (minion && (o == null || o.level() != sl)) return null;
        double r = ShishouHunt.scanRadius(minion, ShishouConfig.huntRadius(), ShishouConfig.minionLeash()), h = ShishouHunt.scanHeight(r);
        Vec3 c = minion ? o.position() : s.position();
        AABB box = new AABB(c.x - r, c.y - h, c.z - r, c.x + r, c.y + h, c.z + r);
        List<Mob> list = sl.getEntitiesOfClass(Mob.class, box, Mob::isAlive);
        int cap = ShishouConfig.huntExamined(), n = 0;
        Mob best = null;
        double bestScore = Double.MAX_VALUE;
        for (Mob m : list) {
            if (++n > cap) break;
            if (s.isIgnored(m) || !s.isValidTarget(m)) continue;
            double sc = ShishouHunt.score(Math.sqrt(m.distanceToSqr(s)), m.getMaxHealth(), m.getTarget() instanceof Player);
            if (sc < bestScore) {
                bestScore = sc;
                best = m;
            }
        }
        return best;
    }

    /**
     * Goal 3 (2.1.0): the telekinetic charge. Direct 3D steering, no path finding (a flying sword needs none and a path that cannot be found never stalls it): the velocity is blended toward the target with a
     * short acceleration curve ({@link ShishouHunt#chargeSpeed}, supersonic feel: 23 to 38 blocks a second), slows near the target ({@link ShishouHunt#arrive}), strikes in reach on a level-scaled cooldown, and a
     * blocked charge nudges up and aside, then gives the target up for a while ({@link ShishouHunt#unstick}).
     */
    public static class TelekineticStrikeGoal extends Goal {
        private final ShishouEntity sword;

        public TelekineticStrikeGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = sword.getTarget();
            return !sword.isDormant() && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void start() {
            sword.setState(ShishouRules.STATE_AGGRO);
            sword.beginCharge();
        }

        @Override
        public void stop() {
            if (!sword.isDormant()) sword.setState(ShishouRules.STATE_IDLE);
            sword.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = sword.getTarget();
            if (t == null) return;
            double reach = ShishouRules.REACH + t.getBbWidth() * 0.5;
            Vec3 aim = new Vec3(t.getX(), t.getY(0.5), t.getZ());
            if (sword.distanceToSqr(aim) > reach * reach) sword.charge(aim);
            else {
                sword.hoverOn(aim);
                if (sword.strikeReady()) sword.strike(t);
            }
        }
    }

    /** Goal 4b: a wild sword with no prey in range flies toward the nearest player (monsters spawn near players) instead of wandering at random; every second one nearest-player lookup at most. */
    public static class PatrolGoal extends Goal {
        private final ShishouEntity sword;
        private Vec3 dest;
        private long nextLook;

        public PatrolGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (sword.isDormant() || sword.isMinion() || sword.getTarget() != null || !ShishouConfig.patrol() || !ShishouConfig.enabled() || !(sword.level() instanceof ServerLevel sl)) return false;
            long now = sl.getGameTime();
            if (now < nextLook) return false;
            nextLook = now + ShishouHunt.PATROL_PERIOD;
            Player p = sl.getNearestPlayer(sword, ShishouHunt.PATROL_MAX);
            if (p == null) return false;
            double d = Math.sqrt(sword.distanceToSqr(p)), leg = ShishouHunt.patrolLeg(d);
            if (leg <= 0.0) return false;
            Vec3 dir = p.position().add(0, 6, 0).subtract(sword.position()).normalize();
            dest = sword.position().add(dir.scale(Math.min(leg, 32.0)));
            return true;
        }

        @Override
        public boolean canContinueToUse() { return dest != null && sword.getTarget() == null && !sword.isDormant() && sword.distanceToSqr(dest) > 9.0 && sword.level().getGameTime() < nextLook + 3L * ShishouHunt.PATROL_PERIOD; }

        @Override
        public void tick() { if (dest != null) sword.fly(dest, 0.9); }

        @Override
        public void stop() { dest = null; }
    }

    /** A thrown sword with no target hovers a few blocks above its wielder's shoulder and follows; far away it flies back (the service teleports it when it is very far). */
    public static class StayByWielderGoal extends Goal {
        private final ShishouEntity sword;

        public StayByWielderGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (sword.isDormant() || !sword.isMinion() || sword.getTarget() != null) return false;
            ServerPlayer o = sword.owner();
            return o != null && o.level() == sword.level() && sword.distanceToSqr(o) > 9.0;
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            ServerPlayer o = sword.owner();
            if (o == null) return;
            double speed = sword.distanceToSqr(o) > (double) ShishouRules.MINION_RETURN * ShishouRules.MINION_RETURN ? ShishouRules.CHARGE_SPEED : 1.2;
            sword.getMoveControl().setWantedPosition(o.getX() + 0.8, o.getY() + 1.9, o.getZ() - 0.8, speed);
        }
    }

    /** Goal 5: after the kills, the days or the mana, find the ground (a bounded search), fly to it, embed, go static. */
    public static class DormantPedestalGoal extends Goal {
        private final ShishouEntity sword;
        private int giveUp;

        public DormantPedestalGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() { return sword.wantsDormant() && !sword.isDormant() && !sword.isMinion(); }

        @Override
        public boolean canContinueToUse() { return sword.wantsDormant() && !sword.isDormant() && giveUp > 0; }

        @Override
        public void start() {
            sword.setTarget(null);
            giveUp = 400;
            sword.setGroundSpot(findGround(sword));
            if (sword.groundSpot() == null) giveUp = 0;
        }

        @Override
        public void stop() { if (!sword.isDormant() && sword.groundSpot() == null) sword.setDormantPending(false); }

        @Override
        public void tick() {
            giveUp--;
            BlockPos g = sword.groundSpot();
            if (g == null) return;
            double tx = g.getX() + 0.5, ty = g.getY() + 1.0, tz = g.getZ() + 0.5;
            if (sword.distanceToSqr(tx, ty + 0.5, tz) < 2.25 || giveUp <= 0) {
                ShishouService.embed(sword, g);
                return;
            }
            sword.getMoveControl().setWantedPosition(tx, ty + 0.5, tz, 1.1);
        }
    }

    /**
     * The nearest solid ground, bounded: straight down up to {@link ShishouRules#GROUND_SEARCH} blocks, then up to {@link ShishouRules#GROUND_SIDE_TRIES} spots within
     * {@link ShishouRules#GROUND_SIDE_RADIUS} blocks. Only loaded chunks are read (no chunk is loaded). At most about 330 block reads, once, when the sword settles. Returns the ground block, or null.
     */
    public static BlockPos findGround(ShishouEntity e) {
        Level level = e.level();
        if (!(level instanceof ServerLevel)) return null;
        BlockPos here = e.blockPosition();
        BlockPos g = column(level, here.getX(), here.getY(), here.getZ());
        if (g != null) return g;
        java.util.Random r = new java.util.Random(here.asLong() ^ 0x5348L);
        for (int i = 0; i < ShishouRules.GROUND_SIDE_TRIES; i++) {
            int dx = r.nextInt(ShishouRules.GROUND_SIDE_RADIUS * 2 + 1) - ShishouRules.GROUND_SIDE_RADIUS, dz = r.nextInt(ShishouRules.GROUND_SIDE_RADIUS * 2 + 1) - ShishouRules.GROUND_SIDE_RADIUS;
            g = column(level, here.getX() + dx, here.getY(), here.getZ() + dz);
            if (g != null) return g;
        }
        return null;
    }

    private static BlockPos column(Level level, int x, int y, int z) {
        if (!level.hasChunkAt(new BlockPos(x, y, z))) return null;
        int bottom = Math.max(level.getMinBuildHeight(), y - ShishouRules.GROUND_SEARCH);
        for (int yy = Math.min(y, level.getMaxBuildHeight() - 1); yy >= bottom; yy--) {
            BlockPos p = new BlockPos(x, yy, z);
            if (level.getBlockState(p).isFaceSturdy(level, p, net.minecraft.core.Direction.UP) && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) return p;
        }
        return null;
    }
}
