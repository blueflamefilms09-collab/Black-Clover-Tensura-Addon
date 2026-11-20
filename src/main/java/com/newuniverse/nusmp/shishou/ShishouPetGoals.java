package com.newuniverse.nusmp.shishou;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Why: the two goals of the PET MODE of the thrown Shishou (server setting {@code pet.mode}; off = both goals never run and the old goals behave as before). {@link PetOwnerTargetGoal} is the wolf's idea: what the
 * wielder hit and what hit the wielder becomes his target for {@link ShishouPet#OWNER_TARGET_TICKS} ticks (never a player, a tamed creature, an ally, a villager, and a peaceful creature the wielder punched is not
 * one either: {@link ShishouTargets#petMayAttack}). {@link PetKeepGoal} is the follow: he comes to the wielder's shoulder when he is farther than the state's start distance, holds his place on a STAY order, and
 * blinks to the shoulder when a flight makes no progress for {@link ShishouPet#STUCK_BLINK_TICKS} ticks while the wielder is far (no stalling on a wall). Both are throttled: a look at the wielder's last-hurt
 * pair is two field reads once every {@link #LOOK_GAP} ticks; no entity query, no path finding.
 */
public final class ShishouPetGoals {
    private ShishouPetGoals() {}

    static final int LOOK_GAP = 5;

    /** Target goal 0: the wielder's attacker first, then what the wielder hit. */
    public static class PetOwnerTargetGoal extends Goal {
        private final ShishouEntity sword;
        private LivingEntity found;
        private long nextLook;

        public PetOwnerTargetGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!sword.isPet() || sword.isDormant() || !(sword.level() instanceof ServerLevel sl) || !ShishouPet.mayHunt(sword.petState())) return false;
            long now = sl.getGameTime();
            if (now < nextLook) return false;
            nextLook = now + LOOK_GAP;
            ServerPlayer o = sword.owner();
            if (o == null || o.level() != sl) return false;
            LivingEntity cur = sword.getTarget();
            if (cur != null && cur.isAlive() && sword.isValidTarget(cur)) return false;
            found = pick(o, ShishouPet.Reason.OWNER_ATTACKER, o.getLastHurtByMob(), o.getLastHurtByMobTimestamp());
            if (found == null) found = pick(o, ShishouPet.Reason.OWNER_HIT, o.getLastHurtMob(), o.getLastHurtMobTimestamp());
            return found != null;
        }

        private LivingEntity pick(ServerPlayer o, ShishouPet.Reason why, LivingEntity t, int stamp) {
            if (t == null || !ShishouPet.fresh(stamp, o.tickCount) || sword.isIgnored(t)) return null;
            if (!ShishouTargets.petMayAttack(why, o, t)) return null;
            double line = ShishouConfig.minionLeash() + ShishouPet.FAR_SLACK + (sword.order() == ShishouPet.Order.ATTACK ? ShishouPet.ATTACK_EXTRA : 0.0);
            return o.distanceToSqr(t) <= line * line ? t : null;
        }

        @Override
        public void start() {
            if (found != null) sword.setPetTarget(found, sword.level().getGameTime() + ShishouPet.OWNER_TARGET_TICKS);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = sword.getTarget();
            return sword.isPet() && !sword.isDormant() && t != null && t.isAlive() && sword.isValidTarget(t) && !sword.isIgnored(t);
        }

        @Override
        public void stop() {
            found = null;
            LivingEntity t = sword.getTarget();
            if (t == null || !t.isAlive() || !sword.isValidTarget(t) || sword.isIgnored(t)) sword.setTarget(null);
        }
    }

    /** Goal 3 (before the old follow goal, which stands down in pet mode): follow, stay, retreat close, blink out of a stall. */
    public static class PetKeepGoal extends Goal {
        private final ShishouEntity sword;

        public PetKeepGoal(ShishouEntity sword) {
            this.sword = sword;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private ServerPlayer wielder() {
            ServerPlayer o = sword.owner();
            return o != null && o.level() == sword.level() ? o : null;
        }

        @Override
        public boolean canUse() {
            if (!sword.isPet() || sword.isDormant() || sword.getTarget() != null) return false;
            ServerPlayer o = wielder();
            if (o == null) return false;
            if (sword.order() == ShishouPet.Order.STAY || sword.petState() == ShishouPet.State.STAY) return true;      // holds his place
            double start = ShishouPet.followStart(sword.petState());
            return sword.distanceToSqr(o) > start * start;
        }

        @Override
        public boolean canContinueToUse() {
            if (!sword.isPet() || sword.isDormant() || sword.getTarget() != null) return false;
            ServerPlayer o = wielder();
            if (o == null) return false;
            if (sword.order() == ShishouPet.Order.STAY || sword.petState() == ShishouPet.State.STAY) return true;
            return sword.distanceToSqr(o) > 4.0;
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void start() { sword.resetStuck(); }

        @Override
        public void tick() {
            ServerPlayer o = wielder();
            if (o == null) return;
            if (sword.order() == ShishouPet.Order.STAY || sword.petState() == ShishouPet.State.STAY) {                // hovers where he is
                sword.setDeltaMovement(sword.getDeltaMovement().scale(0.7));
                return;
            }
            Vec3 spot = new Vec3(o.getX() + 0.8, o.getY() + 1.9, o.getZ() - 0.8);
            double d = Math.sqrt(sword.distanceToSqr(spot)), speed = ShishouHunt.followSpeed(d);
            if (d > ShishouHunt.FOLLOW_EASE) sword.fly(spot, speed);
            else sword.glide(spot, speed);
            if (d >= ShishouPet.BLINK_MIN && sword.stuckTicks() >= ShishouPet.STUCK_BLINK_TICKS) blink(o, spot);
        }

        /** A flight that has made no progress for two seconds: he appears at the wielder's shoulder, when that spot is free (else the unstick rule keeps nudging him). */
        private void blink(ServerPlayer o, Vec3 spot) {
            Vec3 delta = spot.subtract(sword.position());
            if (sword.level().noCollision(sword, sword.getBoundingBox().move(delta))) {
                sword.teleportTo(spot.x, spot.y, spot.z);
                sword.setDeltaMovement(Vec3.ZERO);
                ShishouSpells.fx((ServerLevel) sword.level(), com.newuniverse.nusmp.particle.fx.Fx.options(com.newuniverse.nusmp.particle.fx.FxThemes.SKILL_SHISHOU, com.newuniverse.nusmp.particle.fx.FxTone.LIGHT), spot, 4, 0.25);
            }
            sword.resetStuck();
        }
    }
}
