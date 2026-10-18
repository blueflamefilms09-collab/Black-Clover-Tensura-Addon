package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.WillRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Unbreakable Will: PASSIVE. When a blow would kill you, you refuse to die: you stand back up with 30% of your health (50% when
 * mastered), cleansed of every effect, protected for five seconds and regenerating. After that the will needs ten minutes (five when
 * mastered) to recover. The void and /kill are never refused. Press the skill to see whether the will is ready. Immortal Will (the
 * Ultimate) extends this. Numbers: {@link WillRules}.
 */
public class UnbreakableWillSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("unbreakable_will", "Unbreakable Will",
            "Unique. Passive. Once in a long while, survive a killing blow and stand back up. Press to see whether your will is ready.");

    public UnbreakableWillSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected UnbreakableWillSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (mode == WillRules.READY) status(instance, player);
    }

    /** Tells the player whether the will is ready, or how long until it is. */
    protected final void status(ManasSkillInstance instance, ServerPlayer player) {
        SkillUtil.actionbar(player, instance.onCoolDown(WillRules.READY)
                ? SkillMsg.t("will.recovering", "Your will is still recovering (%s s).", instance.getCoolDown(WillRules.READY)).withStyle(ChatFormatting.GRAY)
                : SkillMsg.t("will.ready", "Your will is unbroken.").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player)) return true;
        if (!WillRules.mayRefuse(instance.onCoolDown(WillRules.READY), source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))) return true; // die normally

        Tier tier = tier(instance, player);
        player.setHealth((float) Math.max(1.0, player.getMaxHealth() * WillRules.restoredShare(tier)));
        player.removeAllEffects();
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, WillRules.shieldTicks(tier), WillRules.shieldAmp(tier)));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        refused(instance, player, tier);

        SkillFx.spray(player.serverLevel(), false, player.position().add(0, 1, 0), 12, 0.5, 1, 0.5, 0.2);
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.TOTEM_USE, 1.0F, 0.8F);
        SkillUtil.actionbar(player, SkillMsg.t("will.refused", "You refuse to fall!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        cooldown(instance, WillRules.READY, DMUtil.cooldown(WillRules.cooldownTicks(tier)));
        grow(instance, player, 5);
        return false; // cancel the death
    }

    /** Hook for the Ultimate's extra answer to a refused death. */
    protected void refused(ManasSkillInstance instance, ServerPlayer player, Tier tier) {}
}
