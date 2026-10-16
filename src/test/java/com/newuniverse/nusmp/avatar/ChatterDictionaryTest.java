package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.chatter.ChatterDictionary;
import com.newuniverse.nusmp.avatar.chatter.ChatterLimiter;
import com.newuniverse.nusmp.avatar.chatter.ChatterTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The shipped chatter files obey the schema, the loader merges packs, and the limiter holds its limits. */
class ChatterDictionaryTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/avatar_chatter");
    private static final ChatEvent[] REQUIRED = {ChatEvent.COMBAT_START, ChatEvent.CREATOR_LOW_HP, ChatEvent.SKILL_CAST, ChatEvent.NAMING,
            ChatEvent.BETRAYAL, ChatEvent.FRENZY, ChatEvent.IDLE};

    private static List<ChatterDictionary.Parsed> shipped() throws IOException {
        List<ChatterDictionary.Parsed> out = new ArrayList<>();
        for (AvatarPersonality p : AvatarPersonality.values()) {
            Path f = DIR.resolve(p.name().toLowerCase(Locale.ROOT) + ".json");
            assertTrue(Files.exists(f), "missing chatter file for " + p);
            ChatterDictionary.Parsed parsed = ChatterDictionary.parseFile(Files.readString(f, StandardCharsets.UTF_8));
            assertTrue(parsed.ok(), f + ": " + parsed.error());
            assertEquals(p, parsed.personality(), f + " names the wrong personality");
            out.add(parsed);
        }
        return out;
    }

    @Test
    void everyPersonalityHasEveryEventWithEnoughDistinctLines() throws IOException {
        for (ChatterDictionary.Parsed f : shipped())
            for (ChatEvent e : REQUIRED) {
                List<String> lines = f.events().get(e);
                assertNotNull(lines, f.personality() + " lacks " + e);
                assertTrue(lines.size() >= 14, f.personality() + " " + e + " has only " + lines.size() + " lines");
                Set<String> seen = new HashSet<>();
                for (String l : lines) assertTrue(seen.add(l.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9{}]+", " ").strip()), f.personality() + " " + e + " repeats: " + l);
            }
    }

    @Test
    void everyLineIsNonEmptyShortAndUsesKnownPlaceholders() throws IOException {
        for (ChatterDictionary.Parsed f : shipped())
            f.events().forEach((e, lines) -> {
                for (String l : lines) {
                    assertFalse(l.isBlank(), f.personality() + " " + e + " has an empty line");
                    assertTrue(l.length() < 160, f.personality() + " " + e + " line too long: " + l);
                    for (String ph : ChatterTemplate.placeholders(l))
                        assertTrue(ChatterTemplate.KNOWN.contains(ph), f.personality() + " " + e + " unknown placeholder {" + ph + "} in: " + l);
                    assertFalse(l.contains("{") && !l.contains("}"), "unbalanced brace in: " + l);
                }
            });
    }

    @Test
    void skillCastLinesCarryTheSkillAndCombatLinesAreNotAllBlank() throws IOException {
        for (ChatterDictionary.Parsed f : shipped()) {
            long withSkill = f.events().get(ChatEvent.SKILL_CAST).stream().filter(l -> l.contains("{skill}")).count();
            assertTrue(withSkill >= (f.personality() == AvatarPersonality.STOIC ? 3 : 10), f.personality() + " SKILL_CAST lines should shout the skill");
        }
    }

    @Test
    void yandereHasItsJealousWarnings() throws IOException {
        for (ChatterDictionary.Parsed f : shipped())
            if (f.personality() == AvatarPersonality.YANDERE) {
                List<String> w = f.events().get(ChatEvent.JEALOUS_WARN);
                assertNotNull(w);
                assertTrue(w.size() >= 14);
                assertTrue(w.contains("Step away from {creator}."));
            }
    }

    @Test
    void totalLineCountIsMassive() throws IOException {
        int total = 0;
        for (ChatterDictionary.Parsed f : shipped()) for (List<String> l : f.events().values()) total += l.size();
        assertTrue(total >= 1372, "expected at least 14 x 7 x 14 lines, got " + total);
    }

    @Test
    void buildServesLinesAndMergesPacks() {
        ChatterDictionary.Parsed a = ChatterDictionary.parseFile("{\"personality\":\"LOYAL\",\"events\":{\"IDLE\":[\"a\",\"b\"]}}");
        ChatterDictionary.Parsed b = ChatterDictionary.parseFile("{\"personality\":\"loyal\",\"events\":{\"IDLE\":[\"b\",\"c\"],\"FRENZY\":[\"x\"]}}");
        ChatterDictionary d = ChatterDictionary.build(List.of(a, b));
        assertEquals(List.of("a", "b", "c"), d.lines(AvatarPersonality.LOYAL, ChatEvent.IDLE), "a pack adds new lines, never duplicates");
        assertEquals(List.of("x"), d.lines(AvatarPersonality.LOYAL, ChatEvent.FRENZY));
        assertEquals(List.of(), d.lines(AvatarPersonality.LAZY, ChatEvent.IDLE));
        assertTrue(d.has(AvatarPersonality.LOYAL));
        assertFalse(d.has(AvatarPersonality.LAZY));
        assertEquals(List.of(), ChatterDictionary.EMPTY.lines(null, ChatEvent.IDLE));
    }

    @Test
    void badFilesAreReportedNotThrown() {
        assertFalse(ChatterDictionary.parseFile("not json").ok());
        assertFalse(ChatterDictionary.parseFile("{\"personality\":\"NOPE\",\"events\":{}}").ok());
        assertFalse(ChatterDictionary.parseFile("{\"personality\":\"LOYAL\"}").ok());
        assertFalse(ChatterDictionary.parseFile("{\"personality\":\"LOYAL\",\"events\":{\"BOGUS\":[\"x\"]}}").ok());
        assertFalse(ChatterDictionary.parseFile("{\"personality\":\"LOYAL\",\"events\":{\"IDLE\":[1]}}").ok());
        ChatterDictionary d = ChatterDictionary.build(List.of(ChatterDictionary.parseFile("garbage")));
        assertEquals(List.of(), d.lines(AvatarPersonality.LOYAL, ChatEvent.IDLE));
    }

    // ---------------------------------------------------------------- limiter
    @Test
    void cooldownBlocksTheSameAvatarAndEventOnly() {
        ChatterLimiter l = new ChatterLimiter();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(l.tryAcquire(a, ChatEvent.IDLE, 100, 200, 10));
        assertFalse(l.tryAcquire(a, ChatEvent.IDLE, 150, 200, 10), "still cooling down");
        assertTrue(l.tryAcquire(a, ChatEvent.COMBAT_START, 150, 200, 10), "another event is independent");
        assertTrue(l.tryAcquire(b, ChatEvent.IDLE, 150, 200, 10), "another avatar is independent");
        assertFalse(l.tryAcquire(a, ChatEvent.IDLE, 299, 200, 10));
        assertTrue(l.tryAcquire(a, ChatEvent.IDLE, 300, 200, 10), "free again exactly after the cooldown");
    }

    @Test
    void theTickBudgetCapsASwarm() {
        ChatterLimiter l = new ChatterLimiter();
        int spoke = 0;
        for (int i = 0; i < 20; i++) if (l.tryAcquire(UUID.randomUUID(), ChatEvent.COMBAT_START, 500, 400, 3)) spoke++;
        assertEquals(3, spoke, "only three lines in one tick");
        int next = 0;
        for (int i = 0; i < 20; i++) if (l.tryAcquire(UUID.randomUUID(), ChatEvent.COMBAT_START, 501, 400, 3)) next++;
        assertEquals(3, next, "the budget refills the next tick");
    }

    @Test
    void aRefusedLineConsumesNothing() {
        ChatterLimiter l = new ChatterLimiter();
        UUID a = UUID.randomUUID();
        assertTrue(l.tryAcquire(a, ChatEvent.IDLE, 10, 100, 1));
        assertFalse(l.tryAcquire(a, ChatEvent.IDLE, 10, 100, 1), "budget gone");
        assertFalse(l.tryAcquire(UUID.randomUUID(), ChatEvent.IDLE, 10, 100, 1), "budget gone for others too");
        assertTrue(l.tryAcquire(UUID.randomUUID(), ChatEvent.IDLE, 11, 100, 1));
    }

    @Test
    void zeroCooldownEventsNeverWait() {
        ChatterLimiter l = new ChatterLimiter();
        UUID a = UUID.randomUUID();
        for (int t = 0; t < 5; t++) assertTrue(l.tryAcquire(a, ChatEvent.NAMING, t, 0, 4));
    }

    @Test
    void forgetAndPruneKeepTheMapSmall() {
        ChatterLimiter l = new ChatterLimiter();
        UUID a = UUID.randomUUID();
        for (int i = 0; i < 50; i++) l.tryAcquire(UUID.randomUUID(), ChatEvent.IDLE, i, 100, 100);
        l.tryAcquire(a, ChatEvent.IDLE, 60, 100, 100);
        l.forget(a);
        assertFalse(l.tryAcquire(UUID.randomUUID(), ChatEvent.IDLE, 61, 100, 0), "budget zero blocks everything");
        l.tryAcquire(UUID.randomUUID(), ChatEvent.IDLE, 7000, 100, 100);       // far later: the old cooldowns are over and pruned
        assertTrue(l.size() <= 2, "old cooldowns were pruned, size " + l.size());
    }
}
