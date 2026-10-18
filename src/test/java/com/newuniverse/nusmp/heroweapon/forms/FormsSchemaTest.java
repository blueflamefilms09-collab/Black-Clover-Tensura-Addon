package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the schema is the gate every forms file passes, in JUnit and in the game, so each rule of design.md section 6 has a case that breaks it and a message the
 * author can act on, and a clean file of each family passes with no complaint. A rule that cannot be broken in a test is a rule nobody enforces.
 */
class FormsSchemaTest {
    // ---------------------------------------------------------------- fixtures
    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put((String) kv[i], kv[i + 1]);
        return out;
    }

    private static Map<String, Object> req(String item, long count) { return m("item", item, "count", count); }

    private static Map<String, Object> tagReq(String tag, long count) { return m("tag", tag, "count", count); }

    /** The base form: tier 0, nothing to ask for. */
    private static Map<String, Object> base() {
        return m("id", "base", "name", "Small Shield", "desc", "The starting shape.", "tier", 0L, "parents", List.of(), "requires", List.of(), "mastery", 0L,
                "stats", m("armor", 2.0), "abilityBonus", m(), "skills", m(), "passives", List.of(), "look", "base", "curse", false, "sin", null, "corruptionPerSecond", 0L);
    }

    /** An ordinary form with sensible defaults; tests change what they want to break. */
    private static Map<String, Object> form(String id, long tier, String... parents) {
        return m("id", id, "name", "Form " + id, "desc", "A form called " + id + ".", "tier", tier, "parents", List.of(parents),
                "requires", new ArrayList<>(List.of(req("minecraft:leather", 16))), "mastery", 100L, "stats", m("armor", 1.0), "abilityBonus", m("armor", 0.25),
                "skills", m(), "passives", List.of());
    }

    private static Map<String, Object> curse(String id, long tier, String sin, String... parents) {
        Map<String, Object> f = form(id, tier, parents);
        f.put("curse", true);
        f.put("sin", sin);
        f.put("look", "curse");
        f.put("corruptionPerSecond", 3L);
        f.put("requires", new ArrayList<>());
        return f;
    }

    private static Map<String, Object> file(HeroKind k, List<Map<String, Object>> forms) {
        return m("weapon", k.itemId(), "forms", new ArrayList<Object>(forms));
    }

    @SafeVarargs
    private static List<Map<String, Object>> list(Map<String, Object>... forms) { return new ArrayList<>(List.of(forms)); }

    /** Eight ordinary forms and a two-form Wrath series: a clean Holy Shield. */
    private static List<Map<String, Object>> holyShield() {
        return list(base(), form("a1", 1, "base"), form("a2", 1, "base"), form("b1", 2, "a1"), form("b2", 2, "a2"), form("c1", 3, "b1", "b2"), form("c2", 3, "b1"),
                form("d1", 4, "c1"), curse("wrath_1", 1, "wrath", "base"), curse("wrath_2", 2, "wrath", "wrath_1"));
    }

    private static boolean hasError(FormsSchema.Parsed p, String part) {
        for (String e : p.errors()) if (e.contains(part)) return true;
        return false;
    }

    private static boolean hasWarning(FormsSchema.Parsed p, String part) {
        for (String w : p.warnings()) if (w.contains(part)) return true;
        return false;
    }

    private static boolean has(FormsSchema.Parsed p, String id) {
        for (HeroForm f : p.forms()) if (f.id().equals(id)) return true;
        return false;
    }

    private static final FormsSchema.SkillIndex SKILLS = new FormsSchema.SkillIndex() {
        @Override public boolean exists(String id) { return Set.of("shield.air_strike", "shield.prison", "shield.big", "shield.bigger", "sword.slash", "bow.volley", "hammer.smash").contains(id); }

        @Override public boolean ultimate(String id) { return id.equals("shield.big") || id.equals("shield.bigger"); }
    };

    // ---------------------------------------------------------------- clean files
    @Test
    void aCleanHolyFilePassesAndIsOrderedBaseFirstThenByTier() {
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, holyShield()), null);
        assertTrue(p.clean(), p.report());
        assertFalse(p.fileRejected());
        assertEquals(10, p.forms().size());
        assertEquals("base", p.forms().get(0).id());
        int last = 0;
        for (HeroForm f : p.forms()) {
            assertTrue(f.tier() >= last, "forms must be ordered by tier");
            last = f.tier();
            assertEquals(HeroKind.SHIELD, f.kind());
        }
        HeroForm w = p.forms().stream().filter(f -> f.id().equals("wrath_1")).findFirst().orElseThrow();
        assertTrue(w.curse());
        assertEquals(Sin.WRATH, w.sin());
        assertEquals("curse", w.look());
        assertEquals(3, w.corruptionPerSecond());
        assertTrue(w.requires().isEmpty());
    }

    @Test
    void aCleanVassalFilePassesWithThreeForms() {
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("heavy", 1, "base"), form("heavier", 2, "heavy"))), null);
        assertTrue(p.clean(), p.report());
        assertEquals(3, p.forms().size());
    }

    @Test
    void requirementsAreNormalisedAndTagsAreKept() {
        Map<String, Object> f = form("leafy", 1, "base");
        f.put("requires", new ArrayList<>(List.of(tagReq("#minecraft:leaves", 32), req("glow_berries", 8))));
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
        assertTrue(p.clean(), p.report());
        HeroForm leafy = p.forms().stream().filter(x -> x.id().equals("leafy")).findFirst().orElseThrow();
        assertTrue(leafy.requires().get(0).isTag());
        assertEquals("minecraft:leaves", leafy.requires().get(0).tag());
        assertEquals("", leafy.requires().get(0).item());
        assertEquals("minecraft:glow_berries", leafy.requires().get(1).item());
        assertFalse(leafy.requires().get(1).isTag());
    }

    // ---------------------------------------------------------------- the file as a whole
    @Test
    void theBaseFormMustComeFirst() {
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(form("heavy", 1, "base"), base())), null);
        assertTrue(p.fileRejected());
        assertTrue(hasError(p, "first form must be the base form"), p.report());
        assertEquals(1, p.forms().size());
        assertEquals("base", p.forms().get(0).id());
        assertEquals(HeroKind.HAMMER.display(), p.forms().get(0).name(), "a rejected file leaves the synthetic base");
    }

    @Test
    void aFileOfAnotherWeaponIsRejected() {
        Map<String, Object> f = file(HeroKind.HAMMER, list(base(), form("heavy", 1, "base"), form("heavier", 2, "heavy")));
        f.put("weapon", "vassal_axe");
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, f, null);
        assertTrue(p.fileRejected());
        assertTrue(hasError(p, "copied file"), p.report());
    }

    @Test
    void aMissingWeaponKeyOnlyWarns() {
        Map<String, Object> f = file(HeroKind.HAMMER, list(base(), form("heavy", 1, "base"), form("heavier", 2, "heavy")));
        f.remove("weapon");
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, f, null);
        assertFalse(p.fileRejected());
        assertTrue(p.errors().isEmpty());
        assertTrue(hasWarning(p, "no \"weapon\" key"));
    }

    @Test
    void garbageNeverThrowsAndRejectsTheFile() {
        Object[] junk = {null, 5L, "text", List.of(), m(), m("forms", 4L), m("forms", List.of()), m("forms", List.of("x")), m("forms", List.of(m()))};
        for (Object tree : junk) {
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.SHIELD, tree, SKILLS);
            assertTrue(p.fileRejected(), String.valueOf(tree));
            assertEquals(1, p.forms().size());
            assertFalse(p.errors().isEmpty());
        }
    }

    @Test
    void textThatIsNotJsonIsRejectedWithAReadableError() {
        FormsSchema.Parsed p = FormsSchema.parseText(HeroKind.SHIELD, "{ \"forms\": [ ", null);
        assertTrue(p.fileRejected());
        assertTrue(hasError(p, "not valid JSON"), p.report());
    }

    @Test
    void jsonTextRoundTripsThroughTheLoaderPath() {
        String json = "{ \"weapon\": \"vassal_hammer\", \"forms\": ["
                + "{\"id\":\"base\",\"name\":\"Hammer\",\"desc\":\"d\",\"tier\":0,\"mastery\":0},"
                + "{\"id\":\"heavy\",\"name\":\"Heavy\",\"desc\":\"d\",\"tier\":1,\"parents\":[\"base\"],\"requires\":[{\"item\":\"minecraft:iron_ingot\",\"count\":24}],\"mastery\":120,\"stats\":{\"attack_damage\":2.5}},"
                + "{\"id\":\"heavier\",\"name\":\"Heavier\",\"desc\":\"d\",\"tier\":2,\"parents\":[\"heavy\"],\"requires\":[{\"tag\":\"c:ingots/gold\",\"count\":32.0}],\"mastery\":200}] }";
        FormsSchema.Parsed p = FormsSchema.parseText(HeroKind.HAMMER, json, null);
        assertTrue(p.clean(), p.report());
        assertEquals(2.5, p.forms().get(1).stat("attack_damage"), 1e-9);
        assertEquals(32, p.forms().get(2).requires().get(0).count(), "a whole number written as 32.0 is accepted");
    }

    @Test
    void aKeyWrittenTwiceIsReportedBecauseTheParserWouldKeepOnlyTheLastOne() {
        String json = "{ \"weapon\": \"vassal_hammer\", \"forms\": ["
                + "{\"id\":\"base\",\"name\":\"Hammer\",\"desc\":\"d\",\"tier\":0,\"mastery\":0},"
                + "{\"id\":\"heavy\",\"name\":\"Heavy\",\"desc\":\"d\",\"tier\":1,\"parents\":[\"base\"],\"requires\":[{\"item\":\"minecraft:iron_ingot\",\"count\":24}],\"mastery\":120,"
                + "\"skills\":{\"key1\":\"hammer.a\",\"key1\":\"hammer.b\"}},"
                + "{\"id\":\"heavier\",\"name\":\"Heavier\",\"desc\":\"d\",\"tier\":2,\"parents\":[\"heavy\"],\"requires\":[{\"item\":\"minecraft:gold_ingot\",\"count\":24}],\"mastery\":120,"
                + "\"skills\":{\"key1\":\"hammer.a\"}}] }";
        FormsSchema.Parsed p = FormsSchema.parseText(HeroKind.HAMMER, json, null);
        assertTrue(hasWarning(p, "the key \"key1\" appears twice in one object (inside \"skills\")"), p.report());
        assertEquals(1, p.warnings().stream().filter(w -> w.contains("appears twice")).count(), "the same key in two different objects is not a duplicate");
        assertTrue(p.errors().isEmpty(), "the file is still used: " + p.report());
        assertEquals(List.of("key1"), List.copyOf(p.forms().get(1).skills().keySet()));
        assertTrue(FormsJson.duplicateKeys("{ not json").isEmpty(), "text that is not JSON is the parser's to report, not the duplicate finder's");
        assertTrue(FormsJson.duplicateKeys("{\"a\":1,\"b\":{\"a\":2}}").isEmpty(), "a repeated name at another depth is fine");
        assertEquals(1, FormsJson.duplicateKeys("{\"a\":1,\"a\":2}").size());
    }

    // ---------------------------------------------------------------- one rule, one case
    @Test
    void anUnknownStatSkipsTheFormAndEverythingBuiltOnIt() {
        Map<String, Object> bad = form("a1", 1, "base");
        bad.put("stats", m("strength", 3.0));
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), bad, form("b1", 2, "a1"), form("other", 1, "base"))), null);
        assertFalse(p.fileRejected());
        assertTrue(hasError(p, "unknown stat 'strength'"), p.report());
        assertFalse(has(p, "a1"));
        assertFalse(has(p, "b1"), "a child of a skipped form is skipped too");
        assertTrue(hasError(p, "the parent 'a1' was not accepted"), p.report());
        assertTrue(has(p, "other"), "an unrelated form stays");
        assertTrue(has(p, "base"));
    }

    @Test
    void statBudgetsAreEnforcedBothWays() {
        String[][] cases = {{"armor", "9.0"}, {"armor", "-9.0"}, {"armor_toughness", "4.5"}, {"max_health", "13"}, {"attack_damage", "15"}, {"attack_speed", "1.5"},
                {"attack_speed", "-3.5"}, {"movement_speed", "0.25"}, {"knockback_resistance", "0.7"}, {"reach", "2.5"}, {"luck", "3.5"}};
        for (String[] c : cases) {
            Map<String, Object> f = form("a1", 1, "base");
            f.put("stats", m(c[0], Double.parseDouble(c[1])));
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
            assertTrue(hasError(p, "outside the budget"), c[0] + " " + c[1] + ": " + p.report());
            assertFalse(has(p, "a1"));
        }
        Map<String, Object> top = form("a1", 1, "base");
        top.put("stats", m("armor", 8.0, "armor_toughness", 4.0, "max_health", 12.0, "attack_damage", 14.0, "attack_speed", 1.0, "movement_speed", 0.2,
                "knockback_resistance", 0.6, "reach", 2.0, "luck", 3.0));
        FormsSchema.Parsed ok = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), top, form("xx", 1, "base"))), null);
        assertTrue(ok.clean(), "the budget itself is allowed: " + ok.report());
    }

    @Test
    void anAbilityBonusMayUseOnlyAQuarterOfTheBudget() {
        Map<String, Object> f = form("a1", 1, "base");
        f.put("abilityBonus", m("armor", 2.5));
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
        assertTrue(hasError(p, "abilityBonus"), p.report());
        Map<String, Object> edge = form("a1", 1, "base");
        edge.put("abilityBonus", m("armor", 2.0, "attack_speed", 0.25, "movement_speed", 0.05));
        assertTrue(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), edge, form("xx", 1, "base"))), null).clean());
        Map<String, Object> over = form("a1", 1, "base");
        over.put("abilityBonus", m("attack_speed", 0.3));
        assertTrue(hasError(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), over, form("xx", 1, "base"))), null), "outside the budget"));
    }

    @Test
    void tierIsTheDepthInTheTree() {
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), form("b1", 3, "a1"))), null);
        assertTrue(hasError(p, "must be 2"), p.report());
        assertFalse(has(p, "b1"));
        FormsSchema.Parsed two = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), form("a2", 2, "a1"), form("cc", 2, "base"))), null);
        assertTrue(hasError(two, "must be 1"), "tier 2 under the base is wrong: " + two.report());
        FormsSchema.Parsed deep = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), form("a2", 2, "a1"), form("a3", 3, "a2"),
                form("a4", 4, "a3"), form("a5", 5, "a4"), form("a6", 6, "a5"))), null);
        assertTrue(hasError(deep, "\"tier\" must be a whole number 0 to 5"), "depth beyond tier 5: " + deep.report());
    }

    @Test
    void parentsMustExistAndMustNotLoop() {
        FormsSchema.Parsed missing = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "ghost"), form("xx", 1, "base"))), null);
        assertTrue(hasError(missing, "'ghost' does not exist"), missing.report());
        FormsSchema.Parsed loop = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 2, "a2"), form("a2", 2, "a1"), form("xx", 1, "base"))), null);
        assertTrue(hasError(loop, "loop: a1 -> a2 -> a1") || hasError(loop, "loop: a2 -> a1 -> a2"), loop.report());
        assertFalse(has(loop, "a1"));
        assertFalse(has(loop, "a2"));
        FormsSchema.Parsed self = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "a1"), form("xx", 1, "base"))), null);
        assertFalse(has(self, "a1"), self.report());
        assertFalse(self.errors().isEmpty());
    }

    @Test
    void aNonBaseFormNeedsAParentAndOnlyBaseHasTierZero() {
        Map<String, Object> orphan = form("a1", 1);
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), orphan, form("xx", 1, "base"))), null);
        assertTrue(hasError(p, "needs at least one parent"), p.report());
        Map<String, Object> zero = form("a1", 0, "base");
        assertTrue(hasError(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), zero, form("xx", 1, "base"))), null), "only the base form has tier 0"));
    }

    @Test
    void idsMustBeUniqueAndSnakeCase() {
        FormsSchema.Parsed dup = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), form("a1", 1, "base"), form("xx", 1, "base"))), null);
        assertTrue(hasError(dup, "used twice"), dup.report());
        assertEquals(1, dup.forms().stream().filter(f -> f.id().equals("a1")).count());
        for (String bad : new String[]{"Leaf", "a", "1leaf", "leaf shield", "x".repeat(40), "Leaf-Shield"}) {
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form(bad, 1, "base"), form("xx", 1, "base"))), null);
            assertTrue(hasError(p, "snake_case"), bad + ": " + p.report());
        }
    }

    @Test
    void theBaseFormIsHeldToItsOwnRules() {
        String[][] cases = {{"tier", "1"}, {"mastery", "50"}};
        for (String[] c : cases) {
            Map<String, Object> b = base();
            b.put(c[0], Long.parseLong(c[1]));
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(b, form("xx", 1, "base"))), null);
            assertTrue(p.fileRejected(), c[0] + ": " + p.report());
        }
        Map<String, Object> withReq = base();
        withReq.put("requires", List.of(req("minecraft:leather", 8)));
        assertTrue(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(withReq, form("xx", 1, "base"))), null).fileRejected());
        Map<String, Object> asCurse = base();
        asCurse.put("curse", true);
        asCurse.put("sin", "envy");
        FormsSchema.Parsed curseBase = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(asCurse, form("xx", 1, "base"))), null);
        assertTrue(curseBase.fileRejected(), curseBase.report());
        assertTrue(hasError(curseBase, "the base form must not be a curse form"), curseBase.report());
    }

    @Test
    void aBonusOnTheBaseFormIsDroppedWithAWarningAndDoesNotCostTheFile() {
        Map<String, Object> withBonus = base();
        withBonus.put("abilityBonus", m("armor", 0.5));
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(withBonus, form("xx", 1, "base"))), null);
        assertFalse(p.fileRejected(), p.report());
        assertTrue(p.errors().isEmpty(), p.report());
        assertTrue(hasWarning(p, "the base form must not have an abilityBonus"), p.report());
        assertFalse(p.clean(), "a warning must still fail the data test");
        assertTrue(p.forms().get(0).abilityBonus().isEmpty(), "the dropped bonus must not reach the form");
        assertTrue(has(p, "xx"), "the rest of the tree must survive");
    }

    @Test
    void masteryMustBeInRange() {
        for (long bad : new long[]{0, 39, 401, 1000}) {
            Map<String, Object> f = form("a1", 1, "base");
            f.put("mastery", bad);
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
            assertTrue(hasError(p, "\"mastery\" must be 40 to 400"), bad + ": " + p.report());
        }
        Map<String, Object> edge = form("a1", 1, "base");
        edge.put("mastery", 40L);
        Map<String, Object> edge2 = form("xx", 1, "base");
        edge2.put("mastery", 400L);
        assertTrue(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), edge, edge2)), null).clean());
        Map<String, Object> none = form("a1", 1, "base");
        none.remove("mastery");
        assertTrue(hasError(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), none, form("xx", 1, "base"))), null), "\"mastery\" is required"));
    }

    // ---------------------------------------------------------------- requirements
    @Test
    void requirementRules() {
        Object[][] cases = {
                {"count below four", List.of(req("minecraft:leather", 3)), "\"count\" must be a whole number 4 to 64"},
                {"count above 64 for a tier 1 form", List.of(req("minecraft:leather", 65)), "only for a top form"},
                {"count above 256", List.of(req("minecraft:leather", 300)), "\"count\" must be a whole number"},
                {"item and tag", List.of(m("item", "minecraft:leather", "tag", "minecraft:leaves", "count", 8L)), "exactly one of"},
                {"neither", List.of(m("count", 8L)), "exactly one of"},
                {"not an object", List.of("minecraft:leather"), "must be an object"},
                {"five entries", List.of(req("minecraft:a", 4), req("minecraft:b", 4), req("minecraft:c", 4), req("minecraft:d", 4), req("minecraft:e", 4)), "at most 4"},
                {"unknown namespace", List.of(req("create:cogwheel", 8)), "items must be minecraft:, tensura: or nusmp:"},
                {"tag from tensura", List.of(tagReq("tensura:parts", 8)), "tags must be minecraft:, c: or nusmp:"},
                {"upper case", List.of(req("Minecraft:Leather", 8)), "NOPE"},
                {"unobtainable", List.of(req("minecraft:bedrock", 8)), "cannot be obtained in survival"},
                {"spawn egg", List.of(req("minecraft:zombie_spawn_egg", 8)), "cannot be obtained in survival"},
                {"duplicate", List.of(req("minecraft:leather", 8), req("minecraft:leather", 16)), "already required"},
                {"bad id", List.of(req("minecraft:lea ther", 8)), "not a valid id"},
        };
        for (Object[] c : cases) {
            Map<String, Object> f = form("a1", 1, "base");
            f.put("requires", c[1]);
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
            if (c[2].equals("NOPE")) {                      // mixed-case ids are normalised, not refused
                assertTrue(p.clean(), c[0] + ": " + p.report());
                continue;
            }
            assertTrue(hasError(p, (String) c[2]), c[0] + ": " + p.report());
            assertFalse(has(p, "a1"), (String) c[0]);
        }
    }

    @Test
    void anOrdinaryFormNeedsRequirementsButACurseFormMayAskForNothing() {
        Map<String, Object> none = form("a1", 1, "base");
        none.put("requires", List.of());
        assertTrue(hasError(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), none, form("xx", 1, "base"))), null), "needs one to 4"));
        Map<String, Object> envy = curse("c1", 1, "envy", "base");
        envy.put("look", "base");                                                   // a vassal has no cursed mesh
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("xx", 1, "base"), form("yy", 2, "xx"), envy)), null);
        assertTrue(p.errors().isEmpty(), p.report());
    }

    @Test
    void aTopFormMayAskForUpTo256() {
        Map<String, Object> top = form("c1", 3, "b1");
        top.put("requires", List.of(req("minecraft:nether_star", 4), req("minecraft:iron_ingot", 256)));
        FormsSchema.Parsed ok = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), form("b1", 2, "a1"), top)), null);
        assertTrue(ok.errors().isEmpty(), ok.report());
        assertTrue(has(ok, "c1"));
        Map<String, Object> notTop = form("b1", 2, "a1");
        notTop.put("requires", List.of(req("minecraft:iron_ingot", 100)));
        FormsSchema.Parsed bad = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), notTop)), null);
        assertTrue(hasError(bad, "only for a top form"), "tier 2 is not deep enough: " + bad.report());
        Map<String, Object> parentOfTop = form("c1", 3, "b1");
        parentOfTop.put("requires", List.of(req("minecraft:iron_ingot", 100)));
        FormsSchema.Parsed bad2 = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), form("a1", 1, "base"), form("b1", 2, "a1"), parentOfTop, form("d1", 4, "c1"))), null);
        assertTrue(hasError(bad2, "only for a top form"), "something builds on it: " + bad2.report());
    }

    // ---------------------------------------------------------------- skills
    @Test
    void skillRulesFollowTheWeaponsStyle() {
        List<Map<String, Object>> good = holyShield();
        good.get(1).put("skills", m("sneak", "shield.air_strike", "key1", "shield.prison"));
        FormsSchema.Parsed fine = FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, good), SKILLS);
        assertTrue(fine.clean(), fine.report());
        assertEquals(List.of("sneak", "key1"), List.copyOf(fine.forms().get(1).skills().keySet()));

        Object[][] cases = {
                {HeroKind.SHIELD, m("use", "shield.air_strike"), "raises the guard"},
                {HeroKind.BOW, m("use", "bow.volley"), "draws the bow"},
                {HeroKind.SHIELD, m("sneak", "sword.slash"), "is not a skill of the Legendary Shield"},
                {HeroKind.SHIELD, m("sneak", "shield.nonexistent"), "no skill 'shield.nonexistent'"},
                {HeroKind.SHIELD, m("middle", "shield.prison"), "unknown slot"},
                {HeroKind.SHIELD, m("Sneak", "shield.prison"), "lower case"},
                {HeroKind.SHIELD, m("sneak", 5L), "must be a skill id text"},
                {HeroKind.SHIELD, m("sneak", "shield.big", "key1", "shield.bigger"), "2 ultimate techniques"},
        };
        for (Object[] c : cases) {
            HeroKind k = (HeroKind) c[0];
            List<Map<String, Object>> forms = k == HeroKind.SHIELD ? holyShield() : holyBow();
            forms.get(1).put("skills", c[1]);
            FormsSchema.Parsed p = FormsSchema.parse(k, file(k, forms), SKILLS);
            assertTrue(hasError(p, (String) c[2]), c[2] + ": " + p.report());
            assertFalse(has(p, "a1"));
        }
        List<Map<String, Object>> sword = list(base(), form("a1", 1, "base"), form("a2", 1, "base"), form("b1", 2, "a1"), form("b2", 2, "a2"), form("c1", 3, "b1"), form("c2", 3, "b2"),
                form("d1", 4, "c1"), curse("greed_1", 1, "greed", "base"), curse("gluttony_1", 1, "gluttony", "base"));
        sword.get(1).put("skills", m("use", "sword.slash"));
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.SWORD, file(HeroKind.SWORD, sword), SKILLS);
        assertTrue(p.clean(), "a swinging weapon may put a technique on use: " + p.report());
    }

    private static List<Map<String, Object>> holyBow() {
        return list(base(), form("a1", 1, "base"), form("a2", 1, "base"), form("b1", 2, "a1"), form("b2", 2, "a2"), form("c1", 3, "b1"), form("c2", 3, "b2"), form("d1", 4, "c1"),
                curse("pride_1", 1, "pride", "base"));
    }

    @Test
    void withoutASkillIndexTheSkillExistenceIsNotChecked() {
        List<Map<String, Object>> forms = holyShield();
        forms.get(1).put("skills", m("sneak", "shield.invented_for_this_test"));
        assertTrue(FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, forms), null).clean(), "JUnit has no logics to ask");
        FormsSchema.Parsed asked = FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, forms), SKILLS);
        assertTrue(hasError(asked, "no skill 'shield.invented_for_this_test'"));
    }

    // ---------------------------------------------------------------- curse
    @Test
    void curseFormsFollowTheirRules() {
        Object[][] cases = {
                {"curse without a sin", "sin", null, "needs a \"sin\""},
                {"a sin the Shield does not carry", "sin", "pride", "carries [wrath] in canon, not pride"},
                {"a made-up sin", "sin", "boredom", "must be one of"},
                {"capital sin", "sin", "Wrath", "must be one of"},
                {"look base on a Holy curse form", "look", "base", "uses look \"curse\""},
                {"look invalid", "look", "shiny", "\"look\" must be"},
                {"corruption too high", "corruptionPerSecond", 101L, "0 to 100"},
                {"corruption negative", "corruptionPerSecond", -1L, "0 to 100"},
                {"curse not a boolean", "curse", "yes", "\"curse\" must be true or false"},
        };
        for (Object[] c : cases) {
            List<Map<String, Object>> forms = holyShield();
            Map<String, Object> w = forms.get(8);
            if (c[1].equals("sin") && c[2] == null) w.put("sin", null);
            else w.put((String) c[1], c[2]);
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, forms), null);
            assertTrue(hasError(p, (String) c[3]), c[0] + ": " + p.report());
            assertFalse(has(p, "wrath_1"), (String) c[0]);
            assertFalse(has(p, "wrath_2"), c[0] + ": the second curse form hangs off the first");
        }
    }

    @Test
    void anOrdinaryFormMayNotCarryCurseData() {
        for (String[] c : new String[][]{{"sin", "wrath"}, {"look", "curse"}}) {
            Map<String, Object> f = form("a1", 1, "base");
            f.put(c[0], c[1]);
            FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
            assertFalse(p.errors().isEmpty(), c[0] + ": " + p.report());
        }
        Map<String, Object> f = form("a1", 1, "base");
        f.put("corruptionPerSecond", 2L);
        assertTrue(hasError(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null), "only for curse forms"));
    }

    @Test
    void aVassalMayCarryAnySinButKeepsTheBaseLook() {
        Map<String, Object> envy = curse("envy_1", 1, "envy", "base");
        envy.put("look", "base");
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.MIRROR, file(HeroKind.MIRROR, list(base(), form("xx", 1, "base"), form("yy", 2, "xx"), envy)), null);
        assertTrue(p.clean(), p.report());
        assertEquals(Sin.ENVY, p.forms().stream().filter(f -> f.id().equals("envy_1")).findFirst().orElseThrow().sin());
        Map<String, Object> shiny = curse("envy_1", 1, "envy", "base");               // the fixture says look "curse"
        FormsSchema.Parsed bad = FormsSchema.parse(HeroKind.MIRROR, file(HeroKind.MIRROR, list(base(), form("xx", 1, "base"), form("yy", 2, "xx"), shiny)), null);
        assertTrue(hasError(bad, "keeps look \"base\""), bad.report());
        Map<String, Object> omitted = curse("envy_1", 1, "envy", "base");
        omitted.remove("look");
        assertTrue(FormsSchema.parse(HeroKind.MIRROR, file(HeroKind.MIRROR, list(base(), form("xx", 1, "base"), form("yy", 2, "xx"), omitted)), null).clean(), "look defaults by family");
        List<Map<String, Object>> holy = holyShield();
        holy.get(8).remove("look");
        assertEquals("curse", FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, holy), null).forms().stream().filter(f -> f.id().equals("wrath_1")).findFirst().orElseThrow().look());
    }

    // ---------------------------------------------------------------- warnings about the file as a whole
    @Test
    void formCountsAreWarningsNotErrors() {
        FormsSchema.Parsed few = FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, list(base(), form("a1", 1, "base"), curse("wrath_1", 1, "wrath", "base"))), null);
        assertTrue(few.errors().isEmpty(), few.report());
        assertTrue(hasWarning(few, "has 2 ordinary (non-curse) forms"), few.report());
        assertFalse(few.fileRejected(), "the file is still used");
        List<Map<String, Object>> many = list(base(), form("a1", 1, "base"), form("a2", 1, "base"), form("a3", 1, "base"), form("a4", 1, "base"), form("a5", 1, "base"), form("a6", 1, "base"));
        FormsSchema.Parsed six = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, many), null);
        assertTrue(hasWarning(six, "asks for 3 to 5"), six.report());
    }

    @Test
    void aHolyWeaponNeedsACurseFormForEveryCanonSin() {
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.SWORD, file(HeroKind.SWORD, list(base(), form("a1", 1, "base"), form("a2", 1, "base"), form("b1", 2, "a1"), form("b2", 2, "a2"),
                form("c1", 3, "b1"), form("c2", 3, "b2"), form("d1", 4, "c1"), curse("greed_1", 1, "greed", "base"))), null);
        assertTrue(p.errors().isEmpty(), p.report());
        assertTrue(hasWarning(p, "gluttony in canon"), p.report());
        assertFalse(hasWarning(p, "greed in canon"));
    }

    @Test
    void aMisspeltKeyIsReportedWithAHint() {
        Map<String, Object> f = form("a1", 1, "base");
        f.put("require", f.remove("requires"));
        f.put("_comment", "ignored");
        f.put("note", "ignored");
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
        assertTrue(hasWarning(p, "unknown key \"require\""), p.report());
        assertTrue(hasWarning(p, "did you mean \"requires\""), p.report());
        assertFalse(hasWarning(p, "_comment") || hasWarning(p, "\"note\""));
        assertTrue(hasError(p, "needs one to 4"), "the misspelt key left the form without requirements");
    }

    @Test
    void aKeyThatIsNotASlipIsIgnoredSilentlyAsTheDesignSays() {
        Map<String, Object> f = form("a1", 1, "base");
        f.put("icon", "minecraft:leather");
        f.put("lore", "Used by the weapon's own code.");
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), f, form("xx", 1, "base"))), null);
        assertTrue(p.clean(), p.report());
        Map<String, Object> g = form("a1", 1, "base");
        g.put("requirements", g.remove("requires"));
        g.put("skill", m());
        FormsSchema.Parsed q = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), g, form("xx", 1, "base"))), null);
        assertTrue(hasWarning(q, "unknown key \"requirements\"") && hasWarning(q, "did you mean \"requires\""), "a common wrong name is reported: " + q.report());
        assertTrue(hasWarning(q, "unknown key \"skill\"") && hasWarning(q, "did you mean \"skills\""), q.report());
    }

    @Test
    void theBaseFormMayLeaveOutTierAndMastery() {
        Map<String, Object> b = base();
        b.remove("tier");
        b.remove("mastery");
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(b, form("a1", 1, "base"), form("xx", 1, "base"))), null);
        assertTrue(p.clean(), p.report());
        Map<String, Object> other = form("a1", 1, "base");
        other.remove("tier");
        assertTrue(hasError(FormsSchema.parse(HeroKind.HAMMER, file(HeroKind.HAMMER, list(base(), other, form("xx", 1, "base"))), null), "\"tier\" must be a whole number"), "every other form must say its tier");
    }

    // ---------------------------------------------------------------- the table itself
    @Test
    void theBudgetTableCoversExactlyTheStatKeys() {
        assertEquals(new java.util.HashSet<>(HeroForm.STAT_KEYS), new java.util.HashSet<>(List.of("attack_damage", "attack_speed", "armor", "armor_toughness", "max_health",
                "movement_speed", "knockback_resistance", "reach", "luck")));
        for (String key : HeroForm.STAT_KEYS) {
            double[] l = FormsSchema.limits(key);
            assertNotNull(l, key + " has no budget in FormsSchema");
            assertTrue(l[0] < 0 && l[1] > 0, key);
        }
        assertNull(FormsSchema.limits("strength"));
        assertEquals("2", FormsSchema.trim(2.0));
        assertEquals("0.25", FormsSchema.trim(0.25));
    }

    @Test
    void immutableResults() {
        FormsSchema.Parsed p = FormsSchema.parse(HeroKind.SHIELD, file(HeroKind.SHIELD, holyShield()), null);
        HeroForm f = p.forms().get(1);
        try {
            f.stats().put("armor", 99.0);
            assertTrue(false, "the stats of a form must not be writable");
        } catch (UnsupportedOperationException expected) {
            // good
        }
        try {
            p.forms().add(null);
            assertTrue(false, "the form list must not be writable");
        } catch (UnsupportedOperationException expected) {
            // good
        }
    }
}
