package com.newuniverse.nusmp.boss;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Tensura skills on this mod's bosses (never on anything else). Learning goes through ManasCore's skill list exactly as a player's;
 * a skill id the registry does not know, or a list that refuses, does nothing. {@link #forget} refuses every player and every entity
 * that is not a registry-listed boss: the boss overhaul can lend a boss a skill and take it back, but it has no way to remove a
 * skill from a player.
 */
public final class BossSkills {
    private BossSkills() {}

    /** Is the skill in the registry? */
    public static boolean exists(String skillId) {
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        try { return SkillAPI.getSkillRegistry().get(id) != null; } catch (Throwable noSkills) { return false; }
    }

    public static boolean has(LivingEntity e, String skillId) {
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        try {
            Skills skills = SkillAPI.getSkillsFrom(e);
            return skills != null && skills.getSkill(id).isPresent();
        } catch (Throwable noSkills) { return false; }
    }

    /** Teaches a boss a skill; true when it has it afterwards. Does nothing for anything that is not a registry-listed boss, and (1.37.0) nothing for a regeneration, Unique or Ultimate skill. */
    public static boolean grant(LivingEntity boss, String skillId) {
        if (!BossEntities.isModBoss(boss)) return false;
        if (com.newuniverse.nusmp.boss.rules.BossSkillRules.refuses(boss, skillId)) return false;    // 1.37.0: the boss rules, see boss/rules/BossSkillPolicy
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        try {
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            Skills skills = SkillAPI.getSkillsFrom(boss);
            if (skill == null || skills == null) return false;
            if (skills.getSkill(id).isEmpty()) skills.learnSkill(skill.createDefaultInstance());
            return skills.getSkill(id).isPresent();
        } catch (Throwable noSkills) { return false; }
    }

    /** Takes a lent skill back from a BOSS. Players and every non-boss are refused: nothing here can remove a player's skill. */
    public static boolean forget(LivingEntity boss, String skillId) {
        if (boss instanceof Player || !BossEntities.isModBoss(boss)) return false;
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        try {
            Skills skills = SkillAPI.getSkillsFrom(boss);
            if (skills == null || skills.getSkill(id).isEmpty()) return true;
            skills.forgetSkill(id);
            return skills.getSkill(id).isEmpty();
        } catch (Throwable noSkills) { return false; }
    }
}
