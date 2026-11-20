package com.newuniverse.nusmp.client.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the player panel's pose cycle is plain arithmetic; the test proves every step is real, the swing only shows with a weapon, the cycle wraps and the mouse follow is clamped. */
class HubPoseCycleTest {
    @Test
    void everyStepNamesAPoseThatExistsAndHasPositiveTimes() throws IOException {
        String poses = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/ui/UiPoses.java"));
        for (HubPoseCycle.Step s : HubPoseCycle.STEPS) {
            assertTrue(poses.contains("new Pose(\"" + s.pose() + "\""), "no pose named " + s.pose());
            assertTrue(s.blend() > 0f && s.hold() > 0f);
        }
    }

    @Test
    void unarmedNeverReturnsAWeaponStepAndEveryStepIsReachable() {
        Set<Integer> armed = new HashSet<>(), bare = new HashSet<>();
        float len = HubPoseCycle.length(true);
        assertTrue(len > HubPoseCycle.length(false));
        for (float t = 0; t < len; t += 0.5f) armed.add(HubPoseCycle.at(t, true));
        for (float t = 0; t < HubPoseCycle.length(false); t += 0.5f) bare.add(HubPoseCycle.at(t, false));
        assertEquals(HubPoseCycle.STEPS.length, armed.size(), "an armed cycle shows every step");
        for (int i : bare) assertFalse(HubPoseCycle.STEPS[i].weapon(), "unarmed showed weapon step " + i);
    }

    @Test
    void theCycleWrapsAndTakesNegativeTimes() {
        for (boolean a : new boolean[] {true, false}) {
            float len = HubPoseCycle.length(a);
            for (float t : new float[] {0f, 3f, 55.5f, 400f}) {
                assertEquals(HubPoseCycle.at(t, a), HubPoseCycle.at(t + len, a));
                assertEquals(HubPoseCycle.at(t, a), HubPoseCycle.at(t - 3 * len, a));
            }
            int i = HubPoseCycle.at(-0.001f, a);
            assertTrue(i >= 0 && i < HubPoseCycle.STEPS.length);
        }
    }

    @Test
    void swayAndMouseFollowStayInRange() {
        for (float t = 0; t < 3000; t += 7) assertTrue(Math.abs(HubPoseCycle.sway(t, 0f)) <= 24.001f);
        assertEquals(28f, HubPoseCycle.follow(1000f, 40f), 1e-4);
        assertEquals(-28f, HubPoseCycle.follow(-1000f, 40f), 1e-4);
        assertEquals(0f, HubPoseCycle.follow(5f, 0f));
        assertEquals(14f, HubPoseCycle.follow(20f, 40f), 1e-4);
    }
}
