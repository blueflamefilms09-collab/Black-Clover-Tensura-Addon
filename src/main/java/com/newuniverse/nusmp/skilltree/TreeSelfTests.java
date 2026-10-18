package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import com.newuniverse.nusmp.skilltree.data.RouteCodecs;
import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.RouteWire;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.FireDragonSkills;
import com.newuniverse.nusmp.skilltree.ft.IceMakeSkills;
import com.newuniverse.nusmp.skilltree.ft.LightningDragonSkills;
import com.newuniverse.nusmp.skilltree.ft.SkyDragonSkills;
import com.newuniverse.nusmp.skilltree.net.RoutesPayload;
import com.newuniverse.nusmp.skilltree.net.SetClassPayload;
import com.newuniverse.nusmp.skilltree.net.TreeSyncPayload;
import com.newuniverse.nusmp.skilltree.net.UnlockNodePayload;
import com.newuniverse.nusmp.skilltree.rules.TreeRules;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.PlayerSkillTree;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing of the Skill Tree can be played by its author, so the core registers three SELF-TESTS that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}, keys {@code tree.core.*}; the smoke script expects the prefix {@code tree}). They check the three things the unit tests cannot: that the routes the mod
 * SHIPS load clean and agree with the skills registered in the real ManasCore registry ({@code tree.core.routes}); that the rules work on a real player object with a real attachment, the real
 * route registry, the real sync queue and the real skill storage ({@code tree.core.rules}, with a stand-in player and two synthetic routes put in the registry for the length of the test and taken
 * out again); and that the packets and the saved form survive the real {@code FriendlyByteBuf} and NBT ({@code tree.core.sync}). The routes are written by other builders, so every test passes
 * on an EMPTY registry and says so in its line; a route that is installed but broken, or a tree skill with no route to hold it, fails. A test that changes the stand-in player's tree puts it back,
 * and the registry is restored in a {@code finally}.
 */
final class TreeSelfTests {
    private static final String PLAYER = "tree_tester";

    private TreeSelfTests() {}

    static void register() {
        SelfTests.register("tree.core.routes", TreeSelfTests::routes);
        SelfTests.register("tree.core.rules", TreeSelfTests::rules);
        SelfTests.register("tree.core.sync", TreeSelfTests::sync);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result problems(List<String> p) {
        return fail(p.size() + " problem(s): " + String.join(" | ", p.subList(0, Math.min(6, p.size()))));
    }

    /** Two small synthetic routes used by the rules and sync tests; built through the real schema so a schema fault shows up here too. */
    private static List<SkillRoute> synthetic(List<String> bad) {
        String one = """
                { "name": "Selftest One", "book": "nusmp:selftest_one_book", "class_id": "selftest_one", "color": "#336699",
                  "nodes": [ { "id": "a", "cost": 1, "max_level": 3, "x": 0, "y": 1, "values": { "damage": [1, 2, 3] } },
                             { "id": "b", "parents": [ { "node": "a", "level": 2 } ], "costs": [2, 3], "max_level": 2, "x": 1, "y": 0 },
                             { "id": "c", "parents": ["a"], "cost": 1, "max_level": 1, "x": 1, "y": 2 },
                             { "id": "d", "parents": ["b", "c"], "cost": 4, "max_level": 1, "x": 2, "y": 1 } ] }""";
        String two = """
                { "name": "Selftest Two", "book": "nusmp:selftest_two_book", "class_id": "selftest_two", "color": "#996633",
                  "nodes": [ { "id": "x", "cost": 1, "max_level": 1 } ] }""";
        List<SkillRoute> out = new ArrayList<>();
        for (String[] e : new String[][]{{"nusmp:selftest_one", one}, {"nusmp:selftest_two", two}}) {
            RouteSchema.Parsed p = RouteSchema.parse(e[0], e[1]);
            if (!p.clean()) bad.add("the synthetic route " + e[0] + " does not validate: " + String.join("; ", p.problems()));
            else out.add(p.route());
        }
        return out;
    }

    // ================================================================ tree.core.routes
    private static SelfTests.Result routes(MinecraftServer server) {
        RouteIndex idx = RouteRegistry.snapshot();
        List<String> bad = new ArrayList<>();
        for (RouteIndex.Entry e : idx.report()) {
            if (!e.loaded()) bad.add(e.id() + " was not loaded: " + (e.problems().isEmpty() ? "?" : e.problems().get(0)));
            else for (String p : e.problems()) bad.add(e.id() + ": " + p);
        }
        Map<String, String> books = new HashMap<>(), classes = new HashMap<>(), skillOwners = new HashMap<>();
        int nodes = 0, skills = 0;
        for (SkillRoute r : idx.all()) {
            String other = books.put(r.book(), r.id());
            if (other != null) bad.add(r.id() + " and " + other + " share the book " + r.book());
            other = classes.put(r.classId(), r.id());
            if (other != null) bad.add(r.id() + " and " + other + " share the class " + r.classId());
            for (SkillNode n : r.nodes()) {
                nodes++;
                if (!n.grantsSkill()) continue;
                skills++;
                other = skillOwners.put(n.skill(), r.id() + "/" + n.id());
                if (other != null) bad.add(r.id() + "/" + n.id() + " and " + other + " grant the same skill " + n.skill());
                ResourceLocation id = ResourceLocation.tryParse(n.skill());
                ManasSkill skill = id == null ? null : SkillAPI.getSkillRegistry().get(id);
                if (skill == null) bad.add(r.id() + "/" + n.id() + " grants '" + n.skill() + "', which is not in the ManasCore skill registry");
                else if (!(skill instanceof TreeSkill ts)) bad.add(r.id() + "/" + n.id() + ": '" + n.skill() + "' is not a TreeSkill, so the cast gate would not apply to it");
                else if (!ts.routeId().equals(r.id()) || !ts.nodeId().equals(n.id())) bad.add(r.id() + "/" + n.id() + ": the skill says it belongs to " + ts.routeId() + "/" + ts.nodeId());
            }
        }
        int registered = 0;
        for (DeferredRegister<ManasSkill> register : List.of(FireDragonSkills.SKILLS, LightningDragonSkills.SKILLS, SkyDragonSkills.SKILLS, IceMakeSkills.SKILLS)) {
            for (DeferredHolder<ManasSkill, ? extends ManasSkill> holder : register.getEntries()) {
                registered++;
                if (!(holder.get() instanceof TreeSkill ts)) {
                    bad.add("the registered skill " + holder.getId() + " is not a TreeSkill");
                    continue;
                }
                SkillRoute r = idx.byId(ts.routeId());
                SkillNode n = r == null ? null : r.node(ts.nodeId());
                if (n == null) bad.add("the skill " + holder.getId() + " belongs to " + ts.routeId() + "/" + ts.nodeId() + ", which is not a loaded route and node");
                else if (!holder.getId().toString().equals(n.skill())) bad.add("the skill " + holder.getId() + " claims node " + ts.routeId() + "/" + ts.nodeId() + ", but that node grants '" + n.skill() + "'");
            }
        }
        if (!bad.isEmpty()) return problems(bad);
        if (idx.all().isEmpty() && registered == 0) return ok("the registry is empty (no route file is installed yet) and no tree skill is registered: nothing to check, and nothing is wrong");
        return ok(idx.all().size() + " route(s), " + nodes + " node(s), " + skills + " skill(s): every file loaded clean, books and classes are unique, every skill is registered, is a TreeSkill and agrees with its node; "
                + registered + " registered tree skill(s) all belong to a node");
    }

    // ================================================================ tree.core.rules
    private static SelfTests.Result rules(MinecraftServer server) {
        if (!SkillTreeApi.isEnabled()) return ok("skipped: the Skill Tree is switched off in the config (skilltree.enabled)");
        List<String> bad = new ArrayList<>();
        List<SkillRoute> mine = synthetic(bad);
        if (!bad.isEmpty()) return problems(bad);
        FakePlayer fp = SelfTests.player(server.overworld(), PLAYER, 0.5, 100, 0.5);
        PlayerSkillTree live = SkillTreeApi.of(fp) instanceof PlayerSkillTree own ? own : null;
        if (live == null) return fail("the stand-in player's tree is not a PlayerSkillTree");
        CompoundTag saved = live.toTag();
        RouteIndex previous = RouteRegistry.snapshot();
        String note = "";
        try {
            live.copyFrom(new PlayerSkillTree());
            List<SkillRoute> both = new ArrayList<>(previous.all());
            both.addAll(mine);
            RouteRegistry.publish(RouteIndex.of(both));
            rulesOnSynthetic(fp, bad);
            if (bad.isEmpty()) note = skillsOnReal(fp, previous, bad);
        } finally {
            RouteRegistry.publish(previous);
            live.copyFrom(PlayerSkillTree.fromTag(saved));
            SkillTreeApi.reconcileSkills(fp);                      // a tree skill the test taught and a failure left behind goes with the tree it came from
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok("unlock, buy with and without SP, parents, maximum, price lists, respec refund, class switch, cast gate and lock all behave on a real player and the real registry; " + note);
    }

    private static void rulesOnSynthetic(FakePlayer fp, List<String> bad) {
        final String one = "nusmp:selftest_one", two = "nusmp:selftest_two";
        IPlayerSkillTree t = SkillTreeApi.of(fp);

        if (SkillTreeApi.unlockRoute(fp, "nusmp:no_such_route", false) != SkillTreeApi.UnlockResult.UNKNOWN_ROUTE) bad.add("an unknown route was not refused");
        if (SkillTreeApi.upgradeNode(fp, one, "a") != SkillTreeApi.UpgradeResult.NO_ROUTE) bad.add("a node of a locked route was bought");
        if (SkillTreeApi.unlockRoute(fp, one, false) != SkillTreeApi.UnlockResult.OK) bad.add("the first unlock did not succeed");
        if (SkillTreeApi.unlockRoute(fp, one, false) != SkillTreeApi.UnlockResult.ALREADY) bad.add("a second unlock was not reported as ALREADY");
        if (!"selftest_one".equals(t.activeClass())) bad.add("the first route did not become the active class (it is '" + t.activeClass() + "')");
        if (!bad.isEmpty()) return;

        // no points: refused, nothing changes
        if (SkillTreeApi.upgradeNode(fp, one, "a") != SkillTreeApi.UpgradeResult.NO_SP) bad.add("a node was bought with 0 SP");
        if (t.nodeLevel(one, "a") != 0 || t.spSpent() != 0) bad.add("a refused purchase changed the tree");
        if (SkillTreeApi.upgradeNode(fp, one, "ghost") != SkillTreeApi.UpgradeResult.UNKNOWN_NODE) bad.add("an unknown node was not refused");

        // addSp follows the pure rule (the cap comes from the config)
        int before = t.sp();
        SkillTreeApi.addSp(fp, 5, "selftest");
        if (t.sp() != TreeRules.addSp(before, 5, SkillTreeApi.maxSp())) bad.add("addSp gave " + t.sp() + ", the rule says " + TreeRules.addSp(before, 5, SkillTreeApi.maxSp()));
        t.setSp(20);

        // the purchase flow: prices, parents, maximum
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "a"), SkillTreeApi.UpgradeResult.OK, "a level 1");
        if (t.sp() != 19 || t.spSpent() != 1) bad.add("after the first purchase SP is " + t.sp() + "/" + t.spSpent() + ", expected 19/1");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "b"), SkillTreeApi.UpgradeResult.PARENTS, "b needs a at level 2");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "a"), SkillTreeApi.UpgradeResult.OK, "a level 2");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "b"), SkillTreeApi.UpgradeResult.OK, "b level 1 (price 2)");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "b"), SkillTreeApi.UpgradeResult.OK, "b level 2 (price 3)");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "b"), SkillTreeApi.UpgradeResult.MAXED, "b is at its maximum");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "d"), SkillTreeApi.UpgradeResult.PARENTS, "d needs b and c");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "c"), SkillTreeApi.UpgradeResult.OK, "c");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "d"), SkillTreeApi.UpgradeResult.OK, "d (price 4)");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "a"), SkillTreeApi.UpgradeResult.OK, "a level 3");
        expect(bad, SkillTreeApi.upgradeNode(fp, one, "a"), SkillTreeApi.UpgradeResult.MAXED, "a is at its maximum");
        int spent = 1 + 1 + 2 + 3 + 1 + 4 + 1;
        if (t.spSpent() != spent || t.sp() != 20 - spent) bad.add("after the whole tree SP is " + t.sp() + "/" + t.spSpent() + ", expected " + (20 - spent) + "/" + spent);

        // the cast gate
        boolean requireClass = TreeConfig.requireActiveClass();
        if (!SkillTreeApi.canCast(fp, one, "a")) bad.add("the cast gate refuses a bought node of the active class");
        if (SkillTreeApi.canCast(fp, one, "ghost")) bad.add("the cast gate allows a node that does not exist");
        if (SkillTreeApi.canCast(fp, two, "x")) bad.add("the cast gate allows a node of a route that is not unlocked");
        expect(bad, SkillTreeApi.unlockRoute(fp, two, false), SkillTreeApi.UnlockResult.OK, "the second route");
        if (!"selftest_one".equals(t.activeClass())) bad.add("unlocking a second route changed the active class");
        t.setSp(5);
        expect(bad, SkillTreeApi.upgradeNode(fp, two, "x"), SkillTreeApi.UpgradeResult.OK, "x of the second route");
        if (SkillTreeApi.canCast(fp, two, "x") == requireClass) bad.add("a node of an inactive class: the gate says " + SkillTreeApi.canCast(fp, two, "x") + " but requireActiveClass is " + requireClass);

        // the class switch
        TreeRules.ClassVerdict v = SkillTreeApi.switchClass(fp, "selftest_two", false);
        if (v != TreeRules.ClassVerdict.OK) bad.add("the first explicit class change was " + v + ", expected OK");
        if (!SkillTreeApi.canCast(fp, two, "x")) bad.add("after switching class the cast gate still refuses the new class");
        if (SkillTreeApi.canCast(fp, one, "a") == requireClass) bad.add("after switching class the old class's node: the gate says " + SkillTreeApi.canCast(fp, one, "a") + " but requireActiveClass is " + requireClass);
        TreeRules.ClassVerdict again = SkillTreeApi.switchClass(fp, "selftest_one", false);
        TreeRules.ClassVerdict wanted = SkillTreeApi.classSwitchSeconds() > 0 && fp.level().getGameTime() > 0 ? TreeRules.ClassVerdict.COOLDOWN : TreeRules.ClassVerdict.OK;
        if (again != wanted) bad.add("an immediate second class change was " + again + ", expected " + wanted + " (cooldown " + SkillTreeApi.classSwitchSeconds() + " s)");
        if (SkillTreeApi.switchClass(fp, "selftest_one", true) == TreeRules.ClassVerdict.COOLDOWN) bad.add("a forced class change was held by the cooldown");
        if (SkillTreeApi.switchClass(fp, "no_such_class", true) != TreeRules.ClassVerdict.NOT_UNLOCKED) bad.add("a class nobody owns was accepted");
        if (!SkillTreeApi.setActiveClass(fp, "selftest_one")) bad.add("choosing the class that is already active was refused");
        if (!bad.isEmpty()) return;

        // respec: the points come back and the total is conserved; the route stays
        int total = t.sp() + t.spSpent();
        SkillTreeApi.RespecResult r = SkillTreeApi.respec(fp, one);
        if (r.sp() != 13) bad.add("the respec of the first route returned " + r.sp() + " SP, expected 13 (a3 + b5 + c1 + d4)");
        if (r.cleared() != 4) bad.add("the respec cleared " + r.cleared() + " nodes, expected 4");
        if (t.sp() + t.spSpent() != total) bad.add("the respec changed the total of SP (" + total + " -> " + (t.sp() + t.spSpent()) + ")");
        if (t.nodeLevel(one, "a") != 0 || t.nodeLevel(two, "x") != 1) bad.add("the respec cleared the wrong nodes");
        if (!t.hasRoute(one)) bad.add("the respec took the route away");
        if (SkillTreeApi.canCast(fp, one, "a")) bad.add("the cast gate allows a node after its respec");

        // lock: the route goes, and the active class moves to a route that is left
        if (!SkillTreeApi.lockRoute(fp, one)) bad.add("lockRoute refused a route the player has");
        if (t.hasRoute(one)) bad.add("the route is still there after lockRoute");
        if (!"selftest_two".equals(t.activeClass())) bad.add("after locking the active route the class is '" + t.activeClass() + "', expected selftest_two");
        if (!SkillTreeApi.lockRoute(fp, two)) bad.add("lockRoute refused the second route");
        if (!t.activeClass().isEmpty()) bad.add("after locking every route the class is '" + t.activeClass() + "', expected none");
        if (SkillTreeApi.lockRoute(fp, two)) bad.add("lockRoute accepted a route the player no longer has");
    }

    private static void expect(List<String> bad, SkillTreeApi.UpgradeResult got, SkillTreeApi.UpgradeResult wanted, String what) {
        if (got != wanted) bad.add(what + ": got " + got + ", expected " + wanted);
    }

    private static void expect(List<String> bad, SkillTreeApi.UnlockResult got, SkillTreeApi.UnlockResult wanted, String what) {
        if (got != wanted) bad.add(what + ": got " + got + ", expected " + wanted);
    }

    /** With a real route that grants a skill: buying its root teaches the skill, a respec takes it back. Returns a sentence for the result line. */
    private static String skillsOnReal(FakePlayer fp, RouteIndex real, List<String> bad) {
        SkillRoute route = null;
        SkillNode root = null;
        for (SkillRoute r : real.all()) {
            for (SkillNode n : r.roots()) {
                ResourceLocation skillId = n.grantsSkill() ? ResourceLocation.tryParse(n.skill()) : null;
                if (skillId != null && SkillAPI.getSkillRegistry().get(skillId) != null) {
                    route = r;
                    root = n;
                    break;
                }
            }
            if (route != null) break;
        }
        if (route == null) return "no installed route has a root node that grants a registered skill, so learning and forgetting a skill was not exercised";
        ResourceLocation id = ResourceLocation.parse(root.skill());
        try {
            var skills = SkillAPI.getSkillsFrom(fp);
            if (skills == null) return "the stand-in player has no skill storage, so learning and forgetting a skill was not exercised";
            IPlayerSkillTree t = SkillTreeApi.of(fp);
            t.setSp(100);
            if (SkillTreeApi.unlockRoute(fp, route.id(), false) != SkillTreeApi.UnlockResult.OK) {
                bad.add("could not unlock the real route " + route.id());
                return "";
            }
            if (skills.getSkill(id).isPresent()) bad.add("the skill " + id + " was learned by unlocking the route alone");
            if (SkillTreeApi.upgradeNode(fp, route.id(), root.id()) != SkillTreeApi.UpgradeResult.OK) {
                bad.add("could not buy the root " + root.id() + " of " + route.id() + " with 100 SP");
                return "";
            }
            if (skills.getSkill(id).isEmpty()) bad.add("buying " + route.id() + "/" + root.id() + " did not teach " + id);
            if (SkillTreeApi.respec(fp, route.id()).cleared() < 1) bad.add("the respec of " + route.id() + " cleared nothing");
            if (skills.getSkill(id).isPresent()) bad.add("the respec of " + route.id() + " did not take " + id + " back");
            SkillTreeApi.lockRoute(fp, route.id());
            return "buying " + route.id() + "/" + root.id() + " taught " + id + " and the respec took it back";
        } catch (RuntimeException e) {
            return "learning a skill on the stand-in player was skipped (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")";
        }
    }

    // ================================================================ tree.core.sync
    private static SelfTests.Result sync(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<SkillRoute> mine = synthetic(bad);
        if (!bad.isEmpty()) return problems(bad);
        List<SkillRoute> all = new ArrayList<>(RouteRegistry.all());
        all.addAll(mine);

        // the routes through a real FriendlyByteBuf
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RouteCodecs.ROUTES.encode(buf, all);
        int written = buf.readableBytes();
        int expected = RouteWire.varIntSize(all.size());
        for (SkillRoute r : all) expected += RouteWire.size(r);
        if (written != expected) bad.add("the routes encode to " + written + " bytes but RouteWire.size predicts " + expected);
        List<SkillRoute> back = RouteCodecs.ROUTES.decode(buf);
        if (!all.equals(back)) bad.add("the routes changed on the way through the codec");
        if (buf.readableBytes() != 0) bad.add(buf.readableBytes() + " byte(s) were left over after decoding the routes");

        FriendlyByteBuf p1 = new FriendlyByteBuf(Unpooled.buffer());
        RoutesPayload.STREAM_CODEC.encode(p1, new RoutesPayload(all));
        if (!all.equals(RoutesPayload.STREAM_CODEC.decode(p1).routes())) bad.add("the RoutesPayload changed on the way through its codec");

        // a truncated packet is refused
        FriendlyByteBuf whole = new FriendlyByteBuf(Unpooled.buffer());
        RouteCodecs.ROUTES.encode(whole, all);
        int half = whole.readableBytes() / 2;
        FriendlyByteBuf cut = new FriendlyByteBuf(Unpooled.buffer());
        cut.writeBytes(whole, half);
        boolean refused = false;
        try {
            RouteCodecs.ROUTES.decode(cut);
        } catch (RuntimeException expectedFailure) {
            refused = true;
        }
        if (!refused) bad.add("a route packet cut in half was accepted");

        // the tree: NBT, the payload, an independent copy, clamped garbage
        PlayerSkillTree tree = new PlayerSkillTree();
        tree.addRoute("nusmp:selftest_one");
        tree.addRoute("nusmp:selftest_two");
        tree.setSp(12);
        tree.setSpSpent(34);
        tree.setNodeLevel("nusmp:selftest_one", "a", 3);
        tree.setNodeLevel("nusmp:selftest_one", "b", 1);
        tree.setActiveClass("selftest_one");
        tree.setClassChangedAt(987654321L);
        tree.extra("market").putLong("gold", 2_500_000L);
        CompoundTag tag = tree.toTag();
        if (!PlayerSkillTree.fromTag(tag).toTag().equals(tag)) bad.add("a tree changed when saved and loaded through NBT");
        FriendlyByteBuf tb = new FriendlyByteBuf(Unpooled.buffer());
        TreeSyncPayload.STREAM_CODEC.encode(tb, new TreeSyncPayload(tag));
        CompoundTag over = TreeSyncPayload.STREAM_CODEC.decode(tb).tree();
        if (!tag.equals(over)) bad.add("the TreeSyncPayload changed on the way through its codec");
        PlayerSkillTree received = new PlayerSkillTree();
        received.copyFrom(PlayerSkillTree.fromTag(over));
        if (!received.toTag().equals(tag)) bad.add("a received tree is not the tree that was sent");
        tree.setSp(1);
        if (received.sp() != 12) bad.add("a received tree shares state with the one it was copied from");
        CompoundTag garbage = new CompoundTag();
        garbage.putInt("sp", -5);
        garbage.putInt("spent", -9);
        PlayerSkillTree clamped = PlayerSkillTree.fromTag(garbage);
        if (clamped.sp() != 0 || clamped.spSpent() != 0) bad.add("negative numbers in a saved tree were not clamped to 0");
        if (!PlayerSkillTree.fromTag(new CompoundTag()).isEmpty()) bad.add("an empty tag did not load as an empty tree");

        // the two requests, and the refusal of an oversized text
        FriendlyByteBuf rb = new FriendlyByteBuf(Unpooled.buffer());
        UnlockNodePayload.STREAM_CODEC.encode(rb, new UnlockNodePayload("nusmp:fire_dragon_slayer", "roar"));
        UnlockNodePayload un = UnlockNodePayload.STREAM_CODEC.decode(rb);
        if (!un.route().equals("nusmp:fire_dragon_slayer") || !un.node().equals("roar")) bad.add("the UnlockNodePayload changed on the way through its codec");
        FriendlyByteBuf cb = new FriendlyByteBuf(Unpooled.buffer());
        SetClassPayload.STREAM_CODEC.encode(cb, new SetClassPayload("fire_dragon_slayer"));
        if (!SetClassPayload.STREAM_CODEC.decode(cb).classId().equals("fire_dragon_slayer")) bad.add("the SetClassPayload changed on the way through its codec");
        FriendlyByteBuf big = new FriendlyByteBuf(Unpooled.buffer());
        big.writeUtf("n".repeat(200), 1000);
        big.writeUtf("roar", 1000);
        boolean tooLong = false;
        try {
            UnlockNodePayload.STREAM_CODEC.decode(big);
        } catch (RuntimeException expectedFailure) {
            tooLong = true;
        }
        if (!tooLong) bad.add("an UnlockNodePayload with a 200-character route id was accepted");

        if (!bad.isEmpty()) return problems(bad);
        return ok(all.size() + " route(s) (" + RouteRegistry.all().size() + " installed and 2 synthetic) and the tree, the routes payload and both requests survive the real byte buffer, a truncated packet and an oversized text are refused"
                + (RouteRegistry.all().isEmpty() ? "; the registry is empty (no route file is installed yet), so only the synthetic routes were sent" : ""));
    }
}
