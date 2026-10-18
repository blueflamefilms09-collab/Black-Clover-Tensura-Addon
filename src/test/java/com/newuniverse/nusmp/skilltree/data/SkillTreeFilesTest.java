package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the route files the mod ships are written by hand by other builders, and a typo in one of them would otherwise only show up as a log line when a server starts. This test runs the REAL
 * schema and the cross-file rules over every shipped file ({@code data/<namespace>/skill_trees/*.json} in the resources) at BUILD time, so a route with a problem, a clashing book or class, a
 * skill granted twice, or a route too big for the sync packet fails the build instead of a player's session. It asserts on problems, not on warnings (a warning is an oddity that still
 * loads). With no file shipped yet (the folder is missing or empty) it passes and says so, which is the state of this branch before the Fairy Tail routes are merged.
 */
class SkillTreeFilesTest {
    private static final Path DATA = Path.of("src/main/resources/data");

    /** {@code namespace:file name} to its parsed JSON, for every file under {@code data/<namespace>/skill_trees/}. */
    private static Map<String, JsonElement> shipped() throws IOException {
        Map<String, JsonElement> out = new LinkedHashMap<>();
        if (!Files.isDirectory(DATA)) return out;
        List<Path> namespaces;
        try (Stream<Path> s = Files.list(DATA)) {
            namespaces = s.filter(Files::isDirectory).sorted().toList();
        }
        for (Path ns : namespaces) {
            for (String folder : new String[]{"skill_trees", "unique_skills"}) {            // 1.82.0: unique_skills is an alias folder of skill_trees (RouteLoader.ALIAS_DIRECTORY)
                Path dir = ns.resolve(folder);
                if (!Files.isDirectory(dir)) continue;
                List<Path> files;
                try (Stream<Path> s = Files.list(dir)) {
                    files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
                }
                for (Path f : files) {
                    String name = f.getFileName().toString();
                    out.putIfAbsent(ns.getFileName() + ":" + name.substring(0, name.length() - 5), JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
                }
            }
        }
        return out;
    }

    @Test
    void everyShippedRouteFileLoadsWithNoProblem() throws IOException {
        Map<String, JsonElement> files = shipped();
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : files.entrySet()) {
            RouteSchema.Parsed p = RouteSchema.parse(e.getKey(), e.getValue());
            if (!p.clean()) bad.add(e.getKey() + ": " + String.join(" | ", p.problems()));
        }
        assertTrue(bad.isEmpty(), "shipped route files with problems: " + String.join("; ", bad));
    }

    @Test
    void theShippedRoutesTogetherObeyTheCrossFileRules() throws IOException {
        Map<String, JsonElement> files = shipped();
        RouteIndex idx = RouteIndex.build(files, List.of(), RouteIndex.Caps.REGISTRY);                 // 1.82.0: the registry's caps (283 lines), not one payload's
        List<String> bad = new ArrayList<>();
        for (RouteIndex.Entry e : idx.report()) if (!e.clean()) bad.add(e.id() + ": " + String.join(" | ", e.problems()));
        assertTrue(bad.isEmpty(), "shipped routes the registry would refuse or trim: " + String.join("; ", bad));
        assertEquals(files.size(), idx.all().size(), "every shipped file becomes a route (an empty folder is 0 of 0)");
        assertTrue(idx.bytes() <= RouteIndex.Caps.REGISTRY.maxTotalBytes());
    }

    @Test
    void theBooksAndClassesOfTheShippedRoutesAreDistinctEvenAcrossNamespaces() throws IOException {
        RouteIndex idx = RouteIndex.build(shipped(), List.of(), RouteIndex.Caps.REGISTRY);
        List<String> books = new ArrayList<>(), classes = new ArrayList<>();
        for (SkillRoute r : idx.all()) {
            assertTrue(!books.contains(r.book()), "book " + r.book() + " is used twice");
            assertTrue(!classes.contains(r.classId()), "class " + r.classId() + " is used twice");
            books.add(r.book());
            classes.add(r.classId());
        }
    }
}
