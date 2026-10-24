package com.newuniverse.nusmp.shishou;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 3.2.0 flurry: the bounce after a strike is away from the target, short, bounded, behind a server setting, and the entity attributes are the owner's numbers. */
class ShishouFlurryTest {
    @Test
    void bounceIsAwayFromTheTarget() {
        double[] v = ShishouHunt.bounce(3.0, 0.0, 4.0);
        assertTrue(v[0] > 0 && v[2] > 0, "away along (dx, dz)");
        assertEquals(ShishouHunt.BOUNCE_SPEED, Math.hypot(v[0], v[2]), 1.0e-9);
        assertEquals(ShishouHunt.BOUNCE_LIFT, v[1], 1.0e-9);
    }

    @Test
    void zeroOffsetBouncesUp() {
        double[] v = ShishouHunt.bounce(0, 0, 0);
        assertEquals(0.0, v[0]);
        assertEquals(ShishouHunt.BOUNCE_SPEED, v[1]);
    }

    @Test
    void bounceIsShortAndSlowerThanTheCharge() {
        assertTrue(ShishouHunt.BOUNCE_TICKS >= 1 && ShishouHunt.BOUNCE_TICKS <= 6);
        assertTrue(ShishouHunt.BOUNCE_SPEED < ShishouHunt.CHARGE_START + ShishouHunt.CHARGE_ACCEL * 3);
    }

    @Test
    void sourceWiring() throws Exception {
        String e = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/shishou/ShishouEntity.java"));
        assertTrue(e.contains("add(Attributes.FLYING_SPEED, 1.5)") && e.contains("add(Attributes.MOVEMENT_SPEED, 0.8)"));
        assertTrue(e.contains("add(Attributes.ATTACK_DAMAGE, 14.0)") && e.contains("add(Attributes.FOLLOW_RANGE, 64.0)"));
        assertTrue(e.contains("ShishouConfig.flurryBounce()"), "behind the server setting");
        String g = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/shishou/ShishouGoals.java"));
        assertFalse(g.contains("MeleeAttackGoal"), "no vanilla melee goal for a flying sword");
        assertTrue(g.contains("bounceOff"));
    }
}
