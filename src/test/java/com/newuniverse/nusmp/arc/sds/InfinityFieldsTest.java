package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The list of Infinity fields: the cap, which one a new field pushes out, and who stands inside. */
class InfinityFieldsTest {
    @Test
    void aNewFieldPushesOutTheOldestWhenTheCapIsFull() {
        InfinityFields f = new InfinityFields();
        assertNull(f.add(0, 0, 5.5, 10, 2));
        assertNull(f.add(20, 0, 5.5, 20, 2));
        assertEquals(2, f.count());
        InfinityFields.Field pushed = f.add(40, 0, 5.5, 30, 2);
        assertNotNull(pushed);
        assertEquals(1, pushed.id(), "the first one goes");
        assertEquals(2, f.count());
        assertFalse(f.stands(1));
        assertTrue(f.stands(2) && f.stands(3));
    }

    @Test
    void theGatesAllowThree() {
        InfinityFields f = new InfinityFields();
        for (int i = 0; i < 3; i++) assertNull(f.add(i * 20, 0, 5, i, 3));
        assertEquals(3, f.count());
        assertNotNull(f.add(100, 0, 5, 9, 3));
        assertEquals(3, f.count());
    }

    @Test
    void aCapOfZeroStillKeepsOne() {
        InfinityFields f = new InfinityFields();
        f.add(0, 0, 5, 0, 0);
        f.add(9, 0, 5, 1, 0);
        assertEquals(1, f.count());
    }

    @Test
    void insideMeansWithinTheRadiusOfAnyStandingField() {
        InfinityFields f = new InfinityFields();
        f.add(0, 0, 5, 0, 2);
        f.add(30, 30, 4, 1, 2);
        assertTrue(f.inside(3, 3));
        assertTrue(f.inside(32, 31));
        assertFalse(f.inside(10, 10));
        assertTrue(f.all().get(0).contains(5, 0), "the rim counts");
    }

    @Test
    void clearingEndsThemAllAndReportsHowMany() {
        InfinityFields f = new InfinityFields();
        f.add(0, 0, 5, 0, 3);
        f.add(9, 9, 5, 1, 3);
        assertEquals(2, f.clear());
        assertEquals(0, f.count());
        assertFalse(f.inside(0, 0));
        assertEquals(0, f.clear());
        f.add(1, 1, 5, 2, 3);
        assertEquals(3, f.all().get(0).id(), "ids never repeat, so an old zone can tell its field is gone");
    }
}
