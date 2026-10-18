package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.FaithRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Goddess of Faith: the Ultimate evolution of {@link FaithSkill} (unlocked automatically by mastering it; the Faith points carry over).
 * Not an alias. It bestows for half the Faith and half the wait from twice as far, prayer yields three Faith, and it adds two modes
 * appended after Check Faith: Blessing (spend 2 Faith: every ally within 16 blocks gains strength, resistance, regeneration and
 * absorption for a minute) and Miracle (spend 5 Faith: allies are healed up to the healing cap, every harmful effect is washed away and
 * they stand protected for five seconds).
 */
public class GoddessOfFaithSkill extends FaithSkill {
    static final SkillText GODDESS_TEXT = SkillText.of("goddess_of_faith", "Goddess of Faith",
            "Ultimate. Evolved from Faith. Bestow for half the Faith and half the wait, pray for triple Faith, grant allies a Blessing, and work a Miracle that heals and protects them.");

    private static final int BLESSING = 3, MIRACLE = 4;

    public GoddessOfFaithSkill() { super(SkillType.ULTIMATE, GODDESS_TEXT, true); }

    @Override public int getModes(ManasSkillInstance instance) { return 5; }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case BLESSING -> "faith.blessing";
            case MIRACLE -> "faith.miracle";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case BLESSING -> SkillMsg.mode("faith.blessing", "Blessing");
            case MIRACLE -> SkillMsg.mode("faith.miracle", "Miracle");
            default -> super.getModeName(instance, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (entity instanceof ServerPlayer player && player.isAlive()) {
            if (mode == BLESSING) { blessing(instance, player); return; }
            if (mode == MIRACLE) { miracle(instance, player); return; }
        }
        super.onPressed(instance, entity, slot, mode);
    }

    /** Pays the Faith, or says why not. */
    private boolean pay(ManasSkillInstance instance, ServerPlayer player, int cost, int mode) {
        if (instance.onCoolDown(mode)) {
            SkillUtil.fail(player, SkillMsg.t("faith.divine_cooldown", "The goddess has not yet recovered."));
            return false;
        }
        int faith = getFaith(instance);
        if (!FaithRules.canSpend(faith, cost)) {
            SkillUtil.fail(player, SkillMsg.t("faith.not_enough", "Not enough Faith (need %s, have %s).", cost, faith));
            return false;
        }
        setFaith(instance, FaithRules.spend(faith, cost));
        return true;
    }

    private void blessing(ManasSkillInstance instance, ServerPlayer player) {
        if (!pay(instance, player, FaithRules.BLESSING_COST, BLESSING)) return;
        ServerLevel level = player.serverLevel();
        int ticks = FaithRules.blessingTicks();
        for (LivingEntity ally : SkillTargets.alliesAround(player, player.position(), FaithRules.blessingRadius(Tier.ULTIMATE))) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks / 2, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
            SkillFx.spray(level, false, ally.position().add(0, ally.getBbHeight() + 0.3, 0), 8, 0.3, 0.2, 0.3, 0.02);
        }
        SkillFx.ring(level, player.position(), 6.0, 0xFFE9A0);
        SkillFx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.5F);
        SkillUtil.actionbar(player, SkillMsg.t("faith.blessing", "Blessing: your allies are strengthened.").withStyle(ChatFormatting.GOLD));
        cooldown(instance, BLESSING, FaithRules.blessingCooldown());
        SkillUtil.castVfx(player, 0xFFFFE08A);
        grow(instance, player, 1);
    }

    private void miracle(ManasSkillInstance instance, ServerPlayer player) {
        if (!pay(instance, player, FaithRules.MIRACLE_COST, MIRACLE)) return;
        ServerLevel level = player.serverLevel();
        for (LivingEntity ally : SkillTargets.alliesAround(player, player.position(), FaithRules.blessingRadius(Tier.ULTIMATE))) {
            SkillUtil.cleanse(ally);
            BalanceLaw.heal(ally, ally.getMaxHealth());          // the balance law caps a single heal at half of max health
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
            Vec3 at = ally.position().add(0, 1, 0);
            SkillFx.beam(level, at.add(0, 4, 0), at, false);
        }
        SkillFx.ring(level, player.position(), 8.0, 0xFFFFFF);
        SkillFx.sound(level, player.position(), SoundEvents.TOTEM_USE, 0.8F, 1.4F);
        SkillUtil.actionbar(player, SkillMsg.t("faith.miracle", "A miracle: your allies are healed and protected.").withStyle(ChatFormatting.GOLD));
        cooldown(instance, MIRACLE, FaithRules.miracleCooldown());
        SkillUtil.castVfx(player, 0xFFFFFFFF);
        grow(instance, player, 2);
    }
}
