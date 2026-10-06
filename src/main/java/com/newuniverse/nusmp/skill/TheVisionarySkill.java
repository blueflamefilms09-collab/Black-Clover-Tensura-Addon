package com.newuniverse.nusmp.skill;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Visionary (Gremmy Thoumeaux): Ultimate. Whatever you imagine becomes real.
 * Meteor: imagine a meteor onto your target (no block damage). Void: imagine a void that
 * swallows everything in an area. Perfect Body: imagine your body stronger.
 */
public class TheVisionarySkill extends Skill {
    private static final int METEOR = 0, VOID = 1, BODY = 2, MODES = 3;
    private static final String[] KEYS = {"meteor", "void", "body"};
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/firework_star.png");
    public TheVisionarySkill() { super(SkillType.ULTIMATE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public int getModes(ManasSkillInstance i) { return MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return r ? (mode + MODES - 1) % MODES : (mode + 1) % MODES; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "visionary." + KEYS[mode]; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) { return Component.translatable("nusmp.skill.mode.visionary." + KEYS[mode]); }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Your imagination needs rest."); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        switch (mode) {
            case METEOR -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(4000))) return;
                Vec3 c = DMUtil.lookPoint(player, 64);
                GodTier.line(level, ParticleTypes.FLAME, c.add(8, 40, 0), c);
                level.explode(player, c.x, c.y, c.z, mastered ? 6.0F : 4.0F, true, Level.ExplosionInteraction.NONE);
                for (LivingEntity t : DMUtil.around(player, c, mastered ? 8 : 6)) {
                    t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 50 : 35));
                    t.igniteForSeconds(6);
                }
                instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(600)), METEOR);
            }
            case VOID -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(3500))) return;
                Vec3 c = DMUtil.lookPoint(player, 40);
                for (LivingEntity t : DMUtil.around(player, c, mastered ? 9 : 6)) {
                    t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 2));
                    t.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 2));
                    t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                }
                level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 1, c.z, 300, 3, 2, 3, 0.02);
                level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0F, 0.5F);
                instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(700)), VOID);
            }
            default -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(3000))) return;
                int ticks = mastered ? 2400 : 1200;
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.3);
                instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(3600)), BODY);
            }
        }
        SkillUtil.castVfx(player, 0xFFFFA0E0);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }
}
