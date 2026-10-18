package com.newuniverse.nusmp.elite.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.world.rules.BiomeFactors;
import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure density model: target, depth, stepping, event decay, quantisation, bands, and the biome table file. */
class MagiculeModelTest {
    @Test
    void targetIsTheSumClampedToOne() {
        assertEquals(0.10, MagiculeModel.target(0.10, 0.0, 0, 0.0, 0.0), 1e-9);
        assertEquals(0.40, MagiculeModel.target(0.10, 1.0, 0, 0.0, 0.0), 1e-9);
        assertEquals(1.0, MagiculeModel.target(0.10, 1.0, 1000, 1.0, 0.8), 1e-9, "everything at once is clamped");
        assertEquals(0.0, MagiculeModel.target(0.10, 0.0, 0, 0.0, -0.5), 1e-9, "a negative event cannot go under 0");
    }

    @Test
    void crystalsAndMothersAreCapped() {
        assertEquals(0.05, MagiculeModel.crystalBonus(5, 0.0), 1e-9);
        assertEquals(MagiculeModel.CRYSTAL_CAP + MagiculeModel.MOTHER_CAP, MagiculeModel.crystalBonus(100000, 100.0), 1e-9);
        assertEquals(0.0, MagiculeModel.crystalBonus(-5, -1.0), 1e-9, "negative inputs give nothing");
    }

    @Test
    void depthBonusRampsUnderSeaLevel() {
        assertEquals(0.0, MagiculeModel.depthBonus(100, 63), 1e-9);
        assertEquals(0.0, MagiculeModel.depthBonus(63, 63), 1e-9);
        assertEquals(MagiculeModel.DEPTH_BONUS / 2.0, MagiculeModel.depthBonus(63 - 48, 63), 1e-9);
        assertEquals(MagiculeModel.DEPTH_BONUS, MagiculeModel.depthBonus(-300, 63), 1e-9);
        assertEquals(1.0, MagiculeModel.effective(0.95, -64, 63), 1e-9, "stored plus depth never passes 1");
    }

    @Test
    void stepMovesTowardTheTargetWithoutOvershoot() {
        assertEquals(0.31, MagiculeModel.step(0.30, 0.90, 30, 0.02, 0.10), 1e-9, "half a minute of 0.02 per minute");
        assertEquals(0.90, MagiculeModel.step(0.89, 0.90, 600, 0.02, 0.10), 1e-9, "stops at the target");
        assertEquals(0.20, MagiculeModel.step(0.30, 0.0, 60, 0.02, 0.10), 1e-9, "falling is faster");
        assertEquals(0.30, MagiculeModel.step(0.30, 0.30, 600, 0.02, 0.10), 1e-9);
        assertEquals(0.30, MagiculeModel.step(0.30, 0.90, 0, 0.02, 0.10), 1e-9, "no time, no change");
        assertEquals(0.30, MagiculeModel.step(0.30, 0.90, -5, 0.02, 0.10), 1e-9, "a clock that went back changes nothing");
    }

    @Test
    void eventDecaysTowardZeroAndStopsThere() {
        assertEquals(0.4, MagiculeModel.decayEvent(0.5, 60, 0.1), 1e-9);
        assertEquals(0.0, MagiculeModel.decayEvent(0.05, 600, 0.1), 1e-9);
        assertEquals(-0.4, MagiculeModel.decayEvent(-0.5, 60, 0.1), 1e-9);
        assertEquals(0.0, MagiculeModel.decayEvent(-0.05, 600, 0.1), 1e-9);
        assertEquals(MagiculeModel.EVENT_MAX, MagiculeModel.decayEvent(5.0, 0, 0.1), 1e-9, "an out of range event is clamped");
    }

    @Test
    void quantisationRoundTripsWithinHalfAStep() {
        for (int i = 0; i <= 100; i++) {
            double d = i / 100.0;
            assertEquals(d, MagiculeModel.dequantize(MagiculeModel.quantize(d)), 0.5 / 255.0 + 1e-9);
        }
        assertEquals(0, MagiculeModel.quantize(-3));
        assertEquals(255, MagiculeModel.quantize(7));
        assertEquals(255, MagiculeModel.quantize(Double.NaN) + 255, "NaN is 0");
    }

    @Test
    void syncThrottleNeedsAChangeOrAFirstSend() {
        assertTrue(MagiculeModel.needsSync(-1, 100, 6));
        assertFalse(MagiculeModel.needsSync(100, 104, 6));
        assertTrue(MagiculeModel.needsSync(100, 106, 6));
        assertTrue(MagiculeModel.needsSync(100, 90, 6));
    }

    @Test
    void bandsFollowTheThresholds() {
        assertEquals(MagiculeModel.Band.THIN, MagiculeModel.Band.of(0.0));
        assertEquals(MagiculeModel.Band.NORMAL, MagiculeModel.Band.of(0.2));
        assertEquals(MagiculeModel.Band.RICH, MagiculeModel.Band.of(0.5));
        assertEquals(MagiculeModel.Band.DENSE, MagiculeModel.Band.of(0.7));
        assertEquals(MagiculeModel.Band.OVERFLOWING, MagiculeModel.Band.of(1.0));
    }

    @Test
    void theShippedBiomeTableGivesSensibleAnswers() throws Exception {
        BiomeFactors f = new BiomeFactors();
        try (var in = MagiculeModelTest.class.getResourceAsStream("/data/nusmp/elite/world/biome_magicule.json")) {
            assertNotNull(in, "the biome table is shipped");
            f.add(JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        }
        assertTrue(f.size() > 10);
        assertEquals(1.0, f.factorOf("minecraft:deep_dark"), 1e-9);
        assertEquals(0.05, f.factorOf("minecraft:desert"), 1e-9);
        assertEquals(0.5, f.factorOf("minecraft:forest"), 1e-9);
        assertEquals(0.7, f.factorOf("minecraft:dark_forest"), 1e-9, "the specific entry comes first");
        assertEquals(0.3, f.factorOf("somemod:strange_place"), 1e-9, "unknown: the default");
        assertEquals(0.3, f.factorOf(null), 1e-9);
        JsonObject over = new JsonObject();
        over.addProperty("default", 0.6);
        f.add(over);
        assertEquals(0.6, f.factorOf("somemod:strange_place"), 1e-9, "a later file may change the default");
    }
}
