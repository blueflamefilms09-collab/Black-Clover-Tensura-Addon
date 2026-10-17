package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.enc.WandererMind.Event;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Heard;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Sighting;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the boss's brain is a state machine with no Minecraft in it precisely so that its promises can be tested: hiding works, a chase ends in the right way
 * and is reported once, a sound is investigated and a sighting interrupts it, a stuck walk is given up, a blow is answered. These tests drive it with a
 * simulated world and pin each of those.
 */
class WandererMindTest {
    private static final UUID P1 = new UUID(0, 1), P2 = new UUID(0, 2);
    private static final WandererRules.Tuning T = WandererRules.of(24, 40, 160);

    /** Five rooms in a row at x = 0, 20 ... 80 and a safe room (id 9) hanging off the middle one. */
    private static PatrolGraph graph() {
        List<PatrolGraph.Node> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) nodes.add(new PatrolGraph.Node(i, i * 20.0, 65, 0, true, false));
        nodes.add(new PatrolGraph.Node(9, 40, 65, 30, false, false));
        return PatrolGraph.build(nodes, List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3}, new int[] {3, 4}, new int[] {2, 9}));
    }

    /** A little world: where the boss stands, what time it is, and the boss's mind. */
    private static final class Sim {
        final WandererMind mind = new WandererMind();
        final PatrolGraph g;
        Random r = new Random(1);
        long now = 1000;
        double x, y = 65, z;
        boolean navDone;

        Sim() { this(graph()); }

        Sim(PatrolGraph g) { this.g = g; }

        WandererMind.Order step(List<Sighting> seen, Heard heard) { return step(seen, heard, null); }

        WandererMind.Order step(List<Sighting> seen, Heard heard, UUID hurtBy) {
            WandererMind.Order o = mind.step(new WandererMind.Input(now, x, y, z, navDone, seen, heard, hurtBy), T, g, r);
            now += 10;
            return o;
        }

        WandererMind.Order step() { return step(List.of(), null); }

        /** Teleports the boss onto its goal, as a walk that succeeded would. */
        void walkTo(WandererMind.Order o) {
            x = o.gx();
            y = o.gy();
            z = o.gz();
        }
    }

    private static Sighting seen(UUID id, double x, double z, double dist, boolean los, boolean sneaking, boolean dark, boolean still, boolean safe) {
        return new Sighting(id, x, 65, z, dist, los, sneaking, dark, still, safe);
    }

    private static Sighting plain(UUID id, double dist) { return seen(id, dist, 0, dist, true, false, false, false, false); }

    @Test
    void aFreshBossPatrolsToANeighbouringRoom() {
        Sim s = new Sim();
        WandererMind.Order o = s.step();
        assertEquals(State.PATROL, o.state());
        assertTrue(o.move());
        assertNull(o.chase());
        assertEquals(Event.NONE, o.event());
        assertEquals(WandererMind.PATROL_SPEED, o.speed(), 0.0);
        assertTrue(s.mind.hasGoal());
        assertFalse(s.mind.goalNode() == 9, "the safe room is never a goal");
    }

    @Test
    void thePatrolWalksOnAfterEachArrivalAndNeverEntersTheSafeRoom() {
        Sim s = new Sim();
        s.x = 40;
        java.util.Set<Integer> goals = new java.util.HashSet<>();
        for (int i = 0; i < 300; i++) {
            WandererMind.Order o = s.step();
            assertEquals(State.PATROL, o.state());
            assertTrue(o.move());
            goals.add(s.mind.goalNode());
            s.walkTo(o);
        }
        assertFalse(goals.contains(9));
        assertEquals(java.util.Set.of(0, 1, 2, 3, 4), goals, "the walk covers every room it may enter");
    }

    @Test
    void aPlayerInTheOpenIsChasedAndTheOrderNamesThem() {
        Sim s = new Sim();
        s.step();
        WandererMind.Order o = s.step(List.of(plain(P1, 15)), null);
        assertEquals(State.CHASE, o.state());
        assertEquals(P1, o.chase());
        assertEquals(Event.SPOTTED, o.event());
        assertEquals(P1, o.eventPlayer());
        assertEquals(WandererMind.CHASE_SPEED, o.speed(), 0.0);
        assertEquals(P1, s.mind.approvedTarget());
        WandererMind.Order again = s.step(List.of(plain(P1, 14)), null);
        assertEquals(Event.NONE, again.event(), "the chase is not announced twice");
        assertEquals(P1, again.chase());
    }

    @Test
    void aCrouchingPlayerInTheDarkStandingStillIsLeftAlone() {
        Sim s = new Sim();
        for (int i = 0; i < 50; i++) {
            WandererMind.Order o = s.step(List.of(seen(P1, 8, 0, 8, true, true, true, true, false)), null);
            assertEquals(State.PATROL, o.state());
            assertNull(o.chase());
            s.walkTo(o);
            s.x = Math.min(s.x, 30);
        }
    }

    @Test
    void aPlayerBehindAWallIsNotSeenEvenCloseBy() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(seen(P1, 5, 0, 5, false, false, false, false, false)), null);
        assertEquals(State.PATROL, o.state());
        assertNull(s.mind.approvedTarget());
    }

    @Test
    void aPlayerInASafeRoomIsNeverChased() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(seen(P1, 3, 0, 3, true, false, false, false, true)), null);
        assertEquals(State.PATROL, o.state());
    }

    @Test
    void theNearestNoticedPlayerIsTheTarget() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(plain(P1, 20), plain(P2, 9)), null);
        assertEquals(P2, o.chase());
    }

    @Test
    void aHiddenTargetIsFollowedOnlyToWhereItWasLastSeen() {
        Sim s = new Sim();
        s.step(List.of(seen(P1, 18, 0, 18, true, false, false, false, false)), null);
        // the player vanishes behind a door: the boss walks to the last seen spot, with no target of its own
        WandererMind.Order o = s.step(List.of(seen(P1, 60, 0, 60, false, false, false, false, false)), null);
        assertEquals(State.CHASE, o.state());
        assertNull(o.chase(), "it no longer perceives the player, so it must not hunt them");
        assertEquals(18.0, o.gx(), 0.0);
        assertEquals(0.0, o.gz(), 0.0);
        assertEquals(P1, s.mind.approvedTarget(), "it is still in the chase state, but the order carries no target");
    }

    @Test
    void aChaseLostForTheLoseTimeEndsInASearchThenAPatrol() {
        Sim s = new Sim();
        s.step(List.of(seen(P1, 18, 0, 18, true, false, false, false, false)), null);
        WandererMind.Order o = null;
        Event end = Event.NONE;
        UUID who = null;
        long chased = 0;
        for (int i = 0; i < 30 && end == Event.NONE; i++) {
            o = s.step(List.of(seen(P1, 60, 0, 60, false, false, false, false, false)), null);
            end = o.event();
            who = o.eventPlayer();
            chased = o.chaseTicks();
        }
        assertEquals(Event.LOST, end);
        assertEquals(P1, who);
        assertTrue(chased >= T.loseTicks(), "chased for " + chased);
        assertEquals(State.SEARCH, o.state());
        assertNull(o.chase());
        assertNull(s.mind.target());
        // the search runs for its time and then the patrol resumes
        int searchPasses = 0;
        while (o.state() == State.SEARCH && searchPasses < 200) {
            s.walkTo(o);
            o = s.step();
            searchPasses++;
        }
        assertEquals(State.PATROL, o.state());
        assertTrue(searchPasses * 10 >= T.searchTicks() - 20, "searched for " + searchPasses * 10 + " ticks");
    }

    @Test
    void aTargetThatReachesASafeRoomEndsTheChaseAtTheDoor() {
        Sim s = new Sim();
        s.step(List.of(seen(P1, 38, 0, 18, true, false, false, false, false)), null);
        WandererMind.Order o = s.step(List.of(seen(P1, 40, 30, 20, true, false, false, false, true)), null);
        assertEquals(Event.REFUGE, o.event());
        assertEquals(P1, o.eventPlayer());
        assertEquals(State.SEARCH, o.state());
        assertNull(o.chase());
        assertTrue(o.chaseTicks() >= 0);
        assertFalse(s.mind.goalNode() == 9, "it searches beside the safe room, never in it");
        assertTrue(s.mind.goalNode() == 2, "the room nearest the door");
    }

    @Test
    void aTargetThatIsGoneEndsTheChaseWithoutBlame() {
        Sim s = new Sim();
        s.step(List.of(plain(P1, 12)), null);
        WandererMind.Order o = s.step(List.of(), null);
        assertEquals(Event.GAVE_UP, o.event());
        assertEquals(P1, o.eventPlayer());
        assertEquals(State.SEARCH, o.state());
    }

    @Test
    void whileTheTargetIsHiddenAnotherVisiblePlayerTakesOverTheChase() {
        Sim s = new Sim();
        s.step(List.of(plain(P1, 12)), null);
        WandererMind.Order o = s.step(List.of(seen(P1, 70, 0, 70, false, false, false, false, false), plain(P2, 10)), null);
        assertEquals(Event.SPOTTED, o.event());
        assertEquals(P2, o.chase());
        assertEquals(P2, s.mind.target());
    }

    @Test
    void aTargetThatIsStillPerceivedIsKeptEvenWhenAnotherIsCloser() {
        Sim s = new Sim();
        s.step(List.of(plain(P1, 12)), null);
        WandererMind.Order o = s.step(List.of(plain(P1, 12), plain(P2, 5)), null);
        assertEquals(P1, o.chase());
        assertEquals(Event.NONE, o.event());
    }

    @Test
    void aLoudSoundSendsItToLookThenSearchThenPatrol() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(), new Heard(70, 65, 0, 40, 6.0));
        assertEquals(Event.HEARD, o.event());
        assertEquals(State.INVESTIGATE, o.state());
        assertTrue(o.move());
        assertEquals(70.0, o.gx(), 0.0);
        assertEquals(WandererMind.INVESTIGATE_SPEED, o.speed(), 0.0);
        s.walkTo(o);
        o = s.step();
        assertEquals(State.SEARCH, o.state(), "it arrived and now looks round");
        int passes = 0;
        while (o.state() == State.SEARCH && passes < 200) {
            s.walkTo(o);
            o = s.step();
            passes++;
        }
        assertEquals(State.PATROL, o.state());
    }

    @Test
    void aSoundDoesNotInterruptAChaseButASightingInterruptsAnInvestigation() {
        Sim s = new Sim();
        s.step(List.of(), new Heard(70, 65, 0, 40, 6.0));
        WandererMind.Order o = s.step(List.of(plain(P1, 10)), null);
        assertEquals(State.CHASE, o.state());
        WandererMind.Order c = s.step(List.of(plain(P1, 10)), new Heard(0, 65, 0, 30, 6.0));
        assertEquals(State.CHASE, c.state());
        assertEquals(Event.NONE, c.event());
    }

    @Test
    void aLouderSoundRedirectsAnInvestigationButAQuieterOneDoesNot() {
        Sim s = new Sim();
        s.step(List.of(), new Heard(70, 65, 0, 40, 3.0));
        WandererMind.Order same = s.step(List.of(), new Heard(0, 65, 0, 40, 3.5));
        assertEquals(Event.NONE, same.event());
        assertEquals(70.0, same.gx(), 0.0);
        WandererMind.Order louder = s.step(List.of(), new Heard(0, 65, 0, 40, 6.0));
        assertEquals(Event.HEARD, louder.event());
        assertEquals(0.0, louder.gx(), 0.0);
    }

    @Test
    void aBossThatMakesNoProgressSetsTheGoalAsideAndTriesTheOtherWay() {
        Sim s = new Sim();
        s.x = 40;
        s.step();
        int first = s.mind.goalNode();
        assertTrue(first == 1 || first == 3, "the neighbours of the middle room: " + first);
        int changedAt = -1;
        for (int i = 1; i <= 15 && changedAt < 0; i++) {
            s.step();
            if (s.mind.goalNode() != first) changedAt = i;
        }
        assertTrue(changedAt > 0 && changedAt <= 12, "the stuck rule fires after about four seconds, it fired at pass " + changedAt);
        assertEquals(first == 1 ? 3 : 1, s.mind.goalNode(), "the room it failed to reach is avoided while another way exists");
    }

    @Test
    void aBossThatKeepsWalkingIsNeverCalledStuck() {
        Sim s = new Sim();
        s.x = 40;
        s.step();
        int goal = s.mind.goalNode();
        double gx = goal * 20.0;
        for (int i = 0; i < 6; i++) {
            s.x += Math.signum(gx - s.x) * 2.0;                 // two blocks every half second, towards a goal 20 blocks away
            s.step();
            assertEquals(goal, s.mind.goalNode(), "still on its way at pass " + i);
        }
    }

    @Test
    void aWalkThatEndedShortOfTheGoalCountsAsStuckAtOnce() {
        Sim s = new Sim();
        s.x = 40;
        s.step();
        int first = s.mind.goalNode();
        s.now += 100;
        s.navDone = true;
        s.step();
        s.navDone = false;
        assertTrue(s.mind.hasGoal());
        assertFalse(s.mind.goalNode() == first, "the walk ended 20 blocks short, so the goal was set aside");
    }

    @Test
    void aBossInAGraphWithNoRoomToWalkHasNoGoalButDoesNotFail() {
        WandererMind m = new WandererMind();
        PatrolGraph none = PatrolGraph.build(List.of(), List.of());
        WandererMind.Order o = m.step(new WandererMind.Input(0, 0, 65, 0, false, List.of(), null, null), T, none, new Random(1));
        assertFalse(o.move());
        assertEquals(State.PATROL, o.state());
    }

    @Test
    void anEventIsReportedOncePerChangeOnly() {
        Sim s = new Sim();
        int spotted = 0, lost = 0;
        for (int i = 0; i < 60; i++) {
            List<Sighting> view = i < 3 ? List.of(plain(P1, 12)) : List.of(seen(P1, 80, 0, 80, false, false, false, false, false));
            WandererMind.Order o = s.step(view, null);
            if (o.event() == Event.SPOTTED) spotted++;
            if (o.event() == Event.LOST) lost++;
        }
        assertEquals(1, spotted);
        assertEquals(1, lost);
    }

    @Test
    void aBossThatIsStruckChasesTheAttackerEvenWithoutSeeingThem() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(seen(P1, 30, 0, 30, false, false, false, false, false)), null, P1);
        assertEquals(State.CHASE, o.state());
        assertEquals(Event.SPOTTED, o.event());
        assertNull(o.chase(), "it cannot see them, so it walks to where the blow came from instead of hunting");
        assertEquals(30.0, o.gx(), 0.0);
    }

    @Test
    void aBossThatIsStruckFromASafeRoomDoesNothing() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(seen(P1, 30, 0, 30, true, false, false, false, true)), null, P1);
        assertEquals(State.PATROL, o.state());
    }

    @Test
    void aStrikeFromSomeoneWhoIsNotInTheSectorIsIgnored() {
        Sim s = new Sim();
        WandererMind.Order o = s.step(List.of(), null, P1);
        assertEquals(State.PATROL, o.state());
    }

    @Test
    void theRoomItFailedToReachIsAvoidedWhateverTheDice() {
        // the blocked room is remembered, so the other way is taken for every seed, not only for a lucky one
        for (int seed = 1; seed <= 60; seed++) {
            Sim s = new Sim();
            s.r = new Random(seed);
            s.x = 40;
            s.step();
            int first = s.mind.goalNode();
            assertTrue(first == 1 || first == 3, "seed " + seed + ": " + first);
            int changedAt = -1;
            for (int i = 1; i <= 15 && changedAt < 0; i++) {
                s.step();
                if (s.mind.goalNode() != first) changedAt = i;
            }
            assertTrue(changedAt > 0, "seed " + seed + ": the stuck goal was chosen again");
            assertEquals(first == 1 ? 3 : 1, s.mind.goalNode(), "seed " + seed);
        }
    }

    @Test
    void aPatrolRemembersWhereItHasBeenAndSeldomTurnsBack() {
        // a ring of eight rooms and a walker that always arrives: arrivals are what fills its memory
        List<PatrolGraph.Node> nodes = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            nodes.add(new PatrolGraph.Node(i, Math.cos(i * Math.PI / 4) * 60, 65, Math.sin(i * Math.PI / 4) * 60, true, false));
            edges.add(new int[] {i, (i + 1) % 8});
        }
        Sim s = new Sim(PatrolGraph.build(nodes, edges));
        s.r = new Random(3);
        s.x = 60;
        s.z = 0;
        List<Integer> goals = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            WandererMind.Order o = s.step();
            goals.add(s.mind.goalNode());
            s.walkTo(o);
        }
        int back = 0;
        for (int i = 2; i < goals.size(); i++) if (goals.get(i).equals(goals.get(i - 2))) back++;
        assertTrue(back < goals.size() * 0.25, "turned straight back " + back + " times in " + goals.size() + " goals");
        assertTrue(new java.util.HashSet<>(goals).size() == 8, "and still covered the whole ring");
    }

    @Test
    void anInvestigationThatKeepsWalkingStillGivesUpAfterItsTime() {
        Sim s = new Sim();
        s.step();
        long began = s.now;
        WandererMind.Order o = s.step(List.of(), new Heard(1000.0, 65, 0, 1000.0, 8.0));
        assertEquals(State.INVESTIGATE, o.state());
        long endedAt = -1;
        for (int i = 0; i < 60 && endedAt < 0; i++) {
            s.x += 2.0;                                        // always making progress, never arriving: the sound is a thousand blocks away
            WandererMind.Order p = s.step();
            if (p.state() != State.INVESTIGATE) endedAt = s.now - 10 - began;
        }
        assertTrue(endedAt > 0, "the investigation never ended");
        assertTrue(endedAt >= 350 && endedAt <= 450, "it should last about twenty seconds, it lasted " + endedAt + " ticks");
    }
}
