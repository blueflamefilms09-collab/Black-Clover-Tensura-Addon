package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.kit.TechniqueSkill;
import com.newuniverse.nusmp.skilltree.line.LineCast;
import com.newuniverse.nusmp.skilltree.line.LineState;
import com.newuniverse.nusmp.skilltree.line.LineText;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: this is the server side of "a Skill Book teaches a Unique skill": who owns which skill, teaching one, and running its art. It is the only place that touches a player's skill list for the books, so the
 * rules are in one spot. OWNING a book is owning its {@link LineUniqueSkill} (the ManasCore skill list is the single record: no second copy in a capability that could drift). {@link #grant} teaches the
 * skill switched ON (so its passives work at once) and refuses if it is already there. {@link #useArt} is what a press of the skill key does: it finds the art of the current mode in the line's route file, checks it
 * has awakened (mastery, {@link MasteryLevels}) and is off cooldown, and runs it through the same line engine the arts always used ({@code LineCast}: the magicule price through {@code AbilityKit.pay}, the target
 * check, the executor, damage through the balance law), or through the technique class for the four Heavy Knight books, whose techniques are classes and not catalogue kinds. A used art earns a point of
 * mastery. {@link #ownedLines} is what the passive gate reads: the owned unique skills of a player, each with its switch. Every method that talks to ManasCore is guarded, because a storage that is not ready (a
 * stand-in player in a test, a skill list that vetoes a change) must be a logged refusal and never an exception.
 */
public final class UniqueBooks {
    private UniqueBooks() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Ticks between the points of mastery a switched-on skill earns by itself (ten seconds). */
    public static final int MASTERY_BEAT = 200;

    /** An owned unique skill of a player with its instance (the switch, the mastery). */
    public record Owned(LineUniqueSkill skill, ManasSkillInstance instance) {
        /** The mastery as a fraction 0..1. */
        public double fraction() { return UniqueBooks.fraction(instance); }

        /** True when the skill is switched on (its passives count). */
        public boolean on() { return instance.isToggled(); }
    }

    /** What {@link #grant} did. */
    public enum Grant { GRANTED, ALREADY, UNKNOWN, FAILED }

    /** The mastery of an instance as a fraction 0..1. */
    public static double fraction(ManasSkillInstance instance) {
        if (instance == null) return 0.0;
        return MasteryLevels.fraction(instance.getMastery(), instance.getMaxMastery());
    }

    // ---------------------------------------------------------------- ownership
    /** The unique skills of the books a player owns, in the order of their skill list. Empty when the storage is not ready. */
    public static List<Owned> ownedLines(Player p) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return List.of();
            List<Owned> out = null;
            for (ManasSkillInstance inst : skills.getLearnedSkills()) {
                if (inst.getSkill() instanceof LineUniqueSkill s) {
                    if (out == null) out = new ArrayList<>(4);
                    out.add(new Owned(s, inst));
                }
            }
            return out == null ? List.of() : out;
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("[nusmp] could not read the skill list of {}", p == null ? "?" : p.getGameProfile().getName(), e);
            return List.of();
        }
    }

    /** The owned skill of this route, or null. */
    public static Owned ownedOf(Player p, String routeId) {
        for (Owned o : ownedLines(p)) if (o.skill().routeId().equals(routeId)) return o;
        return null;
    }

    /** The instance of a book's skill the player holds, or null. */
    public static ManasSkillInstance held(Player p, String bareOrRouteId) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return null;
            return skills.getSkill(UniqueSkills.skillId(bareOrRouteId)).orElse(null);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** True when the player owns the unique skill of this book. */
    public static boolean owns(Player p, String bareOrRouteId) { return held(p, bareOrRouteId) != null; }

    // ---------------------------------------------------------------- teaching
    /**
     * Teaches the player the unique skill of a book, switched on. Never throws. {@link Grant#ALREADY} when the player has it (nothing is changed), {@link Grant#UNKNOWN} when no skill is registered for the id.
     */
    public static Grant grant(ServerPlayer p, String bareOrRouteId, boolean announce) {
        try {
            var holder = UniqueSkills.holder(bareOrRouteId);
            if (holder == null) return Grant.UNKNOWN;
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return Grant.FAILED;
            ResourceLocation id = UniqueSkills.skillId(bareOrRouteId);
            if (skills.getSkill(id).isPresent()) return Grant.ALREADY;
            ManasSkillInstance inst = holder.get().createDefaultInstance();
            inst.setToggled(true);
            boolean ok = announce
                    ? skills.learnSkill(inst, holder.get().getChatDisplayName(true))
                    : skills.learnSkill(inst);
            return ok || skills.getSkill(id).isPresent() ? Grant.GRANTED : Grant.FAILED;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] skill books: could not teach '{}' to {}", bareOrRouteId, p.getGameProfile().getName(), e);
            return Grant.FAILED;
        }
    }

    /** Takes the unique skill of a book away again (the self-test and the migration use it). True when the player no longer has it. */
    public static boolean forget(ServerPlayer p, String bareOrRouteId) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return false;
            ResourceLocation id = UniqueSkills.skillId(bareOrRouteId);
            if (skills.getSkill(id).isPresent()) skills.forgetSkill(id);
            return skills.getSkill(id).isEmpty();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] skill books: could not forget '{}' for {}", bareOrRouteId, p.getGameProfile().getName(), e);
            return false;
        }
    }

    // ---------------------------------------------------------------- the arts
    /** True for a node that is one of the skill's arts (a mode): an active node of a generated line, or a classic node that grants a technique. In route-file order this is the list of modes. */
    public static boolean isArt(SkillNode n) { return n.isLineNode() ? n.nodeType() == SkillNode.NodeType.ACTIVE : n.grantsSkill(); }

    /** The arts of a route in file order: the first is the signature art. */
    public static List<SkillNode> arts(SkillRoute route) {
        List<SkillNode> out = new ArrayList<>();
        if (route != null) for (SkillNode n : route.nodes()) if (isArt(n)) out.add(n);
        return out;
    }

    /** The level a node of an owned, switched-on route counts at for this player now, or 0 (the player does not own the skill, or it is switched off). */
    public static int passiveLevel(ServerPlayer p, String routeId, String nodeId) {
        for (Owned o : ownedLines(p)) {
            if (!o.skill().routeId().equals(routeId) || !o.on()) continue;
            SkillNode n = RouteRegistry.node(routeId, nodeId);
            return MasteryLevels.level(o.fraction(), n == null ? 1 : n.maxLevel());
        }
        return 0;
    }

    /** What a press of the skill key does: casts the art of the mode. */
    public static void useArt(ServerPlayer p, ManasSkillInstance inst, LineUniqueSkill skill, int mode) {
        try {
            if (!p.isAlive() || p.isSpectator()) return;
            SkillRoute route = RouteRegistry.byId(skill.routeId());
            if (route == null) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("msg.nusmp.unique.no_data", "This skill's data is not loaded on the server.").withStyle(ChatFormatting.RED), true);
                return;
            }
            List<SkillNode> arts = arts(route);
            int m = MasteryLevels.wrapMode(mode, arts.size());
            if (m < 0) {
                p.displayClientMessage(LineText.tr(LineText.NO_ART).withStyle(ChatFormatting.GRAY), true);
                return;
            }
            double frac = fraction(inst);
            SkillNode node = arts.get(m);
            if (!MasteryLevels.awake(m, arts.size(), frac)) {
                p.displayClientMessage(LineText.tr(LineText.ART_LOCKED, node.name(), MasteryLevels.percentNeeded(m, arts.size())).withStyle(ChatFormatting.RED), true);
                return;
            }
            if (inst.onCoolDown(m)) {
                p.displayClientMessage(LineText.tr(LineText.CAST_COOLDOWN, node.name(), inst.getCoolDown(m)).withStyle(ChatFormatting.RED), true);
                return;
            }
            int level = MasteryLevels.level(frac, node.maxLevel());
            int cooldown;
            LineCast.Result result;
            if (node.isLineNode()) {
                result = LineCast.cast(p, route, node, level);
                cooldown = LineCast.cooldownTicks(node, level);
            } else {
                TechniqueSkill technique = techniqueOf(node);
                if (technique == null) return;
                cooldown = technique.cooldownAt(level);
                result = castTechnique(p, inst, technique, route, node, level, cooldown);
            }
            if (result == LineCast.Result.OK) {
                inst.setCoolDown(NUSMP.ticksToSeconds(cooldown), m);
                inst.addMasteryPoint(p, 1);
                inst.markDirty();
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] skill books: the art of {} failed for {}", skill.routeId(), p.getGameProfile().getName(), e);
        }
    }

    private static TechniqueSkill techniqueOf(SkillNode node) {
        ResourceLocation id = ResourceLocation.tryParse(node.skill());
        if (id == null) return null;
        ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
        return skill instanceof TechniqueSkill ft ? ft : null;
    }

    private static LineCast.Result castTechnique(ServerPlayer p, ManasSkillInstance inst, TechniqueSkill technique, SkillRoute route, SkillNode node, int level, int cooldown) {
        String key = SkillRoute.key(route.id(), node.id());
        long now = p.serverLevel().getGameTime();
        long ready = LineState.cooldownEnd(p, key);
        if (now < ready) {
            p.displayClientMessage(LineText.tr(LineText.CAST_COOLDOWN, node.name(), (ready - now + 19) / 20).withStyle(ChatFormatting.RED), true);
            return LineCast.Result.COOLDOWN;
        }
        if (!technique.castDirect(p, inst, level)) return LineCast.Result.NO_TARGET;
        LineState.setCooldownEnd(p, key, now + cooldown);
        return LineCast.Result.OK;
    }
}
