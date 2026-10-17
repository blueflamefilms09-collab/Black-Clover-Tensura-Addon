package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.rules.BossHealPolicy;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.boss.rules.BossSkillRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Cursed Spirit is a borrowed body (usually another mod's monster), so none of the boss rules that are written against this mod's own bosses
 * ({@code BossIdentity}) would reach it, and the brief says "boss rules apply, drops nothing special". This class applies them to anything tagged
 * {@code nusmp_cursed_spirit}, reusing the project's own pure policy and config so the spirit follows the owner's switches and can never drift from
 * them: (1) NO HEALING IN COMBAT: a {@code LivingHealEvent} on a spirit that has a target, or was hurt in the last ten seconds, is zeroed and
 * cancelled ({@code BossHealPolicy.blocksHeal}, switch {@code bossRules.stripRegeneration}); (2) NO FORBIDDEN SKILLS: when the spirit is made, every
 * skill {@code BossSkillRules.forbidden} names (regeneration, Unique, Ultimate) is taken off it, the same sweep a boss gets; (3) NOTHING TO LOOT: its
 * drops are cancelled and its experience set to zero (it is an apparition, so the stones, meat and relics of the rest of the dungeon never come
 * from it, and neither does the Order's ledger hear of it); (4) NO ORPHANS: a spirit that is loaded from disk and is not the one a running
 * catastrophe tracks (the server restarted in the middle of the fight, which ends the catastrophe) is refused entry to the level, so no half-fought
 * spirit wanders a finished vault. The heal listener runs for every heal in the game, so its first line is a tag lookup and nothing else.
 */
final class SpiritRules {
    private SpiritRules() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    static boolean isSpirit(Entity e) { return e != null && e.getTags().contains(CursedSpirits.TAG); }

    // ---------------------------------------------------------------- no healing in combat
    static void onHeal(LivingHealEvent e) {
        LivingEntity le = e.getEntity();
        if (le.level().isClientSide || !isSpirit(le)) return;
        boolean target = le instanceof Mob m && m.getTarget() != null && m.getTarget().isAlive();
        if (BossHealPolicy.blocksHeal(BossRulesConfig.stripRegeneration(), true, BossHealPolicy.inCombat(target, le.tickCount, le.getLastHurtByMobTimestamp()), false)) {
            e.setAmount(0f);
            e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- nothing to loot
    static void onDrops(LivingDropsEvent e) {
        if (!e.getEntity().level().isClientSide && isSpirit(e.getEntity())) e.setCanceled(true);
    }

    static void onExperience(LivingExperienceDropEvent e) {
        if (!e.getEntity().level().isClientSide && isSpirit(e.getEntity())) e.setDroppedExperience(0);
    }

    // ---------------------------------------------------------------- no orphans
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !e.loadedFromDisk() || !isSpirit(e.getEntity())) return;
        if (!CatastropheRunner.knowsSpirit(e.getEntity().getUUID())) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- no forbidden skills
    /** Takes every skill the boss rules forbid off the entity (the same sweep a boss gets). Returns how many were removed; never touches a player. */
    static int sweepSkills(LivingEntity entity) {
        if (entity == null || entity instanceof Player || entity.level().isClientSide) return 0;
        int removed = 0;
        try {
            Skills skills = SkillAPI.getSkillsFrom(entity);
            if (skills == null) return 0;
            List<ManasSkillInstance> mine = new ArrayList<>(skills.getLearnedSkills());
            for (ManasSkillInstance i : mine) {
                if (!BossSkillRules.forbidden(i.getSkill())) continue;
                skills.forgetSkill(i.getSkillId());
                removed++;
            }
            if (removed > 0) skills.markDirty();
        } catch (Throwable noSkills) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] mega crucible: the skill sweep of a Cursed Spirit failed: {}", noSkills.toString());
            }
        }
        return removed;
    }
}
