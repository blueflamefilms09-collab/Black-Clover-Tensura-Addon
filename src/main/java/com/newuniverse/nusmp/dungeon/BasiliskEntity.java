package com.newuniverse.nusmp.dungeon;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** A Basilisk (tier 3): a long armoured lizard whose bite poisons. Slower than the Direwolf, tougher, and just as patient. Sleeps when full. */
public class BasiliskEntity extends DungeonCreature {
    public BasiliskEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 38).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.ARMOR, 6).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override public int tier() { return 3; }

    @Override public boolean isPrey(LivingEntity e) { return e instanceof LivingArmorEntity || e instanceof DungeonSlimeEntity; }

    @Override public boolean huntsPlayers() { return true; }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity t) t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        return hit;
    }
}
