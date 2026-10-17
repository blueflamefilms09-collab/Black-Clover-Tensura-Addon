package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Heath's cage must come on a rhythm a party can learn, so the first appointment, the gap and its floor are fixed here. */
class TimetableTest {
    @Test
    void theFirstAppointmentIsOpenAndTheNextOneIsSet() {
        Timetable t = new Timetable(300);
        t.reset();
        assertTrue(t.due(0));
        t.kept(1000, 1.0);
        assertFalse(t.due(1299));
        assertTrue(t.due(1300));
        assertEquals(1300, t.next());
    }

    @Test
    void theGapShrinksWithPowerButNeverUnderTheFloor() {
        Timetable t = new Timetable(300);
        assertEquals(300, t.interval(1.0));
        assertEquals(276, t.interval(0.92));
        assertEquals(210, t.interval(0.70));
        assertEquals(126, t.interval(0.42), "at the Gates the gap is 126, just above the floor of 120");
        assertEquals(Timetable.FLOOR, t.interval(0.05));
        assertEquals(Timetable.FLOOR, new Timetable(10).interval(1.0), "a base under the floor is lifted to it");
    }
}
