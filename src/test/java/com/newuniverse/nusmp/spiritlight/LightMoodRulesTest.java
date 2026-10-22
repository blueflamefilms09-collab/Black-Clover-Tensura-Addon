package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.spiritlight.LightMoodRules.Cue;
import com.newuniverse.nusmp.spiritlight.LightMoodRules.Mood;
import com.newuniverse.nusmp.spiritlight.LightMoodRules.Situation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The pure mood rules: which pose, face, creature sound and spoken line the Spirit of Light shows for what happens, with cooldowns, priorities and a quiet clock. */
class LightMoodRulesTest {
    @Test
    void everyPoseHasANameAFaceALengthAndTheTablesAgree() {
        for (boolean seraph : new boolean[]{false, true}) {
            int n = LightMoodRules.poseCount(seraph);
            assertTrue(n <= 16, "a pose id is four bits of the prop word");
            for (int p = 0; p < n; p++) {
                String name = LightMoodRules.poseName(seraph, p);
                assertEquals(p, LightMoodRules.poseId(seraph, name), name);
                int face = LightMoodRules.face(seraph, p);
                assertTrue(face >= 0 && face < 4, name);
                assertFalse(LightMoodRules.faceName(seraph, p).isBlank());
                assertTrue(LightMoodRules.poseTicks(seraph, p) >= 0);
            }
            assertEquals("idle", LightMoodRules.poseName(seraph, LightMoodRules.POSE_IDLE));
            assertEquals("cast", LightMoodRules.poseName(seraph, LightMoodRules.POSE_CAST));
            assertEquals(-1, LightMoodRules.poseId(seraph, "no such pose"));
            assertEquals(LightMoodRules.poseName(seraph, n - 1), LightMoodRules.poseName(seraph, 99), "an unknown id is clamped to a real pose, never an exception");
        }
    }

    @Test
    void theCubHasTheEightPlayfulPosesAndFourFacesAndTheSeraphTheReverentOnes() {
        for (String p : new String[]{"pounce", "roll", "stretch", "lick", "roar", "sleep", "hurt", "victory"}) assertTrue(LightMoodRules.poseId(false, p) > 0, p);
        for (String p : new String[]{"wings_open", "halo_spin", "bless", "prayer", "hurt", "victory", "cast"}) assertTrue(LightMoodRules.poseId(true, p) > 0, p);
        assertArrayEquals(new String[]{"happy", "sleepy", "fierce", "surprised"}, LightMoodRules.CUB_FACES);
        assertArrayEquals(new String[]{"serene", "stern", "compassionate", "radiant"}, LightMoodRules.SERAPH_FACES);
        assertEquals("sleepy", LightMoodRules.faceName(false, LightMoodRules.poseId(false, "sleep")));
        assertEquals("fierce", LightMoodRules.faceName(false, LightMoodRules.poseId(false, "roar")));
        assertEquals("surprised", LightMoodRules.faceName(false, LightMoodRules.poseId(false, "hurt")));
        assertEquals("compassionate", LightMoodRules.faceName(true, LightMoodRules.poseId(true, "bless")));
        assertEquals("radiant", LightMoodRules.faceName(true, LightMoodRules.poseId(true, "wings_open")));
        assertTrue(LightMoodRules.poseLoops(false, LightMoodRules.poseId(false, "sleep")), "curled up asleep holds");
        assertTrue(LightMoodRules.poseLoops(true, LightMoodRules.poseId(true, "prayer")));
        assertFalse(LightMoodRules.poseLoops(false, LightMoodRules.poseId(false, "pounce")));
    }

    @Test
    void everySituationAsksForAKnownPoseOfItsForm() {
        for (boolean seraph : new boolean[]{false, true})
            for (Situation s : Situation.values())
                for (int c = -3; c < 12; c++) {
                    int p = LightMoodRules.poseFor(seraph, s, c);
                    assertTrue(p >= 0 && p < LightMoodRules.poseCount(seraph), s + " asked for pose " + p);
                }
        assertEquals(LightMoodRules.poseId(false, "hurt"), LightMoodRules.poseFor(false, Situation.HURT, 0));
        assertEquals(LightMoodRules.poseId(true, "wings_open"), LightMoodRules.poseFor(true, Situation.POWER_UP, 0));
        assertEquals(LightMoodRules.poseId(false, "victory"), LightMoodRules.poseFor(false, Situation.OWNER_KILL, 3));
        assertEquals(LightMoodRules.poseId(true, "prayer"), LightMoodRules.poseFor(true, Situation.IDLE_REST, 0));
        List<Integer> seen = new ArrayList<>();
        for (int c = 0; c < 6; c++) {
            int p = LightMoodRules.poseFor(false, Situation.IDLE_FIDGET, c);
            if (!seen.contains(p)) seen.add(p);
        }
        assertTrue(seen.size() >= 3, "the playful fidgets rotate: " + seen);
    }

    @Test
    void aReactionIsRefusedInsideItsCooldownAndAllowedAfter() {
        Mood m = new Mood();
        int first = m.react(false, Situation.HURT, 1000);
        assertEquals(LightMoodRules.poseId(false, "hurt"), first);
        assertEquals(-1, m.react(false, Situation.HURT, 1000 + LightMoodRules.cooldown(Situation.HURT) - 1), "the same situation inside its cooldown");
        assertEquals(first, m.react(false, Situation.HURT, 1000 + LightMoodRules.cooldown(Situation.HURT)));
        for (Situation s : Situation.values()) assertTrue(LightMoodRules.cooldown(s) >= 0);
    }

    @Test
    void aLessImportantReactionNeverCutsAMoreImportantPoseShort() {
        Mood m = new Mood();
        assertTrue(m.react(false, Situation.HURT, 100) >= 0);                                       // priority 4, 12 ticks
        assertEquals(-1, m.react(false, Situation.OWNER_KILL, 105), "a victory (2) does not cut a flinch (4) short");
        assertTrue(m.react(false, Situation.OWNER_KILL, 100 + LightMoodRules.poseTicks(false, LightMoodRules.poseId(false, "hurt")) + 1) >= 0, "after it");
        Mood n = new Mood();
        assertTrue(n.react(false, Situation.CAST_BEAM, 0) >= 0);
        assertTrue(n.react(false, Situation.HURT, 3) >= 0, "a hit (4) does cut a cast (3)");
        assertTrue(LightMoodRules.priority(Situation.OWNER_DIED) >= LightMoodRules.priority(Situation.HURT));
        assertTrue(LightMoodRules.priority(Situation.IDLE_FIDGET) < LightMoodRules.priority(Situation.CAST_BEAM));
    }

    @Test
    void theQuietClockFidgetsAfterALongQuietRestsWhenTheOwnerIsStillAndWakesWhenTheyMove() {
        Mood m = new Mood();
        assertNull(m.quiet(100, 0), "nothing happens right away");
        assertNull(m.quiet(LightMoodRules.FIDGET_AFTER - 1, 0));
        assertEquals(Situation.IDLE_FIDGET, m.quiet(LightMoodRules.FIDGET_AFTER, 0));
        assertNull(m.quiet(LightMoodRules.FIDGET_AFTER + 4, 0), "not twice in a row");
        assertNull(m.quiet(LightMoodRules.FIDGET_AFTER + LightMoodRules.FIDGET_GAP - 1, 0));
        Situation rest = m.quiet(5000, LightMoodRules.REST_AFTER);
        assertEquals(Situation.IDLE_REST, rest);
        int pose = m.react(false, rest, 5000);
        assertEquals(LightMoodRules.poseId(false, "sleep"), pose);
        assertTrue(m.resting());
        assertNull(m.quiet(5100, 400), "asleep, the owner still: it stays asleep");
        assertEquals(Situation.WAKE, m.quiet(5200, 0), "the owner moved: it wakes");
        assertEquals(LightMoodRules.POSE_IDLE, m.react(false, Situation.WAKE, 5200));
        assertFalse(m.resting());
        Mood owner = new Mood();
        assertTrue(owner.react(true, Situation.IDLE_REST, 0) >= 0);
        assertEquals(LightMoodRules.poseId(true, "prayer"), owner.pose());
    }

    @Test
    void onlyTheSeraphSpeaksAndEverySpeakingSituationHasACue() {
        for (Situation s : Situation.values()) assertNull(LightMoodRules.cueFor(false, s, 0), "the cub never speaks: " + s);
        assertEquals(Cue.BEAM, LightMoodRules.cueFor(true, Situation.CAST_BEAM, 0));
        assertEquals(Cue.PULSE, LightMoodRules.cueFor(true, Situation.CAST_PULSE, 0));
        assertEquals(Cue.DAMAGE, LightMoodRules.cueFor(true, Situation.HURT, 0));
        assertEquals(Cue.LOW, LightMoodRules.cueFor(true, Situation.OWNER_LOW, 0));
        assertEquals(Cue.WIN, LightMoodRules.cueFor(true, Situation.OWNER_KILL, 0));
        assertEquals(Cue.DEATH, LightMoodRules.cueFor(true, Situation.OWNER_DIED, 0));
        assertEquals(Cue.FAREWELL, LightMoodRules.cueFor(true, Situation.UNLINK, 0));
        assertEquals(Cue.GREET, LightMoodRules.cueFor(true, Situation.POWER_UP, 0));
        assertEquals(Cue.RISE, LightMoodRules.cueFor(true, Situation.POWER_UP, 1));
        assertNull(LightMoodRules.cueFor(true, Situation.WAKE, 0));
        for (Cue c : Cue.values()) assertTrue(LightMoodRules.cueCooldown(c) >= 100, c + " is never said twice in a hurry");
    }

    @Test
    void aCueSpeaksOnceInsideItsCooldown() {
        Mood m = new Mood();
        assertTrue(m.cueReady(Cue.BEAM, 1000));
        assertFalse(m.cueReady(Cue.BEAM, 1000 + LightMoodRules.cueCooldown(Cue.BEAM) - 1));
        assertTrue(m.cueReady(Cue.PULSE, 1001), "another cue is its own clock");
        assertTrue(m.cueReady(Cue.BEAM, 1000 + LightMoodRules.cueCooldown(Cue.BEAM)));
        assertFalse(m.cueReady(null, 0));
    }

    @Test
    void theLinePickerOnlyUsesLinesThatExistAndNeverRepeatsTheLastOne() {
        List<String> have = List.of("spiritlight_beam_1", "spiritlight_beam_2", "spiritlight_pulse_1");
        assertNull(LightMoodRules.line(Cue.WIN, 0, have, ""), "no line of that cue: null, so the server plays a synthesised cue (nothing is faked)");
        assertEquals("spiritlight_pulse_1", LightMoodRules.line(Cue.PULSE, 7, have, "spiritlight_pulse_1"), "the only line of a cue may repeat");
        for (int c = 0; c < 20; c++) {
            String a = LightMoodRules.line(Cue.BEAM, c, have, "");
            assertTrue(a.startsWith("spiritlight_beam_"));
            assertNotEquals(a, LightMoodRules.line(Cue.BEAM, c, have, a), "not the same line twice in a row");
        }
        assertNull(LightMoodRules.line(Cue.BEAM, 0, List.of(), ""));
    }

    @Test
    void theCubMakesCreatureSoundsOnly() {
        int hurt = LightMoodRules.poseId(false, "hurt");
        assertEquals(SpiritLightRules.S_WHIMPER, LightMoodRules.cubSound(Situation.HURT, hurt));
        assertEquals(SpiritLightRules.S_CUB_ROAR, LightMoodRules.cubSound(Situation.CAST_PULSE, LightMoodRules.poseId(false, "roar")));
        assertEquals(SpiritLightRules.S_YAWN, LightMoodRules.cubSound(Situation.IDLE_FIDGET, LightMoodRules.poseId(false, "stretch")));
        assertEquals(SpiritLightRules.S_PURR, LightMoodRules.cubSound(Situation.IDLE_REST, LightMoodRules.poseId(false, "sleep")));
        assertEquals(SpiritLightRules.S_CHIRP, LightMoodRules.cubSound(Situation.OWNER_KILL, LightMoodRules.poseId(false, "victory")));
        assertEquals(-1, LightMoodRules.cubSound(Situation.WAKE, 0));
        for (Situation s : Situation.values()) {
            int k = LightMoodRules.cubSound(s, 0);
            assertTrue(k == -1 || (k >= 0 && k < SpiritLightRules.SOUND_KINDS), s.name());
        }
        for (Cue c : Cue.values()) {
            int k = LightMoodRules.fallbackSound(c);
            assertTrue(k == -1 || (k >= 0 && k < SpiritLightRules.SOUND_KINDS), c.name());
        }
    }

    @Test
    void lowHealthIsAThirdOrLessAndAboveZero() {
        assertTrue(LightMoodRules.lowHealth(6f, 20f));
        assertTrue(LightMoodRules.lowHealth(0.5f, 20f));
        assertFalse(LightMoodRules.lowHealth(7f, 20f));
        assertFalse(LightMoodRules.lowHealth(0f, 20f), "dead is not low");
        assertFalse(LightMoodRules.lowHealth(5f, 0f));
    }

    @Test
    void theSameAskAlwaysGivesTheSameAnswerSoThereIsNoHiddenRandomness() {
        for (Situation s : Situation.values()) {
            Mood a = new Mood(), b = new Mood();
            assertEquals(a.react(true, s, 500), b.react(true, s, 500), s.name());
            assertEquals(a.pose(), b.pose());
        }
    }
}
