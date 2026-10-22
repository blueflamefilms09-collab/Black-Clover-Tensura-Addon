package com.newuniverse.nusmp.undine;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The mood of the Undine is a pure table, so it is pinned here: what each event makes her do, show and say, that she never flickers (cooldowns, priorities, a minimum gap between two pose changes), that she
 * idles only after a long quiet, and that the data of the poses, faces, lines and speech limits are well formed.
 */
class UndineMoodRulesTest {
    private static UndineMoodRules.State fresh() {
        UndineMoodRules.State s = new UndineMoodRules.State();
        s.reset(0);
        return s;
    }

    @Test
    void everyEventGivesTheMoodOfTheTable() {
        record Row(UndineMoodRules.Event e, UndinePose pose, UndineFace face, UndineLines.Group g) {}
        List<Row> rows = List.of(
                new Row(UndineMoodRules.Event.LINK, UndinePose.RISE, UndineFace.SMILE, UndineLines.Group.LINK),
                new Row(UndineMoodRules.Event.UNLINK, UndinePose.FAREWELL, UndineFace.SERENE, UndineLines.Group.FAREWELL),
                new Row(UndineMoodRules.Event.CAST_SMALL, UndinePose.CAST, UndineFace.CALM, UndineLines.Group.CAST),
                new Row(UndineMoodRules.Event.CAST_BIG, UndinePose.POWER, UndineFace.ANGRY, UndineLines.Group.WAVE),
                new Row(UndineMoodRules.Event.OWNER_HURT, UndinePose.FLINCH, UndineFace.SURPRISED, UndineLines.Group.HURT),
                new Row(UndineMoodRules.Event.OWNER_LOW_HP, UndinePose.SORROW, UndineFace.SAD, UndineLines.Group.LOWHP),
                new Row(UndineMoodRules.Event.OWNER_KILL, UndinePose.WAVE, UndineFace.SMILE, UndineLines.Group.WIN));
        for (Row r : rows) {
            UndineMoodRules.Mood m = UndineMoodRules.decide(fresh(), r.e(), 1000, 0);
            assertNotNull(m, r.e().name());
            assertEquals(r.pose(), m.pose(), r.e().name());
            assertEquals(r.face(), m.face(), r.e().name());
            assertEquals(r.g(), m.speech(), r.e().name());
        }
        UndineMoodRules.Mood died = UndineMoodRules.decide(fresh(), UndineMoodRules.Event.OWNER_DIED, 1000, 0);
        assertNull(died.pose(), "a death has nobody to pose for");
        assertEquals(UndineLines.Group.DIED, died.speech());
        assertNull(UndineMoodRules.base(UndineMoodRules.Event.IDLE), "idle picks from the table in turn");
    }

    @Test
    void anEventOnItsCooldownDoesNothingAndAForcedOneAlwaysGoesThrough() {
        UndineMoodRules.State s = fresh();
        assertNotNull(UndineMoodRules.decide(s, UndineMoodRules.Event.OWNER_HURT, 1000, 0));
        assertNull(UndineMoodRules.decide(s, UndineMoodRules.Event.OWNER_HURT, 1000 + UndineMoodRules.Event.OWNER_HURT.cooldown() - 1, 0), "no second flinch within the cooldown");
        assertNotNull(UndineMoodRules.decide(s, UndineMoodRules.Event.OWNER_HURT, 1000 + UndineMoodRules.Event.OWNER_HURT.cooldown(), 0));
        for (UndineMoodRules.Event e : new UndineMoodRules.Event[]{UndineMoodRules.Event.LINK, UndineMoodRules.Event.UNLINK, UndineMoodRules.Event.OWNER_DIED}) {
            assertTrue(e.forced());
            for (int i = 0; i < 3; i++) assertNotNull(UndineMoodRules.decide(s, e, 2000 + i, 0), e + " is never held back");
        }
        for (UndineMoodRules.Event e : UndineMoodRules.Event.values()) assertTrue(e.cooldown() >= 0);
    }

    @Test
    void aHeldPoseIsNotInterruptedByALessImportantOneButTheLineIsStillSaid() {
        UndineMoodRules.State s = fresh();
        UndineMoodRules.Mood big = UndineMoodRules.decide(s, UndineMoodRules.Event.CAST_BIG, 1000, 0);
        assertEquals(UndinePose.POWER, big.pose());
        UndineMoodRules.Mood hurt = UndineMoodRules.decide(s, UndineMoodRules.Event.OWNER_HURT, 1005, 0);
        assertNotNull(hurt);
        assertNull(hurt.pose(), "the swell goes on");
        assertNull(hurt.face());
        assertEquals(UndineLines.Group.HURT, hurt.speech(), "but she still says her worried line");
        assertEquals(1000 + UndinePose.POWER.ticks(), s.poseUntil());
        UndineMoodRules.Mood after = UndineMoodRules.decide(s, UndineMoodRules.Event.OWNER_KILL, 1000 + UndinePose.POWER.ticks(), 0);
        assertEquals(UndinePose.WAVE, after.pose(), "once the pose is over the next event poses her");
        assertTrue(UndineMoodRules.expired(s, 1000 + UndinePose.POWER.ticks() + UndinePose.WAVE.ticks()));
        UndineMoodRules.cleared(s);
        assertFalse(UndineMoodRules.expired(s, 5000));
    }

    @Test
    void shePosesAreNeverChangedFasterThanTheMinimumGapUnlessTheNewOneIsMoreImportant() {
        Random rnd = new Random(42);
        UndineMoodRules.Event[] all = UndineMoodRules.Event.values();
        UndineMoodRules.State s = fresh();
        long lastAt = Long.MIN_VALUE / 4;
        int lastPriority = 0;
        int changes = 0;
        for (long now = 0; now < 200_000; now += 1 + rnd.nextInt(7)) {
            UndineMoodRules.Event e = all[rnd.nextInt(all.length)];
            UndineMoodRules.Mood m = UndineMoodRules.decide(s, e, now, rnd.nextInt());
            if (m == null || m.pose() == null) continue;
            changes++;
            if (!e.forced() && now - lastAt < UndineMoodRules.MIN_CHANGE_GAP)
                assertTrue(m.pose().priority() > lastPriority, "a pose change " + (now - lastAt) + " ticks after the last one must be more important (" + e + ")");
            lastAt = now;
            lastPriority = m.pose().priority();
        }
        assertTrue(changes > 50, "the simulation did change poses (" + changes + ")");
    }

    @Test
    void sheOnlyIdlesAfterALongQuietAndNeverOverAHeldPose() {
        UndineMoodRules.State s = fresh();
        assertNull(UndineMoodRules.decide(s, UndineMoodRules.Event.IDLE, UndineMoodRules.IDLE_AFTER - 1, 0), "not before the quiet is long enough");
        UndineMoodRules.Mood first = UndineMoodRules.decide(s, UndineMoodRules.Event.IDLE, UndineMoodRules.IDLE_AFTER, 0);
        assertNotNull(first);
        assertEquals(UndinePose.CLASP, first.pose());
        assertEquals(UndineLines.Group.IDLE, first.speech());
        assertNull(UndineMoodRules.decide(s, UndineMoodRules.Event.IDLE, UndineMoodRules.IDLE_AFTER + 1, 0), "not again while the pose is held");
        long next = UndineMoodRules.IDLE_AFTER + UndinePose.CLASP.ticks() + UndineMoodRules.IDLE_AFTER + UndineMoodRules.IDLE_JITTER;
        Set<UndinePose> seen = new HashSet<>();
        seen.add(first.pose());
        long t = next;
        for (int i = 0; i < 6; i++) {
            UndineMoodRules.Mood m = UndineMoodRules.decide(s, UndineMoodRules.Event.IDLE, t, 0);
            assertNotNull(m, "idle moment " + i);
            seen.add(m.pose());
            t += m.pose().ticks() + UndineMoodRules.IDLE_AFTER + UndineMoodRules.IDLE_JITTER;
        }
        assertTrue(seen.containsAll(List.of(UndinePose.CLASP, UndinePose.WAVE, UndinePose.WELCOME)), "she does all three of her idle things in turn: " + seen);
        // an event postpones the next idle moment
        UndineMoodRules.State s2 = fresh();
        UndineMoodRules.decide(s2, UndineMoodRules.Event.CAST_SMALL, 400, 0);
        assertNull(UndineMoodRules.decide(s2, UndineMoodRules.Event.IDLE, 500, 0));
    }

    @Test
    void lowHealthIsAThirdOfTheBarOrLessAndAboveZero() {
        assertTrue(UndineMoodRules.lowHealth(6f, 20f));
        assertTrue(UndineMoodRules.lowHealth(20f / 3f, 20f));
        assertFalse(UndineMoodRules.lowHealth(7f, 20f));
        assertFalse(UndineMoodRules.lowHealth(0f, 20f), "dead is not low");
        assertFalse(UndineMoodRules.lowHealth(5f, 0f));
    }

    @Test
    void theSpeechCounterGoesRoundTheRegisteredLines() {
        UndineMoodRules.State s = fresh();
        assertEquals(0, s.nextSpeech());
        assertEquals(1, s.nextSpeech());
        for (UndineLines.Group g : UndineLines.Group.values()) {
            List<String> l = UndineLines.of(g);
            if (l.isEmpty()) { assertNull(UndineLines.pick(g, 3), g + " has no recorded line: the caller falls back to the hum"); continue; }
            Set<String> got = new HashSet<>();
            for (int i = 0; i < l.size(); i++) got.add(UndineLines.pick(g, i));
            assertEquals(new HashSet<>(l), got, "every registered line of " + g + " comes up before one repeats");
            assertEquals(UndineLines.pick(g, 0), UndineLines.pick(g, l.size()));
            assertNotNull(UndineLines.pick(g, -7), "a negative counter still picks");
        }
    }

    // ---------------------------------------------------------------------------------------------------------- the data
    @Test
    void thePosesAreWellFormed() {
        Set<Integer> ids = new HashSet<>();
        Set<String> clips = new HashSet<>();
        for (UndinePose p : UndinePose.values()) {
            assertTrue(ids.add(p.id()), "id " + p.id() + " is used twice");
            assertEquals(p, UndinePose.of(p.id()));
            if (p == UndinePose.NONE) { assertEquals(List.of("idle", "full_idle"), p.clips()); assertEquals(0, p.ticks()); continue; }
            assertTrue(p.ticks() > 0 && p.priority() > 0);
            assertFalse(p.clip(false).isEmpty());
            for (String c : p.clips()) assertTrue(clips.add(c), "clip " + c + " belongs to one pose only");
            assertEquals(p.clips().size() == 2, !p.clip(false).equals(p.clip(true)), p + ": a '_f' clip exists exactly when the form matters");
        }
        assertEquals(UndinePose.maxId() + 1, ids.size(), "ids 0..max, no hole");
        assertEquals(UndinePose.NONE, UndinePose.of(99));
        assertEquals(UndinePose.NONE, UndinePose.of(-1));
        assertTrue(UndinePose.FAREWELL.priority() > UndinePose.POWER.priority() && UndinePose.POWER.priority() > UndinePose.FLINCH.priority(), "a farewell is never cut short");
    }

    @Test
    void theFacesAreWellFormed() {
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (UndineFace f : UndineFace.values()) {
            assertTrue(ids.add(f.id()));
            assertTrue(keys.add(f.key()));
            assertEquals(f, UndineFace.of(f.id()));
            assertEquals(f != UndineFace.CALM, f.hasOverlay());
        }
        assertEquals(UndineFace.maxId() + 1, ids.size());
        assertEquals(UndineFace.CALM, UndineFace.of(77));
        assertTrue(keys.containsAll(List.of("calm", "smile", "sad", "stern", "angry", "surprised", "serene")), "the seven faces of the owner's list");
    }

    @Test
    void theLineCatalogueHasFortyLinesInNineGroups() {
        assertEquals(9, UndineLines.Group.values().length);
        assertEquals(40, UndineLines.manifestIds().size());
        assertEquals(new HashSet<>(UndineLines.manifestIds()).size(), UndineLines.manifestIds().size());
        for (String id : UndineLines.manifestIds()) {
            assertTrue(id.matches("undine_[a-z]+_[1-9]"), id);
            assertEquals(id, UndineLines.groupOf(id).id(Integer.parseInt(id.substring(id.lastIndexOf('_') + 1))));
        }
        assertTrue(UndineLines.manifestIds().containsAll(UndineLines.ids()), "every registered line is in the manifest");
        assertNull(UndineLines.groupOf("system_floor_enter"));
        assertNull(UndineLines.groupOf(null));
        assertEquals(0, UndineLines.Group.DIED.cooldown(), "her last words are never held back");
        for (UndineLines.Group g : UndineLines.Group.values()) assertTrue(g.count() >= 2 && g.cooldown() >= 0);
    }

    @Test
    void theSpeechBudgetKeepsOneLinePerListenerEveryFourSecondsAndACooldownPerOwnerAndGroup() {
        UndineSpeechBudget b = new UndineSpeechBudget();
        UUID a = UUID.nameUUIDFromBytes("a".getBytes()), c = UUID.nameUUIDFromBytes("c".getBytes());
        assertEquals(80, UndineSpeechBudget.LISTENER_GAP, "4 seconds");
        assertEquals(32.0, UndineSpeechBudget.RADIUS);
        assertTrue(b.listenerMay(a, 100));
        assertFalse(b.listenerMay(a, 179));
        assertTrue(b.listenerCould(a, 180));
        assertTrue(b.listenerMay(a, 180));
        assertTrue(b.listenerMay(c, 100), "another listener has its own gap");
        assertTrue(b.ownerMay(a, UndineLines.Group.CAST, 1000));
        assertFalse(b.ownerMay(a, UndineLines.Group.CAST, 1000 + UndineLines.Group.CAST.cooldown() - 1));
        assertTrue(b.ownerMay(a, UndineLines.Group.HURT, 1001), "another group is its own cooldown");
        assertTrue(b.ownerMay(c, UndineLines.Group.CAST, 1001), "another owner too");
        assertTrue(b.ownerMay(a, UndineLines.Group.DIED, 1002) && b.ownerMay(a, UndineLines.Group.DIED, 1002), "no cooldown for her last words");
        assertFalse(b.ownerMay(null, UndineLines.Group.CAST, 0));
        assertFalse(b.listenerMay(null, 0));
        b.forget(a);
        assertTrue(b.ownerMay(a, UndineLines.Group.CAST, 1002));
        assertTrue(b.listenerMay(a, 181));
        b.prune(1_000_000);
        assertEquals(0, b.tracked());
        b.clear();
    }
}
