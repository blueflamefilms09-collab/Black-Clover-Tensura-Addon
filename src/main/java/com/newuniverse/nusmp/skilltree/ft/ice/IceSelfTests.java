package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.kit.FtRig;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of this route can be played by its author, so it has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega selftest}, key
 * {@code tree.ft.ice_make}; the smoke script expects the prefix {@code tree}). It asks, in this order, the questions that decide whether the route works. (1) Is the data right: the route the server
 * LOADED (so a datapack that breaks a number is caught) obeys the shape and the balance law ({@link IceNumbers#problems}). (2) Is the wiring right: every active node names a registered ManasCore skill that
 * is a tree skill of this route and this node, and the passive the file lists is one the code implements. (3) Does the gate hold: a stand-in player WITHOUT the route cannot cast (the very {@code onRelease}
 * a key press calls refuses, starts no cooldown, spends no magicules, hurts nobody, builds nothing). (4) Does each technique do what it says, with the route granted, at level 1 and at the top level, next
 * to a dummy creature: the lance, the saucer, the eagle, the cannon, the hammer and the sword hurt the dummy (the hammer and the sword NOT before their wind-up is over: the tell), the floor chills it and
 * carries the caster, the shield deletes a foreign arrow and keeps the caster's own, the constructs stop at four and the fifth is refused without costing anything, and the passive cancels freezing and
 * chills a melee attacker only from level 2. Delayed effects are tested for real by playing the tree's clock forward. With an empty route registry (before the data is merged) the test passes and says so, so
 * the prefix is always reported; with the Skill Tree switched off in the config the gate refuses every cast by design, so the test still checks the data and the wiring (questions 1 and 2) and says that
 * the casts were skipped, the way the core's own tests do. The stand-in and everything it touched are put back afterwards ({@link FtRig}).
 */
public final class IceSelfTests {
    private IceSelfTests() {}

    public static final String KEY = "tree.ft.ice_make";

    private static final Set<String> PASSIVES = Set.of(IceRoute.PASSIVE_FROST_BODY);

    public static void register() { SelfTests.register(KEY, IceSelfTests::run); }

    private static SelfTests.Result run(MinecraftServer server) {
        if (RouteRegistry.all().isEmpty()) return SelfTests.Result.ok("skipped: no skill tree is loaded yet (the route registry is empty)");
        SkillRoute route = RouteRegistry.byId(IceRoute.ID);
        if (route == null) return SelfTests.Result.fail(IceRoute.ID + " is not among the " + RouteRegistry.all().size() + " loaded routes");

        List<String> bad = new ArrayList<>(IceNumbers.problems(route));
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
            if (!IceRoute.ID.equals(skill.routeId()) || !n.id().equals(skill.nodeId())) bad.add(n.skill() + " belongs to " + skill.routeId() + "/" + skill.nodeId() + ", not to " + IceRoute.ID + "/" + n.id());
            skills.put(n.id(), skill);
        }
        if (!bad.isEmpty()) return problems(bad);
        if (!SkillTreeApi.isEnabled()) return SelfTests.Result.ok("data and wiring of " + skills.size() + " techniques and 1 passive are right; casts skipped: the Skill Tree is switched off in the config (skilltree.enabled), so the gate refuses every cast by design");

        int refused = 0, worked = 0;
        try (FtRig rig = FtRig.open(server, "tree_ice_tester")) {
            if (rig == null) return SelfTests.Result.ok("data and wiring of " + skills.size() + " techniques and 1 passive are right; casts skipped: the spawn column is not loaded");
            refused = refusals(rig, route, skills, bad);
            for (boolean top : new boolean[] {false, true}) {
                rig.grant(route, top);
                worked += casts(rig, route, skills, top, bad);
                rig.revoke(route);
            }
            IceConstructs.forget(rig.player);
        }
        if (!bad.isEmpty()) return problems(bad);
        return SelfTests.Result.ok(route.nodes().size() + " nodes obey the budget; " + refused + " casts without the route changed nothing; " + worked + " checks of the techniques and the passive held at level 1 and at the top");
    }

    private static SelfTests.Result problems(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    private static boolean sameNumber(double a, double b) { return Double.isNaN(a) ? Double.isNaN(b) : Math.abs(a - b) < 1e-6; }

    private static int level(SkillRoute route, String node, boolean top) { return top ? route.node(node).maxLevel() : 1; }

    // ================================================================ the gate
    private static int refusals(FtRig rig, SkillRoute route, Map<String, FtTreeSkill> skills, List<String> bad) {
        rig.revoke(route);
        IceConstructs.forget(rig.player);
        Zombie dummy = rig.dummy(0, 5.0);
        int n = 0;
        for (Map.Entry<String, FtTreeSkill> e : skills.entrySet()) {
            String id = e.getKey();
            rig.reset();
            if (SkillTreeApi.canCast(rig.player, IceRoute.ID, id)) {
                bad.add(id + ": canCast is true for a player who does not own the route");
                continue;
            }
            ManasSkillInstance instance = e.getValue().createDefaultInstance();
            double before = rig.pool();
            e.getValue().onRelease(instance, rig.player, 0, 0, 0);
            rig.flush(60);
            if (instance.onCoolDown(0)) bad.add(id + ": a refused cast still started the cooldown");
            if (dummy.getHealth() < dummy.getMaxHealth() || dummy.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) bad.add(id + ": a refused cast touched the dummy");
            if (!sameNumber(before, rig.pool())) bad.add(id + ": a refused cast spent magicules");
            if (IceConstructs.standing(rig.player) != 0) bad.add(id + ": a refused cast built a construct");
            n++;
        }
        rig.player.setTicksFrozen(0);
        if (IcePassives.cancelsFreeze(rig.player, rig.level.damageSources().freeze())) bad.add("Ur's Lesson cancelled freezing damage for a player who does not own it");
        dummy.discard();
        rig.flush(60);
        return n;
    }

    // ================================================================ the techniques
    private static int casts(FtRig rig, SkillRoute route, Map<String, FtTreeSkill> skills, boolean top, List<String> bad) {
        String at = top ? " at the top level" : " at level 1";
        ServerPlayer p = rig.player;
        int checks = 0;
        IceConstructs.forget(p);

        for (Map.Entry<String, FtTreeSkill> e : skills.entrySet()) {
            if (!SkillTreeApi.canCast(p, IceRoute.ID, e.getKey())) bad.add(e.getKey() + ": canCast is false although the route and the node are owned" + at);
        }

        // the lance and the saucer, and the eagle with a foe, hurt a dummy standing in front
        checks += hurts(rig, skills.get(IceRoute.LANCE), route, 6.0, 40, top, bad, "the lance" + at);
        checks += hurts(rig, skills.get(IceRoute.SAUCER), route, 5.0, 80, top, bad, "the saucer" + at);
        checks += hurts(rig, skills.get(IceRoute.EAGLE), route, 6.0, 100, top, bad, "the eagles" + at);
        {
            FtTreeSkill eagle = skills.get(IceRoute.EAGLE);
            rig.reset();
            double before = rig.pool();
            if (eagle.castDirect(p, eagle.createDefaultInstance(), level(route, IceRoute.EAGLE, top))) bad.add("the eagles flew with no foe in range" + at);
            else if (!sameNumber(before, rig.pool())) bad.add("the refused eagles spent magicules" + at);
            checks++;
        }

        // the cannon is a turret: it fires by itself at a foe it can see, and it is one of the four constructs
        {
            FtTreeSkill cannon = skills.get(IceRoute.CANNON);
            int lv = level(route, IceRoute.CANNON, top);
            rig.reset();
            IceConstructs.forget(p);
            Zombie dummy = rig.dummy(0, route.node(IceRoute.CANNON).value("reach", lv, 4) + 4.0);
            if (!cannon.castDirect(p, cannon.createDefaultInstance(), lv)) bad.add("the cannon refused to cast" + at);
            else {
                if (IceConstructs.standing(p) != 1) bad.add("the cannon did not take a construct slot" + at);
                rig.flush(120);
                if (dummy.getHealth() >= dummy.getMaxHealth()) bad.add("the cannon never fired at a foe four blocks from it" + at);
            }
            dummy.discard();
            IceConstructs.forget(p);
            checks++;
        }

        // the hammer and the sword are announced: nothing lands before the wind-up is over, then the dummy is hurt
        checks += announced(rig, skills.get(IceRoute.HAMMER), route, 8.0, top, bad, "the hammer" + at);
        checks += announced(rig, skills.get(IceRoute.EXCALIBUR), route, 5.0, top, bad, "Cold Excalibur" + at);
        {
            FtTreeSkill hammer = skills.get(IceRoute.HAMMER);
            rig.reset();
            double before = rig.pool();
            if (hammer.castDirect(p, hammer.createDefaultInstance(), level(route, IceRoute.HAMMER, top))) bad.add("the hammer fell with no foe in sight" + at);
            else if (!sameNumber(before, rig.pool())) bad.add("the refused hammer spent magicules" + at);
            checks++;
        }

        // the floor chills a foe standing on it and carries the caster who stands on it; it takes a slot
        {
            FtTreeSkill floor = skills.get(IceRoute.FLOOR);
            int lv = level(route, IceRoute.FLOOR, top);
            double reach = route.node(IceRoute.FLOOR).value("reach", lv, 8);
            rig.reset();
            IceConstructs.forget(p);
            Vec3 home = p.position(), c = FtWorld.groundAt(p, FtWorld.rayEnd(p, reach));
            Zombie dummy = rig.dummy(c.x - p.getX(), c.z - p.getZ());
            dummy.moveTo(c.x, c.y, c.z);
            if (!floor.castDirect(p, floor.createDefaultInstance(), lv)) bad.add("the floor refused to cast" + at);
            else {
                p.moveTo(c.x, c.y, c.z);
                rig.flush(60);
                if (!dummy.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) bad.add("the floor did not slow a foe standing on it" + at);
                if (dummy.getTicksFrozen() <= 0) bad.add("the floor did not chill a foe standing on it" + at);
                if (!p.hasEffect(MobEffects.MOVEMENT_SPEED)) bad.add("the floor did not carry the caster standing on it" + at);
                if (IceConstructs.standing(p) != 1) bad.add("the floor did not take a construct slot" + at);
            }
            p.moveTo(home.x, home.y, home.z);
            dummy.discard();
            IceConstructs.forget(p);
            checks++;
        }

        // the shield deletes a foreign arrow in the wall and lets the caster's own pass; it gives absorption and takes a slot
        {
            FtTreeSkill shield = skills.get(IceRoute.SHIELD);
            int lv = level(route, IceRoute.SHIELD, top);
            rig.reset();
            IceConstructs.forget(p);
            Zombie archer = rig.dummy(6.0, 12.0);
            Vec3 middle = p.position().add(FtWorld.flatLook(p).scale(IceShieldSkill.AHEAD));
            Arrow foreign = arrow(rig, archer, middle), own = arrow(rig, p, middle.add(0.5, 0, 0));
            if (!shield.castDirect(p, shield.createDefaultInstance(), lv)) bad.add("the shield refused to cast" + at);
            else {
                rig.flush(10);
                if (!foreign.isRemoved()) bad.add("the shield let a foreign arrow through" + at);
                if (own.isRemoved()) bad.add("the shield deleted the caster's own arrow" + at);
                if (!p.hasEffect(MobEffects.ABSORPTION)) bad.add("the shield gave no absorption" + at);
                if (IceConstructs.standing(p) != 1) bad.add("the shield did not take a construct slot" + at);
            }
            archer.discard();
            IceConstructs.forget(p);
            checks++;
        }

        // never more than four constructs: the fifth floor is refused and costs nothing
        {
            FtTreeSkill floor = skills.get(IceRoute.FLOOR);
            int lv = level(route, IceRoute.FLOOR, top);
            rig.reset();
            IceConstructs.forget(p);
            for (int i = 0; i < 4; i++) if (!floor.castDirect(p, floor.createDefaultInstance(), lv)) bad.add("floor number " + (i + 1) + " was refused although fewer than four stand" + at);
            double before = rig.pool();
            if (floor.castDirect(p, floor.createDefaultInstance(), lv)) bad.add("a fifth construct was built" + at);
            else if (!sameNumber(before, rig.pool())) bad.add("the refused fifth construct spent magicules" + at);
            IceConstructs.forget(p);
            rig.flush(400);
            checks++;
        }

        // the passive: freezing damage is cancelled; a melee attacker is chilled from level 2, once per rest
        {
            int lv = level(route, IceRoute.FROST_BODY, top);
            Zombie attacker = rig.dummy(0, 2.0);
            if (!IcePassives.cancelsFreeze(p, rig.level.damageSources().freeze())) bad.add("Ur's Lesson did not cancel freezing damage" + at);
            if (IcePassives.cancelsFreeze(p, rig.level.damageSources().generic())) bad.add("Ur's Lesson cancelled damage that is not freezing" + at);
            rig.player.getPersistentData().remove(IcePassives.REST_UNTIL);
            boolean chilled = IcePassives.thorns(p, attacker);
            if (chilled != lv >= 2) bad.add("Ur's Lesson " + (chilled ? "chilled" : "did not chill") + " a melee attacker" + at);
            if (chilled && !attacker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) bad.add("Ur's Lesson reported a chill but the attacker is not slowed" + at);
            if (chilled && IcePassives.thorns(p, attacker)) bad.add("Ur's Lesson chilled twice with no rest between" + at);
            attacker.discard();
            checks += 2;
        }
        rig.reset();
        return checks;
    }

    private static Arrow arrow(FtRig rig, Entity owner, Vec3 at) {
        Arrow a = EntityType.ARROW.create(rig.level);
        if (a == null) throw new IllegalStateException("an Arrow cannot be created");
        a.setOwner(owner);
        a.moveTo(at.x, at.y + 1.0, at.z, 0f, 0f);
        a.setDeltaMovement(0, 0, -1.0);
        rig.level.addFreshEntity(a);
        return rig.track(a);
    }

    /** Casts an offensive technique at a dummy standing {@code ahead} blocks in front and reports it if the dummy is not hurt after the clock has run. */
    private static int hurts(FtRig rig, FtTreeSkill skill, SkillRoute route, double ahead, int ticks, boolean top, List<String> bad, String what) {
        rig.reset();
        Zombie dummy = rig.dummy(0, ahead);
        if (!skill.castDirect(rig.player, skill.createDefaultInstance(), level(route, skill.nodeId(), top))) bad.add(what + " refused to cast");
        else {
            rig.flush(ticks);
            if (dummy.getHealth() >= dummy.getMaxHealth()) bad.add(what + " did not hurt a foe " + ahead + " blocks ahead");
        }
        dummy.discard();
        return 1;
    }

    /** Casts a technique that is announced: the dummy must be unhurt right after the cast (the tell) and hurt once the clock has run. */
    private static int announced(FtRig rig, FtTreeSkill skill, SkillRoute route, double ahead, boolean top, List<String> bad, String what) {
        rig.reset();
        Zombie dummy = rig.dummy(0, ahead);
        if (!skill.castDirect(rig.player, skill.createDefaultInstance(), level(route, skill.nodeId(), top))) bad.add(what + " refused to cast");
        else {
            if (dummy.getHealth() < dummy.getMaxHealth()) bad.add(what + " hurt the dummy at once, with no tell");
            rig.flush(120);
            if (dummy.getHealth() >= dummy.getMaxHealth()) bad.add(what + " never hurt a foe " + ahead + " blocks ahead");
        }
        dummy.discard();
        return 1;
    }
}
