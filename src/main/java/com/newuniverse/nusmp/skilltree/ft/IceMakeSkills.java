package com.newuniverse.nusmp.skilltree.ft;

import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the ManasCore skills of the route Ice-Make, Maker Magic (nusmp:ice_make). Each Fairy Tail route has its OWN register, owned by one content builder (stfb), so that two builders never edit the
 * same file when they add their abilities; {@code SkillTreeSystem.init} attaches every register to the mod bus once. A skill is added as
 * {@code SKILLS.register("ft_fire_dragons_roar", FireDragonsRoarSkill::new)}: the id must start with {@code ft_} and must be the id the route's JSON names in the node's {@code skill} field.
 * The owner of this file replaces the empty body below with the real registrations.
 */
public final class IceMakeSkills {
    private IceMakeSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);
}
