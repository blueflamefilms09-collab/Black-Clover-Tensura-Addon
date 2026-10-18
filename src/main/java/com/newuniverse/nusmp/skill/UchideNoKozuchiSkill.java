package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.ChantRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Uchide no Kozuchi (Haruhime): a three-line chant, "Level Boost". The ally you look at (a player, or one of your own pets and summons;
 * yourself if you look at nobody) rises above themselves for a minute: strength, resistance, speed, haste, absorption hearts and a
 * restoring heal. Mastery doubles the time. A very long cooldown keeps it a once-per-fight power. Numbers: {@link ChantRules} (boost*).
 */
public class UchideNoKozuchiSkill extends ChantedMagicSkill {
    static final SkillText TEXT = SkillText.of("uchide_no_kozuchi", "Uchide no Kozuchi",
            "Chanted magic (3 lines). Level Boost: greatly raise the power of the ally you look at, or your own, for a time.");

    public UchideNoKozuchiSkill() { super(TEXT, ChantRules.Spell.UCHIDE_NO_KOZUCHI); }

    @Override protected int color() { return 0xFFFFD24A; }

    @Override
    protected MutableComponent chantLine(int line) {
        return switch (line) {
            case 1 -> SkillMsg.chant("uchide_no_kozuchi", 1, "Little mallet of fortune, hear my prayer.");
            case 2 -> SkillMsg.chant("uchide_no_kozuchi", 2, "Lend your strength to the one who stands before me.");
            default -> SkillMsg.chant("uchide_no_kozuchi", 3, "Uchide no Kozuchi: grow, and rise above yourself.");
        };
    }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, Tier tier) {
        LivingEntity target = SkillTargets.lookAlly(player, ChantRules.boostRange());
        if (target == null) target = player;
        int ticks = ChantRules.boostTicks(tier);
        int amp = ChantRules.boostAmp();
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, amp));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, amp));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, amp));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, amp));
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, ChantRules.boostAbsorbAmp(tier)));
        BalanceLaw.heal(target, (float) (target.getMaxHealth() * ChantRules.boostHealShare(tier)));

        Vec3 at = target.position().add(0, 1, 0);
        SkillFx.aura(level, target, false);
        SkillFx.spray(level, false, at, 12, 0.5, 1.0, 0.5, 0.2);
        SkillFx.shape(level, VfxShape.LIGHT_FLARE, at, at, 0xFFFFD24A, 20, 1.2f);
        SkillFx.sound(level, at, SoundEvents.PLAYER_LEVELUP, 1.0F, 0.8F);
        if (target instanceof ServerPlayer boosted) {
            SkillUtil.actionbar(boosted, SkillMsg.t("kozuchi.boost", "LEVEL BOOST! Your power surges.").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }
}
