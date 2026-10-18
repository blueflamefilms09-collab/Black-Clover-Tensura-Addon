package com.newuniverse.nusmp.compat.maps;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The FTB Chunks claim_dimension_blacklist reader (plain text, soft). The key and file shape are from memory; these tests pin the reader. */
class FtbChunksCheckTest {
    private static final List<String> DIMS = List.of("nusmp:mega_dungeon", "nusmp:sanctuary");

    @Test
    void anExactListCoversTheDimensions() {
        String snbt = "{\n  claims: {\n    claim_dimension_blacklist: [\n      \"nusmp:mega_dungeon\"\n      \"nusmp:sanctuary\"\n    ]\n  }\n}";
        assertEquals(List.of("nusmp:mega_dungeon", "nusmp:sanctuary"), FtbChunksCheck.listOf(snbt, FtbChunksCheck.BLACKLIST_KEY));
        FtbChunksCheck.Result r = FtbChunksCheck.analyse(snbt, DIMS, "x");
        assertEquals(FtbChunksCheck.Status.COVERED, r.status());
        assertTrue(r.uncovered().isEmpty());
    }

    @Test
    void aWildcardCoversEverythingOfTheNamespace() {
        assertEquals(FtbChunksCheck.Status.COVERED, FtbChunksCheck.analyse("claim_dimension_blacklist: [ \"nusmp:*\" ]", DIMS, "x").status());
        assertEquals(FtbChunksCheck.Status.COVERED, FtbChunksCheck.analyse("claim_dimension_blacklist: [\"nusmp*\"]", DIMS, "x").status());
        assertEquals(FtbChunksCheck.Status.OPEN, FtbChunksCheck.analyse("claim_dimension_blacklist: [\"other:*\"]", DIMS, "x").status());
    }

    @Test
    void aPartialListReportsWhatIsOpen() {
        FtbChunksCheck.Result r = FtbChunksCheck.analyse("claim_dimension_blacklist: [\"nusmp:mega_dungeon\", \"minecraft:the_end\"]", DIMS, "f");
        assertEquals(FtbChunksCheck.Status.OPEN, r.status());
        assertEquals(List.of("nusmp:sanctuary"), r.uncovered());
        assertEquals("f", r.source());
    }

    @Test
    void anEmptyListOrNoKeyLeavesEverythingOpen() {
        assertEquals(DIMS, FtbChunksCheck.analyse("claim_dimension_blacklist: [ ]", DIMS, "x").uncovered());
        assertEquals(DIMS, FtbChunksCheck.analyse("{ claims: { other: 1 } }", DIMS, "x").uncovered());
        assertNull(FtbChunksCheck.listOf("{ }", FtbChunksCheck.BLACKLIST_KEY));
    }

    @Test
    void aNonEmptyWhitelistBlocksTheRest() {
        assertEquals(FtbChunksCheck.Status.COVERED, FtbChunksCheck.analyse("claim_dimension_whitelist: [\"minecraft:overworld\"]", DIMS, "x").status());
        assertEquals(List.of("nusmp:sanctuary"), FtbChunksCheck.analyse("claim_dimension_whitelist: [\"nusmp:sanctuary\"]", DIMS, "x").uncovered());
        assertEquals(DIMS, FtbChunksCheck.analyse("claim_dimension_whitelist: []", DIMS, "x").uncovered(), "an empty whitelist means no restriction");
    }

    @Test
    void commentsAndCommasAreTolerated() {
        String snbt = "claim_dimension_blacklist: [ // keep out\n \"nusmp:mega_dungeon\", 'nusmp:sanctuary' // hub\n ]";
        assertEquals(FtbChunksCheck.Status.COVERED, FtbChunksCheck.analyse(snbt, DIMS, "x").status());
        assertEquals(FtbChunksCheck.Status.OPEN, FtbChunksCheck.analyse("// claim_dimension_blacklist: [\"nusmp:*\"]\n", DIMS, "x").status(), "a commented-out line does not count");
    }

    @Test
    void unknownWhenThereIsNoText() {
        assertEquals(FtbChunksCheck.Status.UNKNOWN, FtbChunksCheck.analyse(null, DIMS, "x").status());
    }

    @Test
    void matchesRules() {
        assertTrue(FtbChunksCheck.matches("nusmp:sanctuary", "nusmp:sanctuary"));
        assertFalse(FtbChunksCheck.matches("nusmp:sanct", "nusmp:sanctuary"));
        assertTrue(FtbChunksCheck.matches("*", "a:b"));
        assertFalse(FtbChunksCheck.matches(null, "a:b"));
    }

    @Test
    void findsTheFileSoftly() throws IOException {
        Path tmp = Files.createTempDirectory("nusmp-ftbchunks-check");
        assertTrue(FtbChunksCheck.find(tmp.resolve("world"), tmp.resolve("game")).isEmpty(), "no file: empty, no exception");
        assertEquals(FtbChunksCheck.Status.UNKNOWN, FtbChunksCheck.check(null, null, DIMS).status());
        Path dir = Files.createDirectories(tmp.resolve("game").resolve("config").resolve("ftbchunks"));
        Files.writeString(dir.resolve(FtbChunksCheck.FILE), "claim_dimension_blacklist: [\"nusmp:*\"]");
        assertEquals(FtbChunksCheck.Status.COVERED, FtbChunksCheck.check(tmp.resolve("world"), tmp.resolve("game"), DIMS).status());
    }
}
