package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Goliath's move table: what it can do at each power and distance, and how the scheduler plays a fight from it. */
class GoliathTableTest {
    private static final List<GoliathTable.Spec> SPECS = GoliathTable.specs();

    private static MoveScheduler.Situation at(double distance, int power, long now, int foes) {
        return new MoveScheduler.Situation(distance, 1f, power, foes, now, Double.POSITIVE_INFINITY, 900);
    }

    /** How often each move is picked in `draws` draws at a distance and power (nothing is committed, so no cooldown or repeat rule applies: one scheduler, many draws). */
    private static Map<String, Integer> shares(double distance, int power, int draws, int foes) {
        Map<String, Integer> n = new HashMap<>();
        MoveScheduler s = new MoveScheduler(0x5EEDL);
        for (int i = 0; i < draws; i++) {
            int pick = s.pick(SPECS, at(distance, power, 100_000L, foes));
            n.merge(pick < 0 ? "none" : SPECS.get(pick).id(), 1, Integer::sum);
        }
        return n;
    }

    @Test
    void theTableHoldsSixToEightMovesWithOneUltimate() {
        assertTrue(SPECS.size() >= 6 && SPECS.size() <= 8, "six to eight moves: " + SPECS.size());
        Set<String> ids = new HashSet<>();
        int ultimates = 0;
        for (GoliathTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertTrue(s.id().matches("[a-z_]+"));
            assertFalse(s.label().isBlank());
            if (s.ultimate()) ultimates++;
            assertSame(s, GoliathTable.byId(s.id()));
        }
        assertEquals(1, ultimates, "one ultimate");
        assertNull(GoliathTable.byId("nonsense"));
        assertEquals(GoliathTable.BEDROCK_QUAKE, SPECS.get(SPECS.size() - 1).id(), "the ultimate is last");
        for (String id : new String[]{GoliathTable.GROUND_SLAM, GoliathTable.TWIN_STOMP, GoliathTable.ROARING_SHOCKWAVE, GoliathTable.BEDROCK_FIST, GoliathTable.SEISMIC_LINE,
                GoliathTable.RUBBLE_FALL, GoliathTable.BEDROCK_QUAKE}) assertTrue(ids.contains(id), id);
    }

    @Test
    void everyMoveIsTelegraphedAndEscapable() {
        for (GoliathTable.Spec s : SPECS) {
            assertTrue(s.windUpTicks() >= 24, s.id() + " has a real wind-up: " + s.windUpTicks());
            assertTrue(s.windUpTicks() >= 8, "never under the framework's 8 ticks");
            if (s.ultimate()) {
                assertTrue(s.windUpTicks() >= 30, "an ultimate winds up for over a second and a half");
                assertTrue(s.recovery() >= 40, "and leaves the giant exposed (40 ticks or more)");
                assertEquals(60, s.manaCost());
                assertTrue(s.minPower() >= 60, "from 60% power");
            } else {
                assertTrue(s.recovery() >= 10 && s.recovery() < 40, s.id() + " recovers for a moment but is not exposed");
                assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
            }
            assertTrue(s.cooldownTicks() >= 100, s.id() + " is not spammed");
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.weight() >= 1 && s.weight() <= 8);
            assertFalse(GoliathTable.warning(s.id()).isBlank(), "a warning for " + s.id());
        }
        assertEquals("", GoliathTable.warning("nonsense"));
    }

    @Test
    void theRepertoireOpensUpAsThePowerRises() {
        for (GoliathTable.Spec s : SPECS) {
            assertTrue(s.minPower() == 20 || s.minPower() == 40 || s.minPower() == 60, s.id() + " opens at a step of the curve: " + s.minPower());
        }
        for (String id : new String[]{GoliathTable.GROUND_SLAM, GoliathTable.BEDROCK_FIST, GoliathTable.ROARING_SHOCKWAVE}) assertEquals(20, GoliathTable.byId(id).minPower(), id);
        for (String id : new String[]{GoliathTable.TWIN_STOMP, GoliathTable.SEISMIC_LINE}) assertEquals(40, GoliathTable.byId(id).minPower(), id);
        assertEquals(60, GoliathTable.byId(GoliathTable.RUBBLE_FALL).minPower());
        assertEquals(60, GoliathTable.byId(GoliathTable.BEDROCK_QUAKE).minPower());
        Set<String> seen = new HashSet<>(shares(5, 20, 400, 1).keySet());
        assertEquals(Set.of(GoliathTable.GROUND_SLAM, GoliathTable.BEDROCK_FIST), seen, "at the opening only the slam and the fist reach a foe close by");
    }

    @Test
    void moreSlamsCloseInAndMoreShockwavesFarOut() {
        Map<String, Integer> close = shares(3, 20, 4000, 1);
        assertEquals(0, close.getOrDefault(GoliathTable.ROARING_SHOCKWAVE, 0), "no roar at three blocks");
        assertTrue(close.getOrDefault(GoliathTable.GROUND_SLAM, 0) > close.getOrDefault(GoliathTable.BEDROCK_FIST, 0), "the slam beats the fist close in: " + close);
        Map<String, Integer> far = shares(22, 20, 1000, 1);
        assertEquals(1000, far.getOrDefault(GoliathTable.ROARING_SHOCKWAVE, 0), "at twenty-two blocks only the roar reaches: " + far);
        Map<String, Integer> mid = shares(12, 20, 4000, 1);
        assertEquals(0, mid.getOrDefault(GoliathTable.GROUND_SLAM, 0), "the slam does not reach twelve blocks");
        assertTrue(mid.getOrDefault(GoliathTable.BEDROCK_FIST, 0) > 0 && mid.getOrDefault(GoliathTable.ROARING_SHOCKWAVE, 0) > 0, "the fist and the roar share the middle: " + mid);
        double slamShare = close.get(GoliathTable.GROUND_SLAM) / 4000.0, roarFar = 1.0;
        assertTrue(slamShare > 0.5 && roarFar > slamShare, "slams are the common close move: " + slamShare);
    }

    @Test
    void theStompAndTheLineJoinAtFortyAndTheRainAndTheUltimateAtSixty() {
        Map<String, Integer> c40 = shares(6, 40, 6000, 1);
        assertTrue(c40.getOrDefault(GoliathTable.TWIN_STOMP, 0) > 0 && c40.getOrDefault(GoliathTable.SEISMIC_LINE, 0) > 0, "40%: " + c40);
        assertEquals(0, c40.getOrDefault(GoliathTable.RUBBLE_FALL, 0) + c40.getOrDefault(GoliathTable.BEDROCK_QUAKE, 0));
        Map<String, Integer> c60 = shares(6, 60, 6000, 2);
        assertTrue(c60.getOrDefault(GoliathTable.RUBBLE_FALL, 0) > 0 && c60.getOrDefault(GoliathTable.BEDROCK_QUAKE, 0) > 0, "60%: " + c60);
        Map<String, Integer> alone = shares(6, 60, 6000, 1), crowd = shares(6, 60, 6000, 3);
        assertTrue(crowd.getOrDefault(GoliathTable.RUBBLE_FALL, 0) > alone.getOrDefault(GoliathTable.RUBBLE_FALL, 0), "the rain is a crowd move: " + alone + " against " + crowd);
    }

    @Test
    void aWholeFightUsesEveryMoveWithoutRepeatingOrSpammingTheUltimate() {
        MoveScheduler s = new MoveScheduler(7);
        s.reset(0);
        Map<String, Integer> used = new HashMap<>();
        String last = "", beforeLast = "";
        long lastUltimate = -1_000_000L;
        int power = 100, gap = 900;
        for (long now = 0; now < 20 * 60 * 3; now += 4) {
            double distance = 2 + (now / 4 * 7) % 30;                                           // the foe wanders between 2 and 31 blocks
            MoveScheduler.Situation sit = new MoveScheduler.Situation(distance, (float) Math.max(0.05, 1.0 - now / 3600.0), power, 1 + (int) (now / 100 % 3), now, Double.POSITIVE_INFINITY, gap);
            int pick = s.pick(SPECS, sit);
            if (pick < 0) continue;
            GoliathTable.Spec m = SPECS.get(pick);
            s.commit(m, sit, Math.max(10, (int) Math.round(m.cooldownTicks() * 0.42)));
            used.merge(m.id(), 1, Integer::sum);
            assertFalse(m.id().equals(last) && m.id().equals(beforeLast) && !m.ultimate(), "never three of one move in a row: " + m.id());
            if (m.ultimate()) {
                assertTrue(now - lastUltimate >= gap, "ultimates are spaced by the gap");
                lastUltimate = now;
                assertFalse(last.equals(GoliathTable.BEDROCK_QUAKE), "never twice in a row");
            }
            beforeLast = last;
            last = m.id();
        }
        for (GoliathTable.Spec m : SPECS) assertTrue(used.getOrDefault(m.id(), 0) > 0, m.id() + " was never used in three minutes: " + used);
        assertTrue(used.get(GoliathTable.BEDROCK_QUAKE) >= 2 && used.get(GoliathTable.BEDROCK_QUAKE) <= 4, "the quake comes a few times, not constantly: " + used);
    }

    @Test
    void theReachGrowsAQuarterAtTheGates() {
        assertEquals(7.0, GoliathTable.reach(GoliathTable.SLAM_RADIUS, false), 1e-9);
        assertEquals(8.75, GoliathTable.reach(GoliathTable.SLAM_RADIUS, true), 1e-9);
        assertTrue(GoliathTable.SLAM_RADIUS < GoliathTable.STOMP_RADIUS && GoliathTable.FIST_RADIUS < GoliathTable.SLAM_RADIUS);
        assertTrue(GoliathTable.SHOCKWAVE_REACH >= 28 + 4, "the roar crosses the whole pit (its floor is 28 blocks in radius)");
        assertTrue(GoliathTable.QUAKE_REACH >= 28);
        assertTrue(GoliathTable.FIST > GoliathTable.SLAM && GoliathTable.SLAM > GoliathTable.STOMP_FIRST && GoliathTable.STOMP_FIRST > GoliathTable.STOMP_SECOND,
                "the fist is the heavy hit, the second stomp the light one");
        assertTrue(GoliathTable.QUAKE <= GoliathTable.STOMP_SECOND + 1, "the quake's three hits are small ones");
        assertTrue(GoliathTable.STOMP_BUDGET < GoliathTable.QUAKE_BUDGET && GoliathTable.QUAKE_BUDGET < 0.8, "no multi-hit move can take most of someone's health");
    }
}
