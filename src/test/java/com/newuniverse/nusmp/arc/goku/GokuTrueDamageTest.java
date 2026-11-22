package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 4.107.0: the true part of Goku's blow is never lost to a tiny blow, the shortfall is measured, the power-up multiplies within its cap, and the data tags are in place. */
class GokuTrueDamageTest {
    @Test void floorLiftsATinyBlow() {
        assertEquals(30f, GokuBossMath.trueAmount(100f, 0.3, 1.0, 1000f, 0.0), 1e-3f);          // floor off: the share of the blow (4.103.0)
        assertEquals(30f, GokuBossMath.trueAmount(1f, 0.3, 1.0, 1000f, 0.03), 1e-3f);           // a blow cut to 1: the floor (3% of 1000) is what counts
        assertEquals(12f, GokuBossMath.trueAmount(1f, 0.3, 0.4, 1000f, 0.03), 1e-3f);           // Base form: 40% of the floor
    }

    @Test void floorIsHeldAndBadInputIsSafe() {
        assertEquals(100f, GokuBossMath.trueAmount(0f, 0.3, 1.0, 1000f, 5.0), 1e-3f);          // held to 10%
        assertEquals(0f, GokuBossMath.trueAmount(Float.NaN, Double.NaN, Double.NaN, Float.NaN, Double.NaN), 0f);
    }

    @Test void missingHealthIsMeasured() {
        assertEquals(0f, GokuBossMath.missing(10f, 10f), 0f);
        assertEquals(0f, GokuBossMath.missing(10f, 9.7f), 0f);                                  // within 5%: full
        assertEquals(6f, GokuBossMath.missing(10f, 4f), 1e-4f);
        assertEquals(10f, GokuBossMath.missing(10f, -3f), 1e-4f);
        assertEquals(0f, GokuBossMath.missing(0f, 0f), 0f);
    }

    @Test void powerUpMultipliesWithinTheCap() {
        assertEquals(1.5, GokuBossMath.powered(1.0, 1.5, true, 100.0), 1e-9);
        assertEquals(1.0, GokuBossMath.powered(1.0, 1.5, false, 100.0), 1e-9);
        assertEquals(10.0, GokuBossMath.powered(9.0, 5.0, true, 10.0), 1e-9);
        assertEquals(2.0, GokuBossMath.powered(2.0, Double.NaN, true, 10.0), 1e-9);
        assertEquals(14, GokuBossMath.quickGap(18, true));
        assertEquals(6, GokuBossMath.quickGap(6, true));
        assertEquals(18, GokuBossMath.quickGap(18, false));
    }

    @Test void gokuTrueIsInTheBypassTags() throws Exception {
        for (String f : new String[] {"minecraft/tags/damage_type/bypasses_armor.json", "minecraft/tags/damage_type/bypasses_shield.json", "minecraft/tags/damage_type/bypasses_cooldown.json",
                "minecraft/tags/damage_type/bypasses_invulnerability.json", "tensura/tags/damage_type/bypass_dodge.json"}) {
            String t = Files.readString(Path.of("src/main/resources/data/" + f));
            assertTrue(t.contains("anime_multiverse:goku_true"), f);
        }
    }
}
