package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.GradeTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the parser is the only thing standing between a hand-edited data file and a death event, so it must do four things every time: understand the
 * shipped file with no complaint, turn a bad value into a clamp or a skip plus ONE sentence in the log (never a crash, never silence), keep what is good
 * when something next to it is bad, and give a table that cannot drop anything (rather than a half-built one) when nothing usable is left. Each case below
 * is a mistake someone will make: a typo in a slot name, an item id with a capital letter, a range the wrong way round, a curve point past 1, a grade
 * left out, a file that is a list instead of an object.
 */
class GearTablesParserTest {

    private static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** A minimal valid grade: one sword. */
    private static Map<String, Object> gradeWith(Object... itemKv) {
        return obj("items", obj(itemKv));
    }

    private static List<String> problems(Object root) { return GearTablesParser.parse(root).problems(); }

    private static boolean mentions(List<String> problems, String what) {
        for (String p : problems) if (p.contains(what)) return true;
        return false;
    }

    @Test
    void theShippedFileParsesWithNoComplaint() {
        GearTablesParser.Result r = GearFx.shipped();
        assertTrue(r.problems().isEmpty(), "problems: " + r.problems());
        assertTrue(r.tables().isUsable());
        for (GearGrade g : GearGrade.values()) assertTrue(r.tables().offers(g), g + " must offer items");
    }

    @Test
    void aRootThatIsNotAnObjectGivesNothingToDrop() {
        GearTablesParser.Result r = GearTablesParser.parse(List.of());
        assertSame(GearTables.EMPTY, r.tables());
        assertEquals(1, r.problems().size());
        assertSame(GearTables.EMPTY, GearTablesParser.parse(null).tables());
        assertFalse(GearTables.EMPTY.isUsable());
    }

    @Test
    void anUnknownKeyIsReportedNotThrown() {
        Object root = obj("grades", obj("normal", gradeWith("sword", List.of("minecraft:wooden_sword"))), "bogus", 1.0);
        assertTrue(mentions(problems(root), "unknown key 'bogus'"));
        Map<String, Object> g = gradeWith("sword", List.of("minecraft:wooden_sword"));
        g.put("colour", "red");
        assertTrue(mentions(problems(obj("grades", obj("normal", g))), "unknown key 'colour'"));
    }

    @Test
    void aWhyKeyIsDocumentationEverywhere() {
        Object root = obj("why", "x", "curve", obj("why", "y", "normal", List.of(List.of(0.0, 1.0))), "caps", obj("why", "z", "total", 0.4),
                "slotWeights", obj("why", "w", "sword", 2.0),
                "grades", obj("why", "g", "normal", obj("why", "n", "items", obj("why", "i", "sword", List.of("minecraft:wooden_sword")),
                        "enchants", obj("why", "e", "pool", obj("why", "p")), "engravings", obj("why", "e"), "resist", obj("why", "r"))));
        List<String> p = problems(root);
        for (String s : p) assertFalse(s.contains("why"), "a why key was complained about: " + s);
        assertEquals(0.4, GearTablesParser.parse(root).tables().totalCap(), 1e-12);
    }

    @Test
    void anItemIdThatIsNotAnIdIsDroppedAndTheRestKept() {
        Object root = obj("grades", obj("normal", gradeWith("sword", List.of("minecraft:wooden_sword", "Minecraft:Stone_Sword", "nonsense", 5.0))));
        GearTablesParser.Result r = GearTablesParser.parse(root);
        assertEquals(List.of("minecraft:wooden_sword"), r.tables().grade(GearGrade.NORMAL).items(GearSlot.SWORD));
        assertEquals(3, r.problems().stream().filter(s -> s.contains("is not an item id")).count());
    }

    @Test
    void anUnknownSlotOrGradeIsReportedAndSkipped() {
        Object root = obj("grades", obj("normal", gradeWith("sword", List.of("minecraft:wooden_sword"), "spear", List.of("minecraft:trident")),
                "mythic", gradeWith("sword", List.of("minecraft:netherite_sword"))),
                "slotWeights", obj("sword", 1.0, "wand", 1.0), "curve", obj("legend", List.of(List.of(0.0, 1.0))), "caps", obj("legend", 0.1));
        GearTablesParser.Result r = GearTablesParser.parse(root);
        assertTrue(mentions(r.problems(), "unknown slot 'spear'"));
        assertTrue(mentions(r.problems(), "unknown grade 'mythic'"));
        assertTrue(mentions(r.problems(), "unknown slot 'wand'"));
        assertTrue(mentions(r.problems(), "curve: unknown grade 'legend'"));
        assertTrue(mentions(r.problems(), "caps: unknown grade 'legend'"));
        assertTrue(r.tables().offers(GearGrade.NORMAL));
        assertFalse(r.tables().offers(GearGrade.RARE));
    }

    @Test
    void numbersAreClampedAndReported() {
        Map<String, Object> g = gradeWith("sword", List.of("minecraft:wooden_sword"));
        g.put("power", List.of(-5.0, 500.0));
        g.put("resist", obj("armour", List.of(0.0, 250.0), "hand", 7.0));
        g.put("enchants", obj("count", List.of(0.0, 99.0), "levelCap", 40.0));
        Object root = obj("grades", obj("normal", g), "caps", obj("total", 9.0, "normal", -1.0), "slotWeights", obj("sword", 99999.0),
                "curve", obj("normal", List.of(List.of(-1.0, 500.0))));
        GearTablesParser.Result r = GearTablesParser.parse(root);
        GradeTable t = r.tables().grade(GearGrade.NORMAL);
        assertEquals(0, t.power().min());
        assertEquals(200, t.power().max());
        assertEquals(100, t.resistArmour().max());
        assertEquals(7, t.resistHand().min());
        assertEquals(7, t.resistHand().max(), "a bare number is [n, n]");
        assertEquals(6, t.enchantCount().max());
        assertEquals(10, t.enchantLevelCap());
        assertEquals(1.0, r.tables().totalCap(), 0.0);
        assertEquals(0.0, r.tables().maxChance(GearGrade.NORMAL), 0.0);
        assertEquals(1000.0, r.tables().slotWeight(GearSlot.SWORD), 0.0);
        assertEquals(100.0, r.tables().curve(GearGrade.NORMAL).at(0.5), 0.0);
        assertTrue(mentions(r.problems(), "clamped"));
    }

    @Test
    void aReversedRangeIsPutTheRightWayRound() {
        Map<String, Object> g = gradeWith("sword", List.of("minecraft:wooden_sword"));
        g.put("power", List.of(20.0, 10.0));
        GradeTable t = GearTablesParser.parse(obj("grades", obj("normal", g))).tables().grade(GearGrade.NORMAL);
        assertEquals(10, t.power().min());
        assertEquals(20, t.power().max());
    }

    @Test
    void aCurveIsSortedAndInterpolated() {
        Object root = obj("curve", obj("rare", List.of(List.of(1.0, 5.0), List.of(0.0, 1.0), List.of(0.5, 3.0))),
                "grades", obj("rare", gradeWith("sword", List.of("minecraft:iron_sword"))));
        GearTables.Curve c = GearTablesParser.parse(root).tables().curve(GearGrade.RARE);
        assertEquals(3, c.size());
        assertEquals(1.0, c.at(0.0), 1e-12);
        assertEquals(2.0, c.at(0.25), 1e-12);
        assertEquals(3.0, c.at(0.5), 1e-12);
        assertEquals(4.0, c.at(0.75), 1e-12);
        assertEquals(5.0, c.at(1.0), 1e-12);
        assertEquals(5.0, c.at(7.0), 1e-12, "past the last point the last value holds");
        assertEquals(1.0, c.at(-3.0), 1e-12, "before the first point the first value holds");
        assertEquals(1.0, GearTables.Curve.FLAT.at(0.3), 0.0);
    }

    @Test
    void aBadCurveIsReportedAndLeftFlat() {
        Object root = obj("curve", obj("normal", "oops", "special", List.of(List.of(0.0)), "rare", List.of()), "grades", obj());
        GearTablesParser.Result r = GearTablesParser.parse(root);
        assertEquals(1.0, r.tables().curve(GearGrade.NORMAL).at(0.9), 0.0);
        assertEquals(1.0, r.tables().curve(GearGrade.SPECIAL).at(0.9), 0.0);
        assertTrue(r.problems().size() >= 3);
    }

    @Test
    void aMissingGradeOrSlotIsReported() {
        Object root = obj("grades", obj("normal", gradeWith("sword", List.of("minecraft:wooden_sword"))));
        List<String> p = problems(root);
        assertTrue(mentions(p, "grade special has no table"));
        assertTrue(mentions(p, "grade rare has no table"));
        assertTrue(mentions(p, "grade normal has no base item for axe"));
        assertNull(GearTablesParser.parse(obj("grades", obj())).tables().grade(GearGrade.NORMAL));
        assertTrue(mentions(problems(obj("grades", obj())), "nothing will drop"));
    }

    @Test
    void poolsAreReadWithTheirLevels() {
        Map<String, Object> g = gradeWith("sword", List.of("minecraft:iron_sword"));
        g.put("engravings", obj("count", List.of(1.0, 2.0), "pool", obj("sword", List.of(obj("id", "tensura:sturdy", "min", 1.0, "max", 3.0), obj("id", "Bad Id"), "nope"))));
        GearTablesParser.Result r = GearTablesParser.parse(obj("grades", obj("rare", g)));
        List<GearTables.Pool> pool = r.tables().grade(GearGrade.RARE).engravePool(GearSlot.SWORD);
        assertEquals(1, pool.size());
        assertEquals(new GearTables.Pool("tensura:sturdy", 1, 3), pool.get(0));
        assertEquals(2, r.problems().stream().filter(s -> s.contains("engravings.pool")).count());
        assertEquals(1, r.tables().grade(GearGrade.RARE).engraveCount().min());
    }

    @Test
    void theSummaryNamesTheItemCounts() {
        String s = GearTablesParser.summary(GearFx.tables());
        assertTrue(s.contains("normal=") && s.contains("special=") && s.contains("rare="), s);
        assertTrue(GearTablesParser.summary(GearTables.EMPTY).contains("normal=0 items"));
    }
}
