package com.newuniverse.nusmp.vfx.deck;

import com.newuniverse.nusmp.vfx.deck.DeckSummonTimeline.Phase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the card summon of the Deck of Many Things is a client animation nobody can watch in a test, so its clock is pinned: the phases come in order with positive lengths, the whole thing
 * lasts about two seconds, the progress of every phase and the travel, flip and dissolve never go back, the card ends face up and gone, the column is only there between its phase's ends.
 */
class DeckSummonTimelineTest {
    @Test
    void phasesRunInOrderWithPositiveLengthsInsideTheTotal() {
        Phase[] p = Phase.values();
        assertEquals(6, p.length);
        for (int i = 0; i < p.length; i++) {
            assertTrue(DeckSummonTimeline.length(p[i]) > 0, p[i] + " has a length");
            assertTrue(DeckSummonTimeline.end(p[i]) <= DeckSummonTimeline.TOTAL, p[i] + " ends inside the total");
            if (i > 0) {
                assertTrue(DeckSummonTimeline.start(p[i]) >= DeckSummonTimeline.start(p[i - 1]), p[i] + " starts after " + p[i - 1]);
                assertTrue(DeckSummonTimeline.end(p[i]) >= DeckSummonTimeline.end(p[i - 1]), p[i] + " ends after " + p[i - 1]);
            }
        }
        assertEquals(0, DeckSummonTimeline.start(Phase.SLIDE));
        assertEquals(DeckSummonTimeline.TOTAL, DeckSummonTimeline.end(Phase.DISSOLVE));
    }

    @Test
    void itLastsAboutTwoSeconds() {
        assertTrue(DeckSummonTimeline.TOTAL >= 30 && DeckSummonTimeline.TOTAL <= 50, "20 ticks are a second: " + DeckSummonTimeline.TOTAL);
    }

    @Test
    void progressIsMonotonicAndBounded() {
        for (Phase ph : Phase.values()) {
            float last = -1f;
            for (int k = -8; k <= DeckSummonTimeline.TOTAL * 8 + 8; k++) {
                float t = k / 8f, v = DeckSummonTimeline.progress(ph, t);
                assertTrue(v >= 0f && v <= 1f, ph + " progress in 0..1");
                assertTrue(v >= last, ph + " never goes back at t=" + t);
                last = v;
            }
            assertEquals(0f, DeckSummonTimeline.progress(ph, DeckSummonTimeline.start(ph)), 1e-6);
            assertEquals(1f, DeckSummonTimeline.progress(ph, DeckSummonTimeline.end(ph)), 1e-6);
        }
    }

    @Test
    void theCardTravelsFlipsAndDissolvesMonotonically() {
        float travel = -1f, flip = -1f, alpha = 2f;
        for (int k = 0; k <= DeckSummonTimeline.TOTAL * 8; k++) {
            float t = k / 8f;
            float tr = DeckSummonTimeline.travel(t), fl = DeckSummonTimeline.flipDegrees(t), al = DeckSummonTimeline.cardAlpha(t);
            assertTrue(tr >= travel, "travel never goes back at " + t);
            assertTrue(fl >= flip, "the flip never goes back at " + t);
            assertTrue(al <= alpha, "the card only fades at " + t);
            travel = tr;
            flip = fl;
            alpha = al;
        }
        assertEquals(2f, DeckSummonTimeline.travel(DeckSummonTimeline.TOTAL), 1e-5, "it ends on the zone");
        assertEquals(180f, DeckSummonTimeline.flipDegrees(DeckSummonTimeline.TOTAL), 1e-4, "it ends face up");
        assertTrue(DeckSummonTimeline.faceUp(DeckSummonTimeline.TOTAL));
        assertFalse(DeckSummonTimeline.faceUp(0f), "it starts back up");
        assertEquals(1f, DeckSummonTimeline.cardAlpha(0f), 1e-6);
        assertEquals(0f, DeckSummonTimeline.cardAlpha(DeckSummonTimeline.TOTAL), 1e-6, "and is gone");
    }

    @Test
    void theFlipHappensAfterTheSlapAndTheColumnAfterTheFlipStarts() {
        assertTrue(DeckSummonTimeline.start(Phase.FLIP) >= DeckSummonTimeline.end(Phase.SLAP));
        assertTrue(DeckSummonTimeline.start(Phase.COLUMN) >= DeckSummonTimeline.start(Phase.FLIP));
        assertTrue(DeckSummonTimeline.start(Phase.DISSOLVE) >= DeckSummonTimeline.start(Phase.COLUMN));
        assertEquals(0f, DeckSummonTimeline.shockwaveRadius(DeckSummonTimeline.end(Phase.SLAP)), 1e-6, "no ring before the impact");
        assertTrue(DeckSummonTimeline.shockwaveRadius(DeckSummonTimeline.end(Phase.SLAP) + 3) > 0f);
        assertEquals(0f, DeckSummonTimeline.shockwaveAlpha(DeckSummonTimeline.end(Phase.SLAP) + 7), 1e-6, "and none long after");
    }

    @Test
    void theColumnAndTheFlashExistOnlyInsideTheirPhases() {
        assertEquals(0f, DeckSummonTimeline.columnAlpha(DeckSummonTimeline.start(Phase.COLUMN)), 1e-6);
        assertEquals(0f, DeckSummonTimeline.columnAlpha(DeckSummonTimeline.end(Phase.COLUMN)), 1e-6);
        assertTrue(DeckSummonTimeline.columnAlpha((DeckSummonTimeline.start(Phase.COLUMN) + DeckSummonTimeline.end(Phase.COLUMN)) / 2f) > 0.9f);
        assertEquals(0f, DeckSummonTimeline.columnHeight(DeckSummonTimeline.start(Phase.COLUMN)), 1e-6);
        assertEquals(0f, DeckSummonTimeline.flash(DeckSummonTimeline.start(Phase.FLASH)), 1e-6);
        assertEquals(0f, DeckSummonTimeline.flash(DeckSummonTimeline.end(Phase.FLASH)), 1e-6);
        assertTrue(DeckSummonTimeline.flash((DeckSummonTimeline.start(Phase.FLASH) + DeckSummonTimeline.end(Phase.FLASH)) / 2f) > 0.9f);
        assertTrue(DeckSummonTimeline.finished(DeckSummonTimeline.TOTAL));
        assertFalse(DeckSummonTimeline.finished(DeckSummonTimeline.TOTAL - 1));
    }

    @Test
    void sparksAreBoundedAndOnlyAtImpactAndDissolve() {
        int total = DeckSummonTimeline.maxSparks();
        assertTrue(total > 0 && total <= 64, "a bounded number of sparks: " + total);
        assertEquals(0, DeckSummonTimeline.sparks(0));
        assertEquals(0, DeckSummonTimeline.sparks(DeckSummonTimeline.TOTAL));
        assertTrue(DeckSummonTimeline.sparks(DeckSummonTimeline.end(Phase.SLAP)) > 0);
        assertTrue(DeckSummonTimeline.sparks(DeckSummonTimeline.start(Phase.DISSOLVE) + 1) > 0);
    }

    @Test
    void geometryPlacesTheZonesOnTheFanAndTheWorldPointsOnTheLeft() {
        for (int i = 0; i < DeckSummonGeometry.ZONES; i++) {
            float[] z = DeckSummonGeometry.zone(i);
            double d = Math.hypot(z[1] - DeckSummonGeometry.HUB_YD, z[2] - DeckSummonGeometry.HUB_Z);
            assertEquals(DeckSummonGeometry.ZONE_R, d, 1e-3, "zone " + i + " lies on the blade circle");
            assertTrue(z[0] > 3.25f, "zone " + i + " is outside the sleeve layer of the arm");
        }
        // facing +z (yaw 0) the player's left is +x: the slot is left of the body and below the shoulder
        double[] w = DeckSummonGeometry.world(DeckSummonGeometry.slot(), 0f);
        assertTrue(w[0] > 0.5 && w[0] < 1.0, "left of the body: " + w[0]);
        assertTrue(w[1] > 0.5 && w[1] < 1.4, "at forearm height: " + w[1]);
        double[] turned = DeckSummonGeometry.world(DeckSummonGeometry.slot(), 180f);
        assertEquals(-w[0], turned[0], 1e-6, "turned round it is on the other side of the feet");
        for (int mood = 0; mood < 3; mood++) {
            assertTrue(DeckSummonGeometry.alignmentColour(mood) != DeckSummonGeometry.alignmentRim(mood));
        }
        assertTrue(DeckSummonGeometry.alignmentColour(0) != DeckSummonGeometry.alignmentColour(2), "good and bad differ");
    }

    @Test
    void theSeedKeepsTheCardTheZoneAndTheAlignment() {
        for (int card = 0; card < 22; card++) {
            for (int zone = 0; zone < DeckSummonGeometry.ZONES; zone++) {
                for (int mood = 0; mood < 3; mood++) {
                    long s = DeckSummonGeometry.pack(card, zone, mood);
                    assertEquals(card, DeckSummonGeometry.cardOf(s));
                    assertEquals(zone, DeckSummonGeometry.zoneOf(s));
                    assertEquals(mood, DeckSummonGeometry.moodOf(s));
                }
            }
        }
    }
}
