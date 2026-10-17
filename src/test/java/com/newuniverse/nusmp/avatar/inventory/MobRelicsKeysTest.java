package com.newuniverse.nusmp.avatar.inventory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Avatar's trinket slots are a view of the relic pack that {@code MobRelics} keeps for a named mob, and {@code AvatarRelicBridge} repeats the two persistent-data
 * keys of that class because the originals are private. If the relic system ever renames them the trinket slots would silently stop working, so this test reads the
 * sources of both classes and fails the moment the keys differ. (It reads text, not classes: the bridge imports game classes that a pure test cannot load.)
 */
class MobRelicsKeysTest {
    private static String read(String path) throws IOException {
        Path p = Path.of(path);
        assertTrue(Files.exists(p), "run the tests from the project root: " + p.toAbsolutePath());
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static String constant(String source, String name) {
        Matcher m = Pattern.compile("\\b" + name + "\\s*=\\s*\"([^\"]+)\"").matcher(source);
        assertTrue(m.find(), "no constant " + name);
        return m.group(1);
    }

    private static final String RELICS = "src/main/java/com/newuniverse/nusmp/relics/MobRelics.java";
    private static final String BRIDGE = "src/main/java/com/newuniverse/nusmp/avatar/inventory/AvatarRelicBridge.java";

    @Test
    void thePackKeyIsTheRelicSystemsOwn() throws IOException {
        assertEquals(constant(read(RELICS), "PACK"), constant(read(BRIDGE), "PACK"), "MobRelics stores its pack under another key now");
    }

    @Test
    void theMarkerKeyIsTheRelicSystemsOwn() throws IOException {
        assertEquals(constant(read(RELICS), "GIVER"), constant(read(BRIDGE), "GIVER"), "MobRelics marks its carriers with another key now");
    }

    @Test
    void theKeysAreWhatTheDocumentationSays() throws IOException {
        assertEquals("nusmp_mob_relics", constant(read(BRIDGE), "PACK"));
        assertEquals("nusmp_mob_relic_giver", constant(read(BRIDGE), "GIVER"));
    }
}
