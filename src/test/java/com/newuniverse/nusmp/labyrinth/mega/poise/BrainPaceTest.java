package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryAbility;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryBrain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the desperation phase changes the foundry brain in two small ways (a pace that shortens the cooldowns and the roll interval, and a rebuild that adds the ultimate while keeping the
 * cooldowns that are running), and the old rhythm must be exactly what it was when the pace is 1. These tests pin that the default brain behaves as before, that pace shortens a cooldown but
 * never to zero or below the roll, that it never slows a brain down, and that a rebuilt brain does not forget a cooldown (a mob that turns desperate must not get a free volley of everything).
 */
public class BrainPaceTest {
    private static FoundryAbility slam(String id, int cd) {
        return FoundryAbility.clamped(id, FoundryAbility.Kind.SLAM, cd, 0, 6, 10, 1.0, 0, 0, 4, 0, 0, "road", "");
    }

    @Test
    public void thePaceOfOneIsTheOldRhythm() {
        FoundryBrain b = new FoundryBrain(List.of(slam("a", 100)), 30);
        assertEquals(1.0, b.pace(), 1e-9);
        b.used(0, 1000);
        assertEquals(100, b.cooldownLeft(0, 1000));
        assertEquals(-1, b.choose(1010, false, FoundryAbility.Situation.of(3.0, 1.0), new Random(1)), "cooling down");
    }

    @Test
    public void aFasterPaceShortensCooldownsButNeverToZeroOrBelowOne() {
        FoundryBrain b = new FoundryBrain(List.of(slam("a", 100), slam("b", 1)), 30);
        b.setPace(1.4);
        b.used(0, 0);
        assertEquals(71, b.cooldownLeft(0, 0));
        b.used(1, 0);
        assertEquals(1, b.cooldownLeft(1, 0), "a one-tick cooldown stays one tick");
        b.setPace(100.0);
        assertEquals(3.0, b.pace(), 1e-9, "the pace is capped");
        b.setPace(0.2);
        assertEquals(1.0, b.pace(), 1e-9, "a brain never slows down");
        b.setPace(Double.NaN);
        assertEquals(1.0, b.pace(), 1e-9);
    }

    @Test
    public void aFasterPaceRollsMoreOften() {
        FoundryBrain slow = new FoundryBrain(List.of(slam("a", 5)), 30), fast = new FoundryBrain(List.of(slam("a", 5)), 30);
        fast.setPace(1.5);
        FoundryAbility.Situation s = FoundryAbility.Situation.of(3.0, 1.0);
        slow.choose(0, false, s, new Random(1));
        fast.choose(0, false, s, new Random(1));
        assertFalse(slow.due(29));
        assertTrue(slow.due(30));
        assertTrue(fast.due(20));
        assertFalse(fast.due(19));
    }

    @Test
    public void aRebuiltBrainKeepsTheCooldownsThatAreRunning() {
        FoundryBrain old = new FoundryBrain(List.of(slam("a", 100), slam("b", 100)), 30);
        old.used(1, 500);
        FoundryBrain next = new FoundryBrain(List.of(slam("a", 100), slam("b", 100), slam("ultimate", 150)), 30);
        next.inherit(old);
        assertEquals(0, next.cooldownLeft(0, 500));
        assertEquals(100, next.cooldownLeft(1, 500), "b was cooling down and still is");
        assertEquals(0, next.cooldownLeft(2, 500), "the new ability is ready");
        next.inherit(null);
        assertEquals(100, next.cooldownLeft(1, 500), "inheriting nothing changes nothing");
    }
}
