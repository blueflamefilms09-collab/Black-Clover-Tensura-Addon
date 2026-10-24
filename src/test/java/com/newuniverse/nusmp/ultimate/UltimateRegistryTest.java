package com.newuniverse.nusmp.ultimate;

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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the table of Ultimates is the data the whole feature runs on, so it is checked against the REAL sources: every row is within its bounds, the eight flagship books each have exactly one, every effect shape
 * is a real {@code VfxShape} and none is a Time Magic shape (owner rule 3), every potion effect id is a vanilla id, every book is a real registered book, and no Ultimate id collides with a page id that already
 * exists anywhere in the mod (an old page id is a save key). Pure (regular expressions over the sources); the working directory is the project root.
 */
class UltimateRegistryTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static final Set<String> FLAGSHIP = Set.of("book_fire", "book_water", "book_wind", "book_earth", "book_lightning", "book_light", "book_dark", "book_spatial");

    @Test
    void theTableIsSound() {
        assertEquals(List.of(), UltimateRegistry.problems());
    }

    @Test
    void theEightFlagshipBooksEachHaveExactlyOneAndNothingElseYet() {
        Set<String> books = new HashSet<>();
        for (UltimateDef d : UltimateRegistry.all()) assertTrue(books.add(d.bookPath()), "two Ultimates for " + d.bookPath());
        assertTrue(books.containsAll(FLAGSHIP), "the eight flagship books keep their Ultimate (the groups UltimateDefs1..4 add more)");
        for (String b : FLAGSHIP) assertNotNull(UltimateRegistry.forBook(b), b);
        assertNull(UltimateRegistry.forBook("book_time"), "there is no Time Ultimate (owner rule 3: the Time Magic is not touched)");
        assertNull(UltimateRegistry.forBook(null));
        assertNull(UltimateRegistry.forBook("book_nothing"));
    }

    @Test
    void idsAreReadableAndDerivedFromTheElement() {
        for (UltimateDef d : UltimateRegistry.all()) {
            assertTrue(d.id().matches("ultimate_[a-z]+"), d.id());
            assertSame(d, UltimateRegistry.byId(d.id()));
            assertTrue(UltimateRegistry.isUltimatePage(d.id()));
            assertEquals("nusmp.page." + d.bookPath() + "." + d.id(), d.langKey());
        }
        assertFalse(UltimateRegistry.isUltimatePage("fire_ball"));
        assertFalse(UltimateRegistry.isUltimatePage(null));
    }

    private static void assertSame(Object a, Object b) { assertTrue(a == b); }

    @Test
    void everyOriginIsMarkedAndTheCanonOnesAreNotInventedHere() {
        int canon = 0;
        for (UltimateDef d : UltimateRegistry.all()) {
            assertTrue(d.origin().equals("original") || d.origin().startsWith("canon: "), d.id() + ": " + d.origin());
            if (d.canon()) canon++;
        }
        // Black Clover's named Ultimate Magics (Excelitius Leonum, Lightning Battle Fiend, Apocalypra Astrauza) already exist as character pages; the flagship Ultimates are original and say so
        assertEquals(0, canon);
    }

    @Test
    void everyRowIsInsideTheBounds() {
        for (UltimateDef d : UltimateRegistry.all()) {
            assertTrue(d.windupTicks() >= 100 && d.windupTicks() <= 400, d.id());
            assertTrue(d.radius() >= 6 && d.radius() <= 24, d.id());
            assertTrue(d.damage() >= 10 && d.damage() <= 80, d.id());
            assertTrue(d.debuffs().size() >= 3 && d.debuffs().size() <= 4, d.id() + ": heavy debuffs, at most four");
            assertTrue(Math.abs(d.push()) <= 2.5 && d.lift() >= 0 && d.lift() <= 1.0, d.id());
            // after the global scale the release is still bounded
            assertTrue(UltimateRules.windupTicks(d.windupTicks(), 2.0) <= 400 && UltimateRules.radius(d.radius(), 2.0) <= 24);
        }
    }

    @Test
    void everyShapeIsARealVfxShapeAndNeverATimeShape() throws IOException {
        String shapes = Files.readString(SRC.resolve("vfx/VfxShape.java"), StandardCharsets.UTF_8);
        shapes = shapes.substring(0, shapes.indexOf("public static VfxShape byId")).replaceAll("//[^\\n]*", "");      // the enum body only, comments out (several constants may share a line)
        for (UltimateDef d : UltimateRegistry.all())
            for (String s : List.of(d.chargeShape(), d.burstShape(), d.echoShape())) {
                assertTrue(Pattern.compile("(?m)(^|[\\s,])" + s + "[\\s,;(]").matcher(shapes).find(), d.id() + ": no VfxShape " + s);
                assertFalse(s.startsWith("TIME_"), d.id() + " reuses a Time Magic shape: " + s);
            }
    }

    @Test
    void everyPotionEffectIsAVanillaId() {
        Set<String> vanilla = Set.of("minecraft:slowness", "minecraft:weakness", "minecraft:mining_fatigue", "minecraft:hunger", "minecraft:nausea", "minecraft:levitation",
                "minecraft:blindness", "minecraft:glowing", "minecraft:wither", "minecraft:darkness", "minecraft:poison", "minecraft:unluck");
        for (UltimateDef d : UltimateRegistry.all())
            for (UltimateDef.Debuff db : d.debuffs()) assertTrue(vanilla.contains(db.effect()), d.id() + ": " + db.effect());
    }

    @Test
    void everyBookIsARegisteredGrimoireAndTheMagicIsARealMagicType() throws IOException {
        String skills = Files.readString(SRC.resolve("skill/NUSkills.java"), StandardCharsets.UTF_8);
        String magics = Files.readString(SRC.resolve("blackclover/MagicType.java"), StandardCharsets.UTF_8);
        String wiki = Files.readString(SRC.resolve("book/WikiBooks.java"), StandardCharsets.UTF_8);
        wiki = wiki.substring(wiki.indexOf("NEW_ATTRIBUTES = EnumSet.of("), wiki.indexOf("public static Set<MagicType> newAttributes"));
        for (UltimateDef d : UltimateRegistry.all()) {
            assertTrue(skills.contains("book(\"" + d.bookPath() + "\"") || Pattern.compile("MagicType\\." + d.magic() + "\\b").matcher(wiki).find(), d.bookPath() + " is not a registered book");      // the 30 wiki attributes are registered by WikiBooks.newAttributes()
            assertTrue(Pattern.compile("(?m)^\\s{4}" + d.magic() + "\\(\"").matcher(magics).find(), d.magic() + " is not a MagicType");
        }
    }

    @Test
    void noUltimateIdIsUsedByAnyOtherPageOfTheMod() throws IOException {
        Pattern lit = Pattern.compile("\"(ultimate_[a-z]+)\"");
        List<String> clashes = new ArrayList<>();
        Set<String> mine = new HashSet<>();
        for (UltimateDef d : UltimateRegistry.all()) mine.add(d.id());
        try (Stream<Path> w = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("ultimate/")) continue;
                Matcher m = lit.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) if (mine.contains(m.group(1))) clashes.add(rel + " " + m.group(1));
            }
        }
        assertEquals(List.of(), clashes, "an old page already uses an Ultimate id");
        for (String line : Files.readAllLines(ROOT.resolve("src/test/resources/armour_policy/page_ids_1_98.txt"), StandardCharsets.UTF_8))
            for (String id : mine) assertFalse((" " + line + " ").contains(" " + id + " "), "the 1.98.0 snapshot has the page " + id);
    }
}
