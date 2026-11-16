package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.SummonerRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: with the Summoner rework on, Hawk Eye and Spirit Card are modes of the Summoner, so a player who still holds the old skills (an old world, or an
 * admin grant) must not lose them: this carries each one over. The holder gets the Summoner (or keeps the Summoner of Beginnings), the mode it replaces is
 * opened for good ({@link SummonerRules#FORCE_BIRD} / {@link SummonerRules#FORCE_SPIRIT} in the Summoner's own tag), a toggle that was on stays on, the old
 * mastery share is kept if it is higher, and the old skill is forgotten (its id stays registered as an inert skill, see {@link HawkEyeSkill}). The pattern is
 * the one of {@code skilltree.unique.LegacyMigration}, but it runs from the existing player tick (once every five seconds, two registry look-ups per player)
 * because a Hawk Eye can be granted at any time, not only at login. Nothing runs with the setting off.
 */
final class SummonerMigration {
    private SummonerMigration() {}

    /** Called from the player tick; does its work only on a player who still holds one of the two old skills. Never throws. */
    static void check(ServerPlayer player) {
        try {
            if (!NUConfig.summonerRework()) return;
            Skills skills = SkillAPI.getSkillsFrom(player);
            if (skills == null) return;
            var hawk = skills.getSkill(NUSkills.HAWK_EYE.get().getRegistryName());
            var spirit = skills.getSkill(NUSkills.SPIRIT_CARD.get().getRegistryName());
            if (hawk.isEmpty() && spirit.isEmpty()) return;

            ManasSkillInstance target = SummonerSkill.holder(player);
            if (target == null) {
                boolean learned = skills.learnSkill(NUSkills.SUMMONER.get().createDefaultInstance(),
                        SkillMsg.t("summoner.merged", "Hawk Eye and Spirit Card are now part of your Summoner.").withStyle(ChatFormatting.GOLD));
                if (!learned) return;
                target = SummonerSkill.holder(player);
                if (target == null) return;
            }
            if (hawk.isPresent()) carry(skills, hawk.get(), target, SummonerRules.BIRD);
            if (spirit.isPresent()) carry(skills, spirit.get(), target, SummonerRules.SPIRIT);
            target.markDirty();
        } catch (RuntimeException | LinkageError e) {
            // a tick must never fail on a skill that cannot be carried now; it is tried again in five seconds
        }
    }

    private static void carry(Skills skills, ManasSkillInstance old, ManasSkillInstance target, int mode) {
        CompoundTag tag = target.getOrCreateTag();
        int bit = SummonerRules.forceFlag(mode);
        tag.putInt(SummonerSkill.TAG_FORCE, tag.getInt(SummonerSkill.TAG_FORCE) | bit);
        if (old.isToggled()) tag.putInt(SummonerSkill.TAG_ON, tag.getInt(SummonerSkill.TAG_ON) | bit);
        int oldMax = old.getMaxMastery(), newMax = target.getMaxMastery();
        if (oldMax > 0 && newMax > 0) {
            double moved = Math.min(1.0, old.getMastery() / oldMax) * newMax;
            if (moved > target.getMastery()) target.setMastery(moved);
        }
        skills.forgetSkill(old.getSkillId());
    }
}
