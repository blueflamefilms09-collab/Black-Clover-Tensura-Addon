package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.GodRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Hestia, Goddess of the Hearth: heals and blesses you and every ally around you (players, and your own pets and summons). Each ally is
 * healed through the balance law's healing cap, gets Regeneration and Absorption hearts; a mastered Hearth is wider and stronger, washes
 * harmful effects away and wards off fire for a minute. Numbers: {@link GodRules.Hestia}.
 */
public class HestiaSkill extends GodSkill {
    static final SkillText TEXT = SkillText.of("hestia", "Hestia",
            "Unique. Goddess of the Hearth. Heal and bless yourself and every ally around you; once mastered it also cleanses and wards off fire.");

    public HestiaSkill() { super(TEXT); }

    @Override protected double cost() { return GodRules.Hestia.COST; }
    @Override protected int cooldownTicks() { return GodRules.Hestia.COOLDOWN; }
    @Override protected int color() { return 0xFFFFB347; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        double radius = GodRules.Hestia.radius(tier);
        for (LivingEntity ally : SkillTargets.alliesAround(player, player.position(), radius)) {
            BalanceLaw.heal(ally, (float) (ally.getMaxHealth() * GodRules.Hestia.healShare(tier)));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, GodRules.Hestia.regenTicks(), GodRules.Hestia.regenAmp(tier)));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, GodRules.Hestia.absorbTicks(), GodRules.Hestia.absorbAmp(tier)));
            if (GodRules.Hestia.cleanses(tier)) {
                SkillUtil.cleanse(ally);
                ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, GodRules.Hestia.fireWardTicks(tier), 0));
            }
            SkillFx.spray(level, false, ally.position().add(0, ally.getBbHeight() + 0.3, 0), 6, 0.4, 0.2, 0.4, 0.01);
        }
        Vec3 at = player.position();
        SkillFx.ring(level, at, Math.min(radius * 0.6, 8.0), 0xFFB347);
        SkillFx.shape(level, VfxShape.FIRE_PILLAR, at, at.add(0, 1, 0), 0xFFFFB347, 30, 2.0f);
        SkillFx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.4F);
        return true;
    }
}
