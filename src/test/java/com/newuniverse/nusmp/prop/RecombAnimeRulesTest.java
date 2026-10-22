package com.newuniverse.nusmp.prop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.RecombinationRules;
import com.newuniverse.nusmp.prop.RecombAnimeRules.Clip;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the anime look of Recombination Magic (the house-golem) is drawn by the client from numbers that must agree with the server's state word and with the model files: the clip decision, the
 * stride that keeps the feet from sliding, the footfalls that raise dust, where a fist lands. Pure (no game classes). The model numbers are compared with tools/recomb/golem_numbers.json and the
 * animation files, so a regenerated model that moves the blow's landing time or the length of a clip fails here instead of looking wrong in game.
 */
class RecombAnimeRulesTest {
    private static final File ROOT = new File("src/main/resources");

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    @Test
    void stateWordMatchesTheServersAndTheOldPainters() {
        assertEquals(RecombinationRules.ULTRA_BIT, RecombAnimeRules.ULTRA_BIT);
        int p = 1 | (123 << 2) | RecombinationRules.ULTRA_BIT | (77 << 19);
        assertEquals(1, RecombAnimeRules.state(p));
        assertEquals(10, RecombAnimeRules.since(p, 133));
        assertTrue(RecombAnimeRules.ultra(p));
        assertEquals(5, RecombAnimeRules.ultraSince(p, 82));
        assertEquals(0, RecombAnimeRules.ultraSince(0, 82), "no Ultra form, no Ultra time");
        assertEquals(10, RecombAnimeRules.since(65530 << 2, 65540L), "the clock wraps at 65536");
        assertEquals(10, RecombAnimeRules.since(65530 << 2, 65536L + 4L), "and so does the game time");
    }

    @Test
    void clipDecisionFollowsTheLifeOfTheGolem() {
        float life = 1200f;
        assertEquals(Clip.ASSEMBLE, RecombAnimeRules.clip(0, 0, 0f, life, false));
        assertEquals(Clip.ASSEMBLE, RecombAnimeRules.clip(RecombAnimeRules.RAM, 0, 25.9f, life, true), "it cannot punch before it stands");
        assertEquals(Clip.STOMP, RecombAnimeRules.clip(0, 0, 30f, life, false), "it settles with a stomp after the rise");
        assertEquals(Clip.IDLE, RecombAnimeRules.clip(0, 0, 100f, life, false));
        assertEquals(Clip.WALK, RecombAnimeRules.clip(0, 0, 100f, life, true));
        assertEquals(Clip.PUNCH, RecombAnimeRules.clip(RecombAnimeRules.RAM, 27, 100f, life, true));
        assertEquals(Clip.WALK, RecombAnimeRules.clip(RecombAnimeRules.RAM, 28, 100f, life, true), "the ram state lasts 28 ticks");
        assertEquals(Clip.FIRE_VOLLEY, RecombAnimeRules.clip(RecombAnimeRules.FIRE, 11, 100f, life, false));
        assertEquals(Clip.IDLE, RecombAnimeRules.clip(RecombAnimeRules.FIRE, 12, 100f, life, false));
        assertEquals(Clip.COLLAPSE, RecombAnimeRules.clip(RecombAnimeRules.RAM, 3, life - 10f, life, true), "the end of its life beats everything");
        assertEquals(Clip.COLLAPSE, RecombAnimeRules.clip(0, 0, 5f, 20f, false), "a golem that lives less than its collapse is already collapsing");
    }

    @Test
    void clipSecondsCountFromTheirOwnStart() {
        float life = 1200f;
        assertEquals(0.5f, RecombAnimeRules.clipSeconds(Clip.PUNCH, 10f, 500f, life, 0f), 1e-6);
        assertEquals(0.3f, RecombAnimeRules.clipSeconds(Clip.FIRE_VOLLEY, 6f, 500f, life, 0f), 1e-6);
        assertEquals(0.5f, RecombAnimeRules.clipSeconds(Clip.ASSEMBLE, 0f, 10f, life, 0f), 1e-6);
        assertEquals(0.5f, RecombAnimeRules.clipSeconds(Clip.COLLAPSE, 0f, life - 14f, life, 0f), 1e-6);
        assertEquals(0.5f, RecombAnimeRules.clipSeconds(Clip.STOMP, 0f, 36f, life, 0f), 1e-6);
        assertEquals(0.25f, RecombAnimeRules.clipSeconds(Clip.WALK, 0f, 77f, life, 0.25f), 1e-6, "a walk is driven by the stride, not the clock");
    }

    @Test
    void renderScaleGrowsWithTheUltraFormOnly() {
        float s = RecombAnimeRules.renderScale(1.1f, 0f);
        assertEquals(1.1f * RecombAnimeRules.RENDER_FACTOR, s, 1e-6);
        assertEquals(s * 1.6f, RecombAnimeRules.renderScale(1.1f, 1f), 1e-5);
        assertEquals(s * 1.6f, RecombAnimeRules.renderScale(1.1f, 5f), 1e-5, "grow is clamped");
        assertEquals(0.3f * RecombAnimeRules.RENDER_FACTOR, RecombAnimeRules.renderScale(0f, 0f), 1e-6, "a tiny prop scale still draws");
        float blocks = RecombAnimeRules.HEIGHT_PX * RecombAnimeRules.PX * RecombAnimeRules.renderScale(1.2f, 0f);
        assertTrue(blocks > 7f && blocks < 10f, "the golem stands about eight blocks tall at the usual scale: " + blocks);
    }

    @Test
    void theStrideKeepsTheFeetOnTheGround() {
        float sc = RecombAnimeRules.renderScale(1.2f, 0f);
        float cycle = RecombAnimeRules.cycleBlocks(sc);
        assertTrue(cycle > 3f && cycle < 8f, "a walk cycle (two steps) covers a few blocks: " + cycle);
        assertEquals(cycle * 2f, RecombAnimeRules.cycleBlocks(sc * 2f), 1e-4, "a bigger golem takes longer steps");
        float p = 0f;
        for (int i = 0; i < 1000; i++) {
            p = RecombAnimeRules.advancePhase(p, 0.3, sc);
            assertTrue(p >= 0f && p < 1f, "the phase stays in [0, 1): " + p);
        }
        assertEquals(0.5f, RecombAnimeRules.advancePhase(0.25f, cycle * 0.25, sc), 1e-4);
        assertEquals(0.25f, RecombAnimeRules.advancePhase(0.75f, cycle * 0.5, sc), 1e-4, "the phase wraps");
        assertEquals(0.3f, RecombAnimeRules.advancePhase(0.3f, 0.0, sc), 1e-6, "standing still does not turn the legs");
        assertEquals(0.3f * RecombAnimeRules.WALK_CYCLE_S, RecombAnimeRules.walkSeconds(0.3f), 1e-6);
    }

    @Test
    void footfallsHappenOncePerStepAndNeverTwice() {
        assertEquals(1, RecombAnimeRules.footfalls(0.2f, 0.3f));
        assertEquals(2, RecombAnimeRules.footfalls(0.7f, 0.8f));
        assertEquals(0, RecombAnimeRules.footfalls(0.3f, 0.4f));
        assertEquals(0, RecombAnimeRules.footfalls(0.3f, 0.3f), "no move, no step");
        assertEquals(3, RecombAnimeRules.footfalls(0.2f, 0.9f), "a long frame can land both feet");
        assertEquals(1, RecombAnimeRules.footfalls(0.95f, 0.3f), "across the wrap past 1.0");
        int count = 0;
        float p = 0f;
        for (int i = 0; i < 2000; i++) {
            float n = RecombAnimeRules.advancePhase(p, 0.2, 1f);
            count += Integer.bitCount(RecombAnimeRules.footfalls(p, n));
            p = n;
        }
        float cycles = 2000 * 0.2f / RecombAnimeRules.cycleBlocks(1f);
        assertEquals(cycles * 2f, count, 3f, "two footfalls per cycle");
    }

    @Test
    void aCounterCrossesAMarkOnce() {
        assertTrue(RecombAnimeRules.crossed(12, 1, 12));
        assertFalse(RecombAnimeRules.crossed(13, 1, 12));
        assertFalse(RecombAnimeRules.crossed(11, 1, 12));
        assertTrue(RecombAnimeRules.crossed(14, 4, 12), "a frame that skipped ticks still sees it");
        assertFalse(RecombAnimeRules.crossed(14, 0, 12));
        assertTrue(RecombAnimeRules.newTick(5, 6));
        assertFalse(RecombAnimeRules.newTick(6, 6));
    }

    @Test
    void localPointsTurnWithTheYaw() {
        double[] fwd = RecombAnimeRules.toWorld(0, 0, 2);
        assertEquals(0, fwd[0], 1e-9);
        assertEquals(2, fwd[1], 1e-9);
        double[] left = RecombAnimeRules.toWorld(0, 3, 0);
        assertEquals(3, left[0], 1e-9);
        assertEquals(0, left[1], 1e-9);
        double[] east = RecombAnimeRules.toWorld(-90, 0, 1);
        assertEquals(1, east[0], 1e-9, "yaw -90 looks toward +x");
        assertEquals(0, east[1], 1e-9);
        double[] west = RecombAnimeRules.toWorld(90, 0, 1);
        assertEquals(-1, west[0], 1e-9, "yaw 90 looks toward -x");
        double[] fist = RecombAnimeRules.fistLanding(1f);
        assertTrue(fist[0] < 0, "the striking fist is the right one (the model's -x)");
        assertTrue(fist[1] > 3.5 && fist[1] < 6, "it lands a few blocks ahead: " + fist[1]);
        double[] foot = RecombAnimeRules.footLanding(true, 1f), foot2 = RecombAnimeRules.footLanding(false, 1f);
        assertTrue(foot[0] > 0 && foot2[0] < 0 && Math.abs(foot[0] + foot2[0]) < 1e-9);
        assertTrue(foot[1] > 0);
    }

    @Test
    void effectCountsStayInTheirBounds() {
        for (float sc = 0.1f; sc < 4f; sc += 0.1f) {
            assertTrue(RecombAnimeRules.footfallDust(sc) >= 4 && RecombAnimeRules.footfallDust(sc) <= 14);
            assertTrue(RecombAnimeRules.slamDust(sc) >= 6 && RecombAnimeRules.slamDust(sc) <= 28);
            assertTrue(RecombAnimeRules.slamSparks(sc) >= 8 && RecombAnimeRules.slamSparks(sc) <= 30);
            assertTrue(RecombAnimeRules.slamChips(sc) >= 3 && RecombAnimeRules.slamChips(sc) <= 14);
        }
        assertTrue(RecombAnimeRules.chimneyPuffEvery() >= 4);
    }

    @Test
    void numbersAgreeWithTheGeneratorsFile() throws Exception {
        JsonObject n = json(new File("tools/recomb/golem_numbers.json"));
        assertEquals(n.get("height_px").getAsFloat(), RecombAnimeRules.HEIGHT_PX, 0.01);
        assertEquals(n.get("punch_fist_forward_px").getAsFloat(), RecombAnimeRules.PUNCH_FIST_FORWARD_PX, 0.1);
        assertEquals(n.get("punch_fist_front_px").getAsFloat(), RecombAnimeRules.PUNCH_FIST_FRONT_PX, 0.1);
        assertEquals(n.get("punch_fist_height_px").getAsFloat(), RecombAnimeRules.PUNCH_FIST_HEIGHT_PX, 0.1);
        assertEquals(n.get("punch_impact_s").getAsFloat(), RecombAnimeRules.PUNCH_IMPACT_S, 1e-4);
        assertEquals(n.get("punch_length_s").getAsFloat(), RecombAnimeRules.PUNCH_LENGTH_S, 1e-4);
        assertEquals(n.get("walk_cycle_s").getAsFloat(), RecombAnimeRules.WALK_CYCLE_S, 1e-4);
        assertEquals(n.get("stomp_land_s").getAsFloat(), RecombAnimeRules.STOMP_LAND_S, 1e-4);
        assertEquals(n.get("stomp_length_s").getAsFloat(), RecombAnimeRules.STOMP_LENGTH_S, 1e-4);
        assertEquals(n.get("assemble_s").getAsFloat(), RecombAnimeRules.ASSEMBLE_S, 1e-4);
        assertEquals(n.get("collapse_s").getAsFloat(), RecombAnimeRules.COLLAPSE_S, 1e-4);
        assertEquals(n.get("fire_s").getAsFloat(), RecombAnimeRules.FIRE_S, 1e-4);
        assertEquals(n.get("foot_x_px").getAsFloat(), RecombAnimeRules.FOOT_X_PX, 1e-4);
        assertEquals(n.get("chimney_px").getAsJsonArray().get(0).getAsFloat(), RecombAnimeRules.CHIMNEY_PX[0], 1e-4);
        assertEquals(n.get("chimney_px").getAsJsonArray().get(1).getAsFloat(), RecombAnimeRules.CHIMNEY_PX[1], 1e-4);
        assertEquals(n.get("eye_y_px").getAsFloat(), RecombAnimeRules.EYE_PX[1], 1e-4);
    }

    @Test
    void clipLengthsMatchTheServersTicks() throws Exception {
        JsonObject a = json(new File(ROOT, "assets/nusmp/animations/entity/recombanime_golem.animation.json")).getAsJsonObject("animations");
        assertEquals(RecombAnimeRules.ASSEMBLE_TICKS, Math.round(a.getAsJsonObject("assemble").get("animation_length").getAsFloat() * 20f));
        assertEquals(RecombAnimeRules.COLLAPSE_TICKS, Math.round(a.getAsJsonObject("collapse").get("animation_length").getAsFloat() * 20f));
        assertEquals(RecombAnimeRules.RAM_TICKS, Math.round(a.getAsJsonObject("punch").get("animation_length").getAsFloat() * 20f));
        assertEquals(RecombAnimeRules.FIRE_TICKS, Math.round(a.getAsJsonObject("fire").get("animation_length").getAsFloat() * 20f));
        assertEquals(RecombAnimeRules.STOMP_TICKS, Math.round(a.getAsJsonObject("stomp").get("animation_length").getAsFloat() * 20f));
        assertEquals(RecombAnimeRules.WALK_CYCLE_S, a.getAsJsonObject("walk").get("animation_length").getAsFloat(), 1e-4);
        assertEquals(RecombAnimeRules.PUNCH_IMPACT_TICK, Math.round(RecombAnimeRules.PUNCH_IMPACT_S * 20f), "the blow lands when the server's blow does (12 ticks after the ram begins)");
        // the keyframe of the blow: the striking arm is at its far swing at the landing time
        var arm = a.getAsJsonObject("punch").getAsJsonObject("bones").getAsJsonObject("arm_r").getAsJsonObject("rotation");
        assertTrue(arm.has("0.60"), "the right arm has a key at the landing time");
        assertTrue(arm.getAsJsonArray("0.60").get(0).getAsFloat() > 30f, "and it is swung forward there");
        assertTrue(arm.getAsJsonArray("0.52").get(0).getAsFloat() < -30f, "after being drawn back");
    }

    @Test
    void particleIdsExistInTheCatalog() throws Exception {
        JsonObject cat = json(new File(ROOT, "data/nusmp/particle_catalog.json"));
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (var e : cat.getAsJsonArray("particles")) ids.add(e.getAsJsonObject().get("id").getAsString());
        for (String id : RecombAnimeRules.PARTICLE_IDS) assertTrue(ids.contains(id), "particle " + id + " is not in the catalog");
        assertEquals(RecombAnimeRules.PARTICLE_IDS.length, new java.util.HashSet<>(java.util.Arrays.asList(RecombAnimeRules.PARTICLE_IDS)).size());
    }
}
