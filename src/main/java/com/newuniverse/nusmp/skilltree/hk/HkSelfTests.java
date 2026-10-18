package com.newuniverse.nusmp.skilltree.hk;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtRig;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the Heavy Knight routes can be played by its author, so the CI server smoke test runs this in the real game ({@code /multiverse mega selftest}, key
 * {@code tree.hk.heavy_knight}; the smoke script expects the prefix {@code tree}). It asks the two questions that decide whether the family works. (1) Is the data wired: the three route files are loaded,
 * carry the franchise tag {@code exiled_heavy_knight}, every skill a node names is a registered tree skill of that very route and node, and every passive a node lists is one the code implements. (2) Is the
 * single-active-route rule real: with Lady Luck owned and equipped the Luck modifier is on the player, the moment another route's class is equipped it is gone and every Lady Luck node answers 0, and a
 * Riposte of the Swordsmanship raises a blow only while that route is the equipped one. The player is a stand-in (see {@code FtRig}) and everything is removed afterwards.
 */
public final class HkSelfTests {
    private HkSelfTests() {}

    public static final String KEY = "tree.hk.heavy_knight";

    public static void register() { SelfTests.register(KEY, HkSelfTests::run); }

    private static SelfTests.Result run(MinecraftServer server) {
        if (RouteRegistry.all().isEmpty()) return SelfTests.Result.ok("skipped: no skill tree is loaded yet (the route registry is empty)");
        List<String> bad = new ArrayList<>();
        int nodes = 0;
        for (String id : new String[] {HkRoute.OATH, HkRoute.SWORD, HkRoute.LUCK}) {
            SkillRoute route = RouteRegistry.byId(id);
            if (route == null) {
                bad.add(id + " is not among the loaded routes");
                continue;
            }
            if (!HkRoute.FRANCHISE.equals(route.magic())) bad.add(id + " has the franchise '" + route.magic() + "', not " + HkRoute.FRANCHISE);
            for (SkillNode n : route.nodes()) {
                nodes++;
                if (!n.grantsSkill()) {
                    for (String passive : n.passives()) if (!HkRoute.PASSIVES.contains(passive)) bad.add(n.id() + " lists the passive '" + passive + "', which no code implements");
                    continue;
                }
                ResourceLocation rl = ResourceLocation.tryParse(n.skill());
                Object found = null;
                try {
                    found = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
                } catch (Throwable noRegistry) { /* reported below */ }
                if (!(found instanceof TreeSkill ts)) bad.add(n.id() + ": " + n.skill() + " is not a registered tree skill");
                else if (!id.equals(ts.routeId()) || !n.id().equals(ts.nodeId())) bad.add(n.skill() + " belongs to " + ts.routeId() + "/" + ts.nodeId() + ", not to " + id + "/" + n.id());
            }
        }
        if (!bad.isEmpty()) return fail(bad);
        if (!SkillTreeApi.isEnabled()) return SelfTests.Result.ok("data and wiring of " + nodes + " nodes are right; the gate checks were skipped: the Skill Tree is switched off in the config");

        try (FtRig rig = FtRig.open(server, "tree_hk_tester")) {
            if (rig == null) return SelfTests.Result.ok("data and wiring of " + nodes + " nodes are right; the gate checks were skipped: the spawn column is not loaded");
            SkillRoute luck = RouteRegistry.byId(HkRoute.LUCK), sword = RouteRegistry.byId(HkRoute.SWORD);
            rig.grant(luck, true);
            // the sword route is owned too, but not equipped (grant sets the class, so equip Lady Luck again afterwards)
            var tree = SkillTreeApi.of(rig.player);
            tree.addRoute(sword.id());
            for (SkillNode n : sword.nodes()) tree.setNodeLevel(sword.id(), n.id(), n.maxLevel());
            tree.setActiveClass(luck.classId());
            AttributeInstance lk = rig.player.getAttribute(Attributes.LUCK);
            HkPassives.sync(rig.player);
            if (lk == null || !lk.hasModifier(HkPassives.modifierIds().get(3))) bad.add("Lady Luck equipped: the Luck modifier is not on the player");
            if (HkPassives.level(rig.player, HkRoute.LUCK, HkRoute.FORTUNES_FAVOR) < 1) bad.add("Lady Luck equipped: Fortune's Favor answers 0");
            if (HkPassives.level(rig.player, HkRoute.SWORD, HkRoute.RIPOSTE) != 0) bad.add("the Swordsmanship is owned but not equipped, yet the Riposte answers " + HkPassives.level(rig.player, HkRoute.SWORD, HkRoute.RIPOSTE));
            rig.player.getPersistentData().putLong(HkPassives.HURT_AT, rig.level.getGameTime());
            float unequipped = HkPassives.dealt(rig.player, 10f);
            if (unequipped > 15.01f) bad.add("a blow was raised to " + unequipped + " by a route that is not equipped (only a Lucky Strike crit, at most 15, may)");

            tree.setActiveClass(sword.classId());
            HkPassives.sync(rig.player);
            if (lk != null && lk.hasModifier(HkPassives.modifierIds().get(3))) bad.add("Lady Luck unequipped: the Luck modifier is still on the player");
            if (HkPassives.level(rig.player, HkRoute.LUCK, HkRoute.CATS_PAW) != 0) bad.add("Lady Luck unequipped: the Cat's Paw still answers " + HkPassives.level(rig.player, HkRoute.CATS_PAW, HkRoute.CATS_PAW));
            rig.player.getPersistentData().putLong(HkPassives.HURT_AT, rig.level.getGameTime());
            float equipped = HkPassives.dealt(rig.player, 10f);
            if (equipped < 10.5f) bad.add("the Swordsmanship equipped: a Riposte after a blow left the damage at " + equipped);

            rig.player.getPersistentData().remove(HkPassives.HURT_AT);
            tree.setActiveClass(luck.classId());
            HkPassives.sync(rig.player);
            rig.revoke(luck);
            rig.revoke(sword);
            HkPassives.sync(rig.player);
            if (lk != null && lk.hasModifier(HkPassives.modifierIds().get(3))) bad.add("after the routes were removed the Luck modifier is still on the player");
        }
        return bad.isEmpty() ? SelfTests.Result.ok(nodes + " nodes wired; the Luck modifier and the Riposte obey the equipped route") : fail(bad);
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }
}
