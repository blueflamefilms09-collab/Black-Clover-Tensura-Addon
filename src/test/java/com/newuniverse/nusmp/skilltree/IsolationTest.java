package com.newuniverse.nusmp.skilltree;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's exclusion rule is that the Skill Tree never governs the Black Clover Grimoire (innate, bound items) and the two systems share no data, currency, packet or screen logic. A
 * comment in the code cannot enforce that, so the build does: this test reads the source tree and FAILS when any file of the Skill Tree (server side {@code skilltree/**} or client side
 * {@code client/skilltree/**}) names a Grimoire or profile class (the {@code grimoire} and {@code blackclover} packages, {@code MultiverseProfile}, {@code MasteryPages}, {@code GrimoireBook},
 * {@code GrimoireSummon}), and when any file of the Grimoire side ({@code grimoire/**}, {@code blackclover/**}, {@code book/**}, {@code client/grimoire/**}) names the Skill Tree. It looks at the
 * CODE of each file, with comments removed, for the forbidden fully-qualified names anywhere in it, not only in {@code import} lines, because this repository often writes a class by its full name
 * inline, and an import-only scan would pass exactly that. The scanner is a plain function ({@link #violations}) so a negative control can feed it a string: the test proves the scanner
 * catches an import, an inline name, a wildcard import and a static import, and ignores a mention in a comment or a different package that merely starts the same way.
 */
class IsolationTest {
    private static final Path ROOT = Path.of("src/main/java/com/newuniverse/nusmp");

    /** What a Skill Tree file must not name. */
    static final List<String> FORBIDDEN_FOR_THE_TREE = List.of(
            "com.newuniverse.nusmp.grimoire.",
            "com.newuniverse.nusmp.blackclover.",
            "com.newuniverse.nusmp.client.grimoire.",
            "com.newuniverse.nusmp.multiverse.MultiverseProfile",
            "com.newuniverse.nusmp.multiverse.MasteryPages",
            "com.newuniverse.nusmp.book.GrimoireBook",
            "com.newuniverse.nusmp.book.GrimoireSummon");

    /** What a Grimoire file must not name. */
    static final List<String> FORBIDDEN_FOR_THE_GRIMOIRE = List.of(
            "com.newuniverse.nusmp.skilltree.",
            "com.newuniverse.nusmp.client.skilltree.");

    /** The folders of the Skill Tree, and the folders that must never reach into it. */
    static final List<String> TREE_DIRS = List.of("skilltree", "client/skilltree");
    static final List<String> GRIMOIRE_DIRS = List.of("grimoire", "blackclover", "book", "client/grimoire");

    // ------------------------------------------------------------------ the scanner
    /** The source with {@code //} and block comments (and Javadoc) blanked out, string and character literals kept, line structure kept. */
    static String stripComments(String src) {
        StringBuilder out = new StringBuilder(src.length());
        int i = 0, n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                i += 2;
                while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/')) {
                    if (src.charAt(i) == '\n') out.append('\n');
                    i++;
                }
                i += 2;
            } else if (c == '"' && src.startsWith("\"\"\"", i)) {                       // a text block: kept as it is, up to its closing quotes
                int end = src.indexOf("\"\"\"", i + 3);
                while (end > 0 && src.charAt(end - 1) == '\\') end = src.indexOf("\"\"\"", end + 1);
                int stop = end < 0 ? n : end + 3;
                out.append(src, i, stop);
                i = stop;
            } else if (c == '"' || c == '\'') {
                char quote = c;
                out.append(c);
                i++;
                while (i < n && src.charAt(i) != quote) {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        out.append(src.charAt(i));
                        i++;
                    }
                    out.append(src.charAt(i));
                    i++;
                }
                if (i < n) {
                    out.append(quote);
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** The lines (1-based, with the offending name) where this source names one of the forbidden prefixes. */
    static List<String> violations(String file, String source, List<String> forbidden) {
        List<String> out = new ArrayList<>();
        String[] lines = stripComments(source).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            for (String f : forbidden) {
                if (lines[i].contains(f)) out.add(file + ":" + (i + 1) + " names " + f);
            }
        }
        return out;
    }

    private static List<String> scan(List<String> dirs, List<String> forbidden) throws IOException {
        assertTrue(Files.isDirectory(ROOT), "run the tests from the project folder: " + ROOT.toAbsolutePath() + " is not a folder");
        List<String> out = new ArrayList<>();
        for (String dir : dirs) {
            Path base = ROOT.resolve(dir);
            if (!Files.isDirectory(base)) continue;
            try (Stream<Path> walk = Files.walk(base)) {
                List<Path> files = walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
                for (Path p : files) out.addAll(violations(ROOT.relativize(p).toString().replace('\\', '/'), Files.readString(p, StandardCharsets.UTF_8), forbidden));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the real rule
    @Test
    void theSkillTreeNamesNoGrimoireOrProfileClass() throws IOException {
        List<String> offenders = scan(TREE_DIRS, FORBIDDEN_FOR_THE_TREE);
        assertTrue(offenders.isEmpty(), "the Skill Tree must not touch the Grimoire: " + String.join("; ", offenders));
    }

    @Test
    void theGrimoireSideNamesNothingOfTheSkillTree() throws IOException {
        List<String> offenders = scan(GRIMOIRE_DIRS, FORBIDDEN_FOR_THE_GRIMOIRE);
        assertTrue(offenders.isEmpty(), "the Grimoire must not touch the Skill Tree: " + String.join("; ", offenders));
    }

    @Test
    void theScannedFoldersReallyExist() throws IOException {
        // a rule that scans nothing proves nothing: the Skill Tree has files, and so does each Grimoire folder
        int tree = 0;
        for (String dir : TREE_DIRS) {
            Path base = ROOT.resolve(dir);
            assertTrue(Files.isDirectory(base), base + " should exist");
            try (Stream<Path> walk = Files.walk(base)) {
                tree += (int) walk.filter(p -> p.toString().endsWith(".java")).count();
            }
        }
        assertTrue(tree >= 10, "the Skill Tree has at least ten source files, found " + tree);
        for (String dir : GRIMOIRE_DIRS) assertTrue(Files.isDirectory(ROOT.resolve(dir)), ROOT.resolve(dir) + " should exist, or the Grimoire scan is looking at nothing");
    }

    // ------------------------------------------------------------------ negative controls: the same scanner on strings
    @Test
    void theScannerCatchesAnImportAnInlineNameAWildcardAndAStaticImport() {
        String imp = "package a;\nimport com.newuniverse.nusmp.grimoire.GrimoireItem;\nclass X {}\n";
        assertEquals(List.of("X.java:2 names com.newuniverse.nusmp.grimoire."), violations("X.java", imp, FORBIDDEN_FOR_THE_TREE));
        String inline = "class X { void f() { com.newuniverse.nusmp.blackclover.GrimoireSlot.get(null); } }";
        assertEquals(1, violations("X.java", inline, FORBIDDEN_FOR_THE_TREE).size());
        String wildcard = "import com.newuniverse.nusmp.grimoire.*;";
        assertEquals(1, violations("X.java", wildcard, FORBIDDEN_FOR_THE_TREE).size());
        String stat = "import static com.newuniverse.nusmp.multiverse.MasteryPages.slots;";
        assertEquals(1, violations("X.java", stat, FORBIDDEN_FOR_THE_TREE).size());
        String profile = "var x = com.newuniverse.nusmp.multiverse.MultiverseProfile.accepted(p);";
        assertEquals(1, violations("X.java", profile, FORBIDDEN_FOR_THE_TREE).size());
        String book = "import com.newuniverse.nusmp.book.GrimoireBook;\nimport com.newuniverse.nusmp.book.GrimoireSummon;";
        assertEquals(2, violations("X.java", book, FORBIDDEN_FOR_THE_TREE).size());
        String back = "import com.newuniverse.nusmp.skilltree.SkillTreeApi;";
        assertEquals(1, violations("Y.java", back, FORBIDDEN_FOR_THE_GRIMOIRE).size(), "the other direction");
        String clientBack = "x = com.newuniverse.nusmp.client.skilltree.SkillTreeClient.class;";
        assertEquals(1, violations("Y.java", clientBack, FORBIDDEN_FOR_THE_GRIMOIRE).size());
    }

    @Test
    void theScannerIgnoresCommentsAndNeighbouringNames() {
        String comment = "// import com.newuniverse.nusmp.grimoire.GrimoireItem;\n/* com.newuniverse.nusmp.blackclover.X */\n/**\n * see {@link com.newuniverse.nusmp.grimoire.Y}\n */\nclass X {}";
        assertTrue(violations("X.java", comment, FORBIDDEN_FOR_THE_TREE).isEmpty(), "a mention in a comment is not a dependency");
        String neighbours = "import com.newuniverse.nusmp.quests.NusmpEvents;\nimport com.newuniverse.nusmp.balance.BalanceLaw;\nimport com.newuniverse.nusmp.multiverse.MultiverseSync;\nimport com.newuniverse.nusmp.book.RougeCat;";
        assertTrue(violations("X.java", neighbours, FORBIDDEN_FOR_THE_TREE).isEmpty(), "other classes of the same packages are allowed");
        String alike = "import com.newuniverse.nusmp.grimoireshelf.Z;\nimport com.newuniverse.nusmp.blackcloverish.W;";
        assertTrue(violations("X.java", alike, FORBIDDEN_FOR_THE_TREE).isEmpty(), "a package that merely starts with the same letters is another package");
        String skilltreeLike = "import com.newuniverse.nusmp.skilltreeish.Q;";
        assertTrue(violations("Y.java", skilltreeLike, FORBIDDEN_FOR_THE_GRIMOIRE).isEmpty());
    }

    @Test
    void aNameInsideAStringStillCountsAndACommentMarkerInsideAStringDoesNotHideCode() {
        String reflect = "Class.forName(\"com.newuniverse.nusmp.grimoire.GrimoireItem\");";
        assertEquals(1, violations("X.java", reflect, FORBIDDEN_FOR_THE_TREE).size(), "loading a Grimoire class by name is still a dependency");
        String url = "String u = \"http://example.org/*\"; var g = com.newuniverse.nusmp.grimoire.GrimoireItem.class; String v = \"*/\";";
        assertEquals(1, violations("X.java", url, FORBIDDEN_FOR_THE_TREE).size(), "a // or /* inside a string must not start a comment that swallows the real code after it");
        String block = "var json = \"\"\"\n  { \"url\": \"http://x\", \"it's\": 1 }\n  \"\"\"; var g = com.newuniverse.nusmp.grimoire.GrimoireItem.class; // trailing comment";
        assertEquals(1, violations("X.java", block, FORBIDDEN_FOR_THE_TREE).size(), "a text block with quotes, an apostrophe and // inside must not confuse the scan of the code after it");
        assertFalse(stripComments("a // b\nc").contains("b"));
        assertEquals("a \nc", stripComments("a // b\nc"), "the line structure is kept so line numbers stay right");
    }
}
