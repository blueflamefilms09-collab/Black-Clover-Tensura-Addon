package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Act;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Cue;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Foe;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Order;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Role;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.State;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Unit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The hive mind: its states, its roles, its formations, its turns, and what it learns. All of it in a made-up world of numbers. */
class HiveTacticsTest {
    private static HiveTactics hive() { return new HiveTactics(0, 0, 0, 0, 46, 42); }

    private static Unit u(int id, Kind k, double x, double z, double hp) { return new Unit(id, k, x, z, hp, x, z); }

    private static Foe foe(int id, double x, double z, double power) { return new Foe(id, x, z, power); }

    private static double dist(double ax, double az, double bx, double bz) { return Math.hypot(ax - bx, az - bz); }

    /** A mixed party of n members spread in a ring of radius r round (cx, cz): orcs, lizardmen, hounds, slimes, goblins in turn. */
    private static List<Unit> swarm(int n, double cx, double cz, double r) {
        Kind[] kinds = {Kind.ORC, Kind.LIZARDMAN, Kind.ORC, Kind.SLIME, Kind.HOUND, Kind.GOBLIN};
        List<Unit> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / Math.max(1, n);
            out.add(u(i + 1, kinds[i % kinds.length], cx + Math.cos(a) * r, cz + Math.sin(a) * r, 1.0));
        }
        return out;
    }

    private static Order orderOf(List<Unit> units, Order[] orders, int id) {
        for (int i = 0; i < units.size(); i++) if (units.get(i).id() == id) return orders[i];
        throw new AssertionError("no unit " + id);
    }

    private static Map<Role, Integer> roles(Order[] orders) {
        Map<Role, Integer> m = new EnumMap<>(Role.class);
        for (Order o : orders) m.merge(o.role(), 1, Integer::sum);
        return m;
    }

    /** Brings a hive into ENGAGE at the given tick by springing a trap on a foe, and thinks once. */
    private static Order[] engaged(HiveTactics h, List<Unit> units, Foe f, long tick) {
        h.ambush(f, tick);
        Order[] o = h.think(tick, units);
        assertTrue(h.state() == State.ENGAGE || h.state() == State.SURROUND, "engaged: " + h.state());
        return o;
    }

    // ---------------------------------------------------------------- search, scout, alarm
    @Test
    void aHiveThatSeesNothingWandersNearHomeAndSaysNothing() {
        HiveTactics h = hive();
        List<Unit> units = swarm(20, 0, 0, 25);
        for (long t = 0; t < 2000; t += HiveTactics.THINK) {
            Order[] o = h.think(t, units);
            for (int i = 0; i < o.length; i++) {
                assertEquals(Act.WANDER, o[i].act());
                assertTrue(dist(o[i].goalX(), o[i].goalZ(), units.get(i).homeX(), units.get(i).homeZ()) <= 5.5, "near home");
                assertFalse(o[i].strike());
                assertEquals(-1, o[i].target());
                assertTrue(o[i].speed() < 1.0, "an easy walk");
            }
        }
        assertSame(State.SEARCH, h.state());
        assertNull(h.takeCue());
    }

    @Test
    void theFirstToSpotAFoeBecomesTheScoutAndRunsBackToTheBanner() {
        HiveTactics h = hive();
        List<Unit> units = swarm(12, 20, 0, 6);
        h.sight(4, foe(100, 30, 0, 1.0), 10);
        h.sight(7, foe(100, 30, 0, 1.0), 12);
        Order[] o = h.think(20, units);
        assertSame(State.SEARCH, h.state(), "the hive has not been told yet");
        assertEquals(4, h.scoutId());
        Order scout = orderOf(units, o, 4);
        assertEquals(Role.SCOUT, scout.role());
        assertEquals(Act.MOVE, scout.act());
        assertEquals(0.0, scout.goalX(), 1e-9);
        assertEquals(0.0, scout.goalZ(), 1e-9);
        assertTrue(scout.speed() > 1.2, "it runs");
        for (Unit other : units) if (other.id() != 4) assertEquals(Act.WANDER, orderOf(units, o, other.id()).act(), "the others have not heard");
        assertNull(h.takeCue());
    }

    @Test
    void theScoutRaisesTheAlarmAtTheBannerAndTheHiveAssembles() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>(swarm(12, 20, 0, 6));
        h.sight(4, foe(100, 30, 0, 1.0), 10);
        h.think(20, units);
        // the scout arrives at the banner
        for (int i = 0; i < units.size(); i++) if (units.get(i).id() == 4) units.set(i, u(4, units.get(i).kind(), 3, 1, 1.0));
        Order[] o = h.think(30, units);
        assertSame(State.ASSEMBLE, h.state());
        Cue c = h.takeCue();
        assertNotNull(c);
        assertSame(State.SEARCH, c.from());
        assertSame(State.ASSEMBLE, c.to());
        assertNull(h.takeCue(), "a change is announced once");
        for (Order each : o) {
            assertEquals(Act.MOVE, each.act(), "everyone goes to the banner");
            assertTrue(dist(each.goalX(), each.goalZ(), 0, 0) <= HiveTactics.ASSEMBLE_RADIUS, "round the banner");
        }
        assertEquals(-1, h.scoutId(), "the scout's work is done");
    }

    @Test
    void aScoutThatIsTooSlowRaisesTheAlarmAnyway() {
        HiveTactics h = hive();
        List<Unit> units = swarm(8, 40, 0, 3);
        h.sight(2, foe(100, 45, 0, 1.0), 0);
        h.think(10, units);
        assertSame(State.SEARCH, h.state());
        h.think(HiveTactics.ALERT_TIMEOUT - 1, units);
        assertSame(State.SEARCH, h.state());
        h.think(HiveTactics.ALERT_TIMEOUT, units);
        assertSame(State.ASSEMBLE, h.state());
    }

    @Test
    void painIsSharedAndAlarmsTheHiveAtOnce() {
        HiveTactics h = hive();
        List<Unit> units = swarm(10, 20, 0, 5);
        h.hurt(3, 100, 25, 0, 2.0, 50);
        h.think(60, units);
        assertSame(State.ASSEMBLE, h.state());
    }

    @Test
    void aScoutThatFallsBeforeItGetsHomeLeavesTheHiveUnaware() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>(swarm(10, 20, 0, 5));
        h.sight(3, foe(100, 30, 0, 1.0), 0);
        h.think(10, units);
        assertEquals(3, h.scoutId());
        h.died(3, 20);
        units.removeIf(x -> x.id() == 3);
        for (long t = 30; t < 600; t += 10) h.think(t, units);
        assertSame(State.SEARCH, h.state(), "nobody told them");
        assertEquals(-1, h.scoutId());
        // a scout that simply is not there any more (it left the world) is dropped the same way
        h.sight(5, foe(100, 30, 0, 1.0), 700);
        h.think(710, units);
        units.removeIf(x -> x.id() == 5);
        h.think(720, units);
        assertEquals(-1, h.scoutId());
        assertSame(State.SEARCH, h.state());
    }

    @Test
    void aSprungTrapSendsTheHiveStraightToTheFight() {
        HiveTactics h = hive();
        List<Unit> units = swarm(6, 10, 0, 3);
        h.ambush(foe(100, 12, 0, 1.0), 5);
        h.think(10, units);
        assertTrue(h.state() == State.ENGAGE || h.state() == State.SURROUND);
        Cue c = h.takeCue();
        assertSame(State.SEARCH, c.from());
        assertSame(State.ENGAGE, c.to());
    }

    // ---------------------------------------------------------------- assembling
    @Test
    void assemblesUntilEnoughHaveGatheredThenEngages() {
        HiveTactics h = hive();
        List<Unit> far = swarm(20, 30, 20, 4);
        h.hurt(1, 100, 25, 5, 2.0, 0);
        h.think(10, far);
        assertSame(State.ASSEMBLE, h.state());
        h.think(20, far);
        assertSame(State.ASSEMBLE, h.state(), "nobody is at the banner yet");
        List<Unit> gathered = swarm(20, 0, 0, 4);
        h.think(30, gathered);
        assertSame(State.ENGAGE, h.state());
    }

    @Test
    void doesNotWaitForeverToAssemble() {
        HiveTactics h = hive();
        List<Unit> far = swarm(20, 30, 20, 4);
        h.hurt(1, 100, 25, 5, 2.0, 0);
        h.think(10, far);
        assertSame(State.ASSEMBLE, h.state());
        h.think(10 + HiveTactics.ASSEMBLE_MAX - 1, far);
        assertSame(State.ASSEMBLE, h.state());
        h.think(10 + HiveTactics.ASSEMBLE_MAX, far);
        assertSame(State.ENGAGE, h.state());
    }

    @Test
    void givesUpTheHuntWhenTheTrailGoesCold() {
        HiveTactics h = hive();
        List<Unit> units = swarm(14, 0, 0, 5);
        engaged(h, units, foe(100, 20, 0, 1.0), 0);
        assertNotSame(State.SEARCH, h.state());
        h.think(HiveTactics.LOSE + 20, units);
        assertSame(State.SEARCH, h.state(), "nobody has seen anything for too long");
        Order[] o = h.think(HiveTactics.LOSE + 30, units);
        for (Order each : o) assertEquals(Act.WANDER, each.act());
    }

    // ---------------------------------------------------------------- roles and formations
    @Test
    void everyKindGetsItsRoleAndTheFormationFitsIt() {
        HiveTactics h = hive();
        List<Unit> units = swarm(18, -12, 0, 5);
        Foe target = foe(100, 20, 0, 1.0);
        Order[] o = engaged(h, units, target, 0);
        assertSame(State.ENGAGE, h.state(), "not so overwhelming that it rings the foe");
        Map<Role, Integer> r = roles(o);
        assertTrue(r.getOrDefault(Role.TANK, 0) >= 1 && r.get(Role.TANK) <= 8, "tanks: " + r);
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            switch (un.kind()) {
                case SLIME -> assertEquals(Role.SLIME, each.role());
                case HOUND -> assertEquals(Role.FLANKER, each.role(), "hounds flank once the hive is big enough");
                case GOBLIN -> assertEquals(Role.SKIRMISHER, each.role());
                case ORC -> assertTrue(each.role() == Role.TANK || each.role() == Role.SKIRMISHER);
                case LIZARDMAN -> assertTrue(each.role() == Role.FLANKER || each.role() == Role.SKIRMISHER);
            }
            assertEquals(100, each.target());
        }
        double tankSpeed = 9, flankSpeed = 0;
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            double d = dist(each.goalX(), each.goalZ(), 20, 0);
            if (each.role() == Role.TANK) {
                assertEquals(2.3, d, 0.05, "a tank holds the front, close");
                tankSpeed = Math.min(tankSpeed, each.speed());
                assertEquals(Act.ENGAGE, each.act());
                assertTrue(each.goalX() < 20, "on the hive's side of the foe: the front");
            } else if (each.role() == Role.FLANKER) {
                assertEquals(Math.min(3.2, un.kind().reach + 0.8), d, 0.05, "close enough to land a blow from its place");
                assertTrue(Math.abs(each.goalZ()) > 1.0 || each.goalX() > 20 - 0.5, "to the side or behind, not in front: " + each);
                flankSpeed = Math.max(flankSpeed, each.speed());
            } else if (each.role() == Role.SLIME) {
                assertEquals(2.2, d, 0.05);
            }
        }
        assertTrue(tankSpeed < flankSpeed, "tanks are slow, flankers fast");
    }

    @Test
    void flankersCircleToBothSidesAndBehind() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 6; i++) units.add(u(10 + i, Kind.ORC, -15, i - 3, 1.0));
        for (int i = 0; i < 6; i++) units.add(u(20 + i, Kind.HOUND, -15, i - 3, 1.0));
        Order[] o = engaged(h, units, foe(100, 20, 0, 1.0), 0);
        int left = 0, right = 0, behind = 0;
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            if (each.role() != Role.FLANKER) continue;
            double dz = each.goalZ() - 0, dx = each.goalX() - 20;
            if (dx > 2.0) behind++;
            else if (dz > 0.5) left++;
            else if (dz < -0.5) right++;
        }
        assertTrue(left >= 1 && right >= 1, "both sides: " + left + "/" + right);
        assertTrue(behind >= 1, "and one behind it");
    }

    @Test
    void aSmallHiveDoesNotSendFlankers() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 4; i++) units.add(u(i + 1, Kind.HOUND, -10, i, 1.0));
        Order[] o = engaged(h, units, foe(100, 20, 0, 1.0), 0);
        for (Order each : o) assertEquals(Role.SKIRMISHER, each.role(), "alone, a flanker is only a victim");
    }

    @Test
    void rolesDoNotFlipFromThoughtToThought() {
        HiveTactics h = hive();
        List<Unit> units = swarm(18, -12, 0, 5);
        Order[] first = engaged(h, units, foe(100, 20, 0, 1.0), 0);
        for (long t = 10; t < 300; t += 10) {
            h.sight(1, foe(100, 20, 0, 1.0), t);
            Order[] now = h.think(t, units);
            for (int i = 0; i < units.size(); i++) assertEquals(first[i].role(), now[i].role(), "unit " + units.get(i).id() + " at " + t);
        }
    }

    // ---------------------------------------------------------------- turns
    @Test
    void membersStrikeInTurnsAndNeverTogetherOnOneFoe() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 6; i++) units.add(u(10 + i, Kind.ORC, 20 + Math.cos(i) * 2.0, Math.sin(i) * 2.0, 1.0));
        for (int i = 0; i < 2; i++) units.add(u(20 + i, Kind.HOUND, 20 - Math.cos(i + 2) * 2.0, Math.sin(i + 2) * 2.0, 1.0));
        Order[] o = engaged(h, units, foe(100, 20, 0, 1.0), 100);
        List<Integer> holders = new ArrayList<>();
        for (Unit un : units) if (orderOf(units, o, un.id()).strike()) holders.add(un.id());
        assertEquals(HiveTactics.TOKENS_PER_FOE, holders.size(), "two tokens for one foe: " + holders);
        for (int id : holders) assertEquals(Role.TANK, orderOf(units, o, id).role(), "the tanks take the first turns");
        int a = holders.get(0), b = holders.get(1);
        assertTrue(h.mayStrike(a, 100, 100));
        int other = -1;
        for (Unit un : units) if (!holders.contains(un.id())) other = un.id();
        assertFalse(h.mayStrike(other, 100, 100), "no token, no blow");
        h.struck(a, Kind.ORC, 100, 100);
        assertFalse(h.mayStrike(a, 100, 101), "its token is spent");
        assertFalse(h.mayStrike(b, 100, 100 + HiveTactics.FOE_GAP - 1), "one blow on a foe every FOE_GAP ticks, whoever strikes");
        assertTrue(h.mayStrike(b, 100, 100 + HiveTactics.FOE_GAP), "then the next member's turn");
        // at the next thought the one who struck is cooling down
        Order[] later = h.think(110, units);
        assertFalse(orderOf(units, later, a).strike(), "its own cooldown (" + Kind.ORC.gap + " ticks) is not over");
        Order[] much = h.think(100 + Kind.ORC.gap + 10, units);
        int again = 0;
        for (Unit un : units) if (orderOf(units, much, un.id()).strike()) again++;
        assertTrue(again <= HiveTactics.TOKENS_PER_FOE);
    }

    @Test
    void noTokensForThoseOutOfReachOrForTheWoundedOrScouts() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        units.add(u(1, Kind.ORC, 20, 8, 1.0));                // too far
        units.add(u(2, Kind.ORC, 20, 1.5, 0.2));              // wounded: it falls back
        units.add(u(3, Kind.ORC, 20, -1.5, 1.0));             // in reach
        Order[] o = engaged(h, units, foe(100, 20, 0, 1.0), 0);
        assertFalse(orderOf(units, o, 1).strike());
        assertFalse(orderOf(units, o, 2).strike());
        assertEquals(Role.RETREATER, orderOf(units, o, 2).role());
        assertTrue(orderOf(units, o, 3).strike());
    }

    @Test
    void tokensAreFewWhateverTheCrowd() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40;
            units.add(u(i + 1, i % 3 == 0 ? Kind.ORC : i % 3 == 1 ? Kind.GOBLIN : Kind.SLIME, 20 + Math.cos(a) * 1.4, Math.sin(a) * 1.4, 1.0));
        }
        Order[] o = engaged(h, units, foe(100, 20, 0, 1.0), 0);
        int n = 0;
        for (Order each : o) if (each.strike()) n++;
        assertTrue(n <= HiveTactics.TOKENS_PER_FOE, "forty on one, two strike: " + n);
        List<Foe> many = List.of(foe(100, 20, 0, 1.0), foe(101, 21, 0, 1.0), foe(102, 20, 1, 1.0), foe(103, 21, 1, 1.0));
        HiveTactics h2 = hive();
        for (Foe f : many) h2.ambush(f, 0);
        Order[] o2 = h2.think(0, units);
        int m = 0;
        for (Order each : o2) if (each.strike()) m++;
        assertTrue(m <= HiveTactics.MAX_TOKENS, "never more than the tokens: " + m);
    }

    @Test
    void skirmishersHitThenDrawBackThenComeAgain() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        units.add(u(1, Kind.GOBLIN, 18.5, 0, 1.0));
        units.add(u(2, Kind.SLIME, -10, 0, 1.0));
        Foe f = foe(100, 20, 0, 1.0);
        Order[] o = engaged(h, units, f, 100);
        Order gob = orderOf(units, o, 1);
        assertEquals(Role.SKIRMISHER, gob.role());
        assertTrue(gob.strike());
        h.struck(1, Kind.GOBLIN, 100, 100);
        h.sight(2, f, 110);
        Order back = orderOf(units, h.think(110, units), 1);
        assertEquals(Act.MOVE, back.act(), "it draws back");
        assertTrue(dist(back.goalX(), back.goalZ(), 20, 0) > 5.0, "a good way back: " + dist(back.goalX(), back.goalZ(), 20, 0));
        assertFalse(back.strike());
        h.sight(2, f, 125);
        Order still = orderOf(units, h.think(125, units), 1);
        assertEquals(Act.MOVE, still.act());
        h.sight(2, f, 135);
        Order again = orderOf(units, h.think(100 + HiveTactics.SKIRM_RETREAT + 5, units), 1);
        assertEquals(Act.ENGAGE, again.act(), "and in again");
        assertTrue(dist(again.goalX(), again.goalZ(), 20, 0) < 2.5);
    }

    @Test
    void aSlimeMayRootAFoeOnlyNowAndThen() {
        HiveTactics h = hive();
        assertTrue(h.mayRoot(7, 100));
        assertFalse(h.mayRoot(7, 101));
        assertFalse(h.mayRoot(7, 100 + HiveTactics.SLOW_GAP - 1));
        assertTrue(h.mayRoot(7, 100 + HiveTactics.SLOW_GAP));
        assertTrue(h.mayRoot(8, 101), "another foe has its own clock");
    }

    // ---------------------------------------------------------------- surround
    @Test
    void anOverwhelmingHiveClosesARingRoundTheFoeFromEverySide() {
        HiveTactics h = hive();
        List<Unit> units = swarm(30, 8, 0, 6);
        Foe f = foe(100, 20, 0, 1.0);
        Order[] o = engaged(h, units, f, 0);
        assertSame(State.SURROUND, h.state());
        List<Double> angles = new ArrayList<>();
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            if (each.role() == Role.RETREATER) continue;
            double d = dist(each.goalX(), each.goalZ(), 20, 0);
            assertTrue(d >= 2.3 && d <= 3.1, "on the ring: " + d);
            assertTrue(d <= un.kind().reach + 1.0, "within reach of a blow from its place on the ring: " + un.kind() + " " + d);
            angles.add(Math.atan2(each.goalZ(), each.goalX() - 20));
        }
        java.util.Collections.sort(angles);
        double biggest = 0;
        for (int i = 0; i < angles.size(); i++) {
            double next = i + 1 < angles.size() ? angles.get(i + 1) : angles.get(0) + Math.PI * 2;
            biggest = Math.max(biggest, next - angles.get(i));
        }
        assertTrue(biggest < Math.PI / 2, "no gap in the ring wider than a quarter turn: " + biggest);
        boolean behind = false;
        for (double a : angles) if (Math.abs(a) < 0.5) behind = true;                          // the foe's far side (the hive came from the -x side)
        assertTrue(behind, "some stand behind it");
        Cue c = h.takeCue();
        assertNotNull(c);
    }

    @Test
    void theRingBreaksWhenTheOddsTurnAndDoesNotCloseAgainAtOnce() {
        HiveTactics h = hive();
        List<Unit> units = swarm(30, 8, 0, 6);
        engaged(h, units, foe(100, 20, 0, 1.0), 0);
        assertSame(State.SURROUND, h.state());
        // the party grows: three more players join the fight
        for (int i = 0; i < 3; i++) h.sight(1, foe(101 + i, 20 + i, 1, 2.0), 20);
        h.sight(1, foe(100, 20, 0, 1.0), 20);
        h.think(20, units);
        assertSame(State.ENGAGE, h.state(), "too few for a ring");
        h.takeCue();
        // the party shrinks again at once: not surrounded again before the pause is over
        HiveTactics h2 = hive();
        engaged(h2, units, foe(100, 20, 0, 1.0), 0);
        assertSame(State.SURROUND, h2.state());
        h2.think(HiveTactics.SURROUND_MAX, units);
        assertSame(State.ENGAGE, h2.state(), "the ring is held only so long");
        h2.sight(1, foe(100, 20, 0, 1.0), HiveTactics.SURROUND_MAX + 10);
        h2.think(HiveTactics.SURROUND_MAX + 10, units);
        assertSame(State.ENGAGE, h2.state(), "not at once");
        h2.sight(1, foe(100, 20, 0, 1.0), HiveTactics.SURROUND_MAX + HiveTactics.SURROUND_COOL + 5);
        h2.think(HiveTactics.SURROUND_MAX + HiveTactics.SURROUND_COOL + 5, units);
        assertSame(State.SURROUND, h2.state(), "but after it");
    }

    @Test
    void aClumpIsRingedAsOneAndItsMembersAreAllStruckAt() {
        HiveTactics h = hive();
        List<Unit> units = swarm(30, 8, 0, 6);
        h.ambush(foe(100, 20, 0, 1.0), 0);
        h.sight(1, foe(101, 21, 1, 1.0), 0);
        Order[] o = h.think(0, units);
        assertSame(State.SURROUND, h.state());
        double cx = 20.5, cz = 0.5;
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            if (each.role() == Role.RETREATER) continue;
            assertTrue(dist(each.goalX(), each.goalZ(), cx, cz) >= 4.0, "a ring round the two of them, not through them");
        }
    }

    @Test
    void tanksAndSlimesSplitAClumpBetweenTheirMembers() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 4; i++) units.add(u(10 + i, Kind.ORC, 12, i - 2, 1.0));
        for (int i = 0; i < 3; i++) units.add(u(20 + i, Kind.SLIME, 12, i + 3, 1.0));
        for (int i = 0; i < 3; i++) units.add(u(30 + i, Kind.GOBLIN, 12, i - 6, 1.0));
        HiveTactics hh = hive();
        hh.ambush(foe(100, 20, 0, 4.0), 0);                                           // a strong party: the hive does not close a ring round it
        hh.sight(1, foe(101, 21, 1, 4.0), 0);
        hh.sight(1, foe(102, 20, 2, 4.0), 0);
        Order[] o = hh.think(0, units);
        assertSame(State.ENGAGE, hh.state());
        Map<Integer, Integer> hits = new java.util.TreeMap<>();
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            if (each.role() == Role.TANK || each.role() == Role.SLIME) {
                assertTrue(dist(each.goalX(), each.goalZ(), 20.33, 1.0) <= 1.0, "they push into the middle of the clump: " + each);
                hits.merge(each.target(), 1, Integer::sum);
            }
        }
        assertEquals(3, hits.size(), "the strikes are spread over all three: " + hits);
    }

    // ---------------------------------------------------------------- losing, falling back, regrouping
    @Test
    void fallsBackWhenTheLossesMountAndRegroupsAtTheBanner() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>(swarm(20, 8, 0, 6));
        engaged(h, units, foe(100, 20, 0, 4.0), 0);                                    // a strong foe: no ring
        assertSame(State.ENGAGE, h.state());
        long t = 100;
        for (int i = 0; i < 8; i++) {
            h.died(units.get(0).id(), t);
            units.remove(0);
        }
        h.sight(1, foe(100, 20, 0, 4.0), t);
        Order[] o = h.think(t, units);
        assertSame(State.FALL_BACK, h.state(), "eight of twenty fell");
        for (Order each : o) {
            assertTrue(dist(each.goalX(), each.goalZ(), 0, 0) <= 6.0, "home to the banner");
            assertTrue(each.speed() >= 1.2);
            assertFalse(each.strike());
        }
        // home at last
        List<Unit> home = new ArrayList<>();
        for (Unit un : units) home.add(u(un.id(), un.kind(), 2, 1, 0.5));
        h.think(t + 10, home);
        assertSame(State.REFORM, h.state());
        Order[] mend = h.think(t + 20, home);
        for (Order each : mend) assertTrue(each.heal(), "they mend at the banner");
        // mended: back to the fight
        List<Unit> well = new ArrayList<>();
        for (Unit un : home) well.add(u(un.id(), un.kind(), 2, 1, 0.9));
        h.sight(1, foe(100, 40, 10, 4.0), t + 30);
        h.think(t + 30, well);
        assertSame(State.ASSEMBLE, h.state());
    }

    @Test
    void fallsBackFromAPartyItCannotBeat() {
        HiveTactics h = hive();
        List<Unit> units = swarm(8, 8, 0, 4);
        h.ambush(foe(100, 20, 0, 4.0), 0);
        for (int i = 1; i <= 3; i++) h.sight(1, foe(100 + i, 20 + i * 8, i * 8, 4.0), 0);       // four heavily armed players, spread out
        h.think(0, units);
        assertSame(State.ENGAGE, h.state(), "it does not flee at once");
        h.sight(1, foe(100, 20, 0, 4.0), HiveTactics.ENGAGE_DWELL);
        for (int i = 1; i <= 3; i++) h.sight(1, foe(100 + i, 20 + i * 8, i * 8, 4.0), HiveTactics.ENGAGE_DWELL);
        h.think(HiveTactics.ENGAGE_DWELL, units);
        assertSame(State.FALL_BACK, h.state(), "but it sees what it is up against");
    }

    @Test
    void fallsBackWhenTheWholeHiveIsBadlyHurt() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 10; i++) units.add(u(i + 1, Kind.ORC, 8, i, 0.9));
        engaged(h, units, foe(100, 20, 0, 1.0), 0);
        List<Unit> hurt = new ArrayList<>();
        for (Unit un : units) hurt.add(u(un.id(), un.kind(), un.x(), un.z(), 0.3));
        h.sight(1, foe(100, 20, 0, 1.0), HiveTactics.ENGAGE_DWELL);
        h.think(HiveTactics.ENGAGE_DWELL, hurt);
        assertSame(State.FALL_BACK, h.state());
    }

    @Test
    void theFallBackIsNotStartedInTheFirstSecondsOfAFight() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 10; i++) units.add(u(i + 1, Kind.ORC, 8, i, 0.3));
        engaged(h, units, foe(100, 20, 0, 1.0), 0);
        h.think(HiveTactics.ENGAGE_DWELL - 10, units);
        assertSame(State.ENGAGE, h.state(), "give it a moment");
    }

    @Test
    void aRegroupingHiveThatIsCorneredFightsBack() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 10; i++) units.add(u(i + 1, Kind.ORC, 2, i - 5, 0.5));
        h.ambush(foe(100, 20, 0, 4.0), 0);
        h.think(0, units);
        for (int i = 0; i < 6; i++) h.died(99 + i, 10);
        h.sight(1, foe(100, 20, 0, 4.0), 70);
        h.think(70, units);
        // drive it to REFORM by hand: falls back, gets home
        int guard = 0;
        while (h.state() != State.REFORM && guard++ < 100) {
            h.sight(1, foe(100, 40, 20, 4.0), 70 + guard * 10);
            h.think(70 + guard * 10, units);
        }
        assertSame(State.REFORM, h.state());
        long now = 70 + guard * 10 + HiveTactics.REFORM_MIN;
        h.sight(1, foe(100, 5, 0, 1.0), now);                                           // the party is at the banner
        h.think(now, units);
        assertSame(State.ENGAGE, h.state(), "cornered at home: it fights");
    }

    @Test
    void theWoundedFallBackAndOnlyComeBackWhenHealed() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>(swarm(12, 8, 0, 5));
        Foe f = foe(100, 20, 0, 4.0);
        engaged(h, units, f, 0);
        int victim = units.get(0).id();
        List<Unit> now = new ArrayList<>();
        for (Unit un : units) now.add(un.id() == victim ? u(un.id(), un.kind(), un.x(), un.z(), 0.3) : un);
        h.sight(1, f, 10);
        Order o = orderOf(now, h.think(10, now), victim);
        assertEquals(Role.RETREATER, o.role());
        assertTrue(dist(o.goalX(), o.goalZ(), 0, 0) <= 5.5, "to the banner");
        assertEquals(-1, o.target());
        // a little better: still out (the latch)
        List<Unit> better = new ArrayList<>();
        for (Unit un : units) better.add(un.id() == victim ? u(un.id(), un.kind(), 1, 1, 0.5) : un);
        h.sight(1, f, 20);
        Order mid = orderOf(better, h.think(20, better), victim);
        assertEquals(Role.RETREATER, mid.role(), "not healed yet");
        assertTrue(mid.heal(), "mending at the banner");
        // healed
        List<Unit> healed = new ArrayList<>();
        for (Unit un : units) healed.add(un.id() == victim ? u(un.id(), un.kind(), 1, 1, 0.9) : un);
        h.sight(1, f, 30);
        Order back = orderOf(healed, h.think(30, healed), victim);
        assertNotEquals(Role.RETREATER, back.role());
        assertEquals(100, back.target());
    }

    @Test
    void aWoundedMemberDoesNotMendWhileAFoeIsNearIt() {
        HiveTactics h = hive();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 6; i++) units.add(u(i + 1, Kind.ORC, 8, i, 1.0));
        units.add(u(99, Kind.ORC, 3, 0, 0.3));
        Foe near = foe(100, 6, 0, 4.0);
        h.ambush(near, 0);
        Order[] o = h.think(0, units);
        assertEquals(Role.RETREATER, orderOf(units, o, 99).role());
        assertFalse(orderOf(units, o, 99).heal(), "a foe is on top of it");
    }

    // ---------------------------------------------------------------- learning
    @Test
    void learnsWhereTheShotsComeFromAndSendsTheFlankersThere() {
        HiveTactics h = hive();
        List<Unit> units = swarm(18, -12, 0, 5);
        Foe tankSide = foe(100, 20, 0, 4.0), archer = foe(101, 30, 8, 4.0);
        h.sight(1, tankSide, 0);
        h.sight(1, archer, 0);
        h.ambush(tankSide, 0);
        h.think(0, units);
        assertNull(h.markedSpot(0));
        h.hurt(2, 101, 30, 8, 14.0, 20);
        h.hurt(2, 101, 31, 9, 15.0, 30);
        assertNull(h.markedSpot(30), "two shots are not a habit");
        h.hurt(2, 101, 30, 7, 16.0, 40);
        double[] spot = h.markedSpot(40);
        assertNotNull(spot);
        assertEquals(30.3, spot[0], 1.0);
        assertEquals(8.0, spot[1], 1.0);
        h.sight(1, tankSide, 50);
        h.sight(1, archer, 50);
        Order[] o = h.think(50, units);
        int flankers = 0;
        for (Unit un : units) {
            Order each = orderOf(units, o, un.id());
            if (each.role() != Role.FLANKER) continue;
            flankers++;
            assertTrue(dist(each.goalX(), each.goalZ(), spot[0], spot[1]) <= 2.5, "flankers go for the shooter's perch");
            assertEquals(101, each.target(), "and for the shooter");
        }
        assertTrue(flankers >= 2);
        // quiet for long enough and it forgets
        assertNull(h.markedSpot(40 + HiveTactics.SPOT_KEEP + 1));
    }

    @Test
    void closeBlowsAreNotShots() {
        HiveTactics h = hive();
        for (int i = 0; i < 10; i++) h.hurt(1, 100, 10, 0, HiveTactics.RANGED_DIST - 1, i * 10);
        assertNull(h.markedSpot(100));
    }

    @Test
    void shotsFromDifferentPlacesDoNotAddUpToAHabit() {
        HiveTactics h = hive();
        h.hurt(1, 100, 10, 0, 12, 0);
        h.hurt(1, 100, 30, 20, 12, 10);
        h.hurt(1, 100, -10, -20, 12, 20);
        assertNull(h.markedSpot(30));
    }

    // ---------------------------------------------------------------- ground rules
    @Test
    void everyGoalStaysInsideTheArena() {
        double limitA = 46 * 0.92 + 1e-6, limitB = 42 * 0.92 + 1e-6;
        for (double[] foePos : new double[][]{{60, 0}, {-60, 10}, {0, 60}, {0, -60}, {44, 40}, {100, 100}}) {
            HiveTactics h = hive();
            List<Unit> units = swarm(24, 0, 0, 20);
            Order[] o = engaged(h, units, foe(100, foePos[0], foePos[1], 1.0), 0);
            for (Order each : o) {
                double gx = each.goalX(), gz = each.goalZ();
                assertTrue(gx * gx / (limitA * limitA) + gz * gz / (limitB * limitB) <= 1.0 + 1e-9, "goal " + gx + "," + gz + " for a foe at " + foePos[0] + "," + foePos[1]);
            }
        }
    }

    @Test
    void clampKeepsInsidePointsAndPullsOutsideOnes() {
        HiveTactics h = hive();
        assertArrayEquals(new double[]{10, 5}, h.clamp(10, 5), 1e-12);
        double[] c = h.clamp(100, 0);
        assertEquals(46 * 0.92, c[0], 1e-9);
        assertEquals(0.0, c[1], 1e-9);
    }

    @Test
    void noUnitsMeansNoOrdersAndNothingBreaks() {
        HiveTactics h = hive();
        assertEquals(0, h.think(0, List.of()).length);
        h.ambush(foe(100, 5, 5, 1.0), 10);
        assertEquals(0, h.think(20, List.of()).length);
        assertFalse(h.mayStrike(1, 100, 20));
    }

    @Test
    void theSameEventsGiveTheSameOrders() {
        for (int run = 0; run < 3; run++) {
            HiveTactics a = hive(), b = hive();
            List<Unit> units = swarm(25, 5, 5, 9);
            Random r = new Random(40 + run);
            for (long t = 0; t < 1500; t += 10) {
                int kind = r.nextInt(6);
                int id = 1 + r.nextInt(25);
                double x = r.nextDouble() * 60 - 30, z = r.nextDouble() * 50 - 25, d = r.nextDouble() * 20;
                for (HiveTactics each : new HiveTactics[]{a, b}) {
                    if (kind == 0) each.sight(id, foe(100 + id % 3, x, z, 1.5), t);
                    if (kind == 1) each.hurt(id, 100, x, z, d, t);
                    if (kind == 2 && t % 50 == 0) each.ambush(foe(100, x, z, 1.0), t);
                }
                assertArrayEquals(a.think(t, units), b.think(t, units), "tick " + t);
                assertSame(a.state(), b.state());
            }
        }
    }

    // ---------------------------------------------------------------- everything at once
    @Test
    void whateverHappensTheOrdersStayWithinTheRules() {
        for (int run = 0; run < 12; run++) {
            Random r = new Random(1000 + run);
            HiveTactics h = hive();
            List<Unit> units = new ArrayList<>(swarm(10 + r.nextInt(30), r.nextDouble() * 20 - 10, r.nextDouble() * 20 - 10, 8));
            int nextId = 100;
            int changes = 0;
            State last = h.state();
            double limitA = 46 * 0.92 + 1e-6, limitB = 42 * 0.92 + 1e-6;
            for (long t = 0; t < 6000; t += HiveTactics.THINK) {
                int events = r.nextInt(4);
                for (int e = 0; e < events && !units.isEmpty(); e++) {
                    Unit who = units.get(r.nextInt(units.size()));
                    double fx = r.nextDouble() * 70 - 35, fz = r.nextDouble() * 60 - 30;
                    switch (r.nextInt(5)) {
                        case 0 -> h.sight(who.id(), foe(1 + r.nextInt(4), fx, fz, 0.6 + r.nextDouble() * 3.4), t);
                        case 1 -> h.hurt(who.id(), 1 + r.nextInt(4), fx, fz, r.nextDouble() * 25, t);
                        case 2 -> { if (units.size() > 3) { h.died(who.id(), t); units.remove(who); } }
                        case 3 -> { if (r.nextInt(30) == 0) h.ambush(foe(1 + r.nextInt(4), fx, fz, 1.0), t); }
                        default -> { if (units.size() < 45) units.add(u(nextId++, Kind.values()[r.nextInt(5)], fx, fz, 1.0)); }
                    }
                }
                // members move a little, are hurt a little, mend a little
                List<Unit> moved = new ArrayList<>();
                for (Unit un : units) {
                    double hp = Math.max(0.05, Math.min(1.0, un.health() + (r.nextDouble() - 0.55) * 0.15));
                    moved.add(new Unit(un.id(), un.kind(), un.x() + (r.nextDouble() - 0.5) * 3, un.z() + (r.nextDouble() - 0.5) * 3, hp, un.homeX(), un.homeZ()));
                }
                units = moved;
                Order[] o = h.think(t, units);
                assertEquals(units.size(), o.length);
                int tokens = 0;
                for (int i = 0; i < o.length; i++) {
                    Order each = o[i];
                    assertNotNull(each.role());
                    assertNotNull(each.act());
                    assertFalse(Double.isNaN(each.goalX()) || Double.isNaN(each.goalZ()) || Double.isInfinite(each.goalX()), "finite goals");
                    assertTrue(each.goalX() * each.goalX() / (limitA * limitA) + each.goalZ() * each.goalZ() / (limitB * limitB) <= 1.0 + 1e-9, "inside the arena");
                    assertTrue(each.speed() > 0.5 && each.speed() < 1.6);
                    if (each.strike()) {
                        tokens++;
                        assertTrue(h.state() == State.ENGAGE || h.state() == State.SURROUND, "blows only in a fight");
                        assertEquals(Act.ENGAGE, each.act());
                        assertTrue(each.target() >= 0);
                        assertNotEquals(Role.RETREATER, each.role());
                    }
                    if (each.act() == Act.WANDER) assertSame(State.SEARCH, h.state());
                }
                assertTrue(tokens <= HiveTactics.MAX_TOKENS, "tokens: " + tokens);
                if (h.state() != last) {
                    changes++;
                    assertLegal(last, h.state());
                    last = h.state();
                }
                Cue c = h.takeCue();
                if (c != null) assertSame(c.to(), h.state());
            }
            assertTrue(changes < 400, "no endless flapping: " + changes);
        }
    }

    private static void assertLegal(State from, State to) {
        boolean ok = switch (from) {
            case SEARCH -> to == State.ASSEMBLE || to == State.ENGAGE || to == State.SURROUND;
            case ASSEMBLE -> to == State.ENGAGE || to == State.SEARCH || to == State.SURROUND;
            case ENGAGE -> to == State.SURROUND || to == State.FALL_BACK || to == State.SEARCH;
            case SURROUND -> to == State.ENGAGE || to == State.FALL_BACK || to == State.SEARCH;
            case FALL_BACK -> to == State.REFORM || to == State.SEARCH || to == State.ENGAGE;
            case REFORM -> to == State.ASSEMBLE || to == State.SEARCH || to == State.ENGAGE || to == State.SURROUND;
        };
        assertTrue(ok, "a change from " + from + " to " + to + " is not one the hive makes");
    }
}
