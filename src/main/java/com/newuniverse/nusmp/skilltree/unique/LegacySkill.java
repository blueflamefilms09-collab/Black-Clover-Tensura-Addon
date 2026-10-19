package com.newuniverse.nusmp.skilltree.unique;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: an inert placeholder for a skill id that an older world may still hold (see {@link LegacyIds}). The registry entry has to exist so a save that names it still loads; the placeholder does nothing at all
 * (it cannot be used, toggled, ticked, scrolled or slotted, and has no effect on anything), and {@link LegacyMigration} forgets it from the player at the next login, so it never reaches Tensura's menu for more than
 * a moment. It is never taught by anything.
 */
public final class LegacySkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/barrier.png");

    public LegacySkill() { super(SkillType.EXTRA); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override public MutableComponent getName() { return Component.translatableWithFallback("nusmp.skill.legacy", "Forgotten Magic"); }

    @Override public MutableComponent getSkillDescription() { return Component.translatableWithFallback("nusmp.skill.legacy.description", "A magic that was removed from this world. It does nothing and is forgotten when you next log in."); }

    @Override public boolean canInteractSkill(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canActivateSkill(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canScroll(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }

    @Override public boolean canBeSlotted(ManasSkillInstance instance, LivingEntity entity, int slot) { return false; }
}
