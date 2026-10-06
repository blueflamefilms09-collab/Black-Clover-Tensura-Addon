package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Airiel / Tempest (Ais Wallenstein): wind enchantment.
 * Modes: Tempest (wind buff), Wind Dash (burst forward).
 */
public class AirielSkill extends Skill {
    private static final int TEMPEST = 0, DASH = 1;
    private static final ResourceLocation ICON =
            ResourceLocation.withDefaultNamespace("textures/item/feather.png");

    public AirielSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance instance) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return mode == 0 ? 1 : 0; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == TEMPEST ? "airiel.tempest" : "airiel.dash"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        return Component.translatable(mode == TEMPEST ? "nusmp.skill.mode.airiel.tempest" : "nusmp.skill.mode.airiel.dash");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Airiel is on cooldown."); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == TEMPEST) {
            if (!SkillUtil.spendMagicules(player, NUConfig.AIRIEL_MAGICULE_COST.get())) return;
            int ticks = mastered ? 1200 : 600;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, mastered ? 2 : 1));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, mastered ? 1 : 0));
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(NUConfig.AIRIEL_COOLDOWN.get()), TEMPEST);
        } else {
            if (!SkillUtil.spendMagicules(player, NUConfig.AIRIEL_MAGICULE_COST.get() / 3)) return;
            Vec3 look = player.getViewVector(1.0F);
            double power = mastered ? 2.6 : 1.8;
            player.setDeltaMovement(look.x * power, Math.max(0.3, look.y * power * 0.6), look.z * power);
            player.hurtMarked = true;
            player.fallDistance = 0;
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.2F);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 15, 0.3, 0.2, 0.3, 0.05);
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(40), DASH);
        }
        SkillUtil.castVfx(player, 0xFF8CFFC2);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }
}
