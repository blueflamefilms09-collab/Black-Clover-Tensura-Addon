package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registers the Saiyan Prodigy ({@code anime_multiverse:saiyan_prodigy}) on the mod's skill register. A holder of its own so {@link NUSkills} is not touched (shared files are only appended to): the class is
 * first used from {@code SaiyanBoot.init}, in the mod constructor and before the registry opens, which is when a deferred entry may still be added. The id is permanent.
 */
public final class ProdigySkills {
    private ProdigySkills() {}

    public static final DeferredHolder<ManasSkill, SaiyanProdigySkill> PRODIGY = NUSkills.SKILLS.register("saiyan_prodigy", SaiyanProdigySkill::new);

    /** Touches the class (and so the registration); called once from the Saiyan boot. */
    public static void init() { }
}
