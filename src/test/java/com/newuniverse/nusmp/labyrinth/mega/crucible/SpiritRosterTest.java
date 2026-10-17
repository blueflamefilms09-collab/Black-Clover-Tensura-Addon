package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.mega.crucible.SpiritRoster.Entry;
import com.newuniverse.nusmp.labyrinth.mega.theme.MiniJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The Cursed Spirit roster: the shipped file parses clean, the draw is weighted, deterministic and skips entities that do not exist. */
class SpiritRosterTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/mega/cursed_spirits.json");

    private static SpiritRoster.Parsed parse(String json) { return SpiritRoster.parse(MiniJson.parse(json)); }

    @Test
    void theShippedRosterParsesWithoutWarnings() throws IOException {
        String raw = Files.readString(FILE, StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"_why\": \""), "every JSON file says why it is built that way");
        SpiritRoster.Parsed p = SpiritRoster.parse(MiniJson.parse(raw));
        assertEquals(List.of(), p.warnings());
        assertTrue(p.entries().size() >= 6, "a roster of at least six creatures");
        for (Entry e : p.entries()) {
            assertTrue(e.entity().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), e.entity());
            assertTrue(e.weight() > 0 && e.weight() <= 1000);
            assertTrue(e.healthScale() > 0.0 && e.healthScale() <= 20.0, e.entity());
            assertFalse(e.name().isBlank());
        }
        long distinct = p.entries().stream().map(Entry::entity).distinct().count();
        assertEquals(p.entries().size(), distinct, "no entity listed twice");
    }

    @Test
    void anEntryNeedsAnIdAndAPositiveWeight() {
        SpiritRoster.Parsed p = parse("{\"entries\": [{\"entity\": \"a:b\", \"weight\": 2}, {\"weight\": 5}, {\"entity\": \"c:d\", \"weight\": 0}, {\"entity\": \"e:f\", \"weight\": -3}, 7, {\"entity\": \"g\"}]}");
        assertEquals(2, p.entries().size());
        assertEquals("a:b", p.entries().get(0).entity());
        assertEquals("minecraft:g", p.entries().get(1).entity(), "a bare id is a minecraft id");
        assertEquals(1, p.entries().get(1).weight(), "the default weight is 1");
        assertEquals(SpiritRoster.DEFAULT_NAME, p.entries().get(1).name());
        assertEquals(1.0, p.entries().get(1).healthScale(), 0.0001);
        assertEquals(4, p.warnings().size(), p.warnings().toString());
    }

    @Test
    void aBadHealthScaleBecomesOneAndAHugeOneIsCapped() {
        SpiritRoster.Parsed p = parse("{\"entries\": [{\"entity\": \"a:b\", \"healthScale\": -2}, {\"entity\": \"a:c\", \"healthScale\": 500}, {\"entity\": \"a:d\", \"healthScale\": \"big\"}]}");
        assertEquals(1.0, p.entries().get(0).healthScale(), 0.0001);
        assertEquals(20.0, p.entries().get(1).healthScale(), 0.0001);
        assertEquals(1.0, p.entries().get(2).healthScale(), 0.0001);
        assertEquals(1, p.warnings().size());
    }

    @Test
    void aFileThatIsNotARosterGivesNothingAndAWarning() {
        assertEquals(0, SpiritRoster.parse("nope").entries().size());
        assertEquals(1, SpiritRoster.parse(null).warnings().size());
        assertEquals(0, parse("{\"entries\": 3}").entries().size());
        assertEquals(0, parse("[]").entries().size());
    }

    @Test
    void theDrawIsDeterministicAndFollowsTheWeights() {
        List<Entry> roster = List.of(new Entry("a:x", 1, "X", 1.0), new Entry("a:y", 3, "Y", 1.0), new Entry("a:z", 6, "Z", 1.0));
        assertSame(SpiritRoster.pick(roster, 42L, id -> true), SpiritRoster.pick(roster, 42L, id -> true));
        Map<String, Integer> seen = new HashMap<>();
        int n = 20000;
        for (long seed = 0; seed < n; seed++) seen.merge(SpiritRoster.pick(roster, seed, id -> true).entity(), 1, Integer::sum);
        assertEquals(0.1, seen.get("a:x") / (double) n, 0.02);
        assertEquals(0.3, seen.get("a:y") / (double) n, 0.02);
        assertEquals(0.6, seen.get("a:z") / (double) n, 0.02);
    }

    @Test
    void anEntityThatDoesNotExistIsSkipped() {
        List<Entry> roster = List.of(new Entry("gone:mod", 100, "Gone", 1.0), new Entry("here:one", 1, "Here", 1.0));
        for (long seed = 0; seed < 500; seed++) assertEquals("here:one", SpiritRoster.pick(roster, seed, id -> id.startsWith("here:")).entity());
        assertNull(SpiritRoster.pick(roster, 1L, id -> false), "nothing drawable: the catastrophe becomes a collapse");
        assertNull(SpiritRoster.pick(List.of(), 1L, id -> true));
    }
}
