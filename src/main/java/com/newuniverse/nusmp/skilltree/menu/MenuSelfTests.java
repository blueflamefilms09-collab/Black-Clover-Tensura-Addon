package com.newuniverse.nusmp.skilltree.menu;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the unit tests prove the pure rules of the Tensura-menu integration; what only the real game can prove is the REGISTRATION half: that the ManasCore skill registry really holds one stand-in per
 * line, under the ids the rest of the code computes, that each resolves to a {@link LineStandInSkill} of Tensura's UNIQUE type, that every route of the live route registry (the 275 generated lines and the 8 classic routes) has one and every stand-in names a
 * line that exists, and that a stand-in is inert (cannot be used or slotted). One self-test, key {@code tree.tensura_menu} (the {@code tree} prefix is what the CI server smoke test expects), so the smoke test
 * of the build fails if a line was added without its stand-in, if the table in the jar and the route files drifted apart, or if the registry refused an id. It changes nothing in the world.
 */
final class MenuSelfTests {
    private MenuSelfTests() {}

    static void register() { SelfTests.register("tree.tensura_menu", MenuSelfTests::standIns); }

    private static SelfTests.Result standIns(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int n = LineStandIns.count();
        if (n == 0) return SelfTests.Result.fail("no stand-in skills are registered (is assets/nusmp/skill_books.json in the jar?)");
        if (n != LineIndex.load().size()) bad.add("the table has " + LineIndex.load().size() + " lines but " + n + " stand-ins are registered");

        Set<ResourceLocation> seen = new HashSet<>();
        for (var holder : LineStandIns.holders()) {
            ResourceLocation id = holder.getId();
            if (!seen.add(id)) {
                bad.add("duplicate id " + id);
                continue;
            }
            ManasSkill resolved = SkillAPI.getSkillRegistry().get(id);
            if (resolved == null) {
                bad.add(id + " is not in the skill registry");
                continue;
            }
            if (!(resolved instanceof LineStandInSkill s)) {
                bad.add(id + " resolves to " + resolved.getClass().getSimpleName() + ", not a stand-in");
                continue;
            }
            if (resolved != holder.get()) bad.add(id + " resolves to another instance than its holder");
            if (s.getType() != Skill.SkillType.UNIQUE) bad.add(id + " is " + s.getType() + ", not UNIQUE");
            if (!LineStandIns.skillId(s.routeId()).equals(id)) bad.add(id + " does not match its route id " + s.routeId());
            if (s.getName().getString().isEmpty()) bad.add(id + " has an empty name");
            ManasSkillInstance inst = s.createDefaultInstance();
            if (inst == null) bad.add(id + " cannot create an instance");
            else if (s.canInteractSkill(inst, null) || s.canActivateSkill(inst, null, 0) || s.canBeSlotted(inst, null, 0)) bad.add(id + " is not inert");
        }

        Set<String> lineRoutes = new HashSet<>();
        for (SkillRoute r : RouteRegistry.all()) lineRoutes.add(r.id());                       // the 275 generated lines AND the 8 classic routes: every route is a line in the menu
        if (lineRoutes.isEmpty()) bad.add("the route registry holds no route (the datapack is not loaded?)");
        for (String id : lineRoutes) if (!LineStandIns.routeIds().contains(id)) bad.add("the route " + id + " has no stand-in skill");
        for (String id : LineStandIns.routeIds()) if (!lineRoutes.isEmpty() && !lineRoutes.contains(id)) bad.add("the stand-in of " + id + " names no line of the registry");

        StandInPlan.Plan plan = StandInPlan.diff(List.of("nusmp:b"), List.of("nusmp:a"), Set.of("nusmp:a", "nusmp:b"));
        if (!plan.learn().equals(List.of("nusmp:a")) || !plan.forget().equals(List.of("nusmp:b"))) bad.add("the stand-in plan is wrong: " + plan);

        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        return SelfTests.Result.ok(n + " stand-in skills, one per line, all UNIQUE and inert");
    }
}
