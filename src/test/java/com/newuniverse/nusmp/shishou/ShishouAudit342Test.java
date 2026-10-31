package com.newuniverse.nusmp.shishou;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 3.42.0 Shishou audit: the give-up memory, the best-three selection, the follow speed, and the source rules the fixes rest on (read from the repo's files). */
class ShishouAudit342Test {
    private static String code(String file) throws IOException {
        String s = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/shishou/" + file));
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    // ---------------------------------------------------------------- the give-up memory
    @Test
    void severalCreaturesAreGivenUpAtOnce() {
        ShishouIgnore g = new ShishouIgnore();
        g.add(10, 200, 0);
        g.add(11, 200, 0);
        assertTrue(g.ignored(10, 100) && g.ignored(11, 100), "giving up B does not forget A (the old single slot did)");
        assertEquals(2, g.active(100));
        assertFalse(g.ignored(12, 100));
    }

    @Test
    void thePauseEndsAndTheSlotIsReused() {
        ShishouIgnore g = new ShishouIgnore();
        g.add(1, 50, 0);
        assertTrue(g.ignored(1, 49));
        assertFalse(g.ignored(1, 50), "over at the stated time");
        for (int i = 0; i < ShishouIgnore.SLOTS; i++) g.add(100 + i, 1000, 60);
        assertEquals(ShishouIgnore.SLOTS, g.active(61), "the finished entry made room");
    }

    @Test
    void theSameCreatureExtendsAndAFullTableDropsTheSoonestEnding() {
        ShishouIgnore g = new ShishouIgnore();
        g.add(1, 100, 0);
        g.add(1, 500, 10);
        assertEquals(1, g.active(20), "the same id takes one slot");
        assertTrue(g.ignored(1, 400), "and its pause was extended");
        g.add(2, 300, 0);
        g.add(3, 200, 0);
        g.add(4, 150, 0);
        g.add(5, 900, 0);                                                                      // full: 2 (300), 3 (200), 4 (150), 1 (500): 4 ends first
        assertFalse(g.ignored(4, 20), "the entry that would have ended first is the one replaced");
        assertTrue(g.ignored(5, 20) && g.ignored(1, 20) && g.ignored(2, 20) && g.ignored(3, 20));
        assertEquals(ShishouIgnore.SLOTS, g.active(20), "the table never grows");
    }

    @Test
    void anIdOfMinusOneIsNeverIgnored() {
        assertFalse(new ShishouIgnore().ignored(-1, 0));
    }

    // ---------------------------------------------------------------- the best three
    @Test
    void topKeepsTheLowestScoresBestFirst() {
        ShishouHunt.Top t = new ShishouHunt.Top(3);
        double[] s = {9.0, 2.0, 7.0, 1.0, 5.0, 8.0, 0.5};
        for (int i = 0; i < s.length; i++) t.offer(i, s[i]);
        assertEquals(3, t.size());
        assertEquals(6, t.index(0), "0.5");
        assertEquals(3, t.index(1), "1.0");
        assertEquals(1, t.index(2), "2.0");
    }

    @Test
    void topWithFewerCandidatesThanSlotsAndTies() {
        ShishouHunt.Top t = new ShishouHunt.Top(ShishouHunt.LOS_CHECKS);
        t.offer(4, 3.0);
        t.offer(5, 3.0);
        assertEquals(2, t.size());
        assertEquals(4, t.index(0), "a tie keeps the earlier one first");
        assertEquals(5, t.index(1));
        ShishouHunt.Top one = new ShishouHunt.Top(1);
        one.offer(9, 4.0);
        one.offer(8, 1.0);
        one.offer(7, 6.0);
        assertEquals(1, one.size());
        assertEquals(8, one.index(0));
    }

    @Test
    void theLineOfSightCostIsBounded() {
        assertEquals(3, ShishouHunt.LOS_CHECKS);
        // default gap 6..8 ticks: at most about ten rays a second while he is looking (and he looks only while he has no target)
        assertTrue(ShishouHunt.raysPerSecond(ShishouHunt.scanGap(ShishouHunt.SCAN_TICKS, 0)) <= 10.0);
        assertTrue(ShishouHunt.raysPerSecond(ShishouHunt.SCAN_TICKS_MIN) <= 30.0, "even at the fastest setting");
    }

    // ---------------------------------------------------------------- the follow flight
    @Test
    void followSpeedEasesInAndCatchesUp() {
        assertEquals(ShishouHunt.FOLLOW_MIN, ShishouHunt.followSpeed(0.0), 1.0e-9);
        assertEquals(ShishouHunt.FOLLOW_EASE_TOP, ShishouHunt.followSpeed(ShishouHunt.FOLLOW_EASE), 1.0e-9, "continuous where the ease ends");
        double prev = -1.0;
        for (double d = 0.0; d <= 80.0; d += 0.5) {
            double v = ShishouHunt.followSpeed(d);
            assertTrue(v >= prev - 1.0e-12, "never slows down as the wielder gets farther: " + d);
            assertTrue(v >= ShishouHunt.FOLLOW_MIN && v <= ShishouHunt.FOLLOW_MAX, "bounded: " + v);
            prev = v;
        }
        assertEquals(ShishouHunt.FOLLOW_MAX, ShishouHunt.followSpeed(ShishouRules.MINION_RETURN + 4), 1.0e-9, "past the return distance he is at full speed");
        assertTrue(ShishouHunt.followSpeed(ShishouRules.MINION_RETURN) > 0.28 * 3, "faster than a sprinting wielder (0.28 blocks a tick) by far");
        assertTrue(ShishouHunt.DORMANT_SPEED > 0.0 && ShishouHunt.DORMANT_SPEED < ShishouHunt.FOLLOW_MAX);
    }

    // ---------------------------------------------------------------- the source rules
    @Test
    void theScanKeepsOnlyHuntableCreaturesBeforeTheCapAndPrefersASightLine() throws IOException {
        String goals = code("ShishouGoals.java");
        String scan = goals.substring(goals.indexOf("static LivingEntity scan("));
        scan = scan.substring(0, scan.indexOf("\n    }\n"));
        int query = scan.indexOf("getEntitiesOfClass(");
        assertTrue(query > 0 && scan.indexOf("isValidTarget", query) > query && scan.indexOf("isIgnored", query) > query, "the query predicate itself filters, so animals never use the examined cap");
        assertFalse(scan.contains("Mob::isAlive"), "the old unfiltered query is gone");
        assertTrue(scan.contains("hasLineOfSight") && scan.contains("ShishouHunt.Top") && scan.contains("LOS_CHECKS"), "the three best are tried for a clear line");
        assertEquals(1, scan.split("hasLineOfSight", -1).length - 1, "one ray call site, inside the loop over the best three");
        assertFalse(scan.contains("getChunk") || scan.contains("sort"), "no chunk load, no sort");
    }

    @Test
    void everyFlightOfTheSwordSteersLikeTheChargeAndTicksEveryTick() throws IOException {
        String goals = code("ShishouGoals.java");
        assertFalse(goals.contains("setWantedPosition"), "no goal asks the move control for a position any more (its air speed is a vanilla default)");
        for (String g : new String[]{"class PatrolGoal", "class StayByWielderGoal", "class DormantPedestalGoal"}) {
            int at = goals.indexOf(g);
            assertTrue(at >= 0, g);
            int end = goals.indexOf("\n    }\n", at);
            assertTrue(goals.substring(at, end).contains("requiresUpdateEveryTick"), g + " runs every tick (a goal runs every other tick otherwise, which halved its speed)");
        }
        assertTrue(goals.contains("sword.glide(") && goals.contains("sword.fly(") && goals.contains("followSpeed("), "the follow flies by the pure follow speed");
        String ent = code("ShishouEntity.java");
        assertFalse(ent.contains("ignoredId") || ent.contains("ignoreUntil"), "the single-slot memory is gone");
        assertTrue(ent.contains("new ShishouIgnore()") && ent.contains("public void giveUp("), "the give-up memory holds several creatures");
    }

    @Test
    void aSpellRayIsNeverLongerThanItsRange() throws IOException {
        String sp = code("ShishouSpells.java");
        int rec = sp.indexOf("public record Caster(");
        assertTrue(rec >= 0);
        String body = sp.substring(rec, sp.indexOf("public record Result("));
        assertTrue(body.contains("public Caster {") && body.contains("look.normalize()"), "the look of every caster is a unit vector (a thrown sword passed the vector to its target)");
        assertTrue(sp.contains("ShishouConfig.damagePercent()") && sp.indexOf("ShishouConfig.damagePercent()") > sp.indexOf("tickClones("), "the clones use the server's damage scale");
    }
}
