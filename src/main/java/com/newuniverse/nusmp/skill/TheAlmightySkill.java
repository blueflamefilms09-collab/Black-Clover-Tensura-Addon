package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
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
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

/**
 * The Almighty (Yhwach): Ultimate. You see every future.
 * Passive: a share of attacks simply never happen — you already saw and chose another future.
 * Press: Rewrite the Future — for a few seconds no harm can reach you, and your debuffs vanish.
 */
public class TheAlmightySkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/ender_eye.png");
    public TheAlmightySkill() { super(SkillType.ULTIMATE); }
    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "That future is not yet visible."); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(5000))) return;
        int ticks = instance.isMastered(player) ? 160 : 100;
        instance.getOrCreateTag().putLong("FutureUntil", player.serverLevel().getGameTime() + ticks);
        for (MobEffectInstance e : new ArrayList<>(player.getActiveEffects())) {
            if (!e.getEffect().value().isBeneficial()) player.removeEffect(e.getEffect());
        }
        player.displayClientMessage(Component.literal("The future has been rewritten.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.6F);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(3000)), mode);
        SkillUtil.castVfx(player, 0xFF8A0A0A);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }

    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity owner, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        boolean rewriting = owner.level().getGameTime() < instance.getOrCreateTag().getLong("FutureUntil");
        double chance = NUConfig.ALMIGHTY_DODGE_CHANCE.get() * (instance.isMastered(owner) ? 1.5 : 1.0);
        if (!rewriting && owner.getRandom().nextDouble() >= chance) return true;
        if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, owner.getX(), owner.getY() + 1, owner.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
        }
        return false; // that future never happens
    }
}
