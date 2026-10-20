package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.RoleFit.Traits;
import com.newuniverse.nusmp.labyrinth.squad.SquadFormation.Offset;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.Cause;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.Mode;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.Reaction;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.ThreatView;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.WardView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ward, the focus target, the morale and the role fit. */
class SquadMindTest {
    @Test
    void theWardIsTheSoftestMemberAndNeverATankOrTheTankItself() {
        List<WardView> v = List.of(new WardView(0, SquadRole.TANK, 1.0), new WardView(1, SquadRole.DPS_MELEE, 0.2), new WardView(2, SquadRole.DPS_RANGED, 1.0),
                new WardView(3, SquadRole.SUPPORT, 1.0), new WardView(4, SquadRole.TANK, 0.1));
        assertEquals(3, SquadMind.chooseWard(v, 0), "the healer comes before the shooter and the hurt brawler");
        List<WardView> noHealer = List.of(new WardView(0, SquadRole.TANK, 1.0), new WardView(1, SquadRole.DPS_MELEE, 0.2), new WardView(2, SquadRole.DPS_RANGED, 1.0));
        assertEquals(2, SquadMind.chooseWard(noHealer, 0), "a full-health shooter is softer than a hurt brawler");
        assertEquals(-1, SquadMind.chooseWard(List.of(new WardView(0, SquadRole.TANK, 1.0), new WardView(1, SquadRole.LEADER, 1.0)), 0));
        assertEquals(-1, SquadMind.chooseWard(List.of(), 0));
        List<WardView> two = List.of(new WardView(0, SquadRole.SUPPORT, 0.9), new WardView(1, SquadRole.SUPPORT, 0.4));
        assertEquals(1, SquadMind.chooseWard(two, 7), "inside a class the hurt one");
        assertEquals(1, SquadMind.chooseWard(two, 0), "never itself");
    }

    @Test
    void focusFirePicksOneTargetForEveryone() {
        List<ThreatView> t = List.of(new ThreatView(10, 12, 12, 1.0, false, true), new ThreatView(11, 6, 6, 1.0, false, true), new ThreatView(12, 20, 20, 0.2, false, false));
        assertEquals(1, SquadMind.focusTarget(t), "the nearest");
        List<ThreatView> taunt = List.of(new ThreatView(10, 12, 12, 1.0, true, false), new ThreatView(11, 6, 6, 1.0, false, true));
        assertEquals(0, SquadMind.focusTarget(taunt), "whoever hurt a squad member comes first");
        List<ThreatView> hurt = List.of(new ThreatView(10, 10, 10, 1.0, false, true), new ThreatView(11, 10, 10, 0.1, false, true));
        assertEquals(1, SquadMind.focusTarget(hurt), "finish the hurt one");
        List<ThreatView> tie = List.of(new ThreatView(20, 10, 10, 1.0, false, true), new ThreatView(19, 10, 10, 1.0, false, true));
        assertEquals(1, SquadMind.focusTarget(tie), "a tie goes to the lowest id, so every member agrees");
        assertEquals(-1, SquadMind.focusTarget(List.of()));
    }

    @Test
    void moraleBreaksOnTheLeaderOnTheLastManOrOnLosses() {
        assertEquals(Cause.NONE, SquadMind.check(6, 6, true, true, 0.34));
        assertEquals(Cause.LEADER_DOWN, SquadMind.check(6, 5, true, false, 0.34));
        assertEquals(Cause.LAST_STANDING, SquadMind.check(6, 1, false, false, 0.34));
        assertEquals(Cause.LOSSES, SquadMind.check(8, 2, false, false, 0.34));
        assertEquals(Cause.NONE, SquadMind.check(8, 3, false, false, 0.34));
        assertEquals(Cause.NONE, SquadMind.check(8, 0, true, false, 0.34), "a dead squad has nothing left to break");
        assertEquals(Cause.NONE, SquadMind.check(1, 1, true, true, 0.34));
        assertEquals(Cause.NONE, SquadMind.check(6, 3, false, false, 0.0), "a fraction of 0 never breaks on losses");
    }

    @Test
    void survivorsFleeOrEnrageByTheModeAndTheirRole() {
        assertEquals(Reaction.FLEE, SquadMind.react(Mode.FLEE, SquadRole.TANK, 4));
        assertEquals(Reaction.ENRAGE, SquadMind.react(Mode.ENRAGE, SquadRole.SUPPORT, 4));
        assertEquals(Reaction.ENRAGE, SquadMind.react(Mode.MIXED, SquadRole.TANK, 4));
        assertEquals(Reaction.ENRAGE, SquadMind.react(Mode.MIXED, SquadRole.FLANKER, 4));
        assertEquals(Reaction.FLEE, SquadMind.react(Mode.MIXED, SquadRole.SUPPORT, 4));
        assertEquals(Reaction.FLEE, SquadMind.react(Mode.MIXED, SquadRole.DPS_RANGED, 4));
        assertEquals(Reaction.FLEE, SquadMind.react(Mode.ENRAGE, SquadRole.TANK, 1), "the last one standing runs");
        assertEquals(Mode.MIXED, Mode.of(null));
        assertEquals(Mode.MIXED, Mode.of("whatever"));
        assertEquals(Mode.FLEE, Mode.of(" Flee "));
        assertEquals(Mode.ENRAGE, Mode.of("ENRAGE"));
    }

    @Test
    void rolesGoToTheMobsThatFitThem() {
        List<Traits> mobs = List.of(new Traits(20, 0, 0.3, 3, false, false, ""),       // 0 a quick light body
                new Traits(90, 8, 0.2, 5, false, false, ""),                           // 1 a big armoured body
                new Traits(18, 0, 0.25, 4, true, false, ""),                           // 2 an archer
                new Traits(16, 0, 0.25, 2, false, true, ""),                           // 3 a caster
                new Traits(40, 2, 0.23, 7, false, false, ""));                         // 4 a hard hitter
        SquadRole[] r = RoleFit.assign(List.of(SquadRole.TANK, SquadRole.SUPPORT, SquadRole.DPS_RANGED, SquadRole.FLANKER, SquadRole.DPS_MELEE), mobs);
        assertEquals(SquadRole.TANK, r[1]);
        assertEquals(SquadRole.SUPPORT, r[3]);
        assertEquals(SquadRole.DPS_RANGED, r[2]);
        assertEquals(SquadRole.FLANKER, r[0]);
        assertEquals(SquadRole.DPS_MELEE, r[4]);
    }

    @Test
    void aFoundryArchetypeSaysWhatItIs() {
        List<Traits> mobs = new ArrayList<>();
        for (String a : new String[]{"brute", "skirmisher", "archer", "support", "tank", "assassin"}) mobs.add(new Traits(30, 0, 0.25, 4, a.equals("archer"), false, a));
        SquadRole[] r = RoleFit.assign(List.of(SquadRole.TANK, SquadRole.SUPPORT, SquadRole.DPS_RANGED, SquadRole.FLANKER, SquadRole.FLANKER, SquadRole.DPS_MELEE), mobs);
        assertEquals(SquadRole.TANK, r[4]);
        assertEquals(SquadRole.SUPPORT, r[3]);
        assertEquals(SquadRole.DPS_RANGED, r[2]);
        assertTrue(r[1] == SquadRole.FLANKER && r[5] == SquadRole.FLANKER, "the skirmisher and the assassin flank");
        assertEquals(SquadRole.DPS_MELEE, r[0]);
    }

    @Test
    void extraMobsFightAsMeleeAndEveryMobGetsARole() {
        List<Traits> mobs = new ArrayList<>();
        for (int i = 0; i < 8; i++) mobs.add(new Traits(20 + i, 0, 0.25, 3, false, false, ""));
        SquadRole[] r = RoleFit.assign(List.of(SquadRole.TANK, SquadRole.SUPPORT), mobs);
        assertEquals(8, r.length);
        int melee = 0;
        for (SquadRole x : r) {
            assertTrue(x != null);
            if (x == SquadRole.DPS_MELEE) melee++;
        }
        assertEquals(6, melee);
    }

    @Test
    void formationHasTheTanksInFrontAndTheHealerBehindWithNoTwoOnOneSpot() {
        for (long seed = 0; seed < 200; seed++) {
            SquadPlan.Plan p = SquadPlan.plan(seed, 20 + (int) seed, 1 + (int) (seed % 5), (int) (seed % 4), "ogre", "");
            List<Offset> o = SquadFormation.offsets(p.roles(), seed);
            assertEquals(p.size(), o.size());
            for (int i = 0; i < o.size(); i++) {
                for (int j = i + 1; j < o.size(); j++) {
                    double d = Math.hypot(o.get(i).forward() - o.get(j).forward(), o.get(i).lateral() - o.get(j).lateral());
                    assertTrue(d >= SquadFormation.MIN_GAP - 0.05, "slots " + i + " and " + j + " are " + d + " apart: " + p.describe());
                }
            }
            double tank = mean(p.roles(), o, SquadRole.TANK), sup = mean(p.roles(), o, SquadRole.SUPPORT), rng = mean(p.roles(), o, SquadRole.DPS_RANGED);
            assertTrue(tank > rng && rng > sup, "tanks " + tank + " ranged " + rng + " support " + sup);
            assertEquals(o, SquadFormation.offsets(p.roles(), seed), "deterministic");
        }
    }

    private static double mean(List<SquadRole> roles, List<Offset> o, SquadRole r) {
        double s = 0;
        int n = 0;
        for (int i = 0; i < roles.size(); i++) if (roles.get(i) == r) {
            s += o.get(i).forward();
            n++;
        }
        return n == 0 ? 0 : s / n;
    }
}
