package com.newuniverse.nusmp.skilltree.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Ako's Evil Skill Tree syncs every {@code data/<namespace>/skill_trees/*.json} of every mod to the client as one string, and its limit is 1,048,576 characters. This mod ships 284 skill-tree
 * files (5.9 MB), which made that string 2.9 million long and the world impossible to join (3.30.0). The files live in the alias folder {@code unique_skills} (read by {@link RouteLoader} as before);
 * this test keeps any file from coming back into {@code skill_trees}.
 */
class ShippedFolderTest {
    private static final Path DATA = Path.of("src/main/resources/data");

    @Test
    void noShippedFileIsInTheSharedSkillTreesFolder() throws IOException {
        if (!Files.isDirectory(DATA)) return;
        try (Stream<Path> ns = Files.list(DATA)) {
            for (Path n : ns.filter(Files::isDirectory).toList()) {
                Path shared = n.resolve(RouteLoader.DIRECTORY);
                if (!Files.isDirectory(shared)) continue;
                try (Stream<Path> f = Files.list(shared)) {
                    List<Path> files = f.toList();
                    assertTrue(files.isEmpty(), shared + " must stay empty (another mod syncs it as one string); put routes in " + RouteLoader.SHIPPED_DIRECTORY + ": " + files.size() + " file(s)");
                }
            }
        }
    }

    @Test
    void theRoutesAreInTheAliasFolder() throws IOException {
        assertEquals("unique_skills", RouteLoader.SHIPPED_DIRECTORY);
        Path dir = DATA.resolve("nusmp").resolve(RouteLoader.SHIPPED_DIRECTORY);
        if (!Files.isDirectory(DATA)) return;
        assertTrue(Files.isDirectory(dir), "the routes folder is missing");
        try (Stream<Path> f = Files.list(dir)) {
            assertFalse(f.filter(p -> p.toString().endsWith(".json")).findAny().isEmpty());
        }
    }
}
