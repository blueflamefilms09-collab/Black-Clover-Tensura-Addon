package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Decision;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Director;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Event;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Face;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Pose;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Which pose, face and voice group the spirit chooses per situation, and that the cooldowns keep it from being a nuisance. */
class DarkMoodRulesTest {
    @Test
    void everySituationHasARowWithAPoseAFaceAndAVoiceGroupThatIsPlanned() {
        for (Event e : Event.values()) {
            DarkMoodRules.Row r = DarkMoodRules.row(e);
            assertNotNull(r, e.name());
            assertNotNull(r.pose(), e.name());
            assertNotNull(r.face(), e.name());
            assertTrue(SpiritDarkVoiceCatalog.PLANNED.containsKey(r.group()), e + " uses the group " + r.group());
            assertTrue(r.chance() >= 0 && r.chance() <= 100 && r.gap() >= 0 && r.voiceGap() >= 0, e.name());
        }
        Set<String> groups = new HashSet<>();
        for (Event e : Event.values()) groups.add(DarkMoodRules.row(e).group());
        assertEquals(SpiritDarkVoiceCatalog.PLANNED.keySet(), groups, "every planned group is asked for by some situation, and no other");
    }

    @Test
    void theSituationsTheOwnerNamedGiveTheirPosesAndFaces() {
        assertEquals(Pose.BOW, DarkMoodRules.row(Event.LINK).pose());
        assertEquals(Pose.FAREWELL, DarkMoodRules.row(Event.UNLINK).pose());
        assertEquals(Pose.CAST, DarkMoodRules.row(Event.CAST).pose());
        assertEquals(Pose.APPEAR, DarkMoodRules.row(Event.STEP).pose());
        assertEquals(Pose.RECOIL, DarkMoodRules.row(Event.DAMAGED).pose());
        assertEquals(Pose.FEAST, DarkMoodRules.row(Event.STRIKE).pose());
        assertEquals(Pose.CACKLE, DarkMoodRules.row(Event.KILL).pose());
        assertEquals(Face.SURPRISED, DarkMoodRules.row(Event.OWNER_DIED).face());
        assertEquals(Face.ANGRY, DarkMoodRules.row(Event.LOW_HEALTH).face());
        assertEquals(Pose.BATTLE, DarkMoodRules.rest(true), "the powered form rests in its battle stance");
        assertEquals(Pose.LOUNGE, DarkMoodRules.rest(false), "the companion lounges");
        assertEquals(7, Face.values().length);
        assertTrue(Pose.values().length <= 16 && Face.values().length <= 8, "they fit the prop word's bits");
    }

    @Test
    void anEventIsAnsweredOnceThenCoolsDown() {
        Director d = new Director();
        Decision first = d.decide(Event.DAMAGED, false, 1000, 0, 1f);
        assertNotNull(first);
        assertEquals(Pose.RECOIL, first.pose());
        assertEquals(DarkMoodRules.G_HURT, first.voiceGroup(), "roll 0 is under the chance: it speaks");
        assertNull(d.decide(Event.DAMAGED, false, 1000 + DarkMoodRules.row(Event.DAMAGED).gap() - 1, 0, 1f), "still cooling down");
        assertNotNull(d.decide(Event.DAMAGED, false, 1000 + DarkMoodRules.row(Event.DAMAGED).gap(), 99, 1f));
    }

    @Test
    void theVoiceHasItsOwnChanceAndGaps() {
        Director d = new Director();
        Decision quiet = d.decide(Event.CAST, false, 5000, 99, 1f);
        assertNotNull(quiet);
        assertNull(quiet.voiceGroup(), "a roll over the chance stays silent but still strikes the pose");
        Director e = new Director();
        assertNotNull(e.decide(Event.KILL, false, 100, 0, 1f).voiceGroup());
        Decision next = e.decide(Event.CAST, false, 100 + DarkMoodRules.VOICE_GAP_ANY - 1, 0, 1f);
        assertNotNull(next);
        assertNull(next.voiceGroup(), "two lines of one owner are never closer than " + DarkMoodRules.VOICE_GAP_ANY + " ticks");
        Director f = new Director();
        assertNotNull(f.decide(Event.CAST, false, 0, 0, 1f).voiceGroup());
        assertNull(f.decide(Event.CAST, false, DarkMoodRules.row(Event.CAST).gap(), 0, 1f).voiceGroup(), "the same event's voice gap (" + DarkMoodRules.row(Event.CAST).voiceGap() + ") holds");
        assertNotNull(f.decide(Event.CAST, false, DarkMoodRules.row(Event.CAST).voiceGap() + 1, 0, 1f).voiceGroup());
    }

    @Test
    void aStrongerPoseIsNotInterruptedByAWeakerOne() {
        Director d = new Director();
        assertEquals(Pose.RECOIL, d.decide(Event.DAMAGED, false, 100, 99, 1f).pose());
        Decision cast = d.decide(Event.CAST, false, 105, 99, 1f);
        assertNotNull(cast);
        assertNull(cast.pose(), "a cast (priority 2) does not cut a recoil (priority 3) short, but the face still changes");
        assertEquals(Face.GRIN, cast.face());
        Decision after = d.decide(Event.CAST, false, 100 + Pose.RECOIL.ticks + 40, 99, 1f);
        assertEquals(Pose.CAST, after.pose(), "once the recoil has played, the cast pose comes");
        Director f = new Director();
        f.decide(Event.IDLE, false, 0, 99, 1f);
        assertEquals(Pose.FAREWELL, f.decide(Event.UNLINK, false, 900, 99, 1f).pose(), "farewell outranks everything");
    }

    @Test
    void lowHealthOnlyAnswersWhenTheOwnerIsReallyLow() {
        Director d = new Director();
        assertNull(d.decide(Event.LOW_HEALTH, false, 100, 0, 0.9f));
        assertNull(d.decide(Event.LOW_HEALTH, false, 101, 0, DarkMoodRules.LOW_HEALTH_FRACTION + 0.01f));
        Decision low = d.decide(Event.LOW_HEALTH, false, 102, 0, 0.2f);
        assertNotNull(low);
        assertEquals(DarkMoodRules.G_LOW, low.voiceGroup());
        assertNull(d.decide(Event.LOW_HEALTH, false, 200, 0, 0.1f), "a long gap before it nags again");
    }

    @Test
    void theIdleMoodsRotateAndThePoweredFormKeepsItsStance() {
        Director d = new Director();
        Set<Pose> seen = new HashSet<>();
        Set<Face> faces = new HashSet<>();
        long now = 0;
        for (int i = 0; i < 8; i++) {
            now += DarkMoodRules.row(Event.IDLE).gap() + 1;
            Decision x = d.decide(Event.IDLE, false, now, 99, 1f);
            assertNotNull(x);
            if (x.pose() != null) seen.add(x.pose());
            faces.add(x.face());
        }
        assertTrue(seen.size() >= 2 && faces.size() >= 3, "the idle moods vary: " + seen + " " + faces);
        Decision p = new Director().decide(Event.IDLE, true, 5000, 99, 1f);
        assertNull(p.pose(), "the powered form does not lounge or bow idly");
        assertEquals(Face.SINISTER, p.face());
        Director e = new Director();
        assertFalse(e.idleDue(10, 100) && e.idleDue(11, 100), "idle moods are spaced");
        assertTrue(new Director().idleDue(0, 0));
    }

    @Test
    void aClientReturnsToRestWhenAPoseHasRunItsLengthAndTheFaceFades() {
        int word = SpiritDarkRules.param(SpiritDarkRules.V_COMPANION, false, Pose.BOW, Face.GRIN, 1000);
        assertEquals(Pose.BOW, DarkMoodRules.activePose(word, 1000 + Pose.BOW.ticks - 1, false));
        assertEquals(Pose.LOUNGE, DarkMoodRules.activePose(word, 1000 + Pose.BOW.ticks, false));
        assertEquals(Pose.BATTLE, DarkMoodRules.activePose(word, 1000 + Pose.BOW.ticks, true), "the powered form returns to its battle stance");
        assertEquals(Face.GRIN, DarkMoodRules.activeFace(word, 1000 + DarkMoodRules.FACE_HOLD - 1, false));
        assertEquals(DarkMoodRules.baseFace(false), DarkMoodRules.activeFace(word, 1000 + DarkMoodRules.FACE_HOLD, false));
        int plain = SpiritDarkRules.plain(SpiritDarkRules.V_POWERED, false);
        assertEquals(Pose.BATTLE, DarkMoodRules.activePose(plain, 12345, true));
        assertEquals(DarkMoodRules.baseFace(true), DarkMoodRules.activeFace(plain, 12345, true));
        assertEquals(0.5f, DarkMoodRules.clipSeconds(word, 1010, 0f, 99f, false), 1e-5f, "a one-shot counts from its start");
        assertEquals(99f, DarkMoodRules.clipSeconds(SpiritDarkRules.param(0, false, Pose.SMUG, Face.SMUG, 1000), 1010, 0f, 99f, false), "a held pose uses the clock");
        for (Pose p : Pose.values()) assertTrue(p.ticks >= 0 && p.clip != null && !p.clip.isBlank());
    }

    @Test
    void theDirectorStaysSmallForAManyPlayerServer() {
        Director d = new Director();
        for (int i = 0; i < 100_000; i++) d.decide(Event.values()[i % Event.values().length], i % 2 == 0, i * 3L, i % 100, 0.5f);
        assertNotNull(d, "no growing state: arrays of fixed size only");
    }
}
