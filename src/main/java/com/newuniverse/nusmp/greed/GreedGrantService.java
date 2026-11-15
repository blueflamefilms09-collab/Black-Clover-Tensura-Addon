package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Why: the glue between the Black Sword and the Tensura skill system: the wielder is given the Ultimate skill {@code anime_multiverse:lord_of_gluttonous_greed} when the sword binds, and loses it when the sword is no longer
 * theirs (checked at login, at a bind, at a release and every few seconds from the scan). Same shape as Shishou's grant: one map lookup per check, a change only when the list is wrong.
 */
public final class GreedGrantService {
    private GreedGrantService() {}

    private static Skills skillsOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {
            return null;
        }
    }

    public static boolean has(ServerPlayer p) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(NUSkills.LORD_OF_GLUTTONOUS_GREED.getId()).isPresent();
    }

    /** Makes the player's skill list right for the record: the wielder has the skill, everybody else does not. Returns true when something changed. */
    public static boolean sync(ServerPlayer p) {
        if (!GreedData.bound()) return false;
        boolean should = GreedConfig.enabled() && GreedData.get().isOwner(p.getUUID());
        Skills skills = skillsOf(p);
        if (skills == null) return false;
        Optional<ManasSkillInstance> inst = skills.getSkill(NUSkills.LORD_OF_GLUTTONOUS_GREED.getId());
        if (should && inst.isEmpty()) {
            skills.learnSkill(NUSkills.LORD_OF_GLUTTONOUS_GREED.get().createDefaultInstance(), Component.translatableWithFallback("greed.skill_msg.granted", "Greed answers: the Ultimate skill Lord of Gluttonous Greed is yours.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            return true;
        }
        if (!should && inst.isPresent()) {
            skills.forgetSkill(NUSkills.LORD_OF_GLUTTONOUS_GREED.getId(), Component.translatableWithFallback("greed.skill_msg.revoked", "Greed is no longer yours: the skill fades.").withStyle(ChatFormatting.GRAY));
            return true;
        }
        return false;
    }
}
