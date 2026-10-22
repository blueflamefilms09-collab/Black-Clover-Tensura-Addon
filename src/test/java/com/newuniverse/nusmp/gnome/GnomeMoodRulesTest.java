package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.audio.voice.VoiceCatalog;
import com.newuniverse.nusmp.gnome.GnomeMoodRules.Situation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The moods of the Gnome: the table (situation to pose, face, voice group, cue), the thresholds, the cooldowns, the stillness, the line pools and the per-listener limit on the spoken lines. */
class GnomeMoodRulesTest {
    @Test
    void everySituationHasAValidPoseFaceCueAndLineGroup() {
        for (Situation s : Situation.values()) {
            GnomeMoodRules.Mood m = GnomeMoodRules.of(s);
            assertTrue(m.pose() >= GnomeRules.CLIP_IDLE && m.pose() <= GnomeRules.CLIP_KNEEL, s + " pose");
            assertTrue(m.face() >= 0 && m.face() < GnomeRules.FACES, s + " face");
            assertFalse(GnomeMoodRules.planned(m.lineGroup()).isEmpty(), s + " names a line group that exists: " + m.lineGroup());
            if (m.cue() != null) assertNotNull(GnomeSoundRules.cue(m.cue()), s + " cue " + m.cue());
            assertTrue(m.lineChance() >= 0 && m.lineChance() <= 100 && m.cueChance() >= 0 && m.cueChance() <= 100, s + " chances are percents");
            assertTrue(m.cooldown() >= 0, s + " cooldown");
        }
        assertEquals(13, Situation.values().length);
    }

    @Test
    void theTableSaysWhatTheOwnerAsked() {
        assertEquals(GnomeRules.CLIP_KNEEL, GnomeMoodRules.of(Situation.LINK).pose(), "it kneels and rises when linked");
        assertEquals(GnomeRules.CLIP_SINK, GnomeMoodRules.of(Situation.UNLINK).pose());
        assertEquals(GnomeRules.CLIP_PUNCH, GnomeMoodRules.of(Situation.CAST_SMALL).pose(), "a starter page is a punch");
        assertEquals(GnomeRules.CLIP_SLAM, GnomeMoodRules.of(Situation.CAST_MID).pose(), "a mid page is a slam");
        assertEquals(GnomeRules.CLIP_CAST, GnomeMoodRules.of(Situation.CAST_BIG).pose(), "a big page is both fists");
        assertEquals(GnomeRules.FACE_ANGRY, GnomeMoodRules.of(Situation.CAST_BIG).face());
        assertEquals(GnomeRules.CLIP_SHRUG, GnomeMoodRules.of(Situation.HURT).pose(), "a hit on the master is shrugged off");
        assertEquals(GnomeRules.FACE_SURPRISED, GnomeMoodRules.of(Situation.SURPRISE).face());
        assertEquals(GnomeRules.CLIP_GUARD, GnomeMoodRules.of(Situation.LOW_HEALTH).pose(), "arms crossed when the master is nearly dead");
        assertEquals("lowhp", GnomeMoodRules.of(Situation.LOW_HEALTH).lineGroup());
        assertEquals(GnomeRules.CLIP_POUND, GnomeMoodRules.of(Situation.KILL).pose(), "a kill: the chest-pound");
        assertEquals(GnomeRules.FACE_AMUSED, GnomeMoodRules.of(Situation.KILL).face());
        assertEquals(GnomeRules.CLIP_DOZE, GnomeMoodRules.of(Situation.SLEEP).pose());
        assertEquals(GnomeRules.FACE_SLEEPY, GnomeMoodRules.of(Situation.SLEEP).face());
        assertEquals("snore", GnomeMoodRules.of(Situation.SLEEP).cue());
        assertEquals(GnomeRules.CLIP_STOMP, GnomeMoodRules.of(Situation.WAKE).pose());
        assertEquals("death", GnomeMoodRules.of(Situation.OWNER_DEATH).lineGroup());
        assertEquals(0, GnomeMoodRules.of(Situation.OWNER_DEATH).cooldown(), "the mourning is never held back");
    }

    @Test
    void theCostTierPicksThePunchTheSlamOrTheBigPage() {
        assertEquals(Situation.CAST_SMALL, GnomeMoodRules.fromTier(5));
        assertEquals(Situation.CAST_SMALL, GnomeMoodRules.fromTier(11.9));
        assertEquals(Situation.CAST_MID, GnomeMoodRules.fromTier(12));
        assertEquals(Situation.CAST_MID, GnomeMoodRules.fromTier(19.9));
        assertEquals(Situation.CAST_BIG, GnomeMoodRules.fromTier(GnomeRules.FULL_COST_PERCENT));
        assertEquals(Situation.CAST_BIG, GnomeMoodRules.fromTier(60));
        assertTrue(GnomeMoodRules.MID_COST_PERCENT < GnomeRules.FULL_COST_PERCENT);
    }

    @Test
    void aHitOnTheMasterIsAScratchAHurtAShockOrADanger() {
        assertNull(GnomeMoodRules.fromDamage(0.5f, 19.5f, 20f), "a scratch is ignored");
        assertNull(GnomeMoodRules.fromDamage(0f, 20f, 20f));
        assertNull(GnomeMoodRules.fromDamage(5f, 15f, 0f), "no maximum health, no decision");
        assertEquals(Situation.HURT, GnomeMoodRules.fromDamage(3f, 17f, 20f));
        assertEquals(Situation.SURPRISE, GnomeMoodRules.fromDamage(8f, 12f, 20f), "a hit of 8 or more is a shock");
        assertEquals(Situation.LOW_HEALTH, GnomeMoodRules.fromDamage(3f, 6f, 20f), "at 30 percent or less after the hit: danger");
        assertEquals(Situation.LOW_HEALTH, GnomeMoodRules.fromDamage(9f, 2f, 20f), "danger wins over a shock");
        assertEquals(Situation.HURT, GnomeMoodRules.fromDamage(3f, 6.1f, 20f), "just above 30 percent");
    }

    @Test
    void theGnomeDozesAfterThirtySecondsStillAndMakesOneRemarkBefore() {
        assertFalse(GnomeMoodRules.dozeDue(599));
        assertTrue(GnomeMoodRules.dozeDue(600));
        assertFalse(GnomeMoodRules.idleRemarkDue(399, 5000));
        assertTrue(GnomeMoodRules.idleRemarkDue(400, 5000), "20 seconds still and quiet: one remark");
        assertFalse(GnomeMoodRules.idleRemarkDue(400, 100), "not right after another reaction");
        assertFalse(GnomeMoodRules.idleRemarkDue(700, 5000), "the doze takes over");
    }

    @Test
    void eachSituationHasItsOwnCooldownPerOwner() {
        GnomeMoodRules.Cooldowns c = new GnomeMoodRules.Cooldowns();
        assertTrue(c.ready(Situation.HURT, 0), "nothing happened yet");
        assertTrue(c.sinceAny(0) > 1_000_000, "no reaction yet");
        c.mark(Situation.HURT, 1000);
        assertFalse(c.ready(Situation.HURT, 1000 + GnomeMoodRules.of(Situation.HURT).cooldown() - 1));
        assertTrue(c.ready(Situation.HURT, 1000 + GnomeMoodRules.of(Situation.HURT).cooldown()));
        assertTrue(c.ready(Situation.KILL, 1001), "another situation is not held back by it");
        assertEquals(50, c.sinceAny(1050));
        c.mark(Situation.OWNER_DEATH, 2000);
        assertTrue(c.ready(Situation.OWNER_DEATH, 2000), "a zero cooldown is always ready");
        GnomeMoodRules.Cooldowns other = new GnomeMoodRules.Cooldowns();
        assertTrue(other.ready(Situation.HURT, 1001), "another owner has its own");
    }

    @Test
    void stillnessCountsTicksWithoutAStepAndLooksAroundIsStill() {
        GnomeMoodRules.Stillness s = new GnomeMoodRules.Stillness();
        assertEquals(0, s.stillTicks(500), "no position yet");
        assertFalse(s.update(1000, 10, 64, 10));
        assertEquals(0, s.stillTicks(1000));
        assertFalse(s.update(1100, 10.05, 64, 10.05), "a tiny shift is still standing");
        assertEquals(100, s.stillTicks(1100));
        assertTrue(s.update(1200, 12, 64, 10), "a step is a move");
        assertEquals(0, s.stillTicks(1200));
        assertEquals(40, s.stillTicks(1240));
    }

    @Test
    void theLinePoolsAreTheCataloguesAndNeverRepeatTheLastLine() {
        Set<String> all = new HashSet<>();
        for (String g : GnomeMoodRules.groups()) {
            assertFalse(GnomeMoodRules.planned(g).isEmpty(), g);
            for (String suffix : GnomeMoodRules.planned(g)) all.add(suffix);
            for (String id : GnomeMoodRules.lines(g)) {
                assertTrue(VoiceCatalog.has(id), g + ": " + id + " is in the voice catalogue");
                assertTrue(id.startsWith("gnome_"));
            }
        }
        for (String suffix : VoiceCatalog.GNOME_LINES) assertTrue(all.contains(suffix), "the catalogue line " + suffix + " belongs to a group");
        assertTrue(GnomeMoodRules.planned("greet").size() >= 2 && GnomeMoodRules.planned("cast").size() >= 3, "a few variants where the line comes often");
        assertEquals(21, all.size(), "twenty-one lines are planned (the owner asked for 15 to 25)");
        assertNull(GnomeMoodRules.pick("nosuchgroup", 3, null));
        for (String g : GnomeMoodRules.groups()) {
            String first = GnomeMoodRules.pick(g, 0, null);
            if (GnomeMoodRules.lines(g).size() > 1) {
                for (int roll = -5; roll < 20; roll++) assertFalse(first.equals(GnomeMoodRules.pick(g, roll, first)), g + ": the same line twice in a row");
            } else if (GnomeMoodRules.lines(g).size() == 1) {
                assertEquals(first, GnomeMoodRules.pick(g, 7, first), g + ": the only line may repeat");
            } else {
                assertNull(first, g + ": a group with no recording says nothing");
            }
        }
    }

    @Test
    void theChancesAreDeterministicPercents() {
        GnomeMoodRules.Mood m = GnomeMoodRules.of(Situation.HURT);
        int says = 0, cues = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (GnomeMoodRules.says(m, roll)) says++;
            if (GnomeMoodRules.makesCue(m, roll)) cues++;
        }
        assertEquals(m.lineChance(), says);
        assertEquals(m.cueChance(), cues);
        assertTrue(GnomeMoodRules.says(m, -100 + 0) == GnomeMoodRules.says(m, 0), "negative rolls wrap like the others");
        assertFalse(GnomeMoodRules.makesCue(GnomeMoodRules.of(Situation.LINK), 0), "a mood without a cue never makes one");
        assertTrue(GnomeMoodRules.says(GnomeMoodRules.of(Situation.LINK), 99), "the greeting is always said");
    }

    @Test
    void aListenerHearsAtMostOneLineEveryFourSecondsWhoeverSpeaks() {
        GnomeMoodRules.LineGate g = new GnomeMoodRules.LineGate();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(g.allow(a, 1000));
        assertFalse(g.allow(a, 1000 + GnomeMoodRules.LINE_GAP_TICKS - 1), "four seconds is 80 ticks");
        assertTrue(g.allow(b, 1001), "another listener has its own");
        assertTrue(g.allow(a, 1000 + GnomeMoodRules.LINE_GAP_TICKS));
        assertEquals(2, g.tracked());
        g.forget(a);
        assertEquals(1, g.tracked(), "a logout drops the listener");
        g.clear();
        assertEquals(0, g.tracked());
        assertEquals(32.0, GnomeMoodRules.LINE_RADIUS, "lines carry 32 blocks");
        assertEquals(80, GnomeMoodRules.LINE_GAP_TICKS);
    }
}
