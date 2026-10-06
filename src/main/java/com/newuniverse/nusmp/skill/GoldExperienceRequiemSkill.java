package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gold Experience Requiem (Giorno Giovanna): Ultimate. Return to Zero.
 * Passive: some attacks are returned to zero — they never land, and the attacker is left powerless.
 * Press: for a few seconds, EVERY attack against you returns to zero.
 */
public class GoldExperienceRequiemSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/golden_apple.png");
    public GoldExperienceRequiemSkill() { super(SkillType.ULTIMATE); }
    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Requiem is on cooldown."); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(5000))) return;
        int ticks = instance.isMastered(player) ? 200 : 120;
        instance.getOrCreateTag().putLong("ZeroUntil", player.serverLevel().getGameTime() + ticks);
        player.displayClientMessage(Component.literal("Return to Zero.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.5F);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(2400)), mode);
        SkillUtil.castVfx(player, 0xFFFFD040);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }

    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity owner, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == owner) return true;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        boolean active = owner.level().getGameTime() < instance.getOrCreateTag().getLong("ZeroUntil");
        double chance = instance.isMastered(owner) ? 0.35 : 0.2;
        if (!active && owner.getRandom().nextDouble() >= chance) return true;

        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3));
        attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
        }
        return false; // the attack returns to zero
    }
}
