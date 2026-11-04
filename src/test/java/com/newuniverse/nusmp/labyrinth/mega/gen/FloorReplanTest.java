package com.newuniverse.nusmp.labyrinth.mega.gen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FloorReplanTest {
    @Test
    void replansOnlyFirstTimeJobsAndOnlyUpToTheLimit() {
        assertTrue(ExpansionQueue.mayReplan(0, 3, true));
        assertTrue(ExpansionQueue.mayReplan(2, 3, true));
        assertFalse(ExpansionQueue.mayReplan(3, 3, true));
        assertFalse(ExpansionQueue.mayReplan(0, 0, true));
        assertFalse(ExpansionQueue.mayReplan(0, 3, false));
    }
}
