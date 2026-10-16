package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The hard hit cap and the alternating, rate-limited reaction. */
class HitGateTest {
    @Test
    void aBigHitIsCutToTwoPercentOfTheMaximum() {
        HitGate.Result r = HitGate.clamp(1000f, 4800f, 2.0);
        assertTrue(r.clamped());
        assertEquals(96f, r.amount(), 1e-3f);
    }

    @Test
    void aSmallHitPassesUntouched() {
        HitGate.Result r = HitGate.clamp(50f, 4800f, 2.0);
        assertFalse(r.clamped());
        assertEquals(50f, r.amount());
        HitGate.Result exact = HitGate.clamp(96f, 4800f, 2.0);
        assertFalse(exact.clamped(), "a hit exactly at the cap is not clamped");
    }

    @Test
    void noPositiveHitAndABrokenSetupPassUnchanged() {
        assertFalse(HitGate.clamp(0f, 4800f, 2.0).clamped());
        assertFalse(HitGate.clamp(-3f, 4800f, 2.0).clamped());
        assertFalse(HitGate.clamp(10f, 4800f, 0.0).clamped(), "a pct of 0 switches the gate off");
        assertFalse(HitGate.clamp(10f, 0f, 2.0).clamped());
        assertEquals(10f, HitGate.clamp(10f, 0f, 2.0).amount());
    }

    @Test
    void theCapScalesWithThePercent() {
        assertEquals(240f, HitGate.cap(4800f, 5.0), 1e-3f);
        assertEquals(0f, HitGate.cap(4800f, 0.0));
        assertEquals(0f, HitGate.cap(-1f, 2.0));
    }

    @Test
    void reactionsAlternateAndRespectTheCooldown() {
        HitGate g = new HitGate();
        assertEquals(HitGate.Reaction.TELEPORT, g.react(100, false));
        assertNull(g.react(120, false), "on cooldown");
        assertNull(g.react(159, false));
        assertEquals(HitGate.Reaction.COUNTER, g.react(160, false));
        assertEquals(HitGate.Reaction.TELEPORT, g.react(220, false));
    }

    @Test
    void aBusyMomentGivesNoReactionAndKeepsTheAlternation() {
        HitGate g = new HitGate();
        assertNull(g.react(10, true), "channelling or transforming: nothing");
        assertEquals(HitGate.Reaction.TELEPORT, g.react(11, false), "the busy call did not eat the first teleport");
        assertNull(g.react(12, true));
        assertEquals(HitGate.Reaction.COUNTER, g.react(200, false));
    }

    @Test
    void saveAndRestoreKeepTheCooldownAndTheTurn() {
        HitGate g = new HitGate();
        g.react(100, false);
        HitGate back = new HitGate();
        back.restore(g.save());
        assertNull(back.react(110, false));
        assertEquals(HitGate.Reaction.COUNTER, back.react(160, false));
        HitGate bad = new HitGate();
        bad.restore(null);
        assertEquals(HitGate.Reaction.TELEPORT, bad.react(0, false));
    }
}
