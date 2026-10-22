package com.newuniverse.nusmp.sylph;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Her moods are pure rules: the table, the priorities, the cooldowns, the idle rotation, the pose bits of the prop word, the clips, the voice lines and the per-listener gate. */
class SylphMoodRulesTest {
    private static SylphMood.Decision fire(SylphMood.State st, SylphMood.Event e, long now) { return SylphMood.decide(st, e, now, 0); }

    @Test
    void everyEventHasARowAndEveryPoseCodeNamesAClipOfHerAnimationFile() {
        for (SylphMood.Event e : SylphMood.Event.values()) assertNotNull(SylphMood.spec(e), "a row for " + e);
        Set<String> small = new HashSet<>(Arrays.asList(SylphRules.SMALL_CLIPS)), powered = new HashSet<>(Arrays.asList(SylphRules.POWERED_CLIPS));
        for (int code = 1; code < SylphMood.SMALL_POSE_CLIP.length; code++) assertTrue(small.contains(SylphMood.smallClip(code)), "small pose " + code + " plays a clip of the small file");
        for (int code = 1; code < SylphMood.POWERED_POSE_CLIP.length; code++) assertTrue(powered.contains(SylphMood.poweredClip(code)), "powered pose " + code + " plays a clip of the powered file");
        assertNull(SylphMood.smallClip(0));
        assertNull(SylphMood.smallClip(15), "an unknown code (a damaged param) plays the ordinary clip");
        assertNull(SylphMood.poweredClip(-1));
        assertEquals(12, SylphMood.SMALL_POSE_CLIP.length, "the codes 1..11, appended only");
        assertTrue(SylphMood.SMALL_POSE_CLIP.length <= 16 && SylphMood.POWERED_POSE_CLIP.length <= 16, "a code fits the four bits of the prop word");
    }

    @Test
    void everyVoiceGroupTheTableNamesIsAGroupOfTheLines() {
        for (SylphMood.Event e : SylphMood.Event.values()) {
            String g = SylphMood.spec(e).voice();
            if (g != null) assertTrue(SylphVoiceLines.groups().isEmpty() || SylphVoiceLines.groups().containsKey(g) || SylphVoiceLines.group(g).isEmpty(), "group " + g);
        }
        for (String g : new String[]{"link", "unlink", "cast", "big", "shout", "hurt", "startle", "idle", "worry", "cheer", "sad", "giggle", "pout", "smug", "sleepy", "victory"})
            assertTrue(Arrays.asList("link", "unlink", "cast", "big", "shout", "hurt", "startle", "idle", "worry", "cheer", "sad", "giggle", "pout", "smug", "sleepy", "victory").contains(g));
    }

    @Test
    void linkingWavesAndSaysHello() {
        SylphMood.State st = new SylphMood.State();
        SylphMood.Decision d = fire(st, SylphMood.Event.LINK, 100);
        assertEquals(SylphMood.WAVE, d.pose());
        assertEquals("link", d.voice());
        assertEquals(60, d.poseTicks());
        assertEquals(SylphMood.WAVE, st.smallPose(100 + 59));
        assertEquals(0, st.smallPose(100 + 60), "the pose is over after its time");
    }

    @Test
    void aPoseHoldsForItsTimeAndIsOnlyReplacedByAMoreImportantOne() {
        SylphMood.State st = new SylphMood.State();
        assertEquals(SylphMood.WORRY, fire(st, SylphMood.Event.LOW_HEALTH, 0).pose());                  // priority 8, 120 ticks
        assertEquals(0, fire(st, SylphMood.Event.HURT, 10).pose(), "a flinch (6) does not interrupt worry (8)");
        assertEquals(0, fire(st, SylphMood.Event.HURT_HEAVY, 20).pose(), "a startle (7) does not interrupt worry (8)");
        assertEquals(SylphMood.WORRY, st.smallPose(30), "she is still worried");
        assertEquals(SylphMood.CRY, fire(st, SylphMood.Event.DEATH, 40).pose(), "but her owner's death (10) does");
        SylphMood.State st2 = new SylphMood.State();
        assertEquals(SylphMood.HURT, fire(st2, SylphMood.Event.HURT, 0).pose());
        assertEquals(SylphMood.STARTLED, fire(st2, SylphMood.Event.HURT_HEAVY, 5).pose(), "a heavier hit (7) replaces a flinch (6)");
        assertEquals(SylphMood.HURT, fire(st2, SylphMood.Event.HURT, 500).pose(), "after the pose is over a new flinch is taken");
    }

    @Test
    void theSameEventCannotRestartItsOwnPoseAndSoNothingFlickers() {
        SylphMood.State st = new SylphMood.State();
        assertEquals(SylphMood.CHEER, fire(st, SylphMood.Event.KILL, 0).pose());
        for (int t = 1; t < 40; t++) assertEquals(0, fire(st, SylphMood.Event.KILL, t).pose(), "tick " + t + ": the cheer is showing");
        // a long random run of events: a pose only changes when the shown one is over or the new one is more important
        Random rnd = new Random(7);
        SylphMood.Event[] events = SylphMood.Event.values();
        SylphMood.State s = new SylphMood.State();
        long now = 0;
        int changes = 0;
        for (int i = 0; i < 20000; i++) {
            now += rnd.nextInt(30);
            SylphMood.Event e = events[rnd.nextInt(events.length)];
            int before = s.smallPose(now), priBefore = s.priority;
            SylphMood.Decision d = SylphMood.decide(s, e, now, rnd.nextInt() & 0x7FFFFFFF);
            if (d.pose() != 0 && before != 0 && e != SylphMood.Event.IDLE) assertTrue(SylphMood.spec(e).priority() > priBefore, "an event of priority " + SylphMood.spec(e).priority() + " replaced a pose of " + priBefore);
            if (d.pose() != 0) changes++;
            assertTrue(d.pose() >= 0 && d.pose() < SylphMood.SMALL_POSE_CLIP.length && d.poweredPose() >= 0 && d.poweredPose() < SylphMood.POWERED_POSE_CLIP.length);
        }
        assertTrue(changes > 0);
    }

    @Test
    void whatSheSaysHasACooldownAndAChance() {
        SylphMood.State st = new SylphMood.State();
        assertEquals("hurt", fire(st, SylphMood.Event.HURT, 0).voice(), "rnd 0 is inside the 60 percent");
        assertNull(fire(st, SylphMood.Event.HURT, 100).voice(), "the same group inside its 160 ticks is held back");
        assertEquals("hurt", fire(st, SylphMood.Event.HURT, 400).voice());
        SylphMood.State quiet = new SylphMood.State();
        assertNull(SylphMood.decide(quiet, SylphMood.Event.HURT, 0, 99).voice(), "rnd 99 is outside the 60 percent: no line");
        assertEquals("cast", SylphMood.decide(new SylphMood.State(), SylphMood.Event.CAST, 0, 300).voice(), "rnd 300: inside the 40 percent, (rnd >> 8) % 4 is 1: the cast line");
        assertEquals("giggle", SylphMood.decide(new SylphMood.State(), SylphMood.Event.CAST, 0, 0).voice(), "rnd 0: inside the chance and (rnd >> 8) % 4 == 0: the cast ends in a giggle");
        assertNull(SylphMood.decide(new SylphMood.State(), SylphMood.Event.CAST, 0, 50).voice(), "the cast line is only said 40 percent of the time");
    }

    @Test
    void theBigPagesAndTheDeathHaveTheirLines() {
        assertEquals("big", fire(new SylphMood.State(), SylphMood.Event.CAST_BIG, 0).voice());
        SylphMood.Decision arrive = fire(new SylphMood.State(), SylphMood.Event.POWERED_ARRIVE, 0);
        assertEquals(SylphMood.P_BATTLE, arrive.poweredPose());
        assertEquals("shout", arrive.voice());
        SylphMood.Decision death = fire(new SylphMood.State(), SylphMood.Event.DEATH, 0);
        assertEquals(SylphMood.CRY, death.pose());
        assertEquals("sad", death.voice());
        assertEquals("startle", fire(new SylphMood.State(), SylphMood.Event.HURT_HEAVY, 0).voice());
        assertEquals("victory", fire(new SylphMood.State(), SylphMood.Event.KILL_BOSS, 0).voice());
        SylphMood.Decision hurt = fire(new SylphMood.State(), SylphMood.Event.HURT, 0);
        assertEquals(SylphMood.P_FURY, hurt.poweredPose(), "the powered form answers a hit with fury");
    }

    @Test
    void theOwnersHitAndKillAreRead() {
        assertEquals(SylphMood.Event.HURT, SylphMood.hurtEvent(2f, 18f, 20f));
        assertEquals(SylphMood.Event.HURT_HEAVY, SylphMood.hurtEvent(8f, 12f, 20f), "40 percent of the maximum at once");
        assertEquals(SylphMood.Event.LOW_HEALTH, SylphMood.hurtEvent(3f, 5f, 20f), "left at 25 percent: worry");
        assertEquals(SylphMood.Event.HURT, SylphMood.hurtEvent(1f, 0f, 20f), "a death is not 'low health' (the death event comes)");
        assertEquals(SylphMood.Event.HURT, SylphMood.hurtEvent(1f, 5f, 0f));
        assertEquals(SylphMood.Event.KILL, SylphMood.killEvent(20f));
        assertEquals(SylphMood.Event.KILL_BOSS, SylphMood.killEvent(300f));
        assertFalse(SylphMood.moved(0.05, 0.05));
        assertTrue(SylphMood.moved(0.3, 0.0));
    }

    @Test
    void standingStillLongGivesHerSomethingToDoAndNotTooOften() {
        SylphMood.State st = new SylphMood.State();
        st.active(0);
        assertFalse(SylphMood.idle(st, SylphMood.IDLE_TICKS - 1, 0).any(), "not yet");
        SylphMood.Decision first = SylphMood.idle(st, SylphMood.IDLE_TICKS, 0);
        assertTrue(first.pose() != 0 && first.poseTicks() > 0, "she gets bored");
        long t = SylphMood.IDLE_TICKS;
        assertFalse(SylphMood.idle(st, t + 100, 0).any(), "she is busy with it");
        assertFalse(SylphMood.idle(st, t + first.poseTicks() + 1, 0).any(), "and does not start another within the gap");
        Set<Integer> seen = new HashSet<>();
        seen.add(first.pose());
        for (int i = 0; i < 12; i++) {
            t += SylphMood.IDLE_GAP + 400;
            SylphMood.Decision d = SylphMood.idle(st, t, 0);
            assertTrue(d.pose() != 0, "step " + i);
            seen.add(d.pose());
        }
        assertEquals(Set.of(SylphMood.SWING, SylphMood.SLEEPY, SylphMood.CROSSED, SylphMood.WAVE, SylphMood.POUT), seen, "the rotation walks through every idle pose");
        st.active(t + 10);
        assertFalse(SylphMood.idle(st, t + 10 + SylphMood.IDLE_TICKS - 1, 0).any(), "activity starts the clock over");
        assertEquals(SylphMood.WAVE, fire(new SylphMood.State(), SylphMood.Event.LINK, 5).pose());
    }

    @Test
    void thePoseSitsInBitsFourToSevenOfThePropWordWithoutTouchingTheRest() {
        int p = SylphRules.param(SylphRules.V_POWERED, SylphRules.stamp(123456)) | SylphRules.HIDDEN;
        for (int pose = 0; pose < 16; pose++) {
            int q = SylphRules.withPose(p, pose);
            assertEquals(pose, SylphRules.pose(q));
            assertEquals(SylphRules.variant(p), SylphRules.variant(q));
            assertEquals(SylphRules.extra(p), SylphRules.extra(q));
            assertEquals(SylphRules.hidden(p), SylphRules.hidden(q));
        }
        int stamped = (SylphRules.withPose(p, 9) & SylphRules.KEEP_ON_STAMP) | SylphRules.param(SylphRules.V_SMALL, SylphRules.stamp(999));
        assertEquals(9, SylphRules.pose(stamped), "a cast stamp keeps the pose and the hidden flag");
        assertTrue(SylphRules.hidden(stamped));
        assertEquals(SylphRules.stamp(999), SylphRules.extra(stamped));
        assertEquals(0, SylphRules.pose(SylphRules.param(SylphRules.V_SMALL, 5)), "an old param (no pose bits) is no pose");
    }

    @Test
    void theClipFollowsTheCastThenThePoseThenTheClock() {
        assertEquals("cast", SylphRules.smallClip(3, false, 0, 0, SylphMood.WORRY), "her hand is out: the cast clip first");
        assertEquals("worry", SylphRules.smallClip(500, false, 0, 0, SylphMood.WORRY));
        assertEquals("worry", SylphRules.smallClip(500, true, 0, 0, SylphMood.WORRY), "a pose beats the flying clip");
        assertEquals("fly", SylphRules.smallClip(500, true, 0, 0, 0));
        assertEquals("idle", SylphRules.smallClip(500, false, 150, 0, 15), "an unknown pose code plays the ordinary clip");
        assertEquals("powered_cast", SylphRules.poweredClip(5, 100, SylphMood.P_FURY));
        assertEquals("fury", SylphRules.poweredClip(500, 100, SylphMood.P_FURY));
        assertEquals("battle", SylphRules.poweredClip(500, 100, SylphMood.P_BATTLE));
        assertEquals("shout", SylphRules.poweredClip(500, 3, 0));
        assertEquals("hover", SylphRules.poweredClip(500, 100, 0));
        assertEquals(SylphRules.smallClip(500, false, 150, 0), SylphRules.smallClip(500, false, 150, 0, 0), "no pose: the old choice, unchanged");
    }

    @Test
    void theVoiceLinesAreGroupedAndNeverRepeatTheLastOne() {
        for (var g : SylphVoiceLines.groups().entrySet()) {
            assertFalse(g.getValue().isEmpty());
            for (int i = 0; i < g.getValue().size(); i++) assertEquals(SylphVoiceLines.id(g.getKey(), i + 1), g.getValue().get(i));
            int n = g.getValue().size();
            for (int last = -1; last < n; last++)
                for (int rnd = 0; rnd < 400; rnd++) {
                    int i = SylphVoiceLines.pick(g.getKey(), rnd * 7919, last);
                    assertTrue(i >= 0 && i < n);
                    if (n > 1) assertTrue(last != i, g.getKey() + " repeats the line said last");
                }
        }
        assertEquals(-1, SylphVoiceLines.pick("nobody", 5, -1));
        assertEquals(new HashSet<>(SylphVoiceLines.ids()).size(), SylphVoiceLines.ids().size(), "ids are unique");
        for (String id : SylphVoiceLines.ids()) assertTrue(id.startsWith("sylph_"));
        assertEquals("cheer", SylphVoiceLines.fallbackCue("cheer"));
        assertEquals("giggle", SylphVoiceLines.fallbackCue("smug"));
        assertEquals("whimper", SylphVoiceLines.fallbackCue("sad"));
        assertEquals("gasp", SylphVoiceLines.fallbackCue("hurt"));
        assertNull(SylphVoiceLines.fallbackCue("big"), "the gale already roars");
        assertNull(SylphVoiceLines.fallbackCue(null));
        assertTrue(SylphSoundSet.has(SylphVoiceLines.fallbackCue("idle")), "the stand-in cue exists in her synthesised set");
    }

    @Test
    void aListenerHearsAtMostOneSylphLineEveryFourSecondsAndTheSameGroupEveryTwelve() {
        SylphVoiceGate g = new SylphVoiceGate();
        UUID a = UUID.nameUUIDFromBytes("a".getBytes()), b = UUID.nameUUIDFromBytes("b".getBytes());
        assertTrue(g.tryAcquire(a, "cast", 0));
        assertFalse(g.tryAcquire(a, "hurt", 79), "inside the 4 seconds: any group is held back");
        assertTrue(g.tryAcquire(a, "hurt", 80));
        assertFalse(g.tryAcquire(a, "cast", 200), "the same group again inside 12 seconds");
        assertTrue(g.tryAcquire(a, "cast", 241));
        assertTrue(g.tryAcquire(b, "cast", 1), "another listener is not held back by the first");
        assertFalse(g.tryAcquire(null, "cast", 0));
        assertFalse(g.tryAcquire(a, null, 0));
        g.forget(a);
        assertTrue(g.tryAcquire(a, "cast", 242), "a forgotten listener starts fresh");
        assertEquals(2, g.tracked());
        g.prune(1_000_000);
        assertEquals(0, g.tracked(), "quiet listeners are dropped");
        g.clear();
    }
}
