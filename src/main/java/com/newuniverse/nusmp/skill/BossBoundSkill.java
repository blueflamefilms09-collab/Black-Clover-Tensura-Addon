package com.newuniverse.nusmp.skill;

import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.resources.ResourceLocation;

/** Marker skill for Riven's boss-only passives; the boss removes all such skills before death/plunder. */
public final class BossBoundSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/writable_book.png");

    public BossBoundSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
}
