package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Shinra rework (docs/shinra_rework.md) must keep the old Adolla Burst wheel indexes, stay bounded, and keep every old id registered. */
class ShinraRulesTest {
    @Test
    void theWheelsKeepTheOldIndexesAndAppendTheNewMove() {
        assertArrayEquals(new int[]{FireForceRules.DASH, FireForceRules.KICK, FireForceRules.BURST, ShinraRules.RAPID}, ShinraRules.wheel(true));
        assertArrayEquals(new int[]{FireForceRules.DASH, FireForceRules.KICK, ShinraRules.RAPID}, ShinraRules.wheel(false));
        for (int i = 0; i < 3; i++) assertEquals(i, ShinraRules.move(true, i), "the old Adolla Burst modes keep their positions");
        assertEquals(ShinraRules.RAPID, ShinraRules.move(false, 2));
        assertEquals(ShinraRules.RAPID, ShinraRules.move(true, -1), "a stale index wraps");
        ShinraRules.wheel(true)[0] = 99;
        assertEquals(FireForceRules.DASH, ShinraRules.wheel(true)[0], "the wheel is a copy");
    }

    @Test
    void grinAndLinkThresholds() {
        assertTrue(ShinraRules.grin(34, 100));
        assertFalse(ShinraRules.grin(35, 100));
        assertFalse(ShinraRules.grin(0, 100));
        assertFalse(ShinraRules.grin(10, 0));
        assertTrue(ShinraRules.link(24, 100, Tier.MASTERED));
        assertFalse(ShinraRules.link(24, 100, Tier.BASE), "the link needs a mastered skill");
        assertFalse(ShinraRules.link(25, 100, Tier.ULTIMATE));
        assertEquals(-1, ShinraRules.speedAmp(80, 100, Tier.ULTIMATE));
        assertEquals(0, ShinraRules.speedAmp(30, 100, Tier.BASE));
        assertEquals(1, ShinraRules.speedAmp(20, 100, Tier.MASTERED));
        assertEquals(-1, ShinraRules.strengthAmp(30, 100, Tier.ULTIMATE));
        assertEquals(0, ShinraRules.strengthAmp(20, 100, Tier.MASTERED));
        assertEquals(1, ShinraRules.strengthAmp(20, 100, Tier.ULTIMATE));
        assertEquals(1.0, ShinraRules.cooldownFactor(80, 100, Tier.ULTIMATE));
        assertTrue(ShinraRules.cooldownFactor(20, 100, Tier.MASTERED) < 1.0);
    }

    @Test
    void theUltimateOutclassesAndEverythingIsBounded() {
        assertTrue(ShinraRules.rapidDamage(Tier.ULTIMATE) > ShinraRules.rapidDamage(Tier.MASTERED));
        assertTrue(ShinraRules.rapidDamage(Tier.MASTERED) > ShinraRules.rapidDamage(Tier.BASE));
        assertTrue(ShinraRules.rapidRange(Tier.ULTIMATE) > ShinraRules.rapidRange(Tier.BASE));
        assertTrue(ShinraRules.rapidCost(Tier.ULTIMATE) > ShinraRules.rapidCost(Tier.BASE));
        for (Tier t : Tier.values()) {
            assertTrue(ShinraRules.rapidCooldown(t) >= 40, "no spam: a cooldown of two seconds or more");
            assertTrue(ShinraRules.rapidRange(t) <= 16);
            assertTrue(ShinraRules.rapidWidth() <= 3);
        }
        assertTrue(ShinraRules.maxTargets() <= 16);
        assertTrue(ShinraRules.EFFECT_TICKS <= 100, "the passive effects end within five seconds of the toggle going off");
    }

    @Test
    void theOldIdsAreStillRegisteredAndTheSettingIsAServerSetting() throws IOException {
        String skills = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java"), StandardCharsets.UTF_8);
        for (String id : new String[]{"hell_walker", "hell_conqueror", "devils_footprints", "adolla_burst"}) assertTrue(skills.contains("\"" + id + "\""), id + " stays registered");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("define(\"shinraRework\", true)"), "the setting that restores the old behaviour");
        String walker = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skill/HellWalkerSkill.java"), StandardCharsets.UTF_8);
        assertFalse(walker.contains("net.minecraft.client") || walker.contains("Minecraft.getInstance"), "server safe");
        assertFalse(walker.contains("static Map") || walker.contains("static final Map") || walker.contains("HashMap"), "no per-player state outside the skill instance");
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(smoke.contains(",shinra,") && smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the self-test prefix is in the smoke list, the tail stays");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java"), StandardCharsets.UTF_8).contains("ShinraSelfTests.register()"));
    }
}
