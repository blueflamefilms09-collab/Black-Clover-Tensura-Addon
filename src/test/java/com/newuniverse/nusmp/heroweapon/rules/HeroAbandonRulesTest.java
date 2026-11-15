package com.newuniverse.nusmp.heroweapon.rules;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 3.72.0: giving up your own hero weapon takes a question and a confirmation inside 30 seconds; the server side is bounded, UUID-keyed and refuses while the curse holds the wielder. */
class HeroAbandonRulesTest {
    private static String read(String p) throws IOException { return Files.readString(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "heroweapon", p)); }

    @Test
    void aConfirmationNeedsAnOpenQuestion() {
        assertFalse(HeroAbandonRules.confirmed(null, 100), "no question, no confirmation");
        assertTrue(HeroAbandonRules.confirmed(100L, 100), "the same tick");
        assertTrue(HeroAbandonRules.confirmed(100L, 100 + HeroAbandonRules.CONFIRM_TICKS), "the last tick of the window");
        assertFalse(HeroAbandonRules.confirmed(100L, 101 + HeroAbandonRules.CONFIRM_TICKS), "too late");
        assertFalse(HeroAbandonRules.confirmed(100L, 99), "a clock that went back");
        assertEquals(600, HeroAbandonRules.CONFIRM_TICKS, "30 seconds");
    }

    @Test
    void theServerSideIsBoundedKeyedByUuidAndRefusesTheCursedAndTheSwitchedOff() throws IOException {
        String c = read("bond/HeroAbandon.java");
        assertTrue(c.contains("Map<UUID, Long>"), "keyed by UUID");
        assertTrue(c.contains("MAX_OPEN") && c.contains("prune("), "capped and pruned");
        assertTrue(c.contains("HeroCurse.locked(p)") && c.contains("HeroConfig.abandonAllowed()") && c.contains("HeroBonds.release(p)"), "the curse lock, the setting and the shared release path");
        assertFalse(c.contains("net.minecraft.client") || c.contains("Minecraft.getInstance"), "no client class");
        String cmd = read("HeroCommand.java");
        assertTrue(cmd.contains("Commands.literal(\"abandon\")") && cmd.contains("Commands.literal(\"confirm\")"), "the command");
        assertTrue(read("HeroConfig.java").contains("\"abandonAllowed\", true"), "a server setting, on by default");
    }
}
