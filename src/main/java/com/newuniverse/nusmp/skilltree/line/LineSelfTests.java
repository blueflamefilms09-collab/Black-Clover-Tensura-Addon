package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.PlayerSkillTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the unit tests prove the pure parts of the line engine; what only the real game can prove is that the executors and the lookups agree with the REAL registries (every effect, attribute and effect
 * shape word of the catalogue names something that exists), that the gate, the switch, the transient modifiers, the stance and the cast service work together on a real player object with a real attachment
 * and the real route registry, and that a line leaves nothing behind when it is unequipped. Two self-tests (keys {@code tree.lines.*}, run by the CI server smoke test like the {@code tree.core.*} ones): one
 * checks the vocabulary and the executors against the catalogue, the other plays a small synthetic line through a stand-in player and puts everything back in a {@code finally}.
 */
public final class LineSelfTests {
    private LineSelfTests() {}

    public static void register() {
        SelfTests.register("tree.lines.kinds", LineSelfTests::kinds);
        SelfTests.register("tree.lines.engine", LineSelfTests::engine);
    }

    private static SelfTests.Result fail(List<String> bad) { return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size())))); }

    // ================================================================ tree.lines.kinds
    private static SelfTests.Result kinds(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        LineCatalog cat = LineCatalog.get();
        if (!cat.loaded()) return SelfTests.Result.fail("the effect catalogue (nusmp_lines/catalog.json) could not be read from the jar");
        Set<String> active = new HashSet<>(ActiveExecutors.kinds());
        for (String k : cat.activeKinds()) if (!active.remove(k)) bad.add("the active kind " + k + " has no executor");
        for (String k : cat.stanceKinds()) if (!active.remove(k)) bad.add("the stance kind " + k + " has no executor");
        for (String k : active) bad.add("the executor " + k + " is not a kind of the catalogue");
        Set<String> passive = new HashSet<>(PassiveExecutors.kinds());
        for (String k : cat.passiveKinds()) if (!passive.remove(k)) bad.add("the passive kind " + k + " has no executor");
        for (String k : passive) bad.add("the passive executor " + k + " is not a kind of the catalogue");
        for (String k : cat.attrKinds()) if (!LineAttrs.kinds().contains(k)) bad.add("the attr kind " + k + " has no executor");
        for (LineCatalog.Kind k : allKinds(cat)) {
            for (var en : k.enums().entrySet()) {
                for (String w : en.getValue()) {
                    boolean known = switch (en.getKey()) {
                        case "vfx" -> LineLookups.shape(w, null) != null;
                        case "effect", "effect2" -> w.equals("none") || LineLookups.effect(w) != null;
                        case "attribute" -> LineLookups.attribute(w) != null;
                        default -> true;
                    };
                    if (!known) bad.add("kind " + k.name() + ": the word '" + w + "' of " + en.getKey() + " names nothing in the game");
                }
            }
        }
        if (!bad.isEmpty()) return fail(bad);
        return SelfTests.Result.ok(cat.activeKinds().size() + " arts, " + cat.passiveKinds().size() + " passives, the stance and attr all have an executor; every effect, attribute and effect-shape word of the catalogue is a real thing");
    }

    private static List<LineCatalog.Kind> allKinds(LineCatalog cat) {
        List<LineCatalog.Kind> out = new ArrayList<>();
        for (String n : cat.allKinds()) out.add(cat.anyKind(n));
        return out;
    }

    // ================================================================ tree.lines.engine
    private static final String LINE = "nusmp:selftest_line";

    private static final String JSON = """
            { "franchise": "tensura", "magic": "tensura", "name": "Selftest Line", "book": "nusmp:selftest_line_book", "class_id": "selftest_line", "color": "#44AACC",
              "nodes": [
                { "id": "stance", "type": "active_ability", "kind": "stance", "params": {"vfx": "SPIRIT_AURA"}, "cost": 1, "max_level": 1, "x": 0, "y": 0,
                  "values": {"armor": [4], "speed_pct": [0.1], "cooldown": [40]} },
                { "id": "buff", "type": "active_ability", "kind": "buff_self", "params": {"effect": "speed", "effect2": "none", "vfx": "WIND_RING"}, "parents": ["stance"], "cost": 1, "max_level": 1, "x": 2, "y": 0,
                  "values": {"seconds": [5], "amp": [0], "cooldown": [200], "cost": [0.0]} },
                { "id": "hp", "type": "attribute_modifier", "kind": "attr", "params": {"attribute": "armor", "op": "add"}, "parents": ["stance"], "cost": 1, "max_level": 1, "x": 2, "y": 2,
                  "values": {"amount": [3]} },
                { "id": "crit", "type": "passive_toggle", "kind": "crit", "parents": ["hp"], "cost": 1, "max_level": 1, "x": 4, "y": 2, "values": {"chance": [0.1], "mult": [1.5]} }
              ] }""";

    private static SelfTests.Result engine(MinecraftServer server) {
        if (!SkillTreeApi.isEnabled()) return SelfTests.Result.ok("skipped: the Skill Tree is switched off in the config (skilltree.enabled)");
        RouteSchema.Parsed parsed = RouteSchema.parse(LINE, JSON);
        if (!parsed.clean()) return SelfTests.Result.fail("the synthetic line does not validate: " + String.join("; ", parsed.problems()));
        SkillRoute line = parsed.route();
        FakePlayer fp = SelfTests.player(server.overworld(), "lines_tester", 0.5, 100, 0.5);
        if (!(SkillTreeApi.of(fp) instanceof PlayerSkillTree live)) return SelfTests.Result.fail("the stand-in player's tree is not a PlayerSkillTree");
        CompoundTag saved = live.toTag();
        RouteIndex previous = RouteRegistry.snapshot();
        List<String> bad = new ArrayList<>();
        try {
            live.copyFrom(new PlayerSkillTree());
            List<SkillRoute> all = new ArrayList<>(previous.all());
            all.add(line);
            RouteRegistry.publish(RouteIndex.of(all));
            run(fp, live, bad);
        } finally {
            LineAttrs.clear(fp);
            LineStances.clear(fp);
            RouteRegistry.publish(previous);
            live.copyFrom(PlayerSkillTree.fromTag(saved));
            fp.getPersistentData().remove(LineState.ROOT);
        }
        if (!bad.isEmpty()) return fail(bad);
        return SelfTests.Result.ok("a stance, a stat bonus, a passive and an art behave on a real player: the gate follows the equipped line, the switch turns the bonus off, the modifiers are transient and are removed, the cast sets a cooldown");
    }

    private static void run(FakePlayer fp, IPlayerSkillTree t, List<String> bad) {
        t.setSp(20);
        if (SkillTreeApi.unlockRoute(fp, LINE, false) != SkillTreeApi.UnlockResult.OK) {
            bad.add("the synthetic line could not be unlocked");
            return;
        }
        for (String n : new String[]{"stance", "buff", "hp", "crit"}) {
            if (SkillTreeApi.upgradeNode(fp, LINE, n) != SkillTreeApi.UpgradeResult.OK) bad.add("buying " + n + " failed");
        }
        if (!bad.isEmpty()) return;
        if (!t.isPassiveEnabled(LINE, "hp") || !t.isPassiveEnabled(LINE, "crit")) bad.add("a bought passive does not start ON");
        if (t.isPassiveEnabled(LINE, "buff") && t.passiveToggles().containsKey(LINE + "#buff")) bad.add("an art got a switch");

        AttributeInstance armor = fp.getAttribute(Attributes.ARMOR);
        double base = armor == null ? 0 : armor.getValue();
        LinePassives.beat(fp);
        if (LinePassives.active(fp).size() != 2) bad.add("the equipped line should have 2 active passive nodes, has " + LinePassives.active(fp).size());
        if (armor != null && armor.getValue() != base + 3) bad.add("the stat bonus is not applied (armor " + armor.getValue() + ", expected " + (base + 3) + ")");
        if (armor != null && armor.getModifier(LineAttrs.modifierId(LINE, "hp")) == null) bad.add("the stat modifier is missing");

        SkillTreeApi.setPassiveEnabled(fp, LINE, "hp", false);
        LineAttrs.sync(fp);
        if (armor != null && armor.getValue() != base) bad.add("switching the stat bonus off did not remove it (armor " + armor.getValue() + ")");
        if (LinePassives.active(fp).size() != 1) bad.add("a switched-off passive still counts");
        SkillTreeApi.setPassiveEnabled(fp, LINE, "hp", true);
        LineAttrs.sync(fp);
        if (armor != null && armor.getValue() != base + 3) bad.add("switching the stat bonus on did not apply it again");

        if (SkillTreeApi.setPassiveEnabled(fp, LINE, "buff", false)) bad.add("an art accepted a switch");
        if (SkillTreeApi.setPassiveEnabled(fp, LINE, "nosuch", false)) bad.add("a node that does not exist accepted a switch");

        LineCast.Result first = LineCast.cast(fp, LINE, "buff");
        if (first != LineCast.Result.OK) bad.add("casting the art gave " + first + ", expected OK");
        if (LineCast.cast(fp, LINE, "buff") != LineCast.Result.COOLDOWN) bad.add("a second cast at once was not refused for the cooldown");
        if (LineCast.cast(fp, LINE, "crit") == LineCast.Result.OK) bad.add("a passive could be cast as an art");

        if (LineCast.cast(fp, LINE, "stance") != LineCast.Result.OK) bad.add("the stance could not be taken up");
        if (!(LINE + "#stance").equals(LineState.stance(fp))) bad.add("the stance is not recorded");
        if (armor != null && armor.getValue() != base + 3 + 4) bad.add("the stance armour is not applied (armor " + armor.getValue() + ")");

        t.setActiveClass("");                                                         // unequip the line: the next beat must leave nothing behind
        LinePassives.beat(fp);
        if (armor != null && armor.getValue() != base) bad.add("an unequipped line still gives armour (armor " + armor.getValue() + ", expected " + base + ")");
        if (!LineState.stance(fp).isEmpty()) bad.add("the stance of an unequipped line was not dropped");
        if (!LinePassives.active(fp).isEmpty()) bad.add("an unequipped line still has active passives");
        if (LineCast.cast(fp, LINE, "buff") != LineCast.Result.LOCKED) bad.add("an art of an unequipped line could be cast");
        t.setActiveClass("selftest_line");
        SkillTreeApi.lockRoute(fp, LINE);
        if (!t.passiveToggles().isEmpty()) bad.add("taking the line away left switches behind");
    }
}
