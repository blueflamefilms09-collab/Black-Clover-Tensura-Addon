package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.kit.FtRig;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of this route can be played by its author, so it has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega selftest}, key
 * {@code tree.ft.sky_dragon_slayer}; the smoke script expects the prefix {@code tree}). It asks, in this order, the questions that decide whether the route works. (1) Is the data right: the
 * route the server LOADED (not the file in the repository, so a datapack that breaks a number is caught) obeys the shape and the balance law ({@link SkyNumbers#problems}). (2) Is the wiring right:
 * every active node names a registered ManasCore skill that is a tree skill of this route and this node, and every passive a node lists is one the code implements. (3) Does the gate hold: a
 * stand-in player WITHOUT the route cannot cast (the very {@code onRelease} a key press calls refuses, starts no cooldown, spends no magicules, hurts nobody). (4) Does each technique do what it says,
 * with the route granted, at level 1 and at the top level, next to a dummy creature: the roar, the wing attack, the wave wind and the drill hurt the dummy (and the drill does not hurt it before its
 * wind-up is over), the breath and the drill mend the caster, Troia strips the effects in order and refuses when there is nothing to strip, the enchant gives strength and speed, the updraft shrinks a
 * fall and the aura covers the caster. Delayed effects are tested for real by playing the clock forward. With an empty route registry (before the data is merged) the test passes and says so,
 * so the prefix is always reported; with the Skill Tree switched off in the config the gate refuses every cast by design, so the test still checks the data and the wiring (questions 1 and 2)
 * and says that the casts were skipped, the way the core's own tests do. The stand-in and everything it touched are put back afterwards ({@link FtRig}).
 */
public final class SkySelfTests {
    private SkySelfTests() {}

    public static final String KEY = "tree.ft.sky_dragon_slayer";

    private static final Set<String> PASSIVES = Set.of(SkyRoute.PASSIVE_CUSHION, SkyRoute.PASSIVE_AURA);

    public static void register() { SelfTests.register(KEY, SkySelfTests::run); }

    private static SelfTests.Result run(MinecraftServer server) {
        if (RouteRegistry.all().isEmpty()) return SelfTests.Result.ok("skipped: no skill tree is loaded yet (the route registry is empty)");
        SkillRoute route = RouteRegistry.byId(SkyRoute.ID);
        if (route == null) return SelfTests.Result.fail(SkyRoute.ID + " is not among the " + RouteRegistry.all().size() + " loaded routes");

        List<String> bad = new ArrayList<>(SkyNumbers.problems(route));
        Map<String, FtTreeSkill> skills = new LinkedHashMap<>();
        for (SkillNode n : route.nodes()) {
            if (!n.grantsSkill()) {
                for (String passive : n.passives()) if (!PASSIVES.contains(passive)) bad.add(n.id() + " lists the passive '" + passive + "', which no code implements");
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(n.skill());
            Object found = null;
            try {
                found = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            } catch (Throwable noRegistry) { /* reported as not registered below */ }
            if (!(found instanceof FtTreeSkill skill)) {
                bad.add(n.id() + ": " + n.skill() + " is not a registered tree skill");
                continue;
            }
            if (!SkyRoute.ID.equals(skill.routeId()) || !n.id().equals(skill.nodeId())) bad.add(n.skill() + " belongs to " + skill.routeId() + "/" + skill.nodeId() + ", not to " + SkyRoute.ID + "/" + n.id());
            skills.put(n.id(), skill);
        }
        if (!bad.isEmpty()) return problems(bad);
        if (!SkillTreeApi.isEnabled()) return SelfTests.Result.ok("data and wiring of " + skills.size() + " techniques and 2 passives are right; casts skipped: the Skill Tree is switched off in the config (skilltree.enabled), so the gate refuses every cast by design");

        int refused = 0, worked = 0;
        try (FtRig rig = FtRig.open(server, "tree_sky_tester")) {
            if (rig == null) return SelfTests.Result.ok("data and wiring of " + skills.size() + " techniques and 2 passives are right; casts skipped: the spawn column is not loaded");
            refused = refusals(rig, route, skills, bad);
            for (boolean top : new boolean[] {false, true}) {
                rig.grant(route, top);
                worked += casts(rig, route, skills, top, bad);
                rig.revoke(route);
            }
        }
        if (!bad.isEmpty()) return problems(bad);
        return SelfTests.Result.ok(route.nodes().size() + " nodes obey the budget; " + refused + " casts without the route changed nothing; " + worked + " checks of the techniques and passives held at level 1 and at the top");
    }

    private static SelfTests.Result problems(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    // ================================================================ the gate
    private static int refusals(FtRig rig, SkillRoute route, Map<String, FtTreeSkill> skills, List<String> bad) {
        rig.revoke(route);
        Zombie dummy = rig.dummy(0, 2.5);
        int n = 0;
        for (Map.Entry<String, FtTreeSkill> e : skills.entrySet()) {
            String id = e.getKey();
            rig.reset();
            if (SkillTreeApi.canCast(rig.player, SkyRoute.ID, id)) {
                bad.add(id + ": canCast is true for a player who does not own the route");
                continue;
            }
            ManasSkillInstance instance = e.getValue().createDefaultInstance();
            double before = rig.pool();
            e.getValue().onRelease(instance, rig.player, 0, 0, 0);
            if (instance.onCoolDown(0)) bad.add(id + ": a refused cast still started the cooldown");
            if (dummy.getHealth() < dummy.getMaxHealth()) bad.add(id + ": a refused cast hurt the dummy");
            if (!sameNumber(before, rig.pool())) bad.add(id + ": a refused cast spent magicules");
            if (rig.player.hasEffect(MobEffects.DAMAGE_BOOST)) bad.add(id + ": a refused cast buffed the caster");
            n++;
        }
        dummy.discard();
        rig.flush(60);
        return n;
    }

    private static boolean sameNumber(double a, double b) { return Double.isNaN(a) ? Double.isNaN(b) : Math.abs(a - b) < 1e-6; }

    // ================================================================ the techniques
    private static int casts(FtRig rig, SkillRoute route, Map<String, FtTreeSkill> skills, boolean top, List<String> bad) {
        String at = top ? " at the top level" : " at level 1";
        ServerPlayer p = rig.player;
        int checks = 0;

        for (Map.Entry<String, FtTreeSkill> e : skills.entrySet()) {
            String id = e.getKey();
            if (!SkillTreeApi.canCast(p, SkyRoute.ID, id)) bad.add(id + ": canCast is false although the route and the node are owned" + at);
        }

        // the three offensive techniques hurt a dummy that stands in their shape
        checks += hurts(rig, skills.get(SkyRoute.ROAR), route, 3.0, 40, top, bad, "the roar" + at);
        checks += hurts(rig, skills.get(SkyRoute.WING_ATTACK), route, 2.0, 40, top, bad, "the wing attack" + at);
        checks += hurts(rig, skills.get(SkyRoute.WAVE_WIND), route, 4.0, 120, top, bad, "the wave wind" + at);

        // the breath mends a wounded caster, and only by the share the law allows; with nobody wounded it refuses and spends nothing
        {
            FtTreeSkill breath = skills.get(SkyRoute.BREATH);
            int lv = level(route, SkyRoute.BREATH, top);
            rig.reset();
            float half = p.getMaxHealth() / 2f;
            p.setHealth(half);
            if (!breath.castDirect(p, breath.createDefaultInstance(), lv)) bad.add("the breath refused a wounded caster" + at);
            else if (p.getHealth() <= half) bad.add("the breath healed nothing" + at);
            else if (p.getHealth() > half + p.getMaxHealth() * 0.25f + 1e-3f) bad.add("the breath healed more than 25 percent of maximum health" + at + " (" + (p.getHealth() - half) + ")");
            rig.reset();
            double before = rig.pool();
            if (breath.castDirect(p, breath.createDefaultInstance(), lv)) bad.add("the breath cast with nobody wounded" + at);
            else if (!sameNumber(before, rig.pool())) bad.add("the refused breath spent magicules" + at);
            checks += 2;
        }

        // Troia strips the worst effects first and refuses when there is nothing to strip
        {
            FtTreeSkill troia = skills.get(SkyRoute.TROIA);
            int lv = level(route, SkyRoute.TROIA, top);
            int strips = (int) route.node(SkyRoute.TROIA).value("strips", lv, 1);
            rig.reset();
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600));
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            if (!troia.castDirect(p, troia.createDefaultInstance(), lv)) bad.add("Troia refused a caster who is sick" + at);
            else {
                // the worst first: nausea, then poison, then weakness; exactly as many as the node level strips
                if (p.hasEffect(MobEffects.CONFUSION)) bad.add("Troia left the nausea on" + at);
                if (p.hasEffect(MobEffects.POISON) == (strips >= 2)) bad.add("Troia " + (strips >= 2 ? "left the poison on although it strips " : "stripped the poison although it strips only ") + strips + at);
                if (p.hasEffect(MobEffects.WEAKNESS) == (strips >= 3)) bad.add("Troia " + (strips >= 3 ? "left the weakness on although it strips " : "stripped the weakness although it strips only ") + strips + at);
            }
            rig.reset();
            if (troia.castDirect(p, troia.createDefaultInstance(), lv)) bad.add("Troia cast with nothing to cleanse" + at);
            checks += 2;
        }

        // the enchant gives strength and speed, the speed one step higher when the table says so
        {
            FtTreeSkill enchant = skills.get(SkyRoute.ENCHANT);
            int lv = level(route, SkyRoute.ENCHANT, top);
            int amp = (int) route.node(SkyRoute.ENCHANT).value("speed_amp", lv, 0);
            rig.reset();
            if (!enchant.castDirect(p, enchant.createDefaultInstance(), lv)) bad.add("the enchant refused to cast" + at);
            else {
                if (!p.hasEffect(MobEffects.DAMAGE_BOOST)) bad.add("the enchant gave no strength" + at);
                MobEffectInstance speed = p.getEffect(MobEffects.MOVEMENT_SPEED);
                if (speed == null) bad.add("the enchant gave no speed" + at);
                else if (speed.getAmplifier() != amp) bad.add("the enchant gave speed " + speed.getAmplifier() + " but the table says " + amp + at);
            }
            checks++;
        }

        // the Secret Art: it is announced (nothing lands before the wind-up ends), then it hurts the dummy and mends the caster
        {
            FtTreeSkill drill = skills.get(SkyRoute.SKY_DRILL);
            int lv = level(route, SkyRoute.SKY_DRILL, top);
            rig.reset();
            Zombie dummy = rig.dummy(0, 2.0);
            float half = p.getMaxHealth() / 2f;
            p.setHealth(half);
            if (!drill.castDirect(p, drill.createDefaultInstance(), lv)) bad.add("the Sky Drill refused to cast" + at);
            else {
                if (dummy.getHealth() < dummy.getMaxHealth()) bad.add("the Sky Drill hurt the dummy at once, with no tell" + at);
                rig.flush(120);
                if (dummy.getHealth() >= dummy.getMaxHealth()) bad.add("the Sky Drill never hurt the dummy standing inside its circle" + at);
                if (p.getHealth() <= half) bad.add("the Sky Drill did not mend the caster inside its circle" + at);
            }
            dummy.discard();
            checks++;
        }

        // the passives: the updraft shrinks a fall by the table's share, the aura covers the caster
        {
            int lv = level(route, SkyRoute.CUSHION, top);
            double reduction = route.node(SkyRoute.CUSHION).value("fall_reduction", lv, 0);
            LivingFallEvent fall = new LivingFallEvent(p, 12f, 1f);
            float factor = SkyPassives.cushion(p, fall);
            if (Math.abs(factor - (1f - reduction)) > 1e-4 || Math.abs(fall.getDamageMultiplier() - factor) > 1e-4) bad.add("the updraft changed a fall by " + factor + " (multiplier " + fall.getDamageMultiplier() + "), the table says " + (1f - reduction) + at);
            rig.reset();
            if (SkyPassives.aura(p) < 1 || !p.hasEffect(MobEffects.DAMAGE_RESISTANCE)) bad.add("the armor aura did not cover its owner" + at);
            checks += 2;
        }
        rig.reset();
        return checks;
    }

    private static int level(SkillRoute route, String node, boolean top) { return top ? route.node(node).maxLevel() : 1; }

    /** Casts an offensive technique at a dummy standing {@code ahead} blocks in front and reports it if the dummy is not hurt after the clock has run. */
    private static int hurts(FtRig rig, FtTreeSkill skill, SkillRoute route, double ahead, int ticks, boolean top, List<String> bad, String what) {
        rig.reset();
        Zombie dummy = rig.dummy(0, ahead);
        int lv = level(route, skill.nodeId(), top);
        if (!skill.castDirect(rig.player, skill.createDefaultInstance(), lv)) bad.add(what + " refused to cast");
        else {
            rig.flush(ticks);
            if (dummy.getHealth() >= dummy.getMaxHealth()) bad.add(what + " did not hurt a foe " + ahead + " blocks ahead");
        }
        dummy.discard();
        return 1;
    }
}
