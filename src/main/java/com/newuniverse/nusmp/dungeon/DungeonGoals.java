package com.newuniverse.nusmp.dungeon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** The behaviours of the dungeon's creatures (docs/living_dungeon.md section 1, kept as history). */
public final class DungeonGoals {
    private DungeonGoals() {}

    /**
     * Hunt for sustenance: a hungry creature (hunger at or below the threshold) picks the nearest prey within 16 blocks, runs it down,
     * and on the kill is fed (hunger +50). A full one does not look for prey at all.
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
                    mob.feed(50);
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
