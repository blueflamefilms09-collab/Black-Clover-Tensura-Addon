package com.newuniverse.nusmp.labyrinth.mega.gear;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the whole point of making standard gear out of ORDINARY items is that it has no world limit, and the only thing that would take that away is a
 * relic: a relic stack carries a serial the Order's ledger issues, and "each one exists only once in the whole world". The factory is built from Minecraft
 * types, so a unit test cannot make a stack and look for a relic component on it (the in-game self-test {@code gear.roll} does exactly that). What this
 * test can do, and does, is guard the DESIGN at the source: the gear package may import almost nothing of the relic system. The colour and the label of a
 * grade come from the relic {@code Grade} enum (the one import {@code GearLook} needs), and the self-test reads the relic components to prove their absence;
 * nothing else, so a later change that wires a serial, a ledger call or a relic item into the drops fails the build here before a Rare sword
 * can be issued twice or refused because "it already exists". It reads the sources with {@code java.nio}, working directory = project root (the same
 * assumption as the other data tests).
 */
class GearIsNotARelicTest {
    private static final Path PACKAGE = Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/gear");

    /** File -> the relic-system imports it is allowed to have (everything else must have none). */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "GearLook.java", Set.of("com.newuniverse.nusmp.relics.Grade"),
            "GearSelfTests.java", Set.of("com.newuniverse.nusmp.relics.RelicComponents2", "com.newuniverse.nusmp.relics.Relics"));

    private static final String[] FORBIDDEN_IMPORT_PREFIXES = {
            "com.newuniverse.nusmp.relics.", "com.newuniverse.nusmp.dungeon.nexus.relic.", "com.newuniverse.nusmp.item.RelicItem", "com.newuniverse.nusmp.item.BossRelics"};

    private static List<Path> sources() throws IOException {
        try (Stream<Path> files = Files.list(PACKAGE)) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    @Test
    void thePackageImportsOnlyTheGradeFromTheRelicSystem() throws IOException {
        List<Path> files = sources();
        assertTrue(files.size() >= 15, "the gear package should have its classes: " + files.size());
        Map<String, List<String>> bad = new TreeMap<>();
        for (Path f : files) {
            Set<String> allowed = ALLOWED.getOrDefault(f.getFileName().toString(), Set.of());
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (!t.startsWith("import ")) continue;
                String imported = t.substring("import ".length(), t.length() - 1).trim();
                for (String prefix : FORBIDDEN_IMPORT_PREFIXES) {
                    if (imported.startsWith(prefix) && !allowed.contains(imported)) bad.computeIfAbsent(f.getFileName().toString(), k -> new ArrayList<>()).add(imported);
                }
            }
        }
        assertTrue(bad.isEmpty(), "standard gear must not touch the relic system, but these imports do: " + bad);
    }

    @Test
    void noSourceMentionsALedgerSerialOrAnIssueCall() throws IOException {
        for (Path f : sources()) {
            if (f.getFileName().toString().equals("GearSelfTests.java")) continue;       // the self-test names the components to prove their absence
            String code = withoutComments(Files.readString(f, StandardCharsets.UTF_8));
            for (String word : new String[] {"UniqueLedger", "RelicSerial", "RelicComponents2", "relic_serial", ".issue("})
                assertFalse(code.contains(word), f.getFileName() + " uses '" + word + "': standard gear must never be issued through the Order's ledger");
        }
    }

    /** The source with block comments and line comments removed (the comments are allowed to NAME the relic system to explain why it is not used). */
    static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    @Test
    void commentsAreStrippedBeforeTheSearch() {
        assertEquals("a  b", withoutComments("a /* UniqueLedger */ b"));
        assertEquals("x \ny", withoutComments("x // RelicSerial\ny"));
        assertTrue(withoutComments("call(UniqueLedger.get())").contains("UniqueLedger"));
    }

    @Test
    void theOnlyComponentTheGearRegistersIsItsOwn() throws IOException {
        String text = Files.readString(PACKAGE.resolve("GearComponents.java"), StandardCharsets.UTF_8);
        int count = 0;
        for (int i = text.indexOf("registerComponentType("); i >= 0; i = text.indexOf("registerComponentType(", i + 1)) count++;
        assertEquals(1, count, "one data component, nusmp:gear_grade");
        assertTrue(text.contains("\"gear_grade\""));
    }
}
