package com.newuniverse.nusmp.skilltree.line;

import com.google.gson.JsonElement;
import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteWire;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the line data was designed for hundreds of books, and a loader that only works for 8 is not a design. This test creates 300 lines in the generator's shape (written to a temporary folder as real files
 * and read back with the real parser, so the file handling is in the measurement), loads them through the registry's own builder, and checks that the whole set loads clean in a sane time and is bigger than
 * one classic payload (so the registry's own caps, not the old wire cap, are what let it through). The timing is printed so a report can quote it, and bounded generously so a slow machine does not make the build
 * flaky. (1.96.0: the other checks of this class belonged to the client sync, the library and the Passives panel, which were removed with the Skill Tree.)
 */
class LineScaleTest {
    private static final int LINES = 300;

    @Test
    void threeHundredLinesLoadFromFilesCleanAndFast() throws IOException {
        Path dir = Files.createTempDirectory("lines_scale");
        try {
            for (int i = 0; i < LINES; i++) Files.writeString(dir.resolve("scale_line_" + i + ".json"), LineFixtures.line(i).toString(), StandardCharsets.UTF_8);
            long t0 = System.nanoTime();
            Map<String, JsonElement> files = new LinkedHashMap<>();
            try (var s = Files.list(dir)) {
                for (Path f : s.sorted().toList()) {
                    String n = f.getFileName().toString();
                    files.put("nusmp:" + n.substring(0, n.length() - 5), com.google.gson.JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
                }
            }
            RouteIndex idx = RouteIndex.build(files, List.of(), RouteIndex.Caps.REGISTRY);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("[scale] " + LINES + " lines read and validated in " + ms + " ms; " + idx.bytes() + " bytes on the wire in all");
            assertEquals(LINES, idx.all().size());
            for (RouteIndex.Entry e : idx.report()) assertTrue(e.clean(), e.id() + ": " + e.problems());
            assertTrue(ms < 20_000, "loading 300 lines took " + ms + " ms");
            assertTrue(idx.bytes() > RouteWire.MAX_TOTAL_BYTES, "the whole set must be bigger than one classic payload, or the registry caps are not what this test is about");
        } finally {
            try (var s = Files.list(dir)) {
                for (Path f : s.toList()) Files.deleteIfExists(f);
            }
            Files.deleteIfExists(dir);
        }
    }
}
