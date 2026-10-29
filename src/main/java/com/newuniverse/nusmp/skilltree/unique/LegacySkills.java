package com.newuniverse.nusmp.skilltree.unique;

import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registry half of "the skills of the removed Skill Books stay as ids". Every skill the books taught (the 284 unique skills {@code nusmp:line_<id>}, the twelve Heavy Knight techniques) was removed, but a
 * world saved before may still hold one of those ids in a player's skill list, and a registry entry that disappears can make such a save fail to load. So each id stays registered here as an inert, hidden
 * {@link LegacySkill} (it does nothing, cannot be used, and {@link LegacyMigration} forgets it from the player at the next login), in the same order the live skills had, in a register of its own so the position
 * of every other skill of the mod is unchanged. Nothing ever teaches these. Server and client run the same code, so the registry sync sees the same entries on both sides. Safe to call {@link #init} twice.
 */
public final class LegacySkills {
    private LegacySkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);
    private static boolean done;

    static {
        for (String bare : LegacyIds.RETIRED_LINES) SKILLS.register("line_" + bare, LegacySkill::new);
        for (String path : LegacyIds.REMOVED_TECHNIQUES) SKILLS.register(path, LegacySkill::new);
        for (String bare : LegacyIds.REMOVED_LINES) SKILLS.register("line_" + bare, LegacySkill::new);
        for (String path : LegacyIds.RETIRED_TECHNIQUES) SKILLS.register(path, LegacySkill::new);
    }

    /** Registers the stubs on the mod bus. Called once from {@code SkillBookSystem.init}. */
    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        SKILLS.register(modBus);
    }
}
