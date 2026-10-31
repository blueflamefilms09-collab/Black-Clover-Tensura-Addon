package com.newuniverse.nusmp.shishou;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 3.43.0 fast flurry: the numbers, the setting that restores 3.42.0, and the wiring (read from the repo's files). */
class ShishouFastFlurry343Test {
    private static String code(String file) throws IOException {
        String s = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/shishou/" + file));
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    void theFastCooldownRunsFromEightToThree() {
        assertEquals(8, ShishouHunt.strikeCooldownFast(1));
        assertEquals(8, ShishouHunt.strikeCooldownFast(10));
        assertEquals(3, ShishouHunt.strikeCooldownFast(100));
        for (int l = 2; l <= 100; l++) {
            assertTrue(ShishouHunt.strikeCooldownFast(l) <= ShishouHunt.strikeCooldownFast(l - 1), "never slower with a higher level");
            assertTrue(ShishouHunt.strikeCooldownFast(l) >= ShishouHunt.FAST_COOLDOWN_MIN && ShishouHunt.strikeCooldownFast(l) <= ShishouHunt.FAST_COOLDOWN_MAX);
        }
    }

    @Test
    void theFastFlurryIsFasterAtEveryLevelButStaysBounded() {
        for (int l = 1; l <= 100; l++) {
            assertTrue(ShishouHunt.strikeCooldownFast(l) < ShishouHunt.strikeCooldown(l), "faster than 3.42.0 at level " + l);
            assertTrue(ShishouHunt.strikesPerSecond(ShishouHunt.strikeCooldownFast(l)) <= 20.0 / 3, "at most 6.67 strikes a second");
        }
        assertEquals(20.0 / 3, ShishouHunt.strikesPerSecond(3), 1.0e-9);
    }

    @Test
    void theDamageRiseIsBetweenOnePointTwoAndOnePointFour() {
        for (int l = 1; l <= 100; l++) {
            double f = ShishouHunt.dpsFactor(l);
            assertTrue(f > 1.2 && f < 1.45, "damage a second x" + f + " at level " + l);
        }
        assertEquals(0.7, ShishouHunt.strikeShare(true), 1.0e-9);
        assertEquals(1.0, ShishouHunt.strikeShare(false), 1.0e-9);
    }

    @Test
    void theSettingOffIsExactlyThe342Numbers() {
        for (int l = 1; l <= 100; l++) assertEquals(ShishouHunt.strikeCooldown(l), ShishouHunt.strikeCooldown(l, false));
        assertEquals(ShishouHunt.BOUNCE_TICKS, ShishouHunt.bounceTicks(false));
        double[] old = ShishouHunt.bounce(3.0, 1.0, -4.0), off = ShishouHunt.bounce(3.0, 1.0, -4.0, false);
        assertArrayEquals(old, off, 1.0e-12);
    }

    @Test
    void theFastBounceIsSnappierButKeepsDirectionAndLift() {
        assertTrue(ShishouHunt.bounceTicks(true) < ShishouHunt.bounceTicks(false) && ShishouHunt.bounceTicks(true) >= 1);
        double[] v = ShishouHunt.bounce(3.0, 0.0, 4.0, true);
        assertTrue(v[0] > 0 && v[2] > 0, "still away from the target");
        assertEquals(ShishouHunt.FAST_BOUNCE_SPEED, Math.hypot(v[0], v[2]), 1.0e-9);
        assertEquals(ShishouHunt.BOUNCE_LIFT, v[1], 1.0e-9, "the lift is kept");
        assertTrue(ShishouHunt.FAST_BOUNCE_SPEED < ShishouHunt.CHARGE_START + ShishouHunt.CHARGE_ACCEL * 3, "slower than the charge");
        double[] up = ShishouHunt.bounce(0, 0, 0, true);
        assertEquals(0.0, up[0]);
        assertTrue(up[1] > 0, "a zero offset still bounces up");
    }

    @Test
    void theSettingIsOnByDefaultAndWired() throws IOException {
        String cfg = code("ShishouConfig.java");
        assertTrue(cfg.contains("\"flurryFast\", true"), "a server setting, on by default");
        assertTrue(cfg.contains("flurryFast()"), "an accessor");
        String e = code("ShishouEntity.java");
        assertTrue(e.contains("ShishouConfig.flurryFast()"), "the entity reads the setting");
        assertTrue(e.contains("strikeCooldown(swordLevel(), ShishouConfig.flurryFast())"), "cooldown for the setting");
        assertTrue(code("ShishouService.java").contains("ShishouHunt.strikeShare(ShishouConfig.flurryFast())"), "damage share for the setting");
    }

    @Test
    void theWildManaStillPaysForTheSameNumberOfStrikes() {
        assertEquals(200, (int) (ShishouRules.WILD_MANA / ShishouRules.WILD_STRIKE_MANA));
    }
}
