package com.newuniverse.nusmp.skill;

import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.resources.ResourceLocation;

/**
 * The ten exclusive Riven Remake passives (nusmp:riven_*). Registered so they have an id, a name and a subtitle, but they are boss-only:
 * no skill item, no grimoire page drop, and the boss strips his skill storage before he dies so Predator, Usurper and Copy Magic get nothing.
 * Their rules run in {@code entity.riven.RivenPassives}.
 */
public class BossBoundSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/writable_book.png");

    public BossBoundSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
}
