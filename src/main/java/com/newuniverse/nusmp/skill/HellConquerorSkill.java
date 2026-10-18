package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Hell Conqueror: the Ultimate evolution of {@link HellWalkerSkill} (unlocked automatically by mastering it). Not an alias. Its damage
 * curve is far steeper (x2.5 steepness: at ten percent health a blow is more than three times as strong), below half health every blow
 * heals you for a fifth of its damage, and it adds two things the Unique cannot do: below half health a hellfire aura burns every foe
 * within four blocks each second, and below a third of health you are driven berserk (speed and strength). Numbers: {@link AuraRules}.
 */
public class HellConquerorSkill extends HellWalkerSkill {
    static final SkillText CONQUEROR_TEXT = SkillText.of("hell_conqueror", "Hell Conqueror",
            "Ultimate. Toggle. Evolved from Hell Walker. Near death your blows become devastating and heal you, and a hellfire aura burns the foes around you.");

    public HellConquerorSkill() { super(SkillType.ULTIMATE, CONQUEROR_TEXT, true); }

    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        super.pulse(instance, player, tier);
        double hp = player.getHealth(), max = player.getMaxHealth();
        if (AuraRules.hellfire(hp, max, tier)) {
            for (LivingEntity foe : SkillTargets.foes(player, player.position(), AuraRules.hellfireRadius())) {
                SkillDamage.hit(player, foe, AuraRules.hellfireDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC);
                foe.igniteForSeconds(3);
            }
            Vec3 at = player.position();
            SkillFx.shape(player.serverLevel(), VfxShape.FIRE_WILD, at, at.add(0, 1, 0), 0xFFFF2A1E, 20, (float) AuraRules.hellfireRadius());
        }
        if (AuraRules.berserk(hp, max, tier)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false));
        }
    }
}
