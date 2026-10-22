package com.newuniverse.nusmp.firecast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules.Decision;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules.Event;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules.Mood;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules.Tracker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Salamander's moods (docs/magic/fire_casting.md, 4.2), pure: the table event -> pose + face + creature sound, the cooldowns that keep it from flickering, the idle clock, the param word and how a
 * client reads the pose back from the clock, and that every pose names a real clip of the animation files and every sound cue a real cue of the catalogue.
 */
class SalamanderMoodRulesTest {
    private static final Path ANIM = Path.of("src/main/resources/assets/nusmp/animations/entity");

    private static JsonObject clips(String form) throws IOException { return JsonParser.parseString(Files.readString(ANIM.resolve("firecast_" + form + ".animation.json"))).getAsJsonObject().getAsJsonObject("animations"); }

    private static void is(Mood m, int pose, int face, String cue) {
        assertNotNull(m);
        assertEquals(pose, m.pose());
        assertEquals(face, m.face());
        assertEquals(cue, m.cue());
    }

    // ------------------------------------------------------------------ the table
    @Test
    void theSmallFormsTableIsTheOneOfTheDoc() {
        is(SalamanderMoodRules.moodFor(Event.LINK, false), SalamanderMoodRules.GRIP_TIGHTEN, SalamanderMoodRules.FACE_ALERT, "trill");
        is(SalamanderMoodRules.moodFor(Event.UNLINK, false), SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.FACE_CALM, "whoosh");
        is(SalamanderMoodRules.moodFor(Event.CAST_LIGHT, false), SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.FACE_ALERT, "whoosh");
        is(SalamanderMoodRules.moodFor(Event.CAST_MID, false), SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.FACE_ANGRY, "growl");
        is(SalamanderMoodRules.moodFor(Event.CAST_HEAVY, false), SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.FACE_ROARING, "growl");
        is(SalamanderMoodRules.moodFor(Event.DAMAGED, false), SalamanderMoodRules.FLINCH, SalamanderMoodRules.FACE_ANGRY, "hiss");
        is(SalamanderMoodRules.moodFor(Event.LOW_HEALTH, false), SalamanderMoodRules.GRIP_TIGHTEN, SalamanderMoodRules.FACE_ALERT, "trill");
        is(SalamanderMoodRules.moodFor(Event.OWNER_KILL, false), SalamanderMoodRules.PUFF_UP, SalamanderMoodRules.FACE_SMUG, "chirp");
        is(SalamanderMoodRules.moodFor(Event.OWNER_DEATH, false), SalamanderMoodRules.FLINCH, SalamanderMoodRules.FACE_SLEEPY, "whimper");
        assertNull(SalamanderMoodRules.moodFor(Event.OWNER_MELEE, false), "the small form has no reaction to a swing");
        assertNull(SalamanderMoodRules.moodFor(Event.IDLE, false), "the idle poses come from the idle clock, not the table");
    }

    @Test
    void theBigFormsTableIsTheOneOfTheDoc() {
        is(SalamanderMoodRules.moodFor(Event.DRAGON_COME, true), SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.FACE_CALM, "roar");
        is(SalamanderMoodRules.moodFor(Event.CAST_HEAVY, true), SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.FACE_ROARING, null);
        is(SalamanderMoodRules.moodFor(Event.DAMAGED, true), SalamanderMoodRules.HURT, SalamanderMoodRules.FACE_ANGRY, "roar");
        is(SalamanderMoodRules.moodFor(Event.LOW_HEALTH, true), SalamanderMoodRules.WING_DISPLAY, SalamanderMoodRules.FACE_ANGRY, "dragon_flap");
        is(SalamanderMoodRules.moodFor(Event.OWNER_KILL, true), SalamanderMoodRules.ROAR, SalamanderMoodRules.FACE_ROARING, "roar");
        is(SalamanderMoodRules.moodFor(Event.OWNER_MELEE, true), SalamanderMoodRules.CLAW_SWIPE, SalamanderMoodRules.FACE_ANGRY, null);
        is(SalamanderMoodRules.moodFor(Event.OWNER_DEATH, true), SalamanderMoodRules.HURT, SalamanderMoodRules.FACE_SLEEPY, "whimper");
        assertNull(SalamanderMoodRules.moodFor(Event.LINK, true));
    }

    @Test
    void everyPoseAndFaceOfTheTableExistsAndEveryCueIsInTheCatalogue() {
        for (boolean dragon : new boolean[]{false, true}) {
            for (Event ev : Event.values()) {
                Mood m = SalamanderMoodRules.moodFor(ev, dragon);
                if (m == null) continue;
                assertTrue(m.pose() >= 0 && m.pose() < SalamanderMoodRules.poseCount(dragon), ev + " pose " + m.pose());
                assertTrue(m.face() >= 0 && m.face() < SalamanderMoodRules.FACES, ev + " face");
                if (m.cue() != null) assertTrue(SalamanderSoundSet.has(m.cue()), ev + " cue " + m.cue());
                assertTrue(m.priority() >= 0 && m.cooldown() >= 0);
            }
        }
        assertEquals(6, SalamanderMoodRules.FACES);
        for (String f : new String[]{"calm", "alert", "angry", "sleepy", "smug", "roaring"}) {
            boolean found = false;
            for (int i = 0; i < SalamanderMoodRules.FACES; i++) found |= SalamanderMoodRules.faceName(i).equals(f);
            assertTrue(found, f);
        }
    }

    @Test
    void theCastTierFollowsTheCostOfThePage() {
        assertEquals(Event.CAST_LIGHT, SalamanderMoodRules.castTier(0.0));
        assertEquals(Event.CAST_LIGHT, SalamanderMoodRules.castTier(7.9));
        assertEquals(Event.CAST_MID, SalamanderMoodRules.castTier(8.0));
        assertEquals(Event.CAST_MID, SalamanderMoodRules.castTier(19.9));
        assertEquals(Event.CAST_HEAVY, SalamanderMoodRules.castTier(FireCastRules.DRAGON_COST_PERCENT), "the pages that call the big form are the heavy casts");
        assertEquals(Event.CAST_HEAVY, SalamanderMoodRules.castTier(60.0));
    }

    @Test
    void lowHealthIsBelowThirtyPercentAndAliveOnly() {
        assertTrue(SalamanderMoodRules.lowHealth(5f, 20f));
        assertFalse(SalamanderMoodRules.lowHealth(6f, 20f), "exactly 30 percent is not low");
        assertFalse(SalamanderMoodRules.lowHealth(0f, 20f), "a dead owner is not 'low'");
        assertFalse(SalamanderMoodRules.lowHealth(5f, 0f));
    }

    // ------------------------------------------------------------------ poses are real clips
    @Test
    void everyPoseNamesAClipOfItsFormsAnimationFileWithTheLengthOfItsDuration() throws IOException {
        for (boolean dragon : new boolean[]{false, true}) {
            JsonObject clips = clips(dragon ? "dragon" : "salamander");
            for (int pose = 0; pose < SalamanderMoodRules.poseCount(dragon); pose++) {
                String name = SalamanderMoodRules.clipName(dragon, pose);
                assertTrue(clips.has(name), (dragon ? "dragon" : "small") + " clip " + name);
                JsonObject c = clips.getAsJsonObject(name);
                boolean loop = c.get("loop").getAsBoolean();
                assertEquals(SalamanderMoodRules.looping(dragon, pose), loop, name + " loops as the table says");
                if (!loop) assertEquals(SalamanderMoodRules.poseTicks(dragon, pose), (int) Math.round(c.get("animation_length").getAsDouble() * 20.0), name + ": the pose lasts as long as its clip");
            }
            for (String old : new String[]{"idle", "perch", "fly", "breathe"}) assertTrue(clips.has(old), "kept: " + old);
        }
    }

    @Test
    void theNamedPosesOfTheOwnersListAllExist() throws IOException {
        for (String c : new String[]{"grip_tighten", "head_tilt", "tail_flick", "wing_stretch", "yawn", "flinch", "puff_up", "sleepy_curl", "breathe"}) assertTrue(clips("salamander").has(c), "small " + c);
        for (String c : new String[]{"hover", "roar", "wing_display", "claw_swipe", "breathe", "hurt", "descend", "land"}) assertTrue(clips("dragon").has(c), "dragon " + c);
        assertEquals("sleepy_curl", SalamanderMoodRules.clipName(false, SalamanderMoodRules.SLEEPY_CURL));
        assertEquals("land", SalamanderMoodRules.clipName(true, SalamanderMoodRules.LAND));
    }

    // ------------------------------------------------------------------ the param word
    @Test
    void theParamWordKeepsEveryFieldApart() {
        for (int variant : new int[]{FireCastRules.V_SALAMANDER, FireCastRules.V_DRAGON})
            for (int pose = 0; pose < 10; pose++)
                for (int face = 0; face < SalamanderMoodRules.FACES; face++)
                    for (int stamp : new int[]{1, 77, 0x7FFFF, 0xFFFFF}) {
                        int p = SalamanderMoodRules.encode(variant, true, pose, face, stamp);
                        assertEquals(variant, FireCastRules.variant(p));
                        assertTrue(FireCastRules.hidden(p));
                        assertEquals(pose, SalamanderMoodRules.poseOf(p));
                        assertEquals(face, SalamanderMoodRules.faceOf(p));
                        assertEquals(stamp, SalamanderMoodRules.stampOf(p));
                        assertFalse(FireCastRules.hidden(SalamanderMoodRules.encode(variant, false, pose, face, stamp)));
                    }
        assertEquals(1, SalamanderMoodRules.stampOf(SalamanderMoodRules.encode(2, false, 2, 1, 0)), "a stamp of 0 would mean no pose, it becomes 1");
        assertEquals(5, SalamanderMoodRules.stampOf(SalamanderMoodRules.encode(2, false, 2, 1, 0x100005)), "only the low 20 bits of the game tick are kept");
        int old = FireCastRules.param(FireCastRules.V_SALAMANDER, 1234);
        assertEquals(SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.poseOf(old), "the old encoding reads as no pose");
        assertEquals(1234, SalamanderMoodRules.stampOf(old));
    }

    // ------------------------------------------------------------------ what a client draws
    @Test
    void theSmallFormPlaysAPoseFromItsStampForItsDurationAndThenGoesBackToThePerch() {
        int p = SalamanderMoodRules.encode(FireCastRules.V_SALAMANDER, false, SalamanderMoodRules.YAWN, SalamanderMoodRules.FACE_SLEEPY, 1000);
        int dur = SalamanderMoodRules.poseTicks(false, SalamanderMoodRules.YAWN);
        int look = SalamanderMoodRules.look(false, p, 1010, 50, 24000);
        assertEquals(SalamanderMoodRules.YAWN, SalamanderMoodRules.poseOfLook(look));
        assertEquals(SalamanderMoodRules.FACE_SLEEPY, SalamanderMoodRules.faceOfLook(look));
        assertEquals(10, SalamanderMoodRules.elapsedOfLook(look));
        look = SalamanderMoodRules.look(false, p, 1000 + dur - 1, 50, 24000);
        assertEquals(SalamanderMoodRules.YAWN, SalamanderMoodRules.poseOfLook(look), "the last tick of the pose");
        look = SalamanderMoodRules.look(false, p, 1000 + dur, 50, 24000);
        assertEquals(SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.poseOfLook(look), "the pose ends by itself");
        assertEquals(SalamanderMoodRules.FACE_CALM, SalamanderMoodRules.faceOfLook(look), "and the face goes back to calm with it");
        look = SalamanderMoodRules.look(false, p, 995, 50, 24000);
        assertEquals(SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.poseOfLook(look), "a stamp well ahead of the client's clock has not started yet");
        look = SalamanderMoodRules.look(false, p, 999, 50, 24000);
        assertEquals(SalamanderMoodRules.YAWN, SalamanderMoodRules.poseOfLook(look), "but a client one tick behind the server still plays it");
        assertEquals(SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.poseOfLook(SalamanderMoodRules.look(false, FireCastRules.param(FireCastRules.V_SALAMANDER, 0), 5, 5, 100)), "no stamp: the perch");
    }

    @Test
    void theOldEncodingStillPlaysTheBreath() {
        int old = FireCastRules.param(FireCastRules.V_SALAMANDER, 500);
        int look = SalamanderMoodRules.look(false, old, 510, 5, 24000);
        assertEquals(SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.poseOfLook(look));
        assertEquals(10, SalamanderMoodRules.elapsedOfLook(look));
        assertEquals(SalamanderMoodRules.POSE_NONE, SalamanderMoodRules.poseOfLook(SalamanderMoodRules.look(false, old, 500 + 32, 5, 24000)), "the breath lasts 32 ticks, as it did");
        int oldDragon = FireCastRules.param(FireCastRules.V_DRAGON, FireCastRules.DRAGON_BREATH_TICK);
        assertEquals(SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.poseOfLook(SalamanderMoodRules.look(true, oldDragon, 0, 10, 60)), "the dragon's breath begins at its life tick 8");
        assertEquals(SalamanderMoodRules.DESCEND, SalamanderMoodRules.poseOfLook(SalamanderMoodRules.look(true, oldDragon, 0, 3, 60)), "and before that it descends");
    }

    @Test
    void theDragonsTimelineIsDescendThenItsPosesThenHoverThenLand() {
        long now = 5000;
        int summon = SalamanderMoodRules.dragonSummonParam(now);
        int maxLife = FireCastRules.DRAGON_TICKS;
        for (int life = 0; life < SalamanderMoodRules.DRAGON_ARRIVE_TICKS; life++) {
            int look = SalamanderMoodRules.look(true, summon, now + life, life, maxLife);
            assertEquals(SalamanderMoodRules.DESCEND, SalamanderMoodRules.poseOfLook(look), "life " + life);
            assertEquals(life, SalamanderMoodRules.elapsedOfLook(look));
        }
        int look = SalamanderMoodRules.look(true, summon, now + 8, 8, maxLife);
        assertEquals(SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.poseOfLook(look), "the breath begins on tick 8");
        assertEquals(SalamanderMoodRules.FACE_ROARING, SalamanderMoodRules.faceOfLook(look));
        assertEquals(0, SalamanderMoodRules.elapsedOfLook(look));
        look = SalamanderMoodRules.look(true, summon, now + 47, 47, maxLife);
        assertEquals(SalamanderMoodRules.POSE_BREATHE, SalamanderMoodRules.poseOfLook(look));
        look = SalamanderMoodRules.look(true, summon, now + 48, 48, maxLife);
        assertEquals(SalamanderMoodRules.LAND, SalamanderMoodRules.poseOfLook(look), "the last ticks are the landing");
        // with a longer life a hover fills the time between the breath and the landing
        look = SalamanderMoodRules.look(true, summon, now + 60, 60, 100);
        assertEquals(SalamanderMoodRules.HOVER, SalamanderMoodRules.poseOfLook(look));
        assertEquals(SalamanderMoodRules.FACE_CALM, SalamanderMoodRules.faceOfLook(look));
        // a roar set by the server mid-life plays from its stamp
        int roar = SalamanderMoodRules.encode(FireCastRules.V_DRAGON, false, SalamanderMoodRules.ROAR, SalamanderMoodRules.FACE_ROARING, (int) (now + 50));
        look = SalamanderMoodRules.look(true, roar, now + 55, 55, 100);
        assertEquals(SalamanderMoodRules.ROAR, SalamanderMoodRules.poseOfLook(look));
        assertEquals(5, SalamanderMoodRules.elapsedOfLook(look));
        // descend, hover and land are never read from the param (only the clock decides them)
        int fake = SalamanderMoodRules.encode(FireCastRules.V_DRAGON, false, SalamanderMoodRules.LAND, 1, (int) now);
        assertEquals(SalamanderMoodRules.HOVER, SalamanderMoodRules.poseOfLook(SalamanderMoodRules.look(true, fake, now + 2, 30, 100)));
    }

    @Test
    void sinceHandlesTheWrapOfTheTwentyBitClock() {
        assertEquals(10, SalamanderMoodRules.since(1010, 1000));
        assertEquals(10, SalamanderMoodRules.since(0x100000 + 5, 0xFFFFB), "across the wrap of 2^20 ticks (14 hours)");
        assertEquals(-1, SalamanderMoodRules.since(1000, 1010), "ahead of the clock: not yet");
        assertEquals(0, SalamanderMoodRules.since(1000, 1001), "one tick ahead counts as now");
    }

    // ------------------------------------------------------------------ cooldowns: nothing flickers
    @Test
    void aSituationOnCooldownDoesNothingTwice() {
        Tracker t = new Tracker();
        Decision a = t.decide(Event.DAMAGED, false, 1000);
        assertEquals(SalamanderMoodRules.FLINCH, a.pose());
        assertEquals("hiss", a.cue());
        assertEquals(SalamanderMoodRules.NOTHING, t.decide(Event.DAMAGED, false, 1020), "hurt again 1 s later: nothing");
        assertEquals(SalamanderMoodRules.NOTHING, t.decide(Event.DAMAGED, false, 1049));
        assertEquals(SalamanderMoodRules.FLINCH, t.decide(Event.DAMAGED, false, 1050).pose(), "after the cooldown it flinches again");
    }

    @Test
    void aPoseIsHeldUntilItEndsUnlessSomethingMoreImportantComes() {
        Tracker t = new Tracker();
        assertEquals(SalamanderMoodRules.PUFF_UP, t.decide(Event.OWNER_KILL, false, 100).pose());      // priority 4, 36 ticks
        Decision link = t.decide(Event.LINK, false, 110);                                              // priority 3: outranked while the puff plays
        assertEquals(SalamanderMoodRules.POSE_NONE, link.pose(), "a lesser pose does not interrupt");
        Decision hurt = t.decide(Event.DAMAGED, false, 120);                                           // priority 6 interrupts
        assertEquals(SalamanderMoodRules.FLINCH, hurt.pose());
        assertNull(hurt.cue(), "but the sound gap of one Salamander still holds (the puff chirped 20 ticks ago)");
        assertEquals(SalamanderMoodRules.POSE_BREATHE, t.decide(Event.CAST_LIGHT, false, 120 + 20).pose(), "after the flinch is over and the minimum gap has passed a cast takes over");
    }

    @Test
    void twoPosesAreNeverCloserThanTheMinimumGapUnlessOneOutranksTheOther() {
        Tracker t = new Tracker();
        assertEquals(SalamanderMoodRules.POSE_BREATHE, t.decide(Event.CAST_MID, false, 0).pose());
        assertEquals(SalamanderMoodRules.POSE_NONE, t.decide(Event.OWNER_KILL, false, 5).pose(), "a kill (same priority) right behind a cast waits");
        assertEquals(SalamanderMoodRules.FLINCH, t.decide(Event.DAMAGED, false, 6).pose(), "damage outranks and goes through at once");
    }

    @Test
    void aCreatureSoundIsAtMostOnePerThreeSecondsForOneSalamander() {
        Tracker t = new Tracker();
        assertEquals("trill", t.decide(Event.LINK, false, 0).cue());
        assertNull(t.decide(Event.DAMAGED, false, 30).cue(), "1.5 s later: the pose may change, the sound is held back");
        assertEquals("growl", t.decide(Event.CAST_MID, false, 30 + 14 + 60).cue(), "once the gap is over a sound plays again");
        assertEquals(60, SalamanderMoodRules.SOUND_GAP);
        assertEquals(SalamanderSoundSet.LISTENER_GAP, SalamanderMoodRules.SOUND_GAP, "the Salamander's own gap equals the per listener gap");
    }

    @Test
    void aLongRandomDayNeverFlickersAndNeverSpeaksTooOften() {
        for (boolean dragon : new boolean[]{false, true}) {
            Tracker t = new Tracker();
            long seed = dragon ? 7 : 3, lastPose = -1000, lastSound = -1000;
            int lastPrio = 0;
            long until = 0;
            int poses = 0, sounds = 0;
            Event[] evs = Event.values();
            for (long now = 0; now < 60000; now += 1 + (seed = (seed * 6364136223846793005L + 1442695040888963407L)) % 7 + 1) {
                Event ev = evs[(int) ((seed >>> 33) % evs.length)];
                Decision d = ev == Event.IDLE ? t.idle(dragon, now) : t.decide(ev, dragon, now);
                if (d.changesParam()) {
                    Mood m = ev == Event.IDLE ? null : SalamanderMoodRules.moodFor(ev, dragon);
                    int prio = m == null ? 1 : m.priority();
                    boolean outranks = prio > lastPrio && now < until;
                    assertTrue(now - lastPose >= SalamanderMoodRules.MIN_POSE_GAP || outranks || now >= until, "a pose at " + now + " only " + (now - lastPose) + " ticks after the last");
                    lastPose = now;
                    lastPrio = prio;
                    until = now + d.ticks();
                    poses++;
                }
                if (d.cue() != null) {
                    assertTrue(now - lastSound >= SalamanderMoodRules.SOUND_GAP, "a sound at " + now + " only " + (now - lastSound) + " ticks after the last");
                    lastSound = now;
                    sounds++;
                }
            }
            assertTrue(poses > 0 && sounds > 0, "something happened");
            assertTrue(sounds <= 60000 / SalamanderMoodRules.SOUND_GAP + 1, "at most one sound per three seconds in all: " + sounds);
        }
    }

    // ------------------------------------------------------------------ the idle clock
    @Test
    void afterTenQuietSecondsItTurnsItsHeadThenFlicksItsTailThenStretchesThenYawnsAndStartsOver() {
        Tracker t = new Tracker();
        t.begin(0);
        java.util.List<Integer> seen = new java.util.ArrayList<>();
        for (long now = 0; now < SalamanderMoodRules.SLEEP_AFTER - 50; now += 10) {                       // asked every half second, as the prop's tick does
            Decision d = t.idle(false, now);
            if (d.pose() != SalamanderMoodRules.POSE_NONE) {
                assertTrue(now >= SalamanderMoodRules.IDLE_AFTER, "not before the quiet time is over: " + now);
                seen.add(d.pose());
            }
        }
        assertEquals(java.util.List.of(SalamanderMoodRules.HEAD_TILT, SalamanderMoodRules.TAIL_FLICK, SalamanderMoodRules.WING_STRETCH, SalamanderMoodRules.YAWN, SalamanderMoodRules.HEAD_TILT), seen,
                "the idle poses come in turn and start over");
    }

    @Test
    void anythingThatHappensRestartsTheQuietClock() {
        Tracker t = new Tracker();
        t.begin(0);
        t.decide(Event.OWNER_KILL, false, 150);
        assertEquals(SalamanderMoodRules.NOTHING, t.idle(false, 250), "a kill 100 ticks ago: still not idle");
        assertEquals(SalamanderMoodRules.HEAD_TILT, t.idle(false, 150 + SalamanderMoodRules.IDLE_AFTER + 40).pose());
    }

    @Test
    void aLongCalmSendsItToSleepAndItWakesWhenSomethingHappens() {
        Tracker t = new Tracker();
        t.begin(0);
        Decision d = Decision(t, SalamanderMoodRules.SLEEP_AFTER);
        assertEquals(SalamanderMoodRules.SLEEPY_CURL, d.pose());
        assertEquals(SalamanderMoodRules.FACE_SLEEPY, d.face());
        assertEquals("purr", d.cue());
        assertEquals(SalamanderMoodRules.NOTHING, t.idle(false, SalamanderMoodRules.SLEEP_AFTER + 100), "it stays curled up");
        Decision hurt = t.decide(Event.DAMAGED, false, SalamanderMoodRules.SLEEP_AFTER + 120);
        assertEquals(SalamanderMoodRules.FLINCH, hurt.pose(), "damage wakes it at once");
        // after the curl it is awake and the long calm starts over: not asleep again until another SLEEP_AFTER ticks have passed
        Tracker u = new Tracker();
        u.begin(0);
        long wake = SalamanderMoodRules.SLEEP_AFTER + SalamanderMoodRules.poseTicks(false, SalamanderMoodRules.SLEEPY_CURL);
        assertEquals(SalamanderMoodRules.SLEEPY_CURL, Decision(u, SalamanderMoodRules.SLEEP_AFTER).pose());
        Decision next = u.idle(false, wake + 400);
        assertTrue(next.pose() != SalamanderMoodRules.SLEEPY_CURL, "it does not go straight back to sleep");
    }

    private static Decision Decision(Tracker t, long now) { return t.idle(false, now); }

    @Test
    void theBigFormNeverIdlesAndTheSmallFormIsQuietUntilItHasStarted() {
        Tracker t = new Tracker();
        assertEquals(SalamanderMoodRules.NOTHING, t.idle(true, 99999));
        assertEquals(SalamanderMoodRules.NOTHING, t.idle(false, 5), "the first ask only starts the clock");
        t.reset();
        assertEquals(SalamanderMoodRules.NOTHING, t.idle(false, 1_000_000));
    }

    @Test
    void theDragonsSummonIsTrackedSoALaterEventDoesNotCutItOff() {
        Tracker t = new Tracker();
        t.playing(SalamanderMoodRules.POSE_BREATHE, 100, 100 + 8 + 40);
        assertEquals(4, t.playingPriority(120));
        Decision cast = t.decide(Event.CAST_HEAVY, true, 120);
        assertEquals(SalamanderMoodRules.POSE_NONE, cast.pose(), "a second cast during the breath does not restart it");
        assertEquals(SalamanderMoodRules.ROAR, t.decide(Event.OWNER_KILL, true, 125).pose(), "but a kill (priority 5) makes the dragon roar");
        assertEquals("roar", t.decide(Event.DAMAGED, true, 190).cue());
    }

    // ------------------------------------------------------------------ the per listener sound gate
    @Test
    void aListenerHearsAtMostOneSalamanderSoundPerThreeSecondsAndACueNeverInsideItsCooldown() {
        SalamanderSoundSet.Gate g = new SalamanderSoundSet.Gate();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(g.tryAcquire(a, "chirp", 1000));
        assertFalse(g.tryAcquire(a, "hiss", 1010), "another cue 0.5 s later is refused");
        assertFalse(g.tryAcquire(a, "growl", 1059));
        assertTrue(g.tryAcquire(a, "growl", 1060), "3 s later it is allowed");
        assertTrue(g.tryAcquire(b, "chirp", 1000), "another listener has its own record");
        assertFalse(g.tryAcquire(a, "chirp", 1100), "chirp's own cooldown (from 1000) is over, but the growl at 1060 holds the 3 s gap until 1120");
        assertTrue(g.tryAcquire(a, "chirp", 1125));
    }

    @Test
    void aRefusedCueDoesNotUseUpTheListenersGapAndTheGateForgets() {
        SalamanderSoundSet.Gate g = new SalamanderSoundSet.Gate();
        UUID a = UUID.randomUUID();
        assertTrue(g.tryAcquire(a, "chirp", 2000));
        assertFalse(g.tryAcquire(a, "chirp", 2061), "the same cue inside its 100 tick cooldown is refused");
        assertTrue(g.tryAcquire(a, "hiss", 2062), "and that refusal did not use up the gap: another cue is heard as soon as the 3 s are over");
        assertFalse(g.tryAcquire(a, "not_a_cue", 5000), "a cue that is not in the catalogue is never played");
        assertFalse(g.tryAcquire(null, "trill", 5000));
        g.forget(a);
        assertEquals(0, g.tracked(), "forget drops the record");
        assertTrue(g.tryAcquire(a, "chirp", 2063), "a listener that logged out and in starts fresh");
        g.clear();
        assertEquals(0, g.tracked());
    }

    @Test
    void everyEventOfTheTableUsesADistinctOrSharedCueFromTheCatalogue() {
        Set<String> used = new HashSet<>();
        for (boolean dragon : new boolean[]{false, true}) for (Event ev : Event.values()) {
            Mood m = SalamanderMoodRules.moodFor(ev, dragon);
            if (m != null && m.cue() != null) used.add(m.cue());
        }
        for (String need : new String[]{"trill", "growl", "hiss", "whoosh", "chirp", "whimper", "roar", "dragon_flap"}) assertTrue(used.contains(need), "the table uses " + need);
        for (String idle : new String[]{"trill", "flap", "yawn", "purr"}) assertTrue(SalamanderSoundSet.has(idle), idle);
    }
}
