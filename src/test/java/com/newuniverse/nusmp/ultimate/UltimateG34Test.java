package com.newuniverse.nusmp.ultimate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: groups 3 and 4 of the Ultimates (UltimateDefs3 = rows 49 to 72, UltimateDefs4 = rows 73 to 96 of the proposal table in docs/magic/ultimate_audit.md, in table order) are 48 rows of data written in one go,
 * so a typo would be easy to make and hard to see. This pins them against the REAL sources: the rows are exactly the table's rows in the table's order (or are listed as skipped, with a reason, in
 * docs/magic/ultimate_g34.md), every book is a registered grimoire that no other Ultimate uses, nothing is a Time shape or Time book, every shape is a real {@code VfxShape} constant and every potion effect a
 * vanilla one, every row is inside the bounds, no two rows are copies of each other, the worst case of effect packets of a cast holds, and the lang fragment, the armour-policy file and the doc cover every
 * page. Pure (file reads, Gson); the working directory is the project root.
 */
class UltimateG34Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static List<UltimateDef> mine() {
        List<UltimateDef> l = new ArrayList<>(UltimateDefs3.all());
        l.addAll(UltimateDefs4.all());
        return l;
    }

    /** The MagicType names of rows 49 to 96 of the proposal table (the audit lists one row per attribute, Time excluded). */
    private static List<String> tableRows() throws IOException {
        List<String> all = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^\\| `([A-Z_]+)` \\|").matcher(read(ROOT.resolve("docs/magic/ultimate_audit.md")).substring(read(ROOT.resolve("docs/magic/ultimate_audit.md")).indexOf("## 4.")));
        while (m.find()) all.add(m.group(1));
        assertEquals(96, all.size(), "the proposal table has 96 rows");
        return all.subList(48, 96);
    }

    private static String bookOf(String magic) { return "book_" + magic.toLowerCase(java.util.Locale.ROOT); }

    @Test
    void theRowsAreTheLast48RowsOfTheProposalTableInTableOrder() throws IOException {
        List<String> rows = tableRows();
        String doc = read(ROOT.resolve("docs/magic/ultimate_g34.md"));
        Set<String> skipped = new HashSet<>();
        Matcher sk = Pattern.compile("(?m)^\\| SKIPPED \\| `([A-Z_]+)` \\|").matcher(doc);
        while (sk.find()) skipped.add(sk.group(1));
        List<String> expect = new ArrayList<>();
        for (String r : rows) if (!skipped.contains(r)) expect.add(r);
        List<String> built = new ArrayList<>();
        for (UltimateDef d : mine()) built.add(d.magic());
        assertEquals(expect, built, "UltimateDefs3 + UltimateDefs4 hold the rows 49 to 96 in table order (a skipped row is listed as 'SKIPPED' in docs/magic/ultimate_g34.md)");
        assertEquals(rows.subList(0, 24).stream().filter(r -> !skipped.contains(r)).count(), UltimateDefs3.all().size(), "rows 49 to 72 are in UltimateDefs3");
        assertEquals(rows.subList(24, 48).stream().filter(r -> !skipped.contains(r)).count(), UltimateDefs4.all().size(), "rows 73 to 96 are in UltimateDefs4");
        for (UltimateDef d : mine()) assertEquals(bookOf(d.magic()), d.bookPath(), d.id());
    }

    @Test
    void theTableIsSoundAndNoBookOrIdIsUsedTwiceAcrossAllGroups() {
        assertEquals(List.of(), UltimateRegistry.problems());
        Set<String> books = new HashSet<>(), ids = new HashSet<>();
        for (UltimateDef d : mine()) {
            assertTrue(books.add(d.bookPath()), d.bookPath());
            assertTrue(ids.add(d.id()), d.id());
            assertSame(d, UltimateRegistry.byId(d.id()));
            assertSame(d, UltimateRegistry.forBook(d.bookPath()));
        }
        assertEquals(48 - skippedCount(), books.size());
    }

    private static int skippedCount() {
        try {
            Matcher sk = Pattern.compile("(?m)^\\| SKIPPED \\|").matcher(read(ROOT.resolve("docs/magic/ultimate_g34.md")));
            int n = 0;
            while (sk.find()) n++;
            return n;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertSame(Object a, Object b) { assertTrue(a == b); }

    @Test
    void everyBookIsARegisteredGrimoireAndNeverTheTimeBook() throws IOException {
        String skills = read(SRC.resolve("skill/NUSkills.java"));
        String magics = read(SRC.resolve("blackclover/MagicType.java"));
        String wiki = read(SRC.resolve("book/WikiBooks.java"));
        wiki = wiki.substring(wiki.indexOf("NEW_ATTRIBUTES = EnumSet.of("), wiki.indexOf("public static Set<MagicType> newAttributes"));
        for (UltimateDef d : mine()) {
            assertTrue(skills.contains("book(\"" + d.bookPath() + "\"") || Pattern.compile("MagicType\\." + d.magic() + "\\b").matcher(wiki).find(), d.bookPath() + " is not a registered book");
            assertTrue(Pattern.compile("(?m)^\\s{4}" + d.magic() + "\\(\"").matcher(magics).find(), d.magic() + " is not a MagicType");
            assertFalse(d.magic().equals("TIME") || d.bookPath().equals("book_time"), "no Time Ultimate (owner rule 3)");
        }
    }

    @Test
    void everyShapeIsARealVfxShapeAndNeverATimeShape() throws IOException {
        String shapes = read(SRC.resolve("vfx/VfxShape.java"));
        shapes = shapes.substring(0, shapes.indexOf("public static VfxShape byId")).replaceAll("//[^\\n]*", "");
        for (UltimateDef d : mine())
            for (String s : List.of(d.chargeShape(), d.burstShape(), d.echoShape())) {
                assertTrue(Pattern.compile("(^|[\\s,])" + s + "[\\s,;(]").matcher(shapes).find(), d.id() + ": no VfxShape " + s);
                assertFalse(s.startsWith("TIME_"), d.id() + " reuses a Time Magic shape: " + s);
            }
    }

    @Test
    void everyPotionEffectIsVanillaAndTheRowsAreInsideTheBounds() {
        Set<String> vanilla = Set.of("minecraft:slowness", "minecraft:weakness", "minecraft:mining_fatigue", "minecraft:hunger", "minecraft:nausea", "minecraft:levitation",
                "minecraft:blindness", "minecraft:glowing", "minecraft:wither", "minecraft:darkness", "minecraft:poison", "minecraft:unluck");
        for (UltimateDef d : mine()) {
            assertEquals(null, d.problem());
            assertTrue(d.debuffs().size() >= 3 && d.debuffs().size() <= 4, d.id() + ": heavy debuffs, at most four");
            Set<String> seen = new HashSet<>();
            for (UltimateDef.Debuff db : d.debuffs()) {
                assertTrue(vanilla.contains(db.effect()), d.id() + ": " + db.effect());
                assertTrue(seen.add(db.effect()), d.id() + ": " + db.effect() + " twice");
            }
            assertTrue(d.windupTicks() >= 100 && d.windupTicks() <= 400 && d.radius() >= 6 && d.radius() <= 24 && d.damage() >= 10 && d.damage() <= 80, d.id());
            assertTrue(UltimateRules.windupTicks(d.windupTicks(), 2.0) <= 400 && UltimateRules.radius(d.radius(), 2.0) <= 24, d.id());
            assertTrue(UltimateRules.pulseRaw(d.damage(), 2.0, 0, 0.0, d.radius()) <= UltimateRules.MAX_RAW, d.id());
            assertTrue(UltimateRules.packetsFor(UltimateRules.windupTicks(d.windupTicks(), 1.0)) <= UltimateRules.MAX_EFFECT_PACKETS, d.id() + ": too many effect packets");
            assertTrue(UltimateRules.packetsFor(UltimateRules.windupTicks(d.windupTicks(), 2.0)) <= UltimateRules.MAX_EFFECT_PACKETS, d.id() + ": too many effect packets at the longest wind-up");
            assertEquals("original", d.origin(), d.id() + ": no canon Ultimate of these attributes was found (see the doc)");
        }
    }

    @Test
    void noTwoRowsAreCopiesOfEachOther() {
        Set<String> feels = new HashSet<>(), shapesTriples = new HashSet<>();
        List<UltimateDef> all = new ArrayList<>(UltimateRegistry.all());
        for (UltimateDef d : all) {
            StringBuilder b = new StringBuilder();
            for (UltimateDef.Debuff db : d.debuffs()) b.append(db.effect()).append(':').append(db.ticks()).append(':').append(db.amplifier()).append(',');
            String feel = b + "|" + d.push() + "|" + d.lift() + "|" + d.igniteSeconds() + "|" + d.freezeTicks() + "|" + d.radius() + "|" + d.windupTicks();
            assertTrue(feels.add(feel), d.id() + " has the same debuffs, push, lift, fire, freeze, radius and wind-up as another Ultimate");
            assertTrue(shapesTriples.add(d.chargeShape() + "/" + d.burstShape() + "/" + d.echoShape() + "/" + d.bookPath().length()), d.id() + " draws the same three pictures as another");
        }
        // a spread of feels, not 48 variations of one: at least 10 pulls (negative push), 10 throws (push over 0.5), 5 lifts, 3 fire and 2 freeze Ultimates among mine
        long pull = mine().stream().filter(d -> d.push() < -0.3).count(), throwing = mine().stream().filter(d -> d.push() > 0.5).count();
        long lift = mine().stream().filter(d -> d.lift() >= 0.3).count(), fire = mine().stream().filter(d -> d.igniteSeconds() > 0).count(), ice = mine().stream().filter(d -> d.freezeTicks() > 0).count();
        assertTrue(pull >= 10 && throwing >= 10 && lift >= 5 && fire >= 3 && ice >= 2, "pull " + pull + " throw " + throwing + " lift " + lift + " fire " + fire + " freeze " + ice);
        Set<Integer> radii = new HashSet<>(), windups = new HashSet<>();
        for (UltimateDef d : mine()) {
            radii.add((int) d.radius());
            windups.add(d.windupTicks());
        }
        assertTrue(radii.size() >= 10 && windups.size() >= 8, "radii " + radii.size() + " wind-ups " + windups.size());
    }

    @Test
    void theLangFragmentTheArmourPolicyFileAndTheDocCoverEveryPage() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/ultimate_g34.json"))).getAsJsonObject();
        JsonObject pol = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/ultimate_g34_new_pages.json"))).getAsJsonObject();
        String doc = read(ROOT.resolve("docs/magic/ultimate_g34.md"));
        assertEquals(mine().size(), lang.size(), "the fragment holds one text per page and nothing else");
        assertEquals(mine().size(), pol.size());
        Set<String> listed = new HashSet<>();
        for (UltimateDef d : mine()) {
            assertTrue(lang.has(d.langKey()), "no lang text " + d.langKey());
            String t = lang.get(d.langKey()).getAsString();
            assertTrue(t.startsWith(d.name() + " - ") && t.length() >= 120 && t.contains("(original)") && t.contains("cooldown") && t.contains("ALL"), d.langKey() + ": " + t);
            assertTrue(t.contains(((int) d.radius()) + "-block"), d.langKey() + " names its radius");
            JsonArray a = pol.getAsJsonArray(d.magic());
            assertTrue(a != null && a.size() == 1 && a.get(0).getAsString().equals(d.id()), d.magic() + " in the armour-policy file");
            assertTrue(listed.add(d.id()));
            assertTrue(doc.contains("| `" + d.bookPath() + "` | `" + d.id() + "` | " + d.name() + " | original |"), "docs/magic/ultimate_g34.md has no row for " + d.id());
        }
        for (var e : lang.entrySet()) assertTrue(e.getKey().startsWith("nusmp.page.book_"), e.getKey());
        // no other file of the mod lists these ids as a page (they are appended by UltimatePages, never an armour)
        String policy = read(ROOT.resolve("src/main/resources/data/nusmp/armour_policy.json"));
        for (String id : listed) assertFalse(policy.contains(id), "the armour policy names " + id + ": an Ultimate never equips an armour");
        assertFalse(doc.contains("TIME_"), "no Time Magic in the doc's pictures");
    }

    @Test
    void theGroupFilesTouchNoClientClassAndNoTimeMagic() throws IOException {
        for (String f : List.of("UltimateDefs3.java", "UltimateDefs4.java")) {
            String s = read(SRC.resolve("ultimate/" + f));
            assertFalse(s.contains("net.minecraft") || s.contains("com.mojang") || s.contains("nusmp.client") || s.contains("Minecraft.getInstance"), f + " is pure data");
            assertFalse(s.contains("TIME_") || s.contains("book_time") || s.contains("\"TIME\""), f + " has no Time Magic");
        }
    }
}
