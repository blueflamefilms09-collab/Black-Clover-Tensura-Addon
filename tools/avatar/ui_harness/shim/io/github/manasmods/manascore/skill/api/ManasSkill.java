package io.github.manasmods.manascore.skill.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Why: harness shim: a skill with a name and an icon path. */
public class ManasSkill {
    private final String name; private final ResourceLocation icon;
    public ManasSkill(String name, ResourceLocation icon) { this.name = name; this.icon = icon; }
    public Component getName() { return Component.literal(name); }
    public ResourceLocation getSkillIcon() { return icon; }
}
