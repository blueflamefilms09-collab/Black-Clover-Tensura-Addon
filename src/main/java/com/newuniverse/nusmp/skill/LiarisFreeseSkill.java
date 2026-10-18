package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.GrowthRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;

/**
 * Liaris Freese (Bell Cranel): passive rapid growth. Kills can raise the mastery of ALL of your skills: each kill pays out with the
 * configured chance (liarisChance), giving each not-yet-mastered skill the configured points (liarisMasteryPerKill, tripled for a boss).
 * Guards: your own pets and summons, yourself and armour stands never count; a payout needs a quiet second since the last one (a farm that
 * kills dozens of mobs in one tick pays once); grimoire skills are skipped (their mastery opens pages, and pages never come from kills).
 * Press the skill to see how often it has grown you. The rules are in {@link GrowthRules}; {@link #onKill} is the death listener the
 * mod already registers.
 */
public class LiarisFreeseSkill extends NUSkill {
    private static final String GROWN = "Grown", LAST = "LastGrow";

    static final SkillText TEXT = SkillText.of("liaris_freese", "Liaris Freese",
            "Unique. Passive. While your feelings remain, so does your growth: your kills can raise the mastery of all your skills.");

    public LiarisFreeseSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, SkillMsg.t("liaris.status", "Liaris Freese: your skills have grown %s times.",
                    instance.getOrCreateTag().getInt(GROWN)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static boolean isGrimoire(ManasSkillInstance i) {
        return i.getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook || i.getSkill() instanceof GrimoireMagicSkill
                || i.getSkill() instanceof GrimoirePageSkill;
    }

    /** Death listener (registered by NUSMP). Also hands every death to {@link SkillEvents} so summons are cleaned up. */
    public static void onKill(LivingDeathEvent event) {
        try {
            SkillEvents.onDeath(event);
        } catch (RuntimeException ignored) {
            // upkeep must never stop the growth below
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (!GrowthRules.countsAsKill(victim == player, SkillTargets.isOwnedBy(victim, player.getUUID()) || SkillTargets.isSummon(victim),
                victim instanceof ArmorStand)) return;

        Skills skills = SkillAPI.getSkillsFrom(player);
        var owned = skills.getSkill(NUSkills.LIARIS_FREESE.get().getRegistryName());
        if (owned.isEmpty()) return;
        ManasSkillInstance liaris = owned.get();

        long now = player.level().getGameTime();
        var tag = liaris.getOrCreateTag();
        if (!GrowthRules.pays(NUConfig.LIARIS_CHANCE.get(), player.getRandom().nextDouble(), now, tag.getLong(LAST))) return;
        int amount = GrowthRules.amount(NUConfig.LIARIS_MASTERY_PER_KILL.get(), BalanceLaw.isBoss(victim));
        if (amount <= 0) return;

        tag.putLong(LAST, now);
        tag.putInt(GROWN, tag.getInt(GROWN) + 1);
        liaris.markDirty();
        for (ManasSkillInstance instance : new ArrayList<>(skills.getLearnedSkills())) {
            if (instance.isMastered(player) || isGrimoire(instance)) continue;
            instance.addMasteryPoint(player, amount);
            instance.markDirty();
        }
        SkillFx.spray(player.serverLevel(), false, player.position().add(0, 1.2, 0), 6, 0.3, 0.5, 0.3, 0.02);
    }
}
