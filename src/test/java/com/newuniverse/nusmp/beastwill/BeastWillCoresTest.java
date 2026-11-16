package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "only a few of the cores work, the others give no UI". The creative tab and a bare core use the static table {@link BeastWillCores}; this pins every row to the will's own data file (same beast name, the
 * rank is the will's own lowest rank, so the rank gate of the ask passes), that all 27 wills have a row, and that a bare core picks a will the player does not hold. Pure (file reads, project root).
 */
class BeastWillCoresTest {
    private static final File DIR = new File("src/main/resources/data/anime_multiverse/beast_will");

    @Test
    void everyDataFileHasExactlyOneRowWithItsBeastAndItsOwnRank() throws Exception {
        File[] files = DIR.listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(files);
        assertEquals(files.length, BeastWillCores.ALL.size(), "one core row per will file");
        Set<String> seen = new HashSet<>();
        for (BeastWillCores.Entry e : BeastWillCores.ALL) {
            assertTrue(seen.add(e.will()), e.will() + " is listed twice");
            File f = new File(DIR, e.will() + ".json");
            assertTrue(f.isFile(), e.will() + " has no data file");
            JsonObject o;
            try (Reader r = new FileReader(f)) { o = JsonParser.parseReader(r).getAsJsonObject(); }
            assertEquals(o.get("id").getAsString(), e.will());
            assertEquals(o.get("beast").getAsString(), e.beast(), e.will() + ": the beast name");
            BeastRank min = BeastRank.byName(o.get("minRank").getAsString());
            assertNotNull(min, e.will() + ": minRank");
            assertEquals(min, e.rank(), e.will() + ": the core carries the will's own lowest rank");
            assertTrue(e.rank().atLeast(min));
        }
    }

    @Test
    void aBareCoreTakesTheFirstWillThePlayerDoesNotHold() {
        assertEquals(BeastWillCores.ALL.get(0), BeastWillCores.firstFree(w -> false));
        Set<String> held = new HashSet<>();
        held.add(BeastWillCores.ALL.get(0).will());
        held.add(BeastWillCores.ALL.get(1).will());
        assertEquals(BeastWillCores.ALL.get(2), BeastWillCores.firstFree(held::contains));
        // holding every will still answers a row, so the ask opens and names its reason instead of doing nothing
        assertEquals(BeastWillCores.ALL.get(0), BeastWillCores.firstFree(w -> true));
    }
}
