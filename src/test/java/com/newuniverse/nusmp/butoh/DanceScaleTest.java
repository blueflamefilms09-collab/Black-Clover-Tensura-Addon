package com.newuniverse.nusmp.butoh;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The hitbox modifier: whatever the dance does, every way out leaves no modifier behind. */
class DanceScaleTest {
    /** A stand-in for the attribute: it does exactly what the gate says and nothing else. */
    private static final class Fake {
        boolean present;
        double amount;

        void run(DanceScale.Op op) {
            switch (op.kind()) {
                case SET -> { present = true; amount = op.amount(); }
                case CLEAR -> { present = false; amount = 0; }
                case NONE -> { }
            }
        }
    }

    @Test
    void everyMoveAsksForAShrinkOfFifteenToTwentyFivePercent() {
        for (DanceMove m : DanceMove.values()) {
            double a = DanceScale.amountFor(m);
            assertTrue(a >= -0.25 - 1e-9 && a <= -0.15 + 1e-9, m + " " + a);
        }
    }

    @Test
    void startingSetsAndEndingClears() {
        DanceScale g = new DanceScale();
        Fake f = new Fake();
        f.run(g.update(true, DanceMove.SWAY));
        assertTrue(f.present);
        assertEquals(DanceScale.amountFor(DanceMove.SWAY), f.amount, 1e-9);
        f.run(g.update(false, null));
        assertFalse(f.present);
        assertFalse(g.applied());
    }

    @Test
    void changingMoveReplacesTheAmountAndNeverStacks() {
        DanceScale g = new DanceScale();
        Fake f = new Fake();
        f.run(g.update(true, DanceMove.SWAY));
        f.run(g.update(true, DanceMove.FOLD));
        assertEquals(DanceScale.amountFor(DanceMove.FOLD), f.amount, 1e-9);
        assertEquals(DanceScale.Kind.NONE, g.update(true, DanceMove.FOLD).kind(), "the same move again changes nothing");
    }

    @Test
    void clearingWhenNothingIsAppliedDoesNothing() {
        DanceScale g = new DanceScale();
        assertEquals(DanceScale.Kind.NONE, g.update(false, null).kind());
        assertEquals(DanceScale.Kind.NONE, g.update(true, null).kind(), "a dance with no move is no dance");
    }

    @Test
    void anyOrderOfMovesEndsClean() {
        Random r = new Random(7);
        for (int run = 0; run < 500; run++) {
            DanceScale g = new DanceScale();
            Fake f = new Fake();
            int n = r.nextInt(30);
            for (int i = 0; i < n; i++) {
                f.run(g.update(true, DanceMove.byIndex(r.nextInt(100))));
                assertTrue(f.present);
                assertTrue(f.amount >= -0.25 - 1e-9 && f.amount <= -0.15 + 1e-9);
                if (r.nextInt(6) == 0) { f.run(g.update(false, null)); assertFalse(f.present); }
            }
            f.run(g.update(false, null));
            assertFalse(f.present, "run " + run + " must end with no modifier");
            assertEquals(0.0, f.amount, 0.0);
        }
    }
}
