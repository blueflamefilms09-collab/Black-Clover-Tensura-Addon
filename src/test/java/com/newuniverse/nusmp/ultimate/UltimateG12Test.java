package com.newuniverse.nusmp.ultimate;

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
 * Why: the Ultimates of rows 1 to 48 of the proposal table of docs/magic/ultimate_audit.md (groups 1 and 2, files {@code UltimateDefs1} and {@code UltimateDefs2}, built by the g12 builder) are plain data rows,
 * so what could go wrong is data: a repeated id or book, a book that is not registered, a Time Magic shape, a lang text or a new-pages entry that is missing, a row that is a copy of another, a number past the
 * framework's clamps. This test reads the REAL sources (table order from the audit, books from NUSkills, shapes from VfxShape, texts from the lang fragment) and pins all of it. Pure (file reads, Gson); the working
 * directory is the project root. The generic checks over the whole registry stay in {@link UltimateRegistryTest}.
 */
class UltimateG12Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    /** Rows 1 to 48 of the proposal table, in table order. */
    private static final List<UltimateDef> G12 = rows();

    private static List<UltimateDef> rows() {
        List<UltimateDef> l = new ArrayList<>(UltimateDefs1.all());
        l.addAll(UltimateDefs2.all());
        return l;
    }

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static final Set<String> VANILLA = Set.of("minecraft:slowness", "minecraft:weakness", "minecraft:mining_fatigue", "minecraft:hunger", "minecraft:nausea", "minecraft:levitation",
            "minecraft:blindness", "minecraft:glowing", "minecraft:wither", "minecraft:darkness", "minecraft:poison", "minecraft:unluck");

    @Test
    void groupOneAndTwoHaveTwentyFourRowsEachInTableOrder() throws IOException {
        assertEquals(24, UltimateDefs1.all().size());
        assertEquals(24, UltimateDefs2.all().size());
        String audit = read(ROOT.resolve("docs/magic/ultimate_audit.md"));
        Matcher m = Pattern.compile("(?m)^\\| `([A-Z_]+)` \\| [^|]+ \\| Ultimate").matcher(audit.substring(audit.indexOf("## 4.")));
        List<String> table = new ArrayList<>();
        while (m.find() && table.size() < 48) table.add(m.group(1));
        List<String> got = new ArrayList<>();
        for (UltimateDef d : G12) got.add(d.magic());
        assertEquals(table, got, "rows 1 to 48 of the proposal table, in order");
        assertFalse(got.contains("TIME"), "no Time Ultimate (owner rule 3)");
    }

    @Test
    void everyRowIsSoundAndOriginal() {
        Set<String> ids = new HashSet<>(), books = new HashSet<>();
        for (UltimateDef d : G12) {
            assertEquals(null, d.problem(), d.id());
            assertTrue(ids.add(d.id()), "id twice: " + d.id());
            assertTrue(books.add(d.bookPath()), "book twice: " + d.bookPath());
            assertNotCore(d);
            assertEquals("original", d.origin(), d.id() + ": the canon Ultimates of Sword and Plant already exist as pages, so every g12 row is original");
            assertTrue(d.name().startsWith("Ultimate "), d.name());
            assertSame(d, UltimateRegistry.byId(d.id()));
            assertSame(d, UltimateRegistry.forBook(d.bookPath()));
        }
        assertEquals(48, ids.size());
    }

    private static void assertNotCore(UltimateDef d) {
        for (String b : List.of("book_fire", "book_water", "book_wind", "book_earth", "book_lightning", "book_light", "book_dark", "book_spatial", "book_time"))
            assertFalse(d.bookPath().equals(b), d.id() + " sits on " + b);
    }

    private static void assertSame(Object a, Object b) { assertTrue(a == b); }

    @Test
    void everyBookIsRegisteredAndTheMagicIsARealMagicType() throws IOException {
        String skills = read(SRC.resolve("skill/NUSkills.java")), magics = read(SRC.resolve("blackclover/MagicType.java"));
        for (UltimateDef d : G12) {
            assertTrue(skills.contains("book(\"" + d.bookPath() + "\""), d.bookPath() + " is not a registered book");
            assertTrue(Pattern.compile("(?m)^\\s{4}" + d.magic() + "\\(\"").matcher(magics).find(), d.magic() + " is not a MagicType");
            assertEquals("book_" + d.magic().toLowerCase(java.util.Locale.ROOT), d.bookPath(), "the book path is book_<magic>");
            assertEquals("ultimate_" + d.magic().toLowerCase(java.util.Locale.ROOT).replace("_", ""), d.id(), "the id is ultimate_<magic without underscores>");
        }
    }

    @Test
    void everyShapeIsARealVfxShapeAndNeverATimeShape() throws IOException {
        String shapes = read(SRC.resolve("vfx/VfxShape.java"));
        for (UltimateDef d : G12)
            for (String s : List.of(d.chargeShape(), d.burstShape(), d.echoShape())) {
                assertTrue(Pattern.compile("(?m)(?:^\\s*|,\\s*)" + s + "\\s*(?:,|;|\\(|//|$)").matcher(shapes).find(), d.id() + ": no VfxShape " + s);
                assertFalse(s.startsWith("TIME_"), d.id() + " reuses a Time Magic shape: " + s);
            }
        // the three pictures of a row are not the same shape three times (a distinct charge, release and echo), except the zone families that repeat their burst
        for (UltimateDef d : G12) assertFalse(d.chargeShape().equals(d.burstShape()) && d.burstShape().equals(d.echoShape()), d.id() + ": one shape three times");
    }

    @Test
    void numbersStayInsideTheFrameworkClampsEvenAtTheLargestScale() {
        for (UltimateDef d : G12) {
            assertTrue(d.debuffs().size() >= 3 && d.debuffs().size() <= 4, d.id());
            for (UltimateDef.Debuff b : d.debuffs()) {
                assertTrue(VANILLA.contains(b.effect()), d.id() + ": " + b.effect());
                assertTrue(b.ticks() > 0 && b.ticks() <= UltimateRules.MAX_DEBUFF_TICKS && b.amplifier() >= 0 && b.amplifier() <= UltimateRules.MAX_DEBUFF_AMPLIFIER, d.id());
            }
            assertTrue(UltimateRules.windupTicks(d.windupTicks(), 2.0) <= 400 && UltimateRules.windupTicks(d.windupTicks(), 0.25) >= 100, d.id());
            assertTrue(UltimateRules.radius(d.radius(), 2.0) <= 24, d.id());
            assertTrue(UltimateRules.pulseRaw(d.damage(), 2.0, 0, 0.0, d.radius()) <= UltimateRules.MAX_RAW, d.id());
            assertTrue(UltimateRules.packetsFor(UltimateRules.windupTicks(d.windupTicks(), 1.0)) <= UltimateRules.MAX_EFFECT_PACKETS, d.id() + ": packets");
            assertTrue(d.igniteSeconds() <= 15 && d.freezeTicks() <= 300 && Math.abs(d.push()) <= 2.5 && d.lift() <= 1.0, d.id());
            // the picture powers stay under the 24 block cap of UltimateDef.problem (the manager clamps the charge picture again)
            assertTrue(d.chargePower() <= 24f && d.burstPower() <= 24f && d.echoPower() <= 24f, d.id());
        }
    }

    @Test
    void noRowIsACopyOfAnotherAndTheGroupHasVariety() {
        Set<String> sig = new HashSet<>(), shapes = new HashSet<>(), names = new HashSet<>();
        Set<Double> pushes = new HashSet<>();
        Set<Integer> windups = new HashSet<>();
        Set<String> debuffSets = new HashSet<>();
        for (UltimateDef d : G12) {
            StringBuilder deb = new StringBuilder();
            for (UltimateDef.Debuff b : d.debuffs()) deb.append(b.effect()).append(':').append(b.ticks()).append(':').append(b.amplifier()).append(',');
            assertTrue(sig.add(deb + "|" + d.windupTicks() + "|" + d.radius() + "|" + d.push() + "|" + d.lift()), d.id() + " is a copy of another row");
            assertTrue(shapes.add(d.chargeShape() + "|" + d.burstShape() + "|" + d.echoShape()), d.id() + ": the same three pictures as another row");
            assertTrue(names.add(d.name()), d.id());
            pushes.add(d.push());
            windups.add(d.windupTicks());
            debuffSets.add(deb.toString());
        }
        assertTrue(debuffSets.size() >= 40, "at least 40 different debuff mixes: " + debuffSets.size());
        assertTrue(pushes.size() >= 8, "different push / pull strengths: " + pushes.size());
        assertTrue(windups.size() >= 10, "different wind-ups: " + windups.size());
        boolean pull = false, push = false, lift = false, fire = false, freeze = false;
        for (UltimateDef d : G12) {
            pull |= d.push() < 0;
            push |= d.push() > 0.5;
            lift |= d.lift() > 0.2;
            fire |= d.igniteSeconds() > 0;
            freeze |= d.freezeTicks() > 0;
        }
        assertTrue(pull && push && lift && fire && freeze, "the group has pulls, pushes, lifts, fire and freezing");
    }

    @Test
    void theLangFragmentAndTheNewPagesFileCoverEveryRowAndOnlyTheseRows() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/ultimate_g12.json"))).getAsJsonObject();
        JsonObject core = JsonParser.parseString(read(ROOT.resolve("tools/lang/ultimate.json"))).getAsJsonObject();
        assertEquals(48, lang.size());
        for (UltimateDef d : G12) {
            assertTrue(lang.has(d.langKey()), "no lang text " + d.langKey());
            assertFalse(core.has(d.langKey()), "ultimate.json must not be edited: " + d.langKey());
            String t = lang.get(d.langKey()).getAsString();
            assertTrue(t.startsWith(d.name() + " - ") && t.length() >= 120 && t.contains("(original)") && t.contains("cooldown") && t.contains("ALL") && t.contains(((int) d.radius()) + "-block"), d.langKey() + ": " + t);
        }
        for (var e : lang.entrySet()) assertTrue(e.getKey().startsWith("nusmp.page.book_"), e.getKey());
        JsonObject pol = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/ultimate_g12_new_pages.json"))).getAsJsonObject();
        assertEquals(48, pol.size());
        for (UltimateDef d : G12) {
            assertEquals(1, pol.getAsJsonArray(d.magic()).size(), d.magic());
            assertEquals(d.id(), pol.getAsJsonArray(d.magic()).get(0).getAsString());
        }
        String merge = read(ROOT.resolve("tools/lang/merge_ultimate.py"));
        assertTrue(merge.contains("tools/lang/ultimate*.json"), "the merge script must read every ultimate*.json fragment");
    }

    @Test
    void theDocNamesEveryRowAndEverySkippedRowHasAReason() throws IOException {
        String doc = read(ROOT.resolve("docs/magic/ultimate_g12.md"));
        for (UltimateDef d : G12)
            assertTrue(doc.contains("| `" + d.bookPath() + "` | `" + d.id() + "` | " + d.name() + " | " + d.origin()), "no doc row for " + d.id());
        assertTrue(doc.contains("Skipped rows"), "the doc states which rows were skipped (none) and why");
        assertFalse(doc.contains("TIME_"), "no Time shape in the doc");
        assertTrue(doc.contains("Not verified"), "the doc says what was not seen in a game");
    }

    @Test
    void noIdIsUsedByAnotherPageOfTheMod() throws IOException {
        Set<String> mine = new HashSet<>();
        for (UltimateDef d : G12) mine.add(d.id());
        for (String line : Files.readAllLines(ROOT.resolve("src/test/resources/armour_policy/page_ids_1_98.txt"), StandardCharsets.UTF_8))
            for (String id : mine) assertFalse((" " + line + " ").contains(" " + id + " "), "the 1.98.0 snapshot has the page " + id);
    }
}
