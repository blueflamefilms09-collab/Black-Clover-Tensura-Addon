package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.JoJoRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Gold Experience Requiem (Giorno Giovanna): the Ultimate evolution of Gold Experience. Return to Zero: a share of attacks against you
 * (one in five, one in three when mastered) never land, and the attacker is left powerless for five seconds. Mode Return to Zero opens a
 * window of 6 seconds (10 when mastered) in which EVERY attack against you returns to zero. Mode Life Burst floods every ally within
 * 14 blocks (20 mastered) with life: healed through the healing cap, cleansed, regenerating and shielded. Numbers: {@link JoJoRules}.
 */
public class GoldExperienceRequiemSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("gold_experience_requiem", "Gold Experience Requiem",
            "Ultimate. Return to Zero: attacks against you never reach their result. Life Burst floods your allies with life.");

    public GoldExperienceRequiemSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return JoJoRules.requiemModes(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == JoJoRules.ZERO ? JoJoRules.LIFE_BURST : JoJoRules.ZERO; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == JoJoRules.LIFE_BURST ? "requiem.burst" : "requiem.zero"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == JoJoRules.LIFE_BURST ? SkillMsg.mode("requiem.burst", "Life Burst") : SkillMsg.mode("requiem.zero", "Return to Zero");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("requiem.cooldown", "Requiem is on cooldown.")); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == JoJoRules.ZERO) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(JoJoRules.zeroCost()))) return;
            instance.getOrCreateTag().putLong("ZeroUntil", level.getGameTime() + JoJoRules.zeroTicks(mastered));
            SkillUtil.actionbar(player, SkillMsg.t("requiem.zero", "Return to Zero.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            SkillFx.follow(level, VfxShape.LIGHT_FLARE, player, player.position().add(0, 1, 0), 0xFFFFD040, JoJoRules.zeroTicks(mastered), 1.2f);
            SkillFx.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.5F);
            cooldown(instance, mode, DMUtil.cooldown(JoJoRules.zeroCooldown()));
        } else if (mode == JoJoRules.LIFE_BURST) {
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(JoJoRules.burstCost()))) return;
            for (LivingEntity ally : SkillTargets.alliesAround(player, player.position(), JoJoRules.burstRadius(mastered))) {
                SkillUtil.cleanse(ally);
                BalanceLaw.heal(ally, (float) (ally.getMaxHealth() * JoJoRules.burstHealShare()));
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, JoJoRules.burstRegenTicks(), 1));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, JoJoRules.burstAbsorbAmp()));
                SkillFx.spray(level, false, ally.position().add(0, 1, 0), 8, 0.5, 0.8, 0.5, 0.02);
            }
            SkillFx.ring(level, player.position(), 8.0, 0xFFD040);
            SkillFx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.0F);
            cooldown(instance, mode, DMUtil.cooldown(JoJoRules.burstCooldown()));
        } else {
            return;
        }
        SkillUtil.castVfx(player, 0xFFFFD040);
        grow(instance, player, 1);
    }

    /** Return to Zero: the attack never lands, and the one who threw it is left powerless. */
    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity owner, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == owner) return true;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        boolean window = owner.level().getGameTime() < instance.getOrCreateTag().getLong("ZeroUntil");
        if (!JoJoRules.returns(window, owner.getRandom().nextDouble(), instance.isMastered(owner))) return true;

        int ticks = JoJoRules.punishTicks();
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4));
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 3));
        attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
        if (owner.level() instanceof ServerLevel level) {
            Vec3 at = attacker.position().add(0, 1, 0);
            SkillFx.spray(level, false, at, 10, 0.4, 0.6, 0.4, 0.05);
        }
        return false; // the attack returns to zero
    }
}
