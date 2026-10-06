package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Unbreakable Will: PASSIVE. Once per cooldown, refuse to die — you survive the killing blow
 * with 30% health (50% mastered). Press the skill to check if it's ready.
 */
public class UnbreakableWillSkill extends Skill {
    private static final int READY = 0;
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png");
    protected final boolean ultimate;

    public UnbreakableWillSkill() { this(SkillType.UNIQUE, false); }

    protected UnbreakableWillSkill(SkillType type, boolean ultimate) {
        super(type);
        this.ultimate = ultimate;
    }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, instance.onCoolDown(READY)
                    ? Component.literal("Your will is still recovering.").withStyle(ChatFormatting.GRAY)
                    : Component.literal("Your will is unbroken.").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player) || instance.onCoolDown(READY)) return true; // die normally
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;

        boolean mastered = instance.isMastered(player);
        player.setHealth(ultimate ? player.getMaxHealth() : player.getMaxHealth() * (mastered ? 0.5F : 0.3F));
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.4);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal("You refuse to fall!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);

        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(ultimate ? 3600 : (mastered ? 6000 : 12000))), READY);
        instance.addMasteryPoint(player, 5);
        instance.markDirty();
        return false; // cancel death
    }
}
