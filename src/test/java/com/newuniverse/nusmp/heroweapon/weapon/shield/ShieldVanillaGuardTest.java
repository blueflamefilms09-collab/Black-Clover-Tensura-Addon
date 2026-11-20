package com.newuniverse.nusmp.heroweapon.weapon.shield;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the hero Shield must block like a normal shield (full block, no guard break, shots turned back) while the old Resolve meter stays one setting away. */
class ShieldVanillaGuardTest {
    @Test
    void frontalIsTheHalfPlaneInFrontOfTheLook() {
        assertTrue(ShieldReflect.frontal(0, 1, 0, 5));
        assertTrue(ShieldReflect.frontal(0, 1, 3, 3));
        assertFalse(ShieldReflect.frontal(0, 1, 0, -5));
        assertFalse(ShieldReflect.frontal(0, 1, 4, -1));
        assertTrue(ShieldReflect.frontal(0, 0, 4, -1), "looking straight up counts as met");
    }

    @Test
    void refillMakesTheGuardUnbreakableButKeepsTheCounter() {
        GuardMeter g = new GuardMeter();
        g.block(100, 1000, 20, 3.0, false, 1.0);                       // a huge blow breaks the guard...
        assertTrue(g.broken(101));
        double counter = g.counter();
        g.refill();                                                  // ...and the vanilla guard forgets it at once
        assertFalse(g.broken(101));
        assertEquals(counter, g.counter(), 1e-9);
        assertEquals(1.0, g.share(20, 3.0), 1e-9);
    }

    @Test
    void theLogicAsksForTheSettingAndKeepsTheOldBranch() throws Exception {
        String s = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/heroweapon/weapon/shield/ShieldLogic.java"));
        assertTrue(s.contains("shieldVanillaGuard()"));
        assertTrue(s.contains("r.through()"), "the old Resolve rule is still there");
        String c = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/heroweapon/HeroConfig.java"));
        assertTrue(c.contains("define(\"shieldVanillaGuard\", true)") && c.contains("define(\"shieldReflects\", true)"));
    }
}
