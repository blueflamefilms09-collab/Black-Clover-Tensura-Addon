package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Hawk Eye (Allen's Bird Card): TOGGLE. Night vision, and hostile mobs around you glow. */
public class HawkEyeSkill extends ToggleAuraSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/spyglass.png");
    public HawkEyeSkill() { super(SkillType.EXTRA); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int interval() { return 20; }
    @Override protected double drainPerPulse() { return 30; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        double range = mastered ? 48 : 32;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e instanceof Enemy && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
        }
    }

    @Override
    protected void turnedOff(ManasSkillInstance instance, ServerPlayer player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
