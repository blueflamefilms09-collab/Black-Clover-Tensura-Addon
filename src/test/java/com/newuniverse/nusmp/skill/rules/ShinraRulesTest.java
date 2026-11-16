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
    void theCanonWheelAppendsAndUnlocksByTier() {
        assertArrayEquals(new int[]{0, 1, 3, 4, 5, 6}, ShinraRules.wheel(false, true));
        assertArrayEquals(ShinraRules.wheel(true), java.util.Arrays.copyOf(ShinraRules.wheel(true, true), 4), "the first four positions never move");
        assertArrayEquals(ShinraRules.wheel(false), ShinraRules.wheel(false, false));
        assertEquals(ShinraRules.GRACE, ShinraRules.move(true, true, 7));
        assertEquals(ShinraRules.GRACE, ShinraRules.move(true, true, -1), "a stale index wraps");
        assertTrue(ShinraRules.unlocked(ShinraRules.LINK, Tier.BASE), "the Link comes early in the story");
        assertTrue(ShinraRules.unlocked(ShinraRules.RAPID_JET, Tier.BASE));
        assertFalse(ShinraRules.unlocked(ShinraRules.HELLFIRE, Tier.BASE));
        assertTrue(ShinraRules.unlocked(ShinraRules.HELLFIRE, Tier.MASTERED));
        assertFalse(ShinraRules.unlocked(ShinraRules.GRACE, Tier.MASTERED), "Grace is the Ultimate");
        assertTrue(ShinraRules.unlocked(ShinraRules.GRACE, Tier.ULTIMATE_MASTERED));
    }

    @Test
    void hysteriaRapidGraceAndOverheat() {
        assertFalse(ShinraRules.hysteria(20, 100, Tier.BASE), "learned later: mastered");
        assertTrue(ShinraRules.hysteria(20, 100, Tier.MASTERED));
        assertFalse(ShinraRules.hysteria(40, 100, Tier.MASTERED));
        assertTrue(ShinraRules.hysteria(100, 100, Tier.ULTIMATE), "the Ultimate uses it permanently");
        assertFalse(ShinraRules.hysteria(0, 100, Tier.ULTIMATE));
        assertEquals(0, ShinraRules.hysteriaSpeedAmp(20, 100, Tier.MASTERED));
        assertEquals(1, ShinraRules.hysteriaStrengthAmp(100, 100, Tier.ULTIMATE));
        assertEquals(-1, ShinraRules.hysteriaSpeedAmp(100, 100, Tier.MASTERED));
        assertTrue(ShinraRules.involuntaryLink(20, 100));
        assertFalse(ShinraRules.involuntaryLink(30, 100));
        assertTrue(ShinraRules.rapidActive(10, 11));
        assertFalse(ShinraRules.rapidActive(11, 11));
        assertFalse(ShinraRules.rapidActive(5, 0));
        assertTrue(ShinraRules.rapidFireFactor() < 1.0 && ShinraRules.rapidDashFactor() > 1.0 && ShinraRules.rapidKickDamageFactor() > 1.0, "Rapid: less fire, more reach, a harder kick");
        assertTrue(ShinraRules.rapidJetSpeedAmp(Tier.ULTIMATE) > ShinraRules.rapidJetSpeedAmp(Tier.BASE));
        assertTrue(ShinraRules.hellfireDamage(Tier.ULTIMATE) > ShinraRules.hellfireDamage(Tier.MASTERED));
        assertTrue(ShinraRules.hellfireBlastRadius(Tier.ULTIMATE) > ShinraRules.hellfireBlastRadius(Tier.MASTERED));
        assertTrue(ShinraRules.linkRadius(Tier.ULTIMATE) > ShinraRules.linkRadius(Tier.BASE));
        assertTrue(ShinraRules.graceTicks(Tier.ULTIMATE_MASTERED) > ShinraRules.graceTicks(Tier.ULTIMATE));
        assertTrue(ShinraRules.graceActive(5, 6) && !ShinraRules.graceActive(6, 6));
        assertTrue(ShinraRules.overheatDue(100, 100) && ShinraRules.overheatDue(150, 100));
        assertFalse(ShinraRules.overheatDue(99, 100), "not ended yet");
        assertFalse(ShinraRules.overheatDue(5000, 100), "a logout in between pays nothing");
        assertFalse(ShinraRules.overheatDue(100, 0));
    }

    @Test
    void canonPassIsBounded() {
        for (Tier t : Tier.values()) {
            assertTrue(ShinraRules.rapidJetTicks(t) <= 400);
            assertTrue(ShinraRules.rapidJetCooldown(t) >= 100, "Rapid is not a spam move");
            assertTrue(ShinraRules.hellfireRange(t) <= 16);
            assertTrue(ShinraRules.hellfireCooldown(t) >= 60);
            assertTrue(ShinraRules.graceTicks(t) <= 400);
            assertTrue(ShinraRules.linkRadius(t) <= 48, "the target helper caps the radius at 48");
        }
        assertTrue(ShinraRules.graceCooldown() > 10 * ShinraRules.graceTicks(Tier.ULTIMATE_MASTERED), "the Grace is rare");
        assertTrue(ShinraRules.linkMaxSensed() <= 24);
        assertTrue(ShinraRules.linkWindowTicks() <= 1200 && ShinraRules.passiveLinkWindowTicks() <= ShinraRules.linkWindowTicks());
        assertTrue(ShinraRules.overheatTicks() > 0 && ShinraRules.overheatTicks() <= 400, "the Ignition Limit is a short penalty");
    }

    @Test
    void theOldIdsAreStillRegisteredAndTheSettingIsAServerSetting() throws IOException {
        String skills = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java"), StandardCharsets.UTF_8);
        for (String id : new String[]{"hell_walker", "hell_conqueror", "devils_footprints", "adolla_burst"}) assertTrue(skills.contains("\"" + id + "\""), id + " stays registered");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("define(\"shinraRework\", true)"), "the setting that restores the old behaviour");
        assertTrue(cfg.contains("define(\"shinraCanon\", true)"), "the setting that restores the first rework");
        String walker = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skill/HellWalkerSkill.java"), StandardCharsets.UTF_8);
        assertFalse(walker.contains("net.minecraft.client") || walker.contains("Minecraft.getInstance"), "server safe");
        assertTrue(walker.contains("getOrCreateTag().putLong(RAPID_UNTIL") && walker.contains("GRACE_UNTIL"), "Rapid / Link / Grace state lives in the skill instance");
        assertFalse(walker.contains("static Map") || walker.contains("static final Map") || walker.contains("HashMap"), "no per-player state outside the skill instance");
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(smoke.contains(",shinra,") && smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the self-test prefix is in the smoke list, the tail stays");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java"), StandardCharsets.UTF_8).contains("ShinraSelfTests.register()"));
    }
}
