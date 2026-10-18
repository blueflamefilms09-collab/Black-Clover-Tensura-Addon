package com.newuniverse.nusmp.skilltree.ft.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.kit.RouteFiles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Sky Dragon Slayer's numbers are the only thing standing between "a healer" and "a heal button that fixes everything", so they are tested from the data outward: the SHIPPED route file is read and run
 * through the same {@code problems} check the in-game self-test runs (shape, prices, canvas, balance law, fallback table, two-branch Secret Art), the file text is checked against the schema (colour, ids, book), the
 * skill ids of the file are checked against the ids the code registers, and the small pure rules (the heal cap, the wounded-first choice, the cleanse order, the drill timeline, the squeeze, the fall factor)
 * are pinned with edge cases. One test then BREAKS the data on purpose (a damage of 15 on the roar) and demands that the check notice, which is the proof that the check can fail.
 */
class SkyNumbersTest {
    private static final String FILE = "sky_dragon_slayer.json";

    @Test
    void theShippedRouteObeysTheLawTheShapeAndTheFallbackTable() throws Exception {
        SkillRoute route = RouteFiles.load(FILE);
        assertEquals(List.of(), SkyNumbers.problems(route));
        assertEquals(9, route.nodes().size());
        assertEquals(77, route.totalCost(), "the whole route costs 77 SP");
    }

    @Test
    void theFileTextFollowsTheSchema() throws Exception {
        JsonObject o = RouteFiles.json(FILE);
        assertEquals("fairy_tail", o.get("magic").getAsString());
        assertTrue(o.get("color").getAsString().matches("#[0-9A-Fa-f]{6}"), "colour is #RRGGBB");
        assertTrue(o.get("book").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), "the book is a lower-case resource location");
        assertTrue(o.get("class_id").getAsString().matches("[a-z][a-z0-9_]*"), "the class id is lower-case snake");
        assertEquals("Wing of Tender Tempests", o.get("book_name").getAsString());
        assertEquals(2, o.get("loot_weight").getAsInt(), "the owner's brief: Sky weighs 2 in the dungeon drop");
        assertTrue(o.get("book_lore").getAsString().length() > 10);
        Set<String> ids = new HashSet<>();
        for (JsonElement el : o.getAsJsonArray("nodes")) {
            JsonObject n = el.getAsJsonObject();
            String id = n.get("id").getAsString();
            assertTrue(id.matches("[a-z][a-z0-9_]*"), id + " is lower-case snake");
            assertTrue(ids.add(id), id + " is unique");
            assertTrue(n.get("name").getAsString().length() > 2, id + " has a name");
            assertTrue(n.get("desc").getAsString().length() > 20, id + " has a description for the tooltip");
            assertTrue(n.has("icon") && n.get("icon").getAsString().startsWith("minecraft:"), id + " has an item icon");
        }
    }

    @Test
    void theSkillIdsOfTheFileAreTheOnesTheCodeRegisters() throws Exception {
        SkillRoute route = RouteFiles.load(FILE);
        Set<String> inFile = new HashSet<>();
        for (SkillNode n : route.nodes()) if (n.grantsSkill()) inFile.add(n.skill());
        Set<String> inCode = new HashSet<>();
        for (String s : List.of(SkyRoute.SKILL_ROAR, SkyRoute.SKILL_WING_ATTACK, SkyRoute.SKILL_WAVE_WIND, SkyRoute.SKILL_BREATH, SkyRoute.SKILL_TROIA, SkyRoute.SKILL_ENCHANT, SkyRoute.SKILL_DRILL)) inCode.add("nusmp:" + s);
        assertEquals(inCode, inFile);
        Set<String> passivesInFile = new HashSet<>();
        for (SkillNode n : route.nodes()) passivesInFile.addAll(n.passives());
        assertEquals(Set.of(SkyRoute.PASSIVE_CUSHION, SkyRoute.PASSIVE_AURA), passivesInFile);
        assertEquals(SkyRoute.ID, route.id());
        assertEquals(SkyRoute.CLASS_ID, route.classId());
    }

    @Test
    void theFallbackTableCoversExactlyTheNodesOfTheFile() throws Exception {
        SkillRoute route = RouteFiles.load(FILE);
        Set<String> nodes = new HashSet<>();
        for (SkillNode n : route.nodes()) nodes.add(n.id());
        assertEquals(nodes, SkyNumbers.fallbackNodes());
        assertEquals(0.0, SkyNumbers.fallback("roar", "no_such_key"), 0.0, "an unknown pair answers 0, and the test above makes sure no real pair is unknown");
    }

    @Test
    void breakingTheDataIsCaught() throws Exception {
        SkillRoute good = RouteFiles.load(FILE);
        SkillNode roar = good.node(SkyRoute.ROAR);
        Map<String, List<Double>> hot = new LinkedHashMap<>(roar.values());
        hot.put("damage", List.of(5.0, 5.5, 6.5, 7.0, 15.0));
        List<SkillNode> nodes = new ArrayList<>(good.nodes());
        nodes.set(0, new SkillNode(roar.id(), roar.name(), roar.desc(), roar.parents(), roar.cost(), roar.costs(), roar.maxLevel(), roar.skill(), roar.passives(), hot, roar.x(), roar.y(), roar.icon()));
        SkillRoute broken = new SkillRoute(good.id(), good.magic(), good.name(), good.desc(), good.book(), good.bookName(), good.bookLore(), good.color(), good.icon(), good.classId(), good.duplicateSp(), good.lootWeight(), nodes);
        List<String> problems = SkyNumbers.problems(broken);
        assertFalse(problems.isEmpty(), "a roar of 15 raw must be refused");
        boolean named = false;
        for (String s : problems) named |= s.contains("roar.damage");
        assertTrue(named, "the problem names the table: " + problems);
    }

    @Test
    void aHealIsAShareOfMaxHealthNeverOverTheCap() {
        assertEquals(2.0f, SkyNumbers.healAmount(20f, 0.10), 1e-5f);
        assertEquals(5.0f, SkyNumbers.healAmount(20f, 0.25), 1e-5f);
        assertEquals(5.0f, SkyNumbers.healAmount(20f, 0.90), 1e-5f, "a bad table cannot make it more than 25 percent");
        assertEquals(0.0f, SkyNumbers.healAmount(20f, -0.5), 1e-5f);
        assertEquals(0.0f, SkyNumbers.healAmount(-4f, 0.1), 1e-5f);
    }

    @Test
    void theMostWoundedAreHealedFirstAndTheHealthyNever() {
        double[] fraction = {1.0, 0.2, 0.5, 0.2};
        double[] distance = {1.0, 5.0, 2.0, 3.0};
        assertEquals(List.of(3, 1), toList(SkyNumbers.mostWounded(fraction, distance, 2)), "equal wounds: the nearer one first");
        assertEquals(List.of(3, 1, 2), toList(SkyNumbers.mostWounded(fraction, distance, 9)), "the healthy friend 0 is never picked");
        assertEquals(List.of(), toList(SkyNumbers.mostWounded(new double[] {1.0, 1.0}, new double[] {1.0, 2.0}, 4)));
        assertEquals(List.of(), toList(SkyNumbers.mostWounded(fraction, distance, 0)));
    }

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>();
        for (int x : a) out.add(x);
        return out;
    }

    @Test
    void troiaStripsNauseaFirstThenThePoisons() {
        List<String> on = List.of("slowness", "poison", "nausea", "weakness", "some_modded_curse");
        assertEquals(List.of("nausea"), SkyNumbers.pickStrips(on, 1));
        assertEquals(List.of("nausea", "poison"), SkyNumbers.pickStrips(on, 2));
        assertEquals(List.of("nausea", "poison", "slowness"), SkyNumbers.pickStrips(on, 3));
        assertEquals(List.of(), SkyNumbers.pickStrips(on, 0));
        assertEquals(5, SkyNumbers.pickStrips(on, 99).size());
        assertEquals("some_modded_curse", SkyNumbers.pickStrips(on, 99).get(4), "an effect the table does not know goes last");
    }

    @Test
    void theDrillRunsPulsesThenOneBurstThenNothing() {
        assertEquals(1, SkyNumbers.drillStep(0, 3));
        assertEquals(2, SkyNumbers.drillStep(10, 3));
        assertEquals(3, SkyNumbers.drillStep(20, 3));
        assertEquals(4, SkyNumbers.drillStep(30, 3), "pulses + 1 is the burst");
        assertEquals(SkyNumbers.DRILL_NONE, SkyNumbers.drillStep(40, 3));
        assertEquals(SkyNumbers.DRILL_NONE, SkyNumbers.drillStep(5, 3));
        assertEquals(SkyNumbers.DRILL_NONE, SkyNumbers.drillStep(-10, 3));
        assertEquals(31, SkyNumbers.drillLife(3), "the zone must still be alive on tick 30 and not on tick 40");
        assertEquals(2, SkyNumbers.drillStep(10, 1));
        assertEquals(11, SkyNumbers.drillLife(1));
        int bursts = 0;
        for (int age = 0; age < SkyNumbers.drillLife(3); age++) if (SkyNumbers.drillStep(age, 3) == 4) bursts++;
        assertEquals(1, bursts, "exactly one burst inside the zone's life");
    }

    @Test
    void theWallShrinksEvenlyToSixtyPercent() {
        assertEquals(5.0, SkyNumbers.squeezeRadius(5.0, 1, 3), 1e-9);
        assertEquals(4.3333333, SkyNumbers.squeezeRadius(5.0, 2, 3), 1e-6);
        assertEquals(3.0, SkyNumbers.squeezeRadius(5.0, 4, 3), 1e-9, "the burst is at 60 percent");
        assertEquals(3.0, SkyNumbers.squeezeRadius(5.0, 9, 3), 1e-9, "a step past the burst stays at the burst");
        assertEquals(5.0, SkyNumbers.squeezeRadius(5.0, 0, 3), 1e-9, "a step before the first stays at the full radius");
    }

    @Test
    void theUpdraftFactorIsClamped() {
        assertEquals(0.6f, SkyNumbers.fallMultiplier(0.4), 1e-6f);
        assertEquals(0.0f, SkyNumbers.fallMultiplier(1.0), 1e-6f);
        assertEquals(0.0f, SkyNumbers.fallMultiplier(2.5), 1e-6f, "more than total protection is still total, never a heal");
        assertEquals(1.0f, SkyNumbers.fallMultiplier(-1.0), 1e-6f, "a negative reduction never makes a fall worse");
    }
}
