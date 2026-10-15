package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.boss.LuciusCombo.Candidate;
import com.newuniverse.nusmp.boss.LuciusCombo.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Lucius's combo planner: no repeats inside a cooldown or the last three casts, every role comes up, deterministic for a seed. */
class LuciusComboTest {
    private static List<Candidate> roster() {
        List<Candidate> out = new ArrayList<>();
        for (String id : LuciusCombo.roster()) out.add(new Candidate(id, 4));
        out.sort((a, b) -> a.id().compareTo(b.id()));
        return out;
    }

    private static List<String> run(long seed, int casts, int gap, int cooldown) {
        LuciusCombo c = new LuciusCombo(seed);
        List<Candidate> pool = roster();
        List<String> seen = new ArrayList<>();
        long now = 0;
        for (int i = 0; i < casts; i++) {
            String id = c.pick(pool, now).orElseThrow();
            c.commit(id, now, cooldown);
            seen.add(id);
            now += gap;
        }
        return seen;
    }

    @Test
    void neverRepeatsInsideItsCooldownOrTheLastThreeCasts() {
        for (long seed = 1; seed <= 20; seed++) {
            List<String> s = run(seed, 200, 30, 240);
            for (int i = 0; i < s.size(); i++)
                for (int k = Math.max(0, i - 3); k < i; k++) assertTrue(!s.get(k).equals(s.get(i)), "seed " + seed + " cast " + i);
            java.util.Map<String, Integer> at = new java.util.HashMap<>();
            for (int i = 0; i < s.size(); i++) {
                Integer prev = at.put(s.get(i), i);
                if (prev != null) assertTrue((i - prev) * 30 >= 240, "cooldown broken for " + s.get(i) + " seed " + seed);
            }
        }
    }

    @Test
    void aThinPoolStillGivesAnAnswerAndAvoidsTheLastCast() {
        LuciusCombo c = new LuciusCombo(7);
        List<Candidate> two = List.of(new Candidate("soul_motes", 6), new Candidate("light_step", 5));
        String prev = null;
        long now = 0;
        for (int i = 0; i < 50; i++) {
            String id = c.pick(two, now).orElseThrow();
            if (prev != null) assertNotEquals(prev, id);
            c.commit(id, now, 100000);                                        // everything stays on cooldown: the fallback still answers
            prev = id;
            now += 10;
        }
        assertTrue(c.pick(List.of(), 0).isEmpty());
        assertEquals("soul_motes", new LuciusCombo(1).pick(List.of(new Candidate("soul_motes", 1)), 0).orElseThrow());
    }

    @Test
    void everyRoleComesUpOver200Casts() {
        Set<Role> seen = EnumSet.noneOf(Role.class);
        for (String id : run(42, 200, 30, 240)) seen.add(LuciusCombo.roleOf(id));
        assertEquals(EnumSet.allOf(Role.class), seen);
    }

    @Test
    void aComboChainsSpatialOffensiveDefensiveOrControlThenMagic() {
        List<String> s = run(5, 8, 400, 240);
        assertEquals(Role.SPATIAL, LuciusCombo.roleOf(s.get(0)));
        assertEquals(Role.OFFENSIVE, LuciusCombo.roleOf(s.get(1)));
        assertEquals(Role.DEFENSIVE, LuciusCombo.roleOf(s.get(2)));
        assertEquals(Role.MAGIC, LuciusCombo.roleOf(s.get(3)));
        assertEquals(Role.SPATIAL, LuciusCombo.roleOf(s.get(4)));
        assertEquals(Role.OFFENSIVE, LuciusCombo.roleOf(s.get(5)));
        assertEquals(Role.CONTROL, LuciusCombo.roleOf(s.get(6)));
        assertEquals(Role.MAGIC, LuciusCombo.roleOf(s.get(7)));
    }

    @Test
    void theSameSeedGivesTheSameFightAndAnotherSeedADifferentOne() {
        assertEquals(run(99, 100, 30, 240), run(99, 100, 30, 240));
        assertNotEquals(run(99, 100, 30, 240), run(100, 100, 30, 240));
    }

    @Test
    void aWiderRosterIsUsedNotOneSpell() {
        assertTrue(new java.util.HashSet<>(run(3, 200, 30, 240)).size() >= 20);
    }

    @Test
    void resetForgetsTheHistory() {
        LuciusCombo c = new LuciusCombo(2);
        c.commit("megiddo", 0, 1000);
        c.reset();
        assertEquals(0L, c.cooldownLeft("megiddo", 0));
        assertEquals(0, c.slot());
        assertTrue(c.lastCasts().isEmpty());
    }

    @Test
    void everyMoveOfHisTablesHasARole() {
        for (var s : com.newuniverse.nusmp.boss.LuciusSpellTable.allSpecs()) assertTrue(LuciusCombo.roster().contains(s.id()), s.id());
    }
}
