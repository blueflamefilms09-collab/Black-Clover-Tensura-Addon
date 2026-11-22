package com.newuniverse.nusmp.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpiritDiveGateTest {
    @Test
    void lordLevelIsOnlyRequiredBySetting() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/book/GrimoireBook.java"));
        assertTrue(src.contains("SpiritDiveConfig.requireLord() && (spirit == null"));
        assertEquals(false, SpiritDiveConfig.REQUIRE_LORD.getDefault());
    }
}
