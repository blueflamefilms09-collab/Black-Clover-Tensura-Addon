package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: a world saved before 1.96.0 holds the old Skill Tree record in the player's data (the lines the player owned, their Skill Points, bought node levels, the equipped line, the switches) and may hold skills
 * of the removed magics and of the old tree. Nothing may crash on them, and a player must not lose what they owned. So at login (and respawn) this runs once per player who still has something to move: every
 * line the player owned and that still exists becomes the UNIQUE skill of that book, taught switched on ({@link UniqueBooks#grant}; a player who already holds the inert stand-in of 1.92 with the same id
 * gets it switched on); a line of a removed magic is dropped (its skill ids stay registered as inert stubs and are forgotten); the Skill Points, node levels, equipped line and switches are CLEARED, so the old
 * record is ignored from then on. Only when every grant went through are the old numbers cleared, so a refusal from the skill list leaves them for the next login (the grants are idempotent). The part of
 * the record that is not the tree's, the Black Market's gold ({@code extra("market")}), is never touched. Every login also forgets the inert placeholders and the technique skills of the old tree, which cannot be
 * used any more. The decision is a pure function ({@link MigrationPlan#plan}) so the mapping is unit tested; the rest is guarded, because a login must never fail.
 */
public final class LegacyMigration {
    private LegacyMigration() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** True when the old record holds anything that is not the tree's own gold: routes, points, node levels, a class or switches. */
    public static boolean hasOldData(IPlayerSkillTree t) {
        return !t.unlockedRoutes().isEmpty() || t.sp() > 0 || t.spSpent() > 0 || !t.nodeLevels().isEmpty() || !t.activeClass().isEmpty() || !t.passiveToggles().isEmpty();
    }

    /** Login and respawn: forget the dead skills, then move the old record once. Never throws. */
    public static void onJoin(ServerPlayer p) {
        try {
            if (p == null || p instanceof FakePlayer) return;
            forgetDeadSkills(p);
            migrate(p);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] skill books: could not move the old skill tree data of {}", p == null ? "?" : p.getGameProfile().getName(), e);
        }
    }

    /** Forgets every inert placeholder and every technique skill of the old tree the player still holds. Returns how many. */
    public static int forgetDeadSkills(ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills == null) return 0;
        int n = 0;
        for (ManasSkillInstance inst : new ArrayList<>(skills.getLearnedSkills())) {
            if (inst.getSkill() instanceof LegacySkill || inst.getSkill() instanceof TreeSkill) {
                skills.forgetSkill(inst.getSkillId());
                n++;
            }
        }
        return n;
    }

    private static void migrate(ServerPlayer p) {
        IPlayerSkillTree tree = SkillTreeAttachments.get(p);
        if (!hasOldData(tree)) return;
        MigrationPlan.Plan plan = MigrationPlan.plan(tree.unlockedRoutes(), UniqueSkills.bares());
        int taught = 0;
        boolean allDone = true;
        for (String bare : plan.grant()) {
            UniqueBooks.Grant g = UniqueBooks.grant(p, bare, false);
            if (g == UniqueBooks.Grant.FAILED) {
                allDone = false;
                continue;
            }
            ManasSkillInstance inst = UniqueBooks.held(p, bare);
            if (inst != null && !inst.isToggled()) {                           // the 1.92 stand-in of an owned line was never switched on
                inst.setToggled(true);
                inst.markDirty();
            }
            if (g == UniqueBooks.Grant.GRANTED) taught++;
        }
        if (!allDone) {
            LOG.warn("[nusmp] skill books: {} could not be given every skill of the old tree yet; the old record is kept for the next login", p.getGameProfile().getName());
            return;
        }
        clearOld(tree);
        LOG.info("[nusmp] skill books: {} moved from the old skill tree: {} unique skill(s) taught, {} line(s) dropped", p.getGameProfile().getName(), taught, plan.dropped().size());
        if (!plan.grant().isEmpty()) {
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.skillbook.migrated", "The Skill Tree is gone: the %s skill line(s) you owned are now unique skills in your Ability Selection.",
                    plan.grant().size()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Clears the tree's own numbers and leaves the Black Market's gold (and any other sub-record) alone. */
    static void clearOld(IPlayerSkillTree tree) {
        Set<String> routes = new TreeSet<>(tree.unlockedRoutes());
        for (String key : tree.nodeLevels().keySet()) routes.add(MigrationPlan.routePart(key));
        for (String key : tree.passiveToggles().keySet()) routes.add(MigrationPlan.routePart(key));
        for (String r : routes) {
            tree.clearRoute(r);
            tree.removeRoute(r);
        }
        tree.setSp(0);
        tree.setSpSpent(0);
        tree.setActiveClass("");
        tree.setClassChangedAt(0L);
    }
}
