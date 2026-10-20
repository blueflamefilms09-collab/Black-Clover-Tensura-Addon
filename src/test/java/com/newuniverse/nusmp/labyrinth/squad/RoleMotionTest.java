package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Ctx;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What each role does in {@link RoleMotion}: the decisions the goals walk to. */
class RoleMotionTest {
    private static final V2 TARGET = new V2(0, 20), WARD = new V2(0, 0), CENTRE = new V2(0, 0);

    private static Ctx ctx(SquadRole role, int slot, V2 self, V2 target, V2 ward, boolean engaged, int since, int waited, V2 threat, boolean flip, double hp) {
        return new Ctx(role, slot, self, hp, target, 0.0, ward, CENTRE, 0.0, new V2(0, 0), new SquadFormation.Offset(-4.6, 0), engaged, since, waited, threat, flip, 2.0);
    }

    @Test
    void aTankSteps1_5BlocksInFrontOfTheWardThenHolds() {
        Ctx far = ctx(SquadRole.TANK, 0, new V2(8, 4), TARGET, WARD, false, 0, 0, null, false, 1.0);
        Move m = RoleMotion.decide(far).orElseThrow();
        assertEquals(Kind.INTERCEPT, m.kind());
        assertEquals(1.5, m.to().dist(WARD), 1e-9);
        assertEquals(0.0, SquadGeometry.distToSegment(m.to(), WARD, TARGET), 1e-9);
        Ctx there = ctx(SquadRole.TANK, 0, new V2(0.3, 1.6), TARGET, WARD, true, 5, 0, null, false, 1.0);
        assertEquals(Kind.HOLD, RoleMotion.decide(there).orElseThrow().kind(), "already between the two: hold");
        Ctx noWard = ctx(SquadRole.TANK, 0, new V2(8, 4), TARGET, null, false, 0, 0, null, false, 1.0);
        assertFalse(RoleMotion.decide(noWard).isPresent(), "nobody to cover: the tank's own goals act");
    }

    @Test
    void aLeaderOnlyCoversWhenThereIsAWard() {
        Ctx c = ctx(SquadRole.LEADER, 0, new V2(8, 4), TARGET, null, false, 0, 0, null, false, 1.0);
        assertFalse(RoleMotion.decide(c).isPresent());
    }

    @Test
    void aFlankerWaitsForTheTankThenCirclesToTheBack() {
        V2 self = new V2(6, 8);
        Ctx early = ctx(SquadRole.FLANKER, 0, self, TARGET, WARD, false, 0, 10, null, false, 1.0);
        Optional<Move> m0 = RoleMotion.decide(early);
        assertTrue(m0.isEmpty() || m0.get().kind() == Kind.HOLD, "no tank has engaged: it does not commit");
        Ctx waiting = ctx(SquadRole.FLANKER, 0, new V2(0, 14), TARGET, WARD, false, 0, 10, null, false, 1.0);
        assertEquals(Kind.HOLD, RoleMotion.decide(waiting).orElseThrow().kind(), "close to the target and the tank is not there: hold back");
        Ctx go = ctx(SquadRole.FLANKER, 0, self, TARGET, WARD, true, 20, 40, null, false, 1.0);
        Move m = RoleMotion.decide(go).orElseThrow();
        assertEquals(Kind.FLANK, m.kind());
        double off = SquadGeometry.offFacing(TARGET, 0.0, m.to());
        assertTrue(off >= 110.0 - 1e-6 && off <= 150.0 + 1e-6, "off the facing: " + off);
    }

    @Test
    void flankersAreStaggeredBySlot() {
        V2 self = new V2(6, 8);
        Ctx first = ctx(SquadRole.FLANKER, 0, self, TARGET, WARD, true, 0, 0, null, false, 1.0);
        Ctx third = ctx(SquadRole.FLANKER, 3, self, TARGET, WARD, true, 0, 0, null, false, 1.0);
        assertEquals(Kind.FLANK, RoleMotion.decide(first).orElseThrow().kind(), "slot 0 commits the moment the tank engages");
        assertFalse(RoleMotion.decide(third).filter(m -> m.kind() == Kind.FLANK).isPresent(), "slot 3 waits its turn");
        Ctx later = ctx(SquadRole.FLANKER, 3, self, TARGET, WARD, true, 3 * RoleMotion.STAGGER, 0, null, false, 1.0);
        assertEquals(Kind.FLANK, RoleMotion.decide(later).orElseThrow().kind());
        Ctx impatient = ctx(SquadRole.FLANKER, 3, self, TARGET, WARD, false, 0, RoleMotion.MAX_WAIT, null, false, 1.0);
        assertEquals(Kind.FLANK, RoleMotion.decide(impatient).orElseThrow().kind(), "a flanker does not wait for ever");
    }

    @Test
    void aFlankerAbortsWhenSeenFromCloseOrHurt() {
        V2 inFront = new V2(0, 25.0);                       // the target at (0,20) looks along +z: 5 blocks ahead of it
        Ctx target = new Ctx(SquadRole.FLANKER, 0, new V2(0, 25), 1.0, TARGET, 0.0, WARD, CENTRE, 0.0, null, null, true, 50, 50, null, false, 2.0);
        assertEquals(Kind.ABORT, RoleMotion.decide(target).orElseThrow().kind());
        Ctx hurt = ctx(SquadRole.FLANKER, 0, new V2(8, 8), TARGET, WARD, true, 50, 50, null, false, 0.2);
        assertEquals(Kind.ABORT, RoleMotion.decide(hurt).orElseThrow().kind());
        assertTrue(inFront.dist(TARGET) < 8);
    }

    @Test
    void aFlankerCloseEnoughToStrikeLeavesItToTheAttackGoal() {
        Ctx c = ctx(SquadRole.FLANKER, 0, new V2(2.0, 19.0), TARGET, WARD, true, 50, 50, null, false, 1.0);
        assertFalse(RoleMotion.decide(c).filter(m -> m.kind() == Kind.FLANK).isPresent());
    }

    @Test
    void halfTheMeleeDamageDealersFlank() {
        assertFalse(RoleMotion.flanks(SquadRole.DPS_MELEE, 0));
        assertTrue(RoleMotion.flanks(SquadRole.DPS_MELEE, 1));
        assertTrue(RoleMotion.flanks(SquadRole.FLANKER, 4));
        assertFalse(RoleMotion.flanks(SquadRole.TANK, 1));
        Ctx c = ctx(SquadRole.DPS_MELEE, 2, new V2(6, 8), TARGET, WARD, true, 100, 100, null, false, 1.0);
        assertFalse(RoleMotion.decide(c).isPresent(), "a front-line melee member is left to its own goals");
    }

    @Test
    void aSupportKeeps8To12BlocksFromTheThreat() {
        V2 threat = new V2(0, 0);
        Ctx close = new Ctx(SquadRole.SUPPORT, 5, new V2(0, -4), 1.0, threat, 0.0, WARD, new V2(0, -20), 0.0, null, null, true, 0, 0, threat, false, 2.0);
        Move m = RoleMotion.decide(close).orElseThrow();
        assertEquals(Kind.RETREAT, m.kind());
        assertEquals(10.0, m.to().dist(threat), 1e-6);
        Ctx fine = new Ctx(SquadRole.SUPPORT, 5, new V2(0, -10), 1.0, threat, 0.0, WARD, new V2(0, -20), 0.0, null, null, true, 0, 0, threat, false, 2.0);
        assertEquals(Kind.HOLD, RoleMotion.decide(fine).orElseThrow().kind());
    }

    @Test
    void aShooterBacksOffOnlyOnceTheLineIsFormed() {
        V2 target = new V2(0, 0);
        Ctx formed = new Ctx(SquadRole.DPS_RANGED, 4, new V2(0, -3), 1.0, target, 0.0, WARD, new V2(0, -10), 0.0, null, null, true, 5, 0, null, false, 2.0);
        assertEquals(Kind.RETREAT, RoleMotion.decide(formed).orElseThrow().kind());
        Ctx unformed = new Ctx(SquadRole.DPS_RANGED, 4, new V2(0, -3), 1.0, target, 0.0, WARD, new V2(0, -10), 0.0, null, null, false, 0, 0, null, false, 2.0);
        assertFalse(RoleMotion.decide(unformed).isPresent());
    }

    @Test
    void withNoTargetFollowersKeepTheirSlotBehindTheLeader() {
        V2 leader = new V2(100, 100);
        SquadFormation.Offset slot = new SquadFormation.Offset(-4.6, 0);
        Ctx away = new Ctx(SquadRole.SUPPORT, 5, new V2(80, 80), 1.0, null, 0.0, null, leader, 0.0, leader, slot, false, 0, 0, null, false, 2.0);
        Move m = RoleMotion.decide(away).orElseThrow();
        assertEquals(Kind.FORM, m.kind());
        assertEquals(leader.x(), m.to().x(), 1e-9);
        assertEquals(leader.z() - 4.6, m.to().z(), 1e-9, "facing south, four and a half blocks behind");
        Ctx home = new Ctx(SquadRole.SUPPORT, 5, new V2(100.5, 95.7), 1.0, null, 0.0, null, leader, 0.0, leader, slot, false, 0, 0, null, false, 2.0);
        assertEquals(Kind.HOLD, RoleMotion.decide(home).orElseThrow().kind());
        Ctx lead = new Ctx(SquadRole.LEADER, 0, leader, 1.0, null, 0.0, null, leader, 0.0, leader, slot, false, 0, 0, null, false, 2.0);
        assertFalse(RoleMotion.decide(lead).isPresent(), "the leader is walked by the Director");
    }
}
