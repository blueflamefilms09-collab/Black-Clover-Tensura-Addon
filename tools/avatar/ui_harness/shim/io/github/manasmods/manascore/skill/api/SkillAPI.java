package io.github.manasmods.manascore.skill.api;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Why: harness shim: a registry the scenario fills (the real one belongs to ManasCore and Tensura). */
public final class SkillAPI {
    private SkillAPI() {}
    public static final Map<ResourceLocation, ManasSkill> SKILLS = new HashMap<>();
    public static final class Registry { public ManasSkill get(ResourceLocation id) { return SKILLS.get(id); } }
    private static final Registry REGISTRY = new Registry();
    public static Registry getSkillRegistry() { return REGISTRY; }
}
