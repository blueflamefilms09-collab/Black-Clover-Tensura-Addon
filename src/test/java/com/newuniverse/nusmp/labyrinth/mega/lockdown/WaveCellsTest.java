package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the wave of a locked room has to stand INSIDE the room (a monster in the wall layer is in the doorway the barrier is about to fill), away from the party, on
 * the floor row, and differently each time. These tests pin the inset from the walls, the minimum distance from every player, that the template's spawn markers come
 * first, that a marker in a wall or beside a player is dropped, that two waves with different seeds stand in different places, and that a room too small for the inset
 * gives no cell rather than one in the wall.
 */
class WaveCellsTest {
    private static List<double[]> players(double... xz) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i + 1 < xz.length; i += 2) out.add(new double[] {xz[i], 65.0, xz[i + 1]});
        return out;
    }

    @Test
    void everyCellIsInsideTheRoomProperAndOnTheFloorRow() {
        List<WaveCells.Cell> c = WaveCells.pick(0, 0, 30, 30, 65, List.of(), players(15.5, 15.5), 4.0, new Random(1));
        assertFalse(c.isEmpty());
        for (WaveCells.Cell k : c) {
            assertTrue(k.x() >= WaveCells.WALL_INSET && k.x() <= 30 - WaveCells.WALL_INSET, "x " + k);
            assertTrue(k.z() >= WaveCells.WALL_INSET && k.z() <= 30 - WaveCells.WALL_INSET, "z " + k);
            assertEquals(65, k.y());
        }
    }

    @Test
    void noCellIsNearAPlayer() {
        List<double[]> ps = players(10.0, 10.0, 20.0, 20.0);
        for (WaveCells.Cell k : WaveCells.pick(0, 0, 30, 30, 65, List.of(), ps, 5.0, new Random(2))) {
            for (double[] p : ps) assertTrue(Math.hypot(p[0] - (k.x() + 0.5), p[2] - (k.z() + 0.5)) >= 5.0, k + " is too near");
        }
    }

    @Test
    void theTemplatesOwnMarkersComeFirst() {
        List<int[]> spots = List.of(new int[] {8, 65, 8}, new int[] {22, 65, 22});
        List<WaveCells.Cell> c = WaveCells.pick(0, 0, 30, 30, 65, spots, players(15.5, 15.5), 4.0, new Random(3));
        assertTrue(c.size() > 2);
        assertTrue(new WaveCells.Cell(8, 65, 8).equals(c.get(0)) || new WaveCells.Cell(22, 65, 22).equals(c.get(0)));
        assertTrue(new WaveCells.Cell(8, 65, 8).equals(c.get(1)) || new WaveCells.Cell(22, 65, 22).equals(c.get(1)));
    }

    @Test
    void aMarkerInTheWallOrBesideAPlayerIsDropped() {
        List<int[]> spots = List.of(new int[] {1, 65, 15}, new int[] {15, 65, 15}, new int[] {29, 65, 29});
        List<WaveCells.Cell> c = WaveCells.pick(0, 0, 30, 30, 65, spots, players(15.5, 15.5), 4.0, new Random(4));
        for (WaveCells.Cell k : c) {
            assertFalse(new WaveCells.Cell(1, 65, 15).equals(k), "a marker in the wall layer");
            assertFalse(new WaveCells.Cell(15, 65, 15).equals(k), "a marker on the player");
            assertFalse(new WaveCells.Cell(29, 65, 29).equals(k), "a marker in the wall layer (the far corner)");
        }
    }

    @Test
    void twoWavesOfTheSameRoomStandInDifferentPlaces() {
        List<WaveCells.Cell> a = WaveCells.pick(0, 0, 40, 40, 65, List.of(), players(20.5, 20.5), 4.0, new Random(1));
        List<WaveCells.Cell> b = WaveCells.pick(0, 0, 40, 40, 65, List.of(), players(20.5, 20.5), 4.0, new Random(2));
        assertEquals(a.size(), b.size());
        assertFalse(a.equals(b), "the order of the grid is shuffled by the seed");
        assertEquals(WaveCells.pick(0, 0, 40, 40, 65, List.of(), players(20.5, 20.5), 4.0, new Random(1)), a, "the same seed gives the same wave");
    }

    @Test
    void aRoomTooSmallForTheInsetGivesNoCell() {
        assertTrue(WaveCells.pick(0, 0, 5, 5, 65, List.of(), players(2.5, 2.5), 2.0, new Random(5)).isEmpty());
        assertTrue(WaveCells.pick(0, 0, 40, 40, 65, List.of(), players(20.5, 20.5), 1000.0, new Random(5)).isEmpty(), "everywhere is too near a player when the distance is huge");
    }
}
