package com.newuniverse.nusmp.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The catalogue of compatible mods: unique, honest, and the required ones match the mod file. */
class CompatCatalogTest {
    @Test
    void idsAreUniqueAndEveryEntryHasText() {
        Set<String> seen = new HashSet<>();
        for (CompatCatalog.Entry e : CompatCatalog.entries()) {
            assertTrue(seen.add(e.modId()), "duplicate " + e.modId());
            assertFalse(e.name().isBlank());
            assertFalse(e.line().isBlank());
        }
    }

    @Test
    void requiredEntriesAreExactlyTheRequiredDependenciesOfTheModFile() throws IOException {
        String toml;
        try (InputStream in = CompatCatalogTest.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
            assertNotNull(in, "the mod file is on the test classpath");
            toml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Set<String> required = new HashSet<>();
        Matcher m = Pattern.compile("(?s)\\[\\[dependencies\\.nusmp]]\\s*modId=\"([^\"]+)\"\\s*type=\"([^\"]+)\"").matcher(toml);
        while (m.find()) if (m.group(2).equals("required")) required.add(m.group(1));
        Set<String> listed = new HashSet<>();
        for (CompatCatalog.Entry e : CompatCatalog.of(CompatCatalog.Kind.REQUIRED)) listed.add(e.modId());
        assertEquals(required, listed);
        Set<String> optional = new HashSet<>();
        m = Pattern.compile("(?s)\\[\\[dependencies\\.nusmp]]\\s*modId=\"([^\"]+)\"\\s*type=\"([^\"]+)\"").matcher(toml);
        while (m.find()) if (m.group(2).equals("optional")) optional.add(m.group(1));
        for (String id : optional) assertTrue(CompatCatalog.entries().stream().anyMatch(e -> e.modId().equals(id)), "optional " + id + " must be in the codex");
    }

    @Test
    void statusesFollowThePredicate() {
        List<CompatCatalog.Status> none = CompatCatalog.statuses(null);
        assertEquals(CompatCatalog.entries().size(), none.size());
        assertTrue(none.stream().noneMatch(CompatCatalog.Status::loaded));
        List<CompatCatalog.Status> some = CompatCatalog.statuses("curios"::equals);
        assertEquals(1, some.stream().filter(CompatCatalog.Status::loaded).count());
        assertTrue(CompatCatalog.packActive(CompatCatalog.TNI_MODID::equals));
        assertFalse(CompatCatalog.packActive("curios"::equals));
        assertFalse(CompatCatalog.packActive(null));
    }

    @Test
    void theBridgeEntryIsTheOtherMod() {
        List<CompatCatalog.Entry> bridge = CompatCatalog.of(CompatCatalog.Kind.BRIDGE);
        assertEquals(1, bridge.size());
        assertEquals("tensuranoindex", bridge.get(0).modId());
    }
}
