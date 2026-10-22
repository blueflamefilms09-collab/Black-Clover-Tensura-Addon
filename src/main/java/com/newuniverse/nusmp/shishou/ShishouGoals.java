package com.newuniverse.nusmp.shishou;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Why: the five behaviours of the sword (docs/items/shishou.md). Each is a vanilla goal with a cheap guard, so a body that has nothing to do costs almost nothing:
 * {@link WanderAirGoal} (hovers and explores), {@link HuntHostilesGoal} (a nearest-attackable target goal that only ever picks the creatures {@link ShishouEntity#isValidTarget} allows),
 * {@link TelekineticStrikeGoal} (charges at the target and strikes in reach), {@link StayByWielderGoal} (a thrown sword hovers near its wielder and comes back when it is too far) and
 * {@link DormantPedestalGoal} (after its kills, days or mana, finds the ground by a BOUNDED search, flies to it, embeds itself and goes static). All state is on the server.
 */
public final class ShishouGoals {
    private ShishouGoals() {}

    /** Goal 1: hovers and explores the air around it (wild, or a thrown sword with nothing to do and no wielder in the world). */
    public static class WanderAirGoal extends WaterAvoidingRandomFlyingGoal {
        private final ShishouEntity sword;

        public WanderAirGoal(ShishouEntity sword) {
            super(sword, 1.0);
            this.sword = sword;
        }

        @Override
        public boolean canUse() { return !sword.isDormant() && sword.getTarget() == null && (!sword.isMinion() || sword.owner() == null) && super.canUse(); }

        @Override
        public boolean canContinueToUse() { return !sword.isDormant() && sword.getTarget() == null && super.canContinueToUse(); }
    }

    /** Goal 2: the nearest hostile creature, never a player, a villager, a tamed creature or an ally. */
    public static class HuntHostilesGoal extends NearestAttackableTargetGoal<Mob> {
        private final ShishouEntity sword;

        public HuntHostilesGoal(ShishouEntity sword) {
            super(sword, Mob.class, 10, false, false, sword::isValidTarget);
            this.sword = sword;
        }

        @Override
        public boolean canUse() { return !sword.isDormant() && ShishouConfig.enabled() && (!sword.isMinion() || sword.owner() != null) && super.canUse(); }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = sword.getTarget();
            return !sword.isDormant() && t != null && t.isAlive() && sword.isValidTarget(t);
        }
    }

    /** Goal 3: charges at the target at high speed and strikes when it is in reach (the animation state and the damage are the entity's). */
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
        public void start() { sword.setState(ShishouRules.STATE_AGGRO); }

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
            if (sword.distanceToSqr(aim) > reach * reach) sword.getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, ShishouRules.CHARGE_SPEED);
            else if (sword.strikeReady()) sword.strike(t);
        }
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
