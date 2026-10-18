package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Six Eyes (Satoru Gojo): TOGGLE. See everything: night vision, every living thing within 40 blocks (64 when mastered) glows through
 * walls and darkness, and the creature you look at is read out on the action bar (name and health). At most 64 creatures are marked per
 * second. A mastered Six Eyes wastes half the magicules. Limitless (the Ultimate, evolved from this) is a separate, stronger skill.
 * Numbers: {@link AuraRules}.
 */
public class SixEyesSkill extends ToggleAuraSkill {
    private static final int MAX_MARKED = 64;

    static final SkillText TEXT = SkillText.of("six_eyes", "Six Eyes",
            "Unique. Toggle. See every living thing around you through walls and darkness, and read the health of whatever you look at.");

    public SixEyesSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override protected int interval() { return 20; }
    @Override protected double drainPerPulse(Tier tier) { return AuraRules.sixDrain(tier); }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        double range = AuraRules.sixRange(tier);
        Vec3 here = player.position();
        List<LivingEntity> seen = player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && e.distanceToSqr(here) <= range * range);
        seen.sort(Comparator.comparingDouble(e -> e.distanceToSqr(here)));
        for (int i = 0; i < seen.size() && i < MAX_MARKED; i++) {
            seen.get(i).addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
        }
        LivingEntity looked = SkillTargets.lookLiving(player, range);
        if (looked != null) {
            SkillUtil.actionbar(player, SkillMsg.t("sixeyes.read", "%s: %s / %s health", looked.getName(),
                    Math.round(looked.getHealth()), Math.round(looked.getMaxHealth())).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    protected void turnedOff(ManasSkillInstance instance, ServerPlayer player) {
        var night = player.getEffect(MobEffects.NIGHT_VISION);
        if (night != null && night.isAmbient()) player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
