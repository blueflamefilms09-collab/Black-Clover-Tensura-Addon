package com.newuniverse.nusmp.skilltree.ft.dragon;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the Dragon Slayer routes can be played by the people who wrote them, so the CI server smoke test (which runs every key registered with {@link SelfTests})
 * must prove what a player would do with them, with a FAKE player standing in for a person. This is the part of that proof both routes share, so the two routes cannot drift
 * apart in what they prove: the route is loaded and every node agrees with the code (the skill exists, is a tree skill, serves that very node, and the numbers the datapack
 * holds NOW obey the budget); WITHOUT the route the stand-in cannot cast, loses nothing and hurts nothing (the cast gate works); with the route unlocked and every node at
 * level 1 and then at its top level (set on the tree attachment directly, so the proof does not depend on the purchase rules of another builder) every technique is cast once
 * next to a dummy mob, nothing throws or is swallowed, and at least one technique really hurts the dummy; and the route's passives answer through the probe the route supplies.
 * It runs only on the server thread, in a throw-away CI world, and puts everything back (the tree, the learned skills, the dummy), so the other tests are not disturbed. With an
 * empty route registry (before the core is merged) it still checks the shipped defaults and says so, so the prefix is always reported.
 */
public final class DragonSelfTests {
    private DragonSelfTests() {}

    /** The route-specific part: drives the passives of the route with the stand-in and adds a complaint for everything that does not hold. */
    @FunctionalInterface
    public interface Probe {
        void run(FakePlayer standIn, ServerLevel level, Zombie dummy, SkillRoute route, IPlayerSkillTree tree, List<String> problems);
    }

    /**
     * What a route supplies.
     *
     * @param routeId the route id ({@code nusmp:fire_dragon_slayer})
     * @param tester  the stand-in's name (unique per route)
     * @param tables  the route's shipped numbers
     * @param probe   the passives check
     */
    public record Spec(String routeId, String tester, DragonTables tables, Probe probe) {}

    private static SelfTests.Result fail(List<String> problems) {
        return SelfTests.Result.fail(problems.size() + " problem(s): " + String.join(" | ", problems.subList(0, Math.min(6, problems.size()))));
    }

    public static SelfTests.Result run(MinecraftServer server, Spec spec) {
        List<String> defaults = spec.tables().violations();
        if (!defaults.isEmpty()) return fail(defaults);
        if (!SkillTreeApi.isEnabled()) return SelfTests.Result.ok("skipped: the Skill Tree is switched off in the config (skilltree.enabled), so nothing can be cast; the shipped defaults obey the budget");
        if (RouteRegistry.all().isEmpty()) {
            return SelfTests.Result.ok("the route registry is empty (the core is not merged yet): the shipped defaults of " + spec.routeId() + " obey the budget, nothing to cast");
        }
        SkillRoute route = RouteRegistry.byId(spec.routeId());
        if (route == null) return SelfTests.Result.fail("route " + spec.routeId() + " is not in the registry (its file is missing or the loader rejected it)");

        List<String> problems = new ArrayList<>();
        List<SkillNode> skillNodes = new ArrayList<>();
        for (SkillNode n : route.nodes()) {
            if (!spec.tables().hasNode(n.id())) { problems.add("node " + n.id() + " has no code table"); continue; }
            DragonBudget.Kind kind = spec.tables().kind(n.id());
            problems.addAll(DragonBudget.check(n.id(), kind, n.maxLevel(), n.values()));
            if (!n.grantsSkill()) {
                if (kind != DragonBudget.Kind.PASSIVE) problems.add("node " + n.id() + " is active in the code table but grants no skill");
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(n.skill());
            ManasSkill s = id == null ? null : SkillAPI.getSkillRegistry().get(id);
            if (!(s instanceof TreeSkill ts)) problems.add("node " + n.id() + ": '" + n.skill() + "' is not a registered tree skill");
            else if (!ts.routeId().equals(route.id()) || !ts.nodeId().equals(n.id())) problems.add("node " + n.id() + ": the skill says it serves " + ts.routeId() + "/" + ts.nodeId());
            else skillNodes.add(n);
        }
        if (!problems.isEmpty()) return fail(problems);

        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.player(level, spec.tester(), 0.5, 100.0, 0.5);
        fp.setYRot(0.0f);
        fp.setXRot(0.0f);
        IPlayerSkillTree tree = SkillTreeApi.of(fp);
        Skills skills = SkillAPI.getSkillsFrom(fp);
        Zombie dummy = EntityType.ZOMBIE.create(level);
        if (dummy == null) return SelfTests.Result.fail("could not create the dummy mob");
        int casts = 0, hurtCasts = 0, refused = 0;
        try {
            dummy.moveTo(0.5, 100.0, 3.0, 180.0f, 0.0f);
            dummy.setNoAi(true);
            AttributeInstance maxHealth = dummy.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) maxHealth.setBaseValue(2000.0);
            dummy.setHealth(2000.0f);
            level.addFreshEntity(dummy);
            DragonSkill.takeFailures();

            // 1. without the route nothing can be cast, nothing is spent, nothing is hurt
            for (SkillNode n : skillNodes) {
                if (SkillTreeApi.canCast(fp, route.id(), n.id())) problems.add(n.id() + ": the stand-in can cast it without the route");
                ManasSkillInstance inst = learn(skills, n);
                if (inst == null) { problems.add(n.id() + ": the skill could not be taught to the stand-in"); continue; }
                refill(fp);
                double mana = magicule(fp);
                dummy.invulnerableTime = 0;
                float before = dummy.getHealth();
                try {
                    ((TreeSkill) inst.getSkill()).onRelease(inst, fp, 0, 0, 0);
                } catch (Throwable t) {
                    problems.add(n.id() + ": threw " + t + " when refused");
                    continue;
                }
                if (dummy.getHealth() < before) problems.add(n.id() + ": hurt the dummy without the route");
                if (magicule(fp) < mana) problems.add(n.id() + ": spent magicules without the route");
                if (inst.onCoolDown(0)) problems.add(n.id() + ": started a cooldown without the route");
            }
            problems.addAll(DragonSkill.takeFailures());

            // 2. with the route, at level 1 and at the top level, every technique once
            int queued = DragonTimers.pending();
            tree.addRoute(route.id());
            tree.setActiveClass(route.classId());
            for (int pass = 0; pass < 2; pass++) {
                for (SkillNode n : route.nodes()) tree.setNodeLevel(route.id(), n.id(), pass == 0 ? 1 : n.maxLevel());
                for (SkillNode n : skillNodes) {
                    String at = n.id() + " at level " + (pass == 0 ? 1 : n.maxLevel());
                    if (!SkillTreeApi.canCast(fp, route.id(), n.id())) { problems.add(at + ": the stand-in cannot cast it although the route is unlocked and the node bought"); continue; }
                    ManasSkillInstance inst = learn(skills, n);
                    if (inst == null) { problems.add(at + ": the skill could not be taught to the stand-in"); continue; }
                    refill(fp);
                    dummy.invulnerableTime = 0;
                    dummy.setHealth(2000.0f);
                    float before = dummy.getHealth();
                    try {
                        ((TreeSkill) inst.getSkill()).onRelease(inst, fp, 0, 0, 0);
                    } catch (Throwable t) {
                        problems.add(at + ": threw " + t);
                        continue;
                    }
                    List<String> swallowed = DragonSkill.takeFailures();
                    if (!swallowed.isEmpty()) problems.add(at + ": " + swallowed.get(0));
                    if (inst.onCoolDown(0)) casts++;
                    else refused++;
                    if (dummy.getHealth() < before) hurtCasts++;
                }
            }
            if (casts == 0) problems.add("no technique could be cast at all (" + refused + " refused)");
            boolean delayed = false;
            for (SkillNode n : skillNodes) delayed |= n.values().containsKey("windup");
            if (delayed && DragonTimers.pending() <= queued) problems.add("the route has techniques with an inhale or a wind-up, but none of them queued its follow-up on the timer");
            if (hurtCasts == 0) problems.add("no technique hurt the dummy standing 2.5 blocks in front of the stand-in");

            // 3. the passives (the route's own questions)
            dummy.invulnerableTime = 0;
            dummy.setHealth(2000.0f);
            spec.probe().run(fp, level, dummy, route, tree, problems);
        } catch (Throwable t) {
            problems.add("the test itself threw " + t);
        } finally {
            tree.clearRoute(route.id());
            tree.removeRoute(route.id());
            tree.setActiveClass("");
            for (SkillNode n : skillNodes) {
                ResourceLocation id = ResourceLocation.tryParse(n.skill());
                try { if (id != null && skills.getSkill(id).isPresent()) skills.forgetSkill(id); } catch (Throwable ignored) { /* the stand-in is thrown away anyway */ }
            }
            dummy.discard();
        }
        if (!problems.isEmpty()) return fail(problems);
        return SelfTests.Result.ok(skillNodes.size() + " techniques x 2 levels: " + casts + " cast, " + refused + " refused (no room or a cooldown), " + hurtCasts
                + " hurt the dummy; refused without the route with nothing spent; the passives answer; the numbers in the registry obey the budget");
    }

    /** A fresh instance of the node's skill in the stand-in's storage (any old one is forgotten first, so no cooldown carries over). */
    private static ManasSkillInstance learn(Skills skills, SkillNode n) {
        ResourceLocation id = ResourceLocation.tryParse(n.skill());
        if (id == null) return null;
        ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
        if (skill == null) return null;
        if (skills.getSkill(id).isPresent()) skills.forgetSkill(id);
        skills.learnSkill(skill.createDefaultInstance());
        return skills.getSkill(id).orElse(null);
    }

    private static void refill(FakePlayer fp) {
        IExistence ex = TensuraStorages.getExistenceFrom(fp);
        if (ex == null) return;
        ex.setMagicule(EnergyHelper.getMaxMagicule(fp));
        ex.setAura(EnergyHelper.getMaxAura(fp));
        ex.markDirty();
    }

    private static double magicule(FakePlayer fp) {
        IExistence ex = TensuraStorages.getExistenceFrom(fp);
        return ex == null ? 0.0 : ex.getMagicule();
    }
}
