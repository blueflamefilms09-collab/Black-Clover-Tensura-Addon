package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Book Maker (Kumagawa Misogi): pin your target with giant screws, dragging them down to your level. */
public class BookMakerSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/iron_nugget.png");
    public BookMakerSkill() { super(SkillType.UNIQUE); }
    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Book Maker is on cooldown."); return; }
        LivingEntity target = GodTier.lookLiving(player, 24);
        if (target == null) { SkillUtil.fail(player, "No target."); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(400))) return;
        boolean mastered = instance.isMastered(player);
        target.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 16 : 10));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 5));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, mastered ? 2 : 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
        player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 40, 0.4, 0.6, 0.4, 0.2);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
        instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(80)), mode);
        SkillUtil.castVfx(player, 0xFF8A8A9A);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }
}
