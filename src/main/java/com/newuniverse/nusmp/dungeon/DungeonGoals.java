package com.newuniverse.nusmp.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** The behaviours of the Living Dungeon's creatures (docs/living_dungeon.md section 1). */
public final class DungeonGoals {
    private DungeonGoals() {}

    /**
     * Hunt for sustenance: a hungry creature (hunger at or below the threshold) picks the nearest prey within 16 blocks, runs it down,
     * and on the kill is fed (hunger +50, the chunk's magicules +20). A full one does not look for prey at all.
     */
    public static final class HuntForSustenance extends Goal {
        private final DungeonCreature mob;
        private final double speed;
        private LivingEntity prey;
        private int repath, cooldown;

        public HuntForSustenance(DungeonCreature mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.tier() < 2 || !mob.hungry()) return false;
            List<LivingEntity> found = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16), e -> e != mob && e.isAlive() && mob.isPrey(e));
            if (found.isEmpty()) return false;
            found.sort(Comparator.comparingDouble(mob::distanceToSqr));
            prey = found.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() { return prey != null && prey.isAlive() && !mob.full() && mob.distanceToSqr(prey) < 24 * 24; }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(prey, 30f, 30f);
            if (--repath <= 0) { repath = 10; mob.getNavigation().moveTo(prey, speed); }
            if (cooldown > 0) cooldown--;
            double reach = mob.getBbWidth() * 2.0 * mob.getBbWidth() * 2.0 + prey.getBbWidth();
            if (cooldown == 0 && mob.distanceToSqr(prey) <= reach) {
                cooldown = 20;
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(prey);
                if (!prey.isAlive()) {
                    mob.feed(50, 20f);
                    prey = null;
                }
            }
        }

        @Override
        public void stop() {
            prey = null;
            mob.getNavigation().stop();
        }
    }

    /** A scavenger goes for the nearest carcass or Dungeon Mycelium within 10 blocks and eats it. */
    public static final class Scavenge extends Goal {
        private final DungeonCreature mob;
        private final double speed;
        private BlockPos food;
        private int give;

        public Scavenge(DungeonCreature mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean edible(BlockPos p) {
            var b = mob.level().getBlockState(p);
            return b.is(DungeonBlocks.CARCASS.get()) || b.is(DungeonBlocks.DUNGEON_MYCELIUM.get());
        }

        @Override
        public boolean canUse() {
            if (mob.tier() != 1 || mob.full() || (mob.tickCount + mob.getId()) % 40 != 0) return false;
            BlockPos at = mob.blockPosition();
            double best = Double.MAX_VALUE;
            food = null;
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        BlockPos p = at.offset(dx, dy, dz);
                        if (edible(p)) {
                            double d = p.distSqr(at);
                            if (d < best) { best = d; food = p; }
                        }
                    }
                }
            }
            return food != null;
        }

        @Override public boolean canContinueToUse() { return food != null && give < 200 && edible(food) && !mob.full(); }

        @Override public void start() { give = 0; }

        @Override
        public void tick() {
            give++;
            mob.getNavigation().moveTo(food.getX() + 0.5, food.getY(), food.getZ() + 0.5, speed);
            if (mob.distanceToSqr(food.getX() + 0.5, food.getY() + 0.5, food.getZ() + 0.5) < 2.4 && mob.level() instanceof ServerLevel sl) {
                if (sl.getBlockState(food).is(DungeonBlocks.CARCASS.get())) {
                    CarcassBlock.nibble(sl, food);
                    mob.feed(30, 8f);
                } else {
                    sl.removeBlock(food, false);
                    mob.feed(25, 0f);
                }
                food = null;
            }
        }

        @Override public void stop() { food = null; mob.getNavigation().stop(); }
    }

    /** A full apex predator sleeps: it stands still and ignores everything until it is hurt or hungry again. */
    public static final class Sleep extends Goal {
        private final DungeonCreature mob;

        public Sleep(DungeonCreature mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override public boolean canUse() { return mob.tier() == 3 && mob.full() && mob.getTarget() == null; }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void start() { mob.getNavigation().stop(); }
    }

    /** Players are prey only to a hungry creature, or to one defending its den (a player within 6 blocks of where it lives). */
    public static final class HostileToPlayers extends NearestAttackableTargetGoal<Player> {
        public HostileToPlayers(DungeonCreature mob) {
            super(mob, Player.class, 10, true, false, p -> !p.isSpectator() && !((Player) p).isCreative() && (mob.hungry() || mob.nearHome(p)));
        }
    }
}
