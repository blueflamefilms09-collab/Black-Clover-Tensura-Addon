package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the roster file is the contract between the Java command that writes it and the Python generator that reads it; this test pins the format
 * ({@code id|name|saga|role}), the escaping of a bar in a name and the header the reader skips, so the two cannot drift apart unnoticed.
 */
class RosterDumpTest {
    @Test
    void oneLinePerCharacterInTheContractedFormat() {
        ArcCharacter boss = ArcCharacter.boss("heath_grice", "Heath Grice").saga(ArcSaga.ELF_REINCARNATION).build();
        ArcCharacter npc = ArcCharacter.npc("asta", "Asta").saga(ArcSaga.ALLIES).build();
        assertEquals("heath_grice|Heath Grice|elf_reincarnation|boss", RosterDump.line(boss));
        assertEquals("asta|Asta|allies|npc", RosterDump.line(npc));
        assertEquals(List.of("heath_grice|Heath Grice|elf_reincarnation|boss", "asta|Asta|allies|npc"), RosterDump.lines(List.of(boss, npc)));
    }

    @Test
    void aBarInANameCannotBreakTheLine() {
        ArcCharacter c = ArcCharacter.npc("odd_one", "A | B\nC").saga(ArcSaga.ALLIES).build();
        String line = RosterDump.line(c);
        assertEquals(4, line.split("\\|", -1).length, line);
        assertEquals("odd_one|A / B C|allies|npc", line);
    }

    @Test
    void theFileStartsWithACommentTheReaderSkipsAndEndsWithANewline() {
        String text = RosterDump.text(List.of(ArcCharacter.boss("a_b", "AB").saga(ArcSaga.DANMACHI).build()));
        String[] lines = text.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("#"));
        assertEquals("a_b|AB|danmachi|boss", lines[1]);
        assertTrue(text.endsWith("\n"));
        assertEquals("# id|name|saga|role : written by /multiverse arc dump, read by tools/quests/spec_arc.py\n", RosterDump.text(List.of()));
    }

    @Test
    void theShippedRosterFileHoldsTheSevenOlderBossesExactlyAsTheCommandWritesThem() throws IOException {
        List<String> file = Files.readAllLines(Path.of("tools/quests/arc_roster.txt"));
        List<String> expected = RosterDump.lines(ExistingBosses.all());
        assertTrue(file.get(0).startsWith("#"), "the header comment is the reader's cue");
        assertTrue(file.containsAll(expected), "tools/quests/arc_roster.txt lost a line of the older bosses: " + expected);
        Set<String> ids = new HashSet<>();
        for (String line : file) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            assertEquals(4, f.length, "a roster line has four fields: " + line);
            assertTrue(f[0].matches("[a-z0-9_]+"), "bad id in " + line);
            assertTrue(f[3].equals("boss") || f[3].equals("npc"), "bad role in " + line);
            assertTrue(ids.add(f[0]), "listed twice: " + f[0]);
        }
    }

    @Test
    void everyBossOfTheRosterFileHasItsAdvancementAndEverySagaWithABossHasOne() throws IOException {
        Path adv = Path.of("src/main/resources/data/nusmp/advancement/arc");
        Set<String> sagas = new HashSet<>();
        for (String line : Files.readAllLines(Path.of("tools/quests/arc_roster.txt"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            boolean boss = f[3].equals("boss");
            assertTrue(Files.isRegularFile(adv.resolve((boss ? "defeated_" : "met_") + f[0] + ".json")), "no advancement for " + f[0] + ": run tools/quests/gen_arc_adv.py");
            if (boss) sagas.add(f[2]);
        }
        for (String saga : sagas) assertTrue(Files.isRegularFile(adv.resolve("saga_" + saga + ".json")), "no saga advancement for " + saga);
        assertTrue(Files.isRegularFile(adv.resolve("root.json")) && Files.isRegularFile(adv.resolve("arena.json")));
    }
}
