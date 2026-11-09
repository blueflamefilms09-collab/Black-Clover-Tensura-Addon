package com.newuniverse.nusmp.mobfix;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the guard only works if it is wired (the join event, the self-test, the smoke list), is server only, and stays cheap; nothing can be profiled here, so these are pinned by reading the source. */
class SpellcasterGuardSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void theGuardIsWiredAndOnlyTouchesSpellcastersOnTheServer() throws IOException {
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("mobfix.SpellcasterGuard.register("), "the mod constructor must register the guard");
        String g = read("src/main/java/com/newuniverse/nusmp/mobfix/SpellcasterGuard.java");
        assertTrue(g.contains("e.getLevel().isClientSide()") && g.contains("instanceof SpellcasterIllager"), "server side only, spellcasters only");
        assertFalse(g.contains("net.minecraft.client") || g.contains("com.newuniverse.nusmp.client"), "no client class");
        assertTrue(g.contains("SpellcasterGuardSelfTests.register()"), "the self-test is registered with the guard");
        assertFalse(g.contains("getEntitiesOfClass") || g.contains("addFreshEntity"), "the guard scans nothing and spawns nothing");
    }

    @Test
    void theSmokeListHasTheSelfTestAndKeepsTierLast() throws IOException {
        String y = read(".github/workflows/smoke.yml");
        assertTrue(y.contains(",mobfix.spellcaster,"), "the smoke list must expect the self-test");
        assertTrue(y.contains(",tier\""), "tier must stay the last entry of the smoke list");
    }
}
