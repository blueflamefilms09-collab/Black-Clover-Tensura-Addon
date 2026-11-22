package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 4.103.0: Goku's blows are split into normal, true and spiritual parts; the data files that make the two types real exist. */
class GokuSplitDamageTest {
    @Test void partsAddUpToTheBlow() {
        float[] p = GokuBossMath.split(100f, 0.3, 0.2);
        assertEquals(50f, p[0], 1e-3f);
        assertEquals(30f, p[1], 1e-3f);
        assertEquals(20f, p[2], 1e-3f);
        assertEquals(100f, p[0] + p[1] + p[2], 1e-3f);
    }

    @Test void sharesAreHeldAndBadInputIsSafe() {
        float[] p = GokuBossMath.split(100f, 0.8, 0.8);                      // together 1.6 -> held to 0.9
        assertEquals(10f, p[0], 1e-3f);
        assertEquals(100f, p[0] + p[1] + p[2], 1e-3f);
        float[] q = GokuBossMath.split(Float.NaN, Double.NaN, -1);
        assertEquals(0f, q[0] + q[1] + q[2], 0f);
        float[] r = GokuBossMath.split(40f, Double.NaN, Double.NaN);
        assertEquals(40f, r[0], 1e-3f);
    }

    @Test void theShareGrowsWithTheForm() {
        assertEquals(0.4, GokuBossMath.splitScale(0, 9), 1e-9);
        assertEquals(1.0, GokuBossMath.splitScale(9, 9), 1e-9);
        assertTrue(GokuBossMath.splitScale(5, 9) > GokuBossMath.splitScale(4, 9));
        assertEquals(1.0, GokuBossMath.splitScale(99, 9), 1e-9);
    }

    @Test void theTwoDamageTypesAreInTheirTags() throws Exception {
        Path res = Path.of("src/main/resources/data");
        for (String t : new String[] {"goku_true", "goku_spirit"}) assertTrue(Files.exists(res.resolve("anime_multiverse/damage_type/" + t + ".json")), t);
        assertTrue(Files.readString(res.resolve("minecraft/tags/damage_type/bypasses_armor.json")).contains("anime_multiverse:goku_true"));
        assertTrue(Files.readString(res.resolve("minecraft/tags/damage_type/bypasses_resistance.json")).contains("anime_multiverse:goku_true"));
        assertTrue(Files.readString(res.resolve("tensura/tags/damage_type/is_spiritual.json")).contains("anime_multiverse:goku_spirit"));
        assertTrue(Files.readString(res.resolve("tensura/tags/damage_type/bypass_dodge.json")).contains("anime_multiverse:goku_spirit"));
    }
}
