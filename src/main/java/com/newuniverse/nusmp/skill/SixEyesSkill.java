package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Six Eyes (Satoru Gojo): TOGGLE. See everything around you, through walls and darkness. */
public class SixEyesSkill extends ToggleAuraSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/ender_eye.png");
    public SixEyesSkill() { super(SkillType.UNIQUE); }
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int interval() { return 20; }
    @Override protected double drainPerPulse() { return 40; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(mastered ? 64 : 40), e -> e != player && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
        }
    }

    @Override protected void turnedOff(ManasSkillInstance instance, ServerPlayer player) { player.removeEffect(MobEffects.NIGHT_VISION); }
}
