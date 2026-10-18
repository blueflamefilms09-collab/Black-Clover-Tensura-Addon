package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: Hell Walker, Hawk Eye, Spirit Card, Six Eyes and Vector Manipulation are TOGGLE skills: press to switch on, press again to switch
 * off, a magicule drain while on, and an effect each pulse. This base owns the shared rules (drain per pulse through the pack-wide cost
 * multiplier, switch off by itself when the magicules run out, a point of mastery every ten seconds on, translatable on/off messages) so
 * each skill only says what its pulse does.
 */
public abstract class ToggleAuraSkill extends NUSkill {
    protected ToggleAuraSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    protected abstract int interval();
    /** Magicules per pulse for this tier (before the pack-wide cost multiplier). */
    protected abstract double drainPerPulse(Tier tier);
    protected abstract void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier);
    protected void turnedOn(ManasSkillInstance instance, ServerPlayer player) {}
    protected void turnedOff(ManasSkillInstance instance, ServerPlayer player) {}

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, SkillMsg.t("toggle.on", "%s: ON", instance.getDisplayName()).withStyle(ChatFormatting.GREEN));
            turnedOn(instance, player);
        }
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, SkillMsg.t("toggle.off", "%s: OFF", instance.getDisplayName()).withStyle(ChatFormatting.GRAY));
            turnedOff(instance, player);
        }
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player) || !instance.isToggled() || !player.isAlive()) return;
        if (player.tickCount % Math.max(1, interval()) != 0) return;
        Tier tier = tier(instance, player);
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(drainPerPulse(tier)))) {
            instance.setToggled(false);
            instance.onToggleOff(player);
            instance.markDirty();
            return;
        }
        pulse(instance, player, tier);
        if (player.tickCount % 200 == 0) instance.addMasteryPoint(player);
        instance.markDirty();
    }
}
