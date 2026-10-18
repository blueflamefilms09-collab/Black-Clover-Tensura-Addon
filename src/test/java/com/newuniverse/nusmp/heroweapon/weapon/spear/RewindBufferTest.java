package com.newuniverse.nusmp.heroweapon.weapon.spear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.spear.RewindBuffer.Snap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the ring buffer is the memory of the Legendary Spear's time reversal. It must keep only the newest snapshots, hand them out oldest first, know when
 * the next one is due, and drop everything when the clock goes backwards, or the technique would rewind into a timeline that never happened.
 */
class RewindBufferTest {
    private static Snap at(long tick) { return new Snap(tick, tick, 64, 0, 20f, "minecraft:overworld", false, Set.of()); }

    @Test
    void itKeepsTheNewestAndForgetsTheOldestWhenFull() {
        RewindBuffer b = new RewindBuffer(3);
        for (long t = 0; t < 100; t += 20) b.push(at(t));       // 0, 20, 40, 60, 80
        assertEquals(3, b.size());
        List<Snap> list = b.snapshots();
        assertEquals(40L, list.get(0).tick(), "the two oldest were overwritten");
        assertEquals(60L, list.get(1).tick());
        assertEquals(80L, list.get(2).tick());
        assertEquals(80L, b.newest().tick());
        assertEquals(40L, b.get(0).tick());
        assertNull(b.get(3));
        assertNull(b.get(-1));
    }

    @Test
    void itIsDueWhenEmptyAndOnceTheStepHasPassed() {
        RewindBuffer b = new RewindBuffer(10);
        assertTrue(b.due(5, 20), "an empty buffer always wants one");
        b.push(at(100));
        assertFalse(b.due(119, 20));
        assertTrue(b.due(120, 20));
        assertTrue(b.due(500, 20));
    }

    @Test
    void aClockThatWentBackwardsEmptiesTheBuffer() {
        RewindBuffer b = new RewindBuffer(10);
        b.push(at(1000));
        b.push(at(1020));
        assertTrue(b.due(10, 20), "the world clock is now before the newest snapshot: take a new one");
        b.push(at(10));
        assertEquals(1, b.size(), "the old timeline is gone");
        assertEquals(10L, b.newest().tick());
    }

    @Test
    void clearEmptiesIt() {
        RewindBuffer b = new RewindBuffer(4);
        b.push(at(1));
        b.push(at(2));
        b.clear();
        assertEquals(0, b.size());
        assertNull(b.newest());
        assertTrue(b.snapshots().isEmpty());
        b.push(at(3));
        assertEquals(1, b.size());
    }

    @Test
    void aSnapshotCopiesItsEffectSetAndToleratesNulls() {
        java.util.HashSet<String> mutable = new java.util.HashSet<>(Set.of("minecraft:poison"));
        Snap s = new Snap(1, 0, 0, 0, 1f, null, false, mutable);
        mutable.add("minecraft:wither");
        assertEquals(Set.of("minecraft:poison"), s.harmful(), "later changes to the caller's set do not leak in");
        assertEquals("", s.dim());
        assertTrue(new Snap(1, 0, 0, 0, 1f, "x", true, null).harmful().isEmpty());
    }

    @Test
    void aTinyCapacityIsRaisedToTwo() {
        assertEquals(2, new RewindBuffer(0).capacity());
        assertEquals(2, new RewindBuffer(-5).capacity());
    }
}
