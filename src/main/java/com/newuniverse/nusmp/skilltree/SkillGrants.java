package com.newuniverse.nusmp.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the owner's abilities are real ManasCore skills (so they sit in Tensura's skill menu and fire from Tensura's own skill keys), which means the tree has to LEARN a node's skill when the
 * node is bought and FORGET it when the node goes, and has to keep the two in step after a crash, a datapack change, a death or a command. This is the only code that touches a player's skill
 * storage for tree skills, so the rules for it are in one place. They are deliberately asymmetric: LEARNING needs the registry (it has to know which skill a node grants), but FORGETTING is
 * decided from the player's own saved tree alone, never from the registry. A skill instance carries its mastery, so forgetting it is a real loss; if a datapack error made a route vanish from
 * the registry for one reload, a registry-based rule would strip every player's techniques, whereas the tree-based rule forgets only what the saved tree itself says is not bought. It only ever
 * touches a {@link TreeSkill}: it refuses to TEACH a node's skill that is not one (a route file must not be able to hand out a Tensura unique skill or a Grimoire page, which the cast gate
 * would not guard) and it never FORGETS anything a player earned another way. Every call is guarded so a storage that is not ready (a stand-in player in a
 * test, a skill list that vetoes the change) is a logged refusal and never an exception.
 */
final class SkillGrants {
    private static final Logger LOG = LogUtils.getLogger();
    /** Skill ids already reported as missing, so a node that names an unregistered skill is one log line per session and not one per login. Emptied when the server stops. */
    private static final Set<String> WARNED = new HashSet<>();

    private SkillGrants() {}

    /** The server stopped: forget what was reported. */
    static void clear() { WARNED.clear(); }

    /**
     * Teaches the player the skill with this registry id. {@code announce} shows the "technique learned" line (a purchase) or stays quiet (a login). True when the player has the skill afterwards;
     * false when the id is not a registered skill or the storage refused.
     */
    static boolean learn(ServerPlayer player, String skillId, boolean announce) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(skillId);
            if (id == null) return false;
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            if (skill == null) {
                if (WARNED.add(skillId)) LOG.warn("[nusmp] skill tree: a node grants '{}', which is not a registered skill", skillId);
                return false;
            }
            if (!(skill instanceof TreeSkill)) {
                if (WARNED.add(skillId)) LOG.warn("[nusmp] skill tree: a node grants '{}', which is not a TreeSkill, so the tree will not teach it (the cast gate only guards tree skills)", skillId);
                return false;
            }
            Skills skills = SkillAPI.getSkillsFrom(player);
            if (skills == null) return false;
            if (skills.getSkill(id).isPresent()) return true;
            boolean ok = announce
                    ? skills.learnSkill(skill.createDefaultInstance(), Component.translatable("tree.msg.learned", skill.getName()).withStyle(ChatFormatting.GOLD))
                    : skills.learnSkill(skill.createDefaultInstance());
            return ok || skills.getSkill(id).isPresent();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] skill tree: could not teach '{}' to {}", skillId, player.getGameProfile().getName(), e);
            return false;
        }
    }

    /** Teaches the skill of every bought node of one route (silently). */
    static void learnBought(ServerPlayer player, SkillRoute route) {
        IPlayerSkillTree tree = SkillTreeApi.of(player);
        for (SkillNode n : route.nodes()) if (n.grantsSkill() && tree.nodeLevel(route.id(), n.id()) >= 1) learn(player, n.skill(), false);
    }

    /**
     * Makes the player's tree skills match the saved tree: forgets every {@link TreeSkill} whose route is not owned or whose node is not bought, then learns the skill of every bought node of
     * every owned route the registry knows. Nothing else is touched.
     */
    static void reconcile(ServerPlayer player) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(player);
            if (skills == null) return;
            List<ManasSkillInstance> held = new ArrayList<>(skills.getLearnedSkills());           // a copy: forgetting changes the live list
            for (ManasSkillInstance inst : held) {
                if (!(inst.getSkill() instanceof TreeSkill ts)) continue;
                if (SkillTreeApi.isNodeActive(player, ts.routeId(), ts.nodeId())) continue;
                skills.forgetSkill(inst.getSkillId());
            }
            IPlayerSkillTree tree = SkillTreeApi.of(player);
            for (String routeId : new ArrayList<>(tree.unlockedRoutes())) {
                SkillRoute route = RouteRegistry.byId(routeId);
                if (route != null) learnBought(player, route);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] skill tree: could not reconcile the skills of {}", player.getGameProfile().getName(), e);
        }
    }
}
