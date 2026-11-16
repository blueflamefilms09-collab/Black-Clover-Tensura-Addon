package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Why: Devil's Footprints is folded into Hell Walker (the Shinra skill, docs/shinra_rework.md). The id {@code devils_footprints} stays registered (old worlds load; it keeps its dash and kick as the
 * helpers the Shinra skill uses), and a player who holds it is moved to {@code hell_walker} with the same mastery, once, at the next evolution check (every 5 seconds, per player, one skill lookup). A holder
 * of Adolla Burst (or of Hell Walker already) only loses the old Unique, because Adolla Burst and Hell Walker contain it. Nothing is stored per player and nothing runs without the SERVER setting
 * {@code shinraRework}. The old skill is not inert on purpose: until the check reaches a player, it keeps working, so nobody is ever left without their dash.
 */
final class ShinraMigration {
    private ShinraMigration() {}

    static void run(ServerPlayer player, Skills skills) {
        if (skills == null || !NUConfig.shinraRework()) return;
        try {
            Optional<ManasSkillInstance> old = skills.getSkill(NUSkills.DEVILS_FOOTPRINTS.get().getRegistryName());
            if (old.isEmpty()) return;
            boolean has = skills.getSkill(NUSkills.HELL_WALKER.get().getRegistryName()).isPresent()
                    || skills.getSkill(NUSkills.ADOLLA_BURST.get().getRegistryName()).isPresent();
            if (!has) {
                ManasSkillInstance fresh = NUSkills.HELL_WALKER.get().createDefaultInstance();
                fresh.getOrCreateTag().merge(old.get().getOrCreateTag().copy());
                fresh.setMastery(old.get().getMastery());
                if (!skills.learnSkill(fresh, SkillMsg.t("shinra.folded", "Devil's Footprints is now part of your Shinra Kusakabe skill.").withStyle(ChatFormatting.GOLD))) return;
            }
            skills.forgetSkill(old.get().getSkillId());
        } catch (RuntimeException ignored) { /* a skill list that cannot be changed now is looked at again in 5 seconds */ }
    }
}
