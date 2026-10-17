package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the smoke dose reads one question of the cloud list (how thick is it here?), so a disc's shape, its end, the cap and the no-stacking rule are proven once. */
class CloudFieldTest {
    @Test
    void aDiscIsFlatAndRound() {
        CloudField f = new CloudField(4);
        f.add(new CloudField.Cloud(0, 64, 0, 7, 200, 1), 0);
        assertEquals(1, f.gainAt(0, 64, 0, 10));
        assertEquals(1, f.gainAt(6.9, 64, 0, 10));
        assertEquals(0, f.gainAt(7.1, 64, 0, 10), "outside the radius");
        assertEquals(1, f.gainAt(0, 65.9, 0, 10), "a jump above the floor is still in it");
        assertEquals(0, f.gainAt(0, 68, 0, 10), "a foe on a balcony above is not");
        assertEquals(0, f.gainAt(0, 61, 0, 10), "nor one far below");
    }

    @Test
    void aDiscIsGoneWhenItsTimeIsUp() {
        CloudField f = new CloudField(4);
        f.add(new CloudField.Cloud(0, 64, 0, 7, 200, 1), 0);
        assertEquals(1, f.gainAt(0, 64, 0, 199));
        assertEquals(0, f.gainAt(0, 64, 0, 200));
        assertTrue(f.live(200).isEmpty());
        assertEquals(1, f.live(199).size());
    }

    @Test
    void overlapTakesTheThickestAndNeverTheSum() {
        CloudField f = new CloudField(4);
        f.add(new CloudField.Cloud(0, 64, 0, 7, 500, 1), 0);
        f.add(new CloudField.Cloud(12, 64, 0, 6, 500, 2), 0);
        f.add(new CloudField.Cloud(1, 64, 1, 7, 500, 1), 0);
        assertEquals(2, f.gainAt(6.5, 64, 0, 10), "in a garden and in the veil: the veil, not the sum");
        assertEquals(1, f.gainAt(-5, 64, 0, 10), "only a garden reaches this far");
        assertEquals(2, f.gainAt(15, 64, 0, 10), "only the veil");
        assertEquals(0, f.gainAt(30, 64, 0, 10));
    }

    @Test
    void theArenaCanNeverFillWithSmoke() {
        CloudField f = new CloudField(2);
        f.add(new CloudField.Cloud(0, 64, 0, 3, 500, 1), 0);
        f.add(new CloudField.Cloud(20, 64, 0, 3, 500, 1), 0);
        f.add(new CloudField.Cloud(40, 64, 0, 3, 500, 1), 0);
        assertEquals(2, f.live(1).size());
        assertEquals(0, f.gainAt(0, 64, 0, 1), "the oldest was dropped");
        assertEquals(1, f.gainAt(40, 64, 0, 1));
        f.add(new CloudField.Cloud(60, 64, 0, 3, 900, 1), 600);
        assertEquals(1, f.live(601).size(), "the expired ones went first");
        f.clear();
        assertTrue(f.live(601).isEmpty());
    }
}
