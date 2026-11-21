package com.newuniverse.nusmp.arc.goku;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 4.96.0: every Goku form has its own bar (like Lucifero's phases): the size factor, and the wiring behind its setting. */
class GokuFormBarTest {
    @Test
    void theBarFactorIsTheShareOnlyWhenBarsAreOn() {
        assertEquals(1.0, GokuBossMath.barFactor(false, 0.4), 1e-9);
        assertEquals(0.4, GokuBossMath.barFactor(true, 0.4), 1e-9);
        assertEquals(1.0, GokuBossMath.barFactor(true, 7.0), 1e-9);
        assertEquals(0.05, GokuBossMath.barFactor(true, 0.0), 1e-9);
    }

    @Test
    void theBarsAreWiredThroughDieAndRefillOnTheNewForm() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/goku/GokuBoss.java"));
        assertTrue(src.contains("if (!level().isClientSide && breakBar()) return;"), "die() must try the form bar first");
        assertTrue(src.contains("if (barBreak) refillBar();"), "the new form must refill the bar");
        assertTrue(src.contains("if (barBreak && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;"), "no damage while powering up");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/goku/GokuBossConfig.java"));
        assertTrue(cfg.contains("\"formBars\"") && cfg.contains("\"formBarShare\", 0.4"));
    }
}
