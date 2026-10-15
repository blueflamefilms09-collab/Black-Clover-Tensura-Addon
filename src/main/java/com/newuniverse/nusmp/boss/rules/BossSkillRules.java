package com.newuniverse.nusmp.boss.rules;

import com.mojang.logging.LogUtils;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Minecraft-facing half of {@link BossSkillPolicy}. It turns a ManasCore skill into the two strings the policy needs
 * (registry id and category) and applies the verdict at every door a skill can use to reach a boss: the grant path
 * ({@code BossSkills.grant}, the Usurper lease, {@code TensuraCaster.learn}), the periodic sweep that cleans bosses saved under the
 * old design, and the activation veto. The category is read the robust way: Tensura's {@code Skill.getType()} (an enum with UNIQUE and
 * ULTIMATE) when the object is a Tensura {@link Skill}; if that cannot be read, the class's package ({@code ...skill.unique.},
 * {@code ...skill.ultimate.}); and a Tensura skill whose family cannot be told at all is "unknown", which the policy fails closed on
 * through its known-id table. Magic and Battlewill are not {@code Skill}s, so spellcasting is never caught by the unique/ultimate ban.
 * Nothing here ever touches a player: every method returns early for {@link Player} and for anything {@link BossIdentity} does not call
 * a boss.
 */
public final class BossSkillRules {
    private BossSkillRules() {}

    private static boolean warned;

    /** The category string of a skill (see the constants of {@link BossSkillPolicy}). */
    public static String categoryOf(ManasSkill s) {
        if (s == null) return BossSkillPolicy.UNKNOWN;
        try {
            if (s instanceof Skill sk && sk.getType() != null) return sk.getType().getSerializedName().toLowerCase(java.util.Locale.ROOT);
        } catch (Throwable unreadable) { /* fall through to the package */ }
        String cn = s.getClass().getName();
        if (cn.contains(".skill.unique.")) return BossSkillPolicy.UNIQUE;
        if (cn.contains(".skill.ultimate.")) return BossSkillPolicy.ULTIMATE;
        if (cn.contains(".ability.magic.")) return BossSkillPolicy.MAGIC;
        if (cn.contains(".ability.battlewill.")) return BossSkillPolicy.BATTLEWILL;
        if (cn.contains("io.github.manasmods.tensura.")) return BossSkillPolicy.UNKNOWN;
        return BossSkillPolicy.OTHER;
    }

    /** The registry id of a skill, or "" when it has none. */
    public static String idOf(ManasSkill s) {
        try {
            ResourceLocation id = s == null ? null : s.getRegistryName();
            return id == null ? "" : id.toString();
        } catch (Throwable none) { return ""; }
    }

    /** Is this skill forbidden to bosses by the current config? */
    public static boolean forbidden(ManasSkill s) {
        return s != null && BossSkillPolicy.isForbidden(idOf(s), categoryOf(s), BossRulesConfig.settings());
    }

    /** Is the skill with this registry id forbidden to bosses? An id the registry does not know is judged by its name alone. */
    public static boolean forbidden(String skillId) {
        ResourceLocation id = ResourceLocation.tryParse(skillId == null ? "" : skillId);
        if (id == null) return false;
        ManasSkill s = null;
        try { s = SkillAPI.getSkillRegistry().get(id); } catch (Throwable noSkills) { /* name only */ }
        return s != null ? forbidden(s) : BossSkillPolicy.isForbidden(id.toString(), null, BossRulesConfig.settings());
    }

    /** Should this grant to 'boss' be refused? True only for a boss (never a player) and a forbidden skill. */
    public static boolean refuses(LivingEntity boss, String skillId) {
        return boss != null && !(boss instanceof Player) && BossIdentity.isBoss(boss) && forbidden(skillId);
    }

    /** Should this Tensura skill be kept out of a boss's kit? */
    public static boolean refuses(LivingEntity boss, ManasSkill skill) {
        return boss != null && !(boss instanceof Player) && BossIdentity.isBoss(boss) && forbidden(skill);
    }

    /** Is an effect (a registry id) off limits for a boss's permanent buffs? */
    public static boolean forbiddenEffect(String effectId) { return BossSkillPolicy.isForbiddenEffect(effectId, BossRulesConfig.settings()); }

    /** Is a spell page name part of the regeneration family (a healing page the Tower library must not cast)? */
    public static boolean forbiddenSpellName(String name) {
        return BossRulesConfig.stripRegeneration() && BossSkillPolicy.matchesWord(name, BossRulesConfig.forbiddenWords());
    }

    /** Should an activation, release or toggle by 'user' of 'skill' be cancelled? */
    public static boolean vetoes(ManasSkillInstance inst, LivingEntity user) {
        if (inst == null || user == null || user instanceof Player || user.level().isClientSide) return false;
        return BossIdentity.isBoss(user) && forbidden(inst.getSkill());
    }

    /**
     * Takes every forbidden skill off a boss (so bosses saved under the old design are cleaned, and nothing slips in through another
     * mod). Returns how many were removed. Never touches a player or a non-boss.
     */
    public static int sweep(LivingEntity boss) {
        if (boss == null || boss instanceof Player || boss.level().isClientSide || !BossIdentity.isBoss(boss)) return 0;
        int removed = 0;
        try {
            Skills skills = SkillAPI.getSkillsFrom(boss);
            if (skills == null) return 0;
            List<ManasSkillInstance> mine = new ArrayList<>(skills.getLearnedSkills());
            for (ManasSkillInstance i : mine) {
                if (!forbidden(i.getSkill())) continue;
                skills.forgetSkill(i.getSkillId());
                removed++;
            }
            if (removed > 0) skills.markDirty();
        } catch (Throwable noSkills) {
            if (!warned) {
                warned = true;
                LogUtils.getLogger().warn("[nusmp] boss skill sweep failed for {}: {}", boss.getName().getString(), noSkills.toString());
            }
        }
        return removed;
    }
}
