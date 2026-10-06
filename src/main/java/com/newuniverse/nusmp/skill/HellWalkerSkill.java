package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hell Walker (Hell Mode): TOGGLE. The closer you are to death, the harder you hit.
 * At 10% health your attacks deal roughly double damage (more when mastered).
 */
public class HellWalkerSkill extends ToggleAuraSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/blaze_rod.png");
    protected final boolean ultimate;

    public HellWalkerSkill() { this(SkillType.UNIQUE, false); }

    protected HellWalkerSkill(SkillType type, boolean ultimate) {
        super(type);
        this.ultimate = ultimate;
    }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int interval() { return 20; }
    @Override protected double drainPerPulse() { return 60; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        if (player.getHealth() < player.getMaxHealth() * 0.5F) {
            player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 6, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    protected void turnedOn(ManasSkillInstance instance, ServerPlayer player) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.6F, 0.6F);
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!instance.isToggled() || owner.getMaxHealth() <= 0) return true;
        float missing = 1.0F - owner.getHealth() / owner.getMaxHealth();
        float scale = ultimate ? 2.5F : (instance.isMastered(owner) ? 1.6F : 1.1F);
        float dealt = amount.get() * (1.0F + missing * scale);
        amount.set(dealt);
        if (ultimate && missing > 0.5F) owner.heal(dealt * 0.2F); // lifesteal below half health
        return true;
    }
}
