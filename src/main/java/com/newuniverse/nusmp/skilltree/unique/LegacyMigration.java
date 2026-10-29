package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: a world saved before the Skill Books were removed holds, in a player's data, skills that no longer exist (the unique skills the books taught and the magics removed earlier, all kept as inert
 * {@link LegacySkill} stubs so the save still loads) and may still hold the old Skill Tree record (routes, Skill Points, node levels, the equipped line, the switches). Nothing may crash on them. So at login and
 * respawn this forgets every inert stub the player holds, and clears the old record's own numbers; the part of the record that is not the tree's, the Black Market's gold ({@code extra("market")}), is never
 * touched. It teaches nothing any more (the skills are gone), so a player does not get a skill back for a route they owned. Every step is guarded, because a login must never fail.
 */
public final class LegacyMigration {
    private LegacyMigration() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean done;

    /** Hooks login and respawn on the game bus. Called once from {@code SkillBookSystem.init}. */
    public static synchronized void register() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) onJoin(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) onJoin(p); });
    }

    /** True when the old record holds anything that is not the tree's own gold: routes, points, node levels, a class or switches. */
    public static boolean hasOldData(IPlayerSkillTree t) {
        return !t.unlockedRoutes().isEmpty() || t.sp() > 0 || t.spSpent() > 0 || !t.nodeLevels().isEmpty() || !t.activeClass().isEmpty() || !t.passiveToggles().isEmpty();
    }

    /** Login and respawn: forget the dead skills, then clear the old record. Never throws. */
    public static void onJoin(ServerPlayer p) {
        try {
            if (p == null || p instanceof FakePlayer) return;
            forgetDeadSkills(p);
            IPlayerSkillTree tree = SkillTreeAttachments.get(p);
            if (hasOldData(tree)) clearOld(tree);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] could not clear the old skill data of {}", p == null ? "?" : p.getGameProfile().getName(), e);
        }
    }

    /** Forgets every inert stub (a retired skill) the player still holds. Returns how many. */
    public static int forgetDeadSkills(ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills == null) return 0;
        int n = 0;
        for (ManasSkillInstance inst : new ArrayList<>(skills.getLearnedSkills())) {
            if (inst.getSkill() instanceof LegacySkill) {
                skills.forgetSkill(inst.getSkillId());
                n++;
            }
        }
        return n;
    }

    /** Clears the tree's own numbers and leaves the Black Market's gold (and any other sub-record) alone. */
    static void clearOld(IPlayerSkillTree tree) {
        Set<String> routes = new TreeSet<>(tree.unlockedRoutes());
        for (String key : tree.nodeLevels().keySet()) routes.add(routePart(key));
        for (String key : tree.passiveToggles().keySet()) routes.add(routePart(key));
        for (String r : routes) {
            tree.clearRoute(r);
            tree.removeRoute(r);
        }
        tree.setSp(0);
        tree.setSpSpent(0);
        tree.setActiveClass("");
        tree.setClassChangedAt(0L);
    }

    /** The route part of a node key ({@code nusmp:lady_luck#strike} to {@code nusmp:lady_luck}); a key without a {@code #} is returned unchanged. */
    static String routePart(String key) {
        int i = key.indexOf('#');
        return i < 0 ? key : key.substring(0, i);
    }
}
