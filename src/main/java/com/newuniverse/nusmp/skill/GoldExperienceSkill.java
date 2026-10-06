package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gold Experience (Giorno Giovanna): give life.
 * Press: heal the player you look at (or yourself). Passive: your hits overload the
 * target's senses, leaving them slowed.
 */
public class GoldExperienceSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/gold_ingot.png");
    public GoldExperienceSkill() { super(SkillType.UNIQUE); }
    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Gold Experience is on cooldown."); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(800))) return;
        ServerPlayer target = SkillUtil.lookTarget(player, 8);
        if (target == null) target = player;
        boolean mastered = instance.isMastered(player);
        target.heal(mastered ? 16 : 10);
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 30, 0.5, 0.8, 0.5, 0.0);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(300)), mode);
        SkillUtil.castVfx(player, 0xFFFFD040);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        return true;
    }
}
