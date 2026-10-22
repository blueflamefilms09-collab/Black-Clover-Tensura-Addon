package com.newuniverse.nusmp.spiritspace;

import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Expression;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Mood;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Situation;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules.Voice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure mood rules: which pose, expression and voice a situation gets, how long they last, the cooldowns, the hearing rule, the param bits and the numbers of every pose. */
class SpaceMoodRulesTest {
    @Test
    void everySituationHasAPoseAnExpressionAndAVoiceAndTheTableIsAsDocumented() {
        for (Situation s : Situation.values()) {
            assertNotNull(SpaceMoodRules.moodFor(s, false, 0), s.name());
            assertNotNull(SpaceMoodRules.moodFor(s, true, 0), s.name());
            assertNotNull(SpaceMoodRules.expressionFor(s), s.name());
            assertNotNull(SpaceMoodRules.voiceFor(s, 0), s.name());
        }
        assertEquals(Mood.CURIOUS, SpaceMoodRules.moodFor(Situation.LINK, false, 0));
        assertEquals(Mood.COIL, SpaceMoodRules.moodFor(Situation.PORTAL_CHANNEL, true, 0));
        assertEquals(Mood.PORTAL_PULSE, SpaceMoodRules.moodFor(Situation.PORTAL_CHANNEL, false, 0));
        assertEquals(Mood.ASCEND, SpaceMoodRules.moodFor(Situation.PORTAL_OPEN, true, 0));
        assertEquals(Mood.SNAP, SpaceMoodRules.moodFor(Situation.CUT, false, 0), "the orb snaps");
        assertEquals(Mood.ROAR, SpaceMoodRules.moodFor(Situation.CUT, true, 0), "the wisp roars");
        assertEquals(Mood.SQUEEZE, SpaceMoodRules.moodFor(Situation.GRAVITY, false, 0));
        assertEquals(Mood.HURT, SpaceMoodRules.moodFor(Situation.DAMAGED, false, 0));
        assertEquals(Mood.FLARE, SpaceMoodRules.moodFor(Situation.OWNER_KILL, false, 0));
        assertEquals(Mood.FAREWELL, SpaceMoodRules.moodFor(Situation.OWNER_DIED, true, 0));
        assertEquals(Mood.FAREWELL, SpaceMoodRules.moodFor(Situation.UNLINK, false, 0));
        assertEquals(Mood.CALM, SpaceMoodRules.moodFor(Situation.IDLE, false, 0));
        assertEquals(Mood.CURIOUS, SpaceMoodRules.moodFor(Situation.IDLE, false, 2), "now and then an idle spirit leans in");
        assertEquals(Mood.CURIOUS, SpaceMoodRules.moodFor(Situation.IDLE, false, -1), "a negative counter is fine");
        assertEquals(Expression.ANGRY, SpaceMoodRules.expressionFor(Situation.CUT));
        assertEquals(Expression.EXCITED, SpaceMoodRules.expressionFor(Situation.LINK));
        assertEquals(Expression.SLEEPY, SpaceMoodRules.expressionFor(Situation.UNLINK));
        assertEquals(Expression.CALM, SpaceMoodRules.expressionFor(Situation.IDLE));
    }

    @Test
    void theVoiceCuesAreNonVerbalAndTheIdleSpiritSpeaksAboutOnceAMinute() {
        assertEquals(Voice.CHIRP, SpaceMoodRules.voiceFor(Situation.LINK, 0), "the curious chirp");
        assertEquals(Voice.WARBLE, SpaceMoodRules.voiceFor(Situation.DAMAGED, 0), "the distress warble");
        assertEquals(Voice.WARBLE, SpaceMoodRules.voiceFor(Situation.OWNER_LOW, 0));
        assertEquals(Voice.SIGH, SpaceMoodRules.voiceFor(Situation.OWNER_DIED, 0));
        assertEquals(Voice.NONE, SpaceMoodRules.voiceFor(Situation.CUT, 0), "the cut has its own tear sound");
        int voiced = 0;
        for (int cycle = 0; cycle < 90; cycle++) if (SpaceMoodRules.voiceFor(Situation.IDLE, cycle) != Voice.NONE) voiced++;
        assertEquals(30, voiced, "one idle cycle in three has a voice (a cycle is 400 ticks)");
        for (Voice v : Voice.values()) if (v != Voice.NONE) assertTrue(v.id().startsWith("voice_"));
        assertEquals(8, Voice.values().length, "seven cues and NONE");
    }

    @Test
    void aPoseEndsAndTheSpiritIsCalmAgainExceptWhenSleepy() {
        for (Mood m : Mood.values()) {
            int d = SpaceMoodRules.duration(m);
            if (m == Mood.CALM || m == Mood.SLEEPY) {
                assertEquals(0, d);
                assertEquals(m, SpaceMoodRules.effective(m, 100000));
            } else {
                assertTrue(d > 0 && d <= 60, m + " lasts a couple of seconds at most");
                assertEquals(m, SpaceMoodRules.effective(m, d));
                assertEquals(Mood.CALM, SpaceMoodRules.effective(m, d + 1));
            }
        }
        assertEquals(Expression.ANGRY, SpaceMoodRules.effective(Expression.ANGRY, Mood.SNAP, 16 + 40));
        assertEquals(Expression.CALM, SpaceMoodRules.effective(Expression.ANGRY, Mood.SNAP, 16 + 41), "an expression outlasts its pose by two seconds");
        assertEquals(Expression.SLEEPY, SpaceMoodRules.effective(Expression.CALM, Mood.SLEEPY, 99999), "a sleepy pose keeps the sleepy look");
    }

    @Test
    void theSameTriggerCannotSpamAndWeakOnesNeverInterruptARunningPose() {
        SpaceMoodRules.Tracker t = new SpaceMoodRules.Tracker();
        assertTrue(t.ready(Situation.DAMAGED, 1000));
        t.mark(Situation.DAMAGED, 1000);
        assertFalse(t.ready(Situation.DAMAGED, 1039));
        assertTrue(t.ready(Situation.DAMAGED, 1040));
        assertTrue(t.ready(Situation.CUT, 1001), "another situation is its own memory");
        t.mark(Situation.IDLE, 2000);
        assertFalse(t.ready(Situation.IDLE, 2399));
        assertTrue(t.ready(Situation.IDLE, 2400));
        assertTrue(t.voiceReady(0));
        t.markVoice(5000);
        assertFalse(t.voiceReady(5059));
        assertTrue(t.voiceReady(5060));

        assertFalse(SpaceMoodRules.canReplace(Situation.IDLE, Mood.SNAP, 5), "an idle thought does not interrupt a snap");
        assertTrue(SpaceMoodRules.canReplace(Situation.IDLE, Mood.SNAP, 30), "once the snap is over it may");
        assertTrue(SpaceMoodRules.canReplace(Situation.IDLE, Mood.SLEEPY, 0));
        assertTrue(SpaceMoodRules.canReplace(Situation.CUT, Mood.CURIOUS, 3), "a cast always takes over");
        assertTrue(SpaceMoodRules.canReplace(Situation.DAMAGED, Mood.FLARE, 3));
        assertFalse(SpaceMoodRules.canReplace(Situation.CUT, Mood.FAREWELL, 3), "nothing interrupts a farewell");
        assertFalse(SpaceMoodRules.canReplace(Situation.UNLINK, Mood.FAREWELL, 10));
        assertTrue(SpaceMoodRules.canReplace(Situation.LINK, Mood.FAREWELL, 100), "a new spirit may start after the farewell");
    }

    @Test
    void aListenerHearsOnlyWithin32BlocksAndOnceEveryThreeSeconds() {
        assertEquals(32.0, SpaceMoodRules.VOICE_RADIUS);
        assertEquals(60, SpaceMoodRules.VOICE_GAP_LISTENER);
        assertTrue(SpaceMoodRules.hears(31.9 * 31.9, 1000, 0));
        assertTrue(SpaceMoodRules.hears(32.0 * 32.0, 1000, 0));
        assertFalse(SpaceMoodRules.hears(32.1 * 32.1, 1000, 0));
        assertFalse(SpaceMoodRules.hears(1.0, 1059, 1000), "heard one 59 ticks ago");
        assertTrue(SpaceMoodRules.hears(1.0, 1060, 1000));
        assertTrue(SpaceMoodRules.hears(1.0, 1000, Long.MIN_VALUE / 4), "a listener who never heard one");
    }

    @Test
    void theSleepyAndLowHealthRules() {
        assertFalse(SpaceMoodRules.sleepy(SpaceMoodRules.SLEEP_AFTER_TICKS - 1));
        assertTrue(SpaceMoodRules.sleepy(SpaceMoodRules.SLEEP_AFTER_TICKS));
        assertTrue(SpaceMoodRules.ownerLow(5, 20));
        assertFalse(SpaceMoodRules.ownerLow(6.1, 20));
        assertFalse(SpaceMoodRules.ownerLow(0, 20), "a dead owner is not 'low'");
        assertFalse(SpaceMoodRules.ownerLow(5, 0));
    }

    @Test
    void theParamWordKeepsTheMoodTheExpressionAndTheStampApartFromTheVariantAndTheHiddenBit() {
        int base = SpiritSpaceRules.param(SpiritSpaceRules.V_WISP, 0) | SpiritSpaceRules.HIDDEN;
        for (Mood m : Mood.values()) {
            for (Expression e : Expression.values()) {
                int p = SpaceMoodRules.withMood(base, m, e, 777777);
                assertEquals(m, SpaceMoodRules.moodOf(p));
                assertEquals(e, SpaceMoodRules.expressionOf(p));
                assertEquals(SpiritSpaceRules.V_WISP, SpiritSpaceRules.variant(p));
                assertTrue(SpiritSpaceRules.hidden(p));
                assertEquals(777777, SpiritSpaceRules.extra(p));
            }
        }
        assertTrue(Mood.values().length <= 16 && Expression.values().length <= 4, "they fit their bits");
        assertEquals(Mood.CALM, SpaceMoodRules.moodOf(0));
        assertEquals(Mood.CALM, Mood.of(99));
        assertEquals(Expression.CALM, Expression.of(-1));
        assertEquals(1, SpaceMoodRules.moodOf(SpaceMoodRules.withMood(0, Mood.CURIOUS, Expression.CALM, 5)).ordinal(), "the ordinals of the moods are fixed (they travel in the param): only ever append");
    }

    @Test
    void everyPoseStaysInsideItsBoundsAtEveryTick() {
        float[] o = new float[SpaceMoodRules.P_COUNT];
        for (Mood m : Mood.values()) {
            for (float since = 0; since <= 120; since += 0.5f) {
                SpaceMoodRules.pose(o, m, since);
                assertTrue(o[SpaceMoodRules.P_SCALE] >= 0f && o[SpaceMoodRules.P_SCALE] <= 1.5f, m + " scale " + o[SpaceMoodRules.P_SCALE]);
                assertTrue(o[SpaceMoodRules.P_SQUASH] >= 0.5f && o[SpaceMoodRules.P_SQUASH] <= 1.2f, m + " squash");
                assertTrue(o[SpaceMoodRules.P_ALPHA] >= 0f && o[SpaceMoodRules.P_ALPHA] <= 1f, m + " alpha");
                assertTrue(o[SpaceMoodRules.P_SHAKE] >= 0f && o[SpaceMoodRules.P_SHAKE] <= 0.05f, m + " shake");
                assertTrue(o[SpaceMoodRules.P_RADIUS] >= 0.1f && o[SpaceMoodRules.P_RADIUS] <= 1.01f, m + " radius");
                assertTrue(Math.abs(o[SpaceMoodRules.P_HEIGHT]) <= 0.6f, m + " height");
                assertTrue(o[SpaceMoodRules.P_COIL] >= 0f && o[SpaceMoodRules.P_COIL] <= 1f, m + " coil");
                assertTrue(o[SpaceMoodRules.P_RISE] >= 0f && o[SpaceMoodRules.P_RISE] <= 1f, m + " rise");
                assertTrue(o[SpaceMoodRules.P_CAST] == 0f || o[SpaceMoodRules.P_CAST] == 1f);
                assertTrue(o[SpaceMoodRules.P_RING] >= 0.3f && o[SpaceMoodRules.P_RING] <= 2.0f, m + " ring");
            }
        }
        SpaceMoodRules.pose(o, Mood.CALM, 50);
        assertEquals(1f, o[SpaceMoodRules.P_SCALE]);
        assertEquals(1f, o[SpaceMoodRules.P_ALPHA]);
        SpaceMoodRules.pose(o, Mood.FAREWELL, SpaceMoodRules.duration(Mood.FAREWELL));
        assertEquals(0f, o[SpaceMoodRules.P_ALPHA], 1e-6, "the farewell ends in nothing");
        SpaceMoodRules.pose(o, Mood.SLEEPY, 10);
        assertTrue(o[SpaceMoodRules.P_ALPHA] < 0.7f && o[SpaceMoodRules.P_HEIGHT] < 0f, "sleepy: dim and low");
        SpaceMoodRules.pose(o, Mood.CURIOUS, SpaceMoodRules.duration(Mood.CURIOUS) / 2f);
        assertTrue(o[SpaceMoodRules.P_RADIUS] < 0.6f, "curious: it drifts in toward the owner");
        boolean flickered = false;
        for (float s = 0; s < 16; s++) { SpaceMoodRules.pose(o, Mood.HURT, s); if (o[SpaceMoodRules.P_ALPHA] < 0.5f) flickered = true; }
        assertTrue(flickered, "hurt: a flicker");
    }

    @Test
    void everyExpressionHasItsOwnColourStrengthAndRingShape() {
        java.util.Set<Integer> tints = new java.util.HashSet<>(), eyes = new java.util.HashSet<>();
        for (Expression e : Expression.values()) {
            tints.add(SpaceMoodRules.tint(e));
            eyes.add(SpaceMoodRules.eye(e));
            assertTrue(SpaceMoodRules.strength(e) > 0f && SpaceMoodRules.strength(e) <= 1.25f);
            assertTrue(SpaceMoodRules.ringShape(e) > 0.5f && SpaceMoodRules.ringShape(e) < 1.5f);
        }
        assertEquals(4, tints.size());
        assertEquals(4, eyes.size());
        assertTrue(SpaceMoodRules.strength(Expression.SLEEPY) < SpaceMoodRules.strength(Expression.CALM), "sleepy is dim");
        assertTrue(SpaceMoodRules.strength(Expression.EXCITED) > SpaceMoodRules.strength(Expression.CALM));
        int angry = SpaceMoodRules.tint(Expression.ANGRY), excited = SpaceMoodRules.tint(Expression.EXCITED);
        assertTrue(((angry >> 16) & 255) > (angry & 255) * 0.9 && ((angry >> 16) & 255) > ((angry >> 8) & 255), "angry is red-violet");
        assertTrue(((excited >> 8) & 255) > ((excited >> 16) & 255) && (excited & 255) > 200, "excited is cyan");
    }
}
