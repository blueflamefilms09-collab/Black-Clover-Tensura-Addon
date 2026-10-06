package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Base for TOGGLE skills: press to switch on/off. While on, {@link #pulse} runs every
 * {@link #interval} ticks and drains magicules. Runs out of magicules -> switches off.
 */
public abstract class ToggleAuraSkill extends Skill {
    protected ToggleAuraSkill(SkillType type) { super(type); }

    protected abstract int interval();
    protected abstract double drainPerPulse();
    protected abstract void pulse(ManasSkillInstance instance, ServerPlayer player, boolean mastered);
    protected void turnedOn(ManasSkillInstance instance, ServerPlayer player) {}
    protected void turnedOff(ManasSkillInstance instance, ServerPlayer player) {}

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, instance.getDisplayName().copy().append(" ON").withStyle(ChatFormatting.GREEN));
            turnedOn(instance, player);
        }
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, instance.getDisplayName().copy().append(" OFF").withStyle(ChatFormatting.GRAY));
            turnedOff(instance, player);
        }
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player) || !instance.isToggled()) return;
        if (player.tickCount % interval() != 0) return;
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(drainPerPulse()))) {
            instance.setToggled(false);
            instance.onToggleOff(player);
            instance.markDirty();
            return;
        }
        pulse(instance, player, instance.isMastered(player));
        if (player.tickCount % 200 == 0) instance.addMasteryPoint(player);
        instance.markDirty();
    }
}
