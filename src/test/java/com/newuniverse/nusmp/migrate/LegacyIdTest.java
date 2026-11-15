package com.newuniverse.nusmp.migrate;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The id change of 3.84.0: the old namespace reads as the new one, everything else is untouched, and the resource folders moved. */
class LegacyIdTest {
    @Test
    void oldNamespaceBecomesNew() {
        assertEquals("anime_multiverse", LegacyId.namespace(LegacyId.OLD));
        assertEquals("minecraft", LegacyId.namespace("minecraft"));
        assertEquals("tensura", LegacyId.namespace("tensura"));
        assertEquals("anime_multiverse", LegacyId.namespace("anime_multiverse"));
        assertNull(LegacyId.namespace(null));
        assertEquals(LegacyId.OLD + "_pack", LegacyId.namespace(LegacyId.OLD + "_pack"), "a longer namespace is not the old id");
    }

    @Test
    void foldersAndConstantsMoved() throws Exception {
        assertTrue(Files.isDirectory(Path.of("src/main/resources/assets/anime_multiverse")));
        assertTrue(Files.isDirectory(Path.of("src/main/resources/data/anime_multiverse")));
        assertTrue(!Files.exists(Path.of("src/main/resources/assets/" + LegacyId.OLD)), "the old assets folder is gone");
        assertTrue(!Files.exists(Path.of("src/main/resources/data/" + LegacyId.OLD)), "the old data folder is gone");
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        assertTrue(src.contains("MODID = \"anime_multiverse\""));
        String toml = Files.readString(Path.of("src/main/resources/META-INF/neoforge.mods.toml"));
        assertTrue(toml.contains("modId=\"anime_multiverse\""));
    }
}
