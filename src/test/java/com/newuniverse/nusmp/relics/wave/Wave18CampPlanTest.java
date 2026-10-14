package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The layout of The Pocket Campsite: one large tent, two small ones, a fire and seats, each cell once and everything inside the block cap. */
class Wave18CampPlanTest {
    private static long at(Wave18CampPlan.Cell c) { return (((long) c.u() * 1000 + c.y()) * 1000 + c.v()); }

    @Test
    void noCellIsPlacedTwiceAndTheCampFitsTheCap() {
        List<Wave18CampPlan.Cell> plan = Wave18CampPlan.plan();
        Set<Long> seen = new HashSet<>();
        for (Wave18CampPlan.Cell c : plan) assertTrue(seen.add(at(c)), "duplicate cell " + c);
        assertTrue(plan.size() <= Wave18Tune.CAMP_MAX_BLOCKS, "the camp is " + plan.size() + " blocks");
        assertTrue(plan.size() > 100, "a real camp, not a stub");
    }

    @Test
    void thereIsOneFireTwoSmallTentsOneBigWithABedAndFourSeats() {
        List<Wave18CampPlan.Cell> plan = Wave18CampPlan.plan();
        assertEquals(1, plan.stream().filter(c -> c.kind() == Wave18CampPlan.Kind.FIRE).count());
        assertEquals(4, plan.stream().filter(c -> c.kind() == Wave18CampPlan.Kind.SEAT).count());
        assertEquals(1, plan.stream().filter(c -> c.kind() == Wave18CampPlan.Kind.BED_FOOT).count());
        assertEquals(1, plan.stream().filter(c -> c.kind() == Wave18CampPlan.Kind.BED_HEAD).count());
        assertTrue(plan.stream().anyMatch(c -> c.kind() == Wave18CampPlan.Kind.LEFT_ROOF));
        assertTrue(plan.stream().anyMatch(c -> c.kind() == Wave18CampPlan.Kind.RIGHT_ROOF));
        Wave18CampPlan.Cell fire = plan.get(0);
        assertEquals(Wave18CampPlan.Kind.FIRE, fire.kind());
        assertEquals(0, fire.u());
        assertEquals(0, fire.v());
    }

    @Test
    void everyTentHasADoorFacingTheFireAndARoof() {
        List<Wave18CampPlan.Cell> plan = Wave18CampPlan.plan();
        Set<Long> cells = new HashSet<>();
        for (Wave18CampPlan.Cell c : plan) cells.add(at(c));
        // the big tent's door: the middle of the side facing the fire, two blocks high, open
        assertFalse(cells.contains(((long) 0 * 1000 + 0) * 1000 + (Wave18CampPlan.BIG_V - 2)), "door foot");
        assertFalse(cells.contains(((long) 0 * 1000 + 1) * 1000 + (Wave18CampPlan.BIG_V - 2)), "door head");
        // the left tent opens to the right (towards the fire), the right tent to the left
        assertFalse(cells.contains(((long) (-Wave18CampPlan.SMALL_U + 1) * 1000 + 0) * 1000 + Wave18CampPlan.SMALL_V), "left tent door");
        assertFalse(cells.contains(((long) (Wave18CampPlan.SMALL_U - 1) * 1000 + 0) * 1000 + Wave18CampPlan.SMALL_V), "right tent door");
        assertTrue(cells.contains(((long) 0 * 1000 + 3) * 1000 + Wave18CampPlan.BIG_V), "the peaked cap");
        long roofs = plan.stream().filter(c -> c.kind() == Wave18CampPlan.Kind.BIG_ROOF).count();
        assertEquals(25 + 9, roofs);
    }

    @Test
    void theFloorIsTheGroundCellsOnly() {
        List<Wave18CampPlan.Cell> plan = Wave18CampPlan.plan();
        List<Wave18CampPlan.Cell> floor = Wave18CampPlan.floor(plan);
        assertFalse(floor.isEmpty());
        for (Wave18CampPlan.Cell c : floor) assertEquals(0, c.y());
        assertTrue(Wave18CampPlan.footprint(plan).size() >= floor.size());
    }

    @Test
    void rotationTurnsLocalIntoWorldCoordinates() {
        // facing south (+z): forward is +z, right is -x
        assertArrayEquals(new int[]{0, 6}, Wave18CampPlan.world(0, 6, 0, 1));
        assertArrayEquals(new int[]{-5, 2}, Wave18CampPlan.world(5, 2, 0, 1));
        // facing north (-z): forward is -z, right is +x
        assertArrayEquals(new int[]{0, -6}, Wave18CampPlan.world(0, 6, 0, -1));
        assertArrayEquals(new int[]{5, -2}, Wave18CampPlan.world(5, 2, 0, -1));
        // facing east (+x): forward is +x, right is +z
        assertArrayEquals(new int[]{6, 0}, Wave18CampPlan.world(0, 6, 1, 0));
        assertArrayEquals(new int[]{2, 5}, Wave18CampPlan.world(5, 2, 1, 0));
        // facing west (-x): forward is -x, right is -z
        assertArrayEquals(new int[]{-6, 0}, Wave18CampPlan.world(0, 6, -1, 0));
    }
}
