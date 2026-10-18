package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Hawk Eye (Allen's Bird Card): TOGGLE. While on you have night vision and every hostile creature within 32 blocks (48 when mastered)
 * glows, so you can see it through walls and darkness. At most 48 creatures are marked per second (the nearest), so a mob farm cannot
 * lag the server. Costs 30 magicules a second. Numbers: {@link AuraRules}.
 */
public class HawkEyeSkill extends ToggleAuraSkill {
    static final SkillText TEXT = SkillText.of("hawk_eye", "Hawk Eye",
            "Toggle. Bird Card sight: night vision, and every hostile creature around you glows, even through walls.");

    public HawkEyeSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override protected int interval() { return 20; }
    @Override protected double drainPerPulse(Tier tier) { return AuraRules.HAWK_DRAIN; }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        double range = AuraRules.hawkRange(tier);
        Vec3 here = player.position();
        List<LivingEntity> seen = player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(here) <= range * range);
        seen.sort(Comparator.comparingDouble(e -> e.distanceToSqr(here)));
        for (int i = 0; i < seen.size() && i < AuraRules.hawkMaxMarked(); i++) {
            seen.get(i).addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
        }
    }

    @Override
    protected void turnedOff(ManasSkillInstance instance, ServerPlayer player) {
        var night = player.getEffect(MobEffects.NIGHT_VISION);
        if (night != null && night.isAmbient()) player.removeEffect(MobEffects.NIGHT_VISION);   // only the one this skill gave
    }
}
