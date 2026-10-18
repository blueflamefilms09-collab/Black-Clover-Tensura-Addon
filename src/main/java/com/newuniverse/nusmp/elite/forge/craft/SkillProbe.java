package com.newuniverse.nusmp.elite.forge.craft;

import com.newuniverse.nusmp.elite.forge.data.ForgeRecipes;
import com.newuniverse.nusmp.elite.forge.rules.SkillBonusTable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;

/**
 * Why: the only Minecraft part of the Skill Bonus Resolvers. It answers "how well does this player have the skill with this registry id" by a SOFT lookup: the id
 * is parsed, the player's learned skills are asked for it, and anything that goes wrong (a malformed id, a skill that is not installed, a player without a skill
 * storage, an exception from another mod's skill) answers 0, so the table in {@code skill_bonus.json} can name elite skills and Tensura skills alike without caring
 * which exist. 1 = learned, 2 = mastered.
 */
public final class SkillProbe {
    private SkillProbe() {}

    public static int level(ServerPlayer player, String skillId) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(skillId);
            if (rl == null) return SkillBonusTable.NONE;
            Skills skills = SkillAPI.getSkillsFrom(player);
            if (skills == null) return SkillBonusTable.NONE;
            Optional<ManasSkillInstance> inst = skills.getSkill(rl);
            if (inst.isEmpty()) return SkillBonusTable.NONE;
            return inst.get().isMastered(player) ? SkillBonusTable.MASTERED : SkillBonusTable.OWNED;
        } catch (Throwable broken) {
            return SkillBonusTable.NONE;
        }
    }

    /** The player's forge bonus in quality points (capped by the table). */
    public static int bonus(ServerPlayer player) { return ForgeRecipes.skillTable().resolve(id -> level(player, id)); }

    /** Skill id -> points, for the info command. */
    public static Map<String, Integer> explain(ServerPlayer player) { return ForgeRecipes.skillTable().explain(id -> level(player, id)); }
}
