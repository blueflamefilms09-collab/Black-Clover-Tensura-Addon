package com.newuniverse.nusmp.corpse;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Why: Soul Corpse Magic summons zombies (the owner's call: a Soul Corpse is a zombie, not a puppet prop). It is a thin subclass of the vanilla {@link Zombie} so it looks and sounds like one
 * (the vanilla renderer draws it, the Soul Corpse decay layer paints the stitched overlay over it) but has its own entity type, which is the one place the rules differ:
 * <ul>
 *   <li>It does NOT burn in daylight ({@link #isSunSensitive()} is false, see {@link SoulZombieRules#SUN_BURNS}); lava, fire and fire attacks hurt it as usual.</li>
 *   <li>It is owned: it never targets its owner, the owner's allies, the pact's people or any other summon ({@link Corpses#isFoe}); it follows the owner and fights the owner's foes only.</li>
 *   <li>It never turns into a drowned, never infects a villager, never calls reinforcements, never picks up loot, never drops anything or experience, is never saved with the world.</li>
 * </ul>
 * Its life, cap and cleanup (owner logs out, dies or leaves the dimension, the clock runs out, the server stops) are {@link Corpses}' sweep over the {@code ZOMBIES} roster, the same as for
 * Jimmy and the others. It is flagged undead through the {@code minecraft:undead} tag (data), which is what smite and the Soul Burial engraving look at.
 */
public class SoulZombieEntity extends Zombie implements OwnableEntity {
    private UUID ownerId;

    public SoulZombieEntity(EntityType<? extends SoulZombieEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setPersistenceRequired();
        setCanPickUpLoot(false);
        setCanBreakDoors(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, SoulZombieRules.BASE_HEALTH).add(Attributes.ATTACK_DAMAGE, SoulZombieRules.BASE_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, SoulZombieRules.SPEED).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override public UUID getOwnerUUID() { return ownerId; }

    public void setOwner(UUID id) { this.ownerId = id; }

    // ---------------------------------------------------------------- the daylight rule
    /** The point of this class: sunlight never ignites a summoned zombie (and never needs a helmet to be refused). */
    @Override protected boolean isSunSensitive() { return SoulZombieRules.SUN_BURNS; }

    @Override public boolean isSunBurnTick() { return false; }

    /** True when daylight can ignite this zombie (never): the self-test and the rules test read this. */
    public boolean burnsInDaylight() { return isSunSensitive(); }

    /** Water does not turn it into a drowned (the clock ends it, not the sea). */
    @Override protected boolean convertsInWater() { return false; }

    // ---------------------------------------------------------------- goals: the owner's side
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new FollowOwnerGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new FoeGoal(this));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == null) return false;
        if (ownerId != null && (ownerId.equals(other.getUUID()) || ownerId.equals(Corpses.ownerOf(other)))) return true;
        Player owner = ownerPlayer();
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && super.canAttack(target);
    }

    private Player ownerPlayer() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    ServerPlayer ownerServer() {
        return ownerPlayer() instanceof ServerPlayer sp ? sp : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity by = source.getEntity();
        if (by != null && by != this && isAlliedTo(by)) return false;                 // the owner and the owner's friends cannot kill it by accident
        return super.hurt(source, amount);
    }

    /** Killing a villager does not infect it (a zombie of the owner never makes more zombies). */
    @Override public boolean killedEntity(ServerLevel level, LivingEntity entity) { return true; }

    /** A summon of the player must not vanish with the difficulty (a Zombie is a Monster, which Peaceful would remove). */
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override public boolean shouldBeSaved() { return false; }

    @Override protected boolean shouldDropLoot() { return false; }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    // ---------------------------------------------------------------- goals
    /** Picks the owner's nearest foe (see {@link Corpses#pickFoe}) twice a second. */
    private static final class FoeGoal extends TargetGoal {
        private final SoulZombieEntity me;
        private int wait;
        private LivingEntity found;

        FoeGoal(SoulZombieEntity me) {
            super(me, false);
            this.me = me;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (--wait > 0) return false;
            wait = 10;
            if (!(me.level() instanceof ServerLevel)) return false;
            found = Corpses.pickFoe(me.ownerServer(), me);
            return found != null;
        }

        @Override public void start() { me.setTarget(found); super.start(); }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = me.getTarget();
            return t != null && t.isAlive() && me.canAttack(t) && me.distanceToSqr(t) < 40 * 40;
        }
    }

    /** Stays within a few blocks of the owner when there is nothing to fight, and hurries (or steps through) when left far behind. */
    private static final class FollowOwnerGoal extends Goal {
        private final SoulZombieEntity me;
        private int repath;

        FollowOwnerGoal(SoulZombieEntity me) {
            this.me = me;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = me.ownerPlayer();
            return o != null && me.getTarget() == null && me.distanceToSqr(o) > 25;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = me.ownerPlayer();
            return o != null && me.getTarget() == null && me.distanceToSqr(o) > 9;
        }

        @Override
        public void tick() {
            Player o = me.ownerPlayer();
            if (o == null) return;
            me.getLookControl().setLookAt(o, 30f, 30f);
            if (me.distanceToSqr(o) > 28 * 28) { me.teleportTo(o.getX() + 1.5, o.getY(), o.getZ() + 1.5); me.getNavigation().stop(); return; }
            if (--repath > 0) return;
            repath = SoulZombieRules.REPATH_TICKS;
            me.getNavigation().moveTo(o, 1.2);
        }
    }
}
