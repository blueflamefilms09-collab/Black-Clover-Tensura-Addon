package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the characters of the arcs may walk the dungeon's halls as wanderers through an ADDITIVE data file ({@code wanderers_arc.json}) merged after the main one;
 * these tests pin the merge (an extra file adds kinds and never changes a main entry, null sides are empty, the order is main first) and that the shipped extra
 * file is clean under the same strict parser the main file is held to, so a typo in it shows up here and not as a warning in a log nobody reads.
 */
class WandererRosterMergeTest {
    private static final Path ARC = Path.of("src/main/resources/data/nusmp/mega/wanderers_arc.json");
    private static final Path MAIN = Path.of("src/main/resources/data/nusmp/mega/wanderers.json");

    private static WandererRoster of(String... entityWeight) {
        StringBuilder b = new StringBuilder("{\"entries\":[");
        for (int i = 0; i < entityWeight.length; i += 2) {
            if (i > 0) b.append(',');
            b.append("{\"entity\":\"").append(entityWeight[i]).append("\",\"weight\":").append(entityWeight[i + 1]).append('}');
        }
        WandererRoster.Parsed p = WandererRoster.parse(b.append("]}").toString());
        assertTrue(p.clean(), String.valueOf(p.problems()));
        return p.roster();
    }

    @Test
    void theExtraFileAddsKindsAfterTheMainOnesAndNeverChangesAMainEntry() {
        WandererRoster main = of("nusmp:floor_warden", "5", "minecraft:ravager", "4");
        WandererRoster extra = of("nusmp:vetto", "1", "minecraft:ravager", "99");
        WandererRoster merged = WandererRoster.merge(main, extra);
        assertEquals(3, merged.entries().size());
        assertEquals("nusmp:floor_warden", merged.entries().get(0).entity());
        assertEquals("minecraft:ravager", merged.entries().get(1).entity());
        assertEquals(4, merged.entries().get(1).weight(), "the main file's tuning wins");
        assertEquals("nusmp:vetto", merged.entries().get(2).entity());
    }

    @Test
    void aMissingSideIsEmptyAndNothingIsLost() {
        WandererRoster main = of("nusmp:floor_warden", "5");
        assertEquals(main.entries(), WandererRoster.merge(main, null).entries());
        assertEquals(main.entries(), WandererRoster.merge(null, main).entries());
        assertEquals(main.entries(), WandererRoster.merge(main, WandererRoster.EMPTY).entries());
        assertTrue(WandererRoster.merge(null, null).entries().isEmpty());
    }

    @Test
    void mergingTwiceGivesTheSameRoster() {
        WandererRoster main = of("nusmp:floor_warden", "5");
        WandererRoster extra = of("nusmp:vetto", "1");
        WandererRoster once = WandererRoster.merge(main, extra);
        assertEquals(once.entries(), WandererRoster.merge(once, extra).entries());
    }

    @Test
    void theMergedRosterCanBeDrawnFromByThreat() {
        WandererRoster merged = WandererRoster.merge(of("nusmp:floor_warden", "5"), of("nusmp:vetto", "1"));
        Random r = new Random(7);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            WandererRoster.Entry e = merged.pick(3, null, r);
            assertNotNull(e);
            seen.add(e.entity());
        }
        assertEquals(Set.of("nusmp:floor_warden", "nusmp:vetto"), seen);
    }

    @Test
    void theShippedExtraFileIsCleanAndAdditive() throws IOException {
        WandererRoster.Parsed p = WandererRoster.parse(Files.readString(ARC));
        assertTrue(p.clean(), "the shipped wanderers_arc.json has problems: " + p.problems());
        assertTrue(p.roster().entries().size() >= 6, "the extra file should list the first characters of the arcs");
        WandererRoster.Parsed main = WandererRoster.parse(Files.readString(MAIN));
        assertTrue(main.clean(), String.valueOf(main.problems()));
        List<String> mainIds = main.roster().entries().stream().map(WandererRoster.Entry::entity).toList();
        for (WandererRoster.Entry e : p.roster().entries()) {
            assertTrue(e.entity().startsWith("nusmp:"), e.entity() + " is not one of this mod's characters");
            assertFalse(mainIds.contains(e.entity()), e.entity() + " is already in the main file: the extra entry would never be used");
            assertTrue(e.minThreat() >= 1 && e.maxThreat() > e.minThreat(), e.entity() + " has a bad threat window");
            assertTrue(e.weight() >= 1 && e.weight() <= 5, e.entity() + " is too common for an elite: weight " + e.weight());
        }
        WandererRoster merged = WandererRoster.merge(main.roster(), p.roster());
        assertEquals(main.roster().entries().size() + p.roster().entries().size(), merged.entries().size());
    }
}
