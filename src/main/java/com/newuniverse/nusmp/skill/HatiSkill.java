package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hati (Bete Loga): PASSIVE. Fire, lightning and magic hitting you is partly absorbed
 * and turned into strength.
 */
public class HatiSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/magma_cream.png");
    public HatiSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        boolean magical = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING)
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
        if (!magical || amount.get() <= 0) return true;

        boolean mastered = instance.isMastered(owner);
        amount.set(amount.get() * (mastered ? 0.4F : 0.6F));
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, mastered ? 2 : 1));
        if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME, owner.getX(), owner.getY() + 1, owner.getZ(), 15, 0.4, 0.6, 0.4, 0.02);
        }
        if (owner.getRandom().nextInt(4) == 0) instance.addMasteryPoint(owner);
        instance.markDirty();
        return true;
    }
}
