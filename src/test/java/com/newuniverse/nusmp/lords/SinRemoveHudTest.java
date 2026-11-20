package com.newuniverse.nusmp.lords;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 4.64.1: removing a Sin weapon's skill also takes its side bar (the Lord mastery HUD) away, and restoring brings it back. */
class SinRemoveHudTest {
    private static String read(String f) throws Exception {
        return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + f));
    }

    @Test
    void theHudIsHiddenWhileTheSkillIsRemovedAndRefreshedOnRemoveAndRestore() throws Exception {
        assertTrue(read("lords/LordService.java").contains("SinSkillService.isOffById(p, sin.id)"));
        String s = read("sinweapons/SinSkillService.java");
        assertTrue(s.contains("LordService.hud(p, sin, true)"));
        assertTrue(s.indexOf("LordService.hud(p, sin, true)") != s.lastIndexOf("LordService.hud(p, sin, true)"), "both remove and restore refresh it");
    }
}
