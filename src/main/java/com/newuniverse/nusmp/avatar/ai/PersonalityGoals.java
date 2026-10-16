package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.AvatarPersonalityTraits;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Why: the movement flavour of the personalities that is not "attack things": Cowardly and low-health retreat, Lazy idling beside a still creator,
 * Gluttonous eating dropped food, Heroic defending villagers. Each goal reads the personality LIVE on every check (the Avatar's synced personality is
 * not set yet when its goals are registered), so installing all of them on every Avatar costs one cheap boolean per goal check.
 */
public final class PersonalityGoals {
    private PersonalityGoals() {}

    private static AvatarPersonalityTraits.Traits traits(AvatarEntity a) { return AvatarPersonalityTraits.combine(a.primary(), a.secondary()); }

    // ---------------------------------------------------------------- retreat / fear
    /** Backs away from a boss it fears (Cowardly) or from its target when its own health is below its retreat line. Never while frenzied. */
    public static final class Retreat extends Goal {
        private final AvatarEntity avatar;
        private final AvatarMind mind;
        private LivingEntity from;
        private int left;

        public Retreat(AvatarEntity avatar, AvatarMind mind) {
            this.avatar = avatar;
            this.mind = mind;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (avatar.level().isClientSide || avatar.frenzied() || avatar.tickCount % 4 != 0) return false;
            var t = traits(avatar);
            LivingEntity threat = null;
            if (t.fleesBosses() && mind.fear != null && mind.fear.isAlive() && avatar.distanceToSqr(mind.fear) < 20 * 20) threat = mind.fear;
            else if (t.retreatHp() > 0 && avatar.getTarget() != null && avatar.getHealth() < avatar.getMaxHealth() * t.retreatHp() && avatar.distanceToSqr(avatar.getTarget()) < 12 * 12)
                threat = avatar.getTarget();
            from = threat;
            return from != null && flee();
        }

        private boolean flee() {
            Vec3 pos = DefaultRandomPos.getPosAway(avatar, 16, 7, from.position());
            if (pos == null) return false;
            left = 80;
            return avatar.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.35);
        }

        @Override
        public boolean canContinueToUse() { return left > 0 && from != null && from.isAlive() && !avatar.getNavigation().isDone() && !avatar.frenzied(); }

        @Override public void tick() { left--; }

        @Override public void stop() { from = null; avatar.getNavigation().stop(); }
    }

    // ---------------------------------------------------------------- lazy
    /** Lazy: when the creator has stood still for five seconds and nothing threatens, the Avatar stops where it is and idles ("wants to sleep"). */
    public static final class LazyIdle extends Goal {
        private final AvatarEntity avatar;
        private final AvatarMind mind;

        public LazyIdle(AvatarEntity avatar, AvatarMind mind) {
            this.avatar = avatar;
            this.mind = mind;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (avatar.level().isClientSide || avatar.frenzied() || avatar.getTarget() != null || mind.creatorStillTicks < 100 || !traits(avatar).idlesWithCreator() || !AvatarGates.loyal(avatar)) return false;
            ServerPlayer c = avatar.creatorPlayer();
            return c != null && c.level() == avatar.level() && avatar.distanceToSqr(c) < 10 * 10;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void start() { avatar.getNavigation().stop(); }
    }

    // ---------------------------------------------------------------- glutton
    /** Gluttonous: walks to dropped food within 8 blocks and eats one piece (heals a little). */
    public static final class EatDroppedFood extends Goal {
        private final AvatarEntity avatar;
        private ItemEntity food;

        public EatDroppedFood(AvatarEntity avatar) {
            this.avatar = avatar;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (avatar.level().isClientSide || avatar.frenzied() || avatar.getTarget() != null || avatar.tickCount % 20 != 0 || !traits(avatar).eatsFood()) return false;
            List<ItemEntity> l = avatar.level().getEntitiesOfClass(ItemEntity.class, avatar.getBoundingBox().inflate(8), i -> i.isAlive() && i.getItem().has(DataComponents.FOOD));
            food = l.isEmpty() ? null : l.stream().min((a, b) -> Double.compare(a.distanceToSqr(avatar), b.distanceToSqr(avatar))).get();
            return food != null;
        }

        @Override public boolean canContinueToUse() { return food != null && food.isAlive() && avatar.getTarget() == null; }

        @Override public void start() { avatar.getNavigation().moveTo(food, 1.15); }

        @Override
        public void tick() {
            if (food == null) return;
            avatar.getLookControl().setLookAt(food);
            if (avatar.distanceToSqr(food) > 2.25) { if (avatar.tickCount % 10 == 0) avatar.getNavigation().moveTo(food, 1.15); return; }
            FoodProperties fp = food.getItem().get(DataComponents.FOOD);
            food.getItem().shrink(1);
            if (food.getItem().isEmpty()) food.discard();
            if (fp != null) avatar.heal(Math.max(1f, fp.nutrition() * 0.5f));
            avatar.level().playSound(null, avatar.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8f, 1.0f);
            food = null;
        }

        @Override public void stop() { food = null; avatar.getNavigation().stop(); }
    }

    // ---------------------------------------------------------------- heroic
    /** Heroic: a hostile mob that is going for a villager within 20 blocks becomes the Avatar's target. */
    public static final class DefendVillagers extends Goal {
        private final AvatarEntity avatar;

        public DefendVillagers(AvatarEntity avatar) {
            this.avatar = avatar;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (avatar.level().isClientSide || avatar.getTarget() != null || avatar.tickCount % 20 != 0 || !traits(avatar).defendsVillagers()) return false;
            Mob attacker = null;
            double best = Double.MAX_VALUE;
            for (Mob m : avatar.level().getEntitiesOfClass(Mob.class, avatar.getBoundingBox().inflate(20), x -> x instanceof Enemy && x.isAlive() && x.getTarget() instanceof AbstractVillager)) {
                double d = m.distanceToSqr(avatar);
                if (d < best) { best = d; attacker = m; }
            }
            if (attacker == null) return false;
            avatar.setTarget(attacker);
            return true;
        }

        @Override public boolean canContinueToUse() { return false; }
    }

    /** Plain personalities that this class implements goals for, for the docs and tests. */
    public static List<AvatarPersonality> covered() {
        return List.of(AvatarPersonality.COWARDLY, AvatarPersonality.LAZY, AvatarPersonality.GLUTTONOUS, AvatarPersonality.HEROIC);
    }
}
