package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonalityTraits;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.mana.MagicStoneItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Why: a few things must be sampled every tick or every half second for the whole life of the Avatar and belong to no single behaviour: counting
 * down the Yandere frenzy, noticing that the creator is badly hurt (the CREATOR_LOW_HP line, once per drop), idle remarks, a Cowardly Avatar spotting a
 * boss, a Lazy Avatar noticing its creator standing still, a Merchant nudging dropped relics toward the creator, the Tsundere protective bonus, and
 * the numeric personality modifiers (attack speed). It never moves the Avatar itself. Personality is read every time (never cached at install),
 * because {@code registerGoals} runs before the Avatar's synced personality is set.
 */
public final class AvatarWatcherGoal extends Goal {
    private static final ResourceLocation PERSONALITY_ATK = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_personality_attack_speed");
    private static final ResourceLocation PROTECT_DMG = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_protective_damage");
    private static final ResourceLocation PROTECT_SPD = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_protective_speed");
    private static final double FEAR_RADIUS = 24.0;

    private final AvatarEntity avatar;
    private final AvatarMind mind;

    public AvatarWatcherGoal(AvatarEntity avatar, AvatarMind mind) {
        this.avatar = avatar;
        this.mind = mind;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override public boolean canUse() { return !avatar.level().isClientSide && avatar.isAlive(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        int t = avatar.tickCount;
        if (t % 10 != 0) return;
        AvatarPersonalityTraits.Traits traits = AvatarPersonalityTraits.combine(avatar.primary(), avatar.secondary());
        ServerPlayer creator = avatar.creatorPlayer();
        numericModifiers(traits);
        if (creator != null && creator.level() == avatar.level() && AvatarGates.loyal(avatar)) {
            creatorWatch(creator, traits);
            if (traits.pullsLoot()) pullLoot(creator);
        }
        if (t % 20 == 0) bossWatch(traits, creator);
        if (t % 200 == 0 && avatar.getTarget() == null && avatar.getRandom().nextInt(3) == 0) AvatarChatterEngine.say(avatar, ChatEvent.IDLE);
    }

    /** Attack-speed personality factor (Lazy 0.6, Aggressive 1.15 ...) as one transient modifier that is updated, never stacked. */
    private void numericModifiers(AvatarPersonalityTraits.Traits traits) {
        AttributeInstance atk = avatar.getAttribute(Attributes.ATTACK_SPEED);
        if (atk == null) return;
        double delta = traits.attackSpeed() - 1.0;
        if (Math.abs(delta) < 0.001) atk.removeModifier(PERSONALITY_ATK);
        else atk.addOrUpdateTransientModifier(new AttributeModifier(PERSONALITY_ATK, delta, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void creatorWatch(ServerPlayer creator, AvatarPersonalityTraits.Traits traits) {
        float frac = creator.getHealth() / Math.max(1f, creator.getMaxHealth());
        if (frac < 0.30f) {
            if (!mind.lowHpLatched && avatar.distanceToSqr(creator) < 48 * 48) { mind.lowHpLatched = true; AvatarChatterEngine.say(avatar, ChatEvent.CREATOR_LOW_HP); }
        } else if (frac >= 0.45f) mind.lowHpLatched = false;
        boolean protect = traits.protectBelowHp() > 0 && frac < traits.protectBelowHp();
        AttributeInstance dmg = avatar.getAttribute(Attributes.ATTACK_DAMAGE), spd = avatar.getAttribute(Attributes.MOVEMENT_SPEED);
        if (dmg != null) { if (protect) dmg.addOrUpdateTransientModifier(new AttributeModifier(PROTECT_DMG, AvatarPersonalityTraits.PROTECTIVE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); else dmg.removeModifier(PROTECT_DMG); }
        if (spd != null) { if (protect) spd.addOrUpdateTransientModifier(new AttributeModifier(PROTECT_SPD, AvatarPersonalityTraits.PROTECTIVE_BONUS * 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); else spd.removeModifier(PROTECT_SPD); }
        Vec3 now = creator.position();
        if (now.distanceToSqr(mind.creatorSpot) < 0.04) mind.creatorStillTicks += 10;
        else { mind.creatorStillTicks = 0; mind.creatorSpot = now; }
    }

    /** Cowardly: the sight of a boss within 24 blocks sets {@code mind.fear}, a scream and a short Resistance for the creator "from afar". */
    private void bossWatch(AvatarPersonalityTraits.Traits traits, ServerPlayer creator) {
        if (!traits.fleesBosses()) { mind.fear = null; return; }
        LivingEntity boss = null;
        double best = FEAR_RADIUS * FEAR_RADIUS;
        for (LivingEntity e : avatar.level().getEntitiesOfClass(LivingEntity.class, avatar.getBoundingBox().inflate(FEAR_RADIUS), x -> x != avatar && x.isAlive() && BossIdentity.isBoss(x))) {
            double d = e.distanceToSqr(avatar);
            if (d < best) { best = d; boss = e; }
        }
        boolean first = mind.fear == null && boss != null;
        mind.fear = boss;
        if (first) {
            AvatarChatterEngine.say(avatar, ChatEvent.COMBAT_START, boss.getDisplayName().getString());
            if (creator != null && creator.level() == avatar.level()) creator.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, true, true));
        }
    }

    /** Merchant: relics and magic stones lying near the Avatar drift toward the creator (never into the Avatar's own pocket: nothing is stolen). */
    private void pullLoot(ServerPlayer creator) {
        for (ItemEntity item : avatar.level().getEntitiesOfClass(ItemEntity.class, avatar.getBoundingBox().inflate(10))) {
            var it = item.getItem().getItem();
            if (!(it instanceof RelicBearer) && !(it instanceof MagicStoneItem)) continue;
            Vec3 to = creator.position().add(0, 0.5, 0).subtract(item.position());
            if (to.lengthSqr() < 2.5 * 2.5) continue;
            item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(to.normalize().scale(0.22)));
            item.hasImpulse = true;
        }
    }
}
