package com.newuniverse.nusmp.saiyan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 3.71.0: the power-up body clip {@code saiyan_charge} and the continuous envelopes of the power-up effects (nothing may pop). */
class SaiyanChargeClipTest {
    private static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");

    private static JsonObject clip() throws IOException {
        JsonObject all = JsonParser.parseString(Files.readString(RES.resolve("animations/player/cast.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
        assertTrue(all.has("saiyan_charge"), "the clip is in cast.animation.json (python tools/gen_cast_animations.py --append-saiyan)");
        return all.getAsJsonObject("saiyan_charge");
    }

    private static List<double[]> track(JsonObject bone, String channel) {
        List<double[]> out = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> e : bone.getAsJsonObject(channel).entrySet()) {
            JsonArray a = e.getValue().getAsJsonArray();
            out.add(new double[]{Double.parseDouble(e.getKey()), a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()});
        }
        out.sort((x, y) -> Double.compare(x[0], y[0]));
        return out;
    }

    @Test
    void theClipIsAOneShotOfTheChargeUpsLengthAndNamesTheFivePlayerBones() throws IOException {
        JsonObject c = clip();
        assertFalse(c.get("loop").getAsBoolean(), "a one-shot");
        assertEquals(SaiyanLook.BURST_TICKS / 20.0, c.get("animation_length").getAsDouble(), 0.15, "about as long as the charge-up");
        JsonObject bones = c.getAsJsonObject("bones");
        for (String b : new String[]{"body", "left_arm", "right_arm", "left_leg", "right_leg"}) assertTrue(bones.has(b), b);
    }

    @Test
    void everyMoveIsEasedInSmallStepsAndTheClipStartsAndEndsNearRest() throws IOException {
        JsonObject bones = clip().getAsJsonObject("bones");
        for (String b : bones.keySet()) {
            List<double[]> k = track(bones.getAsJsonObject(b), "rotation");
            assertTrue(k.size() >= 40, b + " is keyed densely enough to ease: " + k.size());
            for (int i = 1; i < k.size(); i++) {
                assertTrue(k.get(i)[0] - k.get(i - 1)[0] <= 0.101, b + " keys at most 0.1 s apart");
                for (int a = 1; a <= 3; a++) assertTrue(Math.abs(k.get(i)[a] - k.get(i - 1)[a]) <= 24.0, b + " " + k.get(i)[0] + " moves " + Math.abs(k.get(i)[a] - k.get(i - 1)[a]) + " degrees in one step");
            }
            double[] first = k.get(0), last = k.get(k.size() - 1);
            for (int a = 1; a <= 3; a++) {
                assertTrue(Math.abs(first[a]) <= 20.0, b + " starts near rest");
                assertTrue(Math.abs(last[a]) <= 20.0, b + " ends near rest (so the fade out is soft)");
            }
        }
    }

    @Test
    void theTremorDiesAwayAndTheBurstSnapsTheArmsOpenWithTheFlash() throws IOException {
        JsonObject bones = clip().getAsJsonObject("bones");
        List<double[]> la = track(bones.getAsJsonObject("left_arm"), "rotation");
        double atBurst = 0, atEnd = 0;
        for (double[] k : la) {
            if (Math.abs(k[0] - 1.25) < 0.026) atBurst = Math.abs(k[3]);
            if (k[0] >= 2.2) atEnd = Math.max(atEnd, Math.abs(k[3]));
        }
        assertTrue(atBurst > 60, "the arm is thrown out sideways at the burst: " + atBurst);
        assertTrue(atEnd < 20, "and settles: " + atEnd);
        assertEquals(SaiyanLook.GATHER_TICKS + 3, 1.25 * 20, 0.001, "the snap lands on the flash's peak tick");
    }

    @Test
    void everyEnvelopeOfThePowerUpIsContinuousAndZeroAtBothEnds() {
        for (int which = 0; which < 5; which++) {
            float prev = f(which, 0f);
            assertEquals(0f, prev, 1e-6, "env " + which + " starts at 0");
            for (float t = 0.1f; t <= 90f; t += 0.1f) {
                float v = f(which, t);
                assertTrue(v >= 0f && v <= 1.0001f, "env " + which + " t=" + t + " = " + v);
                assertTrue(Math.abs(v - prev) <= 0.06f, "env " + which + " jumps at t=" + t + " (" + prev + " -> " + v + ")");
                prev = v;
            }
            assertEquals(0f, f(which, 90f), 1e-6, "env " + which + " ends at 0");
        }
        assertEquals(1f, SaiyanLook.halo(SaiyanLook.GATHER_TICKS + 4f), 1e-5, "the burst is brightest just after the burst");
        assertTrue(SaiyanLook.HALO_END < SaiyanLook.CRACK_TICKS, "the backdrop is gone before the painter stops drawing the charge-up");
    }

    private static float f(int which, float t) {
        return switch (which) {
            case 0 -> SaiyanLook.flash(t);
            case 1 -> SaiyanLook.halo(t);
            case 2 -> SaiyanLook.shake(t);
            case 3 -> SaiyanLook.windAlpha(t);
            default -> SaiyanLook.pillarAlpha(t);
        };
    }

    @Test
    void theWindStaysOnTheBodyAndInsideItsBudget() {
        float[] o = new float[5];
        for (int i = 0; i < SaiyanLook.MAX_WIND; i++)
            for (float t = 0f; t < 90f; t += 1.7f) {
                SaiyanLook.wind(i, t, o);
                assertTrue(Math.hypot(o[0], o[2]) <= 1.3f && o[1] >= 0f && o[1] <= 3.3f && o[3] >= 0f && o[3] <= 1f && o[4] > 0.5f && o[4] < 1.4f, "wind " + i + " t=" + t);
            }
        for (SaiyanLook.Look k : SaiyanLook.all()) {
            assertTrue(SaiyanLook.quads(k, false) <= SaiyanLook.MAX_QUADS, k.id());
            assertTrue(SaiyanLook.quads(k, true) <= 40, k.id() + " far");
        }
    }
}
